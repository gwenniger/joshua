package joshua.decoder.ff.featureScorePrediction;

import java.util.HashMap;
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
        SparseSubstitutionDescription  substitutionPair = getSparseSubstitutionDescription (featureTypePrefix, featureString);
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

  private static  SparseSubstitutionDescription  getSparseSubstitutionDescription (String featureTypePrefix, String featureString) {
    String featureSubstring = featureString.substring(featureString.indexOf(featureTypePrefix)
        + featureTypePrefix.length());
    String[] parts = featureSubstring.split(LabelSubstitutionFF
        .getBasicLabelSubstitutionFeatureSubstitutionInfix());
    Assert.assertEquals(2, parts.length);
    // TODO fixme
    return null;
    //return new SubstitutionPair(parts[0], parts[1]);
  }

  private static class SparseSubstitutionDescription {

  }
}
