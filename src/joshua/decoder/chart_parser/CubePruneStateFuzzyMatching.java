package joshua.decoder.chart_parser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.DotChart.DotNodeMultiLabel;
import joshua.decoder.chart_parser.ValidAntNodeComputer.ValidAntNodeComputerFuzzyMatching;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class CubePruneStateFuzzyMatching extends CubePruneStateBase<DotNodeMultiLabel> {

  public CubePruneStateFuzzyMatching(ComputeNodeResult score, int[] ranks, List<Rule> rules,
      List<HGNode> antecedents, DotNodeMultiLabel dotNode,
      ValidAntNodeComputerFuzzyMatching validAntNodeComputerFuzzyMatching) {
    super(score, ranks, rules, antecedents, dotNode, validAntNodeComputerFuzzyMatching);
    // assertValidAntNodes();
  }

  public static CubePruneStateFuzzyMatching createCubePruneStateFuzzyMatching(
      ComputeNodeResult score, int[] ranks, List<Rule> rules, List<HGNode> antecedents,
      DotNodeMultiLabel dotNode) {
    return new CubePruneStateFuzzyMatching(score, ranks, rules, antecedents, dotNode,
        ValidAntNodeComputerFuzzyMatching.createValidAntNodeComputerFuzzyMatching(dotNode));
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
    return validAntNodeComputer.getAlternativesListNonterminal(nonterminalIndex, chart);
  }

  @Override
  public List<Integer> getAcceptableLabelIndicesNonterminal(int nonterminalIndex) {
    return validAntNodeComputer.getAcceptableLabelIndicesNonterminal(nonterminalIndex);
  }

}
