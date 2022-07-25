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

import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;

/**
 * Information about callable references.
 *
 * @author James Hamilton
 */
public interface CallableReferenceInfo
{
    /**
     * The Kotlin name of the callable, the one which was declared in the source code (@JvmName doesn't change it).
     *
     * @return The Kotlin name.
     */
    String getName();

    /**
     * The signature of the callable.
     *
     * @return The signature.
     */
    String getSignature();


    /**
     * The class or package where the callable should be located, usually specified on the LHS of the '::' operator.
     *
     * Note: this is not necessarily the location where the callable is *declared* - it could be declared in
     * a superclass.
     *
     * @return The owner.
     */
    KotlinDeclarationContainerMetadata getOwner();

    void accept(CallableReferenceInfoVisitor callableReferenceInfoVisitor);

    void ownerAccept(KotlinMetadataVisitor kotlinMetadataVisitor);
}
