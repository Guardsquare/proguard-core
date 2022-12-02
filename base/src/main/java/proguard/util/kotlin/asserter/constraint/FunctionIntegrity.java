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
package proguard.util.kotlin.asserter.constraint;

import static proguard.classfile.kotlin.KotlinConstants.DEFAULT_METHOD_SUFFIX;

import proguard.classfile.AccessConstants;
import proguard.classfile.Clazz;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeCounter;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinConstants;
import proguard.classfile.kotlin.KotlinFunctionMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.visitor.AllFunctionVisitor;
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.util.kotlin.asserter.AssertUtil;

/**
 * This class checks the assumption: All functions need a JVM signature
 */
public class FunctionIntegrity
extends      AbstractKotlinMetadataConstraint
implements   KotlinFunctionVisitor
{

    private boolean hasDefaults = false;

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
        kotlinMetadata.accept(clazz, new AllFunctionVisitor(this));
    }

    // Implementations for KotlinFunctionVisitor.
    @Override
    public void visitAnyFunction(Clazz                  clazz,
                                 KotlinMetadata         kotlinMetadata,
                                 KotlinFunctionMetadata kotlinFunctionMetadata)
    {
        AssertUtil util = new AssertUtil("Function " + kotlinFunctionMetadata.name, reporter, programClassPool, libraryClassPool);

        util.reportIfNull("jvmSignature", kotlinFunctionMetadata.jvmSignature);

        util.reportIfNullReference("referencedMethod", kotlinFunctionMetadata.referencedMethod);
        util.reportIfNullReference("referencedMethodClass", kotlinFunctionMetadata.referencedMethodClass);
        util.reportIfClassDangling("referencedMethodClass", kotlinFunctionMetadata.referencedMethodClass);

        util.reportIfMethodDangling("referenced method",
                                    kotlinFunctionMetadata.referencedMethodClass,
                                    kotlinFunctionMetadata.referencedMethod);

        util.reportIfMethodDangling(
            "referenced default method",
            kotlinFunctionMetadata.referencedDefaultMethodClass,
            kotlinFunctionMetadata.referencedDefaultMethod);

        util.reportIfMethodDangling(
            "referenced default implementation method",
            kotlinFunctionMetadata.referencedDefaultImplementationMethodClass,
            kotlinFunctionMetadata.referencedDefaultImplementationMethod);

        // If any parameter has a hasDefault flag, then there should be a corresponding $default method.
        hasDefaults = false;
        kotlinFunctionMetadata.valueParametersAccept(clazz, kotlinMetadata, (_clazz, vp) -> hasDefaults |= vp.flags.hasDefaultValue);
        if (hasDefaults)
        {
            boolean hasDefaultMethod =
                kotlinFunctionMetadata.referencedDefaultMethod != null &&
                kotlinFunctionMetadata.referencedDefaultMethod.getName(kotlinFunctionMetadata.referencedDefaultMethodClass)
                    .equals(kotlinFunctionMetadata.referencedMethod.getName(kotlinFunctionMetadata.referencedMethodClass) + DEFAULT_METHOD_SUFFIX);

            if (!hasDefaultMethod)
            {
                reporter.report(kotlinFunctionMetadata.name + DEFAULT_METHOD_SUFFIX + " method not found [" + kotlinFunctionMetadata.jvmSignature + "]");
            }

            if (kotlinFunctionMetadata.referencedDefaultMethod      != null &&
                kotlinFunctionMetadata.referencedDefaultMethodClass != null)
            {
                // $default methods should have at least 2 extra parameters (integer bitmask(s) and an object param).
                int methodParameterCount   = ClassUtil.internalMethodParameterCount(kotlinFunctionMetadata.referencedMethod.getDescriptor(kotlinFunctionMetadata.referencedMethodClass));
                int defaultParameterCount  = ClassUtil.internalMethodParameterCount(kotlinFunctionMetadata.referencedDefaultMethod.getDescriptor(kotlinFunctionMetadata.referencedDefaultMethodClass));
                int expectedParameterCount = methodParameterCount + (2 + (methodParameterCount / 32));
                if ((kotlinFunctionMetadata.referencedMethod.getAccessFlags() & AccessConstants.STATIC) == 0)
                {
                    // Static $default methods have the instance as first param.
                    expectedParameterCount++;
                }
                if (defaultParameterCount != expectedParameterCount)
                {
                    reporter.report("Expected " + expectedParameterCount + " parameter" + (expectedParameterCount > 1 ? "s" : "") + " for " +
                                    kotlinFunctionMetadata.referencedDefaultMethod.getName(kotlinFunctionMetadata.referencedDefaultMethodClass) +
                                    kotlinFunctionMetadata.referencedDefaultMethod.getDescriptor(kotlinFunctionMetadata.referencedDefaultMethodClass) +
                                    " method");
                }
            }
        }

        // If the function is non-abstract and in an interface,
        // then there must be a default implementation in the $DefaultImpls class,
        // unless using Java 8+ ability to have default methods in interfaces.
        if (!kotlinFunctionMetadata.flags.modality.isAbstract &&
            kotlinMetadata.k == KotlinConstants.METADATA_KIND_CLASS)
        {
            KotlinClassKindMetadata kotlinClassKindMetadata = (KotlinClassKindMetadata)kotlinMetadata;
            if (kotlinClassKindMetadata.flags.isInterface)
            {
                AttributeCounter attributeCounter = new AttributeCounter();
                kotlinFunctionMetadata.referencedMethodAccept(
                    new AllAttributeVisitor(
                        new AttributeNameFilter(Attribute.CODE, attributeCounter)
                    )
                );

                if (attributeCounter.getCount() != 1)
                {
                    util.reportIfNullReference("default implementation method",
                                               kotlinFunctionMetadata.referencedDefaultImplementationMethod);
                    util.reportIfMethodDangling("default implementation method",
                                                kotlinFunctionMetadata.referencedDefaultImplementationMethodClass,
                                                kotlinFunctionMetadata.referencedDefaultImplementationMethod);
                    util.reportIfNullReference("default implementation method class",
                                               kotlinFunctionMetadata.referencedDefaultImplementationMethodClass);
                    util.reportIfClassDangling("default implementation method class",
                                               kotlinFunctionMetadata.referencedDefaultImplementationMethodClass);
                }
            }
        }

    }
}
