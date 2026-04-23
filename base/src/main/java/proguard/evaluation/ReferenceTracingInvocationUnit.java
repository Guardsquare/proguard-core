/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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

package proguard.evaluation;

import java.util.Objects;
import java.util.function.Predicate;
import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.constant.*;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.evaluation.value.*;
import proguard.util.PartialEvaluatorUtils;

/**
 * This {@link InvocationUnit} tags reference values of retrieved fields, passed method parameters,
 * method return values, and caught exceptions, so they can be traced throughout the execution of a
 * method. The tags are instruction offsets or parameter indices (not parameter offsets).
 *
 * @see TracedReferenceValue
 * @see InstructionOffsetValue
 * @author Eric Lafortune
 */
public class ReferenceTracingInvocationUnit extends SimplifiedInvocationUnit {
  private final SimplifiedInvocationUnit invocationUnit;

  private int offset;

  // This will hold the "instance" value for non-static method invocations.
  private Value instance;

  /**
   * Creates a new ReferenceTracingInvocationUnit.
   *
   * @param invocationUnit the invocation unit to which invocations will be delegated.
   */
  public ReferenceTracingInvocationUnit(SimplifiedInvocationUnit invocationUnit) {
    this.invocationUnit = invocationUnit;
  }

  // Implementations for InvocationUnit.

  @Override
  public void enterExceptionHandler(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      int catchType,
      Stack stack) {
    this.offset = offset;

    super.enterExceptionHandler(clazz, method, codeAttribute, offset, catchType, stack);
  }

  @Override
  public void invokeMember(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      ConstantInstruction constantInstruction,
      Stack stack,
      Variables variables) {
    this.offset = offset;

    super.invokeMember(clazz, method, codeAttribute, offset, constantInstruction, stack, variables);
  }

  // Implementations for SimplifiedInvocationUnit.

  @Override
  public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant) {
    try {
      super.visitAnyMethodrefConstant(clazz, anyMethodrefConstant);
    } finally {
      instance = null;
    }
  }

  @Override
  public Value getExceptionValue(Clazz clazz, ClassConstant catchClassConstant) {
    return trace(
        invocationUnit.getExceptionValue(clazz, catchClassConstant),
        offset | InstructionOffsetValue.EXCEPTION_HANDLER);
  }

  @Override
  public void setFieldClassValue(
      Clazz clazz, FieldrefConstant fieldrefConstant, ReferenceValue value) {
    invocationUnit.setFieldClassValue(clazz, fieldrefConstant, value);
  }

  @Override
  public Value getFieldClassValue(Clazz clazz, FieldrefConstant fieldrefConstant, String type) {
    return trace(
        invocationUnit.getFieldClassValue(clazz, fieldrefConstant, type),
        offset | InstructionOffsetValue.FIELD_VALUE);
  }

  @Override
  public void setFieldValue(Clazz clazz, FieldrefConstant fieldrefConstant, Value value) {
    invocationUnit.setFieldValue(clazz, fieldrefConstant, value);
  }

  @Override
  public Value getFieldValue(Clazz clazz, FieldrefConstant fieldrefConstant, String type) {
    return trace(
        invocationUnit.getFieldValue(clazz, fieldrefConstant, type),
        offset | InstructionOffsetValue.FIELD_VALUE);
  }

  @Override
  public void setMethodParameterValue(
      Clazz clazz, AnyMethodrefConstant anyMethodrefConstant, int parameterIndex, Value value) {
    invocationUnit.setMethodParameterValue(clazz, anyMethodrefConstant, parameterIndex, value);

    if (!isStatic && parameterIndex == 0) {
      instance = value;
    }
  }

  @Override
  public Value getMethodParameterValue(
      Clazz clazz, Method method, int parameterIndex, String type, Clazz referencedClass) {
    Value parameterValue =
        invocationUnit.getMethodParameterValue(
            clazz, method, parameterIndex, type, referencedClass);

    // We're attaching the parameter index as a trace value. It doesn't
    // take into account Category 2 values, so it is not compatible with
    // variable indices.
    return trace(parameterValue, parameterIndex | InstructionOffsetValue.METHOD_PARAMETER);
  }

  @Override
  public void setMethodReturnValue(Clazz clazz, Method method, Value value) {
    invocationUnit.setMethodReturnValue(clazz, method, value);
  }

  @Override
  public Value getMethodReturnValue(
      Clazz clazz, AnyMethodrefConstant anyMethodrefConstant, String type) {
    Value returnValue = invocationUnit.getMethodReturnValue(clazz, anyMethodrefConstant, type);

    // The invocation might have had side effects.
    applySideEffects(clazz, anyMethodrefConstant, type);

    return trace(returnValue, offset | InstructionOffsetValue.METHOD_RETURN_VALUE);
  }

  @Override
  public Value getMethodReturnValue(
      Clazz clazz, InvokeDynamicConstant invokeDynamicConstant, String type) {
    Value returnValue = invocationUnit.getMethodReturnValue(clazz, invokeDynamicConstant, type);

    return trace(returnValue, offset | InstructionOffsetValue.METHOD_RETURN_VALUE);
  }

  @Override
  protected boolean methodMayHaveSideEffects(
      Clazz clazz, AnyMethodrefConstant anyMethodrefConstant, String returnType) {
    // We keep track of when references are initialized.
    return ClassConstants.METHOD_NAME_INIT.equals(anyMethodrefConstant.getName(clazz))
        || invocationUnit.methodMayHaveSideEffects(clazz, anyMethodrefConstant, returnType);
  }

  // Small utility methods.

  protected void applySideEffects(
      Clazz clazz, AnyMethodrefConstant methodrefConstant, String type) {
    // Did we invoke the constructor?
    if (ClassConstants.METHOD_NAME_INIT.equals(methodrefConstant.getName(clazz))) {
      // We can only update the instance reference if it is specific. This assumption can break due
      // to potentially incorrect bytecode.
      if (instance instanceof ReferenceValue && instance.isSpecific()) {
        ReferenceValue instanceReference = untrace(instance).referenceValue();
        Object instanceId =
            PartialEvaluatorUtils.getIdFromSpecificReferenceValue(instanceReference);

        Predicate<Value> predicate =
            (value) ->
                value instanceof ReferenceValue
                    && value.isSpecific()
                    && Objects.equals(
                        instanceId,
                        PartialEvaluatorUtils.getIdFromSpecificReferenceValue(
                            untrace(value).referenceValue()));

        Value replacement =
            new TracedReferenceValue(
                instanceReference, new InstructionOffsetValue(offset), ReferenceValue.ALWAYS);

        variables.replaceReferencesIf(predicate, x -> replacement);
        stack.replaceReferencesIf(predicate, x -> replacement);
      }
    }
  }

  /**
   * Sets or replaces the trace value on a given value, if it's a reference value, returning the
   * result.
   */
  protected Value trace(Value value, int trace) {
    return (value != null && value.computationalType() == Value.TYPE_REFERENCE)
        ? trace(value, new InstructionOffsetValue(trace))
        : value;
  }

  /** Sets or replaces the trace value on a given value, returning the result. */
  protected Value trace(Value value, InstructionOffsetValue traceValue) {
    return new TracedReferenceValue(untrace(value).referenceValue(), traceValue);
  }

  /** Removes the trace value from a given value, if present, returning the result. */
  private Value untrace(Value value) {
    return value instanceof TracedReferenceValue
        ? ((TracedReferenceValue) value).getReferenceValue()
        : value;
  }
}
