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
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;
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
        byte invocationOpcode,
        boolean controlFlowDependent,
        boolean runtimeTypeDependent)
    {
        super(caller, instance, arguments, returnValue, throwsNullptr, invocationOpcode, controlFlowDependent, runtimeTypeDependent);
        this.targetClass = targetClass;
        this.target = target;
    }

    public ConcreteCall(CodeLocation caller,
        Clazz targetClass,
        Method target,
        int throwsNullptr,
        byte invocationOpcode,
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
             invocationOpcode,
             controlFlowDependent,
             runtimeTypeDependent);
    }

    @Override
    public MethodSignature getTarget()
    {
        return (MethodSignature) Signature.of(targetClass, target);
    }

    public Clazz getTargetClass()
    {
        return targetClass;
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
