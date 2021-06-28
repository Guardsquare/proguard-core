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

import kotlinx.metadata.*;
import proguard.classfile.*;
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.util.*;

import java.util.Map;
import java.util.Objects;

public class KotlinAnnotation
extends      SimpleProcessable
implements   Processable
{
    public KmAnnotation kmAnnotation;
    public Clazz        referencedAnnotationClass;

    // Keys correspond to methods in Java class files.
    public Map<String, Method> referencedArgumentMethods;

    public KotlinAnnotation(KmAnnotation kmAnnotation)
    {
        this.kmAnnotation = kmAnnotation;
    }


    public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotationVisitor kotlinAnnotationVisitor)
    {
        kotlinAnnotationVisitor.visitAnyAnnotation(clazz, annotatable, this);
    }

    public void accept(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata, KotlinAnnotationVisitor kotlinAnnotationVisitor)
    {
        kotlinAnnotationVisitor.visitTypeAnnotation(clazz, kotlinTypeMetadata, this);
    }


    public void accept(Clazz clazz, KotlinTypeAliasMetadata kotlinTypeAliasMetadata, KotlinAnnotationVisitor kotlinAnnotationVisitor)
    {
        kotlinAnnotationVisitor.visitTypeAliasAnnotation(clazz, kotlinTypeAliasMetadata, this);
    }


    public void accept(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata, KotlinAnnotationVisitor kotlinAnnotationVisitor)
    {
        kotlinAnnotationVisitor.visitTypeParameterAnnotation(clazz, kotlinTypeParameterMetadata, this);
    }


    public void referencedClassAccept(ClassVisitor classVisitor)
    {
        if (this.referencedAnnotationClass != null)
        {
            this.referencedAnnotationClass.accept(classVisitor);
        }
    }


    // Implementations for Object.

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KotlinMetadataAnnotation that = (KotlinMetadataAnnotation) o;
        return kmAnnotation.equals(that.kmAnnotation);
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(kmAnnotation);
    }


    @Override
    public String toString()
    {
        return kmAnnotation.getClassName() + "(" + kmAnnotation.getArguments() + ")";
    }
}
