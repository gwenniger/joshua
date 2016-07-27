package joshua.decoder.ff.tm.hash_based;

import java.io.IOException;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.NonterminalMatcher;
import joshua.decoder.ff.tm.Rule;

/**
 * 
 * A MemoryBasedBatchGrammar in which the labels are removed inside the Trie.
 * By removing the labels (except Goal and OOV) which are ignored anyhow in the matching,
 * the process of finding the matching grammar Trie nodes in the dotchart becomes much 
 * faster, and no longer slows down with an increase in the number of labels.
 * This is most likely the fastest possible implementation structure, 
 * for which decoding should be approximately as fast as for Hiero itself, except for the added overhead of 
 * computing the label substitution features.
 * 
 * @author gemaille
 *
 */

public class MemoryBasedBatchGrammarLabelsOutsideTrie extends MemoryBasedBatchGrammar {

  // We obtain the Hiero label key once from the Vocabulary, and then reuse it for efficiency
  private  Integer hieroLabelKey = null;

  public MemoryBasedBatchGrammarLabelsOutsideTrie(String formatKeyword, String grammarFile,
      String owner, String defaultLHSSymbol, int spanLimit, JoshuaConfiguration joshuaConfiguration)
      throws IOException {
    super(formatKeyword, grammarFile, owner, defaultLHSSymbol, spanLimit, joshuaConfiguration);
  }

  private int getHieroLabelKey() {
    if(hieroLabelKey != null){
      return hieroLabelKey;
    }
    
    hieroLabelKey =  Vocabulary.id(JoshuaConfiguration.STANDARD_GLUE_RULE_RIGHT_HAND_SIDE_LABEL);
    return hieroLabelKey;
  }

  /**
   * This method takes in an array of integer keys representing a rule side, and returns a new rule
   * side (array of integers) wherein every nonterminal key not representing a Goal or OOV
   * nonterminal is replaced with the Hiero nonterminal 'X'.
   * 
   * @param ruleElementIndices
   * @return
   */
  private int[] getRuleSideWithLabeledNonterminalsExceptOOVAndGoalReplacedWithX(
      int[] ruleElementIndices) {
    int[] result = new int[ruleElementIndices.length];

    for (int i = 0; i < ruleElementIndices.length; i++) {
      int ruleElementIndex = ruleElementIndices[i];

      if(!Vocabulary.hasId(ruleElementIndex)){
        throw new RuntimeException("Error: Unknown rule element!!!");
      }
     // System.err.println("Gideon: >>> rule element " + i + "key: " + ruleElementIndex + " + String: "  +   Vocabulary.word(ruleElementIndex));
      
      if (NonterminalMatcher.isNonterminal(ruleElementIndex)
          && (!NonterminalMatcher.isOOVLabelOrGoalLabel(Vocabulary.word(ruleElementIndex),
              joshuaConfiguration))) {
        //System.err.println("Gideon: >>> Nonterminal: " + Vocabulary.word(ruleElementIndex));

        result[i] = getHieroLabelKey();       

      } else {
        result[i] = ruleElementIndex;
      }

    }

    return result;

  }

  private Rule getRuleWithLabelsReplacedWithHieroLabels(Rule rule) {

    //System.err.println(">>> Entered getRuleWithLabelsReplacedWithHieroLabels(Rule rule");

    Rule result = null;

    // Note that we don't have to replace the English side
    // since this side does not store actual labels for the nonterminals, just 
    // indices -1 or -2 that denote the first or second nonterminal from the source side
    result = new Rule(rule.getLHS(),
        getRuleSideWithLabeledNonterminalsExceptOOVAndGoalReplacedWithX(rule.getFrench()),
        rule.getEnglish(),
        rule.getFeatureString(), rule.getArity(), rule.getAlignmentString());
    return result;
  }

  @Override
  protected Rule getRuleForIndexingInTrie(Rule rule) {
    return getRuleWithLabelsReplacedWithHieroLabels(rule);
  }

  /**
   * Method that creates a new MemoryBasedTrie. This method is overwritten here with a
   * specialization of MemoryBasedTrie : MemoryBasedTrieEfficientNonterminalLookup that is more
   * efficient for the lookup of nonterminals by caching them
   * 
   * @return
   */
  @Override
  protected MemoryBasedTrie createNewTrie() {
    return new MemoryBasedTrieEfficientNonterminalLookup();
  }

}
