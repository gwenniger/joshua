package joshua.decoder.chart_parser;

import java.util.TreeSet;
import joshua.decoder.hypergraph.HGNode;

/**
 * Class for keeping only a limited number highest scoring HGNodes
 * 
 * @author gemaille
 *
 */
public class RestrictedSizeBestHGNodeSet {

  private static final int MAX_NUM_LABELED_VERSIONS_PER_LM_STATE = 100;
  private final TreeSet<HGNode> hgNodes;

  private RestrictedSizeBestHGNodeSet(TreeSet<HGNode> hgNodes) {
    this.hgNodes = hgNodes;
  }

  public static RestrictedSizeBestHGNodeSet createRestrictedSizeBestHGNodeSet() {
    return new RestrictedSizeBestHGNodeSet(new TreeSet<>(HGNode.inverseLogPComparator));
  }

  /**
   * This method adds a HGNode to the sorted set, then removes the HGNode that was 
   * removed to make place for it (if any) or null if none was removed
   * 
   * @param hgNode
   * @return
   */
  public HGNode addNodeAndReturnRemovedNode(HGNode hgNode) {
    HGNode removedNode = null;

    if (hgNodes.size() < MAX_NUM_LABELED_VERSIONS_PER_LM_STATE) {
      hgNodes.add(hgNode);
    } else {
      HGNode first = hgNodes.first();
      if (first.compareTo(hgNode) < 0) {
        removedNode = hgNodes.pollFirst();
        hgNodes.add(hgNode);
      }
    }
    return removedNode;
  }

}
