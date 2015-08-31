package joshua.decoder.chart_parser;

import java.util.List;

import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.chart_parser.DotChart.DotNodeBase;
import joshua.decoder.chart_parser.DotChart.DotNodeMultiLabel;
import joshua.decoder.ff.tm.Trie;

public abstract class DotNodeTypeCreater<T extends DotNodeBase, T2> {
  protected abstract T createDotNodeType(int i, int j, Trie trieNode, List<T2> antSuperNodes,
      SourcePath srcPath);

  public static class DotNodeCreater extends DotNodeTypeCreater<DotNode, SuperNode> {

    public DotNodeCreater() {
    };

    @Override
    protected DotNode createDotNodeType(int i, int j, Trie trieNode, List<SuperNode> antSuperNodes,
        SourcePath srcPath) {
      return new DotNode(i, j, trieNode, antSuperNodes, srcPath);
    }
  }

  public static class DotNodeMultiLabelCreater extends
      DotNodeTypeCreater<DotNodeMultiLabel, List<SuperNode>> {

    public DotNodeMultiLabelCreater() {
    };

    @Override
    protected DotNodeMultiLabel createDotNodeType(int i, int j, Trie trieNode,
        List<List<SuperNode>> antSuperNodes, SourcePath srcPath) {
      return new DotNodeMultiLabel(i, j, trieNode, antSuperNodes, srcPath);
    }

  }
}
