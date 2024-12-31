package proguard.analysis.cpa.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.bam.BamCache;
import proguard.analysis.cpa.bam.BamCpa;
import proguard.analysis.cpa.bam.BlockAbstraction;
import proguard.analysis.cpa.defaults.BreadthFirstWaitlist;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.Waitlist;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.domain.memory.BamLocationDependentJvmMemoryLocation;
import proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationAbstractState;
import proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationCpa;
import proguard.analysis.cpa.jvm.domain.memory.TraceExtractor;
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSink;
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;

/**
 * Provides results for a run of {@link TaintAnalyzer}.
 *
 * <p>The results are not intended as just for the last execution of {@link
 * TaintAnalyzer#analyze(MethodSignature)}, but as a view of the entire results extracted from the
 * cache of the analysis.
 *
 * <p>Since the cache at the moment has no capability to remember the last execution, the result
 * will change as the cache changes (i.e., after calling {@link
 * TaintAnalyzer#analyze(MethodSignature)} on a new method with the same {@link TaintAnalyzer}, old
 * instances of {@link TaintAnalyzerResult} will also be updated).
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
 *   <li>Witness traces are no calculated unless the trace reconstruction result has been explicitly
 *       requested via {@link TaintAnalyzerResult#getTraceReconstructionResult()}.
 * </ul>
 */
public class TaintAnalyzerResult {
  private final TaintAnalysisResult taintAnalysisResult;
  private final JvmMemoryLocationCpa<SetAbstractState<JvmTaintSource>> traceReconstructionCpa;
  // lazy initialization, created only when requested
  private @Nullable TraceExtractor<SetAbstractState<JvmTaintSource>> traceExtractor;

  /* package private */ TaintAnalyzerResult(
      BamCpa<SetAbstractState<JvmTaintSource>> executedTaintCpa,
      ProgramLocationDependentReachedSet<JvmAbstractState<SetAbstractState<JvmTaintSource>>>
          mainMethodReachedSet,
      Collection<? extends JvmTaintSink> taintSinks,
      JvmMemoryLocationCpa<SetAbstractState<JvmTaintSource>> traceReconstructionCpa) {

    this.taintAnalysisResult =
        new TaintAnalysisResult(executedTaintCpa, mainMethodReachedSet, taintSinks);
    this.traceReconstructionCpa = traceReconstructionCpa;
  }

  /** Get the result for the taint analysis. */
  public TaintAnalysisResult getTaintAnalysisResult() {
    return taintAnalysisResult;
  }

  /**
   * Get the result of trace reconstruction.
   *
   * <p>Trace reconstruction is a very expensive operation and does not run if not explicitly
   * requested, calling this method for the first time on a specific {@link TaintAnalyzerResult}
   * triggers running the witness traces creation.
   *
   * <p>The witness trace is based uniquely on the taint analysis cache, and at the moment the cache
   * has no capability to remember a snapshot of the last run of {@link
   * TaintAnalyzer#analyze(MethodSignature)}. Until this holds true, if the same {@link
   * TaintAnalyzer} has been used to analyze several methods (to exploit block abstraction
   * memoization with the cache), the results of the trace reconstruction should be retrieved only
   * once for the last call of {@link TaintAnalyzer#analyze(MethodSignature)}, since doing otherwise
   * would result in computing the same traces all over again.
   */
  public TraceExtractor<SetAbstractState<JvmTaintSource>> getTraceReconstructionResult() {
    if (traceExtractor == null) {
      // This triggers the witness trace analysis
      traceExtractor = new TraceReconstructionResult(traceReconstructionCpa, taintAnalysisResult);
    }
    return traceExtractor;
  }

  /**
   * Results for taint analysis. Provides the reached states and which sinks were triggered, but no
   * witness trace on how the sink was reached from a source (use {@link
   * TaintAnalyzerResult#getTraceReconstructionResult()} for that).
   */
  public static class TaintAnalysisResult {

    private final ProgramLocationDependentReachedSet<
            JvmAbstractState<SetAbstractState<JvmTaintSource>>>
        mainMethodReachedSet;
    private final BamCpa<SetAbstractState<JvmTaintSource>> executedTaintCpa;
    private final Collection<? extends JvmTaintSink> taintSinks;
    // This field is lazy and initialized the first time endpoints are requested
    private @Nullable Map<
            BamLocationDependentJvmMemoryLocation<SetAbstractState<JvmTaintSource>>,
            List<JvmTaintSink>>
        endpointToTriggeredSinks = null;

    private TaintAnalysisResult(
        BamCpa<SetAbstractState<JvmTaintSource>> executedTaintCpa,
        ProgramLocationDependentReachedSet<JvmAbstractState<SetAbstractState<JvmTaintSource>>>
            mainMethodReachedSet,
        Collection<? extends JvmTaintSink> taintSinks) {
      this.executedTaintCpa = executedTaintCpa;
      this.mainMethodReachedSet = mainMethodReachedSet;
      this.taintSinks = taintSinks;
    }

    public BamCache<SetAbstractState<JvmTaintSource>> getTaintResultCache() {
      return executedTaintCpa.getCache();
    }

    public ProgramLocationDependentReachedSet<JvmAbstractState<SetAbstractState<JvmTaintSource>>>
        getMainMethodReachedSet() {
      return mainMethodReachedSet;
    }

    /**
     * Get locations where sinks have been triggered by a valid source.
     *
     * <p>The endpoints are computed lazily, since it can be an expensive operation, and multiple
     * runs of {@link TaintAnalyzer#analyze(MethodSignature)} just update the same cache. So, if the
     * same {@link TaintAnalyzer} performs several runs, it's better to get the endpoints only after
     * all runs have been executed.
     */
    public Collection<BamLocationDependentJvmMemoryLocation<SetAbstractState<JvmTaintSource>>>
        getEndpoints() {
      if (endpointToTriggeredSinks == null) {
        this.endpointToTriggeredSinks = calculateEndpointsMapping(executedTaintCpa, taintSinks);
      }
      return endpointToTriggeredSinks.keySet();
    }

    /**
     * Maps locations where sinks have been triggered by valid sources to the triggered sink.
     *
     * <p>The endpoints are computed lazily, since it can be an expensive operation, and multiple
     * runs of {@link TaintAnalyzer#analyze(MethodSignature)} just update the same cache. So, if the
     * same {@link TaintAnalyzer} performs several runs, it's better to get the endpoints only after
     * all runs have been executed.
     */
    public Map<
            BamLocationDependentJvmMemoryLocation<SetAbstractState<JvmTaintSource>>,
            List<JvmTaintSink>>
        getEndpointToTriggeredSinks() {
      if (endpointToTriggeredSinks == null) {
        this.endpointToTriggeredSinks = calculateEndpointsMapping(executedTaintCpa, taintSinks);
      }
      return endpointToTriggeredSinks;
    }

    private Map<
            BamLocationDependentJvmMemoryLocation<SetAbstractState<JvmTaintSource>>,
            List<JvmTaintSink>>
        calculateEndpointsMapping(
            BamCpa<SetAbstractState<JvmTaintSource>> executedTaintCpa,
            Collection<? extends JvmTaintSink> taintSinks) {
      Map<
              BamLocationDependentJvmMemoryLocation<SetAbstractState<JvmTaintSource>>,
              List<JvmTaintSink>>
          endpointToSinks = new HashMap<>();
      Map<Signature, Map<JvmTaintSink, Set<JvmMemoryLocation>>> fqnToSinkLocations =
          JvmTaintSink.convertSinksToMemoryLocations(taintSinks);

      // find reached taint sinks in all cached reached sets
      executedTaintCpa.getCache().values().stream()
          .map(BlockAbstraction::getReachedSet)
          .forEach(
              reachedSet ->
                  reachedSet
                      .asCollection()
                      .forEach(
                          state ->
                              state
                                  .getProgramLocation()
                                  .getLeavingEdges()
                                  .forEach(
                                      edge ->
                                          createEndpointsForEdgeIfTainted(
                                              reachedSet,
                                              state,
                                              edge,
                                              fqnToSinkLocations,
                                              endpointToSinks))));

      return endpointToSinks;
    }

    /**
     * Creates a endpoint (entry point of the {@link JvmMemoryLocationCpa}) for each tainted
     * location of a sink.
     *
     * @param reachedSet A reached set containing the abstraction for one (or multiple if the entry
     *     states match) method calls
     * @param state A state that has to be checked to be a sink reached by a taint
     * @param edge A CFA edge that will be checked if it corresponds to a sink
     * @param signatureToSinkLocations A map from {@link Signature}s to corresponding {@link
     *     JvmTaintSink}s to all the locations that trigger the sink if tainted
     * @param endPointToSinks A mapping from the detected endpoints to corresponding sinks. In case
     *     of tainted sink locations new states are added here
     */
    private void createEndpointsForEdgeIfTainted(
        ProgramLocationDependentReachedSet<JvmAbstractState<SetAbstractState<JvmTaintSource>>>
            reachedSet,
        JvmAbstractState<SetAbstractState<JvmTaintSource>> state,
        JvmCfaEdge edge,
        Map<Signature, Map<JvmTaintSink, Set<JvmMemoryLocation>>> signatureToSinkLocations,
        Map<
                BamLocationDependentJvmMemoryLocation<SetAbstractState<JvmTaintSource>>,
                List<JvmTaintSink>>
            endPointToSinks) {
      signatureToSinkLocations
          .getOrDefault(edge.targetSignature(), Collections.emptyMap())
          .entrySet()
          .stream()
          .filter(sinkToMemoryLocations -> sinkToMemoryLocations.getKey().matchCfaEdge(edge))
          .forEach(
              sinkToMemoryLocations ->
                  sinkToMemoryLocations.getValue().stream()
                      .filter(
                          memoryLocation ->
                              isStateTaintedForMemoryLocation(
                                  state, memoryLocation, sinkToMemoryLocations.getKey()))
                      .forEach(
                          memoryLocation ->
                              createAndAddEndpoint(
                                  reachedSet,
                                  state,
                                  memoryLocation,
                                  sinkToMemoryLocations.getKey(),
                                  endPointToSinks)));
    }

    /**
     * Creates and adds an endpoint for each taint sink corresponding to a CFA edge triggered by a
     * taint.
     *
     * @param reachedSet A reached set containing the abstraction for one (or multiple if the entry
     *     states match) method calls
     * @param state A state where a sink is reached by a taint
     * @param taintLocation A sensitive location where the taint reaches the sink
     * @param sink A sink reached by a taint
     * @param endPointToSinks A mapping from the detected endpoints to corresponding sinks. The new
     *     state is added here
     */
    private void createAndAddEndpoint(
        ProgramLocationDependentReachedSet<JvmAbstractState<SetAbstractState<JvmTaintSource>>>
            reachedSet,
        JvmAbstractState<SetAbstractState<JvmTaintSource>> state,
        JvmMemoryLocation taintLocation,
        JvmTaintSink sink,
        Map<
                BamLocationDependentJvmMemoryLocation<SetAbstractState<JvmTaintSource>>,
                List<JvmTaintSink>>
            endPointToSinks) {
      BamLocationDependentJvmMemoryLocation<SetAbstractState<JvmTaintSource>> memoryLocation =
          new BamLocationDependentJvmMemoryLocation<>(
              taintLocation, state.getProgramLocation(), reachedSet);
      endPointToSinks.computeIfAbsent(memoryLocation, x -> new ArrayList<>()).add(sink);
    }

    private boolean isStateTaintedForMemoryLocation(
        JvmAbstractState<SetAbstractState<JvmTaintSource>> state,
        JvmMemoryLocation memoryLocation,
        JvmTaintSink sink) {
      SetAbstractState<JvmTaintSource> extractedState =
          memoryLocation.extractValueOrDefault(state, SetAbstractState.bottom);

      return extractedState.stream().anyMatch(sink.isValidForSource);
    }
  }

  /**
   * This result provides the witness trace for the taint analysis (i.e., the steps from source to
   * sink).
   *
   * <p>The witness traces are computed when a {@link TraceReconstructionResult} is initialized.
   */
  private static class TraceReconstructionResult
      implements TraceExtractor<SetAbstractState<JvmTaintSource>> {

    private final ProgramLocationDependentReachedSet<
            JvmMemoryLocationAbstractState<SetAbstractState<JvmTaintSource>>>
        traceReconstructionReachedSet;
    private final TaintAnalysisResult taintAnalysisResult;

    private TraceReconstructionResult(
        JvmMemoryLocationCpa<SetAbstractState<JvmTaintSource>> traceReconstructionCpa,
        TaintAnalysisResult taintAnalysisResult) {
      this.traceReconstructionReachedSet =
          runTraceReconstruction(traceReconstructionCpa, taintAnalysisResult);
      this.taintAnalysisResult = taintAnalysisResult;
    }

    private ProgramLocationDependentReachedSet<
            JvmMemoryLocationAbstractState<SetAbstractState<JvmTaintSource>>>
        runTraceReconstruction(
            JvmMemoryLocationCpa<SetAbstractState<JvmTaintSource>> traceCpa,
            TaintAnalyzerResult.TaintAnalysisResult taintAnalysisResult) {
      CpaAlgorithm<JvmMemoryLocationAbstractState<SetAbstractState<JvmTaintSource>>> cpaAlgorithm =
          new CpaAlgorithm<>(traceCpa);

      Waitlist<JvmMemoryLocationAbstractState<SetAbstractState<JvmTaintSource>>> waitlist =
          new BreadthFirstWaitlist<>();
      ProgramLocationDependentReachedSet<
              JvmMemoryLocationAbstractState<SetAbstractState<JvmTaintSource>>>
          reachedSet = new ProgramLocationDependentReachedSet<>();

      List<JvmMemoryLocationAbstractState<SetAbstractState<JvmTaintSource>>> initialStates =
          // This triggers the endpoints computation, unless they are already available
          taintAnalysisResult.getEndpoints().stream()
              .map(JvmMemoryLocationAbstractState::new)
              .collect(Collectors.toList());

      waitlist.addAll(initialStates);
      reachedSet.addAll(initialStates);

      // TODO: move abortOperator to the ConfigurableProgramAnalysis interface
      AbortOperator abortOperator = traceCpa.getAbortOperator();

      cpaAlgorithm.run(reachedSet, waitlist, abortOperator);
      return reachedSet;
    }

    /** Get locations where sinks have been triggered by a valid source. */
    @Override
    public Collection<BamLocationDependentJvmMemoryLocation<SetAbstractState<JvmTaintSource>>>
        getEndPoints() {
      return taintAnalysisResult.getEndpoints();
    }

    /**
     * Gets the states of the {@link JvmMemoryLocationCpa} containing the witness trace for the
     * taint analysis.
     */
    @Override
    public ProgramLocationDependentReachedSet<
            JvmMemoryLocationAbstractState<SetAbstractState<JvmTaintSource>>>
        getTraceReconstructionReachedSet() {
      return traceReconstructionReachedSet;
    }
  }
}
