package joshua.decoder.ff.featureScorePrediction;

import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.LabelSubstitutionFF;

import org.junit.Assert;

public class LabelMatchingFeatureScorePredictor {
  private final double matchScore;
  private final double noMatchScore;

  private final String featureTypePrefix;

  private LabelMatchingFeatureScorePredictor(double matchScore, double noMatchScore,
      String featureTypePrefix) {
    this.matchScore = matchScore;
    this.noMatchScore = noMatchScore;
    this.featureTypePrefix = featureTypePrefix;
  }

  public static LabelMatchingFeatureScorePredictor createLabelMatchingFeatureScorePredictor(
      String featureTypePrefix, FeatureVector featureVector) {
    return new LabelMatchingFeatureScorePredictor(
        getMatchScore(featureTypePrefix, featureVector), getNoMatchScore(featureTypePrefix,
            featureVector), featureTypePrefix);
  }

  private static String getMatchFeatureString(String featureTypePrefix) {
    return featureTypePrefix + LabelSubstitutionFF.getMatchFeatureSuffix();
  }

  private static String getNoMatchFeatureString(String featureTypePrefix) {
    return featureTypePrefix + LabelSubstitutionFF.getNoMatchFeatureSuffix();
  }

  private static double getMatchScore(String featureTypePrefix, FeatureVector featureVector) {
    Assert.assertTrue(featureVector.containsKey(getMatchFeatureString(featureTypePrefix)));
    return featureVector.get(getMatchFeatureString(featureTypePrefix));
  }

  private static double getNoMatchScore(String featureTypePrefix, FeatureVector featureVector) {
    Assert.assertTrue(featureVector.containsKey(getNoMatchFeatureString(featureTypePrefix)));
    return featureVector.get(getNoMatchFeatureString(featureTypePrefix));
  }

  public double getNoMatchScore() {
    return noMatchScore;
  }

  public double getMatchScore() {
    return matchScore;
  }

  public String getFeatureTypePrefix() {
    return featureTypePrefix;
  }

}