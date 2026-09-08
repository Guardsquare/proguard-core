package proguard.analysis;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Utility to collect statistical information. */
public class Metrics {

  /** Constants which are used as metric types. */
  public enum MetricType {
    MISSING_CLASS,
    MISSING_METHODS,
    UNSUPPORTED_OPCODE,
    PARTIAL_EVALUATOR_EXCESSIVE_COMPLEXITY,
    PARTIAL_EVALUATOR_VALUE_IMPRECISE,
    SYMBOLIC_CALL,
    CONCRETE_CALL,
    INCOMPLETE_CALL_SKIPPED,
    CALL_TO_ABSTRACT_METHOD,
    CALL_GRAPH_RECONSTRUCTION_MAX_DEPTH_REACHED,
    CONCRETE_CALL_NO_CODE_ATTRIBUTE,
    DEX2PRO_INVALID_INNER_CLASS,
    DEX2PRO_UNPARSEABLE_METHOD_SKIPPED,
    CALLGRAPHWALKER_MAX_DEPTH,
    CALLGRAPHWALKER_MAX_WIDTH,
  }

  public static final Map<MetricType, Integer> counts =
      Collections.synchronizedMap(new EnumMap<>(MetricType.class));

  public static void increaseCount(MetricType type) {
    counts.merge(type, 1, Integer::sum);
  }

  /**
   * Set the value of the metric to the given value if it's bigger than the current value.
   *
   * @param type the type of metric
   * @param value the value to set
   */
  public static void setIfMax(MetricType type, int value) {
    counts.compute(
        type, (key, mappedValue) -> mappedValue == null ? value : Integer.max(mappedValue, value));
  }

  /** Get all collected data as a string and clear it afterwards. */
  public static String flush() {
    StringBuilder result = new StringBuilder("Metrics:\n");

    counts.forEach(
        (type, count) -> result.append(type.name()).append(": ").append(count).append("\n"));
    counts.clear();
    return result.toString();
  }
}
