package joshua.decoder.ff;
/***
 * @author Gideon Wenniger
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.NonterminalMatcher;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

public abstract class LabelSubstitutionFF extends StatelessFF {
  private static final String STANDARD_LABEL_SUBSTITUTION_BASIC_FEATURE_FUNCTION_NAME = "LabelSubstitution";
  private static final String STANDARD_LABEL_SUBSTITUTION_SPARSE_FEATURE_FUNCTION_NAME = "LabelSubstitutionSparse";
  private static final String DOUBLE_LABEL_ARGUMENT = "DoubleLabel";
  private static final String DOUBLE_LABEL_SEPARATOR = "<<>>";

  private static final String FUZZY_MATCHING_GLUE_GRAMMAR_NONTERIMINAL = "[X]";

  private final JoshuaConfiguration joshuaConfiguration;
  protected  List<LabelSubstitutionLabelSmoother> labelSmoothersList;

  public LabelSubstitutionFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "LabelSubstitution", args, config);
    this.joshuaConfiguration = config;
    this.labelSmoothersList = createLabelSmoothersList(args);
    if(this.labelSmoothersList == null){
      throw new RuntimeException("Error: label smoothers list is null");
    }
  }

  protected LabelSubstitutionFF(FeatureVector weights, String[] args, JoshuaConfiguration config, String featureName) {
    super(weights, featureName, args, config);
    this.joshuaConfiguration = config;
    this.labelSmoothersList = createLabelSmoothersList(args);
  }
  
  
  protected static List<LabelSubstitutionLabelSmoother> createLabelSmoothersList(String[] args){
    if(hasDoubleLabelArgument(Arrays.asList(args))){        
        return  createDoubleLabelSmoothingLabelSubstiontionSmoothersList();
    }
    else{
      return createNoSmoothingLabelSubstiontionSmoothersList();
    }
  }
  
  public static boolean hasDoubleLabelArgument(List<String> args){
    // The first argument args[0] is the name of the feature itself
    if((args.size() > 1) && (args.get(1).equals(DOUBLE_LABEL_ARGUMENT))){
      return true;
    }
    return false;
  }
  
  public static LabelSubstitutionBasicFF createStandardLabelSubstitutionFF(FeatureVector weights,
      JoshuaConfiguration joshuaConfiguration) {
    return new LabelSubstitutionBasicFF(weights, getFeatureNameStandardFeature(),
        joshuaConfiguration, createNoSmoothingLabelSubstiontionSmoothersList());
  }

  public static LabelSubstitutionSparseFF createStandardLabelSubstitutionSparseFF(
      FeatureVector weights, JoshuaConfiguration joshuaConfiguration) {
    return new LabelSubstitutionSparseFF(weights, getFeatureNameStandardSparseFeature(),
        joshuaConfiguration, createNoSmoothingLabelSubstiontionSmoothersList());
  }

  public static LabelSubstitutionBasicFF createLabelSubstitutionFFDoubleLabel(
      FeatureVector weights, JoshuaConfiguration joshuaConfiguration) {
    return new LabelSubstitutionBasicFF(weights, getFeatureNameStandardFeature(),
        joshuaConfiguration, createDoubleLabelSmoothingLabelSubstiontionSmoothersList());
  }

  public static LabelSubstitutionSparseFF createLabelSubstitutionFFDoubleLabelSparse(
      FeatureVector weights, JoshuaConfiguration joshuaConfiguration) {
    return new LabelSubstitutionSparseFF(weights, getFeatureNameStandardSparseFeature(),
        joshuaConfiguration, createDoubleLabelSmoothingLabelSubstiontionSmoothersList());
  }

  public static String getFeatureNameStandardFeature() {
    return STANDARD_LABEL_SUBSTITUTION_BASIC_FEATURE_FUNCTION_NAME;
  }

  public static String getFeatureNameStandardSparseFeature() {
    return STANDARD_LABEL_SUBSTITUTION_SPARSE_FEATURE_FUNCTION_NAME;
  }

  public static List<String> getLabelParts(String originalLabelString) {
    String originalLabelStringWithoutBrackets = originalLabelString.substring(1,
        originalLabelString.length() - 1);
    return Arrays.asList(originalLabelStringWithoutBrackets.split(DOUBLE_LABEL_SEPARATOR));
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

  public String getLowerCasedFeatureName() {
    return name.toLowerCase();
  }

  public static String getFeatureNamesPrefix(String labelSubstitutionRootTypeName, LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother){
    return labelSubstitutionRootTypeName + labelSubstitutionLabelSmoother.typeOfSmoothingSuffixString() + "_";
  }
  
  private final String getFeatureNamePrefix(
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    return getFeatureNamesPrefix(this.name, labelSubstitutionLabelSmoother);
  }

  private final String computeLabelMatchingFeature(String ruleNonterminal,
      String substitutionNonterminal, LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {    
    return LabelSubstitutionFeatureStrings.computeLabelMatchingFeature(getFeatureNamePrefix(labelSubstitutionLabelSmoother), 
        
        labelSubstitutionLabelSmoother.getSmoothedLabelString(ruleNonterminal),
        labelSubstitutionLabelSmoother.getSmoothedLabelString(substitutionNonterminal));
  }

  private final String computeLabelSubstitutionFeature(String ruleNonterminal,
      String substitutionNonterminal, LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    return LabelSubstitutionFeatureStrings.computeLabelSubstitutionFeature(getFeatureNamePrefix(labelSubstitutionLabelSmoother), 
        labelSubstitutionLabelSmoother.getSmoothedLabelString(ruleNonterminal), 
        labelSubstitutionLabelSmoother.getSmoothedLabelString(substitutionNonterminal));
  }

  private static List<String> getLabelSmoothedLabelsList(List<String> labelsList,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    List<String> result = new ArrayList<String>();
    for (String label : labelsList) {
      result.add(labelSubstitutionLabelSmoother.getSmoothedLabelString(label));
    }
    return result;
  }

  private static String  getLabelSmoothedLHS(Rule rule,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother){
    return labelSubstitutionLabelSmoother.getSmoothedLabelString(RulePropertiesQuerying.getLHSAsString(rule)); 
  }
  
  private static List<String>  getLabelSmoothedSourceNonterminalsg(Rule rule,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother){
    List<String> ruleSourceNonterminals = RulePropertiesQuerying
        .getRuleSourceNonterminalStrings(rule);
    List<String> labelSmoothedSourceNonterminals = getLabelSmoothedLabelsList(
        ruleSourceNonterminals, labelSubstitutionLabelSmoother);
    return labelSmoothedSourceNonterminals;
  }
  
  private static List<String> getSmoothedSubstitutionLabelsList(List<HGNode> tailNodes,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother){
    List<String> substitutionNonterminals = RulePropertiesQuerying
        .getSourceNonterminalStrings(tailNodes);
    List<String> smoothedSubstitutionLabelsList = getLabelSmoothedLabelsList(
        substitutionNonterminals, labelSubstitutionLabelSmoother);
    return smoothedSubstitutionLabelsList;
  }
  
  
  public final String getGapLabelsForRuleIdenitySubstitutionFeature(Rule rule,
      List<HGNode> tailNodes, LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother) {
    return LabelSubstitutionFeatureStrings.getGapLabelsForRuleIdenitySubstitutionFeature(
        getFeatureNamePrefix(labelSubstitutionLabelSmoother),  
        getLabelSmoothedLHS(rule, labelSubstitutionLabelSmoother), 
        getLabelSmoothedSourceNonterminalsg(rule, labelSubstitutionLabelSmoother), 
        getSmoothedSubstitutionLabelsList(tailNodes, labelSubstitutionLabelSmoother),
        rule.isInverting());
  }

  private static List<String> getAllLabelsList(List<String> ruleSourceNonterminals,
      List<String> substitutionNonterminals) {
    List<String> allLabelsList = new ArrayList<String>();
    allLabelsList.addAll(ruleSourceNonterminals);
    allLabelsList.addAll(substitutionNonterminals);
    return allLabelsList;

  }

  private boolean isFuzzyMatchingGlueGrammarNonterminal(String ruleNonterminal) {
    return ruleNonterminal.equals(FUZZY_MATCHING_GLUE_GRAMMAR_NONTERIMINAL);
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

      /**
       * We only want to add matching and substitution features for labels that are actually allowed
       * to match something apart from themselves. The GOAL and OOV label which can only match
       * themselves otherwise distort the information of the real" (i.e. free) substitutions
       */
      if (!NonterminalMatcher.isOOVLabelOrGoalLabel(ruleNonterminal, joshuaConfiguration)) {

        /**
         * For matching features, we are furthermore not interested in substitutions of labels for
         * the glue grammar nonterminal symbol ([X]) as such substitutions are necessary and again
         * would confuse the intended purpose of this feature, as something that counts fully
         * optional substitutions of either matching labels or non-matching labels.
         */
        if (!isFuzzyMatchingGlueGrammarNonterminal(ruleNonterminal)) {
          acc.add(
              computeLabelMatchingFeature(ruleNonterminal, substitutionNonterminal,
                  labelSubstitutionLabelSmoother), 1);
        }

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
      Sentence sentence, Accumulator acc) {
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
          addFeatures(acc, labelSubstitutionLabelSmoother, ruleSourceNonterminals,
              substitutionNonterminals, rule, tailNodes);
        }
      }
    }
    return null;
  }

  /**
   * This method computes the score for this feature type only. This is useful for prediction as
   * needed for optimization of addition of candidates during cube pruning, when working with fuzzy
   * matching.
   * 
   * @param rule
   * @param tailNodes
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return
   */
  public double computeScoreThisFeatureOnly(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, Sentence sentence) {
    ScoreAccumulator scoreAccumulator = new ScoreAccumulator();
    compute(rule, tailNodes, i, j, sourcePath, sentence, scoreAccumulator);
    return scoreAccumulator.getScore();
  }



  private static class LabelSubstitutionBasicFF extends LabelSubstitutionFF {
    
    public LabelSubstitutionBasicFF (FeatureVector weights, String[] args, JoshuaConfiguration config) {
      super(weights, args, config, "LabelSubstitutionBasic");
    }
    
    private LabelSubstitutionBasicFF(FeatureVector weights, String name,
        JoshuaConfiguration joshuaConfiguration,
        List<LabelSubstitutionLabelSmoother> labelSmoothersList) {
          super(weights, new String[0], joshuaConfiguration, "LabelSubstitutionBasic");
          this.labelSmoothersList = labelSmoothersList;
    }

    protected void addFeatures(Accumulator acc,
        LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother,
        List<String> ruleSourceNonterminals, List<String> substitutionNonterminals, Rule rule,
        List<HGNode> tailNodes) {
      addBasicFeatures(acc, labelSubstitutionLabelSmoother, ruleSourceNonterminals,
          substitutionNonterminals);
    }
  }

  
  public static abstract class LabelSubstitutionLabelSmoother {

    public abstract String getSmoothedLabelString(String originalLabelString);

    protected abstract String typeOfSmoothingSuffixString();

    protected abstract boolean ruleHasAppropriateLabelsForSmoother(List<String> allLabelsList);

    protected static String getStringWithLabelBrackets(String string) {
      return "[" + string + "]";
    }

    protected static boolean ruleLHSHasMultipleLabelParts(List<String> allLabelsList) {
      for (String label : allLabelsList) {
        if (label.contains(DOUBLE_LABEL_SEPARATOR)) {
          return true;
        }
      }
      return false;
    }

  }

  public static class NoSmoothingLabelSubstitutionLabelSmoother extends
      LabelSubstitutionLabelSmoother {
    @Override
    public String getSmoothedLabelString(String originalLabelString) {
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

  public static class FirstSublabelOnlyLabelSubstitutionLabelSmoother extends
      LabelSubstitutionLabelSmoother {
    @Override
    public String getSmoothedLabelString(String originalLabelString) {
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

  public static class LastSublabelOnlyLabelSubstitutionLabelSmoother extends
      LabelSubstitutionLabelSmoother {
    @Override
    public String getSmoothedLabelString(String originalLabelString) {
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
