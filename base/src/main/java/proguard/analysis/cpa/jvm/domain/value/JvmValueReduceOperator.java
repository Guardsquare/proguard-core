package proguard.analysis.cpa.jvm.domain.value;

import java.util.HashSet;
import java.util.Set;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.JvmDefaultReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;

/**
 * A {@link JvmDefaultReduceOperator} that creates {@link JvmValueAbstractState}s.
 */
public class JvmValueReduceOperator extends JvmDefaultReduceOperator<ValueAbstractState>
{
    private final ValueFactory valueFactory;
    private final ExecutingInvocationUnit executingInvocationUnit;

    public JvmValueReduceOperator(ValueFactory valueFactory, ExecutingInvocationUnit executingInvocationUnit)
    {
        this(valueFactory, executingInvocationUnit, true);
    }

    public JvmValueReduceOperator(ValueFactory valueFactory, ExecutingInvocationUnit executingInvocationUnit, boolean reduceHeap)
    {
        super(reduceHeap);
        this.valueFactory            = valueFactory;
        this.executingInvocationUnit = executingInvocationUnit;
    }

    @Override
    protected void reduceHeap(JvmHeapAbstractState<ValueAbstractState>     heap,
                              JvmFrameAbstractState<ValueAbstractState>    reducedFrame,
                              MapAbstractState<String, ValueAbstractState> reducedStaticFields)
    {
        Set<Object> referencesToKeep = new HashSet<>();
        for (ValueAbstractState parameter : reducedFrame.getLocalVariables())
        {
            Value value = parameter.getValue();
            if (value instanceof IdentifiedReferenceValue)
            {
                referencesToKeep.add(((IdentifiedReferenceValue) value).id);
            }
        }
        for (ValueAbstractState field : reducedStaticFields.values())
        {
            Value value = field.getValue();
            if (value instanceof IdentifiedReferenceValue)
            {
                referencesToKeep.add(((IdentifiedReferenceValue) value).id);
            }
        }

        heap.reduce(referencesToKeep);
    }

    @Override
    public JvmValueAbstractState createJvmAbstractState(JvmCfaNode programLocation, JvmFrameAbstractState frame, JvmHeapAbstractState heap, MapAbstractState staticFields)
    {
        return new JvmValueAbstractState(valueFactory, executingInvocationUnit, programLocation, frame, heap, staticFields);
    }
}
