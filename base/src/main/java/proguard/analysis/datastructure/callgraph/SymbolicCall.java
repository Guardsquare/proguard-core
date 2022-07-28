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
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.evaluation.value.Value;

/**
 * A call whose target {@link Method} is not available in our {@link ClassPool}.
 *
 * @author Samuel Hopstock
 */
public class SymbolicCall
    extends Call
{

    private final MethodSignature target;

    public SymbolicCall(CodeLocation caller,
                        MethodSignature target,
                        Value instance,
                        List<Value> arguments,
                        Value returnValue,
                        int throwsNullptr,
                        byte invocationOpcode)
    {
        this(caller, target, instance, arguments, returnValue, throwsNullptr, invocationOpcode, false, false);
    }

    public SymbolicCall(CodeLocation caller,
                        MethodSignature target,
                        Value instance,
                        List<Value> arguments,
                        Value returnValue,
                        int throwsNullptr,
                        byte invocationOpcode,
                        boolean controlFlowDependent,
                        boolean runtimeTypeDependent)
    {
        super(caller, instance, arguments, returnValue, throwsNullptr, invocationOpcode, controlFlowDependent, runtimeTypeDependent);
        this.target = target;
    }

    public SymbolicCall(CodeLocation caller,
                        MethodSignature target,
                        int throwsNullptr,
                        byte invocationOpcode,
                        boolean controlFlowDependent,
                        boolean runtimeTypeDependent)
    {
        this(caller,
             target,
             null,
             Collections.emptyList(),
             null,
             throwsNullptr,
             invocationOpcode,
             controlFlowDependent,
             runtimeTypeDependent);
    }

    @Override
    public MethodSignature getTarget()
    {
        return target;
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
        SymbolicCall that = (SymbolicCall) o;
        return Objects.equals(target, that.target);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), target);
    }
}
