package joshua.decoder.ff.featureScorePrediction;

import java.util.HashMap;
import java.util.Map;

import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.LabelSubstitutionFF;
import junit.framework.Assert;

public class BasicLabelSubstitutionFeatureScorePredictor {

  private final String featureTypePrefix;
  private final Map<SubstitutionPair, Float> substitutionPairToScoreMap;

  private BasicLabelSubstitutionFeatureScorePredictor(String featureTypePrefix,
      Map<SubstitutionPair, Float> substitutionPairToScoreMap) {
    this.substitutionPairToScoreMap = substitutionPairToScoreMap;
    this.featureTypePrefix = featureTypePrefix;
  }

  public static BasicLabelSubstitutionFeatureScorePredictor createBasicLabelSubstitutionFeatureScorePredictor(
      String featureTypePrefix, FeatureVector featureVector) {
    return new BasicLabelSubstitutionFeatureScorePredictor(featureTypePrefix,
        computeSubstitutionPairToScoreMap(featureTypePrefix, featureVector));
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
    return new SubstitutionPair(parts[0], parts[1]);
  }

  private static class SubstitutionPair {
    private final String gapLabel;
    private final String substiutedRuleLabel;

    private SubstitutionPair(String gapLabel, String substiutedRuleLabel) {
      this.gapLabel = gapLabel;
      this.substiutedRuleLabel = substiutedRuleLabel;
    }

  }

}
