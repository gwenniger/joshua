package joshua.decoder.ff;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import joshua.util.Pair;

public class LabelSubstitutionFFCache {
  private final ConcurrentHashMap<Pair<String,String>,List<String>> basicFeaturesCacheMap;
  
  public LabelSubstitutionFFCache(ConcurrentHashMap<Pair<String,String>,List<String>> basicFeaturesCacheMap){
    this.basicFeaturesCacheMap = basicFeaturesCacheMap;
  }
  
  public static LabelSubstitutionFFCache createLabelSubstitutionFFCache(){
    return new LabelSubstitutionFFCache(new ConcurrentHashMap<Pair<String,String>,List<String>>());
  }
  
  public boolean containsBasicSubstitutionFeatures(Pair<String,String> substitutionPair){
    return basicFeaturesCacheMap.containsKey(substitutionPair);
  }
  public List<String> getBaicFeatures(Pair<String,String> substitutionPair){
    return basicFeaturesCacheMap.get(substitutionPair);
  }
  
  public void addFeatures(Pair<String,String> substitutionPair, List<String> features){
    basicFeaturesCacheMap.putIfAbsent(substitutionPair, features);
  }  

}
