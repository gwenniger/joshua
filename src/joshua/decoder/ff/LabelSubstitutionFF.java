package joshua.decoder.ff;
/***
 * @author Gideon Wenniger
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.NonterminalMatcher;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;
import joshua.util.Pair;

public class LabelSubstitutionFF extends StatelessFF {
  private static final String STANDARD_LABEL_SUBSTITUTION_BASIC_FEATURE_FUNCTION_NAME = "LabelSubstitution";
  private static final String STANDARD_LABEL_SUBSTITUTION_SPARSE_FEATURE_FUNCTION_NAME = "LabelSubstitutionSparse";
  private static final String DOUBLE_LABEL_WITH_LABEL_SPLITTING_SMOOTHING_ARGUMENT = "DoubleLabelWithLabelSplittingSmoothing";
  private static final String DOUBLE_LABEL_WITH_LABEL_SPLITTING_SMOOTHING_ONLY_SINGLE_LABEL_FEATURES_ARGUMENT = "DoubleLabelWithLabelSplittingSmoothingOnlySingleLabelFeatures";
  private static final String DOUBLE_LABEL_SEPARATOR = "<<>>";
  private static Pattern DOUBLE_LABEL_SEPARATOR_PATTERN = Pattern.compile(DOUBLE_LABEL_SEPARATOR);

  private static final String FUZZY_MATCHING_GLUE_GRAMMAR_NONTERIMINAL = "[X]";

  private final JoshuaConfiguration joshuaConfiguration;
  protected  List<LabelSubstitutionLabelSmoother> labelSmoothersList;

  private static final LabelSubstitutionFFCache LABEL_SUBSTITUTION_FF_CACHE = LabelSubstitutionFFCache.createLabelSubstitutionFFCache(); 
  
  
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
    if(hasDoubleLabelWithLabelSplittingSmoothingArgument(Arrays.asList(args))){        
        return  createDoubleLabelSmoothingLabelSubstiontionSmoothersList();
    }
    else if(hasDoubleLabelWithLabelSplittingSmoothingOnlySingleLabelFeaturesArgument(Arrays.asList(args))){
      return createDoubleLabelSmoothingOnlySingleLabelLabelSubstiontionSmoothersList();
    }
    else{
      return createNoSmoothingLabelSubstiontionSmoothersList();
    }
  }
  
  public static boolean hasDoubleLabelWithLabelSplittingSmoothingArgument(List<String> args){
    // The first argument args[0] is the name of the feature itself
    if((args.size() > 1) && (args.get(1).equals(DOUBLE_LABEL_WITH_LABEL_SPLITTING_SMOOTHING_ARGUMENT))){
      return true;
    }
    return false;
  }

  public static boolean hasDoubleLabelWithLabelSplittingSmoothingOnlySingleLabelFeaturesArgument(List<String> args){
    // The first argument args[0] is the name of the feature itself
    if((args.size() > 1) && (args.get(1).equals(DOUBLE_LABEL_WITH_LABEL_SPLITTING_SMOOTHING_ONLY_SINGLE_LABEL_FEATURES_ARGUMENT))){
      return true;
    }
    return false;
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
    return Arrays.asList(DOUBLE_LABEL_SEPARATOR_PATTERN.split(originalLabelStringWithoutBrackets));
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
  
  /**
   * This method creates label smoothers that only produced features for the single labels, i.e. the 
   * NoSmoothingLabelSubstitutionLabelSmoother is not added to the list
   * @return
   */
  private static List<LabelSubstitutionLabelSmoother> createDoubleLabelSmoothingOnlySingleLabelLabelSubstiontionSmoothersList() {
    List<LabelSubstitutionLabelSmoother> result = new ArrayList<LabelSubstitutionLabelSmoother>();
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
  
  private static List<String>  getLabelSmoothedSourceNonterminals(Rule rule,
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
        getLabelSmoothedSourceNonterminals(rule, labelSubstitutionLabelSmoother), 
        getSmoothedSubstitutionLabelsList(tailNodes, labelSubstitutionLabelSmoother),
        rule.isInverting());
  }

//  private static List<String> getAllLabelsList(List<String> ruleSourceNonterminals,
//      List<String> substitutionNonterminals) {
//    List<String> allLabelsList = new ArrayList<String>();
//    allLabelsList.addAll(ruleSourceNonterminals);
//    allLabelsList.addAll(substitutionNonterminals);
//    return allLabelsList;
//
//  }

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
   * This method adds the basic label substitution features: 1. Label matching features (MATCH and
   * NOMATCH) 2. Simple Label substitution features: LabelX substitutes LabelY
   * 
   * @param acc
   * @param labelSubstitutionLabelSmoother
   * @param ruleSourceNonterminals
   * @param substitutionNonterminals
   */
  protected List<String> getBasicFeaturesForCaching(Accumulator acc,
      List<LabelSubstitutionLabelSmoother> labelSubstitutionLabelSmoothers,
      List<String> ruleSourceNonterminals, List<String> substitutionNonterminals) {
    
    List<String> result = new ArrayList<String>();
    
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
          for(LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother : labelSubstitutionLabelSmoothers){
          result.add(
              computeLabelMatchingFeature(ruleNonterminal, substitutionNonterminal,
                  labelSubstitutionLabelSmoother));
          }
        }

        for(LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother : labelSubstitutionLabelSmoothers){
          result.add(
              computeLabelSubstitutionFeature(ruleNonterminal, substitutionNonterminal,
                  labelSubstitutionLabelSmoother));  
        }        
      }
    }
    return result;
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

  /**
   *  This method add the features to the accumulator, and is overwritten in LabelSubstitutionSparseFF
   * @param acc
   * @param labelSubstitutionLabelSmoothers
   * @param ruleSourceNonterminals
   * @param substitutionNonterminals
   * @param rule
   * @param tailNodes
   */
  protected void addAllFeatures(Accumulator acc,
      List<LabelSubstitutionLabelSmoother> labelSubstitutionLabelSmoothers,
      List<String> ruleSourceNonterminals, List<String> substitutionNonterminals, Rule rule,
      List<HGNode> tailNodes){
    addAllBasicFeatures(acc, labelSubstitutionLabelSmoothers, ruleSourceNonterminals, substitutionNonterminals, rule, tailNodes);
  }
  
  /**
   * Method that adds all Basic Features, using caching to make this more efficient
   * 
   * @param acc
   * @param labelSubstitutionLabelSmoothers
   * @param ruleSourceNonterminals
   * @param substitutionNonterminals
   * @param rule
   * @param tailNodes
   */
  protected void addAllBasicFeatures(Accumulator acc,
      List<LabelSubstitutionLabelSmoother> labelSubstitutionLabelSmoothers,
      List<String> ruleSourceNonterminals, List<String> substitutionNonterminals, Rule rule,
      List<HGNode> tailNodes){
    
    for (int nonterinalIndex = 0; nonterinalIndex < ruleSourceNonterminals.size(); nonterinalIndex++) {
      String ruleNonterminal = ruleSourceNonterminals.get(nonterinalIndex);
      String substitutionNonterminal = substitutionNonterminals.get(nonterinalIndex);
      
      Pair<String,String> substitutionPair = new Pair<String,String>(ruleNonterminal,substitutionNonterminal);
      List<String> features = null; 
      
      if(LABEL_SUBSTITUTION_FF_CACHE.containsBasicSubstitutionFeatures(substitutionPair)){
        features = LABEL_SUBSTITUTION_FF_CACHE.getBaicFeatures(substitutionPair);
      }
      else{
        features = getBasicFeaturesForCaching(acc, labelSubstitutionLabelSmoothers, ruleSourceNonterminals, substitutionNonterminals);
        LABEL_SUBSTITUTION_FF_CACHE.addFeatures(substitutionPair, features);
      }
      for(String feature : features){
        acc.add(feature, 1);
      }      
    }
  }
  

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    if (rule != null && (tailNodes != null)) {

      List<String> ruleSourceNonterminals = RulePropertiesQuerying
          .getRuleSourceNonterminalStrings(rule);
      List<String> substitutionNonterminals = RulePropertiesQuerying
          .getSourceNonterminalStrings(tailNodes);
      
      
      addAllFeatures(acc, this.labelSmoothersList, ruleSourceNonterminals, substitutionNonterminals, rule, tailNodes);
      
     // for (LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother : this.labelSmoothersList) {
        // Removed the check for appropriate labels for the labelSubstitutionLabelSmoother smoother. 
        // (Commented out code)
        // We assume that when double 
        // labels are used this is done uniformly, so this check, which is computationally costly,
        // is unnecessary and therefore better to avoid.
        
        //if (labelSubstitutionLabelSmoother.ruleHasAppropriateLabelsForSmoother(getAllLabelsList(
        //    ruleSourceNonterminals, substitutionNonterminals))) {
          // Assert.assertEquals(ruleSourceNonterminals.size(), substitutionNonterminals.size());

          // Add the features. Depending on the implementation these are only the basic features
          // or the basic features and sparse feature
        //  addFeatures(acc, labelSubstitutionLabelSmoother, ruleSourceNonterminals,
         //     substitutionNonterminals, rule, tailNodes);
        //}
      //}
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
  
  public static abstract class LabelSubstitutionLabelSmoother {

    public abstract String getSmoothedLabelString(String originalLabelString);

    protected abstract String typeOfSmoothingSuffixString();

    //protected abstract boolean ruleHasAppropriateLabelsForSmoother(List<String> allLabelsList);

    protected static String getStringWithLabelBrackets(String string) {
      return "[" + string + "]";
    }

    /*
    protected static boolean ruleLHSHasMultipleLabelParts(List<String> allLabelsList) {
      for (String label : allLabelsList) {
        if (label.contains(DOUBLE_LABEL_SEPARATOR)) {
          return true;
        }
      }
      return false;
    }*/

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

    //@Override
    //protected boolean ruleHasAppropriateLabelsForSmoother(List<String> allLabelsList) {
    //  return true;
    //}

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

    //@Override
    //protected boolean ruleHasAppropriateLabelsForSmoother(List<String> allLabelsList) {
    //  return ruleLHSHasMultipleLabelParts(allLabelsList);
    //}
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

    //@Override
    //protected boolean ruleHasAppropriateLabelsForSmoother(List<String> allLabelsList) {
    //  return ruleLHSHasMultipleLabelParts(allLabelsList);
    //}
  }

}
