
package joshua.decoder.chart_parser;

import java.util.List;

public class MeanStdComputation {

  public static double computeDoublesMean(List<Double> values) {
    double total = 0;
    for (double value : values) {
      total += value;
    }

    return (total / values.size());
  }

  public static double computeIntsMean(List<Integer> values) {
    double total = 0;
    for (int value : values) {
      total += value;
    }

    return (((double) total) / values.size());
  }

  public static double computeDoublesStd(List<Double> values) {
    double mean = computeDoublesMean(values);

    double total = 0;
    for (double value : values) {
      double squaredDiff = (value - mean) * (value - mean);
      total += squaredDiff;
    }

    double result = Math.sqrt(total / (values.size() - 1));
    return result;
  }

  public static double computeIntsStd(List<Integer> values) {
    double mean = computeIntsMean(values);

    double total = 0;
    for (double value : values) {
      double squaredDiff = (value - mean) * (value - mean);
      total += squaredDiff;
    }

    double result = Math.sqrt(total / (values.size() - 1));
    return result;
  }
}
