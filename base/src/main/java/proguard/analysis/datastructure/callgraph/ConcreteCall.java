/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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
import proguard.analysis.datastructure.CodeLocation;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.visitor.MemberVisitor;
import proguard.evaluation.value.Value;

/**
 * A method call whose target {@link Method} is contained in our {@link ClassPool}.
 *
 * @author Samuel Hopstock
 */
public class ConcreteCall
    extends Call
{

    private final Clazz  targetClass;
    private final Method target;


    public ConcreteCall(CodeLocation caller,
                        Clazz targetClass,
                        Method target,
                        Value instance,
                        List<Value> arguments,
                        Value returnValue,
                        int throwsNullptr,
                        Instruction instruction,
                        boolean controlFlowDependent,
                        boolean runtimeTypeDependent)
    {
        super(caller,
              instance,
              arguments,
              returnValue,
              throwsNullptr,
              instruction,
              controlFlowDependent,
              runtimeTypeDependent);
        this.targetClass = targetClass;
        this.target = target;
    }

    public ConcreteCall(CodeLocation caller,
                        Clazz targetClass,
                        Method target,
                        int throwsNullptr,
                        Instruction instruction,
                        boolean controlFlowDependent,
                        boolean runtimeTypeDependent)
    {
        this(caller,
             targetClass,
             target,
             null,
             Collections.emptyList(),
             null,
             throwsNullptr,
             instruction,
             controlFlowDependent,
             runtimeTypeDependent);
    }

    @Override
    public MethodSignature getTarget()
    {
        return (MethodSignature) Signature.of(targetClass, target);
    }

    @Override
    public boolean hasIncompleteTarget()
    {
        // Concrete calls by definition know what the target will be.
        return false;
    }

    public Clazz getTargetClass()
    {
        return targetClass;
    }

    public Method getTargetMethod()
    {
        return target;
    }

    public void targetMethodAccept(MemberVisitor memberVisitor)
    {
        if (this.targetClass != null && this.target != null)
        {
            this.target.accept(this.targetClass, memberVisitor);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }
        ConcreteCall that = (ConcreteCall) o;
        return Objects.equals(targetClass, that.targetClass) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), targetClass, target);
    }
}
