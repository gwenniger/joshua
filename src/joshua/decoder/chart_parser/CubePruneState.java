package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class CubePruneState extends CubePruneStateBase<DotNode>{

  public CubePruneState(ComputeNodeResult score, int[] ranks, List<Rule> rules,
      List<HGNode> antecedents, DotNode dotNode) {
    super(score, ranks, rules, antecedents, dotNode,createValidAntNoteCompterBasic(dotNode));
  }

  public static List<ValidAntNodeComputer<DotNode>> createValidAntNoteCompterBasic(DotNode dotNode){
    List<ValidAntNodeComputer<DotNode>> result = new ArrayList<ValidAntNodeComputer<DotNode>>();
    for(int nonterminalIndex = 0; nonterminalIndex < dotNode.antSuperNodes.size();nonterminalIndex++){
      result.add(ValidAntNodeComputer.createValidAntNodeComputerBasic(dotNode,nonterminalIndex));
    }
    return result;
  }
  
}
