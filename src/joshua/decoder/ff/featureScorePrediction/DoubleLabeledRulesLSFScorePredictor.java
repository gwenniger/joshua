package joshua.decoder.ff.featureScorePrediction;

import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.LabelSubstitutionFF;
import joshua.decoder.ff.LabelSubstitutionFF.LabelSubstitutionLabelSmoother;

public class DoubleLabeledRulesLSFScorePredictor implements FeatureScorePredictor {
  private final SparseLabelSubstitutionFeatureScorePredictor firstLabelOnlyFeatureScorePredictor;
  private final SparseLabelSubstitutionFeatureScorePredictor secondLabelOnlyFeatureScorePredictor;
  private final SparseLabelSubstitutionFeatureScorePredictor bothLabelsTogetherFeatureScorePredictor;

  private DoubleLabeledRulesLSFScorePredictor(
      SparseLabelSubstitutionFeatureScorePredictor firstLabelOnlyFeatureScorePredictor,
      SparseLabelSubstitutionFeatureScorePredictor secondLabelOnlyFeatureScorePredictor,
      SparseLabelSubstitutionFeatureScorePredictor bothLabelsTogetherFeatureScorePredictor) {
    this.firstLabelOnlyFeatureScorePredictor = firstLabelOnlyFeatureScorePredictor;
    this.secondLabelOnlyFeatureScorePredictor = secondLabelOnlyFeatureScorePredictor;
    this.bothLabelsTogetherFeatureScorePredictor = bothLabelsTogetherFeatureScorePredictor;
  }

  public static SparseLabelSubstitutionFeatureScorePredictor createSparseLabelSubstitutionFeatureScorePredictor(
      String labelSubstitutionRootTypeName,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother, FeatureVector featureVector) {

    String featureTypePrefix = LabelSubstitutionFF.getFeatureNamesPrefix(
        labelSubstitutionRootTypeName, labelSubstitutionLabelSmoother);
    return SparseLabelSubstitutionFeatureScorePredictor
        .createSparseLabelSubstitutionFeatureScorePredictor(featureTypePrefix, featureVector);
  }

  public static DoubleLabeledRulesLSFScorePredictor createDoubleLabeledRulesLSFScorePredictor(
      String labelSubstitutionRootTypeName, FeatureVector featureVector) {

    SparseLabelSubstitutionFeatureScorePredictor firstLabelOnlyFeatureScorePredictor = createSparseLabelSubstitutionFeatureScorePredictor(
        labelSubstitutionRootTypeName,
        new LabelSubstitutionFF.FirstSublabelOnlyLabelSubstitutionLabelSmoother(), featureVector);
    SparseLabelSubstitutionFeatureScorePredictor secondLabelOnlyFeatureScorePredictor = createSparseLabelSubstitutionFeatureScorePredictor(
        labelSubstitutionRootTypeName,
        new LabelSubstitutionFF.LastSublabelOnlyLabelSubstitutionLabelSmoother(), featureVector);

    SparseLabelSubstitutionFeatureScorePredictor bothLabelsTogetherFeatureScorePredictor = createSparseLabelSubstitutionFeatureScorePredictor(
        labelSubstitutionRootTypeName,
        new LabelSubstitutionFF.NoSmoothingLabelSubstitutionLabelSmoother(), featureVector);

    return new DoubleLabeledRulesLSFScorePredictor(firstLabelOnlyFeatureScorePredictor,
        secondLabelOnlyFeatureScorePredictor, bothLabelsTogetherFeatureScorePredictor);

  }

  public float predictLabelSubstitutionFeatureScore(
      SparseSubstitutionDescription sparseSubstitutionDescription) {
    float result = bothLabelsTogetherFeatureScorePredictor
        .predictLabelSubstitutionFeatureScore(sparseSubstitutionDescription);

    SparseSubstitutionDescription substitutionDescriptionFirstLabelOnly = sparseSubstitutionDescription
        .getFirstLabelOnlySparseSubstitutionDescription();
    SparseSubstitutionDescription substitutionDescriptionSecondLabelOnly = sparseSubstitutionDescription
        .getSecondLabelOnlySparseSubstitutionDescription();

    // The combined score is the score for the sparse double-labeled variant plus the scores of the
    // two single-labeled variants
    result += firstLabelOnlyFeatureScorePredictor
        .predictLabelSubstitutionFeatureScore(substitutionDescriptionFirstLabelOnly);
    result += secondLabelOnlyFeatureScorePredictor
        .predictLabelSubstitutionFeatureScore(substitutionDescriptionSecondLabelOnly);
    return result;
  }

  public float predictMaximumLabelSubstitutionFeatureScoreUnaryRule() {
    float result = firstLabelOnlyFeatureScorePredictor
        .predictMaximumLabelSubstitutionFeatureScoreUnaryRule()
        + secondLabelOnlyFeatureScorePredictor
            .predictMaximumLabelSubstitutionFeatureScoreUnaryRule();

    for (SparseSubstitutionDescription sparseSubstitutionDescription : this.bothLabelsTogetherFeatureScorePredictor
        .getSparseSubstitutionDescriptionsUnaryRules()) {
      float candidateScore = predictLabelSubstitutionFeatureScore(sparseSubstitutionDescription);

      if (candidateScore > result) {
        result = candidateScore;
      }
    }
    return result;
  }

  public float predictMaxiLabelSubstitutionFeatureScoreBinaryRule() {
    float result = firstLabelOnlyFeatureScorePredictor
        .predictMaxiLabelSubstitutionFeatureScoreBinaryRule()
        + secondLabelOnlyFeatureScorePredictor
            .predictMaxiLabelSubstitutionFeatureScoreBinaryRule();

    for (SparseSubstitutionDescription sparseSubstitutionDescription : this.bothLabelsTogetherFeatureScorePredictor
        .getSparseSubstitutionDescriptionsBinaryRules()) {
      float candidateScore = predictLabelSubstitutionFeatureScore(sparseSubstitutionDescription);

      if (candidateScore > result) {
        result = candidateScore;
      }
    }
    return result;
  }

}
