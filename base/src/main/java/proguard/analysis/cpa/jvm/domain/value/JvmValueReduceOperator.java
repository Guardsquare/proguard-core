package proguard.analysis.cpa.jvm.domain.value;

import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.JvmDefaultReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.ValueFactory;

/**
 * A {@link JvmDefaultReduceOperator} that creates {@link JvmValueAbstractState}s.
 */
public class JvmValueReduceOperator extends JvmDefaultReduceOperator<JvmAbstractState<ValueAbstractState>>
{
    private final ValueFactory valueFactory;
    private final ExecutingInvocationUnit executingInvocationUnit;

    public JvmValueReduceOperator(ValueFactory valueFactory, ExecutingInvocationUnit executingInvocationUnit)
    {
        this.valueFactory            = valueFactory;
        this.executingInvocationUnit = executingInvocationUnit;
    }

    @Override
    public JvmValueAbstractState createJvmAbstractState(JvmCfaNode programLocation, JvmFrameAbstractState frame, JvmHeapAbstractState heap, MapAbstractState staticFields)
    {
        return new JvmValueAbstractState(valueFactory, executingInvocationUnit, programLocation, frame, heap, staticFields);
    }
}
