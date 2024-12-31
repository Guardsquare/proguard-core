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

package proguard.analysis.cpa.jvm.domain.memory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import proguard.analysis.cpa.bam.BamLocationDependent;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.classfile.MethodSignature;

/**
 * This {@link AbstractState} consists of a {@link BamLocationDependentJvmMemoryLocation} with a set
 * of sources contributed into its value and the call stack that generated it.
 *
 * @param <ContentT> The content of the jvm states for the traced analysis. For example, this can be
 *     a {@link SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public class JvmMemoryLocationAbstractState<ContentT extends LatticeAbstractState<ContentT>>
    implements LatticeAbstractState<JvmMemoryLocationAbstractState<ContentT>>,
        ProgramLocationDependent,
        BamLocationDependent<ContentT> {

  private static final JvmMemoryLocationAbstractState<?> top =
      new JvmMemoryLocationAbstractState<>();
  private final BamLocationDependentJvmMemoryLocation<ContentT> locationDependentMemoryLocation;
  private final Set<BamLocationDependentJvmMemoryLocation<ContentT>> sourceLocations;
  // LinkedList is used here because the data structure needs to support both stack operations and
  // an equals operator that compares all the members (which is not a case for ArrayDeque)
  private final LinkedList<StackEntry<ContentT>> callStack;

  private JvmMemoryLocationAbstractState() {
    this((BamLocationDependentJvmMemoryLocation<ContentT>) null, null, null);
  }

  /**
   * Create a {@link JvmMemoryLocationAbstractState} with empty source locations and call stack.
   *
   * @param locationDependentMemoryLocation a {@link JvmMemoryLocation} in a specified program
   *     location coming from a specific reached set.
   */
  public JvmMemoryLocationAbstractState(
      BamLocationDependentJvmMemoryLocation<ContentT> locationDependentMemoryLocation) {
    this(locationDependentMemoryLocation, new HashSet<>(), new LinkedList<>());
  }

  /**
   * Create a {@link JvmMemoryLocationAbstractState} with empty source locations and call stack.
   *
   * @param memoryLocation a memory location.
   * @param programLocation the program location of the memory location.
   * @param sourceReachedSet the reached set of the traced analysis from which the analyzed state
   *     comes from.
   */
  public JvmMemoryLocationAbstractState(
      JvmMemoryLocation memoryLocation,
      JvmCfaNode programLocation,
      ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> sourceReachedSet) {
    this(memoryLocation, programLocation, sourceReachedSet, new HashSet<>());
  }

  /**
   * Create a {@link JvmMemoryLocationAbstractState} with empty call stack.
   *
   * @param memoryLocation a memory location.
   * @param programLocation the program location of the memory location.
   * @param sourceReachedSet the reached set of the traced analysis from which the analyzed state
   *     comes from.
   * @param sourceLocations the succcessor memory locations.
   */
  public JvmMemoryLocationAbstractState(
      JvmMemoryLocation memoryLocation,
      JvmCfaNode programLocation,
      ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> sourceReachedSet,
      Set<BamLocationDependentJvmMemoryLocation<ContentT>> sourceLocations) {
    this(memoryLocation, programLocation, sourceReachedSet, sourceLocations, new LinkedList<>());
  }

  /**
   * Create a {@link JvmMemoryLocationAbstractState} with empty source locations.
   *
   * @param memoryLocation a memory location.
   * @param programLocation the program location of the memory location.
   * @param sourceReachedSet the reached set of the traced analysis from which the analyzed state
   *     comes from.
   * @param callStack the call stack.
   */
  public JvmMemoryLocationAbstractState(
      JvmMemoryLocation memoryLocation,
      JvmCfaNode programLocation,
      ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> sourceReachedSet,
      LinkedList<StackEntry<ContentT>> callStack) {
    this(memoryLocation, programLocation, sourceReachedSet, new HashSet<>(), callStack);
  }

  /**
   * Create a {@link JvmMemoryLocationAbstractState} with source locations.
   *
   * @param memoryLocation a memory location.
   * @param programLocation the program location of the memory location.
   * @param sourceReachedSet the reached set of the traced analysis from which the analyzed state
   *     comes from.
   * @param sourceLocations the succcessor memory locations.
   * @param callStack the call stack.
   */
  public JvmMemoryLocationAbstractState(
      JvmMemoryLocation memoryLocation,
      JvmCfaNode programLocation,
      ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> sourceReachedSet,
      Set<BamLocationDependentJvmMemoryLocation<ContentT>> sourceLocations,
      LinkedList<StackEntry<ContentT>> callStack) {
    this(
        new BamLocationDependentJvmMemoryLocation<>(
            memoryLocation, programLocation, sourceReachedSet),
        sourceLocations,
        callStack);
  }

  private JvmMemoryLocationAbstractState(
      BamLocationDependentJvmMemoryLocation<ContentT> locationDependentMemoryLocation,
      Set<BamLocationDependentJvmMemoryLocation<ContentT>> sourceLocations,
      LinkedList<StackEntry<ContentT>> callStack) {
    this.locationDependentMemoryLocation = locationDependentMemoryLocation;
    this.sourceLocations = sourceLocations;
    this.callStack = callStack;
  }

  public BamLocationDependentJvmMemoryLocation<ContentT> getLocationDependentMemoryLocation() {
    return locationDependentMemoryLocation;
  }

  /**
   * Returns the information of the caller, null if the caller of the method the state belongs to is
   * unknown.
   */
  public StackEntry<ContentT> peekCallStack() {
    return callStack.peek();
  }

  /** Returns true if a method is present in the call stack. */
  public boolean callStackContains(MethodSignature signature) {
    return callStack.stream().anyMatch(e -> signature.equals(e.signature));
  }

  /** Returns a shallow copy of the call stack. */
  public LinkedList<StackEntry<ContentT>> copyStack() {
    return new LinkedList<>(callStack);
  }

  // implementations for LatticeAbstractState

  @Override
  public JvmMemoryLocationAbstractState<ContentT> join(
      JvmMemoryLocationAbstractState<ContentT> abstractState) {
    // FIXME: There is a bug in the definition of this lattice. The join operation does not do
    //        anything with the callStack, but the comparison operations use them. Therefore,
    //        the standard semi-lattice invariants do not hold. Specifically, the bug is, that
    //        there can be two states A and B, which merge into state M. But then A <= M
    //        or B <= M might not hold.
    if (!locationDependentMemoryLocation.equals(abstractState.locationDependentMemoryLocation)) {
      // FIXME: if this condition is ever triggered, the analysis will crash with an NPE
      return top();
    }
    JvmMemoryLocationAbstractState<ContentT> result = copy();
    result.sourceLocations.addAll(abstractState.sourceLocations);
    return equals(result) ? this : result;
  }

  @Override
  public boolean isLessOrEqual(JvmMemoryLocationAbstractState<ContentT> abstractState) {
    return abstractState == top()
        || locationDependentMemoryLocation.equals(abstractState.locationDependentMemoryLocation)
            && callStack.equals(abstractState.callStack)
            && abstractState.sourceLocations.containsAll(sourceLocations);
  }

  // implementations for ProgramLocationDependent

  @Override
  public JvmCfaNode getProgramLocation() {
    return locationDependentMemoryLocation.getProgramLocation();
  }

  @Override
  public void setProgramLocation(JvmCfaNode programLocation) {
    locationDependentMemoryLocation.setProgramLocation(programLocation);
  }

  @Override
  public ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> getSourceReachedSet() {
    return locationDependentMemoryLocation.getSourceReachedSet();
  }

  @Override
  public void setSourceReachedSet(
      ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> sourceReachedSet) {
    locationDependentMemoryLocation.setSourceReachedSet(sourceReachedSet);
  }

  /** Adds a source location to the source set. */
  public void addSourceLocation(BamLocationDependentJvmMemoryLocation<ContentT> sourceLocation) {
    sourceLocations.add(sourceLocation);
  }

  /** Returns the source set. */
  public Set<BamLocationDependentJvmMemoryLocation<ContentT>> getSourceLocations() {
    return sourceLocations;
  }

  // implementations for AbstractState

  @Override
  public JvmMemoryLocationAbstractState<ContentT> copy() {
    return new JvmMemoryLocationAbstractState<>(
        locationDependentMemoryLocation.copy(), new HashSet<>(sourceLocations), callStack);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof JvmMemoryLocationAbstractState)) {
      return false;
    }
    JvmMemoryLocationAbstractState<?> other = (JvmMemoryLocationAbstractState<?>) obj;
    return Objects.equals(locationDependentMemoryLocation, other.locationDependentMemoryLocation)
        && Objects.equals(callStack, other.callStack)
        && Objects.equals(sourceLocations, other.sourceLocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(locationDependentMemoryLocation);
  }

  /**
   * An entry of the call stack of the state.
   *
   * @param <ContentT> The content of the jvm states. For example, this can be a {@link
   *     SetAbstractState} of taints for taint analysis or a {@link
   *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
   */
  public static class StackEntry<ContentT extends LatticeAbstractState<ContentT>> {

    public final MethodSignature signature;
    public final ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> reachedSet;
    public final JvmAbstractState<ContentT> callerState;

    public StackEntry(
        MethodSignature signature,
        ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> reachedSet,
        JvmAbstractState<ContentT> callerState) {
      this.signature = signature;
      this.reachedSet = reachedSet;
      this.callerState = callerState;
    }

    @Override
    public int hashCode() {
      return Objects.hash(signature, reachedSet, callerState);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof StackEntry)) {
        return false;
      }

      StackEntry<?> other = (StackEntry<?>) obj;
      return Objects.equals(reachedSet, other.reachedSet)
          && Objects.equals(signature, other.signature)
          && Objects.equals(callerState, other.callerState);
    }
  }

  /**
   * Get the top state of the semi lattice for the given content type.
   *
   * @param <ContentT> The content of the jvm states. For example, this can be a {@link
   *     SetAbstractState} of taints for taint analysis or a {@link
   *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
   */
  public static <ContentT extends LatticeAbstractState<ContentT>>
      JvmMemoryLocationAbstractState<ContentT> top() {
    // top behaves the same for all contents, so we can safely convert a unique top state
    @SuppressWarnings("unchecked")
    JvmMemoryLocationAbstractState<ContentT> topParametrized =
        (JvmMemoryLocationAbstractState<ContentT>) top;
    return topParametrized;
  }
}
