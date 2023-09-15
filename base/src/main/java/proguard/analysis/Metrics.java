package proguard.analysis;

import java.util.*;

/**
 * Utility to collect statistical information.
 *
 * @author Dennis Titze
 */
public class Metrics
{

    /**
     * Constants which are used as metric types.
     */
    public enum MetricType
    {
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
        CALL_GRAPH_RECONSTRUCTION_MAX_WIDTH_REACHED,
        CONCRETE_CALL_NO_CODE_ATTRIBUTE,
        DEX2PRO_INVALID_INNER_CLASS,
        DEX2PRO_UNPARSEABLE_METHOD_SKIPPED
    }

    public static final Map<MetricType, Integer> counts = new TreeMap<>();


    public static void increaseCount(MetricType type)
    {
        counts.merge(type, 1, Integer::sum);
    }


    /**
     * Get all collected data as a string and clear it afterwards.
     */
    public static String flush()
    {
        StringBuilder result = new StringBuilder("Metrics:\n");

        counts.forEach((type, count) -> result.append(type.name())
                                              .append(": ")
                                              .append(count)
                                              .append("\n"));
        counts.clear();
        return result.toString();
    }
}
