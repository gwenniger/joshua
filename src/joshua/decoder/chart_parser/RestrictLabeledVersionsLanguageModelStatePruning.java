package joshua.decoder.chart_parser;

import java.util.HashMap;
import java.util.Map;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.hypergraph.HGNode;

public class RestrictLabeledVersionsLanguageModelStatePruning {

  private final Map<SignatureIgnoringLHS, RestrictedSizeBestHGNodeSet> signatureIgnoringLHSToHGNodeMap;

  private RestrictLabeledVersionsLanguageModelStatePruning(
      Map<SignatureIgnoringLHS, RestrictedSizeBestHGNodeSet> signatureIgnoringLHSToHGNodeMap) {
    this.signatureIgnoringLHSToHGNodeMap = signatureIgnoringLHSToHGNodeMap;
  }

  public static RestrictLabeledVersionsLanguageModelStatePruning createRestrictLabeledVersionsLanguageModelStatePruning() {
    Map<SignatureIgnoringLHS, RestrictedSizeBestHGNodeSet> signatureIgnoringLHSToHGNodeMap = new HashMap<>();
    return new RestrictLabeledVersionsLanguageModelStatePruning(signatureIgnoringLHSToHGNodeMap);
  }

  private RestrictedSizeBestHGNodeSet getOrPutAndGetRestrictedSizeBestHGNodeSet(
      SignatureIgnoringLHS signatureIgnoringLHS) {
    if (signatureIgnoringLHSToHGNodeMap.containsKey(signatureIgnoringLHS)) {
      return signatureIgnoringLHSToHGNodeMap.get(signatureIgnoringLHS);
    } else {
      RestrictedSizeBestHGNodeSet result = RestrictedSizeBestHGNodeSet
          .createRestrictedSizeBestHGNodeSet();
      signatureIgnoringLHSToHGNodeMap.put(signatureIgnoringLHS, result);
      return result;
    }
  }

  public HGNode addHGNodeAndReturnRemovedNodeIfAny(HGNode hgNode) {
    SignatureIgnoringLHS signatureIgnoringLHS = new SignatureIgnoringLHS(hgNode);
    RestrictedSizeBestHGNodeSet restrictedSizeBestHGNodeSet = getOrPutAndGetRestrictedSizeBestHGNodeSet(
        signatureIgnoringLHS);
    HGNode removedNode = restrictedSizeBestHGNodeSet.addNodeAndReturnRemovedNode(hgNode);

    if (removedNode != null) {
      System.err.println("Gideon: removed node: " + removedNode);
    }

    return removedNode;
  }

  private static class SignatureIgnoringLHS {

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

}
