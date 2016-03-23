package joshua.decoder.ff.featureScorePrediction;

import java.util.List;

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

  
  public static boolean isLabelSubstitutionFeature(String featureName){
    return isBasicLabelSubsituttionFeatureName(featureName)
        || isSparseLabelSubstitutionFeatureName(featureName);
  }
  
  /*
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
  }*/

  private static FeatureScorePredictor createBasicHeavyWeightLabelSubstitutionFeatureScorePredictorForFeatureName(
      String featureName,List<String> args, FeatureVector weights, JoshuaConfiguration joshuaConfiguration){
    if(LabelSubstitutionFF.hasDoubleLabelArgument(args)){
      return DoubleLabeledRulesLSFScorePredictor.createDoubleLabeledRulesLSFScorePredictor(
          LabelSubstitutionFF.getFeatureNameStandardFeature(), weights); 
    }  
    else{  
      return BasicLabelSubstitutionFeatureScorePredictor
          .createBasicLabelSubstitutionFeatureScorePredictorStandAlone(
              LabelSubstitutionFF.getFeatureNameStandardFeature(), weights);
    }
  }

  private static FeatureScorePredictor createSparseHeavyWeightLabelSubstitutionFeatureScorePredictorForFeatureName(
      String featureName,List<String> args, FeatureVector weights, JoshuaConfiguration joshuaConfiguration){
    if(LabelSubstitutionFF.hasDoubleLabelArgument(args)){
      return DoubleLabeledRulesLSFScorePredictor.createDoubleLabeledRulesLSFScorePredictor(
          LabelSubstitutionFF.getFeatureNameStandardSparseFeature(), weights);
    }  
    else{        
      return SparseLabelSubstitutionFeatureScorePredictor
          .createSparseLabelSubstitutionFeatureScorePredictorStandAlone(
              LabelSubstitutionFF.getFeatureNameStandardSparseFeature(), weights);
    }
  }
  
  
  private static FeatureScorePredictor createHeavyWeightLabelSubstitutionFeatureScorePredictorForFeatureName(
      String featureName,List<String> args, FeatureVector weights, JoshuaConfiguration joshuaConfiguration) {
    if (isBasicLabelSubsituttionFeatureName(featureName)) {
      return createBasicHeavyWeightLabelSubstitutionFeatureScorePredictorForFeatureName(featureName, args, weights, joshuaConfiguration);
    } else if (isSparseLabelSubstitutionFeatureName(featureName)) {
      return createSparseHeavyWeightLabelSubstitutionFeatureScorePredictorForFeatureName(featureName, args, weights, joshuaConfiguration);
    }  else {
      throw new RuntimeException("Error: unrecognized label substitution feature name");
    }
  }

  public static FeatureScorePredictor createLightWeightLabelSubstitutionFeatureScorePredictorForFeatureName(          
      String featureName, List<String> args, FeatureVector weights, JoshuaConfiguration joshuaConfiguration) {
    System.err.println("createLightWeightLabelSubstitutionFeatureScorePredictorForFeatureName - args: "+ args);
    FeatureScorePredictor heavyWeightPredictor = createHeavyWeightLabelSubstitutionFeatureScorePredictorForFeatureName(
        featureName,args, weights, joshuaConfiguration);
    System.err.println("heavyWeightPredictor: " + heavyWeightPredictor);
    return LabelSubstitutionFeatureScorePredictionMaxScoresOnly
        .createLabelSubstitutionFeatureScorePredictionMaxScoresOnly(heavyWeightPredictor);
  }

}
