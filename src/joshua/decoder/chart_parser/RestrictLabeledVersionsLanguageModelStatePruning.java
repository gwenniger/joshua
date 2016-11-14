package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.RestrictedSizeBestHGNodeSet.NodeAdditionReport;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.hypergraph.HGNode;

public class RestrictLabeledVersionsLanguageModelStatePruning {

  private final Map<SignatureIgnoringLHS, RestrictedSizeBestHGNodeSet> signatureIgnoringLHSToHGNodeMap;
  // to maintain uniqueness of nodes
  private final Map<HGNode.Signature, HGNode> nodesSignatureMap;
  private final JoshuaConfiguration joshuaConfiguration;

  private RestrictLabeledVersionsLanguageModelStatePruning(
      Map<SignatureIgnoringLHS, RestrictedSizeBestHGNodeSet> signatureIgnoringLHSToHGNodeMap,
      Map<HGNode.Signature, HGNode> nodesSignatureMap, JoshuaConfiguration joshuaConfiguration) {
    this.signatureIgnoringLHSToHGNodeMap = signatureIgnoringLHSToHGNodeMap;
    this.nodesSignatureMap = nodesSignatureMap;
    this.joshuaConfiguration = joshuaConfiguration;
  }

  public static RestrictLabeledVersionsLanguageModelStatePruning createRestrictLabeledVersionsLanguageModelStatePruning(
      JoshuaConfiguration joshuaConfiguration) {
    Map<SignatureIgnoringLHS, RestrictedSizeBestHGNodeSet> signatureIgnoringLHSToHGNodeMap = new HashMap<>();
    Map<HGNode.Signature, HGNode> nodesSignatureTable = new HashMap<>();
    return new RestrictLabeledVersionsLanguageModelStatePruning(signatureIgnoringLHSToHGNodeMap,
        nodesSignatureTable, joshuaConfiguration);
  }

  private RestrictedSizeBestHGNodeSet getOrPutAndGetRestrictedSizeBestHGNodeSet(
      SignatureIgnoringLHS signatureIgnoringLHS) {
    if (signatureIgnoringLHSToHGNodeMap.containsKey(signatureIgnoringLHS)) {
      return signatureIgnoringLHSToHGNodeMap.get(signatureIgnoringLHS);
    } else {
      RestrictedSizeBestHGNodeSet result = RestrictedSizeBestHGNodeSet
          .createRestrictedSizeBestHGNodeSet(
              joshuaConfiguration.max_number_alternative_labeled_versions_per_language_model_state);
      signatureIgnoringLHSToHGNodeMap.put(signatureIgnoringLHS, result);
      return result;
    }
  }

  protected boolean nodeWithSameSignatureExistedBeforehand(HGNode hgNode) {
    HGNode.Signature signature = hgNode.signature();
    if (this.nodesSignatureMap.containsKey(signature)) {
      return true;
    }
    return false;
  }

  public void putHGNodeSignature(HGNode.Signature signature, HGNode hgNode) {
    this.nodesSignatureMap.put(signature, hgNode);
  }

  public void removeHGNodeWithSignature(HGNode.Signature signature) {
    this.nodesSignatureMap.remove(signature);
  }

  public HGNode getHGNodeWithSignature(HGNode.Signature signature) {
    return this.nodesSignatureMap.get(signature);
  }

  public HGNode addHGNodeAndReturnRemovedNodeIfAny(HGNode hgNode) {
    SignatureIgnoringLHS signatureIgnoringLHS = new SignatureIgnoringLHS(hgNode);
    RestrictedSizeBestHGNodeSet restrictedSizeBestHGNodeSet = getOrPutAndGetRestrictedSizeBestHGNodeSet(
        signatureIgnoringLHS);
    HGNode removedNode = restrictedSizeBestHGNodeSet.addNodeAndReturnRemovedNode(hgNode, this);

    // if (removedNode != null) {
    // System.err.println("Gideon: removed node: " + removedNode);
    // }

    return removedNode;
  }

  /**
   * Print a node addition report, to get an idea about the effect of restricting the number of
   * labeled versions per language model state
   */
  public NodeAdditionStatistics collectNodeAdditionStatistics() {
    NodeAdditionStatistics result = NodeAdditionStatistics.createNodeAdditionStatistics();

    for (Entry<SignatureIgnoringLHS, RestrictedSizeBestHGNodeSet> restrictedSizeBestHGNodeSet : this.signatureIgnoringLHSToHGNodeMap
        .entrySet()) {
      NodeAdditionReport nodeAdditionReport = restrictedSizeBestHGNodeSet.getValue()
          .getFinalNodeAdditionReport();
      result.addNodeAdditionReport(nodeAdditionReport);
    }
    return result;
  }

  /**
   * Print a node addition report, to get an idea about the effect of restricting the number of
   * labeled versions per language model state
   */
  public void showNodeAdditionReport() {
    System.err.println("\n<NodeAdditionReport>");
    System.err.println(
        "nodesAddedWithExistingSignature  , nodesAddedWithNewSignature  , finalNumberOfLabelings");

    for (Entry<SignatureIgnoringLHS, RestrictedSizeBestHGNodeSet> restrictedSizeBestHGNodeSet : this.signatureIgnoringLHSToHGNodeMap
        .entrySet()) {
      NodeAdditionReport nodeAdditionReport = restrictedSizeBestHGNodeSet.getValue()
          .getFinalNodeAdditionReport();

      System.err.println(
          nodeAdditionReport.getNodeAdditionReportString(restrictedSizeBestHGNodeSet.getKey()));
      System.err.println(nodeAdditionReport
          .getNodeAdditionReportStringDense(restrictedSizeBestHGNodeSet.getKey()));
    }
    System.err.println("</NodeAdditionReport>");
  }

  static class SignatureIgnoringLHS {

    protected final HGNode hgNode;

    private SignatureIgnoringLHS(HGNode hgNode) {
      this.hgNode = hgNode;
    }

    // Cached hash code.
    private int hash = 0;

    @Override
    public int hashCode() {
      if (hash == 0) {
        hash = 31;
        if (null != hgNode.getDPStates() && hgNode.getDPStates().size() > 0)
          for (DPState dps : hgNode.getDPStates())
            hash = hash * 19 + dps.hashCode();
      }
      return hash;
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof SignatureIgnoringLHS) {
        SignatureIgnoringLHS that = (SignatureIgnoringLHS) other;

        if ((hgNode.i != that.hgNode.i) || (hgNode.j != that.hgNode.j))
          return false;
        if (this.hgNode.getDPStates() == null)
          return (that.hgNode.getDPStates() == null);
        if (that.hgNode.getDPStates() == null)
          return false;
        if (this.hgNode.getDPStates().size() != that.hgNode.getDPStates().size())
          return false;
        for (int i = 0; i < this.hgNode.getDPStates().size(); i++) {
          if (!this.hgNode.getDPStates().get(i).equals(that.hgNode.getDPStates().get(i)))
            return false;
        }
        return true;
      }
      return false;
    }

    public String toString() {
      return String.format("%d", hashCode());
    }
  }

  protected static class NodeAdditionStatistics {
    private final List<Integer> nodesAddedWithExistingSignatureList;
    private final List<Integer> nodesAddedWithNewSignatureList;
    private final List<Integer> finalNumberOfLabelingsList;

    private NodeAdditionStatistics(List<Integer> nodesAddedWithExistingSignatureList,
        List<Integer> nodesAddedWithNewSignatureList, List<Integer> finalNumberOfLabelingsList) {
      this.nodesAddedWithExistingSignatureList = nodesAddedWithExistingSignatureList;
      this.nodesAddedWithNewSignatureList = nodesAddedWithNewSignatureList;
      this.finalNumberOfLabelingsList = finalNumberOfLabelingsList;
    }

    public static NodeAdditionStatistics createNodeAdditionStatistics() {
      return new NodeAdditionStatistics(new ArrayList<Integer>(), new ArrayList<Integer>(),
          new ArrayList<Integer>());
    }

    public List<Integer> getNodesAddedWithExistingSignatureList() {
      return this.nodesAddedWithExistingSignatureList;
    }

    public List<Integer> getNodesAddedWithNewSignatureList() {
      return this.nodesAddedWithNewSignatureList;
    }

    public List<Integer> getFinalNumberOfLabelingsList() {
      return this.finalNumberOfLabelingsList;
    }

    public void addNodeAdditionReport(NodeAdditionReport nodeAdditionReport) {
      this.nodesAddedWithExistingSignatureList
          .add(nodeAdditionReport.getNodesAddedWithExistingSignature());
      this.nodesAddedWithNewSignatureList.add(nodeAdditionReport.getNodesAddedWithNewSignature());
      this.finalNumberOfLabelingsList.add(nodeAdditionReport.getFinalNumberOfLabelings());
    }

    public void addNodeAdditionStatistics(NodeAdditionStatistics nodeAdditionStatistics) {
      this.nodesAddedWithExistingSignatureList
          .addAll(nodeAdditionStatistics.getNodesAddedWithExistingSignatureList());
      this.nodesAddedWithNewSignatureList
          .addAll(nodeAdditionStatistics.getNodesAddedWithNewSignatureList());
      this.finalNumberOfLabelingsList
          .addAll(nodeAdditionStatistics.getFinalNumberOfLabelingsList());
    }

    private String getMinMaxMeanStdString(List<Integer> integerList) {
      return "" + Collections.min(integerList) + "," + Collections.max(integerList) + ","
          + MeanStdComputation.computeIntsMean(integerList) + ","
          + MeanStdComputation.computeIntsStd(integerList) + "\n";
    }

    private String getNodesAddedWithExistingSignatureStatisticsString() {
      return "Nodes added with existing signature: "
          + getMinMaxMeanStdString(nodesAddedWithExistingSignatureList);
    }

    private String getNodesAddedWithNewSignatureStatisticsString() {
      return "Nodes added with new signature: "
          + getMinMaxMeanStdString(nodesAddedWithNewSignatureList);
    }

    private String getFinalNumberOfLabelingsStatisticsString() {
      return "Final number of labelings: " + getMinMaxMeanStdString(finalNumberOfLabelingsList);
    }

    public String getNodeAdditionStatitisticsReportString() {
      String result = "<NodeAdditionStatistics>\n";
      result += "DESCRIPTION , MIN, MAX, MEAN, STD" + "\n";
      result += getNodesAddedWithExistingSignatureStatisticsString();
      result += getNodesAddedWithNewSignatureStatisticsString();
      result += getFinalNumberOfLabelingsStatisticsString();
      result += "</NodeAdditionStatistics>\n";
      return result;
    }

  }
}
