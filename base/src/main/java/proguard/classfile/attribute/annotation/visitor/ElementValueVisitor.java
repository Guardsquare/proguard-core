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
package proguard.classfile.attribute.annotation.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.attribute.annotation.*;

/**
 * This interface specifies the methods for a visitor of {@link ElementValue}
 * instances.
 *
 * @author Eric Lafortune
 */
public interface ElementValueVisitor
{
    /**
     * Visits any ElementValue instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyElementValue(Clazz clazz, Annotation annotation, ElementValue elementValue)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+elementValue.getClass().getName());
    }


    default void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
        visitAnyElementValue(clazz, annotation, constantElementValue);
    }


    default void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        visitAnyElementValue(clazz, annotation, enumConstantElementValue);
    }


    default void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        visitAnyElementValue(clazz, annotation, classElementValue);
    }


    default void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        visitAnyElementValue(clazz, annotation, annotationElementValue);
    }


    default void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        visitAnyElementValue(clazz, annotation, arrayElementValue);
    }
}
