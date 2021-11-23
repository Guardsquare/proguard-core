/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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
