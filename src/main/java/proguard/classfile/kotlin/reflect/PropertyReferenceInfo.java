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

import static proguard.classfile.util.kotlin.KotlinNameUtil.generateGetterName;

/**
 * Property reference info.
 *
 * @author James Hamilton
 */
public class PropertyReferenceInfo
implements   CallableReferenceInfo
{

    private final Clazz                              ownerClass;
    private final KotlinDeclarationContainerMetadata ownerMetadata;
    private final KotlinPropertyMetadata             propertyMetadata;

    public PropertyReferenceInfo(Clazz                              ownerClass,
                                 KotlinDeclarationContainerMetadata ownerMetadata,
                                 KotlinPropertyMetadata             propertyMetadata)
    {
        this.ownerClass       = ownerClass;
        this.propertyMetadata = propertyMetadata;
        this.ownerMetadata    = ownerMetadata;
    }

    @Override
    public String getName()
    {
        return propertyMetadata.name;
    }


    /**
     * For properties this is the signature of it's JVM getter method.
     *
     * If the property has no getter in the bytecode (e.g. private property in a class), it's still the signature of
     * the imaginary default getter that would be generated otherwise e.g. "myProperty" -> "getMyProperty".
     *
     * @return the signature.
     */
    @Override
    public String getSignature()
    {
        return propertyMetadata.getterSignature != null        ?
                   propertyMetadata.getterSignature.method + propertyMetadata.getterSignature.descriptor :
                   generateGetterName(propertyMetadata.backingFieldSignature.memberName) +
                                      "()" + propertyMetadata.backingFieldSignature.descriptor;
    }


    @Override
    public KotlinDeclarationContainerMetadata getOwner()
    {
        return this.ownerMetadata;
    }

    @Override
    public void accept(CallableReferenceInfoVisitor callableReferenceInfoVisitor)
    {
        callableReferenceInfoVisitor.visitPropertyReferenceInfo(this);
    }

    @Override
    public void ownerAccept(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        this.ownerMetadata.accept(this.ownerClass, kotlinMetadataVisitor);
    }
}
