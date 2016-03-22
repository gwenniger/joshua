package joshua.decoder.ff.featureScorePrediction;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.ls.LSSerializerFilter;

import joshua.decoder.ff.LabelSubstitutionFF;
import joshua.decoder.ff.LabelSubstitutionFF.FirstSublabelOnlyLabelSubstitutionLabelSmoother;
import joshua.decoder.ff.LabelSubstitutionFF.LastSublabelOnlyLabelSubstitutionLabelSmoother;
import joshua.decoder.ff.LabelSubstitutionFF.LabelSubstitutionLabelSmoother;
import joshua.decoder.ff.featureScorePrediction.BasicLabelSubstitutionFeatureScorePredictor.SubstitutionPair;
import junit.framework.Assert;

public class SparseSubstitutionDescription {
  private final boolean ruleIsInverted;
  private final String leftHandSideLabel;
  private final List<String> ruleNonterminalLabels;
  private final List<String> substitutedToLabels;

  public SparseSubstitutionDescription(boolean ruleIsInverted, String ruleLeftHandSideLabel,
      List<String> ruleNonterminalLabels, List<String> substitutedToLabels) {
    this.ruleIsInverted = ruleIsInverted;
    this.leftHandSideLabel = ruleLeftHandSideLabel;
    this.ruleNonterminalLabels = ruleNonterminalLabels;
    this.substitutedToLabels = substitutedToLabels;
    Assert.assertEquals(ruleNonterminalLabels.size(), substitutedToLabels.size());
  }

  protected static SparseSubstitutionDescription creatSparseSubstitutionDescription(
      String featureTypePrefix, String sparseFeatureString) {

    //System.err.println("sparse feature String: " + sparseFeatureString);

    //String featureSubstring = sparseFeatureString.substring(sparseFeatureString
     //   .indexOf(featureTypePrefix) + featureTypePrefix.length());
    //System.err.println("feature substring: " + featureSubstring);

    boolean ruleIsInverted = LabelSubstitutionFF
        .geRuleOrientationIsInvertedFromSparseFeatureString(sparseFeatureString);
    String ruleLeftHandSideLabel = LabelSubstitutionFF
        .getLeftHandSideFromSparseFeatureString(sparseFeatureString);
    List<String> ruleNonterminalLabels = LabelSubstitutionFF
        .geRuleNonterminalsFromSparseFeatureString(sparseFeatureString);
    List<String> substitutedToLabels = LabelSubstitutionFF
        .geRuleSubstitutedToLabelsFromSparseFeatureString(sparseFeatureString);

    // substitutedToLabels

    return new SparseSubstitutionDescription(ruleIsInverted, ruleLeftHandSideLabel,
        ruleNonterminalLabels, substitutedToLabels);
  }

  /**
   * Compute the label smoothed version of a list of labels, typically selecting only
   * the first or second half of a double label 
   * @param labelSubstitutionLabelSmoother
   * @param unsmoothedLabels
   * @return
   */
  private List<String> getLabelSmoothedLabels(
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother, List<String> unsmoothedLabels) {
    List<String> result = new ArrayList<String>();
    for (String ruleNonTerminalLabel : unsmoothedLabels) {
      result.add(labelSubstitutionLabelSmoother.getSmoothedLabelString(ruleNonTerminalLabel));
    }
    return result;
  }

  private SparseSubstitutionDescription getOneLabelOnlySparseSubstitutionDescription(
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {

    String leftHandSideLabelFirstLabelOnly = labelSubstitutionLabelSmoother
        .getSmoothedLabelString(leftHandSideLabel);

    List<String> ruleNonterminalLabelsFirstOnly = getLabelSmoothedLabels(
        labelSubstitutionLabelSmoother, this.ruleNonterminalLabels);

    List<String> ruleSubstitutedToLabelsFirstOnly = getLabelSmoothedLabels(
        labelSubstitutionLabelSmoother, this.substitutedToLabels);

    return new SparseSubstitutionDescription(ruleIsInverted, leftHandSideLabelFirstLabelOnly,
        ruleNonterminalLabelsFirstOnly, ruleSubstitutedToLabelsFirstOnly);

  }

  public SparseSubstitutionDescription getFirstLabelOnlySparseSubstitutionDescription() {
    return getOneLabelOnlySparseSubstitutionDescription(new FirstSublabelOnlyLabelSubstitutionLabelSmoother());
  }

  public SparseSubstitutionDescription getSecondLabelOnlySparseSubstitutionDescription() {
    return getOneLabelOnlySparseSubstitutionDescription(new LastSublabelOnlyLabelSubstitutionLabelSmoother());
  }

  public int getNumberOfMatchingSubstitutions() {
    int result = 0;
    for (SubstitutionPair substitutionPair : getSubstitutionPairs()) {
      if (substitutionPair.isMatchingSubstitution()) {
        result++;
      }
    }
    return result;
  }

  public int getNumberOfNonMatchingSubstitutions() {
    return ruleNonterminalLabels.size() - getNumberOfMatchingSubstitutions();
  }

  public List<SubstitutionPair> getSubstitutionPairs() {
    List<SubstitutionPair> result = new ArrayList<SubstitutionPair>();
    for (int i = 0; i < ruleNonterminalLabels.size(); i++) {
      String ruleNonterminalLabel = ruleNonterminalLabels.get(i);
      String substitutedToLabel = substitutedToLabels.get(i);
      result.add(SubstitutionPair.createSubstitutionPair(ruleNonterminalLabel, substitutedToLabel));
    }
    return result;
  }

  public boolean isUnaryRule() {
    return (this.ruleNonterminalLabels.size() == 1);
  }

  public boolean isBinaryRule() {
    return (this.ruleNonterminalLabels.size() == 2);
  }

}