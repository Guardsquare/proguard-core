package proguard.analysis.cpa.jvm.domain.value;

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
import proguard.classfile.MethodSignature;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.ParticularValueFactory;
import proguard.evaluation.value.ValueFactory;

import java.util.Collection;

import static java.util.Collections.singletonList;
import static proguard.analysis.cpa.jvm.domain.value.ValueAbstractState.UNKNOWN;

/**
 * This run wraps the execution of BAM JVM Value Analysis CPA (see {@link JvmValueAbstractState}).
 */
public class JvmValueBamCpaRun
    extends JvmBamCpaRun<SimpleCpa, JvmAbstractState<ValueAbstractState>, JvmValueAbstractState>
{
    private final MethodSignature mainMethodSignature;
    private final ValueFactory valueFactory;
    private final ExecutingInvocationUnit executingInvocationUnit;
    private final JvmHeapAbstractState<ValueAbstractState> heap = new JvmShallowHeapAbstractState<>(new HashMapAbstractState<>(), JvmCfaNode.class, UNKNOWN);
    private final MapAbstractState<String, ValueAbstractState> staticFields;


    private JvmValueBamCpaRun(JvmCfa                                       cfa,
                              MethodSignature                              mainMethodSignature,
                              ValueFactory                                 valueFactory,
                              ExecutingInvocationUnit                      executingInvocationUnit,
                              int                                          maxCallStackDepth,
                              HeapModel                                    heapModel,
                              MapAbstractState<String, ValueAbstractState> staticFields,
                              AbortOperator                                abortOperator,
                              boolean                                      reduceHeap)
    {
        super(cfa, maxCallStackDepth, heapModel, abortOperator, reduceHeap);
        this.valueFactory            = valueFactory;
        this.executingInvocationUnit = executingInvocationUnit;
        this.mainMethodSignature     = mainMethodSignature;
        this.staticFields            = staticFields;
    }

    @Override
    public SimpleCpa createIntraproceduralCPA()
    {
        DelegateAbstractDomain<ValueAbstractState> abstractDomain = new DelegateAbstractDomain<>();
        return new SimpleCpa(
            abstractDomain,
            new JvmValueTransferRelation(valueFactory, executingInvocationUnit),
            new MergeJoinOperator(abstractDomain),
            new StopJoinOperator(abstractDomain),
            new StaticPrecisionAdjustment()
        );
    }

    @Override
    public ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> createReduceOperator()
    {
        return new JvmValueReduceOperator(valueFactory, executingInvocationUnit);
    }

    @Override
    protected ReachedSet createReachedSet()
    {
        return new ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature>();
    }

    protected Waitlist createWaitlist()
    {
        return new DepthFirstWaitlist();
    }

    @Override
    public ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> createExpandOperator()
    {
        return new JvmValueExpandOperator(valueFactory, executingInvocationUnit, cfa);
    }

    @Override
    public MethodSignature getMainSignature()
    {
        return mainMethodSignature;
    }

    @Override
    public Collection<JvmValueAbstractState> getInitialStates()
    {
        return singletonList(new JvmValueAbstractState(
            valueFactory,
            executingInvocationUnit,
            cfa.getFunctionEntryNode(getMainSignature()),
            new JvmFrameAbstractState<>(),
            heap,
            staticFields
        ));
    }

    public static class Builder extends JvmBamCpaRun.Builder
    {

        private final MethodSignature                              mainSignature;
        private       ValueFactory                                 valueFactory;
        private       MapAbstractState<String, ValueAbstractState> staticFields = new HashMapAbstractState<>();


        public Builder(JvmCfa cfa, MethodSignature mainSignature)
        {
            super.cfa               = cfa;
            super.heapModel         = HeapModel.SHALLOW;
            this.mainSignature      = mainSignature;
            super.maxCallStackDepth = 10;
        }

        @Override
        public JvmValueBamCpaRun build()
        {
            ValueFactory valueFactory = this.valueFactory == null ?
                    new ParticularValueFactory(new JvmCfaReferenceValueFactory(cfa)) :
                    this.valueFactory;
            return new JvmValueBamCpaRun(
                cfa,
                mainSignature,
                valueFactory,
                new ExecutingInvocationUnit(valueFactory),
                maxCallStackDepth,
                heapModel,
                staticFields,
                abortOperator,
                reduceHeap
            );
        }

        public Builder setValueFactory(ValueFactory valueFactory)
        {
            this.valueFactory = valueFactory;
            return this;
        }

        public Builder setStaticFields(MapAbstractState<String, ValueAbstractState> staticFields)
        {
            this.staticFields = staticFields;
            return this;
        }

        @Override
        public Builder setMaxCallStackDepth(int maxCallStackDepth)
        {
            return (Builder) super.setMaxCallStackDepth(maxCallStackDepth);
        }
    }
}
