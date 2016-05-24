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
  protected final List<ValidAntNodeComputer<T>> validAntNodeComputers;

  public CubePruneStateBase(ComputeNodeResult score, int[] ranks, List<Rule> rules, List<HGNode> antecedents, T dotNode,
      List<ValidAntNodeComputer<T>> validAntNodeComputers) {
    this.computeNodeResult = score;
    this.ranks = ranks;
    this.rules = rules;
    // create a new vector is critical, because currentAntecedents will change later
    this.antNodes = new ArrayList<HGNode>(antecedents);
    this.dotNode = dotNode;
    this.validAntNodeComputers = validAntNodeComputers;
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
    sb.append("STATE ||| rule=" + getRule() );
    if(computeNodeResult != null){
      sb.append(" inside cost = " + computeNodeResult.getViterbiCost() + " estimate = " + computeNodeResult.getPruningEstimate());
    }
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
    
    // For the two cube pruning states to be equal, in addition to the ranks and the DotNodes, the 
    // lists with acceptable labels from the ValidAntNodeComputers should also be equal.
    // This is necessary because in case of DotNodeMultiLabel, multiple CubePruneStates are 
    // created possibly for the same DotNodeMultiLabel, but these states are distinguished 
    // by their ValidAntNodeComputers
    for(int i = 0 ; i < validAntNodeComputers.size(); i++){
     List<Integer> acceptableLabelsThisNonterminalI =   this.validAntNodeComputers.get(i).getAcceptableLabelIndicesNonterminal();
     List<Integer> acceptableLabelsStateNonterminalI =   state.validAntNodeComputers.get(i).getAcceptableLabelIndicesNonterminal();
     if(!acceptableLabelsThisNonterminalI.equals(acceptableLabelsStateNonterminalI)){
       return false;
     }
    
    }

    return true;
  }

  public int hashCode() {
    int prime = 31;
    int hash = (dotNode != null) ? dotNode.hashCode() : 0;
    hash = hash * prime +  Arrays.hashCode(ranks);
    hash = hash * prime + hashCodeValidAntNodeComputers();
    return hash;
  }
  
  private  int hashCodeValidAntNodeComputers(){
    int prime = 31;
    int hash = 1;
    for(ValidAntNodeComputer<?>  validAntNodeComputer: validAntNodeComputers){
      hash = hash * prime + validAntNodeComputer.getAcceptableLabelIndicesNonterminal().hashCode();
    }
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
  
  /**
   * It is convenient to be able to set the node result at a later state, when we are sure the 
   * CubePruneState is not a repeat and will actually be added
   * @param computeNodeResult
   */
  public void setNodeResult(ComputeNodeResult computeNodeResult){
    this.computeNodeResult = computeNodeResult;
  }
  
 
  public  List<HGNode> getAlternativesListNonterminal(int nonterminalIndex, Chart<?,?> chart){
    return validAntNodeComputers.get(nonterminalIndex).getAlternativesListNonterminal(chart);
  }
  
  public  List<Integer> getAcceptableLabelIndicesNonterminal(int nonterminalIndex){
    return validAntNodeComputers.get(nonterminalIndex).getAcceptableLabelIndicesNonterminal();
  }
  
  public List<ValidAntNodeComputer<T>> getValidAntNodeComputers(){
    return this.validAntNodeComputers;
  }
  
}