package joshua.decoder.ff.featureScorePrediction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.LabelSubstitutionFF;
import junit.framework.Assert;

public class SparseLabelSubstitutionFeatureScorePredictor {
  private final String featureTypePrefix;
  private final Map<SparseSubstitutionDescription, Float> substitutionPairToScoreMap;

  private SparseLabelSubstitutionFeatureScorePredictor(String featureTypePrefix,
      Map<SparseSubstitutionDescription, Float> substitutionPairToScoreMap) {
    this.substitutionPairToScoreMap = substitutionPairToScoreMap;
    this.featureTypePrefix = featureTypePrefix;
  }

  public static SparseLabelSubstitutionFeatureScorePredictor createSparseLabelSubstitutionFeatureScorePredictor(
      String featureTypePrefix, FeatureVector featureVector) {
    return new SparseLabelSubstitutionFeatureScorePredictor(featureTypePrefix,
        computeSubstitutionPairToScoreMap(featureTypePrefix, featureVector));
  }

  private static Map<SparseSubstitutionDescription, Float> computeSubstitutionPairToScoreMap(
      String featureTypePrefix, FeatureVector featureVector) {
    Map<SparseSubstitutionDescription, Float> result = new HashMap<SparseSubstitutionDescription, Float>();

    for (String featureString : featureVector.keySet()) {
      if (isRelevantFeature(featureTypePrefix, featureString)) {
        SparseSubstitutionDescription substitutionPair = SparseSubstitutionDescription
            .creatSparseSubstitutionDescription(featureTypePrefix, featureString);
        Float score = featureVector.get(featureString);
        result.put(substitutionPair, score);
      }
    }
    return result;
  }

  private static boolean isRelevantFeature(String featureTypePrefix, String featureString) {
    if (featureString.startsWith(featureTypePrefix)) {
      return LabelSubstitutionFF.isBasicLabelSubstitutionFeatureString(featureString);
    }
    return false;
  }

  private static class SparseSubstitutionDescription {
    private final boolean ruleIsInverted;
    private final String leftHandSideLabel;
    private final List<String> ruleNonterminalLabels;
    private final List<String> substitutedToLabels;

    public SparseSubstitutionDescription(boolean ruleIsInverted, String ruleLeftHandSideLabel,
        List<String> ruleNonterminalLabels, List<String> substitutedToLabels) {
      this.ruleIsInverted = ruleIsInverted;
      this.leftHandSideLabel = ruleLeftHandSideLabel;
      this.ruleNonterminalLabels = ruleNonterminalLabels;
      this.substitutedToLabels = substitutedToLabels;
    }

    private static SparseSubstitutionDescription creatSparseSubstitutionDescription(
        String featureTypePrefix, String sparseFeatureString) {
      String featureSubstring = sparseFeatureString.substring(sparseFeatureString
          .indexOf(featureTypePrefix) + featureTypePrefix.length());
      String[] parts = featureSubstring.split(LabelSubstitutionFF
          .getBasicLabelSubstitutionFeatureSubstitutionInfix());
      Assert.assertEquals(2, parts.length);

      boolean ruleIsInverted = LabelSubstitutionFF
          .geRuleOrientationIsInvertedFromSparseFeatureString(sparseFeatureString);
      String ruleLeftHandSideLabel = LabelSubstitutionFF
          .getLeftHandSideFromSparseFeatureString(sparseFeatureString);
      List<String> ruleNonterminalLabels = LabelSubstitutionFF
          .geRuleNonterminalsFromSparseFeatureString(sparseFeatureString);
      List<String> substitutedToLabels = LabelSubstitutionFF
          .geRuleSubstitutedToLabelsFromSparseFeatureString(sparseFeatureString);

      // substitutedToLabels

      return new SparseSubstitutionDescription(ruleIsInverted, ruleLeftHandSideLabel,
          ruleNonterminalLabels, substitutedToLabels);
    }

  }
}
