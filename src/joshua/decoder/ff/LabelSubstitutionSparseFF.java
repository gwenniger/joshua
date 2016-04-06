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
  protected void addFeatures(Accumulator acc,
      LabelSubstitutionLabelSmoother labelSubstitutionLabelSmoother,
      List<String> ruleSourceNonterminals, List<String> substitutionNonterminals, Rule rule,
      List<HGNode> tailNodes) {
    addBasicFeatures(acc, labelSubstitutionLabelSmoother, ruleSourceNonterminals,
        substitutionNonterminals);
    addSparseFeature(acc, labelSubstitutionLabelSmoother, rule, tailNodes);
  }
}
