/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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
package proguard.util.kotlin.asserter.constraint;

import static proguard.classfile.kotlin.KotlinAnnotationArgument.ClassValue;
import static proguard.classfile.kotlin.KotlinAnnotationArgument.EnumValue;
import static proguard.classfile.kotlin.KotlinAnnotationArgument.Value;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinAnnotatable;
import proguard.classfile.kotlin.KotlinAnnotation;
import proguard.classfile.kotlin.KotlinAnnotationArgument;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinTypeAliasMetadata;
import proguard.classfile.kotlin.KotlinTypeMetadata;
import proguard.classfile.kotlin.KotlinTypeParameterMetadata;
import proguard.classfile.kotlin.visitor.AllTypeAliasVisitor;
import proguard.classfile.kotlin.visitor.AllTypeParameterVisitor;
import proguard.classfile.kotlin.visitor.AllTypeVisitor;
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor;
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeAliasVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor;
import proguard.classfile.kotlin.visitor.MultiKotlinMetadataVisitor;
import proguard.util.kotlin.asserter.AssertUtil;

/**
 * This class checks the assumption: All properties need a JVM signature for their getter
 */
public class KmAnnotationIntegrity
extends      AbstractKotlinMetadataConstraint
implements   KotlinTypeVisitor,
             KotlinTypeAliasVisitor,
             KotlinTypeParameterVisitor,
             KotlinAnnotationVisitor,
             KotlinAnnotationArgumentVisitor
{

    private AssertUtil util;


    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
        kotlinMetadata.accept(clazz, new MultiKotlinMetadataVisitor(
            new AllTypeVisitor(         this),
            new AllTypeAliasVisitor(    this),
            new AllTypeParameterVisitor(this)
        ));
    }

    // Implementations for KotlinTypeVisitor.
    @Override
    public void visitAnyType(Clazz clazz, KotlinTypeMetadata type)
    {
        util = new AssertUtil("Type", reporter,programClassPool, libraryClassPool);
        type.annotationsAccept(clazz, this);

    }

    // Implementations for KotlinTypeAliasVisitor.
    @Override
    public void visitTypeAlias(Clazz                              clazz,
                               KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                               KotlinTypeAliasMetadata            kotlinTypeAliasMetadata)
    {
        util = new AssertUtil("Type alias", reporter,programClassPool, libraryClassPool);
        kotlinTypeAliasMetadata.annotationsAccept(clazz, this);
    }

    // Implementations for KotlinTypeParameterVisitor.
    @Override
    public void visitAnyTypeParameter(Clazz                       clazz,
                                      KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
    {
        util = new AssertUtil("Type parameter", reporter,programClassPool, libraryClassPool);
        kotlinTypeParameterMetadata.annotationsAccept(clazz, this);
    }

    // Implementations for KotlinAnnotationVisitor.
    @Override
    public void visitAnyAnnotation(Clazz                clazz,
                                   KotlinAnnotatable    annotatable,
                                   KotlinAnnotation     antn)
    {

        // TODO: there's an annotation added by the compiler, ParameterName, but it's not in the
        //       class pool - should this be a dummy class, are there more?
        //       util.reportIfClassDangling("annotation class", antn.referencedAnnotationClass);

        util.reportIfNullReference("annotation class", antn.referencedAnnotationClass);
        antn.argumentsAccept(clazz, annotatable, this);
    }

    // Implementations for KotlinAnnotationArgumentVisitor.
    @Override
    public void visitAnyArgument(Clazz                    clazz,
                                 KotlinAnnotatable        annotatable,
                                 KotlinAnnotation         annotation,
                                 KotlinAnnotationArgument argument,
                                 Value                    value)
    {
        util.reportIfNullReference("annotation method", argument.referencedAnnotationMethod);
        util.reportIfMethodDangling("annotation method", argument.referencedAnnotationMethodClass, argument.referencedAnnotationMethod);
        util.reportIfNullReference("annotation class", argument.referencedAnnotationMethodClass);
        util.reportIfClassDangling("annotation class", argument.referencedAnnotationMethodClass);
    }

    @Override
    public void visitClassArgument(Clazz                               clazz,
                                   KotlinAnnotatable                   annotatable,
                                   KotlinAnnotation                    annotation,
                                   KotlinAnnotationArgument            argument,
                                   ClassValue value)
    {
        visitAnyArgument(clazz, annotatable, annotation, argument, value);
        util.reportIfNullReference("annotation argument class referenced", value.referencedClass);
        util.reportIfClassDangling("annotation argument class referenced", value.referencedClass);
    }

    @Override
    public void visitEnumArgument(Clazz                    clazz,
                                  KotlinAnnotatable        annotatable,
                                  KotlinAnnotation         annotation,
                                  KotlinAnnotationArgument argument,
                                  EnumValue                value)
    {
        visitAnyArgument(clazz, annotatable, annotation, argument, value);
        util.reportIfNullReference("annotation argument enum referenced", value.referencedClass);
        util.reportIfClassDangling("annotation argument enum referenced", value.referencedClass);
    }
}
