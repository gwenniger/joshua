package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Trie;

/**
 * This abstract class and its implementations serve to refine the behavior of DotChart using
 * strategy. Basically there are different ways that nonterminals of rules can be matched, either
 * strict, or soft syntactic (nonterminals can all match each other). This interface defines a
 * method that produce matching nodes for the nonterminal level. The interface is then implemented
 * in different classes for the different types of matching (currently just strict or
 * soft-syntactic)
 * 
 * The factory method produces different flavors of NonterminalMatcher corresponding to strict
 * (basic) matching, Regular Expression matching and soft syntactic matching. Notice that regular
 * expression matching and soft constraint matching can in fact be combined, getting the 'loosest'
 * way of matching possible.
 * 
 * @author Gideon Maillette de Buy Wenniger <gemdbw AT gmail DOT com>
 * 
 */
public abstract class NonterminalMatcher {

  public static boolean isOOVLabelOrGoalLabel(String label, JoshuaConfiguration joshuaConfiguration) {
    return (label.equals(joshuaConfiguration.default_non_terminal) || label
        .equals(joshuaConfiguration.goal_symbol));
  }

  /**
   * This method returns a list of all indices corresponding to Nonterminals in the Vocabulary
   * 
   * @return
   */
  public static List<Integer> getAllNonterminalIndicesExceptForGoalAndOOV(
      JoshuaConfiguration joshuaConfiguration) {
    List<Integer> result = new ArrayList<Integer>();
    List<Integer> nonterminalIndices = Vocabulary.getNonterminalIndices();
    for (Integer nonterminalIndex : nonterminalIndices) {
      if (!isOOVLabelOrGoalLabel(Vocabulary.word(nonterminalIndex), joshuaConfiguration)) {
        result.add(nonterminalIndex);
      }
    }
    return result;
  }

  public static NonterminalMatcher createNonterminalMatcher(Logger logger,
      JoshuaConfiguration joshuaConfiguration) {
    List<Integer> allNonterminalIndicesExceptForGoalAndOOV = getAllNonterminalIndicesExceptForGoalAndOOV(joshuaConfiguration);

    if (allNonterminalIndicesExceptForGoalAndOOV.isEmpty()) {
      throw new RuntimeException(
          "Error: NonterminalMatcherFactory. createNonterminalMatcher -  empty nonterminal indices table");
    }

    if (joshuaConfiguration.fuzzy_matching) {
      return new StandardNonterminalMatcherSoftConstraints(logger, joshuaConfiguration);
    } else {
      return new StandardNonterminalMatcherStrict(logger, joshuaConfiguration);
    }
  }

  protected final Logger logger;
  protected final JoshuaConfiguration joshuaConfiguration;

  protected NonterminalMatcher(Logger logger, JoshuaConfiguration joshuaConfiguration) {
    this.logger = logger;
    this.joshuaConfiguration = joshuaConfiguration;
  }

  /**
   * This is the abstract method used to get the matching child nodes for the nonterminal level
   * 
   * @param dotNode
   * @param superNode
   * @return
   */
  public abstract List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
      SuperNode superNode);

  private static boolean isNonterminal(int wordIndex) {
    return wordIndex < 0;
  }

  private List<Trie> getNonTerminalsListFromChildren(Trie trie, int wordID) {
    HashMap<Integer, ? extends Trie> childrenTbl = trie.getChildren();
    List<Trie> trieList = new ArrayList<Trie>();

    Iterator<Integer> nonterminalIterator = trie.getNonterminalExtensionIterator();
    while (nonterminalIterator.hasNext()) {
      int nonterminalIndex = nonterminalIterator.next();
      if (!isOOVLabelOrGoalLabel(Vocabulary.word(nonterminalIndex), joshuaConfiguration)) {
        trieList.add(childrenTbl.get(nonterminalIndex));
      }
    }

    return trieList;

  }

  protected List<Trie> matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(DotNode dotNode, int wordID) {

    // logger.info("wordID: " + wordID + " Vocabulary.word(Math.abs(wordID)) "
    // + Vocabulary.word(Math.abs(wordID)));

    if (!isNonterminal(wordID)) {
      throw new RuntimeException("Error : expexted nonterminal, but did not get it "
          + "in matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(DotNode dotNode, int wordID)");
    }
    return getNonTerminalsListFromChildren(dotNode.getTrieNode(), wordID);

  }

  public static List<Trie> produceStandardMatchingChildTNodesNonterminalLevel(DotNode dotNode,
      SuperNode superNode) {
    Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
    List<Trie> child_tnodes = Arrays.asList(child_node);
    return child_tnodes;
  }

  protected abstract static class StandardNonterminalMatcher extends NonterminalMatcher {

    protected StandardNonterminalMatcher(Logger logger, JoshuaConfiguration joshuaConfiguration) {
      super(logger, joshuaConfiguration);
    }
  }

  protected static class StandardNonterminalMatcherStrict extends StandardNonterminalMatcher {

    protected StandardNonterminalMatcherStrict(Logger logger,
        JoshuaConfiguration joshuaConfiguration) {
      super(logger, joshuaConfiguration);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {
      return produceStandardMatchingChildTNodesNonterminalLevel(dotNode, superNode);
    }
  }

  protected static class StandardNonterminalMatcherSoftConstraints extends
      StandardNonterminalMatcher {

    /**
     * 
     * @param logger
     * @param joshuaConfiguration
     */
    protected StandardNonterminalMatcherSoftConstraints(Logger logger,
        JoshuaConfiguration joshuaConfiguration) {
      super(logger, joshuaConfiguration);
    }

    /**
     * This method will perform strict matching if the target node superNode is a Goal Symbol.
     * Otherwise it will call a method that produces all available substitutions that correspond to
     * Nonterminals.
     * 
     * @param dotNode
     * @param superNode
     */
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {

      // We do not allow substitution of other things for GOAL labels or OOV
      // symbols
      if (isOOVLabelOrGoalLabel(Vocabulary.word(superNode.lhs), joshuaConfiguration)) {
        // logger.info("BLAA - Vocabulary.word(superNode.lhs)" +
        // Vocabulary.word(superNode.lhs));
        Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
        // logger.info("child_node.toString()" + child_node);
        List<Trie> child_tnodes = Arrays.asList(child_node);
        return child_tnodes;
      } else {
        // logger.info("Vocabulary.word(superNode.lhs): " +
        // Vocabulary.word(superNode.lhs));
        return matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(dotNode, superNode.lhs);
      }
    }
  }
}
