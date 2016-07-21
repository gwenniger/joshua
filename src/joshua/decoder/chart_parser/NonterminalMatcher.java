package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.chart_parser.DotChart.DotNodeBase;
import joshua.decoder.chart_parser.DotChart.DotNodeMultiLabel;
import joshua.decoder.chart_parser.DotNodeTypeCreater.DotNodeCreater;
import joshua.decoder.chart_parser.DotNodeTypeCreater.DotNodeMultiLabelCreater;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammarEfficientNonterminalLookup;
import joshua.decoder.ff.tm.hash_based.MemoryBasedTrieEfficientNonterminalLookup;
import joshua.lattice.Lattice;
import joshua.decoder.segment_file.Token;

/**
 * This abstract class and its implementations serve to refine the behavior of DotChart using
 * strategy. Basically there are different ways that nonterminals of rules can be matched, either
 * strict, or soft syntactic (nonterminals can all match each other). This interface defines a
 * method that produce matching nodes for the nonterminal level. The interface is then implemented
 * in different classes for the different types of matching (currently just strict or
 * soft-syntactic)
 * 
 * The factory method produces different flavors of NonterminalMatcher corresponding to strict
 * (basic) matching, Regular Expression matching and soft syntactic matching. Notice that regular
 * expression matching and soft constraint matching can in fact be combined, getting the 'loosest'
 * way of matching possible.
 * 
 * @author Gideon Maillette de Buy Wenniger <gemdbw AT gmail DOT com>
 * 
 */
public abstract class NonterminalMatcher <T extends DotNodeBase> {

  public static boolean isOOVLabelOrGoalLabel(String label, JoshuaConfiguration joshuaConfiguration) {
    return (label.equals(joshuaConfiguration.default_non_terminal) || label
        .equals(joshuaConfiguration.goal_symbol));
  }
  
  public static Integer getGoalKey(JoshuaConfiguration joshuaConfiguration){
    return Vocabulary.id(joshuaConfiguration.goal_symbol);
  }
  
  
  public static Integer getOOVKey(JoshuaConfiguration joshuaConfiguration){
    return Vocabulary.id(joshuaConfiguration.default_non_terminal);
  }
  

  /**
   * This method returns a list of all indices corresponding to Nonterminals in the Vocabulary
   * 
   * @return
   */
  public static List<Integer> getAllNonterminalIndicesExceptForGoalAndOOV(
      JoshuaConfiguration joshuaConfiguration) {
    List<Integer> result = new ArrayList<Integer>();
    List<Integer> nonterminalIndices = Vocabulary.getNonterminalIndices();
    for (Integer nonterminalIndex : nonterminalIndices) {
      if (!isOOVLabelOrGoalLabel(Vocabulary.word(nonterminalIndex), joshuaConfiguration)) {
        result.add(nonterminalIndex);
      }
    }
    return result;
  }

  
  public static Set<Integer> getGoalAndOOVNonterminalIndicesNegative(
      JoshuaConfiguration joshuaConfiguration) {
    Set<Integer> result = new HashSet<Integer>();
    List<Integer> nonterminalIndices = Vocabulary.getNonterminalIndices();
    for (Integer nonterminalIndex : nonterminalIndices) {
      if (isOOVLabelOrGoalLabel(Vocabulary.word(nonterminalIndex), joshuaConfiguration)) {
        
        if(nonterminalIndex < 0 ){
          throw new RuntimeException("Expected to get a positive index");
        }
        // We want negative nonterminal indexes for compatibility with the indices of Super Nodes
        // Since  Vocabulary.getNonterminalIndices() returns positive indices, we have to negate the value
        result.add(-nonterminalIndex);
      }
    }
    return result;
  }
  
  public static NonterminalMatcher<?> createNonterminalMatcher(Logger logger,
      JoshuaConfiguration joshuaConfiguration) {
    List<Integer> allNonterminalIndicesExceptForGoalAndOOV = getAllNonterminalIndicesExceptForGoalAndOOV(joshuaConfiguration);

    if (allNonterminalIndicesExceptForGoalAndOOV.isEmpty()) {
      throw new RuntimeException(
          "Error: NonterminalMatcherFactory. createNonterminalMatcher -  empty nonterminal indices table");
    }

    if (joshuaConfiguration.fuzzy_matching) {
      return new StandardNonterminalMatcherSoftConstraints(logger, joshuaConfiguration);
    } else {
      return new StandardNonterminalMatcherStrict(logger, joshuaConfiguration);
    }
  }

  @SuppressWarnings("unchecked")
  public static DotChart<?,?> createDotChart(Logger logger,
      JoshuaConfiguration joshuaConfiguration, Lattice<Token> inputLattice,
      Grammar grammar,Chart theChart,NonterminalMatcher<?> nonterminalMatcher
      ) {
    List<Integer> allNonterminalIndicesExceptForGoalAndOOV = getAllNonterminalIndicesExceptForGoalAndOOV(joshuaConfiguration);

    if (allNonterminalIndicesExceptForGoalAndOOV.isEmpty()) {
      throw new RuntimeException(
          "Error: NonterminalMatcherFactory. createNonterminalMatcher -  empty nonterminal indices table");
    }

    if (joshuaConfiguration.fuzzy_matching) {
      DotNodeMultiLabelCreater  dotNodeMultiLabelCreater  = new  DotNodeMultiLabelCreater();
      return new DotChart<DotNodeMultiLabel, DotChart.SuperNodeAlternativesSpecification>(inputLattice,grammar, theChart, (NonterminalMatcher<DotNodeMultiLabel>) nonterminalMatcher,dotNodeMultiLabelCreater, grammar.isRegexpGrammar());
    } else {     
      DotNodeCreater dotNodeCreater =  new DotNodeCreater();
      return new DotChart<DotNode,SuperNode>(inputLattice,grammar, theChart, (NonterminalMatcher<DotNode>) nonterminalMatcher,dotNodeCreater, grammar.isRegexpGrammar());
    }
  }
  
  protected final Logger logger;
  protected final JoshuaConfiguration joshuaConfiguration;

  protected NonterminalMatcher(Logger logger, JoshuaConfiguration joshuaConfiguration) {
    this.logger = logger;
    this.joshuaConfiguration = joshuaConfiguration;
  }

  /**
   * This is the abstract method used to get the matching child nodes for the nonterminal level
   * 
   * @param dotNode
   * @param superNode
   * @return
   */
  public abstract List<Trie> produceMatchingChildTNodesNonterminalLevel(T dotNode,
      SuperNode superNode);
  
  
  protected abstract boolean performFuzzyMatching();
  protected abstract boolean exploreAllPossibleLabelSubstitutionsForAllRulesInCubePruningInitialization();
  protected abstract boolean useSeparateCubePruningStatesForMatchingSubstitutions();
  protected abstract boolean exploreAllLabelsForGlueRulesInCubePruningInitialization();
  
  protected boolean performFuzzyMatchingWithRefinedStateExploration(){
    return exploreAllLabelsForGlueRulesInCubePruningInitialization() || 
        useSeparateCubePruningStatesForMatchingSubstitutions() || exploreAllPossibleLabelSubstitutionsForAllRulesInCubePruningInitialization();
  }
  
  private static boolean isNonterminal(int wordIndex) {
    return wordIndex < 0;
  }

  private List<Trie> getNonTerminalsListFromChildren(Trie trie, int wordID) {
    HashMap<Integer, ? extends Trie> childrenTbl = trie.getChildren();
    List<Trie> trieList = new ArrayList<Trie>();

    if(trie instanceof MemoryBasedTrieEfficientNonterminalLookup){
      
      // Optimization: we directly obtain an iterator over nonterminals that are neither OOV nor goal nonterminals
      Iterator<Integer> nonterminalIterator = trie.getNeitherOOVNorGoalLabelNonterminalExtensionIterator(joshuaConfiguration);
      
      while (nonterminalIterator.hasNext()) {
        int nonterminalIndex = nonterminalIterator.next();      
        trieList.add(childrenTbl.get(nonterminalIndex));      
      }
    }
    else{    
      Iterator<Integer> nonterminalIterator = trie.getNonterminalExtensionIterator();      
      
      while (nonterminalIterator.hasNext()) {
        int nonterminalIndex = nonterminalIterator.next();      
        if(!NonterminalMatcher.isOOVLabelOrGoalLabel(Vocabulary.word(nonterminalIndex), joshuaConfiguration)){        
          trieList.add(childrenTbl.get(nonterminalIndex));      
        }  
      }
    }
      
    


    return trieList;

  }

  protected List<Trie> matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(DotNodeMultiLabel dotNode, int wordID) {

    // logger.info("wordID: " + wordID + " Vocabulary.word(Math.abs(wordID)) "
    // + Vocabulary.word(Math.abs(wordID)));

    if (!isNonterminal(wordID)) {
      throw new RuntimeException("Error : expexted nonterminal, but did not get it "
          + "in matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(DotNode dotNode, int wordID)");
    }
    return getNonTerminalsListFromChildren(dotNode.getTrieNode(), wordID);

  }

  /**
   * For usage by fuzzy matching without dot chart
   * @param trieNode
   * @param wordID
   * @return
   */
  protected List<Trie> matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(Trie trieNode, int wordID) {
    if (!isNonterminal(wordID)) {
      throw new RuntimeException("Error : expexted nonterminal, but did not get it "
          + "in matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(DotNode dotNode, int wordID)");
    }
    return getNonTerminalsListFromChildren(trieNode, wordID);

  }
  
  
  public List<SuperNode> getOOAndGoalLabelSuperNodeSubList(List<SuperNode> superNodes){
    List<SuperNode> result = new ArrayList<SuperNode>();
    for(SuperNode superNode : superNodes){
      if(isOOVLabelOrGoalLabel(Vocabulary.word(superNode.lhs), joshuaConfiguration)){
        result.add(superNode);
      }
    }
    return result;
  }

  /**
   * Returns none, zero or both of the Goal key and OOV key, depending on which of these 
   * are present in the input list (basically an intersection of the set of these two 
   * elements with the elements in the input list)
   * 
   * Provided the set implements the contains method with O(1) complexity, which
   * is true for a HashSet as well as the key set of a HashMap (the use case)
   * then the complexity of this method is also O(1)
   * 
   * @param superNodeLHSs
   * @return
   */
  public List<Integer> getOOAndGoalLabelSuperNodeLHSSubList(Set<Integer> superNodeLHSs){
    List<Integer> result = new ArrayList<Integer>();
   
    if(superNodeLHSs.contains(getOOVKey(joshuaConfiguration))){
      result.add(getOOVKey(joshuaConfiguration));
    }
    
    if(superNodeLHSs.contains(getGoalKey(joshuaConfiguration))){
      result.add(getGoalKey(joshuaConfiguration));
    }
    
    return result;
  }
  
  
  public List<SuperNode> getNeitherOOVNorGoalLabelSuperNodeSubList(List<SuperNode> superNodes){
    List<SuperNode> result = new ArrayList<SuperNode>();
    for(SuperNode superNode : superNodes){
      if(!isOOVLabelOrGoalLabel(Vocabulary.word(superNode.lhs), joshuaConfiguration)){
        result.add(superNode);
      }
    }
    return result;
  }

  public SuperNode getFirstNeitherOOVNorGoalLabelSuperNode(List<SuperNode> superNodes){
    for(SuperNode superNode : superNodes){
      if(!isOOVLabelOrGoalLabel(Vocabulary.word(superNode.lhs), joshuaConfiguration)){
        return superNode;
      }
    }
    return null;
  }

  public Integer getFirstNeitherOOVNorGoalLabelSuperNodeLHS(Set<Integer> superNodeLHSs){
    for(Integer superNodeLHS : superNodeLHSs){
      if(!isOOVLabelOrGoalLabel(Vocabulary.word(superNodeLHS), joshuaConfiguration)){
        return superNodeLHS;
      }
    }
    return null;
  }
  
  public static List<Trie> produceStandardMatchingChildTNodesNonterminalLevel(DotNode dotNode,
      SuperNode superNode) {
    Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
    List<Trie> child_tnodes = Collections.singletonList(child_node);
    return child_tnodes;
  }

  protected abstract static class StandardNonterminalMatcher<T extends DotNodeBase> extends NonterminalMatcher<T> {

    protected StandardNonterminalMatcher(Logger logger, JoshuaConfiguration joshuaConfiguration) {
      super(logger, joshuaConfiguration);
    }
  }

  protected static class StandardNonterminalMatcherStrict extends StandardNonterminalMatcher<DotNode> {

    protected StandardNonterminalMatcherStrict(Logger logger,
        JoshuaConfiguration joshuaConfiguration) {
      super(logger, joshuaConfiguration);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {
      return produceStandardMatchingChildTNodesNonterminalLevel(dotNode, superNode);
    }

    @Override
    protected boolean performFuzzyMatching() {
      return false;
    }

    @Override
    protected boolean useSeparateCubePruningStatesForMatchingSubstitutions() {
      return false;
    }

    @Override
    protected boolean exploreAllLabelsForGlueRulesInCubePruningInitialization() {
      return false;
    }

    @Override
    protected boolean exploreAllPossibleLabelSubstitutionsForAllRulesInCubePruningInitialization() {
      return false;
    }
  }

  protected static class StandardNonterminalMatcherSoftConstraints extends
      StandardNonterminalMatcher<DotNodeMultiLabel> {

    /**
     * 
     * @param logger
     * @param joshuaConfiguration
     */
    protected StandardNonterminalMatcherSoftConstraints(Logger logger,
        JoshuaConfiguration joshuaConfiguration) {
      super(logger, joshuaConfiguration);
    }

    /**
     * This method will perform strict matching if the target node superNode is a Goal Symbol.
     * Otherwise it will call a method that produces all available substitutions that correspond to
     * Nonterminals.
     * 
     * @param dotNode
     * @param superNode
     */
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNodeMultiLabel dotNode,
        SuperNode superNode) {

      // We do not allow substitution of other things for GOAL labels or OOV
      // symbols
      if (isOOVLabelOrGoalLabel(Vocabulary.word(superNode.lhs), joshuaConfiguration)) {
        // logger.info("BLAA - Vocabulary.word(superNode.lhs)" +
        // Vocabulary.word(superNode.lhs));
        Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
        // logger.info("child_node.toString()" + child_node);
        List<Trie> child_tnodes = Collections.singletonList(child_node);
        return child_tnodes;
      } else {
        // logger.info("Vocabulary.word(superNode.lhs): " +
        // Vocabulary.word(superNode.lhs));
        return matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(dotNode, superNode.lhs);
      }
    }

    @Override
    protected boolean performFuzzyMatching() {
      return true;
    }

    @Override
    protected boolean useSeparateCubePruningStatesForMatchingSubstitutions() {
      return joshuaConfiguration.separate_cube_pruning_states_for_matching_substitutions;
    }
    
    @Override
    protected boolean exploreAllLabelsForGlueRulesInCubePruningInitialization() {
      return joshuaConfiguration.explore_all_labels_for_glue_rules_in_cube_pruning_initialization;
    }

    @Override
    protected boolean exploreAllPossibleLabelSubstitutionsForAllRulesInCubePruningInitialization() {
      return joshuaConfiguration.explore_all_possible_label_substitutions_for_all_rules_in_cube_pruning_initialization;
    }
  }
  
  
}
