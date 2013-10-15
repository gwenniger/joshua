package joshua.decoder.ff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.NonterminalMatcher;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.util.ListUtil;

public abstract class LabelSubstitutionFF extends StatelessFF {
  private static final String STANDARD_LABEL_SUBSTITUTION_BASIC_FEATURE_FUNCTION_NAME = "LabelSubstitution";
  private static final String STANDARD_LABEL_SUBSTITUTION_SPARSE_FEATURE_FUNCTION_NAME = "LabelSubstitutionSparse";
  private static final String DOUBLE_LABEL_SMOOTHED_LABEL_SUBSTITUTION_BASIC_FEATURE_FUNCTION_NAME = "LabelSubstitutionDoubleLabel";
  private static final String DOUBLE_LABEL_SMOOTHED_LABEL_SUBSTITUTION_SPARSE_FEATURE_FUNCTION_NAME = "LabelSubstitutionDoubleLabelSparse";
  private static final String MATCH_SUFFIX = "MATCH";
  private static final String NO_MATCH_SUFFIX = "NOMATCH";

  private final JoshuaConfiguration joshuaConfiguration;
  private final List<LabelSubstitutionLabelSmoother> labelSmoothersList;

  private LabelSubstitutionFF(FeatureVector weights, String name,
      JoshuaConfiguration joshuaConfiguration,
      List<LabelSubstitutionLabelSmoother> labelSmoothersList) {
    super(weights, name);
    this.joshuaConfiguration = joshuaConfiguration;
    this.labelSmoothersList = labelSmoothersList;
  }

  public static LabelSubstitutionBasicFF createStandardLabelSubstitutionFF(FeatureVector weights,
      JoshuaConfiguration joshuaConfiguration) {
    return new LabelSubstitutionBasicFF(weights, getFeatureNameStandardFeature(),
        joshuaConfiguration, createNoSmoothingLabelSubstiontionSmoothersList());
  }
  
  public static LabelSubstitutionSparseFF createStandardLabelSubstitutionSparseFF(FeatureVector weights,
      JoshuaConfiguration joshuaConfiguration) {
    return new LabelSubstitutionSparseFF(weights, getFeatureNameStandardFeature(),
        joshuaConfiguration, createNoSmoothingLabelSubstiontionSmoothersList());
  }

  public static LabelSubstitutionBasicFF createLabelSubstitutionFFDoubleLabel(FeatureVector weights,
      JoshuaConfiguration joshuaConfiguration) {
    return new LabelSubstitutionBasicFF(weights, getFeatureNameDoubleLabelFeature(),
        joshuaConfiguration, createDoubleLabelSmoothingLabelSubstiontionSmoothersList());
  }
  
  public static LabelSubstitutionSparseFF createLabelSubstitutionFFDoubleLabelSparse(FeatureVector weights,
      JoshuaConfiguration joshuaConfiguration) {
    return new LabelSubstitutionSparseFF(weights, getFeatureNameDoubleLabelFeature(),
        joshuaConfiguration, createDoubleLabelSmoothingLabelSubstiontionSmoothersList());
  }

  public static String getFeatureNameStandardFeature() {
    return STANDARD_LABEL_SUBSTITUTION_BASIC_FEATURE_FUNCTION_NAME;
  }
  
  public static String getFeatureNameStandardSparseFeature() {
    return STANDARD_LABEL_SUBSTITUTION_SPARSE_FEATURE_FUNCTION_NAME;
  }

  public static String getFeatureNameDoubleLabelFeature() {
    return DOUBLE_LABEL_SMOOTHED_LABEL_SUBSTITUTION_BASIC_FEATURE_FUNCTION_NAME;
  }
  
  public static String getFeatureNameDoubleLabelSparseFeature() {
    return DOUBLE_LABEL_SMOOTHED_LABEL_SUBSTITUTION_SPARSE_FEATURE_FUNCTION_NAME;
  }


  private static List<LabelSubstitutionLabelSmoother> createNoSmoothingLabelSubstiontionSmoothersList() {
    List<LabelSubstitutionLabelSmoother> result = new ArrayList<LabelSubstitutionLabelSmoother>();
    result.add(new NoSmoothingLabelSubstitutionLabelSmoother());
    return result;
  }

  private static List<LabelSubstitutionLabelSmoother> createDoubleLabelSmoothingLabelSubstiontionSmoothersList() {
    List<LabelSubstitutionLabelSmoother> result = new ArrayList<LabelSubstitutionLabelSmoother>();
    result.add(new NoSmoothingLabelSubstitutionLabelSmoother());
    result.add(new FirstSublabelOnlyLabelSubstitutionLabelSmoother());
    result.add(new LastSublabelOnlyLabelSubstitutionLabelSmoother());
    return result;
  }

  public static String getMatchFeatureSuffix(String ruleNonterminal, String substitutionNonterminal) {
    if (ruleNonterminal.equals(substitutionNonterminal)) {
      return MATCH_SUFFIX;
    } else {
      return NO_MATCH_SUFFIX;
    }
  }

  public static String getSubstitutionSuffix(String ruleLabel, String substitutionLabel) {
    return substitutionLabel + "_substitutes_" + ruleLabel;
  }

  private final String computeLabelMatchingFeature(String ruleNonterminal,
      String substitutionNonterminal, LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    String result = this.name + labelSubstitutionLabelSmoother.typeOfSmoothingSuffixString() + "_";
    result += getMatchFeatureSuffix(
        labelSubstitutionLabelSmoother.getSmoothedLabelString(ruleNonterminal),
        labelSubstitutionLabelSmoother.getSmoothedLabelString(substitutionNonterminal));
    return result;
  }

  private final String computeLabelSubstitutionFeature(String ruleNonterminal,
      String substitutionNonterminal, LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    String result = this.name + labelSubstitutionLabelSmoother.typeOfSmoothingSuffixString() + "_";
    result += getSubstitutionSuffix(
        labelSubstitutionLabelSmoother.getSmoothedLabelString(ruleNonterminal),
        labelSubstitutionLabelSmoother.getSmoothedLabelString(substitutionNonterminal));
    return result;
  }

  private static List<String> getLabelSmoothedLabelsList(List<String> labelsList,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    List<String> result = new ArrayList<String>();
    for (String label : labelsList) {
      result.add(labelSubstitutionLabelSmoother.getSmoothedLabelString(label));
    }
    return result;
  }

  private static final String getRuleLabelsDescriptorString(Rule rule,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    String result = "";
    String leftHandSide = RulePropertiesQuerying.getLHSAsString(rule);
    List<String> ruleSourceNonterminals = RulePropertiesQuerying
        .getRuleSourceNonterminalStrings(rule);
    List<String> labelSmoothedSourceNonterminals = getLabelSmoothedLabelsList(
        ruleSourceNonterminals, labelSubstitutionLabelSmoother);

    boolean isInverting = rule.isInverting();
    result += "<LHS>" + labelSubstitutionLabelSmoother.getSmoothedLabelString(leftHandSide)
        + "</LHS>";
    result += "_<Nont>";
    result += ListUtil
        .stringListStringWithoutBracketsCommaSeparated(labelSmoothedSourceNonterminals);
    result += "</Nont>";
    if (isInverting) {
      result += "_INV";
    } else {
      result += "_MONO";
    }

    return result;
  }

  private static final String getSubstitutionsDescriptorString(List<HGNode> tailNodes,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    String result = "_<Subst>";
    List<String> substitutionNonterminals = RulePropertiesQuerying
        .getSourceNonterminalStrings(tailNodes);
    List<String> smoothedSubstitutionLabelsList = getLabelSmoothedLabelsList(
        substitutionNonterminals, labelSubstitutionLabelSmoother);
    result += ListUtil
        .stringListStringWithoutBracketsCommaSeparated(smoothedSubstitutionLabelsList);
    result += "</Subst>";
    return result;
  }

  public final String getGapLabelsForRuleIdenitySubstitutionFeature(Rule rule,
      List<HGNode> tailNodes, LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    String result = this.name + labelSubstitutionLabelSmoother.typeOfSmoothingSuffixString() + "_";
    result += getRuleLabelsDescriptorString(rule, labelSubstitutionLabelSmoother);
    result += getSubstitutionsDescriptorString(tailNodes, labelSubstitutionLabelSmoother);
    return result;
  }

  private static List<String> getAllLabelsList(List<String> ruleSourceNonterminals,
      List<String> substitutionNonterminals) {
    List<String> allLabelsList = new ArrayList<String>();
    allLabelsList.addAll(ruleSourceNonterminals);
    allLabelsList.addAll(substitutionNonterminals);
    return allLabelsList;

  }

  /**
   * This method adds the basic label substitution features: 1. Label matching features (MATCH and
   * NOMATCH) 2. Simple Label substitution features: LabelX substitutes LabelY
   * 
   * @param acc
   * @param labelSubstitutionLabelSmoother
   * @param ruleSourceNonterminals
   * @param substitutionNonterminals
   */
  protected void addBasicFeatures(Accumulator acc,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother,
      List<String> ruleSourceNonterminals, List<String> substitutionNonterminals) {
    for (int nonterinalIndex = 0; nonterinalIndex < ruleSourceNonterminals.size(); nonterinalIndex++) {
      String ruleNonterminal = ruleSourceNonterminals.get(nonterinalIndex);
      String substitutionNonterminal = substitutionNonterminals.get(nonterinalIndex);

      // We only want to add matching and substitution features for labels that are
      // actually allowed to match something apart from themselves. The GOAL and OOV
      // label which can only match themselves otherwise distort the information of the
      // "real" (i.e. free) substitutions
      if (!NonterminalMatcher.isOOVLabelOrGoalLabel(ruleNonterminal, joshuaConfiguration)) {
        acc.add(
            computeLabelMatchingFeature(ruleNonterminal, substitutionNonterminal,
                labelSubstitutionLabelSmoother), 1);
        acc.add(
            computeLabelSubstitutionFeature(ruleNonterminal, substitutionNonterminal,
                labelSubstitutionLabelSmoother), 1);
      }
    }
  }

  /**
   * This method adds a sparse label substitution feature to the accumulator. The feature consists
   * of the rule identity, as summarized by its labels and whether it is inverted or not, plus the
   * list of substituting nonterminals. This feature is considerably more sparse than the basic
   * label substitution features and should be used with caution, as it can easily lead to
   * over-fitting of the development set during tuning.
   * 
   * @param acc
   * @param labelSubstitutionLabelSmoother
   * @param rule
   * @param tailNodes
   */
  protected void addSparseFeature(Accumulator acc,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother, Rule rule,
      List<HGNode> tailNodes) {

    acc.add(
        getGapLabelsForRuleIdenitySubstitutionFeature(rule, tailNodes,
            labelSubstitutionLabelSmoother), 1);
  }

  protected abstract void addFeatures(Accumulator acc,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother,
      List<String> ruleSourceNonterminals, List<String> substitutionNonterminals, Rule rule,
      List<HGNode> tailNodes);

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {
    if (rule != null && (tailNodes != null)) {

      for (LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother : this.labelSmoothersList) {

        List<String> ruleSourceNonterminals = RulePropertiesQuerying
            .getRuleSourceNonterminalStrings(rule);
        List<String> substitutionNonterminals = RulePropertiesQuerying
            .getSourceNonterminalStrings(tailNodes);

        if (labelSubstitutionLabelSmoother.ruleHasAppropriateLabelsForSmoother(getAllLabelsList(
            ruleSourceNonterminals, substitutionNonterminals))) {
          // Assert.assertEquals(ruleSourceNonterminals.size(), substitutionNonterminals.size());

          // Add the features. Depending on the implementation these are only the basic features 
          // or the basic features and sparse feature
          addFeatures(acc, labelSubstitutionLabelSmoother, ruleSourceNonterminals, substitutionNonterminals, rule, tailNodes);
        }
      }
    }
    return null;
  }

  private static class LabelSubstitutionBasicFF extends LabelSubstitutionFF {
    private LabelSubstitutionBasicFF(FeatureVector weights, String name,
        JoshuaConfiguration joshuaConfiguration,
        List<LabelSubstitutionLabelSmoother> labelSmoothersList) {
      super(weights, name, joshuaConfiguration, labelSmoothersList);
    }

    protected void addFeatures(Accumulator acc,
        LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother,
        List<String> ruleSourceNonterminals, List<String> substitutionNonterminals, Rule rule,
        List<HGNode> tailNodes) {
      addBasicFeatures(acc, labelSubstitutionLabelSmoother, ruleSourceNonterminals,
          substitutionNonterminals);
    }
  }

  private static class LabelSubstitutionSparseFF extends LabelSubstitutionFF {
    private LabelSubstitutionSparseFF(FeatureVector weights, String name,
        JoshuaConfiguration joshuaConfiguration,
        List<LabelSubstitutionLabelSmoother> labelSmoothersList) {
      super(weights, name, joshuaConfiguration, labelSmoothersList);
    }

    protected void addFeatures(Accumulator acc,
        LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother,
        List<String> ruleSourceNonterminals, List<String> substitutionNonterminals, Rule rule,
        List<HGNode> tailNodes) {
      addBasicFeatures(acc, labelSubstitutionLabelSmoother, ruleSourceNonterminals,
          substitutionNonterminals);
      addSparseFeature(acc, labelSubstitutionLabelSmoother, rule, tailNodes);
    }
  }

  private static abstract class LabelSubstitutionLabelSmoother {
    private static final String LABEL_SEPARATOR = "<<>>";

    protected abstract String getSmoothedLabelString(String originalLabelString);

    protected abstract String typeOfSmoothingSuffixString();

    protected abstract boolean ruleHasAppropriateLabelsForSmoother(List<String> allLabelsList);

    protected List<String> getLabelParts(String originalLabelString) {
      String originalLabelStringWithoutBrackets = originalLabelString.substring(1,
          originalLabelString.length() - 1);
      return Arrays.asList(originalLabelStringWithoutBrackets.split(LABEL_SEPARATOR));
    }

    protected static String getStringWithLabelBrackets(String string) {
      return "[" + string + "]";
    }

    protected static boolean ruleLHSHasMultipleLabelParts(List<String> allLabelsList) {
      for (String label : allLabelsList) {
        if (label.contains(LABEL_SEPARATOR)) {
          return true;
        }
      }
      return false;
    }

  }

  private static class NoSmoothingLabelSubstitutionLabelSmoother extends
      LabelSubstitutionLabelSmoother {
    @Override
    protected String getSmoothedLabelString(String originalLabelString) {
      return originalLabelString;
    }

    @Override
    protected String typeOfSmoothingSuffixString() {
      return "";
    }

    @Override
    protected boolean ruleHasAppropriateLabelsForSmoother(List<String> allLabelsList) {
      return true;
    }

  }

  private static class FirstSublabelOnlyLabelSubstitutionLabelSmoother extends
      LabelSubstitutionLabelSmoother {
    @Override
    protected String getSmoothedLabelString(String originalLabelString) {
      List<String> sublabelsList = getLabelParts(originalLabelString);
      return getStringWithLabelBrackets(sublabelsList.get(0));
    }

    @Override
    protected String typeOfSmoothingSuffixString() {
      return "_PhraseCentric";
    }

    @Override
    protected boolean ruleHasAppropriateLabelsForSmoother(List<String> allLabelsList) {
      return ruleLHSHasMultipleLabelParts(allLabelsList);
    }
  }

  private static class LastSublabelOnlyLabelSubstitutionLabelSmoother extends
      LabelSubstitutionLabelSmoother {
    @Override
    protected String getSmoothedLabelString(String originalLabelString) {
      List<String> sublabelsList = getLabelParts(originalLabelString);
      return getStringWithLabelBrackets(sublabelsList.get(sublabelsList.size() - 1));
    }

    @Override
    protected String typeOfSmoothingSuffixString() {
      return "_ParentRelative";
    }

    @Override
    protected boolean ruleHasAppropriateLabelsForSmoother(List<String> allLabelsList) {
      return ruleLHSHasMultipleLabelParts(allLabelsList);
    }
  }

}
