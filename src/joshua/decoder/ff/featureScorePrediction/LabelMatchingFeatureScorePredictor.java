package joshua.decoder.ff.featureScorePrediction;

import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.LabelSubstitutionFF;
import joshua.decoder.ff.featureScorePrediction.BasicLabelSubstitutionFeatureScorePredictor.SubstitutionPair;

public class LabelMatchingFeatureScorePredictor  {
  private final float matchScore;
  private final float noMatchScore;

  private final String featureTypePrefix;

  private LabelMatchingFeatureScorePredictor(float matchScore, float noMatchScore,
      String featureTypePrefix) {
    this.matchScore = matchScore;
    this.noMatchScore = noMatchScore;
    this.featureTypePrefix = featureTypePrefix;
  }

  public static LabelMatchingFeatureScorePredictor createLabelMatchingFeatureScorePredictor(
      String featureTypePrefix, FeatureVector featureVector) {
    return new LabelMatchingFeatureScorePredictor(getMatchScore(featureTypePrefix, featureVector),
        getNoMatchScore(featureTypePrefix, featureVector), featureTypePrefix);
  }

  private static String getMatchFeatureString(String featureTypePrefix) {
    return featureTypePrefix + LabelSubstitutionFF.getMatchFeatureSuffix();
  }

  private static String getNoMatchFeatureString(String featureTypePrefix) {
    return featureTypePrefix + LabelSubstitutionFF.getNoMatchFeatureSuffix();
  }

  private static float getMatchScore(String featureTypePrefix, FeatureVector featureVector) {
    // It is possible that the match feature has no weight yet, and this is even certain 
    // to be the case at the first tuning iteration
    if(featureVector.hasValue(getMatchFeatureString(featureTypePrefix))){
      return featureVector.getWeight(getMatchFeatureString(featureTypePrefix));  
    }
    return 0;
    
  }

  private static float getNoMatchScore(String featureTypePrefix, FeatureVector featureVector) {
    // It is possible that the match feature has no weight yet, and this is even certain 
    // to be the case at the first tuning iteration
    if(featureVector.hasValue(getNoMatchFeatureString(featureTypePrefix))){
      return featureVector.getWeight(getNoMatchFeatureString(featureTypePrefix));  
    }
    return 0;
  }

  public float getNoMatchScore() {
    return noMatchScore;
  }

  public float getMatchScore() {
    return matchScore;
  }

  public String getFeatureTypePrefix() {
    return featureTypePrefix;
  }

  public float getMaxOfMatchAndNoMatchScore(){
    return Math.max(getMatchScore(), getNoMatchScore());
  }
  
  
  public float predictLabelSubstitutionFeatureScore(SubstitutionPair substitutionPair){
    if(substitutionPair.isMatchingSubstitution()){
      return getMatchScore();
    }
    return getNoMatchScore();
  }
  
  public float predictLabelSubstitutionFeatureScore(
      SparseSubstitutionDescription sparseSubstitutionDescription) {
    float result = 0;
    result += sparseSubstitutionDescription.getNumberOfMatchingSubstitutions() * getMatchScore();
    result += sparseSubstitutionDescription.getNumberOfNonMatchingSubstitutions()
        * getNoMatchScore();
    return result;
  }

}