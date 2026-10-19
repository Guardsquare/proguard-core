package proguard.analysis.cpa.jvm.domain.taint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.DefaultExpandOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Signature;
import proguard.classfile.instruction.Instruction;

/**
 * This {@link proguard.analysis.cpa.bam.ExpandOperator} inherits all the functionalities of a
 * {@link DefaultExpandOperator} and in addition taints the return values if the called function is
 * a source.
 */
public class TaintExpandOperator extends DefaultExpandOperator<SetAbstractState<TaintSource>> {

  private final Map<Signature, Set<TaintSource>> signaturesToSources;

  /**
   * Create the operator specifying the taint sources.
   *
   * @param cfa the control flow automaton of the analyzed program.
   * @param signaturesToSources a mapping from method signatures to their {@link TaintSource}
   * @param expandHeap whether expansion of the heap is performed
   */
  public TaintExpandOperator(
      JvmCfa cfa, Map<Signature, Set<TaintSource>> signaturesToSources, boolean expandHeap) {
    super(cfa, expandHeap);
    this.signaturesToSources = signaturesToSources;
  }

  /**
   * Create the operator specifying the taint sources.
   *
   * @param cfa the control flow automaton of the analyzed program.
   * @param signaturesToSources a mapping from method signatures to their {@link TaintSource}
   */
  public TaintExpandOperator(JvmCfa cfa, Map<Signature, Set<TaintSource>> signaturesToSources) {
    this(cfa, signaturesToSources, true);
  }

  // implementations for DefaultExpandOperator

  @Override
  public JvmAbstractState<SetAbstractState<TaintSource>> expand(
      JvmAbstractState<SetAbstractState<TaintSource>> expandedInitialState,
      JvmAbstractState<SetAbstractState<TaintSource>> reducedExitState,
      JvmCfaNode blockEntryNode,
      Call call) {
    JvmAbstractState<SetAbstractState<TaintSource>> result =
        super.expand(expandedInitialState, reducedExitState, blockEntryNode, call);
    List<JvmMethodTaintSource> detectedSources =
        signaturesToSources.getOrDefault(call.getTarget(), Collections.emptySet()).stream()
            .filter(JvmMethodTaintSource.class::isInstance)
            .map(JvmMethodTaintSource.class::cast)
            .filter(s -> s.callMatcher.map(m -> m.test(call)).orElse(true))
            .collect(Collectors.toList());
    if (detectedSources.isEmpty()) {
      return result;
    }
    // taint static fields
    Map<String, SetAbstractState<TaintSource>> fqnToValue = new HashMap<>();
    detectedSources.stream()
        .filter(s -> !s.getTaintGlobals().isEmpty())
        .forEach(
            s -> {
              SetAbstractState<TaintSource> newValue = new SetAbstractState<>(s);
              s.getTaintGlobals()
                  .forEach(fqn -> fqnToValue.merge(fqn, newValue, SetAbstractState::join));
            });
    fqnToValue.forEach((fqn, value) -> result.setStatic(fqn, value, SetAbstractState.bottom()));

    return result;
  }

  /**
   * The calculation of return values supports tainting it in case the analyzed method is a taint
   * source.
   */
  @Override
  protected List<SetAbstractState<TaintSource>> calculateReturnValues(
      JvmAbstractState<SetAbstractState<TaintSource>> reducedExitState,
      Instruction returnInstruction,
      Call call) {
    List<JvmMethodTaintSource> detectedSources =
        signaturesToSources.getOrDefault(call.getTarget(), Collections.emptySet()).stream()
            .filter(JvmMethodTaintSource.class::isInstance)
            .map(JvmMethodTaintSource.class::cast)
            .filter(s -> s.callMatcher.map(m -> m.test(call)).orElse(true))
            .filter(s -> s.getTaintsReturn())
            .collect(Collectors.toList());

    if (detectedSources.isEmpty()) {
      return super.calculateReturnValues(reducedExitState, returnInstruction, call);
    }

    List<SetAbstractState<TaintSource>> returnValues = new ArrayList<>();

    SetAbstractState<TaintSource> answerContent = new SetAbstractState<>(detectedSources);
    int returnSize = returnInstruction.stackPopCount(null);
    for (int i = 0; i < returnSize; i++) {
      SetAbstractState<TaintSource> returnByte = reducedExitState.peek(i);
      answerContent = answerContent.join(returnByte);
    }

    // pad to meet the return type size and append the abstract state
    for (int i = returnSize; i > 1; i--) {
      returnValues.add(SetAbstractState.bottom());
    }
    if (returnSize > 0) {
      returnValues.add(answerContent);
    }

    return returnValues;
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

  /** Returns the mapping from fqns to taint sources. */
  public Map<Signature, Set<TaintSource>> getSignaturesToSources() {
    return signaturesToSources;
  }
}
