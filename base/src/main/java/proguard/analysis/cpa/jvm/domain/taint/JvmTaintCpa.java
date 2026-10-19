package proguard.analysis.cpa.jvm.domain.taint;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import proguard.analysis.cpa.defaults.MergeJoinOperator;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StaticPrecisionAdjustment;
import proguard.analysis.cpa.defaults.StopJoinOperator;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;

/**
 * The {@link JvmTaintCpa} computes abstract states containing {@link TaintSource}s which can reach
 * the given code location.
 */
public class JvmTaintCpa extends SimpleCpa<JvmAbstractState<SetAbstractState<TaintSource>>> {

  /**
   * Create a taint CPA.
   *
   * @param sources a set of taint sources
   */
  public JvmTaintCpa(Set<? extends TaintSource> sources) {
    this(
        createSourcesMap(sources),
        Collections.emptyMap(),
        Collections.emptyMap(),
        NeverAbortOperator.INSTANCE);
  }

  /**
   * Create a taint CPA.
   *
   * @param signaturesToSources a mapping from method signature to taint sources
   * @param taintTransformers a mapping from method signature to a transformer object applied to the
   *     taint state when that method is invoked
   * @param extraTaintPropagationLocations a mapping from a specific method call to any jvm state
   *     location that is tainted as a result of the call
   * @param abortOperator an operator used to stop the analysis prematurely.
   */
  public JvmTaintCpa(
      Map<Signature, Set<TaintSource>> signaturesToSources,
      Map<MethodSignature, JvmTaintTransformer> taintTransformers,
      Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations,
      AbortOperator abortOperator) {
    super(
        new JvmTaintTransferRelation(
            signaturesToSources, taintTransformers, extraTaintPropagationLocations),
        new MergeJoinOperator<>(),
        new StopJoinOperator<>(),
        new StaticPrecisionAdjustment(),
        abortOperator);
  }

  /**
   * Constructs a map from the source signatures to the sources with that signature, starting from a
   * set of sources.
   */
  public static Map<Signature, Set<TaintSource>> createSourcesMap(
      Set<? extends TaintSource> sources) {
    Map<Signature, Set<TaintSource>> taintSourcesMap = new HashMap<>();
    sources.forEach(
        s -> taintSourcesMap.computeIfAbsent(s.getSignature(), key -> new HashSet<>()).add(s));
    return taintSourcesMap;
  }
}
