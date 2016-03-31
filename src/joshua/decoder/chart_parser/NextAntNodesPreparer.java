package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;
import joshua.decoder.hypergraph.HGNode;

public class NextAntNodesPreparer {

  private final int[] ranks;
  private final ValidAntNodeComputer<?> validAntNodeComputer;
  private final Chart<?, ?> chart;

  private NextAntNodesPreparer(int[] ranks, ValidAntNodeComputer<?> validAntNodeComputer,
      Chart<?, ?> chart) {
    this.ranks = ranks;
    this.validAntNodeComputer = validAntNodeComputer;
    this.chart = chart;
  }
  
  public static NextAntNodesPreparer createNextAntNodesPreparer(int[] ranks, ValidAntNodeComputer<?> validAntNodeComputer,
      Chart<?, ?> chart){
    return new NextAntNodesPreparer(ranks, validAntNodeComputer, chart);
  }

  public List<HGNode> getNextAntNodes() {
    List<HGNode> nextAntNodes = new ArrayList<HGNode>();

    for (int x = 0; x < ranks.length - 1; x++) {
      // nextAntNodes.add(superNodes.get(x).nodes.get(nextRanks[x + 1] - 1));
      HGNode nextAntNode = validAntNodeComputer.findNextValidAntNodeAndUpdateRanks(ranks, x, chart);

      if (nextAntNode != null) {
        nextAntNodes.add(nextAntNode);
        //System.err.println(">>Gideon: foundNextValidAntNode");
      } else {
        //System.err.println(">>Gideon: failed foundNextValidAntNode");
        return null;
      }
    }
    return nextAntNodes;
  }

}
