package joshua.decoder.chart_parser;

import java.util.Comparator;
import java.util.TreeMap;
import joshua.decoder.hypergraph.HGNode;

/**
 * Class for keeping only a limited number highest scoring HGNodes
 * 
 * @author gemaille
 *
 */
public class RestrictedSizeBestHGNodeSet {

  private static final int MAX_NUM_LABELED_VERSIONS_PER_LM_STATE = 5;
  private final TreeMap<HGNode, HGNode> hgNodes;
  private final NodeAdditionReport nodeAdditionReport;

  private RestrictedSizeBestHGNodeSet(TreeMap<HGNode, HGNode> hgNodes) {
    this.hgNodes = hgNodes;
    this.nodeAdditionReport = new NodeAdditionReport();
  }

  public static RestrictedSizeBestHGNodeSet createRestrictedSizeBestHGNodeSet() {
    return new RestrictedSizeBestHGNodeSet(new TreeMap<>(inverseLogComparator()));
  }

  /**
   * This method adds a HGNode to the sorted set, then removes the HGNode that was removed to make
   * place for it (if any) or null if none was removed
   * 
   * We pass the value of (boolean) nodeWithSameSignatureExistedBeforehand so that we don't have to
   * compute it here, as it depends on the Node Signature table present in cell, which we do not
   * want to replicate here.
   * 
   * @param hgNode
   * @return
   */
  public HGNode addNodeAndReturnRemovedNode(HGNode hgNode,
      RestrictLabeledVersionsLanguageModelStatePruning restrictLabeledVersionsLanguageModelStatePruning) {
    HGNode removedNode = null;

    if (restrictLabeledVersionsLanguageModelStatePruning
        .nodeWithSameSignatureExistedBeforehand(hgNode)) {
      // System.err.println("Add node with existing signature");
      // Add node with existing signature: in this case no node to remove needs to be returned
      // because in the cell the new node will be merged with the existing one in any case
      addNodeWithExistingSignature(hgNode, restrictLabeledVersionsLanguageModelStatePruning);
    } else {
      // System.err.println("Add node with new signature");
      // Add node with new signature, and update the signature set contained in
      // restrictLabeledVersionsLanguageModelStatePruning
      removedNode = addNodeWithNewSignatureAndReturnRemovedNode(hgNode,
          restrictLabeledVersionsLanguageModelStatePruning);
    }
    return removedNode;
  }

  /**
   * This method is used when a node with existing signature is added. In this case we must keep the
   * node with the highest score, which can be either the new node or the existing one
   * 
   * @param newHGNode
   * @return
   */
  private void addNodeWithExistingSignature(HGNode newHGNode,
      RestrictLabeledVersionsLanguageModelStatePruning restrictLabeledVersionsLanguageModelStatePruning) {
    nodeAdditionReport.incrementNodesAddedWithExistingSignature();

    HGNode existingNodeWithSameSignature = restrictLabeledVersionsLanguageModelStatePruning
        .getHGNodeWithSignature(newHGNode.signature());
    if (existingNodeWithSameSignature == null) {
      throw new RuntimeException("Error: existingNodeWithSameSignature is null!!!");
    }

    if (HGNode.logPComparator.compare(existingNodeWithSameSignature, newHGNode) < 0) {
      
      assertFirstHGNodeHasProbabilityEqualOrHigerThanLastKey(newHGNode,existingNodeWithSameSignature);
      // System.err.println("\nFound node: \n" + newHGNode
      // + "\nwith better score than existing node:\n" + existingNodeWithSameSignature + "\n");

      // The new node has a score that is better than the existing node with the same signature
      // replace it and return the removed node
      this.hgNodes.remove(existingNodeWithSameSignature);
      this.hgNodes.put(newHGNode, newHGNode);
      // put the new HGNode in the HGNode signature map in
      // restrictLabeledVersionsLanguageModelStatePruning,
      // replacing the existing entry
      restrictLabeledVersionsLanguageModelStatePruning.putHGNodeSignature(newHGNode.signature(),
          newHGNode);
    } // else {
      // The new node has a score that is worse than the existing node with the same signature:
      // nothing to be done}
      // System.err.println("\nFound node:\n" + newHGNode + "\nwith worse score than existing
      // node:\n"
      // + existingNodeWithSameSignature + "\n");
      // }
  }

  private HGNode addNodeWithNewSignatureAndReturnRemovedNode(HGNode newHGNode,
      RestrictLabeledVersionsLanguageModelStatePruning restrictLabeledVersionsLanguageModelStatePruning) {
    nodeAdditionReport.incrementNodesAddedWithNewSignature();

    HGNode removedNode = null;
    if (hgNodes.size() < MAX_NUM_LABELED_VERSIONS_PER_LM_STATE) {
      hgNodes.put(newHGNode, newHGNode);
      restrictLabeledVersionsLanguageModelStatePruning.putHGNodeSignature(newHGNode.signature(),
          newHGNode);
      return null;
    } else {
      // System.err.println(">>>Exceeded max num labeled versions per LM state: "
      // + MAX_NUM_LABELED_VERSIONS_PER_LM_STATE);
      HGNode firstHGNode = hgNodes.firstKey();
      // System.err.println(">>> First node: " + firstHGNodeSignaturePair);
      // System.err.println(">>> Second node: " + newHGNode);

      assertFirsKeyHasProbabilityEqualOrHigerThanLastKey();
      assertSignaturesAreDifferent(firstHGNode, newHGNode);

      if (HGNode.logPComparator.compare(firstHGNode, newHGNode) < 0) {
        assertFirstHGNodeHasProbabilityEqualOrHigerThanLastKey(firstHGNode, newHGNode);

        // System.err.println(">>> removing first node...");
        removedNode = hgNodes.pollFirstEntry().getKey();
        hgNodes.put(newHGNode, newHGNode);
        restrictLabeledVersionsLanguageModelStatePruning
            .removeHGNodeWithSignature(removedNode.signature());
        restrictLabeledVersionsLanguageModelStatePruning.putHGNodeSignature(newHGNode.signature(),
            newHGNode);
      } else {
        // System.err.println(">>> removing second node (added node itself)...");
        // the added node itself is not within the top MAX_NUM_LABELED_VERSIONS_PER_LM_STATE, so
        // must be removed again
        removedNode = newHGNode;
      }
      return removedNode;
    }
  }

  /**
   * Method to sanity check of the sorting order: 
   * check that the first key in the TreeSet has indeed the highest probability
   * and not the last one
   */
  private void assertFirsKeyHasProbabilityEqualOrHigerThanLastKey() {
    if (this.hgNodes.firstKey().getScore() < this.hgNodes.lastKey().getScore()) {
      throw new RuntimeException(
          "Error: Expected the first key to have highest probability, but is not true!");
    }
  }

  /**
   * Method to sanity check that we are indeed keeping the highest scoring HGNode
   * @param first
   * @param second
   */
  private void assertFirstHGNodeHasProbabilityEqualOrHigerThanLastKey(HGNode first, HGNode second) {
    if (first.getScore() < second.getScore()) {
      throw new RuntimeException(
          "Error: Expected the first HGNode to have highest probability, but is not true!");
    }
  }

  private void assertSignaturesAreDifferent(HGNode first, HGNode second) {
    if (first.signature().equals(second.signature())) {
      throw new RuntimeException(
          "Error: nodes are supposed to have different signature, but do not!!!");
    }
  }

  public NodeAdditionReport getFinalNodeAdditionReport() {
    nodeAdditionReport.setFinalNumberOfLabelings(this);
    return nodeAdditionReport;
  }

  /**
   * Comparator used to sort the HGNodes in the map by score (i.e. log probability)
   * 
   * @return
   */
  public static Comparator<HGNode> inverseLogComparator() {
    return Comparator.comparing(HGNode::getScore).reversed();
  }

  protected static class NodeAdditionReport {
    private int nodesAddedWithExistingSignature = 0;
    private int nodesAddedWithNewSignature = 0;
    private int finalNumberOfLabelings;

    public int getNodesAddedWithExistingSignature() {
      return nodesAddedWithExistingSignature;
    }

    public void incrementNodesAddedWithExistingSignature() {
      this.nodesAddedWithExistingSignature++;
    }

    public int getNodesAddedWithNewSignature() {
      return nodesAddedWithNewSignature;
    }

    public void incrementNodesAddedWithNewSignature() {
      this.nodesAddedWithNewSignature++;
    }

    protected void setFinalNumberOfLabelings(
        RestrictedSizeBestHGNodeSet restrictedSizeBestHGNodeSet) {
      finalNumberOfLabelings = restrictedSizeBestHGNodeSet.hgNodes.size();
    }

    public String getNodeAdditionReportString(
        RestrictLabeledVersionsLanguageModelStatePruning.SignatureIgnoringLHS signatureIgnoringLHS) {
      String result = "<NodeAdditionReportString>\n";
      result += "grouping signature:" + signatureIgnoringLHS.hgNode.toString() + "\n";
      result += "Nodes added with existing signature: " + nodesAddedWithExistingSignature + "\n";
      result += "Nodes added with new signature: " + nodesAddedWithNewSignature + "\n";
      result += "Final number of labelings: " + finalNumberOfLabelings + "\n";
      result += "</NodeAdditionReportString>\n";
      return result;
    }

    public String getNodeAdditionReportStringDense(
        RestrictLabeledVersionsLanguageModelStatePruning.SignatureIgnoringLHS signatureIgnoringLHS) {
      return nodesAddedWithExistingSignature + " , " + nodesAddedWithNewSignature + " , "
          + finalNumberOfLabelings + " , " + signatureIgnoringLHS.hgNode.toString();
    }

  }

  /*
   * protected static class HGNodeSignatureHGNodePair { private final HGNode.Signature signature;
   * private final HGNode hgNode;
   * 
   * private HGNodeSignatureHGNodePair(HGNode.Signature signature, HGNode hgNode) { this.signature =
   * signature; this.hgNode = hgNode; }
   * 
   * @Override public boolean equals(Object o) { System.err.println(
   * "HGNodeSignatureHGNodePair.equals called..."); if (o instanceof HGNodeSignatureHGNodePair) {
   * return this.signature.equals(((HGNodeSignatureHGNodePair) o).signature); } return false; }
   * 
   * @Override public int hashCode() { return signature.hashCode(); }
   * 
   * public static Comparator<HGNodeSignatureHGNodePair> inverseLogThenSignatureComparator() {
   * 
   * return Comparator.comparing(HGNodeSignatureHGNodePair::getLogProbability).reversed()
   * .thenComparing(HGNodeSignatureHGNodePair::getSignatureString); }
   * 
   * public String getSignatureString() { // System.err.println(
   * ">> HGNodeSignatureHGNodePair.getSignatureString called..."); return signature.toString(); }
   * 
   * public double getLogProbability() { return hgNode.getScore(); } }
   */
}
