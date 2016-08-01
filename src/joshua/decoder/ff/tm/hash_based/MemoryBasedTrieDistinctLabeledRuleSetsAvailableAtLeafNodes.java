package joshua.decoder.ff.tm.hash_based;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;

/**
 * The goal of this class is to provide additional functionality for getting 
 * sorted Rule collections that are separated by their labeling, that is their 
 * labels for the LHS and rule gaps (nonterminals).
 * This in turn is motivated by the desire to be able to explore the best matching 
 * rules given different rule labels, without having to explicitly split over rule 
 * labels throughout the grammar Trie structure, which is more inefficient computationally.
 * 
 * @author gemaille
 *
 */
public class MemoryBasedTrieDistinctLabeledRuleSetsAvailableAtLeafNodes extends
    MemoryBasedTrieEfficientNonterminalLookup {

  private List<RuleCollection> distinctLabeledRuleSets = null;

  
  public MemoryBasedTrieDistinctLabeledRuleSetsAvailableAtLeafNodes(){
    super();
  }
  
  // We need the JoshuaConfiguration to be able to decide what are OOV and Goal Nonterminals
  public List<RuleCollection> getDistinctLabeledRuleSetsSorted(List<FeatureFunction> models) {
    // We first try to get the distinctLabeledRuleSets if they are already computed
    if (distinctLabeledRuleSets != null) {
      return distinctLabeledRuleSets;
    }
    // Call the synchronized method to compute the nonterminals extensions
    return getOrComputeAndGetDistinctLabeledRuleSetsSorted(models);
  }

  private Map<String, List<Rule>> computeRuleLabelingToRuleListMap(List<FeatureFunction> models) {
    Map<String, List<Rule>> rulesMap = new HashMap<String, List<Rule>>();
    RuleCollection ruleCollection = getRuleCollection();
    Assert.assertNotNull(models);
    List<Rule> sortedRulesList = ruleCollection.getSortedRules(models);

    for (Rule rule : sortedRulesList) {
      String ruleLabelStringRepresentation = getRuleLabelStringRepresentation(rule);
      // Create a new list of rules for the rule label string represenation if not present yet
      if (!rulesMap.containsKey(ruleLabelStringRepresentation)) {
        rulesMap.put(ruleLabelStringRepresentation, new ArrayList<Rule>());
      }
      rulesMap.get(ruleLabelStringRepresentation).add(rule);
    }
    return rulesMap;

  }

  private List<RuleCollection> computeDistinctLabeledRuleSetsSorted(List<FeatureFunction> models) {
    List<RuleCollection> result = new ArrayList<RuleCollection>();

    Map<String, List<Rule>> rulesMap = computeRuleLabelingToRuleListMap(models);
    for (List<Rule> sortedRulesList : rulesMap.values()) {
      MemoryBasedRuleBin memmoryBasedRuleBin = new MemoryBasedRuleBin(this.getRuleCollection()
          .getArity(), this.getRuleCollection().getSourceSide());
      for (Rule rule : sortedRulesList) {
        memmoryBasedRuleBin.addRule(rule);
      }
      result.add(memmoryBasedRuleBin);
    }
    return result;
  }

  private synchronized List<RuleCollection> getOrComputeAndGetDistinctLabeledRuleSetsSorted(
      List<FeatureFunction> models) {
    // Possibly another thread already entered the method and computed the array,
    // in which case we should not do it again
    if (distinctLabeledRuleSets != null) {
      return distinctLabeledRuleSets;
    }
    distinctLabeledRuleSets = computeDistinctLabeledRuleSetsSorted(models);
    return distinctLabeledRuleSets;
  }

  private String getRuleLabelStringRepresentation(Rule rule) {
    String result = "";
    result += "LHS:+" + rule.getLHS();
    for (Integer nonterminalKey : rule.getForeignNonTerminals()) {
      result += "-nont: " + nonterminalKey;
    }
    return result;
  }

}
