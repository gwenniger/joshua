package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;

// ===============================================================
// CubePruneState class
// ===============================================================
public abstract class CubePruneStateBase<T extends joshua.decoder.chart_parser.DotChart.DotNodeBase<?>> implements Comparable<CubePruneStateBase<T>> {
  int[] ranks;
  ComputeNodeResult computeNodeResult;
  List<HGNode> antNodes;
  List<Rule> rules;
  protected T dotNode;
  //private DotNode dotNode;
  protected final ValidAntNodeComputer<T> validAntNodeComputer;

  public CubePruneStateBase(ComputeNodeResult score, int[] ranks, List<Rule> rules, List<HGNode> antecedents, T dotNode,
      ValidAntNodeComputer<T> validAntNodeComputer) {
    this.computeNodeResult = score;
    this.ranks = ranks;
    this.rules = rules;
    // create a new vector is critical, because currentAntecedents will change later
    this.antNodes = new ArrayList<HGNode>(antecedents);
    this.dotNode = dotNode;
    this.validAntNodeComputer = validAntNodeComputer;
  }

  /**
   * This returns the list of DP states associated with the result.
   * 
   * @return
   */
  List<DPState> getDPStates() {
    return this.computeNodeResult.getDPStates();
  }
  
  Rule getRule() {
    return this.rules.get(this.ranks[0]-1);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("STATE ||| rule=" + getRule() + " inside cost = " + computeNodeResult.getViterbiCost()
        + " estimate = " + computeNodeResult.getPruningEstimate());
    return sb.toString();
  }

  public void setDotNode(T node) {
    this.dotNode = node;
  }

  public T getDotNode() {
    return this.dotNode;
  }

  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (!this.getClass().equals(obj.getClass()))
      return false;
    @SuppressWarnings("unchecked")
    CubePruneStateBase<T> state = (CubePruneStateBase<T>) obj;
    if (state.ranks.length != ranks.length)
      return false;
    for (int i = 0; i < ranks.length; i++)
      if (state.ranks[i] != ranks[i])
        return false;
    if (getDotNode() != state.getDotNode())
      return false;

    return true;
  }

  public int hashCode() {
    int hash = (dotNode != null) ? dotNode.hashCode() : 0;
    hash += Arrays.hashCode(ranks);

    return hash;
  }

  /**
   * Compares states by ExpectedTotalLogP, allowing states to be sorted according to their inverse
   * order (high-prob first).
   */
  public int compareTo(CubePruneStateBase<T> another) {
    if (this.computeNodeResult.getPruningEstimate() < another.computeNodeResult
        .getPruningEstimate()) {
      return 1;
    } else if (this.computeNodeResult.getPruningEstimate() == another.computeNodeResult
        .getPruningEstimate()) {
      return 0;
    } else {
      return -1;
    }
  }
  
 
  public  List<HGNode> getAlternativesListNonterminal(int nonterminalIndex, Chart<?,?> chart){
    return validAntNodeComputer.getAlternativesListNonterminal(nonterminalIndex, chart);
  }
  
  public  List<Integer> getAcceptableLabelIndicesNonterminal(int nonterminalIndex){
    return validAntNodeComputer.getAcceptableLabelIndicesNonterminal(nonterminalIndex);
  }
  
  public HGNode findNextValidAntNodeAndUpdateRanks(int[] nextRanks, int nonterminalIndex, Chart<?,?> chart){
    return validAntNodeComputer.findNextValidAntNodeAndUpdateRanks(nextRanks, nonterminalIndex, chart);
  }
  
  public ValidAntNodeComputer<T> getValidAntNodeComputer(){
    return this.validAntNodeComputer;
  }
  
}