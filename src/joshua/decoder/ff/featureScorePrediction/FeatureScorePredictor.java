package joshua.decoder.ff.featureScorePrediction;


public interface FeatureScorePredictor {

  public float predictMaximumLabelSubstitutionFeatureScoreUnaryRule();
  public float predictMaxiLabelSubstitutionFeatureScoreBinaryRule();
  
}
