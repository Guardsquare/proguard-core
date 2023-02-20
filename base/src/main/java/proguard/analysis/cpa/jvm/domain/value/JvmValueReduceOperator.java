package proguard.analysis.cpa.jvm.domain.value;

import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.JvmDefaultReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.evaluation.value.ValueFactory;

/**
 * A {@link JvmDefaultReduceOperator} that creates {@link JvmValueAbstractState}s.
 */
public class JvmValueReduceOperator extends JvmDefaultReduceOperator<JvmAbstractState<ValueAbstractState>>
{
    private final ValueFactory valueFactory;

    public JvmValueReduceOperator(ValueFactory valueFactory)
    {
        this.valueFactory = valueFactory;
    }

    @Override
    public JvmValueAbstractState createJvmAbstractState(JvmCfaNode programLocation, JvmFrameAbstractState frame, JvmHeapAbstractState heap, MapAbstractState staticFields)
    {
        return new JvmValueAbstractState(valueFactory, programLocation, frame, heap, staticFields);
    }
}
