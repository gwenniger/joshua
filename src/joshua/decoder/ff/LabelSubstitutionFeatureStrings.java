package joshua.decoder.ff;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import joshua.util.ListUtil;

public class LabelSubstitutionFeatureStrings {

  private static final String MATCH_SUFFIX = "MATCH";
  private static final String NO_MATCH_SUFFIX = "NOMATCH";
  private static final String BASIC_FEATURE_SUBSTITUTES_INFIX = "_substitutes_";
  private static final String SPARSE_FEATURE_LHS_TAG = "LHS";
  private static final String MONOTONE_TAG = "MONO";
  private static final String INVERTED_TAG = "INV";
  private static final String SPARSE_FEATURE_NONTERMINALS_TAG = "Nont";
  private static final String SPARSE_FEATURE_SUBST_TAG = "Subst";

  public static String getMatchFeatureSuffix() {
    return MATCH_SUFFIX;
  }

  public static String getNoMatchFeatureSuffix() {
    return NO_MATCH_SUFFIX;
  }

  public static String getMatchFeatureSuffix(String ruleNonterminal, String substitutionNonterminal) {
    if (ruleNonterminal.equals(substitutionNonterminal)) {
      return MATCH_SUFFIX;
    } else {
      return NO_MATCH_SUFFIX;
    }
  }

  public static boolean isBasicLabelSubstitutionFeatureString(String featureString) {
    return featureString.contains(BASIC_FEATURE_SUBSTITUTES_INFIX);
  }

  public static String getBasicLabelSubstitutionFeatureSubstitutionInfix() {
    return BASIC_FEATURE_SUBSTITUTES_INFIX;
  }

  public static String getSubstitutionSuffix(String ruleLabel, String substitutionLabel) {
    return substitutionLabel + BASIC_FEATURE_SUBSTITUTES_INFIX + ruleLabel;
  }

  public static String computeLabelMatchingFeature(String featurenNamePrefix,
      String ruleNonterminalLabelString, String substitutionNonterminalLabelString) {
    String result = featurenNamePrefix;
    result += getMatchFeatureSuffix(ruleNonterminalLabelString, substitutionNonterminalLabelString);
    return result;
  }

  public static String computeLabelSubstitutionFeature(String featurenNamePrefix,
      String ruleNonterminalLabelString, String substitutionNonterminalLabelString) {
    String result = featurenNamePrefix;
    result += getSubstitutionSuffix(ruleNonterminalLabelString, substitutionNonterminalLabelString);
    return result;
  }

  private static String startTagString(String tag) {
    return "<" + tag + ">";
  }

  private static String closeTagString(String tag) {
    return "</" + tag + ">";
  }

  private static String getSparseFeaturePropertyFromFeatureString(String sparseFeatureString,
      String propertyString) {
    String startTagString = startTagString(propertyString);
    String endTagString = closeTagString(propertyString);
    int subStringStartIndex = sparseFeatureString.indexOf(startTagString) + startTagString.length();
    int subStringEndIndex = sparseFeatureString.indexOf(endTagString);

    String result = sparseFeatureString.substring(subStringStartIndex, subStringEndIndex);
    return result;
  }

  public static boolean isSparseLabelSubstitutionFeatureString(String featureString) {
    return featureString.contains(startTagString(SPARSE_FEATURE_LHS_TAG))
        && featureString.contains(closeTagString(SPARSE_FEATURE_LHS_TAG))
        && featureString.contains(startTagString(SPARSE_FEATURE_NONTERMINALS_TAG))
        && featureString.contains(closeTagString(SPARSE_FEATURE_NONTERMINALS_TAG))
        && featureString.contains(startTagString(SPARSE_FEATURE_SUBST_TAG))
        && featureString.contains(closeTagString(SPARSE_FEATURE_SUBST_TAG));
  }

  public static String getNonterminalsTagStartString() {
    return startTagString(SPARSE_FEATURE_NONTERMINALS_TAG);
  }

  public static String getNonterminalsTagEndString() {
    return closeTagString(SPARSE_FEATURE_NONTERMINALS_TAG);
  }

  public static String getSubstitutedToTagStartString() {
    return startTagString(SPARSE_FEATURE_SUBST_TAG);
  }

  public static String getSubstitutedToTagEndString() {
    return closeTagString(SPARSE_FEATURE_SUBST_TAG);
  }

  public static String getLHSTagStartString() {
    return startTagString(SPARSE_FEATURE_LHS_TAG);
  }

  public static String getLHSTagEndString() {
    return closeTagString(SPARSE_FEATURE_LHS_TAG);
  }

  public static List<String> geRuleNonterminalsFromSparseFeatureString(String sparseFeatureString) {
    String propertyString = getSparseFeaturePropertyFromFeatureString(sparseFeatureString,
        SPARSE_FEATURE_NONTERMINALS_TAG);
    String[] nonterminalsArray = propertyString.split(",");
    return Arrays.asList(nonterminalsArray);
  }

  public static List<String> geRuleSubstitutedToLabelsFromSparseFeatureString(
      String sparseFeatureString) {
    String propertyString = getSparseFeaturePropertyFromFeatureString(sparseFeatureString,
        SPARSE_FEATURE_SUBST_TAG);
    String[] nonterminalsArray = propertyString.split(",");
    return Arrays.asList(nonterminalsArray);
  }

  public static boolean geRuleOrientationIsInvertedFromSparseFeatureString(
      String sparseFeatureString) {
    String startTagString = closeTagString(SPARSE_FEATURE_NONTERMINALS_TAG) + "_";
    String endTagString = "_" + startTagString(SPARSE_FEATURE_SUBST_TAG);
    int subStringStartIndex = sparseFeatureString.indexOf(startTagString) + startTagString.length();
    int subStringEndIndex = sparseFeatureString.indexOf(endTagString);

    String orientationString = sparseFeatureString
        .substring(subStringStartIndex, subStringEndIndex);
    Assert.assertTrue(orientationString.equals(INVERTED_TAG)
        || orientationString.equals(MONOTONE_TAG));
    if (orientationString.equals(INVERTED_TAG)) {
      return true;
    }
    return false;
  }
  
  public static String getLeftHandSideFromSparseFeatureString(String sparseFeatureString) {
    return getSparseFeaturePropertyFromFeatureString(sparseFeatureString, SPARSE_FEATURE_LHS_TAG);
  }

  private static final String getRuleLabelsDescriptorString(String leftHandSideLabelString,
      List<String> labelSmoothedSourceNonterminals, boolean isInverting) {
    String result = "";

    result += getLHSTagStartString() + leftHandSideLabelString + getLHSTagEndString();
    result += "_" + getNonterminalsTagStartString();
    result += ListUtil
        .stringListStringWithoutBracketsCommaSeparated(labelSmoothedSourceNonterminals);
    result += getNonterminalsTagEndString();
    if (isInverting) {
      result += "_" + INVERTED_TAG;
    } else {
      result += "_" + MONOTONE_TAG;
    }

    return result;
  }

  private static final String getSubstitutionsDescriptorString(
      List<String> smoothedSubstitutionLabelsList) {
    String result = "_" + getSubstitutedToTagStartString();
    result += ListUtil
        .stringListStringWithoutBracketsCommaSeparated(smoothedSubstitutionLabelsList);
    result += getSubstitutedToTagEndString();
    return result;
  }

  public static String getGapLabelsForRuleIdenitySubstitutionFeature(String featurenNamePrefix,
      String leftHandSideLabelString, List<String> labelSmoothedSourceNonterminals,
      List<String> smoothedSubstitutionLabelsList, boolean isInverting) {
    String result = featurenNamePrefix;
    result += getRuleLabelsDescriptorString(leftHandSideLabelString,
        labelSmoothedSourceNonterminals, isInverting);
    result += getSubstitutionsDescriptorString(smoothedSubstitutionLabelsList);
    return result;
  }
}
