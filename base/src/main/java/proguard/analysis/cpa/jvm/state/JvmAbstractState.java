/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

package proguard.analysis.cpa.jvm.state;

import java.util.List;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.classfile.Clazz;

/**
 * The {@link JvmAbstractState} consists of the method frame {@link JvmFrameAbstractState} and the
 * heap {@link JvmHeapAbstractState}.
 *
 * @param <ContentT> The type of the states contained in the JVM state. e.g., for taint analysis
 *     this would be a {@link proguard.analysis.cpa.defaults.SetAbstractState} containing the taints
 *     and for value analysis a {@link proguard.analysis.cpa.jvm.domain.value.ValueAbstractState}.
 */
public class JvmAbstractState<ContentT extends AbstractState<ContentT>>
    implements AbstractState<JvmAbstractState<ContentT>>, ProgramLocationDependent {
  public static final String DEFAULT_FIELD = "";

  protected final JvmFrameAbstractState<ContentT> frame;
  protected final JvmHeapAbstractState<ContentT> heap;
  protected final MapAbstractState<String, ContentT> staticFields;
  protected JvmCfaNode programLocation;
  protected static final JvmCfaNode topLocation = new JvmCfaNode(null, -1, null);

  /**
   * Create a JVM abstract state.
   *
   * @param programLocation a CFA node
   * @param frame a frame abstract state
   * @param heap a heap abstract state
   * @param staticFields a static field table
   */
  public JvmAbstractState(
      JvmCfaNode programLocation,
      JvmFrameAbstractState<ContentT> frame,
      JvmHeapAbstractState<ContentT> heap,
      MapAbstractState<String, ContentT> staticFields) {
    this.programLocation = programLocation;
    this.frame = frame;
    this.heap = heap;
    this.staticFields = staticFields;
  }

  // implementations for AbstractState

  @Override
  public JvmAbstractState<ContentT> join(JvmAbstractState<ContentT> abstractState) {
    JvmAbstractState<ContentT> answer =
        new JvmAbstractState<>(
            programLocation.equals(abstractState.programLocation) ? programLocation : topLocation,
            frame.join(abstractState.frame),
            heap.join(abstractState.heap),
            staticFields.join(abstractState.staticFields));
    return equals(answer) ? this : answer;
  }

  @Override
  public boolean isLessOrEqual(JvmAbstractState<ContentT> abstractState) {
    return (programLocation.equals(abstractState.programLocation)
            || abstractState.programLocation.equals(topLocation))
        && frame.isLessOrEqual(abstractState.frame)
        && heap.isLessOrEqual(abstractState.heap)
        && staticFields.isLessOrEqual(abstractState.staticFields);
  }

  // implementations for ProgramLocationDependent

  @Override
  public JvmCfaNode getProgramLocation() {
    return programLocation;
  }

  @Override
  public void setProgramLocation(JvmCfaNode programLocation) {
    this.programLocation = programLocation;
  }

  // implementations for AbstractState

  @Override
  public JvmAbstractState<ContentT> copy() {
    return new JvmAbstractState<>(programLocation, frame.copy(), heap.copy(), staticFields.copy());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JvmAbstractState)) {
      return false;
    }
    JvmAbstractState<ContentT> other = (JvmAbstractState<ContentT>) obj;
    return programLocation.equals(other.programLocation)
        && frame.equals(other.frame)
        && heap.equals(other.heap)
        && staticFields.equals(other.staticFields);
  }

  @Override
  public int hashCode() {
    // Since the states are used in hashsets, we must ensure that the hash code is fixed.
    // Frame, heap, staticFields can change over time and result in different hash codes.
    return programLocation.hashCode();
  }

  /** Returns the top element of the operand stack. */
  public ContentT peek() {
    return peek(0);
  }

  /** Returns the {@code index}th element from the top of the operand stack. */
  public ContentT peek(int index) {
    return frame.peek(index);
  }

  /**
   * Returns the top element of the operand stack or returns {@code defaultState} if the stack is
   * empty.
   */
  public ContentT peekOrDefault(ContentT defaultState) {
    return peekOrDefault(0, defaultState);
  }

  /**
   * Returns the {@code index}th element from the top of the operand stack or returns {@code
   * defaultState} if the stack does not have enough elements.
   */
  public ContentT peekOrDefault(int index, ContentT defaultState) {
    return frame.peekOrDefault(index, defaultState);
  }

  /** Removes the top element of the operand stack end returns it. */
  public ContentT pop() {
    return frame.pop();
  }

  /**
   * Removes the top element of the operand stack end returns it. Returns {@code defaultState} if
   * the stack is empty.
   */
  public ContentT popOrDefault(ContentT defaultState) {
    return frame.popOrDefault(defaultState);
  }

  /** Inserts {@code state} to the top of the operand stack and returns it. */
  public ContentT push(ContentT state) {
    return frame.push(state);
  }

  /**
   * Consequentially inserts elements of {@code states} to the top of the operand stack and returns
   * {@code states}.
   */
  public List<ContentT> pushAll(List<ContentT> states) {
    states.forEach(frame::push);
    return states;
  }

  /** Empties the operand stack. */
  public void clearOperandStack() {
    frame.getOperandStack().clear();
  }

  /**
   * Returns an abstract state at the {@code index}th position of the variable array or {@code
   * defaultState} if there is no entry.
   */
  public ContentT getVariableOrDefault(int index, ContentT defaultState) {
    return frame.getVariableOrDefault(index, defaultState);
  }

  /**
   * Sets the {@code index}th position of the variable array to {@code state} and returns {@code
   * state}. If the array has to be extended, the added cells are padded with {@code defaultState}.
   */
  public ContentT setVariable(int index, ContentT state, ContentT defaultState) {
    return frame.setVariable(index, state, defaultState);
  }

  /**
   * Returns an abstract state representing the static field {@code fqn} or {@code defaultState} if
   * there is no entry.
   */
  public ContentT getStaticOrDefault(String fqn, ContentT defaultState) {
    return staticFields.getOrDefault(fqn, defaultState);
  }

  /**
   * Sets the static field {@code fqn} to {@code value}, unless the value is {@code defaultState}.
   */
  public void setStatic(String fqn, ContentT value, ContentT defaultState) {
    if (value.equals(defaultState)) {
      return;
    }
    staticFields.put(fqn, value);
  }

  /**
   * Returns an abstract state representing the field {@code descriptor} of the {@code object} or
   * {@code defaultState} if there is no entry.
   */
  public <T> ContentT getFieldOrDefault(T object, String descriptor, ContentT defaultValue) {
    return heap.getFieldOrDefault(object, descriptor, defaultValue);
  }

  /**
   * Returns an abstract state representing the default field of the {@code object} or {@code
   * defaultState} if there is no entry.
   */
  public <T> ContentT getFieldOrDefault(T object, ContentT defaultValue) {
    return this.getFieldOrDefault(object, DEFAULT_FIELD, defaultValue);
  }

  /** Sets the field {@code descriptor} of the {@code object} to {@code value}. */
  public <T> void setField(T object, String descriptor, ContentT value) {
    heap.setField(object, descriptor, value);
  }

  /** Sets the default field of the {@code object} to {@code value}. */
  public <T> void setField(T object, ContentT value) {
    this.setField(object, DEFAULT_FIELD, value);
  }

  /** Returns the frame abstract state. */
  public JvmFrameAbstractState<ContentT> getFrame() {
    return this.frame;
  }

  /** Returns the static field table abstract state. */
  public MapAbstractState<String, ContentT> getStaticFields() {
    return staticFields;
  }

  /** Returns the heap abstract state. */
  public JvmHeapAbstractState<ContentT> getHeap() {
    return heap;
  }

  /**
   * Returns an abstract state for a new array for the given {@code type} and {@code dimentions}.
   */
  public ContentT newArray(String type, List<ContentT> dimensions) {
    return heap.newArray(type, dimensions, programLocation);
  }

  /** Returns an abstract state for a new object of the given {@code className}. */
  public ContentT newObject(String className) {
    return heap.newObject(className, programLocation);
  }

  /** Returns an abstract state for a new object of the given {@link Clazz}. */
  public ContentT newObject(Clazz clazz) {
    return heap.newObject(clazz, programLocation);
  }

  /**
   * Returns an abstract state for the {@code array} element at the given {@code index} or the
   * {@code abstractDefault} if there is no information available.
   */
  public <T> ContentT getArrayElementOrDefault(T array, ContentT index, ContentT abstractDefault) {
    return heap.getArrayElementOrDefault(array, index, abstractDefault);
  }

  /** Sets the {@code array} element at the given {@code index} to the {@code value}. */
  public <T> void setArrayElement(T array, ContentT index, ContentT value) {
    heap.setArrayElement(array, index, value);
  }
}
