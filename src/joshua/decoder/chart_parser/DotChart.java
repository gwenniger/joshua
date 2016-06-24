package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.DotNodeTypeCreater.DotNodeCreater;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.segment_file.Token;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;
import joshua.util.ChartSpan;

/**
 * The DotChart handles Earley-style implicit binarization of translation rules.
 * 
 * The {@link DotNode} object represents the (possibly partial) application of a synchronous rule.
 * The implicit binarization is maintained with a pointer to the {@link Trie} node in the grammar,
 * for easy retrieval of the next symbol to be matched. At every span (i,j) of the input sentence,
 * every incomplete DotNode is examined to see whether it (a) needs a terminal and matches against
 * the final terminal of the span or (b) needs a nonterminal and matches against a completed
 * nonterminal in the main chart at some split point (k,j).
 * 
 * Once a rule is completed, it is entered into the {@link DotChart}. {@link DotCell} objects are
 * used to group completed DotNodes over a span.
 * 
 * There is a separate DotChart for every grammar.
 * DotNodeBase
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 * @author Kristy Hollingshead Seitz
 */
class DotChart<T extends joshua.decoder.chart_parser.DotChart.DotNodeBase<T2>,T2> {

  // ===============================================================
  // Package-protected instance fields
  // ===============================================================
  /**
   * Two-dimensional chart of cells. Some cells might be null. This could definitely be represented
   * more efficiently, since only the upper half of this triangle is every used.
   */
  private ChartSpan<DotCell<T>> dotcells;

  public DotCell<T> getDotCell(int i, int j) {
    return dotcells.get(i, j);
  }

  // ===============================================================
  // Private instance fields (maybe could be protected instead)
  // ===============================================================

  /**
   * CKY+ style parse chart in which completed span entries are stored.
   */
  private Chart dotChart;

  /**
   * Translation grammar which contains the translation rules.
   */
  private Grammar pGrammar;

  /* Length of input sentence. */
  private final int sentLen;

  /* Represents the input sentence being translated. */
  private final Lattice<Token> input;

  /* If enabled, rule terminals are treated as regular expressions. */
  private final boolean regexpMatching;
  /*
   * nonTerminalMatcher determines the behavior of nonterminal matching: strict or soft-syntactic
   * matching
   */
  private final NonterminalMatcher<T> nonTerminalMatcher;

  
  // A DotNodeTypeCreater which takes care of using the right type of DotNodes,using strategy
  private final DotNodeTypeCreater<T,T2> dotNodeTypeCreater;


  // ===============================================================
  // Static fields
  // ===============================================================

  private static final Logger logger = Logger.getLogger(DotChart.class.getName());

  // ===============================================================
  // Constructors
  // ===============================================================

  // TODO: Maybe this should be a non-static inner class of Chart. That would give us implicit
  // access to all the arguments of this constructor. Though we would need to take an argument, i,
  // to know which Chart.this.grammars[i] to use.

  /**
   * Constructs a new dot chart from a specified input lattice, a translation grammar, and a parse
   * chart.
   * 
   * @param input A lattice which represents an input sentence.
   * @param grammar A translation grammar.
   * @param chart A CKY+ style chart in which completed span entries are stored.
   */



  public DotChart(Lattice<Token> input, Grammar grammar, Chart chart,
      NonterminalMatcher<T> nonTerminalMatcher, DotNodeTypeCreater<T,T2> dotNodeTypeCreater, 
      boolean regExpMatching) {
//=======
//  public DotChart(Lattice<Token> input, Grammar grammar, Chart chart, boolean regExpMatching) {
//>>>>>>> 090cb8c5287c85bec08ba4b48c16088e2b9a8449

    this.dotChart = chart;
    this.pGrammar = grammar;
    this.input = input;
    this.sentLen = input.size();

    this.dotcells = new ChartSpan<DotCell<T>>(sentLen, null);
    this.nonTerminalMatcher = nonTerminalMatcher;
    this.dotNodeTypeCreater = dotNodeTypeCreater;
    this.regexpMatching = regExpMatching;

    seed();
  }

  /**
   * Add initial dot items: dot-items pointer to the root of the grammar trie.
   */
  void seed() {
    for (int j = 0; j <= sentLen - 1; j++) {
      if (pGrammar.hasRuleForSpan(j, j, input.distance(j, j))) {
        if (null == pGrammar.getTrieRoot()) {
          throw new RuntimeException("trie root is null");
        }
        addDotItem(pGrammar.getTrieRoot(), j, j, null, null, new SourcePath());
      }
    }
  }

  /**
   * This function computes all possible expansions of all rules over the provided span (i,j). By
   * expansions, we mean the moving of the dot forward (from left to right) over a nonterminal or
   * terminal symbol on the rule's source side.
   * 
   * There are two kinds of expansions:
   * 
   * <ol>
   * <li>Expansion over a nonterminal symbol. For this kind of expansion, a rule has a dot
   * immediately prior to a source-side nonterminal. The main Chart is consulted to see whether
   * there exists a completed nonterminal with the same label. If so, the dot is advanced.
   * 
   * Discovering nonterminal expansions is a matter of enumerating all split points k such that i <
   * k and k < j. The nonterminal symbol must exist in the main Chart over (k,j).
   * 
   * <li>Expansion over a terminal symbol. In this case, expansion is a simple matter of determing
   * whether the input symbol at position j (the end of the span) matches the next symbol in the
   * rule. This is equivalent to choosing a split point k = j - 1 and looking for terminal symbols
   * over (k,j). Note that phrases in the input rule are handled one-by-one as we consider longer
   * spans.
   * </ol>
   */
  void expandDotCell(int i, int j) {
    if (logger.isLoggable(Level.FINEST))
      logger.finest("Expanding dot cell (" + i + "," + j + ")");

    /*
     * (1) If the dot is just to the left of a non-terminal variable, we look for theorems or axioms
     * in the Chart that may apply and extend the dot position. We look for existing axioms over all
     * spans (k,j), i < k < j.
     */
    for (int k = i + 1; k < j; k++) {
      extendDotItemsWithProvedItems(i, k, j, false);
    }

    /*
     * (2) If the the dot-item is looking for a source-side terminal symbol, we simply match against
     * the input sentence and advance the dot.
     */
    Node<Token> node = input.getNode(j - 1);
    for (Arc<Token> arc : node.getOutgoingArcs()) {

      int last_word = arc.getLabel().getWord();
      int arc_len = arc.getHead().getNumber() - arc.getTail().getNumber();

      // int last_word=foreign_sent[j-1]; // input.getNode(j-1).getNumber(); //

      if (null != dotcells.get(i, j - 1)) {
        // dotitem in dot_bins[i][k]: looking for an item in the right to the dot


        for (T dotNode : dotcells.get(i, j - 1).getDotNodes()) {

          // String arcWord = Vocabulary.word(last_word);
          // Assert.assertFalse(arcWord.endsWith("]"));
          // Assert.assertFalse(arcWord.startsWith("["));
          // logger.info("DotChart.expandDotCell: " + arcWord);

          // List<Trie> child_tnodes = ruleMatcher.produceMatchingChildTNodesTerminalevel(dotNode,
          // last_word);

          List<Trie> child_tnodes = null;

          if (this.regexpMatching) {
            child_tnodes = matchAll(dotNode, last_word);
          } else {
            Trie child_node = dotNode.trieNode.match(last_word);
            child_tnodes = Arrays.asList(child_node);
          }

          if (!(child_tnodes == null || child_tnodes.isEmpty())) {
            for (Trie child_tnode : child_tnodes) {
              if (null != child_tnode) {
                addDotItem(child_tnode, i, j - 1 + arc_len, dotNode.getAntSuperNodes(), null,
                    dotNode.srcPath.extend(arc));
              }
            }
          }
        }
      }
    }
  }

  /**
   * note: (i,j) is a non-terminal, this cannot be a cn-side terminal, which have been handled in
   * case2 of dotchart.expand_cell add dotitems that start with the complete super-items in
   * cell(i,j)
   */
  void startDotItems(int i, int j) {
    extendDotItemsWithProvedItems(i, i, j, true);
  }

  
  // ===============================================================
  // Private methods
  // ===============================================================

  /**
   * Attempt to combine an item in the dot chart with an item in the main chart to create a new item
   * in the dot chart. The DotChart item is a {@link DotNode} begun at position i with the dot
   * currently at position k, that is, a partially-applied rule.
   * 
   * In other words, this method looks for (proved) theorems or axioms in the completed chart that
   * may apply and extend the dot position.
   * 
   * @param i Start index of a dot chart item
   * @param k End index of a dot chart item; start index of a completed chart item
   * @param j End index of a completed chart item
   * @param skipUnary if true, don't extend unary rules
   */
  private void extendDotItemsWithProvedItems(int i, int k, int j, boolean skipUnary) {
    if (this.dotcells.get(i, k) == null || this.dotChart.getCell(k, j) == null) {
      return;
    }

    // complete super-items (items over the same span with different LHSs)
    List<SuperNode> superNodes = new ArrayList<SuperNode>(this.dotChart.getCell(k, j)
        .getSortedSuperItems().values());
    
    List<SuperNode> oovAndGoalSymbolSuperNodes = nonTerminalMatcher.getOOAndGoalLabelSuperNodeSubList(superNodes);
    SuperNode firstNeitherOOVNorGoalSymbolSuperNode = nonTerminalMatcher.getFirstNeitherOOVNorGoalLabelSuperNode(superNodes);
    
    List<SuperNode> neitherOOVNorGoalSymbolSuperNodes = null;
    // We only compete the neitherOOVNorGoalSymbolSuperNodes if they are needed, because computation is relatively expensive
    if(nonTerminalMatcher.performFuzzyMatchingWithRefinedStateExploration()){
      neitherOOVNorGoalSymbolSuperNodes = nonTerminalMatcher.getNeitherOOVNorGoalLabelSuperNodeSubList(superNodes);
    }

    /* For every partially complete item over (i,k) */
    for (T dotNode : dotcells.get(i, k).dotNodes) {

      // String arcWord = Vocabulary.word(superNode.lhs);
      // logger.info("DotChart.extendDotItemsWithProvedItems: " + arcWord);
      // Assert.assertTrue(arcWord.endsWith("]"));
      // Assert.assertTrue(arcWord.startsWith("["));     
      
      if(nonTerminalMatcher.performFuzzyMatching()){
        
        // Here it does not really matter what supernode we use, since no matching is enforced, so we use the first
        SuperNodeAlternativesSpecification superNodesAlternativesSpecification = null;
        
        if(nonTerminalMatcher.performFuzzyMatchingWithRefinedStateExploration()){
          superNodesAlternativesSpecification =
              new SuperNodeAlternativesSpecification(neitherOOVNorGoalSymbolSuperNodes,firstNeitherOOVNorGoalSymbolSuperNode,false);  
        }
        else{
          superNodesAlternativesSpecification =
            new SuperNodeAlternativesSpecification(oovAndGoalSymbolSuperNodes,firstNeitherOOVNorGoalSymbolSuperNode,true);
        }        
        
        //logger.info(" addEfficientMultiLabelDotItemsFuzzyMatching (1a)");
        addEfficientMultiLabelDotItemsFuzzyMatching(i, j, superNodesAlternativesSpecification,firstNeitherOOVNorGoalSymbolSuperNode,
            dotNode, skipUnary);
      }else{
        addDotItemsBasic(i, j, neitherOOVNorGoalSymbolSuperNodes, dotNode, skipUnary);
        //logger.info("  addDotItemsBasic(i, j, neitherOOVNorGoalSymbolSuperNodes, dotNode, skipUnary) (1b)");
      }     
      addDotItemsBasic(i, j, oovAndGoalSymbolSuperNodes, dotNode, skipUnary);
      //logger.info(" addDotItemsBasic(i, j, oovAndGoalSymbolSuperNodes, dotNode, skipUnary) (2)");
    }
  }
  
  
  
  
  @SuppressWarnings("unchecked")
  private void addEfficientMultiLabelDotItemsFuzzyMatching(int i, int j,SuperNodeAlternativesSpecification superNodesAlternativesSpecification,
      SuperNode firstNeitherOOVNorGoalSuperNode, T dotNode, boolean skipUnary){
    /* For every completed nonterminal in the main chart */
    
    if(firstNeitherOOVNorGoalSuperNode == null){
      //logger.info("Gideon:  firstNeitherOOVNorGoalSuperNode == null :(");
      return;
    } 

      List<Trie> child_tnodes = nonTerminalMatcher.produceMatchingChildTNodesNonterminalLevel(
          dotNode, firstNeitherOOVNorGoalSuperNode);


      if (!child_tnodes.isEmpty()) {
        

        //logger.info("Gideon: looping over childe_tnodes");
        for (Trie child_tnode : child_tnodes) {
          if (child_tnode != null) {
            if ((!skipUnary) || (child_tnode.hasExtensions())) {
              
              //logger.info("Gideon: addDotItem for nonterminal!!!");
              addDotItem(child_tnode, i, j, dotNode.getAntSuperNodes(), (T2) superNodesAlternativesSpecification, dotNode
                  .getSourcePath().extendNonTerminal());
            }

          }
        }
      }               
  }
  
  private void addDotItemsBasic(int i, int j,List<SuperNode> neitherOOVNorGoalSymbolSuperNodes,T dotNode, boolean skipUnary){
  /* For every completed nonterminal in the main chart */
  for (SuperNode superNode : neitherOOVNorGoalSymbolSuperNodes) {

    List<Trie> child_tnodes = nonTerminalMatcher.produceMatchingChildTNodesNonterminalLevel(
        dotNode, superNode);

    if (!child_tnodes.isEmpty()) {

      for (Trie child_tnode : child_tnodes) {
        if (child_tnode != null) {
          if ((!skipUnary) || (child_tnode.hasExtensions())) {
            
            addDotItem(child_tnode, i, j, dotNode.getAntSuperNodes(),dotNodeTypeCreater.createSuperNodeTypeFromSingleSuperNode(superNode), dotNode
                .getSourcePath().extendNonTerminal());
          }
        }
      }
    }
  }
  }
  

  /*
   * We introduced the ability to have regular expressions in rules for matching against terminals.
   * For example, you could have the rule
   * 
   * <pre> [X] ||| l?s herman?s ||| siblings </pre>
   * 
   * When this is enabled for a grammar, we need to test against *all* (positive) outgoing arcs of
   * the grammar trie node to see if any of them match, and then return the whole set. This is quite
   * expensive, which is why you should only enable regular expressions for small grammars.
   */

  private ArrayList<Trie> matchAll(T dotNode, int wordID) {
    ArrayList<Trie> trieList = new ArrayList<Trie>();
    HashMap<Integer, ? extends Trie> childrenTbl = dotNode.trieNode.getChildren();

    if (childrenTbl != null && wordID >= 0) {
      // get all the extensions, map to string, check for *, build regexp
      for (Integer arcID : childrenTbl.keySet()) {
        if (arcID == wordID) {
          trieList.add(childrenTbl.get(arcID));
        } else {
          String arcWord = Vocabulary.word(arcID);
          if (Vocabulary.word(wordID).matches(arcWord)) {
            trieList.add(childrenTbl.get(arcID));
          }
        }
      }
    }
    return trieList;
  }


  /**
   * Creates a {@link DotNode} and adds it into the {@link DotChart} at the correct place. These
   * are (possibly incomplete) rule applications. 
   * 
   * @param tnode the trie node pointing to the location ("dot") in the grammar trie
   * @param i
   * @param j
   * @param antSuperNodesIn the supernodes representing the rule's tail nodes
   * @param curSuperNode the lefthand side of the rule being created
   * @param srcPath the path taken through the input lattice
   */
  private void addDotItem(Trie tnode, int i, int j, List<T2> antSuperNodesIn,
      T2 curSuperNode, SourcePath srcPath) {
    List<T2> antSuperNodes = new ArrayList<T2>();

    if (antSuperNodesIn != null) {
      antSuperNodes.addAll(antSuperNodesIn);
    }
    if (curSuperNode != null) {
      antSuperNodes.add(curSuperNode);
    }

    //DotNode item = new DotNode(i, j, tnode, antSuperNodes, srcPath);
    T item = dotNodeTypeCreater.createDotNodeType(i, j, tnode, antSuperNodes, srcPath);
    if (dotcells.get(i, j) == null) {
      dotcells.set(i, j, new DotCell<T>());
    }
    dotcells.get(i, j).addDotNode(item);
    dotChart.nDotitemAdded++;

    if (logger.isLoggable(Level.FINEST)) {
      logger.finest(String.format("Add a dotitem in cell (%d, %d), n_dotitem=%d, %s", i, j,
          dotChart.nDotitemAdded, srcPath));

      RuleCollection rules = tnode.getRuleCollection();
      if (rules != null) {
        for (Rule r : rules.getRules()) {
          // System.out.println("rule: "+r.toString());
          logger.finest(r.toString());
        }
      }
    }
  }

  protected boolean performFuzzyMatching(){
    return nonTerminalMatcher.performFuzzyMatching();
  }
  
  protected boolean useSeparateCubePruningStatesForMatchingSubstitutions(){
    return nonTerminalMatcher.useSeparateCubePruningStatesForMatchingSubstitutions();
  }
  
  protected boolean exploreAllLabelsForGlueRulesInCubePruningInitialization() {
    return nonTerminalMatcher.exploreAllLabelsForGlueRulesInCubePruningInitialization();
  }
  
  protected boolean exploreAllPossibleLabelSubstitutionsForAllRulesInCubePruningInitialization() {
    return nonTerminalMatcher.exploreAllPossibleLabelSubstitutionsForAllRulesInCubePruningInitialization();
  }
  
  // ===============================================================
  // Package-protected classes
  // ===============================================================

  /**
   * A DotCell groups together DotNodes that have been applied over a particular span. A DotNode, in
   * turn, is a partially-applied grammar rule, represented as a pointer into the grammar trie
   * structure.
   */
  static class DotCell<T extends DotNodeBase<?>> {

    // Package-protected fields
    private List<T> dotNodes = new ArrayList<T>();

    public List<T> getDotNodes() {
      return dotNodes;
    }

    private void addDotNode(T dt) {
      /*
       * if(l_dot_items==null) l_dot_items= new ArrayList<DotItem>();
       */
      dotNodes.add(dt);
    }
  }

  /**
   * A DotNode represents the partial application of a rule rooted to a particular span (i,j). It
   * maintains a pointer to the trie node in the grammar for efficient mapping.
   */
  static abstract class DotNodeBase<T> {

    // =======================================================
    // Package-protected instance fields
    // =======================================================

    protected int i, j; //start and end position in the chart
    protected Trie trieNode = null; // dot_position, point to grammar trie node, this is the only
                                  // place that the DotChart points to the grammar
     
    /* The source lattice cost of applying the rule */
    protected SourcePath srcPath;


    protected  DotNodeBase(int i, int j, Trie trieNode, SourcePath srcPath) {
      this.i = i;
      this.j = j;
      this.trieNode = trieNode;
      this.srcPath = srcPath;
    }

    
    /* A list of grounded (over a span) nonterminals that have been crossed in traversing the rule */
    protected List<T> antSuperNodes = null;
        

    @Override
    public String toString() {
      int size = 0;
      if (trieNode != null && trieNode.getRuleCollection() != null)
        size = trieNode.getRuleCollection().getRules().size();
      return String.format("DOTNODE i=%d j=%d #rules=%d #tails=%d", i, j, size, antSuperNodes.size());
    }
    

    public boolean equals(Object obj) {
      if (obj == null)
        return false;
      if (!this.getClass().equals(obj.getClass()))
        return false;
      DotNodeBase state = (DotNodeBase) obj;

      /*
       * Technically, we should be comparing the span inforamtion as well, but that would require us
       * to store it, increasing memory requirements, and we should be able to guarantee that we
       * won't be comparing DotNodes across spans.
       */
      // if (this.i != state.i || this.j != state.j)
      // return false;

      if (this.trieNode != state.trieNode)
        return false;

      return true;
    }

    /**
     * Technically the hash should include the span (i,j), but since DotNodes are grouped by span,
     * this isn't necessary, and we gain something by not having to store the span.
     */
    public int hashCode() {
      return this.trieNode.hashCode();
    }

    // convenience function
    public boolean hasRules() {
      return getTrieNode().getRuleCollection() != null && getTrieNode().getRuleCollection().getRules().size() != 0;
    }
    
    public RuleCollection getRuleCollection() {
      return getTrieNode().getRuleCollection();
    }

    public Trie getTrieNode() {
      return trieNode;
    }

    public SourcePath getSourcePath() {
      return srcPath;
    }
    
    public abstract List<T> getAntSuperNodes();
    

    public int begin() {
      return i;
    }
  
    public int end() {
      return j;
    }
  }
      
  
  
  /**
   * A DotNode represents the partial application of a rule rooted to a particular span (i,j). It
   * maintains a pointer to the trie node in the grammar for efficient mapping.
   */
  static class DotNode extends DotNodeBase<SuperNode>{  
    
    /**
     * Initialize a dot node with the span, grammar trie node, list of supernode tail pointers, and
     * the lattice sourcepath.
     * 
     * @param i
     * @param j
     * @param trieNode
     * @param antSuperNodes
     * @param srcPath
     */
    public DotNode(int i, int j, Trie trieNode, List<SuperNode> antSuperNodes, SourcePath srcPath) {
      super(i,j,trieNode,srcPath);
      this.antSuperNodes = antSuperNodes;
    }

    @Override
    public List<SuperNode> getAntSuperNodes() {
        return antSuperNodes;
    }      
  }
  
  /**
   * A specialized DotNode class that will be used for efficiently decoding 
   * with fuzzy matching, compactly storing many DotItems that are identical,
   * except for their labels
   */
  static class DotNodeMultiLabel extends DotNodeBase<SuperNodeAlternativesSpecification>{    
    
    public DotNodeMultiLabel(int i, int j, Trie trieNode, List<SuperNodeAlternativesSpecification> antSuperNodeLists, SourcePath srcPath) {
      super(i,j,trieNode,srcPath);
      this.antSuperNodes = antSuperNodeLists;
    }

    @Override
    public List<SuperNodeAlternativesSpecification> getAntSuperNodes() {
      return antSuperNodes;
    }
    
  }

  
  
  
  static class SuperNodeAlternativesSpecification{
    final List<SuperNode> alternativeAcceptableOrForbiddenSuperNodes;
 // We need this extra SuperNode, because the list of alternativeAcceptableOrForbiddenSuperNodes may be empty
    private final SuperNode firstSuperNode;   
    private final boolean describesForbiddenSuperNodes;
    
    public SuperNodeAlternativesSpecification(List<SuperNode> alternativeAcceptableOrForbiddenSuperNodes,SuperNode firstSuperNode, 
        boolean describesForbiddenSuperNodes){
      this.alternativeAcceptableOrForbiddenSuperNodes = alternativeAcceptableOrForbiddenSuperNodes;
      this.firstSuperNode = firstSuperNode;
      this.describesForbiddenSuperNodes = describesForbiddenSuperNodes;
    }
    
    public List<SuperNode> getAlternativeSuperNodes(){
      return this.alternativeAcceptableOrForbiddenSuperNodes;
    }
    
    public boolean describesAcceptableSuperNodes(){
      return !this.describesForbiddenSuperNodes;
    }
    
    public boolean describesForbiddenSuperNodes(){
      return describesForbiddenSuperNodes;
    }
    
    public SuperNode getRepresentingSuperNode(){
      return this.firstSuperNode;
    }
    
    public void throwRuntimeExceptionIfNotDescribingAcceptableSuperNodes(String callingMethodName){
      if(!describesAcceptableSuperNodes()){
        throw new RuntimeException("Error: " + callingMethodName + " used with "
            + "Dotnode containing SuperNodeAlternativesSpecification that does not describe acceptable supernodes");
      }
    }
      
  }

    

}
