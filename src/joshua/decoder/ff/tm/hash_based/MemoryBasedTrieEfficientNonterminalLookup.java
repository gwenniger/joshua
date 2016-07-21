package joshua.decoder.ff.tm.hash_based;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.NonterminalMatcher;

public class MemoryBasedTrieEfficientNonterminalLookup extends MemoryBasedTrie {

  private int[] neitherOOVNorGoalLabelNonterminalExtensions;



  public MemoryBasedTrieEfficientNonterminalLookup() {
    super();
  }

  private boolean isNonterminalIndex(int nodeIndex) {
    return nodeIndex < 0;
  }

  private static int[] toIntArray(List<Integer> list) {
    int[] ret = new int[list.size()];
    int i = 0;
    for (Integer e : list)
      ret[i++] = e.intValue();
    return ret;
  }

  private int[] computeNeitherOOVNorGoalNonterminalExtensionsArray(JoshuaConfiguration joshuaConfiguration) {
    List<Integer> resultList = new ArrayList<Integer>();
    if (this.childrenTbl != null) {
      for (Integer childIndex : this.childrenTbl.keySet()) {
        // Check that the index corresponds to a nonterminal that is neither an OOV or a Goal label
        if (isNonterminalIndex(childIndex) && (!NonterminalMatcher.isOOVLabelOrGoalLabel(Vocabulary.word(childIndex), joshuaConfiguration))) {
          resultList.add(childIndex);
        }
      }
    }
    return toIntArray(resultList);
  }

  // We need the JoshuaConfiguration to be able to decide what are OOV and Goal Nonterminals
  private int[] getNeitherOOVNorGoalNonterminalsArray(JoshuaConfiguration joshuaConfiguration) {
    // We first try to get the nonterminal extensions if they are already computed
    if (neitherOOVNorGoalLabelNonterminalExtensions != null) {
      return neitherOOVNorGoalLabelNonterminalExtensions;
    }
    // Call the synchronized method to compute the nonterminals extensions
    return getOrComputeAndGetNonterminalsArraySynchronized(joshuaConfiguration);
  }

  private synchronized int[] getOrComputeAndGetNonterminalsArraySynchronized(JoshuaConfiguration joshuaConfiguration) {
    // Possibly another thread already entered the method and computed the array,
    // in which case we should not do it again
    if (neitherOOVNorGoalLabelNonterminalExtensions != null) {
      return neitherOOVNorGoalLabelNonterminalExtensions;
    }
    int[] nonterimalExtensions = computeNeitherOOVNorGoalNonterminalExtensionsArray(joshuaConfiguration);
    this.neitherOOVNorGoalLabelNonterminalExtensions = nonterimalExtensions;
    return this.neitherOOVNorGoalLabelNonterminalExtensions;
  }

  @Override
  public Iterator<Integer> getNeitherOOVNorGoalLabelNonterminalExtensionIterator(JoshuaConfiguration joshuaConfiguration) {
    return new CachedNonterminalsIterator(getNeitherOOVNorGoalNonterminalsArray(joshuaConfiguration));
  }

  private class CachedNonterminalsIterator implements Iterator<Integer> {

    private final int[] nonterminalExtensions;
    int currentElementIndex;

    private CachedNonterminalsIterator(int[] nonterminalExtensions) {
      this.nonterminalExtensions = nonterminalExtensions;
      this.currentElementIndex = 0;
    }

    @Override
    public boolean hasNext() {
      return (currentElementIndex < nonterminalExtensions.length);
    }

    @Override
    public Integer next() {
      int result = this.nonterminalExtensions[currentElementIndex];
      currentElementIndex++;
      return result;
    }

    @Override
    public void remove() {
      throw new RuntimeException("Not implemented");
    }

  }
}
