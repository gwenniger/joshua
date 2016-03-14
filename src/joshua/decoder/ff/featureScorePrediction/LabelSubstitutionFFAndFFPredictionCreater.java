package joshua.decoder.ff.featureScorePrediction;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.LabelSubstitutionFF;

public class LabelSubstitutionFFAndFFPredictionCreater {

  public static boolean stringsEqualIgnoreCase(String string1, String string2) {
    return ((string1.toLowerCase()).equals((string2.toLowerCase())));
  }

  private static boolean isBasicLabelSubsituttionFeatureName(String featureName) {
    return stringsEqualIgnoreCase(featureName, LabelSubstitutionFF.getFeatureNameStandardFeature());
  }

  public static boolean isSparseLabelSubstitutionFeatureName(String featureName) {
    return stringsEqualIgnoreCase(featureName,
        LabelSubstitutionFF.getFeatureNameStandardSparseFeature());
  }

  public static boolean isBasicDoubleLabelFeatureName(String featureName) {
    return stringsEqualIgnoreCase(featureName,
        LabelSubstitutionFF.getFeatureNameDoubleLabelFeature());
  }

  public static boolean isSparseDoubleLabelFeatureName(String featureName) {
    return stringsEqualIgnoreCase(featureName,
        LabelSubstitutionFF.getFeatureNameDoubleLabelSparseFeature());
  }

  public static boolean isRelevantFeatureName(String featureName) {
    return isBasicLabelSubsituttionFeatureName(featureName)
        || isSparseLabelSubstitutionFeatureName(featureName)
        || isBasicDoubleLabelFeatureName(featureName)
        || isSparseDoubleLabelFeatureName(featureName);
  }

  public static LabelSubstitutionFF createLabelSubstitutionFFForFeatureName(String featureName,
      FeatureVector weights, JoshuaConfiguration joshuaConfiguration) {
    if (isBasicLabelSubsituttionFeatureName(featureName)) {
      return LabelSubstitutionFF.createStandardLabelSubstitutionFF(weights, joshuaConfiguration);
    } else if (isSparseLabelSubstitutionFeatureName(featureName)) {
      return LabelSubstitutionFF.createStandardLabelSubstitutionSparseFF(weights,
          joshuaConfiguration);
    } else if (isBasicDoubleLabelFeatureName(featureName)) {
      return LabelSubstitutionFF.createLabelSubstitutionFFDoubleLabel(weights, joshuaConfiguration);
    } else if (isSparseDoubleLabelFeatureName(featureName)) {
      return LabelSubstitutionFF.createLabelSubstitutionFFDoubleLabelSparse(weights,
          joshuaConfiguration);
    } else {
      throw new RuntimeException("Error: unrecognized label substitution feature name");
    }
  }

  private static FeatureScorePredictor createHeavyWeightLabelSubstitutionFeatureScorePredictorForFeatureName(
      String featureName, FeatureVector weights, JoshuaConfiguration joshuaConfiguration) {
    if (isBasicLabelSubsituttionFeatureName(featureName)) {
      return BasicLabelSubstitutionFeatureScorePredictor
          .createBasicLabelSubstitutionFeatureScorePredictorStandAlone(
              LabelSubstitutionFF.getFeatureNameStandardFeature(), weights);
    } else if (isSparseLabelSubstitutionFeatureName(featureName)) {
      return SparseLabelSubstitutionFeatureScorePredictor
          .createSparseLabelSubstitutionFeatureScorePredictorStandAlone(
              LabelSubstitutionFF.getFeatureNameStandardSparseFeature(), weights);
    } else if (isBasicDoubleLabelFeatureName(featureName)) {
      return DoubleLabeledRulesLSFScorePredictor.createDoubleLabeledRulesLSFScorePredictor(
          LabelSubstitutionFF.getFeatureNameDoubleLabelFeature(), weights);
    } else if (isSparseDoubleLabelFeatureName(featureName)) {
      return DoubleLabeledRulesLSFScorePredictor.createDoubleLabeledRulesLSFScorePredictor(
          LabelSubstitutionFF.getFeatureNameDoubleLabelSparseFeature(), weights);
    } else {
      throw new RuntimeException("Error: unrecognized label substitution feature name");
    }
  }

  public static FeatureScorePredictor createLightWeightLabelSubstitutionFeatureScorePredictorForFeatureName(
      String featureName, FeatureVector weights, JoshuaConfiguration joshuaConfiguration) {
    FeatureScorePredictor heavyWeightPredictor = createHeavyWeightLabelSubstitutionFeatureScorePredictorForFeatureName(
        featureName, weights, joshuaConfiguration);
    return LabelSubstitutionFeatureScorePredictionMaxScoresOnly
        .createLabelSubstitutionFeatureScorePredictionMaxScoresOnly(heavyWeightPredictor);
  }

}
