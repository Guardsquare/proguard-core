package proguard.analysis.cpa.jvm.domain.value;

import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.JvmDefaultExpandOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.evaluation.value.ValueFactory;

/**
 * A {@link JvmDefaultExpandOperator} that creates {@link JvmValueAbstractState}s.
 */
public class JvmValueExpandOperator extends JvmDefaultExpandOperator<JvmAbstractState<ValueAbstractState>>
{
    private final ValueFactory valueFactory;

    public JvmValueExpandOperator(ValueFactory valueFactory, JvmCfa cfa)
    {
        super(cfa);
        this.valueFactory = valueFactory;
    }

    @Override
    public JvmValueAbstractState createJvmAbstractState(JvmCfaNode programLocation, JvmFrameAbstractState frame, JvmHeapAbstractState heap, MapAbstractState staticFields)
    {
        return new JvmValueAbstractState(valueFactory, programLocation, frame, heap, staticFields);
    }
}
