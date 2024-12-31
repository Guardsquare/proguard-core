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

package proguard.analysis.cpa.jvm.operators;

import java.util.ListIterator;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.ListAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.defaults.StackAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.util.ClassUtil;

/**
 * This {@link ReduceOperator} simulates the JVM behavior on a method call. It takes a clone of the
 * caller {@link JvmAbstractState}, creates an empty stack and a local variables array with the
 * callee arguments.
 *
 * @param <ContentT> The content of the jvm states. For example, this can be a {@link
 *     SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public class DefaultReduceOperator<ContentT extends LatticeAbstractState<ContentT>>
    implements ReduceOperator<ContentT> {

  private final boolean reduceHeap;

  /** Create the default reduce operator for the JVM. */
  public DefaultReduceOperator() {
    this(true);
  }

  /**
   * Create the default reduce operator for the JVM.
   *
   * @param reduceHeap whether reduction of the heap is performed
   */
  public DefaultReduceOperator(boolean reduceHeap) {
    this.reduceHeap = reduceHeap;
  }

  // Implementations for ReduceOperator

  @Override
  public JvmAbstractState<ContentT> reduceImpl(
      JvmAbstractState<ContentT> expandedInitialState, JvmCfaNode blockEntryNode, Call call) {

    JvmAbstractState<ContentT> initialJvmState = expandedInitialState.copy();
    initialJvmState.setProgramLocation(blockEntryNode);

    ListAbstractState<ContentT> localVariables = new ListAbstractState<>();
    StackAbstractState<ContentT> callStack = new StackAbstractState<>();
    JvmFrameAbstractState<ContentT> frame = new JvmFrameAbstractState<>(localVariables, callStack);

    int i = 0;
    if (call.getTarget().descriptor.argumentTypes != null) {
      int argSize = call.getJvmArgumentSize();
      ListIterator<String> iterator =
          call.getTarget()
              .descriptor
              .argumentTypes
              .listIterator(call.getTarget().descriptor.argumentTypes.size());

      // set local variables in reverse order from the stack
      // variables of size 2 need to be reversed as done in JvmTransferRelation
      while (iterator.hasPrevious()) {
        String type = iterator.previous();
        int size = ClassUtil.internalTypeSize(type);

        ContentT state = initialJvmState.peek(i++);

        localVariables.set(argSize - size, state, null);

        if (size == 2) {
          state = initialJvmState.peek(i++);

          localVariables.set(argSize - size + 1, state, null);
        }

        argSize -= size;
      }
    }

    MapAbstractState<String, ContentT> staticFields = initialJvmState.getStaticFields();
    reduceStaticFields(staticFields);

    if (!call.isStatic()) {
      ContentT state = initialJvmState.peek(i);

      localVariables.set(0, state, null);
    }

    JvmHeapAbstractState<ContentT> heap = initialJvmState.getHeap();
    if (reduceHeap) {
      reduceHeap(heap, frame, initialJvmState.getStaticFields());
    }

    return createJvmAbstractState(
        initialJvmState.getProgramLocation(), frame, heap, initialJvmState.getStaticFields());
  }

  /**
   * Reduces the static fields. The default implementation doesn't perform any reduction.
   *
   * @param staticFields the static fields map that is modified by this method by performing
   *     reduction
   */
  protected void reduceStaticFields(MapAbstractState<String, ContentT> staticFields) {}

  /**
   * Reduces the heap state. The default implementation doesn't perform any reduction.
   *
   * @param heap the heap that is modified by this method by performing reduction
   * @param reducedFrame the frame after reduction has been performed on it
   * @param reducedStaticFields the static fields after reduction has been performed on them
   */
  protected void reduceHeap(
      JvmHeapAbstractState<ContentT> heap,
      JvmFrameAbstractState<ContentT> reducedFrame,
      MapAbstractState<String, ContentT> reducedStaticFields) {}

  protected JvmAbstractState<ContentT> createJvmAbstractState(
      JvmCfaNode programLocation,
      JvmFrameAbstractState<ContentT> frame,
      JvmHeapAbstractState<ContentT> heap,
      MapAbstractState<String, ContentT> staticFields) {
    return new JvmAbstractState<>(programLocation, frame, heap, staticFields);
  }
}
