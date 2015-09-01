package joshua.decoder.chart_parser;

import java.util.Collections;
import java.util.List;

import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.chart_parser.DotChart.DotNodeBase;
import joshua.decoder.chart_parser.DotChart.DotNodeMultiLabel;
import joshua.decoder.ff.tm.Trie;

public abstract class DotNodeTypeCreater<T extends DotNodeBase<T2>, T2> {
  protected abstract T createDotNodeType(int i, int j, Trie trieNode, List<T2> antSuperNodes,
      SourcePath srcPath);

  protected abstract T2 createSuperNodeTypeFromSingleSuperNode(SuperNode superNode);

  public static class DotNodeCreater extends DotNodeTypeCreater<DotNode, SuperNode> {

    public DotNodeCreater() {
    };

    @Override
    protected DotNode createDotNodeType(int i, int j, Trie trieNode, List<SuperNode> antSuperNodes,
        SourcePath srcPath) {
      return new DotNode(i, j, trieNode, antSuperNodes, srcPath);
    }

    @Override
    protected SuperNode createSuperNodeTypeFromSingleSuperNode(SuperNode superNode) {
      return superNode;
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

    @Override
    protected List<SuperNode> createSuperNodeTypeFromSingleSuperNode(SuperNode superNode) {
      return Collections.singletonList(superNode);
    }

  }
}
