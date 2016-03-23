package joshua.decoder.ff.featureScorePrediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.LabelSubstitutionFF;
import joshua.decoder.ff.LabelSubstitutionFeatureStrings;

public class SparseLabelSubstitutionFeatureScorePredictor implements FeatureScorePredictor {
  private final BasicLabelSubstitutionFeatureScorePredictor basicLabelSubstitutionFeatureScorePredictor;
  private final String featureTypePrefix;
  private final Map<SparseSubstitutionDescription, Float> sparseSubstitutionDescriptionToScoreMap;

  private SparseLabelSubstitutionFeatureScorePredictor(
      BasicLabelSubstitutionFeatureScorePredictor basicLabelSubstitutionFeatureScorePredictor,
      String featureTypePrefix, Map<SparseSubstitutionDescription, Float> substitutionPairToScoreMap) {
    this.basicLabelSubstitutionFeatureScorePredictor = basicLabelSubstitutionFeatureScorePredictor;
    this.sparseSubstitutionDescriptionToScoreMap = substitutionPairToScoreMap;
    this.featureTypePrefix = featureTypePrefix;
  }

  public static SparseLabelSubstitutionFeatureScorePredictor createSparseLabelSubstitutionFeatureScorePredictor(
      String featureTypePrefix, FeatureVector featureVector) {
    return new SparseLabelSubstitutionFeatureScorePredictor(
        BasicLabelSubstitutionFeatureScorePredictor.createBasicLabelSubstitutionFeatureScorePredictor(
            featureTypePrefix, featureVector), featureTypePrefix,
        computeSubstitutionPairToScoreMap(featureTypePrefix, featureVector));
  }
  
  public static SparseLabelSubstitutionFeatureScorePredictor createSparseLabelSubstitutionFeatureScorePredictorStandAlone(
      String labelSubstitutionRootTypeName, FeatureVector featureVector) {
    System.err.println(">>> createSparseLabelSubstitutionFeatureScorePredictorStandAlone");
    String featureTypePrefix = LabelSubstitutionFF.getFeatureNamesPrefix(
        labelSubstitutionRootTypeName, new LabelSubstitutionFF.NoSmoothingLabelSubstitutionLabelSmoother());
    return createSparseLabelSubstitutionFeatureScorePredictor(featureTypePrefix, featureVector);
  }
  
  private static Map<SparseSubstitutionDescription, Float> computeSubstitutionPairToScoreMap(
      String featureTypePrefix, FeatureVector featureVector) {
    Map<SparseSubstitutionDescription, Float> result = new HashMap<SparseSubstitutionDescription, Float>();   
    
    for (String featureString : featureVector.keySet()) {
      if (isRelevantFeature(featureTypePrefix, featureString)) {
        SparseSubstitutionDescription substitutionPair = SparseSubstitutionDescription
            .creatSparseSubstitutionDescription(featureTypePrefix, featureString);
        Float score = featureVector.getWeight(featureString);
        result.put(substitutionPair, score);
      }
    }
    return result;
  }

  private static boolean isRelevantFeature(String featureTypePrefix, String featureString) {
    if (featureString.startsWith(featureTypePrefix)) {
      return LabelSubstitutionFeatureStrings.isSparseLabelSubstitutionFeatureString(featureString);
    }
    return false;
  }

  protected List<SparseSubstitutionDescription> getSparseSubstitutionDescriptionsUnaryRules() {
    List<SparseSubstitutionDescription> result = new ArrayList<SparseSubstitutionDescription>();
    for (SparseSubstitutionDescription sparseSubstitutionDescription : this.sparseSubstitutionDescriptionToScoreMap
        .keySet()) {
      if (sparseSubstitutionDescription.isUnaryRule()) {
        result.add(sparseSubstitutionDescription);
      }
    }
    return result;
  }

  protected List<SparseSubstitutionDescription> getSparseSubstitutionDescriptionsBinaryRules() {
    List<SparseSubstitutionDescription> result = new ArrayList<SparseSubstitutionDescription>();
    for (SparseSubstitutionDescription sparseSubstitutionDescription : this.sparseSubstitutionDescriptionToScoreMap
        .keySet()) {
      if (sparseSubstitutionDescription.isBinaryRule()) {
        result.add(sparseSubstitutionDescription);
      }
    }
    return result;
  }

  public float predictLabelSubstitutionFeatureScore(
      SparseSubstitutionDescription sparseSubstitutionDescription) {
    float result = 0;

    // Because the feature representation is sparse, it is possible that a more complex sparse
    // double labeled feature  exists, but no non-zero weight features
    // exist for some or all of the smoothed single label sparse substitution features.
    // Therefore we must check for the existence of the parseSubstitutionDescription
    if (this.sparseSubstitutionDescriptionToScoreMap.containsKey(sparseSubstitutionDescription)) {
      result += this.sparseSubstitutionDescriptionToScoreMap.get(sparseSubstitutionDescription);
    }
    result += basicLabelSubstitutionFeatureScorePredictor
        .predictLabelSubstitutionFeatureScore(sparseSubstitutionDescription);
    return result;
  }

  public float predictMaximumLabelSubstitutionFeatureScoreUnaryRule() {
    float result = basicLabelSubstitutionFeatureScorePredictor
        .predictMaximumLabelSubstitutionFeatureScoreUnaryRule();
    for (SparseSubstitutionDescription sparseSubstitutionDescription : getSparseSubstitutionDescriptionsUnaryRules()) {
      float candidateResult = predictLabelSubstitutionFeatureScore(sparseSubstitutionDescription);
      if (candidateResult > result) {
        result = candidateResult;
      }
    }

    return result;
  }

  public float predictMaxiLabelSubstitutionFeatureScoreBinaryRule() {
    float result = basicLabelSubstitutionFeatureScorePredictor
        .predictMaxiLabelSubstitutionFeatureScoreBinaryRule();
    for (SparseSubstitutionDescription sparseSubstitutionDescription : getSparseSubstitutionDescriptionsBinaryRules()) {
      float candidateResult = predictLabelSubstitutionFeatureScore(sparseSubstitutionDescription);
      if (candidateResult > result) {
        result = candidateResult;
      }
    }

    return result;
  }

  public String getFeatureTypePrefix() {
    return featureTypePrefix;
  }

  public String toString(){
    String result = "<SparseLabelSubstitutionFeatureScorePredictor>";
    result += "\nSparse features:\n";
    for(SparseSubstitutionDescription sparseSubstitutionDescription: sparseSubstitutionDescriptionToScoreMap.keySet()){
      float weight = sparseSubstitutionDescriptionToScoreMap.get(sparseSubstitutionDescription);
      result += "\n" + sparseSubstitutionDescription.getFeatureString(featureTypePrefix) + " weight: " + weight;
    }
    result += "\n</SparseLabelSubstitutionFeatureScorePredictor>";
    return result;
  }
  
}
