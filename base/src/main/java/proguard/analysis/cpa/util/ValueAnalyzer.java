package proguard.analysis.cpa.util;

import static proguard.analysis.cpa.jvm.domain.value.ValueAbstractState.UNKNOWN;

import java.util.function.Function;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.bam.BamCache;
import proguard.analysis.cpa.bam.BamCacheImpl;
import proguard.analysis.cpa.bam.BamCpa;
import proguard.analysis.cpa.bam.CpaWithBamOperators;
import proguard.analysis.cpa.bam.NoOpRebuildOperator;
import proguard.analysis.cpa.defaults.DepthFirstWaitlist;
import proguard.analysis.cpa.defaults.HashMapAbstractState;
import proguard.analysis.cpa.defaults.MergeJoinOperator;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StaticPrecisionAdjustment;
import proguard.analysis.cpa.defaults.StopJoinOperator;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.TransferRelation;
import proguard.analysis.cpa.interfaces.Waitlist;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.value.JvmCfaReferenceValueFactory;
import proguard.analysis.cpa.jvm.domain.value.JvmValueAbstractState;
import proguard.analysis.cpa.jvm.domain.value.JvmValueTransferRelation;
import proguard.analysis.cpa.jvm.domain.value.ValueAbstractState;
import proguard.analysis.cpa.jvm.domain.value.ValueExpandOperator;
import proguard.analysis.cpa.jvm.domain.value.ValueReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmShallowHeapAbstractState;
import proguard.classfile.ClassPool;
import proguard.classfile.MethodSignature;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.ParticularValueFactory;
import proguard.evaluation.value.ValueFactory;

/**
 * Helper class to analyze values (e.g., function parameters) in a program inter-procedurally.
 *
 * <p>The analyzer can be configured in several ways via {@link ValueAnalyzer.Builder}.
 *
 * <p>The {@link ValueAnalyzer#analyze(MethodSignature)} method can be called to perform the
 * configured analysis starting from a given method in the program.
 *
 * <p>The same analyzer can be used to analyze several methods in sequence, in this case the
 * analysis {@link BamCache} will be shared between the sequential analyses making them potentially
 * avoid recalculating the results when a method is called again with known parameters.
 *
 * <p>This might sometimes not be the desired behavior, since the cache might take a lot of memory.
 * If this is a concern rebuilding the {@link ValueAnalyzer} from the original {@link
 * ValueAnalyzer.Builder} will provide a fresh cache.
 *
 * <p>{@link ValueAnalyzer} is currently not designed to be thread safe. Among the known reasons,
 * the currently available {@link BamCache}s are not designed for concurrent access and a different
 * {@link ExecutingInvocationUnit} (used by {@link JvmValueTransferRelation}) should be used by each
 * thread.
 */
public class ValueAnalyzer {

  private final Function<MethodSignature, BamCpa<ValueAbstractState>> cpaCreator;
  private final Function<MethodSignature, JvmValueAbstractState> initialStateCreator;

  private ValueAnalyzer(
      Function<MethodSignature, BamCpa<ValueAbstractState>> cpaCreator,
      Function<MethodSignature, JvmValueAbstractState> initialStateCreator) {
    this.cpaCreator = cpaCreator;
    this.initialStateCreator = initialStateCreator;
  }

  /**
   * Run the value analysis on the given method.
   *
   * <p>The results are not intended as just for the last execution, but as a view on the full
   * analysis' cache.
   *
   * <p>Since the cache at the moment has no capability to remember the last execution, the result
   * will change as the cache changes (i.e., after calling this again on a new method, old instances
   * of {@link ValueAnalysisResult} will also be updated).
   *
   * @param mainSignature the signature of the method to analyze.
   * @return the result of the analysis.
   */
  public ValueAnalysisResult analyze(MethodSignature mainSignature) {
    BamCpa<ValueAbstractState> cpa = cpaCreator.apply(mainSignature);
    CpaAlgorithm<JvmAbstractState<ValueAbstractState>> cpaAlgorithm = new CpaAlgorithm<>(cpa);

    Waitlist<JvmAbstractState<ValueAbstractState>> waitList = new DepthFirstWaitlist<>();
    ReachedSet<JvmAbstractState<ValueAbstractState>> reachedSet =
        new ProgramLocationDependentReachedSet<>();

    JvmValueAbstractState initialState = initialStateCreator.apply(mainSignature);
    waitList.add(initialState);
    reachedSet.add(initialState);

    cpaAlgorithm.run(reachedSet, waitList);
    return new ValueAnalysisResult(cpa);
  }

  /**
   * Provides results for the analysis.
   *
   * <p>The results are not intended as just for the last execution of {@link
   * ValueAnalyzer#analyze(MethodSignature)}, but as a view on the full analysis' cache.
   *
   * <p>Since the cache at the moment has no capability to remember the last execution, the result
   * will change as the cache changes (i.e., after calling {@link
   * ValueAnalyzer#analyze(MethodSignature)} on a new method, old instances of {@link
   * ValueAnalysisResult} will also be updated).
   */
  public static class ValueAnalysisResult {
    private final BamCache<ValueAbstractState> resultCache;

    private ValueAnalysisResult(BamCpa<ValueAbstractState> executedCpa) {
      resultCache = executedCpa.getCache();
    }

    /**
     * Returns the cache of the analysis. NB: the returned cache is updated every time {@link
     * ValueAnalyzer#analyze(MethodSignature)} is called.
     *
     * <p>While initially this is the only way to access the results, direct access to the cache is
     * discouraged as soon as more fine-grained access to the result is available.
     */
    public BamCache<ValueAbstractState> getResultCache() {
      return resultCache;
    }
  }

  /**
   * Class to configure and build a {@link ValueAnalyzer}.
   *
   * <p>Each separate built {@link ValueAnalyzer} uses its own {@link BamCache}, so different
   * analyzers won't share results, while all execution of {@link
   * ValueAnalyzer#analyze(MethodSignature)} from the same analyzer will use the same cache.
   */
  public static class Builder {
    private final JvmCfa cfa;
    private int maxCallStackDepth = 10;
    private AbortOperator abortOperator = NeverAbortOperator.INSTANCE;
    private final ExecutingInvocationUnit.Builder invocationUnitBuilder;

    /**
     * Create a builder for a {@link ValueAnalyzer} using a default {@link ExecutingInvocationUnit}.
     *
     * <p>The {@link proguard.evaluation.InvocationUnit} defines how to handle invocations of {@link
     * proguard.classfile.LibraryMethod}s through custom {@link
     * proguard.evaluation.executor.Executor}s.
     *
     * <p>The only executor for the default invocation unit is {@link
     * proguard.evaluation.executor.StringReflectionExecutor}.
     */
    public Builder(JvmCfa cfa, ClassPool programClassPool, ClassPool libraryClassPool) {
      this.cfa = cfa;
      this.invocationUnitBuilder =
          new ExecutingInvocationUnit.Builder(programClassPool, libraryClassPool)
              .setEnableSameInstanceIdApproximation(true);
    }

    /**
     * Create a builder for a {@link ValueAnalyzer} using a custom {@link ExecutingInvocationUnit}.
     *
     * <p>The {@link proguard.evaluation.InvocationUnit} defines how to handle invocations of {@link
     * proguard.classfile.LibraryMethod}s.
     */
    public Builder(JvmCfa cfa, ExecutingInvocationUnit.Builder invocationUnitBuilder) {
      this.cfa = cfa;
      this.invocationUnitBuilder = invocationUnitBuilder;
    }

    /** Build a {@link ValueAnalyzer}. */
    public ValueAnalyzer build() {
      ValueFactory valueFactory = new ParticularValueFactory(new JvmCfaReferenceValueFactory(cfa));
      ExecutingInvocationUnit invocationUnit = invocationUnitBuilder.build(valueFactory);
      TransferRelation<JvmAbstractState<ValueAbstractState>> valueTransferRelation =
          new JvmValueTransferRelation(valueFactory, invocationUnit);

      ConfigurableProgramAnalysis<JvmAbstractState<ValueAbstractState>> intraproceduralCpa =
          new SimpleCpa<>(
              valueTransferRelation,
              new MergeJoinOperator<>(),
              new StopJoinOperator<>(),
              new StaticPrecisionAdjustment(),
              NeverAbortOperator.INSTANCE);

      boolean reduceHeap = true;
      CpaWithBamOperators<ValueAbstractState> interProceduralCpa =
          new CpaWithBamOperators<>(
              intraproceduralCpa,
              new ValueReduceOperator(valueFactory, invocationUnit, reduceHeap),
              new ValueExpandOperator(valueFactory, invocationUnit, cfa, reduceHeap),
              new NoOpRebuildOperator());
      BamCache<ValueAbstractState> cache = new BamCacheImpl<>();

      return new ValueAnalyzer(
          mainMethodSignature ->
              new BamCpa<>(interProceduralCpa, cfa, mainMethodSignature, cache, maxCallStackDepth),
          mainMethodSignature ->
              new JvmValueAbstractState(
                  valueFactory,
                  invocationUnit,
                  cfa.getFunctionEntryNode(mainMethodSignature),
                  new JvmFrameAbstractState<>(),
                  new JvmShallowHeapAbstractState<>(
                      new HashMapAbstractState<>(), JvmCfaNode.class, UNKNOWN),
                  new HashMapAbstractState<>()));
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

    /**
     * Sets the abort operator to allow premature CPA algorithm termination.
     *
     * <p>The default operator never aborts the analysis.
     *
     * @param abortOperator an {@link AbortOperator}.
     * @return this {@link ValueAnalyzer} builder.
     */
    public Builder setAbortOperator(AbortOperator abortOperator) {
      this.abortOperator = abortOperator;
      return this;
    }
  }
}
