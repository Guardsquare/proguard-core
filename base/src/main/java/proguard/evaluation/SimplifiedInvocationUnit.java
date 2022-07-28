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

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.evaluation.value.*;

/**
 * This {@link InvocationUnit} sets up the variables for entering a method,
 * and it updates the stack for the invocation of a class member,
 * using simple values.
 *
 * @author Eric Lafortune
 */
public abstract class SimplifiedInvocationUnit
implements            InvocationUnit,
                      ParameterVisitor,
                      ConstantVisitor
{
    private final MemberVisitor parameterInitializer = new AllParameterVisitor(true, this);

    // Fields acting as parameters between the visitor methods.
    protected Variables variables;
    protected boolean   isStatic;
    protected boolean   isLoad;
    protected Stack     stack;
    protected Method    method;

    // Implementations for InvocationUnit.

    public void enterMethod(Clazz clazz, Method method, Variables variables)
    {
        // Count the number of parameters, taking into account their categories.
        int parameterSize =
            ClassUtil.internalMethodParameterSize(method.getDescriptor(clazz),
                                                  method.getAccessFlags());

        // Reuse the existing parameters object, ensuring the right size.
        variables.reset(parameterSize);

        // Initialize the parameters.
        this.variables = variables;
        method.accept(clazz, parameterInitializer);
        this.variables = null;
    }


    // Implementation for ParameterVisitor.

    public void visitParameter(Clazz clazz, Member member, int parameterIndex, int parameterCount, int parameterOffset, int parameterSize, String parameterType, Clazz referencedClass)
    {
        Method method = (Method)member;

        // Get the parameter value.
        Value value = getMethodParameterValue(clazz,
                                              method,
                                              parameterIndex,
                                              parameterType,
                                              referencedClass);

        // Store the value in the corresponding variable.
        variables.store(parameterOffset, value);
    }


    public void exitMethod(Clazz clazz, Method method, Value returnValue)
    {
        setMethodReturnValue(clazz, method, returnValue);
    }


    public void enterExceptionHandler(Clazz         clazz,
                                      Method        method,
                                      CodeAttribute codeAttribute,
                                      int           offset,
                                      int           catchType,
                                      Stack         stack)
    {
        ClassConstant exceptionClassConstant =
            (ClassConstant)((ProgramClass)clazz).getConstant(catchType);

        stack.push(getExceptionValue(clazz, exceptionClassConstant));
    }


    public void invokeMember(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction, Stack stack, Variables variables)
    {
        int constantIndex = constantInstruction.constantIndex;

        switch (constantInstruction.opcode)
        {
            case Instruction.OP_GETSTATIC:
                isStatic = true;
                isLoad   = true;
                break;

            case Instruction.OP_PUTSTATIC:
                isStatic = true;
                isLoad   = false;
                break;

            case Instruction.OP_GETFIELD:
                isStatic = false;
                isLoad   = true;
                break;

            case Instruction.OP_PUTFIELD:
                isStatic = false;
                isLoad   = false;
                break;

            case Instruction.OP_INVOKESTATIC:
            case Instruction.OP_INVOKEDYNAMIC:
                isStatic = true;
                break;

            case Instruction.OP_INVOKEVIRTUAL:
            case Instruction.OP_INVOKESPECIAL:
            case Instruction.OP_INVOKEINTERFACE:
                isStatic = false;
                break;
        }

        // Pop the parameters and push the return value.
        this.stack = stack;
        this.variables = variables;
        this.method = method;
        clazz.constantPoolEntryAccept(constantIndex, this);
        this.method = null;
        this.variables = null;
        this.stack = null;
    }


    // Implementations for ConstantVisitor.

    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        // Pop the field value, if applicable.
        if (!isLoad)
        {
            setFieldValue(clazz, fieldrefConstant, stack.pop());
        }

        // Pop the reference value, if applicable.
        if (!isStatic)
        {
            setFieldClassValue(clazz, fieldrefConstant, stack.apop());
        }

        // Push the field value, if applicable.
        if (isLoad)
        {
            String type = fieldrefConstant.getType(clazz);

            stack.push(getFieldValue(clazz, fieldrefConstant, type));
        }
    }


    public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant)
    {
        String type = anyMethodrefConstant.getType(clazz);

        // Count the number of parameters.
        int parameterCount = ClassUtil.internalMethodParameterCount(type, isStatic);

        // Pop the parameters and the class reference, in reverse order.
        for (int parameterIndex = parameterCount-1; parameterIndex >= 0; parameterIndex--)
        {
            setMethodParameterValue(clazz, anyMethodrefConstant, parameterIndex, stack.pop());
        }

        // Push the return value, if applicable.
        String returnType = ClassUtil.internalMethodReturnType(type);

        Value returnValue = null;

        // Check if the methodReturnValue needs to be calculated, i.e., if the function needs to be executed.
        if (returnType.charAt(0) != TypeConstants.VOID
            || methodMayHaveSideEffects(clazz, anyMethodrefConstant, returnType))
        {
            returnValue = getMethodReturnValue(clazz, anyMethodrefConstant, returnType);
        }

        // Only push the value on the stack if the method actually returns something.
        if (returnType.charAt(0) != TypeConstants.VOID)
        {
            stack.push(returnValue);
        }
    }


    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        String type = invokeDynamicConstant.getType(clazz);

        // Count the number of parameters.
        int parameterCount = ClassUtil.internalMethodParameterCount(type, isStatic);

        // Pop the parameters and the class reference, in reverse order.
        for (int parameterIndex = parameterCount-1; parameterIndex >= 0; parameterIndex--)
        {
            stack.pop();
        }

        // Push the return value, if applicable.
        String returnType = ClassUtil.internalMethodReturnType(type);
        if (returnType.charAt(0) != TypeConstants.VOID)
        {
            stack.push(getMethodReturnValue(clazz, invokeDynamicConstant, returnType));
        }
    }


    /**
     * Returns the value of the specified exception.
     */
    public abstract Value getExceptionValue(Clazz         clazz,
                                            ClassConstant catchClassConstant);


    /**
     * Sets the class through which the specified field is accessed.
     */
    public abstract void setFieldClassValue(Clazz            clazz,
                                            FieldrefConstant fieldrefConstant,
                                            ReferenceValue   value);


    /**
     * Returns the class though which the specified field is accessed.
     */
    public abstract Value getFieldClassValue(Clazz            clazz,
                                             FieldrefConstant fieldrefConstant,
                                             String           type);


    /**
     * Sets the value of the specified field.
     */
    public abstract void setFieldValue(Clazz            clazz,
                                       FieldrefConstant fieldrefConstant,
                                       Value            value);


    /**
     * Returns the value of the specified field.
     */
    public abstract Value getFieldValue(Clazz            clazz,
                                        FieldrefConstant fieldrefConstant,
                                        String           type);


    /**
     * Sets the value of the specified method parameter.
     */
    public abstract void setMethodParameterValue(Clazz                clazz,
                                                 AnyMethodrefConstant anyMethodrefConstant,
                                                 int                  parameterIndex,
                                                 Value                value);


    /**
     * Returns the value of the specified method parameter.
     */
    public abstract Value getMethodParameterValue(Clazz  clazz,
                                                  Method method,
                                                  int    parameterIndex,
                                                  String type,
                                                  Clazz  referencedClass);


    /**
     * Sets the return value of the specified method.
     */
    public abstract void setMethodReturnValue(Clazz  clazz,
                                              Method method,
                                              Value  value);


    /**
     * Returns the return value of the specified method.
     */
    public abstract Value getMethodReturnValue(Clazz                clazz,
                                               AnyMethodrefConstant anyMethodrefConstant,
                                               String               type);


    /**
     * Returns the return value of the specified method.
     */
    public abstract Value getMethodReturnValue(Clazz                 clazz,
                                               InvokeDynamicConstant invokeDynamicConstant,
                                               String                type);

    /**
     * Returns true if the method itself can modify the stack/variables and therefore
     * needs to be executed even if it returns void.
     */
    protected boolean methodMayHaveSideEffects(Clazz clazz,
                                               AnyMethodrefConstant anyMethodrefConstant,
                                               String returnType)
    {
        return false;
    }
}
