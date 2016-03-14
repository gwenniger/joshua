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
  }

  protected static SparseSubstitutionDescription creatSparseSubstitutionDescription(
      String featureTypePrefix, String sparseFeatureString) {
    String featureSubstring = sparseFeatureString.substring(sparseFeatureString
        .indexOf(featureTypePrefix) + featureTypePrefix.length());
    String[] parts = featureSubstring.split(LabelSubstitutionFF
        .getBasicLabelSubstitutionFeatureSubstitutionInfix());
    Assert.assertEquals(2, parts.length);

    boolean ruleIsInverted = LabelSubstitutionFF
        .geRuleOrientationIsInvertedFromSparseFeatureString(sparseFeatureString);
    String ruleLeftHandSideLabel = LabelSubstitutionFF
        .getLeftHandSideFromSparseFeatureString(sparseFeatureString);
    List<String> ruleNonterminalLabels = LabelSubstitutionFF
        .geRuleNonterminalsFromSparseFeatureString(sparseFeatureString);
    List<String> substitutedToLabels = LabelSubstitutionFF
        .geRuleSubstitutedToLabelsFromSparseFeatureString(sparseFeatureString);

    // substitutedToLabels

    Assert.assertEquals(ruleNonterminalLabels.size(), substitutedToLabels.size());

    return new SparseSubstitutionDescription(ruleIsInverted, ruleLeftHandSideLabel,
        ruleNonterminalLabels, substitutedToLabels);
  }

  private SparseSubstitutionDescription getOneLabelOnlySparseSubstitutionDescription(
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {

    String leftHandSideLabelFirstLabelOnly = labelSubstitutionLabelSmoother
        .getSmoothedLabelString(leftHandSideLabel);
    List<String> ruleNonterminalLabelsFirstOnly = new ArrayList<String>();
    for (String ruleNonTerminalLabel : this.ruleNonterminalLabels) {
      ruleNonterminalLabelsFirstOnly.add(labelSubstitutionLabelSmoother
          .getSmoothedLabelString(ruleNonTerminalLabel));
    }
    List<String> ruleSubstitutedToLabelsFirstOnly = new ArrayList<String>();

    for (String substitutedToLabel : this.substitutedToLabels) {
      ruleNonterminalLabelsFirstOnly.add(labelSubstitutionLabelSmoother
          .getSmoothedLabelString(substitutedToLabel));
    }

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