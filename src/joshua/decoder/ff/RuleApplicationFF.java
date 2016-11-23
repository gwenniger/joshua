package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

/**
 * This feature called RuleApplicationFF is a feature that simply marks the application of 
 * all rules. This is mostly useful for debugging, to see how a translation was formed, from 
 * what rules, without needing to go into the complication of outputting entire derivations, 
 * which is somewhat harder to do. 
 * In particular for translations that are unexpected, meaning it is not clear how they could be 
 * formed, this is useful. 
 *  
 * @author gemaille
 *
 */
public class RuleApplicationFF extends StatelessFF {
  private static final String FEATURE_NAME = "RuleApplication";
  private static final String RULE_APPLICATION_FEATURE_PREFIX = "R.A.F._";

  public RuleApplicationFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, FEATURE_NAME, args, config);
  }

  
  private String getRuleStringReplacedBrackets(Rule rule){
    // Replace the whitespace and "|||" to avoid confusing the tuner (mira)
    return rule.getRuleString().replace("|||","###").replace(" ","_");
  }
  
  
  private String getRuleFeatureString(Rule rule) {
    String result = "";
    result += RULE_APPLICATION_FEATURE_PREFIX + getRuleStringReplacedBrackets(rule);
    return result;
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    acc.add(getRuleFeatureString(rule), 1);
    return null;
  }

}
