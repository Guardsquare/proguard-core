package proguard.analysis.cpa.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.bam.BamCache;
import proguard.analysis.cpa.bam.BamCacheImpl;
import proguard.analysis.cpa.bam.BamCpa;
import proguard.analysis.cpa.bam.CpaWithBamOperators;
import proguard.analysis.cpa.bam.NoOpRebuildOperator;
import proguard.analysis.cpa.defaults.BreadthFirstWaitlist;
import proguard.analysis.cpa.defaults.HashMapAbstractState;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.Waitlist;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationCpa;
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintCpa;
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSink;
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource;
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintTransformer;
import proguard.analysis.cpa.jvm.domain.taint.TaintExpandOperator;
import proguard.analysis.cpa.jvm.domain.taint.TaintReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmForgetfulHeapAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;

/**
 * Helper class to analyze taints propagation in a program inter-procedurally (i.e., if the result
 * of the invocation of a {@link TaintSource} affects a {@link
 * proguard.analysis.cpa.domain.taint.TaintSink}).
 *
 * <p>The analyzer can be configured in several ways via {@link TaintAnalyzer.Builder}.
 *
 * <p>The {@link TaintAnalyzer#analyze(MethodSignature)} method can be called to perform the
 * configured analysis starting from a given method in the program.
 *
 * <p>The same analyzer can be used to analyze several methods in sequence, in this case the
 * analysis {@link BamCache} will be shared between the sequential analyses making them potentially
 * avoid recalculating the results when a method is called again with known parameters.
 *
 * <p>This might sometimes not be the desired behavior, since the cache might take a lot of memory.
 * If this is a concern rebuilding the {@link TaintAnalyzer} from the original {@link
 * TaintAnalyzer.Builder} will provide a fresh cache.
 *
 * <p>Another problem of not currently having snapshots of the cache for a single run of {@link
 * TaintAnalyzer#analyze(MethodSignature)} is that some components of {@link TaintAnalyzerResult}
 * that take a long time to compute might be recalculated several times for different runs. For this
 * reason:
 *
 * <ul>
 *   <li>Sink locations triggered by a valid source are not computed until needed. This happens when
 *       computing the witness traces or when calling {@link
 *       TaintAnalyzerResult.TaintAnalysisResult#getEndpoints()} or {@link
 *       TaintAnalyzerResult.TaintAnalysisResult#getEndpointToTriggeredSinks()}.
 *   <li>Witness traces are not calculated unless the trace reconstruction result has been
 *       explicitly requested via {@link TaintAnalyzerResult#getTraceReconstructionResult()}.
 * </ul>
 *
 * <p>When using a cache that just keeps all results (i.e. without evictions), which is the only
 * type originally available, this means that it's best to calculate endpoints and witness traces
 * only on the {@link TaintAnalyzerResult} provided by the last run of {@link
 * TaintAnalyzer#analyze(MethodSignature)} for the analyzer.
 *
 * <p>{@link TaintAnalyzer} is currently not designed to be thread safe. Among the known reasons,
 * the currently available {@link BamCache}s are not designed for concurrent access.
 */
public class TaintAnalyzer {

  private final Function<MethodSignature, BamCpa<SetAbstractState<JvmTaintSource>>> cpaCreator;
  private final Function<MethodSignature, JvmAbstractState<SetAbstractState<JvmTaintSource>>>
      initialStateCreator;
  private final Function<
          BamCpa<SetAbstractState<JvmTaintSource>>,
          JvmMemoryLocationCpa<SetAbstractState<JvmTaintSource>>>
      memoryCpaCreator;
  private final Collection<? extends JvmTaintSink> taintSinks;

  private TaintAnalyzer(
      Function<MethodSignature, BamCpa<SetAbstractState<JvmTaintSource>>> cpaCreator,
      Function<MethodSignature, JvmAbstractState<SetAbstractState<JvmTaintSource>>>
          initialStateCreator,
      Function<
              BamCpa<SetAbstractState<JvmTaintSource>>,
              JvmMemoryLocationCpa<SetAbstractState<JvmTaintSource>>>
          memoryCpaCreator,
      Collection<? extends JvmTaintSink> taintSinks) {
    this.cpaCreator = cpaCreator;
    this.initialStateCreator = initialStateCreator;
    this.memoryCpaCreator = memoryCpaCreator;
    this.taintSinks = taintSinks;
  }

  /**
   * Run the taint analysis on the given method.
   *
   * <p>The results are not intended as just for the last execution, but as a view on the full
   * analysis' cache.
   *
   * <p>Since the cache at the moment has no capability to remember the last execution, the result
   * will change as the cache changes (i.e., after calling this again on a new method, old instances
   * of {@link TaintAnalyzerResult} will also be updated).
   *
   * @param mainSignature the signature of the method to analyze.
   * @return the result of the analysis.
   */
  public TaintAnalyzerResult analyze(MethodSignature mainSignature) {
    BamCpa<SetAbstractState<JvmTaintSource>> taintCpa = cpaCreator.apply(mainSignature);
    CpaAlgorithm<JvmAbstractState<SetAbstractState<JvmTaintSource>>> cpaAlgorithm =
        new CpaAlgorithm<>(taintCpa);

    Waitlist<JvmAbstractState<SetAbstractState<JvmTaintSource>>> waitList =
        new BreadthFirstWaitlist<>();
    ProgramLocationDependentReachedSet<JvmAbstractState<SetAbstractState<JvmTaintSource>>>
        reachedSet = new ProgramLocationDependentReachedSet<>();

    JvmAbstractState<SetAbstractState<JvmTaintSource>> initialState =
        initialStateCreator.apply(mainSignature);
    waitList.add(initialState);
    reachedSet.add(initialState);

    cpaAlgorithm.run(reachedSet, waitList);

    return new TaintAnalyzerResult(
        taintCpa, reachedSet, taintSinks, memoryCpaCreator.apply(taintCpa));
  }

  /**
   * Class to configure and build a {@link TaintAnalyzer}.
   *
   * <p>Each separate built {@link TaintAnalyzer} uses its own {@link BamCache}, so different
   * analyzers won't share results.
   */
  public static class Builder {
    private final JvmCfa cfa;
    private final Set<? extends JvmTaintSource> taintSources;
    private final Set<? extends JvmTaintSink> taintSinks;
    private int maxCallStackDepth = 10;
    private AbortOperator abortOperator = NeverAbortOperator.INSTANCE;
    private AbortOperator memoryLocationAbortOperator = NeverAbortOperator.INSTANCE;
    private Map<MethodSignature, JvmTaintTransformer> taintTransformers = Collections.emptyMap();
    private Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations =
        Collections.emptyMap();

    public Builder(
        JvmCfa cfa,
        Set<? extends JvmTaintSource> taintSources,
        Set<? extends JvmTaintSink> taintSinks) {
      this.cfa = cfa;
      this.taintSources = taintSources;
      this.taintSinks = taintSinks;
    }

    /** Build a {@link TaintAnalyzer} */
    public TaintAnalyzer build() {
      Map<Signature, Set<JvmTaintSource>> sourcesMap = JvmTaintCpa.createSourcesMap(taintSources);

      ConfigurableProgramAnalysis<JvmAbstractState<SetAbstractState<JvmTaintSource>>>
          intraproceduralCpa =
              new JvmTaintCpa(
                  sourcesMap, taintTransformers, extraTaintPropagationLocations, abortOperator);

      boolean reduceHeap = false;
      CpaWithBamOperators<SetAbstractState<JvmTaintSource>> interproceduralCpa =
          new CpaWithBamOperators<>(
              intraproceduralCpa,
              new TaintReduceOperator(reduceHeap, sourcesMap),
              new TaintExpandOperator(cfa, sourcesMap, reduceHeap),
              new NoOpRebuildOperator());
      BamCache<SetAbstractState<JvmTaintSource>> cache = new BamCacheImpl<>();

      return new TaintAnalyzer(
          mainMethodSignature ->
              new BamCpa<>(interproceduralCpa, cfa, mainMethodSignature, cache, maxCallStackDepth),
          mainMethodSignature ->
              new JvmAbstractState<>(
                  cfa.getFunctionEntryNode(mainMethodSignature),
                  new JvmFrameAbstractState<>(),
                  new JvmForgetfulHeapAbstractState<>(SetAbstractState.bottom()),
                  new HashMapAbstractState<>()),
          taintBamCpa ->
              new JvmMemoryLocationCpa<>(
                  SetAbstractState.bottom(),
                  taintBamCpa,
                  extraTaintPropagationLocations,
                  memoryLocationAbortOperator),
          taintSinks);
    }

    /**
     * Set the max depth call depth of the inter-procedural analysis. After the max depth has been
     * reached, the analysis will provide a default result for the method (i.e., in the same way
     * {@link proguard.classfile.LibraryMethod}s are usually not analyzed).
     *
     * <p>The default value is 10.
     *
     * @param maxCallStackDepth maximum depth of the call stack analyzed inter-procedurally. 0 means
     *     intra-procedural analysis. < 0 means no maximum depth.
     * @return this {@link ValueAnalyzer} builder.
     */
    public Builder setMaxCallStackDepth(int maxCallStackDepth) {
      this.maxCallStackDepth = maxCallStackDepth;
      return this;
    }

    /** Sets the abort operator for premature CPA algorithm termination. */
    public Builder setAbortOperator(AbortOperator abortOperator) {
      this.abortOperator = abortOperator;
      return this;
    }

    /** Sets the abort operator for premature trace reconstruction termination. */
    public Builder setMemoryLocationAbortOperator(AbortOperator memoryLocationAbortOperator) {
      this.memoryLocationAbortOperator = memoryLocationAbortOperator;
      return this;
    }

    /**
     * Set a mapping from method signature to a transformer object applied to the taint state when
     * that method is invoked.
     */
    public Builder setTaintTransformers(
        Map<MethodSignature, JvmTaintTransformer> taintTransformers) {
      this.taintTransformers = taintTransformers;
      return this;
    }

    /**
     * Set a mapping from a call to the set of locations which should get tainted after the call
     * invocation.
     */
    public Builder setExtraTaintPropagationLocations(
        Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations) {
      this.extraTaintPropagationLocations = extraTaintPropagationLocations;
      return this;
    }
  }
}
