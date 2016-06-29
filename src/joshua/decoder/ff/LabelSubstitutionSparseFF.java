package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public  class LabelSubstitutionSparseFF extends LabelSubstitutionFF {
  
  public LabelSubstitutionSparseFF (FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, args, config, "LabelSubstitutionSparse");
  }

  protected LabelSubstitutionSparseFF(FeatureVector weights, String name,
      JoshuaConfiguration joshuaConfiguration,
      List<LabelSubstitutionLabelSmoother> labelSmoothersList) {
        super(weights, new String[0], joshuaConfiguration, "LabelSubstitutionSparse");
        this.labelSmoothersList = labelSmoothersList;
  }
  
  
  @Override
  protected void addAllFeatures(Accumulator acc,
      List<LabelSubstitutionLabelSmoother> labelSubstitutionLabelSmoothers,
      List<String> ruleSourceNonterminals, List<String> substitutionNonterminals, Rule rule,
      List<HGNode> tailNodes){
    addAllBasicFeatures(acc, labelSubstitutionLabelSmoothers, ruleSourceNonterminals, substitutionNonterminals, rule, tailNodes);
    addAllSparseFeatures(acc, labelSubstitutionLabelSmoothers, ruleSourceNonterminals, substitutionNonterminals, rule, tailNodes);
  }
  
  protected void addAllSparseFeatures(Accumulator acc,
      List<LabelSubstitutionLabelSmoother> labelSubstitutionLabelSmoothers,
      List<String> ruleSourceNonterminals, List<String> substitutionNonterminals, Rule rule,
      List<HGNode> tailNodes){
    for(LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother : labelSubstitutionLabelSmoothers){
      addSparseFeature(acc, labelSubstitutionLabelSmoother, rule, tailNodes);
    }
  }
}
