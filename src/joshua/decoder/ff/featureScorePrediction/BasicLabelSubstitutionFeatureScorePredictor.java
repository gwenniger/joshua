package joshua.decoder.ff.featureScorePrediction;

import java.util.HashMap;
import java.util.Map;

import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.LabelSubstitutionFF;
import joshua.decoder.ff.LabelSubstitutionFF.LabelSubstitutionLabelSmoother;
import junit.framework.Assert;

public class BasicLabelSubstitutionFeatureScorePredictor implements FeatureScorePredictor {

  private final LabelMatchingFeatureScorePredictor labelMatchingFeatureScorePredictor;
  private final String featureTypePrefix;
  private final Map<SubstitutionPair, Float> substitutionPairToScoreMap;

  private BasicLabelSubstitutionFeatureScorePredictor(
      LabelMatchingFeatureScorePredictor labelMatchingFeatureScorePredictor,
      String featureTypePrefix, Map<SubstitutionPair, Float> substitutionPairToScoreMap) {
    this.labelMatchingFeatureScorePredictor = labelMatchingFeatureScorePredictor;
    this.substitutionPairToScoreMap = substitutionPairToScoreMap;
    this.featureTypePrefix = featureTypePrefix;
  }

  public static BasicLabelSubstitutionFeatureScorePredictor createBasicLabelSubstitutionFeatureScorePredictor(
      String featureTypePrefix, FeatureVector featureVector) {
    return new BasicLabelSubstitutionFeatureScorePredictor(
        LabelMatchingFeatureScorePredictor.createLabelMatchingFeatureScorePredictor(
            featureTypePrefix, featureVector), featureTypePrefix,
        computeSubstitutionPairToScoreMap(featureTypePrefix, featureVector));
  }

  public static BasicLabelSubstitutionFeatureScorePredictor createBasicLabelSubstitutionFeatureScorePredictor(
      String labelSubstitutionRootTypeName,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother, FeatureVector featureVector) {
    String featureTypePrefix = LabelSubstitutionFF.getFeatureNamesPrefix(
        labelSubstitutionRootTypeName, labelSubstitutionLabelSmoother);
    return createBasicLabelSubstitutionFeatureScorePredictor(featureTypePrefix, featureVector);
  }

  public static BasicLabelSubstitutionFeatureScorePredictor createBasicLabelSubstitutionFeatureScorePredictorStandAlone(
      String labelSubstitutionRootTypeName, FeatureVector featureVector) {
    String featureTypePrefix = LabelSubstitutionFF.getFeatureNamesPrefix(
        labelSubstitutionRootTypeName, new LabelSubstitutionFF.NoSmoothingLabelSubstitutionLabelSmoother());
    return createBasicLabelSubstitutionFeatureScorePredictor(featureTypePrefix, featureVector);
  }
  
  private static Map<SubstitutionPair, Float> computeSubstitutionPairToScoreMap(
      String featureTypePrefix, FeatureVector featureVector) {
    Map<SubstitutionPair, Float> result = new HashMap<BasicLabelSubstitutionFeatureScorePredictor.SubstitutionPair, Float>();

    for (String featureString : featureVector.keySet()) {
      if (isRelevantFeature(featureTypePrefix, featureString)) {
        SubstitutionPair substitutionPair = getSubstitutionPair(featureTypePrefix, featureString);
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

  private static SubstitutionPair getSubstitutionPair(String featureTypePrefix, String featureString) {
    String featureSubstring = featureString.substring(featureString.indexOf(featureTypePrefix)
        + featureTypePrefix.length());
    String[] parts = featureSubstring.split(LabelSubstitutionFF
        .getBasicLabelSubstitutionFeatureSubstitutionInfix());
    Assert.assertEquals(2, parts.length);
    return new SubstitutionPair(parts[1], parts[0]);
  }

  public float predictMaximumLabelSubstitutionFeatureScoreUnaryRule() {
    // Note that initialization is done to the max of the match score and
    // nomatch score of the labelMatchingFeatureScorePredictor.
    // This corresponds to choosing a SubstitutionPair for which no (Basic) score exists,
    // but which still needs to be matching or mismatching, therefore getting the
    // score of one of these two options from labelMatchingFeatureScorePredictor.

    float result = labelMatchingFeatureScorePredictor.getMaxOfMatchAndNoMatchScore();

    for (SubstitutionPair substitutionPair : substitutionPairToScoreMap.keySet()) {
      // The score corresponds to the specific score of the substitution pair
      float candidateResult = substitutionPairToScoreMap.get(substitutionPair);
      // plus the corresponding coarse matching score for the substitution pair
      candidateResult += labelMatchingFeatureScorePredictor
          .predictLabelSubstitutionFeatureScore(substitutionPair);
      if (candidateResult > result) {
        result = candidateResult;
      }
    }
    return result;
  }

  public float predictMaxiLabelSubstitutionFeatureScoreBinaryRule() {
    return 2 * predictMaxiLabelSubstitutionFeatureScoreBinaryRule();
  }

  public float predictLabelSubstitutionFeatureScore(
      SparseSubstitutionDescription sparseSubstitutionDescription) {
    float result = 0;
    for (SubstitutionPair substitutionPair : sparseSubstitutionDescription.getSubstitutionPairs()) {
      // Because the feature representation is sparse, it is possible that a more complex sparse
      // feature involving certain substitution pairs exists, but no non-zero weight features
      // exist for some or all of the contained substitution pairs.
      // Therefore we need to check for every substitution pair if it exists, and else ignore it.
      if (substitutionPairToScoreMap.containsKey(substitutionPair)) {
        result += substitutionPairToScoreMap.get(substitutionPair);
      }
    }
    result += labelMatchingFeatureScorePredictor
        .predictLabelSubstitutionFeatureScore(sparseSubstitutionDescription);
    return result;
  }

  protected static class SubstitutionPair {
    private final String substiutedRuleLabel;
    private final String gapLabel;

    private SubstitutionPair(String substiutedRuleLabel, String gapLabel) {
      this.gapLabel = gapLabel;
      this.substiutedRuleLabel = substiutedRuleLabel;
    }

    public static SubstitutionPair createSubstitutionPair(String substiutedRuleLabel,
        String gapLabel) {
      return new SubstitutionPair(substiutedRuleLabel, gapLabel);
    }

    public String getGapLabel() {
      return gapLabel;
    }

    public String getSubstiutedRuleLabel() {
      return substiutedRuleLabel;
    }

    public boolean isMatchingSubstitution() {
      return substiutedRuleLabel.equals(gapLabel);
    }

  }

  public String getFeatureTypePrefix() {
    return featureTypePrefix;
  }

}
