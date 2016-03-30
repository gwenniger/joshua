package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joshua.decoder.chart_parser.DotChart.DotNodeMultiLabel;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class CubePruneStateFuzzyMatching extends CubePruneStateBase<DotNodeMultiLabel>{

  public CubePruneStateFuzzyMatching(ComputeNodeResult score, int[] ranks, List<Rule> rules,
      List<HGNode> antecedents, DotNodeMultiLabel dotNode) {
    super(score, ranks, rules, antecedents, dotNode);
    assertValidAntNodes();
  }

  private void assertValidAntNodes(){
    int length = this.antNodes.size();
    
    for(int i = 0; i < length; i++){
      HGNode antNode = this.antNodes.get(i);
      int antNodeLHS = antNode.lhs;
      List<SuperNode> superNodesList = this.dotNode.getAntSuperNodes().get(i);
      Set<Integer> superNodeLHSSet = new HashSet<Integer>();
      for(SuperNode superNode : superNodesList){
        superNodeLHSSet.add(superNode.lhs);
      }
      if(!superNodeLHSSet.contains(antNodeLHS)){
        throw new RuntimeException("Error: antNode.lhs: " + antNodeLHS + " not contained in " 
            + "list of valid lhs for nonterminal: " + superNodeLHSSet);
      }
    }
  }
  
  @Override
  public  List<HGNode> getAlternativesListNonterminal(int nonterminalIndex, Chart<?,?> chart) {
    SuperNode firstSuperNodeForIndex = dotNode.getAntSuperNodes().get(nonterminalIndex).get(0);
    HGNode firstNodeForSuperNode = firstSuperNodeForIndex.nodes.get(0); 
    int i = firstNodeForSuperNode.i;
    int j = firstNodeForSuperNode.j;
    Cell cell = chart.getCell(i, j);
    List<HGNode> result = cell.getSortedNodes();
    System.err.println(">>> Gideon: CubePruneStateFuzzyMatching.getAlternativesListNonterminal. size result: " + result.size());
    return result;
  }

  @Override
  public List<Integer> getAcceptableLabelIndicesNonterminal(int nonterminalIndex) {
    List<Integer> result = new ArrayList<Integer>();
    for(SuperNode superNode : this.dotNode.getAntSuperNodes().get(nonterminalIndex)){
      result.add(superNode.lhs);
    }
    return result;
  }

}
