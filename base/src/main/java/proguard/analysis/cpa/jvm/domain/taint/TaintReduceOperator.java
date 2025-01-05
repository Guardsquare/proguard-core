/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.analysis.cpa.jvm.domain.taint;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
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
public class TaintReduceOperator extends DefaultReduceOperator<SetAbstractState<JvmTaintSource>> {

  private final Map<Signature, Set<JvmTaintSource>> taintSourcesTaintArgs;

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
  public TaintReduceOperator(boolean reduceHeap, Map<Signature, Set<JvmTaintSource>> taintSources) {
    super(reduceHeap);
    this.taintSourcesTaintArgs = taintSources;
  }

  // implementations for JvmAbstractStateFactory

  @Override
  protected JvmAbstractState<SetAbstractState<JvmTaintSource>> createJvmAbstractState(
      JvmCfaNode programLocation,
      JvmFrameAbstractState<SetAbstractState<JvmTaintSource>> frame,
      JvmHeapAbstractState<SetAbstractState<JvmTaintSource>> heap,
      MapAbstractState<String, SetAbstractState<JvmTaintSource>> staticFields) {
    return new JvmAbstractState<>(programLocation, frame, heap, staticFields);
  }

  @Override
  public JvmAbstractState<SetAbstractState<JvmTaintSource>> onMethodEntry(
      JvmAbstractState<SetAbstractState<JvmTaintSource>> reducedState, boolean isCallStatic) {
    JvmFrameAbstractState<SetAbstractState<JvmTaintSource>> frame = reducedState.getFrame();
    JvmCfaNode programLocation = reducedState.getProgramLocation();
    if (taintSourcesTaintArgs.containsKey(programLocation.getSignature())) {
      taintSourcesTaintArgs
          .get(programLocation.getSignature())
          .forEach(
              source ->
                  source.taintsArgs.forEach(
                      index -> updateTaintState(frame, isCallStatic ? index - 1 : index, source)));
    }
    return new JvmAbstractState<>(
        programLocation, frame, reducedState.getHeap(), reducedState.getStaticFields());
  }

  private void updateTaintState(
      JvmFrameAbstractState<SetAbstractState<JvmTaintSource>> frame,
      int index,
      JvmTaintSource source) {
    SetAbstractState<JvmTaintSource> oldState =
        frame.getLocalVariables().getOrDefault(index, SetAbstractState.bottom());
    SetAbstractState<JvmTaintSource> newState = oldState.join(new SetAbstractState<>(source));
    frame.getLocalVariables().set(index, newState, SetAbstractState.bottom());
  }
}
