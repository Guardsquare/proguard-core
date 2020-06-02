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

package proguard.classfile.kotlin.reflect;

import proguard.classfile.*;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;

/**
 * @author James Hamilton
 */
public abstract class JavaReferenceInfo
implements            CallableReferenceInfo
{
    private final   Clazz  ownerClass;
    protected final Clazz  clazz;
    protected final Member member;

    public JavaReferenceInfo(Clazz ownerClass, Clazz clazz, Member member)
    {
        this.ownerClass = ownerClass;
        this.clazz      = clazz;
        this.member     = member;
    }

    @Override
    public String getName()
    {
        return this.member.getName(this.clazz);
    }

    @Override
    public String getSignature()
    {
        return this.getName() + this.member.getDescriptor(this.clazz);
    }


    @Override
    public KotlinDeclarationContainerMetadata getOwner()
    {
        return null;
    }

    @Override
    public void accept(CallableReferenceInfoVisitor callableReferenceInfoVisitor)
    {
        callableReferenceInfoVisitor.visitJavaReferenceInfo(this);
    }

    @Override
    public void ownerAccept(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        // There's no owner.
    }
}
