package joshua.decoder.ff.featureScorePrediction;

public class LabelSubstitutionFeatureScorePredictionMaxScoresOnly implements FeatureScorePredictor {
  private final float maxUnaryRulesScore;
  private final float maxBinaryRulesScore;

  private LabelSubstitutionFeatureScorePredictionMaxScoresOnly(float maxUnaryRulesScore,
      float maxBinaryRulesScore) {
    this.maxUnaryRulesScore = maxUnaryRulesScore;
    this.maxBinaryRulesScore = maxBinaryRulesScore;
  }

  public static LabelSubstitutionFeatureScorePredictionMaxScoresOnly createLabelSubstitutionFeatureScorePredictionMaxScoresOnly(
      FeatureScorePredictor featureScorePredictor) {

    float maxUnaryRulesScore = featureScorePredictor
        .predictMaximumLabelSubstitutionFeatureScoreUnaryRule();
    float maxBinaryRulesScore = featureScorePredictor
        .predictMaximumLabelSubstitutionFeatureScoreUnaryRule();

    return new LabelSubstitutionFeatureScorePredictionMaxScoresOnly(maxUnaryRulesScore,
        maxBinaryRulesScore);
  }

  @Override
  public float predictMaximumLabelSubstitutionFeatureScoreUnaryRule() {
    return maxUnaryRulesScore;
  }

  @Override
  public float predictMaxiLabelSubstitutionFeatureScoreBinaryRule() {
    return maxBinaryRulesScore;
  }

  public String toString() {
    String result = "<FeatureScorePredictor>" + "predicted maximum score unary rule: "
        + predictMaximumLabelSubstitutionFeatureScoreUnaryRule() + "  "
        + "predicted maximum score binary rule: "
        + predictMaxiLabelSubstitutionFeatureScoreBinaryRule() + "</FeatureScorePredictor>";
    return result;
  }
}
