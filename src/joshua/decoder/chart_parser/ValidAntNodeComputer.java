package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.chart_parser.DotChart.DotNodeBase;
import joshua.decoder.chart_parser.DotChart.DotNodeMultiLabel;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * This abstract class takes care of computation of valid HGNodes ("antnodes") for a DotNodeBase
 * instance with specialization for DotNHode or DotNodeMultiLabel.
 * 
 * For DotNode the behavior is mostly trivial, in this case the antnodes get grouped by the
 * SuperNodes and all HGNodes grouped under a SuperNode are automatically valid.
 * 
 * For a DotNodeMultiLabel (used for fuzzy matching) things become more complicated, and this
 * motivates this class. In this case, there is a list of lists of Supernodes [L1,..,Ln] , every
 * list L1...Ln containing alternative labels for the n-th nonterminal. Now instead of taking
 * nonterminals grouped under any SuperNode, we rather directly use the full list of HGNodes
 * obtained from the corresponding Cell for the nonterminal index. Then
 * 
 * 
 * 
 * @author gemaille
 * 
 * @param <T>
 */
public abstract class ValidAntNodeComputer<T extends DotNodeBase<?>> {

  protected final T dotNode;
  protected final int nonterminalIndex;

  protected ValidAntNodeComputer(T dotNode, int nonterminalIndex) {
    this.dotNode = dotNode;
    this.nonterminalIndex = nonterminalIndex;
  }

  public static ValidAntNodeComputerBasic createValidAntNodeComputerBasic(DotNode dotNode,
      int nonterminalIndex) {
    return new ValidAntNodeComputerBasic(dotNode, nonterminalIndex);
  }

  public static ValidAntNodeComputerFuzzyMatching createValidAntNodeComputerFuzzyMatching(
      DotNodeMultiLabel dotNode, int nonterminalIndex) {
    return new ValidAntNodeComputerFuzzyMatching(dotNode, nonterminalIndex,
        ValidAntNodeComputerFuzzyMatching.getAcceptableLabelIndicesNonterminal(dotNode,
            nonterminalIndex));
  }

  public static ValidAntNodeComputerFuzzyMatching createValidAntNodeComputerFuzzyMatchingNotMatchingRuleLabel(
      DotNodeMultiLabel dotNode, int nonterminalIndex, Rule rule) {
    return new ValidAntNodeComputerFuzzyMatching(dotNode, nonterminalIndex,
        ValidAntNodeComputerFuzzyMatching.getAcceptableLabelIndicesNonterminalNotMatchingRuleLabel(
            dotNode, nonterminalIndex, rule));
  }

  
  public static List<ValidAntNodeComputer<DotNodeMultiLabel>> createValidAntNodeComputersStandardFuzzyMatching(
      DotNodeMultiLabel dotNode) {
    List<ValidAntNodeComputer<DotNodeMultiLabel>> result = new ArrayList<ValidAntNodeComputer<DotNodeMultiLabel>>();
    for (int nonterminalIndex = 0; nonterminalIndex < dotNode.getAntSuperNodes().size(); nonterminalIndex++) {
      result.add(ValidAntNodeComputer.createValidAntNodeComputerFuzzyMatching(dotNode,
          nonterminalIndex));
    }
    return result;
  }

  public static List<ValidAntNodeComputer<DotNodeMultiLabel>> createValidAntNodeComputersImprovedCubePruningFuzzyMatching(
      DotNodeMultiLabel dotNode, Rule rule, List<Boolean> useFixedRuleMatchingNonterminalsFlags) {
    List<ValidAntNodeComputer<DotNodeMultiLabel>> result = new ArrayList<ValidAntNodeComputer<DotNodeMultiLabel>>();
    for (int nonterminalIndex = 0; nonterminalIndex < dotNode.getAntSuperNodes().size(); nonterminalIndex++) {
      boolean useFixedRuleMatchingNonterminal = useFixedRuleMatchingNonterminalsFlags
          .get(nonterminalIndex);
      
      //System.err.println(">>>> createValidAntNodeComputersImprovedCubePruningFuzzyMatching >>>  useFixedRuleMatchingNonterminal: " + useFixedRuleMatchingNonterminal);
      
      if (useFixedRuleMatchingNonterminal) {
        result.add(ValidAntNodeComputerFuzzyMatchingFixedLabel
            .createValidAntNodeComputerFuzzyMatchingFixedMatchingLabel(dotNode, nonterminalIndex, rule));
      } else {
        result.add(ValidAntNodeComputer
            .createValidAntNodeComputerFuzzyMatchingNotMatchingRuleLabel(dotNode, nonterminalIndex,
                rule));
      }
    }
    return result;
  }
  
  public static List<ValidAntNodeComputer<DotNodeMultiLabel>> createValidAntNodeComputersSelectedSuperNodes(DotNodeMultiLabel dotNode, Rule rule, List<SuperNode> selectedSuperNodeNonterminals){
    List<ValidAntNodeComputer<DotNodeMultiLabel>> validAntNodeComputers = new ArrayList<ValidAntNodeComputer<DotNodeMultiLabel>>();
   
    for(int nonterminalIndex = 0; nonterminalIndex < selectedSuperNodeNonterminals.size(); nonterminalIndex++){
      validAntNodeComputers.add(ValidAntNodeComputerFuzzyMatchingFixedLabel.createValidAntNodeComputerFuzzyMatchingFixedLabelSpecificSuperNode(dotNode, nonterminalIndex, selectedSuperNodeNonterminals.get(nonterminalIndex)));
    }
    
    return validAntNodeComputers;
  }
  
  
  public static List<ValidAntNodeComputer<DotNodeMultiLabel>> createValidAntNodeComputersImprovedCubePruningFuzzyMatchingGlueRule(DotNodeMultiLabel dotNode, Rule rule, SuperNode selectedSuperNodeSecondGlueRuleNonterminal){

    List<ValidAntNodeComputer<DotNodeMultiLabel>> validAntNodeComputers = new ArrayList<ValidAntNodeComputer<DotNodeMultiLabel>>();
    validAntNodeComputers.add(ValidAntNodeComputerFuzzyMatchingFixedLabel.createValidAntNodeComputerFuzzyMatchingFixedMatchingLabel(dotNode, 0, rule));
    validAntNodeComputers.add(ValidAntNodeComputerFuzzyMatchingFixedLabel.createValidAntNodeComputerFuzzyMatchingFixedLabelSpecificSuperNode(dotNode, 1, selectedSuperNodeSecondGlueRuleNonterminal));
    return validAntNodeComputers;
  }
  
  

  
  
  /**
   * This method returns the list of alternative possible HGNode instances for a nonterminal index.
   * For normal decoding this is simply the list of HGNodes under the corresponding SuperNode for
   * the nonterminal index. If we do fuzzy matching instead, the entire list of all HGNodes from the
   * cell corresponding to the nonterminal should be retrieved.
   * 
   * @param nonterminalIndex
   * @param chart
   * @return
   */
  public abstract List<HGNode> getAlternativesListNonterminal(Chart<?, ?> chart);

  /**
   * This method returns a set of keys corresponding to acceptable label indices for the
   * nonterminal index. This is needed in fuzzy matching, because there rather than working with the
   * sorted list of HGNodes grouped by SuperNodes - which automatically guarantees acceptability -
   * we instead work with the one level higher list of all sorted HGNodes present in the cell for
   * the nonterminal at nonterminalIndex.
   * 
   * This method can then be used to check whether a specific HGNode has one of the acceptable label
   * choices for the next choice for a nonterminal, or doesn't and hence should be skipped over.
   * 
   * We use a (hash) set instead of a list, to allow efficient checking of set containment.
   * @param nonterminalIndex
   * @return
   */
  public abstract Set<Integer> getAcceptableLabelIndicesNonterminal();

  public HGNode findNextValidAntNodeAndUpdateRanks(int[] nextRanks, Chart<?, ?> chart) {
    // nextAntNodes.add(superNodes.get(x).nodes.get(nextRanks[x + 1] - 1));
    Set<Integer> acceptableLabelIndices = getAcceptableLabelIndicesNonterminal();
    // System.err.println("Gideon: acceptableLabelIndices: " + acceptableLabelIndices);

    int numAlternatives = getAlternativesListNonterminal(chart).size();
    // System.err.println("Gideon: numAlternatives: " + numAlternatives);
    // System.err.println("Gideon: nextRanks[" + (nonterminalIndex + 1) +"] : " +
    // nextRanks[nonterminalIndex+1]);

    while (nextRanks[nonterminalIndex + 1] <= numAlternatives) {
      int index = nextRanks[nonterminalIndex + 1] - 1;
      HGNode node = getAlternativesListNonterminal(chart).get(index);       
      if (acceptableLabelIndices.contains(node.lhs)) {
        //System.err.println(">>>   findNextValidAntNodeAndUpdateRanks -- using HGNode at index " + index);
        
        return node;
      } else {
        //System.err.println(">>>   findNextValidAntNodeAndUpdateRanks -- not contained in acceptable indices, skip");
        nextRanks[nonterminalIndex + 1]++;
      }
    }
    return null;
  }

  public static class ValidAntNodeComputerBasic extends ValidAntNodeComputer<DotNode> {

    protected ValidAntNodeComputerBasic(DotNode dotNode, int nonterminalIndex) {
      super(dotNode, nonterminalIndex);
    }

    @Override
    public List<HGNode> getAlternativesListNonterminal(Chart<?, ?> chart) {
      return dotNode.getAntSuperNodes().get(nonterminalIndex).nodes;
    }

    @Override
    public Set<Integer> getAcceptableLabelIndicesNonterminal() {
      return Collections.singleton(this.dotNode.getAntSuperNodes().get(nonterminalIndex).lhs);
    }
  }

  public static class ValidAntNodeComputerFuzzyMatchingFixedLabel extends
      ValidAntNodeComputer<DotNodeMultiLabel> {

    private final SuperNode matchingLabelSuperNode;

    protected ValidAntNodeComputerFuzzyMatchingFixedLabel(DotNodeMultiLabel dotNode,
        int nonterminalIndex, SuperNode matchingLabelSuperNode) {
      super(dotNode, nonterminalIndex);
      this.matchingLabelSuperNode = matchingLabelSuperNode;
    }

    public static ValidAntNodeComputerFuzzyMatchingFixedLabel createValidAntNodeComputerFuzzyMatchingFixedMatchingLabel(
        DotNodeMultiLabel dotNode, int nonterminalIndex, Rule rule) {
      //System.err.println(">>>>>>>ValidAntNodeComputerFuzzyMatchingFixedLabel.createValidAntNodeComputerFuzzyMatchingFixedLabel called");
      return new ValidAntNodeComputerFuzzyMatchingFixedLabel(dotNode, nonterminalIndex,
          getSuperNodeMatchingRuleGapLabel(rule, dotNode, nonterminalIndex));
    }
    
    public static ValidAntNodeComputerFuzzyMatchingFixedLabel createValidAntNodeComputerFuzzyMatchingFixedLabelSpecificSuperNode(
        DotNodeMultiLabel dotNode, int nonterminalIndex, SuperNode selectedSuperNode) {
      //System.err.println(">>>>>>>ValidAntNodeComputerFuzzyMatchingFixedLabel.createValidAntNodeComputerFuzzyMatchingFixedLabel called");
      return new ValidAntNodeComputerFuzzyMatchingFixedLabel(dotNode, nonterminalIndex,
          selectedSuperNode);
    }


    public static SuperNode getSuperNodeMatchingRuleGapLabel(Rule rule,
        DotNodeMultiLabel dotNodeMultiLabel, int nonterminalIndex) {
      SuperNode result = null;
      List<SuperNode> superNodeAlternativesList = dotNodeMultiLabel.getAntSuperNodes().get(
          nonterminalIndex);
      for (SuperNode superNodeAlternative : superNodeAlternativesList) {
        if (CubePruneStateFuzzyMatching.superNodeMatchesRuleNonterminal(superNodeAlternative, rule, nonterminalIndex)) {
          result = superNodeAlternative;
          return result;
        }
      }
      throw new RuntimeException(">>> ValidAntNodeComputer.getSuperNodeMatchingRuleGapLabelResult is null!!!");
    }

    @Override
    public List<HGNode> getAlternativesListNonterminal(Chart<?, ?> chart) {
      //System.err.println(">>>>ValidAntNodeComputerFuzzyMatchingFixedLabel.getAlternativesListNonterminal called ");
      return matchingLabelSuperNode.nodes;
    }

    @Override
    public Set<Integer> getAcceptableLabelIndicesNonterminal() {
      return Collections.singleton(matchingLabelSuperNode.lhs);
    }

  }

  public static class ValidAntNodeComputerFuzzyMatching extends
      ValidAntNodeComputer<DotNodeMultiLabel> {

    // We save the acceptable labels for nonterminals, so that we don't have to recompute these all
    // the time
    // We use a HashSet as opposed to generic set, to assure that the contains method has 
    // O(1) complexity.
    private final HashSet<Integer> acceptableLabelIndicesNonterminal;

    protected ValidAntNodeComputerFuzzyMatching(DotNodeMultiLabel dotNode, int nonterminalIndex,
        HashSet<Integer> acceptableLabelIndicesNonterminal) {
      super(dotNode, nonterminalIndex);
      this.acceptableLabelIndicesNonterminal = acceptableLabelIndicesNonterminal;
    }

    public static HashSet<Integer> getAcceptableLabelIndicesNonterminalNotMatchingRuleLabel(
        DotNodeMultiLabel dotNode, int nonterminalIndex, Rule rule) {
      HashSet<Integer> result = new HashSet<Integer>();
      for (SuperNode superNode : dotNode.getAntSuperNodes().get(nonterminalIndex)) {
        int key = superNode.lhs;
        if (!CubePruneStateFuzzyMatching.superNodeMatchesRuleNonterminal(superNode,
            rule, nonterminalIndex)) {
          result.add(key);
          //System.err.println(">>> adding key for word" + Vocabulary.word(key));
        }
      }
      return result;
    }

    public static HashSet<Integer> getAcceptableLabelIndicesNonterminal(DotNodeMultiLabel dotNode,
        int nonterminalIndex) {
      HashSet<Integer> result = new HashSet<Integer>();
      for (SuperNode superNode : dotNode.getAntSuperNodes().get(nonterminalIndex)) {
        result.add(superNode.lhs);
      }
      return result;
    }

    @Override
    public List<HGNode> getAlternativesListNonterminal(Chart<?, ?> chart) {
      SuperNode firstSuperNodeForIndex = dotNode.getAntSuperNodes().get(nonterminalIndex).get(0);
      HGNode firstNodeForSuperNode = firstSuperNodeForIndex.nodes.get(0);
      int i = firstNodeForSuperNode.i;
      int j = firstNodeForSuperNode.j;
      Cell cell = chart.getCell(i, j);
      List<HGNode> result = cell.getSortedNodes();
      // System.err.println(">>> Gideon: CubePruneStateFuzzyMatching.getAlternativesListNonterminal. size result: "
      // + result.size());
      return result;
    }

    @Override
    public Set<Integer> getAcceptableLabelIndicesNonterminal() {
      return this.acceptableLabelIndicesNonterminal;
    }

  }

}
