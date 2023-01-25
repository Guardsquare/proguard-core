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
package proguard.classfile.kotlin;

import proguard.classfile.*;
import proguard.classfile.kotlin.flags.KotlinFunctionFlags;
import proguard.classfile.kotlin.visitor.*;
import proguard.classfile.visitor.MemberVisitor;
import proguard.util.*;

import java.util.*;

public class KotlinFunctionMetadata
extends      SimpleProcessable
implements   Processable
{
    public String name;


    public List<KotlinContractMetadata> contracts;

    public KotlinTypeMetadata receiverType;

    public List<KotlinTypeMetadata> contextReceivers;

    public KotlinTypeMetadata returnType;

    public List<KotlinTypeParameterMetadata> typeParameters;

    public List<KotlinValueParameterMetadata> valueParameters;

    public KotlinVersionRequirementMetadata versionRequirement;

    public KotlinFunctionFlags flags;

    // Extensions.
    public MethodSignature jvmSignature;
    public Method          referencedMethod;
    public Clazz           referencedMethodClass;

    // Functions with default parameters have a corresponding $default method.
    // this could be in the $DefaultImpls of an interface, so we need a class ref as well.
    public Method          referencedDefaultMethod;
    public Clazz           referencedDefaultMethodClass;

    // Interfaces with non-abstract methods have an implementation in a corresponding inner class called DefaultImpls.
    public Method          referencedDefaultImplementationMethod;
    public Clazz           referencedDefaultImplementationMethodClass;

    // The JVM internal name of the original class the lambda class where this function is copied from. This
    // information is present for lambdas copied from bodies of inline functions to the use site by kotlinc.
    public String          lambdaClassOriginName;
    public Clazz           referencedLambdaClassOrigin;


    public KotlinFunctionMetadata(KotlinFunctionFlags flags, String name)
    {
        this.name  = name;
        this.flags = flags;
    }


    public void accept(Clazz                              clazz,
                       KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                       KotlinFunctionVisitor              kotlinFunctionVisitor)
    {
        kotlinFunctionVisitor.visitFunction(clazz, kotlinDeclarationContainerMetadata, this);
    }


    public void accept(Clazz                            clazz,
                       KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata,
                       KotlinFunctionVisitor            kotlinFunctionVisitor)
    {
        kotlinFunctionVisitor.visitSyntheticFunction(clazz, kotlinSyntheticClassKindMetadata, this);
    }


    public void valueParametersAccept(Clazz                       clazz,
                                      KotlinMetadata              kotlinMetadata,
                                      KotlinValueParameterVisitor kotlinValueParameterVisitor)
    {
        if (valueParameters != null)
        {
            for (KotlinValueParameterMetadata valueParameter : valueParameters)
            {
                valueParameter.accept(clazz, kotlinMetadata, this, kotlinValueParameterVisitor);
            }
        }
    }


    public void typeParametersAccept(Clazz clazz, KotlinMetadata kotlinMetadata, KotlinTypeParameterVisitor kotlinTypeParameterVisitor)
    {
        if (typeParameters != null)
        {
            for (KotlinTypeParameterMetadata typeParameter : typeParameters)
            {
                typeParameter.accept(clazz, kotlinMetadata, this, kotlinTypeParameterVisitor);
            }
        }
    }


    public void returnTypeAccept(Clazz             clazz,
                                 KotlinMetadata    kotlinMetadata,
                                 KotlinTypeVisitor kotlinTypeVisitor)
    {
        kotlinTypeVisitor.visitFunctionReturnType(clazz, kotlinMetadata, this, returnType);
    }


    public void receiverTypeAccept(Clazz             clazz,
                                   KotlinMetadata    kotlinMetadata,
                                   KotlinTypeVisitor kotlinTypeVisitor)
    {
        if (receiverType != null)
        {
            kotlinTypeVisitor.visitFunctionReceiverType(clazz,
                                                        kotlinMetadata,
                                                        this,
                                                        receiverType);
        }
    }

    public void contextReceiverTypesAccept(Clazz             clazz,
                                           KotlinMetadata    kotlinMetadata,
                                           KotlinTypeVisitor kotlinTypeVisitor)
    {
        if (contextReceivers != null)
        {
            for (KotlinTypeMetadata contextReceiver : contextReceivers)
            {
                kotlinTypeVisitor.visitFunctionContextReceiverType(clazz, kotlinMetadata, this, contextReceiver);
            }
        }
    }

    public void contractsAccept(Clazz                 clazz,
                                KotlinMetadata        kotlinMetadata,
                                KotlinContractVisitor kotlinContractVisitor)
    {
        if (contracts != null)
        {
            for (KotlinContractMetadata contract : contracts)
            {
                contract.accept(clazz, kotlinMetadata, this, kotlinContractVisitor);
            }
        }
    }


    public void versionRequirementAccept(Clazz                           clazz,
                                         KotlinMetadata                  kotlinMetadata,
                                         KotlinVersionRequirementVisitor kotlinVersionRequirementVisitor)
    {
        if (versionRequirement != null)
        {
            versionRequirement.accept(clazz, kotlinMetadata, this, kotlinVersionRequirementVisitor);
        }
    }


    @Deprecated
    public void referencedMethodAccept(Clazz clazz, MemberVisitor methodVisitor)
    {
        referencedMethodAccept(methodVisitor);
    }

    public void referencedMethodAccept(MemberVisitor methodVisitor)
    {
        if (referencedMethod      != null &&
            referencedMethodClass != null)
        {
            referencedMethod.accept(referencedMethodClass, methodVisitor);
        }
    }

    public void referencedDefaultMethodAccept(MemberVisitor methodVisitor)
    {
        if (referencedDefaultMethod      != null &&
            referencedDefaultMethodClass != null)
        {
            referencedDefaultMethod.accept(referencedDefaultMethodClass, methodVisitor);
        }
    }

    public void referencedDefaultImplementationMethodAccept(MemberVisitor memberVisitor)
    {
        if (referencedDefaultImplementationMethodClass != null &&
            referencedDefaultImplementationMethod      != null)
        {
            referencedDefaultImplementationMethod.accept(referencedDefaultImplementationMethodClass, memberVisitor);
        }
    }


    // Implementations for Object.
    @Override
    public String toString()
    {
        return "Kotlin " +
               (flags.isSynthesized ? "synthesized " : "") +
               "function(" + name + ")";
    }
}
