package proguard.analysis.cpa.jvm.domain.value;

import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.DefaultReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ValueFactory;

/** A {@link DefaultReduceOperator} that creates {@link JvmValueAbstractState}s. */
public class ValueReduceOperator extends DefaultReduceOperator<ValueAbstractState> {
  private final ValueFactory valueFactory;
  private final ExecutingInvocationUnit executingInvocationUnit;

  public ValueReduceOperator(
      ValueFactory valueFactory, ExecutingInvocationUnit executingInvocationUnit) {
    this(valueFactory, executingInvocationUnit, true);
  }

  public ValueReduceOperator(
      ValueFactory valueFactory,
      ExecutingInvocationUnit executingInvocationUnit,
      boolean reduceHeap) {
    super(reduceHeap);
    this.valueFactory = valueFactory;
    this.executingInvocationUnit = executingInvocationUnit;
  }

  @Override
  protected void reduceHeap(
      JvmHeapAbstractState<ValueAbstractState> heap,
      JvmFrameAbstractState<ValueAbstractState> frame,
      MapAbstractState<String, ValueAbstractState> staticFields) {
    heap.reduce(
        Stream.of(frame.getLocalVariables(), staticFields.values())
            .flatMap(it -> it.stream().map(ValueAbstractState::getValue))
            // Only IdentifiedReferenceValue can point to something on the heap.
            .filter(IdentifiedReferenceValue.class::isInstance)
            // But Strings are never stored on the heap.
            .filter(it -> !TYPE_JAVA_LANG_STRING.equals(it.internalType()))
            // The key in the heap is the ID.
            .map(it -> ((IdentifiedReferenceValue) it).id)
            .collect(Collectors.toSet()));
  }

  @Override
  protected JvmAbstractState<ValueAbstractState> createJvmAbstractState(
      JvmCfaNode programLocation,
      JvmFrameAbstractState<ValueAbstractState> frame,
      JvmHeapAbstractState<ValueAbstractState> heap,
      MapAbstractState<String, ValueAbstractState> staticFields) {
    return new JvmValueAbstractState(
        valueFactory, executingInvocationUnit, programLocation, frame, heap, staticFields);
  }
}
