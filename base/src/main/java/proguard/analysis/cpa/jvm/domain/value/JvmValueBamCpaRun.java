package proguard.analysis.cpa.jvm.domain.value;

import static java.util.Collections.singletonList;
import static proguard.analysis.cpa.jvm.domain.value.ValueAbstractState.UNKNOWN;
import static proguard.exception.ErrorId.ANALYSIS_JVM_VALUE_BAM_CPA_RUN_SET_CFA_UNSUPPORTED;

import java.util.Collection;
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.DepthFirstWaitlist;
import proguard.analysis.cpa.defaults.HashMapAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.MergeJoinOperator;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StaticPrecisionAdjustment;
import proguard.analysis.cpa.defaults.StopJoinOperator;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.TransferRelation;
import proguard.analysis.cpa.interfaces.Waitlist;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.HeapModel;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmShallowHeapAbstractState;
import proguard.analysis.cpa.jvm.util.JvmBamCpaRun;
import proguard.classfile.ClassPool;
import proguard.classfile.MethodSignature;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.ParticularValueFactory;
import proguard.evaluation.value.ValueFactory;
import proguard.exception.ProguardCoreException;

/**
 * This run wraps the execution of BAM JVM Value Analysis CPA (see {@link JvmValueAbstractState}).
 */
public class JvmValueBamCpaRun
    extends JvmBamCpaRun<SimpleCpa, JvmAbstractState<ValueAbstractState>, JvmValueAbstractState> {
  public interface TransferRelationFactory {
    TransferRelation newTransferRelation(ValueFactory vf, ExecutingInvocationUnit eiu);
  }

  private final TransferRelationFactory transferRelationFactory;
  private final MethodSignature mainMethodSignature;
  private final ValueFactory valueFactory;
  private final ExecutingInvocationUnit executingInvocationUnit;
  private final JvmHeapAbstractState<ValueAbstractState> heap =
      new JvmShallowHeapAbstractState<>(new HashMapAbstractState<>(), JvmCfaNode.class, UNKNOWN);
  private final MapAbstractState<String, ValueAbstractState> staticFields;

  private JvmValueBamCpaRun(
      JvmCfa cfa,
      MethodSignature mainMethodSignature,
      ValueFactory valueFactory,
      TransferRelationFactory transferRelationFactory,
      ExecutingInvocationUnit executingInvocationUnit,
      int maxCallStackDepth,
      HeapModel heapModel,
      MapAbstractState<String, ValueAbstractState> staticFields,
      AbortOperator abortOperator,
      boolean reduceHeap) {
    super(cfa, maxCallStackDepth, heapModel, abortOperator, reduceHeap);
    this.valueFactory = valueFactory;
    this.transferRelationFactory = transferRelationFactory;
    this.executingInvocationUnit = executingInvocationUnit;
    this.mainMethodSignature = mainMethodSignature;
    this.staticFields = staticFields;
  }

  @Override
  public SimpleCpa createIntraproceduralCPA() {
    DelegateAbstractDomain<ValueAbstractState> abstractDomain = new DelegateAbstractDomain<>();
    return new SimpleCpa(
        abstractDomain,
        transferRelationFactory.newTransferRelation(valueFactory, executingInvocationUnit),
        new MergeJoinOperator(abstractDomain),
        new StopJoinOperator(abstractDomain),
        new StaticPrecisionAdjustment());
  }

  @Override
  public ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> createReduceOperator() {
    return new JvmValueReduceOperator(valueFactory, executingInvocationUnit, reduceHeap);
  }

  @Override
  protected ReachedSet createReachedSet() {
    return new ProgramLocationDependentReachedSet<
        JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature>();
  }

  protected Waitlist createWaitlist() {
    return new DepthFirstWaitlist();
  }

  @Override
  public ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> createExpandOperator() {
    return new JvmValueExpandOperator(valueFactory, executingInvocationUnit, cfa, reduceHeap);
  }

  @Override
  public MethodSignature getMainSignature() {
    return mainMethodSignature;
  }

  @Override
  public Collection<JvmValueAbstractState> getInitialStates() {
    return singletonList(
        new JvmValueAbstractState(
            valueFactory,
            executingInvocationUnit,
            cfa.getFunctionEntryNode(getMainSignature()),
            new JvmFrameAbstractState<>(),
            heap,
            staticFields));
  }

  public static class Builder extends JvmBamCpaRun.Builder {

    private final ClassPool programClassPool;
    private final ClassPool libraryClassPool;
    private MethodSignature mainSignature;
    private ValueFactory valueFactory;
    private TransferRelationFactory transferRelationFactory;
    private MapAbstractState<String, ValueAbstractState> staticFields =
        new HashMapAbstractState<>();

    public Builder(
        ClassPool programClassPool,
        ClassPool libraryClassPool,
        JvmCfa cfa,
        MethodSignature mainSignature) {
      this.programClassPool = programClassPool;
      this.libraryClassPool = libraryClassPool;
      super.heapModel = HeapModel.SHALLOW;
      super.maxCallStackDepth = 10;
      super.cfa = cfa;
      this.valueFactory = new ParticularValueFactory(new JvmCfaReferenceValueFactory(cfa));
      this.mainSignature = mainSignature;
      this.transferRelationFactory = Builder::defaultTransferRelationFactory;
    }

    private static JvmValueTransferRelation defaultTransferRelationFactory(
        ValueFactory valueFactory, ExecutingInvocationUnit executingInvocationUnit) {
      return new JvmValueTransferRelation(valueFactory, executingInvocationUnit);
    }

    @Override
    public JvmValueBamCpaRun build() {
      return new JvmValueBamCpaRun(
          cfa,
          mainSignature,
          valueFactory,
          transferRelationFactory,
          new ExecutingInvocationUnit.Builder(programClassPool, libraryClassPool)
              .setEnableSameInstanceIdApproximation(true)
              .build(valueFactory),
          maxCallStackDepth,
          heapModel,
          staticFields,
          abortOperator,
          reduceHeap);
    }

    @Override
    public Builder setCfa(JvmCfa cfa) {
      // Don't allow setting the CFA here because it could
      // result in a different CFA than the one used for the default
      // ValueFactory.
      throw new ProguardCoreException.Builder(
              "CFA should only be set via the Builder constructor",
              ANALYSIS_JVM_VALUE_BAM_CPA_RUN_SET_CFA_UNSUPPORTED)
          .build();
    }

    public Builder setMainSignature(MethodSignature mainSignature) {
      this.mainSignature = mainSignature;
      return this;
    }

    public Builder setValueFactory(ValueFactory valueFactory) {
      this.valueFactory = valueFactory;
      return this;
    }

    public Builder setTransferRelationFactory(TransferRelationFactory transferRelationFactory) {
      this.transferRelationFactory = transferRelationFactory;
      return this;
    }

    public Builder setStaticFields(MapAbstractState<String, ValueAbstractState> staticFields) {
      this.staticFields = staticFields;
      return this;
    }

    @Override
    public Builder setAbortOperator(AbortOperator abortOperator) {
      this.abortOperator = abortOperator;
      return this;
    }

    @Override
    public Builder setReduceHeap(boolean reduceHeap) {
      this.reduceHeap = reduceHeap;
      return this;
    }

    @Override
    public Builder setMaxCallStackDepth(int maxCallStackDepth) {
      return (Builder) super.setMaxCallStackDepth(maxCallStackDepth);
    }
  }
}
