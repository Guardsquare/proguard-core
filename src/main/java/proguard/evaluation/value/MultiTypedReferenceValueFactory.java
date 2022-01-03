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

package proguard.evaluation.value;

import proguard.analysis.CallResolver;
import proguard.analysis.datastructure.callgraph.SymbolicCall;
import proguard.classfile.*;
import proguard.classfile.constant.FieldrefConstant;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassCollector;

import java.util.*;
import java.util.stream.*;

/**
 * This class provides a wrapper around {@link TypedReferenceValueFactory}
 * that provides new {@link MultiTypedReferenceValue}s.
 *
 * @author Samuel Hopstock
 */
public class MultiTypedReferenceValueFactory extends TypedReferenceValueFactory
{
    private boolean addSubClasses;
    private final ClassPool programClassPool;
    private final ClassPool libraryClassPool;

    public MultiTypedReferenceValueFactory()
    {
        this(false, null, null);
    }


    /**
     * See {@link #MultiTypedReferenceValueFactory()}
     * Parameters, fields and return values have included in possible types all sub classes.S
     *
     * @param addSubClasses a flag indicating if possible types should be extended with subclasses
     * @param programClassPool program {@link ClassPool} to search for reference class
     * @param libraryClassPool library {@link ClassPool} to search for reference class
     */
    public MultiTypedReferenceValueFactory(boolean addSubClasses, ClassPool programClassPool, ClassPool libraryClassPool)
    {
        this.addSubClasses = addSubClasses;
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;
    }

    private MultiTypedReferenceValue wrap(ReferenceValue base)
    {
        if (base instanceof MultiTypedReferenceValue)
        {
            return (MultiTypedReferenceValue) base;
        }
        else if (base instanceof TypedReferenceValue)
        {
            return new MultiTypedReferenceValue((TypedReferenceValue) base, false);
        }
        throw new IllegalStateException("Can't handle value of type " + base.getClass().getSimpleName());
    }

    @Override
    public ReferenceValue createReferenceValueNull()
    {
        return wrap(super.createReferenceValueNull());
    }

    @Override
    public ReferenceValue createReferenceValue(String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull)
    {
        return wrap(super.createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull));
    }

    @Override
    public ReferenceValue createArrayReferenceValue(String type, Clazz referencedClass, IntegerValue arrayLength)
    {
        return wrap(super.createArrayReferenceValue(type, referencedClass, arrayLength));
    }

    @Override
    public ReferenceValue createArrayReferenceValue(String type, Clazz referencedClass, IntegerValue arrayLength, Value elementValue)
    {
        return wrap(super.createArrayReferenceValue(type, referencedClass, arrayLength, elementValue));
    }

    @Override
    public Value createValue(String  type,
                             Clazz referencedClass,
                             boolean mayBeExtension,
                             boolean mayBeNull)
    {
        Value ret = super.createValue(type, referencedClass, mayBeExtension, mayBeNull);
        if (!addSubClasses) // if the flag is not set use default method
        {
            return ret;
        }
        if (ret instanceof MultiTypedReferenceValue)
        {
            MultiTypedReferenceValue multiTypedRet = (MultiTypedReferenceValue) ret;
            Set<Clazz> subClasses = new HashSet<>();
            for (TypedReferenceValue t : multiTypedRet.getPotentialTypes())
            {
                // do not add subclasses as possible types if there is java/lang/Object in possible types
                if (t.type.equals(ClassConstants.NAME_JAVA_LANG_OBJECT))
                {
                    return ret;
                }
                Clazz realReferencedClass = programClassPool.getClass(t.type);
                if (realReferencedClass == null)
                {
                    realReferencedClass = libraryClassPool.getClass(t.type);
                }
                if (realReferencedClass == null)
                {
                    return ret;
                }
                realReferencedClass.hierarchyAccept(true, false, false, true, new ClassCollector(subClasses));
            }

            Set<TypedReferenceValue> possibleTypes = subClasses.stream()
                                                               .map(cls -> new TypedReferenceValue(cls.getName(),
                                                                                                   cls,
                                                                                                   mayBeExtension,
                                                                                                   mayBeNull))
                                                               .collect(Collectors.toCollection(HashSet::new));
            possibleTypes.addAll(multiTypedRet.getPotentialTypes());

            ret = new MultiTypedReferenceValue(possibleTypes, multiTypedRet.mayBeUnknown);
        }
        return ret;
    }
}
