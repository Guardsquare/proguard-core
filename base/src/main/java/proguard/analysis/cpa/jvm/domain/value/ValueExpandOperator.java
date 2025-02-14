package proguard.analysis.cpa.jvm.domain.value;

import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.DefaultExpandOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.ValueFactory;

/** A {@link DefaultExpandOperator} that creates {@link JvmValueAbstractState}s. */
public class ValueExpandOperator extends DefaultExpandOperator<ValueAbstractState> {
  private final ValueFactory valueFactory;
  private final ExecutingInvocationUnit executingInvocationUnit;

  public ValueExpandOperator(
      ValueFactory valueFactory, ExecutingInvocationUnit executingInvocationUnit, JvmCfa cfa) {
    this(valueFactory, executingInvocationUnit, cfa, true);
  }

  public ValueExpandOperator(
      ValueFactory valueFactory,
      ExecutingInvocationUnit executingInvocationUnit,
      JvmCfa cfa,
      boolean expandHeap) {
    super(cfa, expandHeap);
    this.valueFactory = valueFactory;
    this.executingInvocationUnit = executingInvocationUnit;
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
