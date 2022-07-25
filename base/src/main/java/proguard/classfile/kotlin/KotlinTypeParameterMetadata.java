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
import proguard.classfile.kotlin.flags.KotlinTypeParameterFlags;
import proguard.classfile.kotlin.visitor.*;
import proguard.util.*;

import java.util.*;

public class KotlinTypeParameterMetadata
extends      SimpleProcessable
implements   Processable,
             KotlinAnnotatable
{
    public String name;

    public int id;

    public KotlinTypeVariance variance;

    public List<KotlinTypeMetadata> upperBounds;

    public KotlinTypeParameterFlags flags;

    // Extensions.
    public List<KotlinAnnotation> annotations;


    public KotlinTypeParameterMetadata(KotlinTypeParameterFlags flags, String name, int id, KotlinTypeVariance variance)
    {
        this.name     = name;
        this.id       = id;
        this.variance = variance;
        this.flags    = flags;
    }


    public void accept(Clazz                      clazz,
                       KotlinClassKindMetadata    kotlinClassKindMetadata,
                       KotlinTypeParameterVisitor kotlinTypeParameterVisitor)
    {
        kotlinTypeParameterVisitor.visitClassTypeParameter(clazz, kotlinClassKindMetadata, this);
    }


    public void accept(Clazz                              clazz,
                       KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                       KotlinPropertyMetadata             kotlinPropertyMetadata,
                       KotlinTypeParameterVisitor         kotlinTypeParameterVisitor)
    {
        kotlinTypeParameterVisitor.visitPropertyTypeParameter(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata, this);
    }


    public void accept(Clazz                      clazz,
                       KotlinMetadata             kotlinMetadata,
                       KotlinFunctionMetadata     kotlinFunctionMetadata,
                       KotlinTypeParameterVisitor kotlinTypeParameterVisitor)
    {
        kotlinTypeParameterVisitor.visitFunctionTypeParameter(clazz, kotlinMetadata, kotlinFunctionMetadata, this);
    }


    public void accept(Clazz                              clazz,
                       KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                       KotlinTypeAliasMetadata            kotlinPropertyMetadata,
                       KotlinTypeParameterVisitor         kotlinTypeParameterVisitor)
    {
        kotlinTypeParameterVisitor.visitAliasTypeParameter(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata, this);
    }


    public void upperBoundsAccept(Clazz             clazz,
                                  KotlinTypeVisitor kotlinTypeVisitor)
    {
        for (KotlinTypeMetadata upperBound : upperBounds)
        {
            upperBound.accept(clazz, this, kotlinTypeVisitor);
        }
    }

    public void annotationsAccept(Clazz                   clazz,
                                  KotlinAnnotationVisitor kotlinAnnotationVisitor)
    {
        for (KotlinAnnotation annotation : annotations)
        {
            kotlinAnnotationVisitor.visitTypeParameterAnnotation(clazz, this, annotation);
        }
    }


    // Implementations for Object.
    @Override
    public String toString()
    {
        return "Kotlin " +
               (flags.isReified ? "primary " : "") +
               "constructor";
    }
}
