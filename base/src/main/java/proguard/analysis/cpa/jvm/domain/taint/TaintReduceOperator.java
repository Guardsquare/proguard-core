package proguard.analysis.cpa.jvm.domain.taint;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.DefaultReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.classfile.Signature;

/**
 * This {@link proguard.analysis.cpa.bam.ReduceOperator} inherits all the functionalities of a
 * {@link DefaultReduceOperator} while adding taint-analysis specific functionalities.
 */
public class TaintReduceOperator extends DefaultReduceOperator<SetAbstractState<TaintSource>> {

  private final Map<Signature, Set<TaintSource>> taintSourcesTaintArgs;

  /**
   * Create the taint reduce operator for the JVM.
   *
   * @param reduceHeap whether reduction of the heap is performed
   */
  public TaintReduceOperator(boolean reduceHeap) {
    super(reduceHeap);
    taintSourcesTaintArgs = new HashMap<>();
  }

  /**
   * Create the taint reduce operator for the JVM.
   *
   * @param reduceHeap whether reduction of the heap is performed
   * @param taintSources collection of taint sources
   */
  public TaintReduceOperator(boolean reduceHeap, Map<Signature, Set<TaintSource>> taintSources) {
    super(reduceHeap);
    this.taintSourcesTaintArgs = taintSources;
  }

  // implementations for JvmAbstractStateFactory

  @Override
  protected JvmAbstractState<SetAbstractState<TaintSource>> createJvmAbstractState(
      JvmCfaNode programLocation,
      JvmFrameAbstractState<SetAbstractState<TaintSource>> frame,
      JvmHeapAbstractState<SetAbstractState<TaintSource>> heap,
      MapAbstractState<String, SetAbstractState<TaintSource>> staticFields) {
    return new JvmAbstractState<>(programLocation, frame, heap, staticFields);
  }

  @Override
  public JvmAbstractState<SetAbstractState<TaintSource>> onMethodEntry(
      JvmAbstractState<SetAbstractState<TaintSource>> reducedState, boolean isCallStatic) {
    JvmFrameAbstractState<SetAbstractState<TaintSource>> frame = reducedState.getFrame();
    JvmCfaNode programLocation = reducedState.getProgramLocation();
    if (taintSourcesTaintArgs.containsKey(programLocation.getSignature())) {
      taintSourcesTaintArgs.get(programLocation.getSignature()).stream()
          .filter(JvmMethodTaintSource.class::isInstance)
          .map(JvmMethodTaintSource.class::cast)
          .forEach(
              source ->
                  source
                      .getTaintArgs()
                      .forEach(
                          index ->
                              updateTaintState(frame, isCallStatic ? index - 1 : index, source)));
    }
    return new JvmAbstractState<>(
        programLocation, frame, reducedState.getHeap(), reducedState.getStaticFields());
  }

  private void updateTaintState(
      JvmFrameAbstractState<SetAbstractState<TaintSource>> frame,
      int index,
      JvmMethodTaintSource source) {
    SetAbstractState<TaintSource> oldState =
        frame.getLocalVariables().getOrDefault(index, SetAbstractState.bottom());
    SetAbstractState<TaintSource> newState = oldState.join(new SetAbstractState<>(source));
    frame.getLocalVariables().set(index, newState, SetAbstractState.bottom());
  }
}
