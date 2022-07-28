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
package proguard.classfile.attribute.annotation;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.annotation.visitor.*;

/**
 * This {@link Attribute} represents a type annotations attribute.
 *
 * @author Eric Lafortune
 */
public abstract class TypeAnnotationsAttribute extends AnnotationsAttribute
{
    /**
     * Creates an uninitialized TypeAnnotationsAttribute.
     */
    protected TypeAnnotationsAttribute()
    {
    }


    /**
     * Creates an initialized TypeAnnotationsAttribute.
     */
    protected TypeAnnotationsAttribute(int              u2attributeNameIndex,
                                       int              u2annotationsCount,
                                       TypeAnnotation[] annotations)
    {
        super(u2attributeNameIndex, u2annotationsCount, annotations);
    }


    /**
     * Applies the given visitor to all class annotations.
     */
    public void typeAnnotationsAccept(Clazz clazz, TypeAnnotationVisitor typeAnnotationVisitor)
    {
        TypeAnnotation[] annotations = (TypeAnnotation[])this.annotations;

        for (int index = 0; index < u2annotationsCount; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of Annotation.
            typeAnnotationVisitor.visitTypeAnnotation(clazz, annotations[index]);
        }
    }


    /**
     * Applies the given visitor to all field annotations.
     */
    public void typeAnnotationsAccept(Clazz clazz, Field field, TypeAnnotationVisitor typeAnnotationVisitor)
    {
        TypeAnnotation[] annotations = (TypeAnnotation[])this.annotations;

        for (int index = 0; index < u2annotationsCount; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of Annotation.
            typeAnnotationVisitor.visitTypeAnnotation(clazz, field, annotations[index]);
        }
    }


    /**
     * Applies the given visitor to all method annotations.
     */
    public void typeAnnotationsAccept(Clazz clazz, Method method, TypeAnnotationVisitor typeAnnotationVisitor)
    {
        TypeAnnotation[] annotations = (TypeAnnotation[])this.annotations;

        for (int index = 0; index < u2annotationsCount; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of Annotation.
            typeAnnotationVisitor.visitTypeAnnotation(clazz, method, annotations[index]);
        }
    }


    /**
     * Applies the given visitor to all code attribute annotations.
     */
    public void typeAnnotationsAccept(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotationVisitor typeAnnotationVisitor)
    {
        TypeAnnotation[] annotations = (TypeAnnotation[])this.annotations;

        for (int index = 0; index < u2annotationsCount; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of Annotation.
            typeAnnotationVisitor.visitTypeAnnotation(clazz, method, codeAttribute, annotations[index]);
        }
    }
}
