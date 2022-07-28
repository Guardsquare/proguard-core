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

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;

/**
 * FunctionReference info.
 *
 * @author James Hamilton
 */
public class FunctionReferenceInfo
implements   CallableReferenceInfo
{
    private final Clazz                              ownerClass;
    private final KotlinDeclarationContainerMetadata ownerMetadata;
    private final KotlinFunctionMetadata             functionMetadata;

    public FunctionReferenceInfo(Clazz                              ownerClass,
                                 KotlinDeclarationContainerMetadata ownerMetadata,
                                 KotlinFunctionMetadata             functionMetadata)
    {
        this.ownerClass       = ownerClass;
        this.functionMetadata = functionMetadata;
        this.ownerMetadata    = ownerMetadata;
    }

    @Override
    public String getName()
    {
        return functionMetadata.name;
    }

    /**
     * For functions this is just their name and descriptor.
     *
     * @return The JVM signature.
     */
    @Override
    public String getSignature()
    {
        return functionMetadata.jvmSignature.method + functionMetadata.jvmSignature.descriptor;
    }


    @Override
    public KotlinDeclarationContainerMetadata getOwner()
    {
        return this.ownerMetadata;
    }


    @Override
    public void accept(CallableReferenceInfoVisitor callableReferenceInfoVisitor)
    {
        callableReferenceInfoVisitor.visitFunctionReferenceInfo(this);
    }

    @Override
    public void ownerAccept(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        this.ownerMetadata.accept(this.ownerClass, kotlinMetadataVisitor);
    }
}
