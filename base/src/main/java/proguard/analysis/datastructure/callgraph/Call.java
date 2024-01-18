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

package proguard.analysis.datastructure.callgraph;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.CallResolver;
import proguard.analysis.CallVisitor;
import proguard.analysis.datastructure.CodeLocation;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionUtil;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.MemberVisitor;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.Value;

/**
 * Represents a method call. If the call target is a {@link Method} that is present in the class
 * pool, a {@link ConcreteCall} is instantiated. If the call target is not a known method, a {@link
 * SymbolicCall} is the appropriate subclass.
 *
 * @author Samuel Hopstock
 */
public abstract class Call {

  private static final Logger log = LogManager.getLogger(Call.class);
  /** The location where the call was invoked. */
  public final CodeLocation caller;
  /**
   * Describes whether this call will throw a {@link NullPointerException} at runtime. Either {@link
   * Value#NEVER}, {@link Value#MAYBE} or {@link Value#ALWAYS}.
   */
  public final int throwsNullptr;
  /**
   * The instruction performing this call. There are several different ways of invoking a method
   * call in the JVM:
   *
   * <ul>
   *   <li>{@link Instruction#OP_INVOKESTATIC}
   *   <li>{@link Instruction#OP_INVOKEVIRTUAL}
   *   <li>{@link Instruction#OP_INVOKEINTERFACE}
   *   <li>{@link Instruction#OP_INVOKESPECIAL}
   *   <li>{@link Instruction#OP_INVOKEDYNAMIC}
   * </ul>
   *
   * See the {@link CallResolver} for more details.
   */
  public final Instruction instruction;
  /**
   * If false, control flow in the calling method will always reach this call. Otherwise, whether
   * the call will be invoked at runtime might depend on e.g. specific branches being taken.
   */
  public final boolean controlFlowDependent;
  /**
   * If true, this call might only be one of several alternative targets, depending on the actual
   * type of the called object during runtime. Otherwise, this call is the only possible target.
   */
  public final boolean runtimeTypeDependent;

  private Value instance;
  private List<Value> arguments;
  private Value returnValue;
  private boolean valuesCleared;

  protected Call(
      CodeLocation caller,
      Value instance,
      List<Value> arguments,
      Value returnValue,
      int throwsNullptr,
      Instruction instruction,
      boolean controlFlowDependent,
      boolean runtimeTypeDependent) {
    this.caller = caller;
    this.instance = instance;
    this.arguments = arguments;
    this.returnValue = returnValue;
    this.throwsNullptr = throwsNullptr;
    this.instruction = instruction;
    this.controlFlowDependent = controlFlowDependent;
    this.runtimeTypeDependent = runtimeTypeDependent;
  }

  /** Check if this call is static (no implicit {@link #instance} set) or not. */
  public boolean isStatic() {
    return InstructionUtil.isStaticCall(instruction.opcode);
  }

  /** The {@link MethodSignature} of the method that is being called. */
  public abstract MethodSignature getTarget();

  /** Check if this call's target is fully known or only parts of it (e.g. only the descriptor). */
  public abstract boolean hasIncompleteTarget();

  /** Returns the number of arguments. */
  public int getArgumentCount() {
    if (!valuesCleared) {
      return arguments.size();
    }

    return ClassUtil.internalMethodParameterCount(getTarget().descriptor.toString());
  }

  /** Returns the number of elements that need to be popped from the JVM stack for this call. */
  public int getJvmArgumentSize() {
    return ClassUtil.internalMethodParameterSize(getTarget().descriptor.toString(), isStatic());
  }

  /**
   * Get the value for a specific argument index.
   *
   * <p><b>Note:</b> This is only to be used in implementations of {@link
   * CallVisitor#visitCall(Call)}. Afterwards, the values will have been cleared to reduce
   * unnecessary memory usage, as argument values are not needed for the full call graph
   * reconstruction.
   */
  public Value getArgument(int index) {
    if (valuesCleared) {
      log.error("Argument requested after values have been cleared!");
      return null;
    }
    if (arguments.size() <= index) {
      return null;
    }
    return arguments.get(index);
  }

  public void setArguments(List<Value> arguments) {
    this.arguments = arguments;
  }

  /**
   * If this is a virtual call, this describes the <code>this</code> pointer of the object whose
   * method is called, usually an {@link IdentifiedReferenceValue}. For static calls this is null.
   *
   * <p><b>Note:</b> This is only to be used in implementations of {@link
   * CallVisitor#visitCall(Call)}. Afterwards, the value will have been cleared to reduce
   * unnecessary memory usage, as the instance value is not needed for the full call graph
   * reconstruction.
   */
  public Value getInstance() {
    if (valuesCleared) {
      log.error("Instance requested after values have been cleared!");
      return null;
    }
    return instance;
  }

  public void setInstance(Value instance) {
    this.instance = instance;
  }

  /**
   * Get the return value of this call.
   *
   * <p><b>Note:</b> This is only to be used in implementations of {@link
   * CallVisitor#visitCall(Call)}. Afterwards, the value will have been cleared to reduce
   * unnecessary memory usage, as the return value is not needed for the full call graph
   * reconstruction.
   */
  public Value getReturnValue() {
    if (valuesCleared) {
      log.error("Return value requested after values have been cleared!");
      return null;
    }
    return returnValue;
  }

  public void setReturnValue(Value returnValue) {
    this.returnValue = returnValue;
  }

  /** Clear all {@link Value} object references from this call. */
  public void clearValues() {
    arguments = Collections.nCopies(arguments.size(), null);
    returnValue = null;
    instance = null;
    valuesCleared = true;
  }

  /**
   * Returns true if this call is always executed, no matter which branch in the methods are taken
   * and which type the called object has during runtime.
   */
  public boolean isCertainlyCalled() {
    return !controlFlowDependent && !runtimeTypeDependent;
  }

  @Override
  public String toString() {
    String nullSuffix = "";
    if (throwsNullptr == Value.ALWAYS) {
      nullSuffix = " (always throws NullPointerException)";
    } else if (throwsNullptr == Value.MAYBE) {
      nullSuffix = " (might throw NullPointerException)";
    }

    return "[" + instruction.getName() + "] " + caller + " -> " + getTarget() + nullSuffix;
  }

  /**
   * Prints a shorter version of the regular `toString()` without the caller or null pointer
   * information.
   */
  public String toSimpleString() {
    return "[" + instruction.getName() + "] " + getTarget();
  }

  public void targetMethodAccept(MemberVisitor memberVisitor) {}

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Call that = (Call) o;
    return throwsNullptr == that.throwsNullptr
        && Objects.equals(caller, that.caller)
        && Objects.equals(arguments, that.arguments)
        && Objects.equals(instruction, that.instruction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caller, throwsNullptr);
  }
}
