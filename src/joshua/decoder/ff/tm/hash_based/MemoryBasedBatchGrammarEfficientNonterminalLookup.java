package joshua.decoder.ff.tm.hash_based;

import java.io.IOException;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.tm.Trie;

public class MemoryBasedBatchGrammarEfficientNonterminalLookup extends MemoryBasedBatchGrammar {

  public MemoryBasedBatchGrammarEfficientNonterminalLookup(String formatKeyword,
      String grammarFile, String owner, String defaultLHSSymbol, int spanLimit,
      JoshuaConfiguration joshuaConfiguration) throws IOException {
    super(formatKeyword, grammarFile, owner, defaultLHSSymbol, spanLimit, joshuaConfiguration);
  }
  
  
  /**
   * Method that creates a new MemoryBasedTrie. This method is overwritten here with 
   * a specialization of MemoryBasedTrie : MemoryBasedTrieEfficientNonterminalLookup
   *  that is more efficient for the lookup of nonterminals by caching them
   * @return
   */
  @Override
  protected MemoryBasedTrie createNewTrie(){
    return new MemoryBasedTrieEfficientNonterminalLookup();
  }
  
  @Override
  MemoryBasedTrieEfficientNonterminalLookup castTrieObject(Trie trie){
    return (MemoryBasedTrieEfficientNonterminalLookup) trie;
  }
  
}
