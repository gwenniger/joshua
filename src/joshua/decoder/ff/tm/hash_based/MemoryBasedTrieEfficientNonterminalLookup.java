package joshua.decoder.ff.tm.hash_based;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MemoryBasedTrieEfficientNonterminalLookup extends MemoryBasedTrie {

  private int[] nonterminalExtensions;

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

  private int[] computeNonterminalExtensionsArray() {
    List<Integer> resultList = new ArrayList<Integer>();
    if (this.childrenTbl != null) {
      for (Integer childIndex : this.childrenTbl.keySet()) {
        if (isNonterminalIndex(childIndex)) {
          resultList.add(childIndex);
        }
      }
    }
    return toIntArray(resultList);
  }

  private int[] getNonterminalsArray() {
    // We first try to get the nonterminal extensions if they are already computed
    if (nonterminalExtensions != null) {
      return nonterminalExtensions;
    }
    // Call the synchronized method to compute the nonterminals extensions
    return getOrComputeAndGetNonterminalsArraySynchronized();
  }

  private synchronized int[] getOrComputeAndGetNonterminalsArraySynchronized() {
    // Possibly another thread already entered the method and computed the array,
    // in which case we should not do it again
    if (nonterminalExtensions != null) {
      return nonterminalExtensions;
    }
    int[] nonterimalExtensions = computeNonterminalExtensionsArray();
    this.nonterminalExtensions = nonterimalExtensions;
    return this.nonterminalExtensions;
  }

  @Override
  public Iterator<Integer> getNonterminalExtensionIterator() {
    return new CachedNonterminalsIterator(getNonterminalsArray());
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
