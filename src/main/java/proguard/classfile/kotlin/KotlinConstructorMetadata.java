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
package proguard.classfile.kotlin;

import proguard.classfile.*;
import proguard.classfile.kotlin.flags.KotlinConstructorFlags;
import proguard.classfile.kotlin.visitor.*;
import proguard.classfile.visitor.MemberVisitor;
import proguard.util.*;

import java.util.*;

public class KotlinConstructorMetadata
extends    SimpleProcessable
implements Processable
{
    public List<KotlinValueParameterMetadata> valueParameters;

    public KotlinVersionRequirementMetadata versionRequirement;

    public KotlinConstructorFlags flags;

    // Extensions.
    public MethodSignature jvmSignature;
    public Method          referencedMethod;


    public KotlinConstructorMetadata(KotlinConstructorFlags flags)
    {
        this.flags = flags;
    }


    public void accept(Clazz                    clazz,
                       KotlinClassKindMetadata  kotlinClassKindMetadata,
                       KotlinConstructorVisitor kotlinConstructorVisitor)
    {
        kotlinConstructorVisitor.visitConstructor(clazz, kotlinClassKindMetadata, this);
    }


    public void valueParametersAccept(Clazz                       clazz,
                                      KotlinClassKindMetadata     kotlinClassKindMetadata,
                                      KotlinValueParameterVisitor kotlinValueParameterVisitor)
    {
        for (KotlinValueParameterMetadata valueParameter : valueParameters)
        {
            valueParameter.accept(clazz, kotlinClassKindMetadata, this, kotlinValueParameterVisitor);
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


    public void referencedMethodAccept(Clazz clazz, MemberVisitor methodVisitor)
    {
        if (referencedMethod != null)
        {
            referencedMethod.accept(clazz, methodVisitor);
        }
    }

    public boolean isParameterless()
    {
        return this.valueParameters.isEmpty();
    }

    // Implementations for Object.
    @Override
    public String toString()
    {
        return "Kotlin " +
               (flags.isPrimary ? "primary " : "") +
               "constructor";
    }
}
