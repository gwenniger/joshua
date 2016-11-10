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

  private static final int MAX_NUM_LABELED_VERSIONS_PER_LM_STATE = 50;
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
      System.err.println(">>>Exceeded max num labeled versions per LM state: " + MAX_NUM_LABELED_VERSIONS_PER_LM_STATE);
      HGNode first = hgNodes.first();
      System.err.println(">>> First node: " + first);
      System.err.println(">>> Second node: " + hgNode);
      if (HGNode.logPComparator.compare(first,hgNode) < 0) {
        System.err.println(">>> removing first node...");
        removedNode = hgNodes.pollFirst();
        hgNodes.add(hgNode);
      }else{
        System.err.println(">>> removing second node (added node itself)...");
        // the added node itself is not within the top MAX_NUM_LABELED_VERSIONS_PER_LM_STATE, so must be removed again
        removedNode = hgNode;
      }
    }
    return removedNode;
  }

}
