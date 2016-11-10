package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;

import joshua.decoder.chart_parser.DotChart.DotNodeBase;
import joshua.decoder.hypergraph.HGNode;

public class NextAntNodesPreparer<T extends DotNodeBase<?>> {

  private final int[] ranks;
  private final List<ValidAntNodeComputer<T>> validAntNodeComputers;
  private final Chart<?, ?> chart;

  private NextAntNodesPreparer(int[] ranks, List<ValidAntNodeComputer<T>> validAntNodeComputers,
      Chart<?, ?> chart) {
    this.ranks = ranks;
    this.validAntNodeComputers = validAntNodeComputers;
    this.chart = chart;
  }
  
  public static<T extends DotNodeBase<?>> NextAntNodesPreparer<T> createNextAntNodesPreparer(int[] ranks, List<ValidAntNodeComputer<T>> validAntNodeComputers,
      Chart<?, ?> chart){
    return new NextAntNodesPreparer<T>(ranks, validAntNodeComputers, chart);
  }

  public List<HGNode> getNextAntNodes() {
    List<HGNode> nextAntNodes = new ArrayList<HGNode>();

    for (int x = 0; x < ranks.length - 1; x++) {
      // nextAntNodes.add(superNodes.get(x).nodes.get(nextRanks[x + 1] - 1));
      HGNode nextAntNode = validAntNodeComputers.get(x).findNextValidAntNodeAndUpdateRanks(ranks,chart);

      if (nextAntNode != null) {
        nextAntNodes.add(nextAntNode);
        //System.err.println(">>Gideon: foundNextValidAntNode");
      } else {
        System.err.println(">>Gideon: failed foundNextValidAntNode");
        return null;
      }
    }
    return nextAntNodes;
  }

}
