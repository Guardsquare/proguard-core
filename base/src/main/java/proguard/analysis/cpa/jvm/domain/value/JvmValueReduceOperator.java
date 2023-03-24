package proguard.analysis.cpa.jvm.domain.value;

import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.JvmDefaultReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ValueFactory;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING;

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
                              JvmFrameAbstractState<ValueAbstractState>    frame,
                              MapAbstractState<String, ValueAbstractState> staticFields)
    {
        heap.reduce(
            Stream.of(frame.getLocalVariables(), staticFields.values())
                    .flatMap(it -> it.stream().map(ValueAbstractState::getValue))
                    // Only IdentifiedReferenceValue point to something on the heap.
                    .filter(it -> it instanceof IdentifiedReferenceValue)
                    // But Strings are never stored on the heap.
                    .filter(it -> !TYPE_JAVA_LANG_STRING.equals(it.internalType()))
                    // The key in the heap is the ID.
                    .map(it -> ((IdentifiedReferenceValue) it).id)
                    .collect(Collectors.toSet())
        );
    }

    @Override
    public JvmValueAbstractState createJvmAbstractState(JvmCfaNode programLocation, JvmFrameAbstractState frame, JvmHeapAbstractState heap, MapAbstractState staticFields)
    {
        return new JvmValueAbstractState(valueFactory, executingInvocationUnit, programLocation, frame, heap, staticFields);
    }
}
