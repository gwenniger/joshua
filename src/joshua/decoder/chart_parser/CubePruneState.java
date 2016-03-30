package joshua.decoder.chart_parser;

import java.util.Collections;
import java.util.List;

import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class CubePruneState extends CubePruneStateBase<DotNode>{

  public CubePruneState(ComputeNodeResult score, int[] ranks, List<Rule> rules,
      List<HGNode> antecedents, DotNode dotNode) {
    super(score, ranks, rules, antecedents, dotNode);
  }

  @Override
  public List<HGNode>  getAlternativesListNonterminal(int nonterminalIndex,Chart<?,?> chart){
    return dotNode.getAntSuperNodes().get(nonterminalIndex).nodes;    
  }

  @Override
  public List<Integer> getAcceptableLabelIndicesNonterminal(int nonterminalIndex) {
    return Collections.singletonList(this.dotNode.getAntSuperNodes().get(nonterminalIndex).lhs);
  }

}
