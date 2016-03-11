package joshua.decoder.ff;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

/**
 * This class stores predictions for LabelSubstitionFeatures for all possible <rule labels, gap
 * labels> combinations that are possible, given the nonterminal labels existing in the grammar.
 * 
 * @author gemaille
 * 
 */
public class LabelSubstitutionFFValuePrediction {

  // A map that stores the mapping from specific label substitution combinations to lists of feature
  // names
  // private final Map<LabelSubstitutionCombination, List<String>>
  // labelSubstitutionCombinationToFeaturesMap;

  // This map stores the mapping from LabelSubstitionCombination to score.
  // The score depends on the feature weights, therefore, this map needs to be updated whenever the
  // feature
  // weights are changed (after every iteration during normal tuning with MIRA).
  // However, this is not a problem, as the decoder is called freshly for every iteration during
  // tuning
  private final Map<LabelSubstitutionCombination, Double> labelSubstitutionCombinationToScoresMap;

  private LabelSubstitutionFFValuePrediction(
      Map<LabelSubstitutionCombination, Double> labelSubstitutionCombinationToScoresMap) {
    this.labelSubstitutionCombinationToScoresMap = labelSubstitutionCombinationToScoresMap;

  }

  public static LabelSubstitutionFFValuePrediction createLabelSubstitutionFFValuePrediction() {
    return new LabelSubstitutionFFValuePrediction(computeLabelSubstitutionCombinationScoresMap());
  }

  public static Map<LabelSubstitutionCombination, Double> computeLabelSubstitutionCombinationScoresMap() {
    Map<LabelSubstitutionCombination, Double> result = new HashMap<LabelSubstitutionFFValuePrediction.LabelSubstitutionCombination, Double>();

    // TODO implement:
    // 1: Loop over all possible LabelSubstitution Combinations

    // 2: For each LabelSubstitutionCombination, compute list of produced features (Strings)

    // 3. For each produced feature list (= list of strings) compute the resulting feature score

    // 4. Add the mapping <LabelSubstitutionCombination,score> to the result map

    return result;
  }

  /**
   * A class for characterizing a particular label substitution, which is described by only the rule
   * source nonterminals and the substitution nonterminals (labels of the gaps)
   * 
   * @author gemaille
   * 
   */
  private static class LabelSubstitutionCombination {
    final List<String> ruleSourceNonterminals;
    final List<String> substitutionNonterminals;

    private LabelSubstitutionCombination(List<String> ruleSourceNonterminals,
        List<String> substitutionNonterminals) {
      this.ruleSourceNonterminals = ruleSourceNonterminals;
      this.substitutionNonterminals = substitutionNonterminals;
    }
  }

 

}
