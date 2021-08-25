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
package proguard.classfile.attribute.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.module.*;
import proguard.classfile.attribute.preverification.*;

/**
 * This interface specifies the methods for a visitor of {@link Attribute}
 * instances.
 *
 * @author Eric Lafortune
 */
public interface AttributeVisitor
{
    /**
     * Visits any Attribute instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyAttribute(Clazz clazz, Attribute attribute)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+attribute.getClass().getName());
    }


    default void visitUnknownAttribute(Clazz clazz, UnknownAttribute unknownAttribute)
    {
        visitAnyAttribute(clazz, unknownAttribute);
    }


    default void visitSourceDebugExtensionAttribute(Clazz clazz, SourceDebugExtensionAttribute sourceDebugExtensionAttribute)
    {
        visitAnyAttribute(clazz, sourceDebugExtensionAttribute);
    }


    default void visitBootstrapMethodsAttribute(Clazz clazz, BootstrapMethodsAttribute bootstrapMethodsAttribute)
    {
        visitAnyAttribute(clazz, bootstrapMethodsAttribute);
    }


    default void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute)
    {
        visitAnyAttribute(clazz, sourceFileAttribute);
    }


    default void visitSourceDirAttribute(Clazz clazz, SourceDirAttribute sourceDirAttribute)
    {
        visitAnyAttribute(clazz, sourceDirAttribute);
    }


    default void visitRecordAttribute(Clazz clazz, RecordAttribute recordAttribute)
    {
        visitAnyAttribute(clazz, recordAttribute);
    }


    default void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        visitAnyAttribute(clazz, innerClassesAttribute);
    }


    default void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        visitAnyAttribute(clazz, enclosingMethodAttribute);
    }


    default void visitNestHostAttribute(Clazz clazz, NestHostAttribute nestHostAttribute)
    {
        visitAnyAttribute(clazz, nestHostAttribute);
    }


    default void visitNestMembersAttribute(Clazz clazz, NestMembersAttribute nestMembersAttribute)
    {
        visitAnyAttribute(clazz, nestMembersAttribute);
    }


    default void visitPermittedSubclassesAttribute(Clazz clazz, PermittedSubclassesAttribute permittedSubclassesAttribute)
    {
        visitAnyAttribute(clazz, permittedSubclassesAttribute);
    }


    default void visitModuleAttribute(Clazz clazz, ModuleAttribute moduleAttribute)
    {
        visitAnyAttribute(clazz, moduleAttribute);
    }


    default void visitModuleMainClassAttribute(Clazz clazz, ModuleMainClassAttribute moduleMainClassAttribute)
    {
        visitAnyAttribute(clazz, moduleMainClassAttribute);
    }


    default void visitModulePackagesAttribute(Clazz clazz, ModulePackagesAttribute modulePackagesAttribute)
    {
        visitAnyAttribute(clazz, modulePackagesAttribute);
    }


    default void visitDeprecatedAttribute(Clazz clazz, DeprecatedAttribute deprecatedAttribute)
    {
        visitAnyAttribute(clazz, deprecatedAttribute);
    }



    default void visitDeprecatedAttribute(Clazz clazz, Member member, DeprecatedAttribute deprecatedAttribute)
    {
        visitDeprecatedAttribute(clazz, deprecatedAttribute);
    }


    default void visitDeprecatedAttribute(Clazz clazz, Field field, DeprecatedAttribute deprecatedAttribute)
    {
        visitDeprecatedAttribute(clazz, (Member)field, deprecatedAttribute);
    }


    default void visitDeprecatedAttribute(Clazz clazz, Method method, DeprecatedAttribute deprecatedAttribute)
    {
        visitDeprecatedAttribute(clazz, (Member)method, deprecatedAttribute);
    }


    default void visitSyntheticAttribute(Clazz clazz, SyntheticAttribute syntheticAttribute)
    {
        visitAnyAttribute(clazz, syntheticAttribute);
    }



    default void visitSyntheticAttribute(Clazz clazz, Member member, SyntheticAttribute syntheticAttribute)
    {
        visitSyntheticAttribute(clazz, syntheticAttribute);
    }


    default void visitSyntheticAttribute(Clazz clazz, Field field, SyntheticAttribute syntheticAttribute)
    {
        visitSyntheticAttribute(clazz, (Member)field, syntheticAttribute);
    }


    default void visitSyntheticAttribute(Clazz clazz, Method method, SyntheticAttribute syntheticAttribute)
    {
        visitSyntheticAttribute(clazz, (Member)method, syntheticAttribute);
    }


    default void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        visitAnyAttribute(clazz, signatureAttribute);
    }



    default void visitSignatureAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, SignatureAttribute signatureAttribute)
    {
        visitSignatureAttribute(clazz, signatureAttribute);
    }


    default void visitSignatureAttribute(Clazz clazz, Member member, SignatureAttribute signatureAttribute)
    {
        visitSignatureAttribute(clazz, signatureAttribute);
    }


    default void visitSignatureAttribute(Clazz clazz, Field field, SignatureAttribute signatureAttribute)
    {
        visitSignatureAttribute(clazz, (Member)field, signatureAttribute);
    }


    default void visitSignatureAttribute(Clazz clazz, Method method, SignatureAttribute signatureAttribute)
    {
        visitSignatureAttribute(clazz, (Member)method, signatureAttribute);
    }


    default void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        visitAnyAttribute(clazz, constantValueAttribute);
    }


    default void visitMethodParametersAttribute(Clazz clazz, Method method, MethodParametersAttribute methodParametersAttribute)
    {
        visitAnyAttribute(clazz, methodParametersAttribute);
    }


    default void visitExceptionsAttribute(Clazz clazz, Method method, ExceptionsAttribute exceptionsAttribute)
    {
        visitAnyAttribute(clazz, exceptionsAttribute);
    }


    default void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        visitAnyAttribute(clazz, codeAttribute);
    }


    default void visitStackMapAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapAttribute stackMapAttribute)
    {
        visitAnyAttribute(clazz, stackMapAttribute);
    }


    default void visitStackMapTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapTableAttribute stackMapTableAttribute)
    {
        visitAnyAttribute(clazz, stackMapTableAttribute);
    }


    default void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        visitAnyAttribute(clazz, lineNumberTableAttribute);
    }


    default void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        visitAnyAttribute(clazz, localVariableTableAttribute);
    }


    default void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        visitAnyAttribute(clazz, localVariableTypeTableAttribute);
    }



    /**
     * Visits any AnnotationsAttribute instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        visitAnyAttribute(clazz, annotationsAttribute);
    }


    default void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        visitAnyAnnotationsAttribute(clazz, runtimeVisibleAnnotationsAttribute);
    }



    default void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        visitRuntimeVisibleAnnotationsAttribute(clazz, runtimeVisibleAnnotationsAttribute);
    }


    default void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, Member member, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        visitRuntimeVisibleAnnotationsAttribute(clazz, runtimeVisibleAnnotationsAttribute);
    }


    default void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, Field field, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        visitRuntimeVisibleAnnotationsAttribute(clazz, (Member)field, runtimeVisibleAnnotationsAttribute);
    }


    default void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        visitRuntimeVisibleAnnotationsAttribute(clazz, (Member)method, runtimeVisibleAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        visitAnyAnnotationsAttribute(clazz, runtimeInvisibleAnnotationsAttribute);
    }



    default void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        visitRuntimeInvisibleAnnotationsAttribute(clazz, runtimeInvisibleAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, Member member, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        visitRuntimeInvisibleAnnotationsAttribute(clazz, runtimeInvisibleAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, Field field, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        visitRuntimeInvisibleAnnotationsAttribute(clazz, (Member)field, runtimeInvisibleAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        visitRuntimeInvisibleAnnotationsAttribute(clazz, (Member)method, runtimeInvisibleAnnotationsAttribute);
    }



    /**
     * Visits any ParameterAnnotationsAttribute instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        visitAnyAttribute(clazz, parameterAnnotationsAttribute);
    }


    default void visitRuntimeVisibleParameterAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotationsAttribute)
    {
        visitAnyParameterAnnotationsAttribute(clazz, method, runtimeVisibleParameterAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleParameterAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute)
    {
        visitAnyParameterAnnotationsAttribute(clazz, method, runtimeInvisibleParameterAnnotationsAttribute);
    }



    /**
     * Visits any TypeAnnotationsAttribute instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyTypeAnnotationsAttribute(Clazz clazz, TypeAnnotationsAttribute typeAnnotationsAttribute)
    {
        visitAnyAnnotationsAttribute(clazz, typeAnnotationsAttribute);
    }


    default void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        visitAnyTypeAnnotationsAttribute(clazz, runtimeVisibleTypeAnnotationsAttribute);
    }



    default void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        visitRuntimeVisibleTypeAnnotationsAttribute(clazz, runtimeVisibleTypeAnnotationsAttribute);
    }


    default void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Member member, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        visitRuntimeVisibleTypeAnnotationsAttribute(clazz, runtimeVisibleTypeAnnotationsAttribute);
    }


    default void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Field field, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        visitRuntimeVisibleTypeAnnotationsAttribute(clazz, (Member)field, runtimeVisibleTypeAnnotationsAttribute);
    }


    default void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        visitRuntimeVisibleTypeAnnotationsAttribute(clazz, (Member)method, runtimeVisibleTypeAnnotationsAttribute);
    }


    default void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        visitRuntimeVisibleTypeAnnotationsAttribute(clazz, method, runtimeVisibleTypeAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        visitAnyTypeAnnotationsAttribute(clazz, runtimeInvisibleTypeAnnotationsAttribute);
    }



    default void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, runtimeInvisibleTypeAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Member member, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, runtimeInvisibleTypeAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Field field, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, (Member)field, runtimeInvisibleTypeAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, (Member)method, runtimeInvisibleTypeAnnotationsAttribute);
    }


    default void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, method, runtimeInvisibleTypeAnnotationsAttribute);
    }


    default void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        visitAnyAttribute(clazz, annotationDefaultAttribute);
    }
}
