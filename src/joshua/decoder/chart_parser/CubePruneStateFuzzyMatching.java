package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.DotChart.DotNodeMultiLabel;
import joshua.decoder.chart_parser.ValidAntNodeComputer.ValidAntNodeComputerFuzzyMatching;
import joshua.decoder.chart_parser.ValidAntNodeComputer.ValidAntNodeComputerFuzzyMatchingFixedLabel;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class CubePruneStateFuzzyMatching extends CubePruneStateBase<DotNodeMultiLabel> {

  public CubePruneStateFuzzyMatching(ComputeNodeResult score, int[] ranks, List<Rule> rules,
      List<HGNode> antecedents, DotNodeMultiLabel dotNode,
      List<ValidAntNodeComputer<DotNodeMultiLabel>> validAntNodeComputers) {
    super(score, ranks, rules, antecedents, dotNode, validAntNodeComputers);
    // assertValidAntNodes();
  }

  public static CubePruneStateFuzzyMatching createCubePruneStateFuzzyMatchingStandard(
      ComputeNodeResult score, int[] ranks, List<Rule> rules, List<HGNode> antecedents,
      DotNodeMultiLabel dotNode) {
    return new CubePruneStateFuzzyMatching(score, ranks, rules, antecedents, dotNode,
        ValidAntNodeComputer.createValidAntNodeComputersStandardFuzzyMatching(dotNode));
  }

  public static CubePruneStateFuzzyMatching createCubePruneStateFuzzyMatchingImprovedCubePruning(
      ComputeNodeResult score, int[] ranks, List<Rule> rules, List<HGNode> antecedents,
      DotNodeMultiLabel dotNode, Rule rule, List<Boolean> useFixedRuleMatchingNonterminalsFlags) {
    return new CubePruneStateFuzzyMatching(score, ranks, rules, antecedents, dotNode,
        ValidAntNodeComputer.createValidAntNodeComputersImprovedCubePruningFuzzyMatching(dotNode,
            rule, useFixedRuleMatchingNonterminalsFlags));
  }

  private static boolean hasMatchingSubstitutions(DotNodeMultiLabel dotNode, Rule rule,
      int nonterminalIndex) {
    boolean result = false;
    int ruleNonterminalKey = rule.getForeignNonTerminals()[nonterminalIndex];
    for (SuperNode superNode : dotNode.getAntSuperNodes().get(nonterminalIndex)) {
      if (superNode.lhs == ruleNonterminalKey) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasNonMatchingSubstitutions(DotNodeMultiLabel dotNode, Rule rule,
      int nonterminalIndex) {
    int numSuperNodes = dotNode.getAntSuperNodes().get(nonterminalIndex).size();
    return ((numSuperNodes > 1) || ((numSuperNodes > 0)
        && (!hasMatchingSubstitutions(dotNode, rule, nonterminalIndex))));

  }

  /**
   * Create alternatives for fixing gap labels or not. Every possible combination is created 
   * given the options for the gap labels that are possible
   * 
   * In the example case, for binary rules with labels that can be matched or not
   * (both a matching and a non-matching substitution option exists for both gap labels)
   * there are four options in total.
   * @param dotNode
   * @param rule
   * @return
   */
  public static List<List<Boolean>> createCubePruningStateCreationFixedRuleMatchingNonterminalsFlagsAlternatives(
      DotNodeMultiLabel dotNode, Rule rule) {   
    
    List<List<Boolean>> optionsForGapLabelsListsList = new ArrayList<List<Boolean>>();
    for(int nonterminalIndex = 0; nonterminalIndex < dotNode.getAntSuperNodes().size(); nonterminalIndex++){

      //System.err.println(">>>> nonterminalIndex: " +nonterminalIndex);
      List<Boolean> optionsForGapNonterminalList = new ArrayList<Boolean>();
      if(hasMatchingSubstitutions(dotNode, rule, nonterminalIndex)){
        optionsForGapNonterminalList.add(true);
      }
      if(hasNonMatchingSubstitutions(dotNode, rule, nonterminalIndex)){
        optionsForGapNonterminalList.add(false);
      }
      optionsForGapLabelsListsList.add(optionsForGapNonterminalList);
    }
    //System.err.println(">>>> optionsForGapLabelsListsList): " + optionsForGapLabelsListsList);
    // Unpack all combinations to get the final result
    List<List<Boolean>>  result =  Chart.unpackAllPossibleTCombinations(optionsForGapLabelsListsList);
    //System.err.println(">>>> result: " + result);
    return result;
    
  }

  private void assertValidAntNodes() {
    int length = this.antNodes.size();

    for (int i = 0; i < length; i++) {
      HGNode antNode = this.antNodes.get(i);
      String antNodeLHS = Vocabulary.word(antNode.lhs);
      List<SuperNode> superNodesList = this.dotNode.getAntSuperNodes().get(i);
      Set<String> superNodeLHSSet = new HashSet<String>();
      for (SuperNode superNode : superNodesList) {
        superNodeLHSSet.add(Vocabulary.word(superNode.lhs));
      }
      if (!superNodeLHSSet.contains(antNodeLHS)) {
        throw new RuntimeException("Error: antNode.lhs: " + antNodeLHS + " not contained in "
            + "list of valid lhs for nonterminal: " + superNodeLHSSet);
      }
    }
  }

  @Override
  public List<HGNode> getAlternativesListNonterminal(int nonterminalIndex, Chart<?, ?> chart) {
    return validAntNodeComputers.get(nonterminalIndex).getAlternativesListNonterminal(chart);
  }

  @Override
  public List<Integer> getAcceptableLabelIndicesNonterminal(int nonterminalIndex) {
    return validAntNodeComputers.get(nonterminalIndex).getAcceptableLabelIndicesNonterminal();
  }

}
