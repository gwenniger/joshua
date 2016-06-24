package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class CubePruneStateFuzzyMatchingWithoutDotChart extends CubePruneStateBase<DotNode> {

  public CubePruneStateFuzzyMatchingWithoutDotChart(ComputeNodeResult score, int[] ranks,
      List<Rule> rules, List<HGNode> antecedents, DotNode dotNode,
      List<ValidAntNodeComputer<DotNode>> validAntNodeComputers) {
    super(score, ranks, rules, antecedents, dotNode, validAntNodeComputers);
  }
  
  public static CubePruneStateFuzzyMatchingWithoutDotChart createCubePruneStateFuzzyMatchingWithoutDotChart(ComputeNodeResult score, int[] ranks, List<Rule> rules,
      List<HGNode> antecedents, DotNode dotNode, Set<Integer> oovAndGoalNonterminalIndices){
    return new CubePruneStateFuzzyMatchingWithoutDotChart(score, ranks, rules, antecedents, dotNode, createValidAntNoteComptersFuzzyMatchingWithoutDotChart(dotNode, rules.get(0), oovAndGoalNonterminalIndices));
  }

  public static List<ValidAntNodeComputer<DotNode>> createValidAntNoteComptersFuzzyMatchingWithoutDotChart(
      DotNode dotNode, Rule rule, Set<Integer> oovAndGoalNonterminalIndices) {
    List<ValidAntNodeComputer<DotNode>> result = new ArrayList<ValidAntNodeComputer<DotNode>>();
    for (int nonterminalIndex = 0; nonterminalIndex < dotNode.antSuperNodes.size(); nonterminalIndex++) {
      result.add(ValidAntNodeComputer.createValidAntNodeComputerFuzzyMatchingWithoutDotChart(
          dotNode, nonterminalIndex, oovAndGoalNonterminalIndices,
          strictMatching(rule, nonterminalIndex, oovAndGoalNonterminalIndices)));
    }
    return result;
  }

  /*
   * This method determines whether strict matching is required for the given rule and nonterminal
   * index, which is the case if the rule nonterminal at the nonterminal index is an OOV or goal
   * symbol
   */
  private static boolean strictMatching(Rule rule, int nonterminalIndex,
      Set<Integer> oovAndGoalNonterminalIndices) {

    int ruleNonterminalKey = rule.getForeignNonTerminals()[nonterminalIndex];

    boolean isOOVOrGoalNonterminal = false;
    for (Integer oovOrGoalIndex : oovAndGoalNonterminalIndices) {
      if (Vocabulary.word(oovOrGoalIndex).equals(Vocabulary.word(ruleNonterminalKey))) {
        isOOVOrGoalNonterminal = true;   
        //System.err.println(">>>>>> Strict matching rule!!!");
      }
    }
    return isOOVOrGoalNonterminal;
  }

}
