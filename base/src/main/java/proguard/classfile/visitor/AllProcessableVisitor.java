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
package proguard.classfile.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.visitor.*;
import proguard.classfile.attribute.preverification.*;
import proguard.classfile.attribute.preverification.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.*;
import proguard.util.Processable;
import proguard.util.ProcessableVisitor;

/**
 * The {@link AllProcessableVisitor} lets a given {@link ProcessableVisitor} visit all {@link
 * Processable}s and any nested processables.
 */
public class AllProcessableVisitor
    implements ClassVisitor,
        ConstantVisitor,
        MemberVisitor,
        AttributeVisitor,
        BootstrapMethodInfoVisitor,
        ExceptionInfoVisitor,
        RecordComponentInfoVisitor,
        InnerClassesInfoVisitor,
        StackMapFrameVisitor,
        VerificationTypeVisitor,
        ParameterInfoVisitor,
        LocalVariableInfoVisitor,
        LocalVariableTypeInfoVisitor,
        AnnotationVisitor,
        TypeAnnotationVisitor,
        ElementValueVisitor,
        ProcessableVisitor {

  private final ProcessableVisitor processableVisitor;
  private final KotlinProcessableVisitor kotlinProcessableVisitor;

  public AllProcessableVisitor(ProcessableVisitor processableVisitor) {
    this.processableVisitor = processableVisitor;
    this.kotlinProcessableVisitor = new KotlinProcessableVisitor(processableVisitor);
  }

  // Implementation for ProcessableVisitor.

  @Override
  public void visitAnyProcessable(Processable processable) {
    processable.accept(processableVisitor);
  }

  // Implementations for ClassVisitor.

  @Override
  public void visitAnyClass(Clazz clazz) {
    throw new UnsupportedOperationException(
        this.getClass().getName() + " does not support " + clazz.getClass().getName());
  }

  @Override
  public void visitProgramClass(ProgramClass programClass) {
    programClass.accept(this);

    programClass.constantPoolEntriesAccept(this);

    programClass.fieldsAccept(this);
    programClass.methodsAccept(this);

    programClass.attributesAccept(this);

    programClass.kotlinMetadataAccept(kotlinProcessableVisitor);
  }

  @Override
  public void visitLibraryClass(LibraryClass libraryClass) {
    libraryClass.accept(this);
    ;

    libraryClass.fieldsAccept(this);
    libraryClass.methodsAccept(this);

    libraryClass.kotlinMetadataAccept(kotlinProcessableVisitor);
  }

  // Implementations for ConstantVisitor.

  public void visitAnyConstant(Clazz clazz, Constant constant) {
    constant.accept(processableVisitor);
  }

  // Implementations for MemberVisitor.

  public void visitProgramMember(ProgramClass programClass, ProgramMember programMember) {
    programMember.accept(this);

    programMember.attributesAccept(programClass, this);
  }

  public void visitLibraryMember(LibraryClass libraryClass, LibraryMember libraryMember) {
    libraryMember.accept(this);
  }

  // Implementations for AttributeVisitor.

  public void visitAnyAttribute(Clazz clazz, Attribute attribute) {
    attribute.accept(this);
  }

  public void visitBootstrapMethodsAttribute(
      Clazz clazz, BootstrapMethodsAttribute bootstrapMethodsAttribute) {
    bootstrapMethodsAttribute.accept(this);

    bootstrapMethodsAttribute.bootstrapMethodEntriesAccept(clazz, this);
  }

  public void visitRecordAttribute(Clazz clazz, RecordAttribute recordAttribute) {
    recordAttribute.accept(this);

    recordAttribute.componentsAccept(clazz, this);
  }

  public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute) {
    innerClassesAttribute.accept(this);

    innerClassesAttribute.innerClassEntriesAccept(clazz, this);
  }

  public void visitMethodParametersAttribute(
      Clazz clazz, Method method, MethodParametersAttribute methodParametersAttribute) {
    methodParametersAttribute.accept(this);

    methodParametersAttribute.parametersAccept(clazz, method, this);
  }

  public void visitExceptionsAttribute(
      Clazz clazz, Method method, ExceptionsAttribute exceptionsAttribute) {
    exceptionsAttribute.accept(this);

    exceptionsAttribute.exceptionEntriesAccept(clazz, this);
  }

  public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
    codeAttribute.accept(this);

    codeAttribute.exceptionsAccept(clazz, method, this);
    codeAttribute.attributesAccept(clazz, method, this);
  }

  public void visitStackMapAttribute(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      StackMapAttribute stackMapAttribute) {
    stackMapAttribute.accept(this);

    stackMapAttribute.stackMapFramesAccept(clazz, method, codeAttribute, this);
  }

  public void visitStackMapTableAttribute(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      StackMapTableAttribute stackMapTableAttribute) {
    stackMapTableAttribute.accept(this);

    stackMapTableAttribute.stackMapFramesAccept(clazz, method, codeAttribute, this);
  }

  public void visitLocalVariableTableAttribute(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      LocalVariableTableAttribute localVariableTableAttribute) {
    localVariableTableAttribute.accept(this);

    localVariableTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
  }

  public void visitLocalVariableTypeTableAttribute(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      LocalVariableTypeTableAttribute localVariableTypeTableAttribute) {
    localVariableTypeTableAttribute.accept(this);

    localVariableTypeTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
  }

  public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute) {
    annotationsAttribute.accept(this);

    annotationsAttribute.annotationsAccept(clazz, this);
  }

  public void visitAnyParameterAnnotationsAttribute(
      Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute) {
    parameterAnnotationsAttribute.accept(this);

    parameterAnnotationsAttribute.annotationsAccept(clazz, method, this);
  }

  public void visitAnyTypeAnnotationsAttribute(
      Clazz clazz, TypeAnnotationsAttribute typeAnnotationsAttribute) {
    typeAnnotationsAttribute.accept(this);

    typeAnnotationsAttribute.typeAnnotationsAccept(clazz, this);
  }

  public void visitAnnotationDefaultAttribute(
      Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute) {
    annotationDefaultAttribute.accept(this);

    annotationDefaultAttribute.defaultValueAccept(clazz, this);
  }

  // Implementations for BootstrapMethodInfoVisitor.

  public void visitBootstrapMethodInfo(Clazz clazz, BootstrapMethodInfo bootstrapMethodInfo) {
    bootstrapMethodInfo.accept(this);
  }

  // Implementations for RecordComponentInfoVisitor.

  public void visitRecordComponentInfo(Clazz clazz, RecordComponentInfo recordComponentInfo) {
    recordComponentInfo.accept(this);

    recordComponentInfo.attributesAccept(clazz, this);
  }

  // Implementations for InnerClassesInfoVisitor.

  public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo) {
    innerClassesInfo.accept(this);
  }

  // Implementations for ExceptionInfoVisitor.

  public void visitExceptionInfo(
      Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo) {
    exceptionInfo.accept(this);
  }

  // Implementations for StackMapFrameVisitor.

  public void visitSameZeroFrame(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      SameZeroFrame sameZeroFrame) {
    sameZeroFrame.accept(this);
  }

  public void visitSameOneFrame(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      SameOneFrame sameOneFrame) {
    sameOneFrame.accept(this);

    sameOneFrame.stackItemAccept(clazz, method, codeAttribute, offset, this);
  }

  public void visitLessZeroFrame(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      LessZeroFrame lessZeroFrame) {
    lessZeroFrame.accept(this);
  }

  public void visitMoreZeroFrame(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      MoreZeroFrame moreZeroFrame) {
    moreZeroFrame.accept(this);

    moreZeroFrame.additionalVariablesAccept(clazz, method, codeAttribute, offset, this);
  }

  public void visitFullFrame(
      Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, FullFrame fullFrame) {
    fullFrame.accept(this);

    fullFrame.variablesAccept(clazz, method, codeAttribute, offset, this);
    fullFrame.stackAccept(clazz, method, codeAttribute, offset, this);
  }

  // Implementations for VerificationTypeVisitor.

  public void visitAnyVerificationType(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      VerificationType verificationType) {
    verificationType.accept(this);
  }

  // Implementations for ParameterInfoVisitor.

  public void visitParameterInfo(
      Clazz clazz, Method method, int parameterIndex, ParameterInfo parameterInfo) {
    parameterInfo.accept(this);
  }

  // Implementations for LocalVariableInfoVisitor.

  public void visitLocalVariableInfo(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      LocalVariableInfo localVariableInfo) {
    localVariableInfo.accept(this);
  }

  // Implementations for LocalVariableTypeInfoVisitor.

  public void visitLocalVariableTypeInfo(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      LocalVariableTypeInfo localVariableTypeInfo) {
    localVariableTypeInfo.accept(this);
  }

  // Implementations for AnnotationVisitor.

  public void visitAnnotation(Clazz clazz, Annotation annotation) {
    annotation.accept(this);

    annotation.elementValuesAccept(clazz, this);
  }

  // Implementations for TypeAnnotationVisitor.

  public void visitTypeAnnotation(Clazz clazz, TypeAnnotation typeAnnotation) {
    typeAnnotation.accept(this);

    // typeAnnotation.targetInfoAccept(clazz, this);
    // typeAnnotation.typePathInfosAccept(clazz, this);
    typeAnnotation.elementValuesAccept(clazz, this);
  }

  // Implementations for ElementValueVisitor.

  public void visitAnyElementValue(Clazz clazz, Annotation annotation, ElementValue elementValue) {
    elementValue.accept(this);
  }

  public void visitAnnotationElementValue(
      Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue) {
    annotationElementValue.accept(this);

    annotationElementValue.annotationAccept(clazz, this);
  }

  public void visitArrayElementValue(
      Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue) {
    arrayElementValue.accept(this);
  }

  private static class KotlinProcessableVisitor
      implements ProcessableVisitor,
          KotlinMetadataVisitor,
          KotlinPropertyVisitor,
          KotlinFunctionVisitor,
          KotlinConstructorVisitor,
          KotlinTypeVisitor,
          KotlinTypeAliasVisitor,
          KotlinValueParameterVisitor,
          KotlinTypeParameterVisitor {

    private final ProcessableVisitor visitor;

    KotlinProcessableVisitor(ProcessableVisitor visitor) {
      this.visitor = visitor;
    }

    // Implementations for ProcessableVisitor.

    /**
     * Visits any {@link Processable} instance. The more specific default implementations of this
     * interface delegate to this method.
     */
    public void visitAnyProcessable(Processable processable) {
      processable.accept(visitor);
    }

    // Implementations for KotlinMetadataVisitor.

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {
      kotlinMetadata.accept(this);
    }

    @Override
    public void visitKotlinDeclarationContainerMetadata(
        Clazz clazz, KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata) {
      visitAnyKotlinMetadata(clazz, kotlinDeclarationContainerMetadata);

      kotlinDeclarationContainerMetadata.propertiesAccept(clazz, this);
      kotlinDeclarationContainerMetadata.delegatedPropertiesAccept(clazz, this);
      kotlinDeclarationContainerMetadata.functionsAccept(clazz, this);
      kotlinDeclarationContainerMetadata.typeAliasesAccept(clazz, this);
    }

    @Override
    public void visitKotlinClassMetadata(
        Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata) {
      visitKotlinDeclarationContainerMetadata(clazz, kotlinClassKindMetadata);

      kotlinClassKindMetadata.typeParametersAccept(clazz, this);
      kotlinClassKindMetadata.contextReceiverTypesAccept(clazz, this);
      kotlinClassKindMetadata.superTypesAccept(clazz, this);
      kotlinClassKindMetadata.constructorsAccept(clazz, this);
      kotlinClassKindMetadata.inlineClassUnderlyingPropertyTypeAccept(clazz, this);
    }

    @Override
    public void visitKotlinSyntheticClassMetadata(
        Clazz clazz, KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata) {
      visitAnyKotlinMetadata(clazz, kotlinSyntheticClassKindMetadata);

      kotlinSyntheticClassKindMetadata.functionsAccept(clazz, this);
    }

    // Implementations for KotlinPropertyVisitor.
    @Override
    public void visitAnyProperty(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata) {
      kotlinPropertyMetadata.accept(this);

      kotlinPropertyMetadata.typeParametersAccept(clazz, kotlinDeclarationContainerMetadata, this);
      kotlinPropertyMetadata.receiverTypeAccept(clazz, kotlinDeclarationContainerMetadata, this);
      kotlinPropertyMetadata.contextReceiverTypesAccept(
          clazz, kotlinDeclarationContainerMetadata, this);
      kotlinPropertyMetadata.typeAccept(clazz, kotlinDeclarationContainerMetadata, this);
      kotlinPropertyMetadata.setterParameterAccept(clazz, kotlinDeclarationContainerMetadata, this);
    }

    // Implementations for KotlinFunctionVisitor.
    @Override
    public void visitAnyFunction(
        Clazz clazz, KotlinMetadata kotlinMetadata, KotlinFunctionMetadata kotlinFunctionMetadata) {
      kotlinFunctionMetadata.accept(this);

      kotlinFunctionMetadata.typeParametersAccept(clazz, kotlinMetadata, this);
      kotlinFunctionMetadata.receiverTypeAccept(clazz, kotlinMetadata, this);
      kotlinFunctionMetadata.contextReceiverTypesAccept(clazz, kotlinMetadata, this);
      kotlinFunctionMetadata.valueParametersAccept(clazz, kotlinMetadata, this);
      kotlinFunctionMetadata.returnTypeAccept(clazz, kotlinMetadata, this);
      kotlinFunctionMetadata.contractsAccept(clazz, kotlinMetadata, new AllTypeVisitor(this));
    }

    // Implementations for KotlinConstructorVisitor.
    @Override
    public void visitConstructor(
        Clazz clazz,
        KotlinClassKindMetadata kotlinClassKindMetadata,
        KotlinConstructorMetadata kotlinConstructorMetadata) {
      kotlinConstructorMetadata.accept(this);

      kotlinConstructorMetadata.valueParametersAccept(clazz, kotlinClassKindMetadata, this);
    }

    // Implementations for KotlinTypeVisitor.
    @Override
    public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata) {
      kotlinTypeMetadata.accept(this);

      kotlinTypeMetadata.typeArgumentsAccept(clazz, this);
      kotlinTypeMetadata.outerClassAccept(clazz, this);
      kotlinTypeMetadata.upperBoundsAccept(clazz, this);
      kotlinTypeMetadata.abbreviationAccept(clazz, this);
    }

    // Implementations for KotlinTypeAliasVisitor.
    @Override
    public void visitTypeAlias(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinTypeAliasMetadata kotlinTypeAliasMetadata) {
      kotlinTypeAliasMetadata.accept(this);

      kotlinTypeAliasMetadata.underlyingTypeAccept(clazz, kotlinDeclarationContainerMetadata, this);
      kotlinTypeAliasMetadata.expandedTypeAccept(clazz, kotlinDeclarationContainerMetadata, this);
      kotlinTypeAliasMetadata.typeParametersAccept(clazz, kotlinDeclarationContainerMetadata, this);
    }

    // Implementations for KotlinValueParameterVisitor
    @Override
    public void visitAnyValueParameter(
        Clazz clazz, KotlinValueParameterMetadata kotlinValueParameterMetadata) {
      kotlinValueParameterMetadata.accept(this);
    }

    @Override
    public void visitFunctionValParameter(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata) {
      visitAnyValueParameter(clazz, kotlinValueParameterMetadata);

      kotlinValueParameterMetadata.typeAccept(clazz, kotlinMetadata, kotlinFunctionMetadata, this);
    }

    @Override
    public void visitConstructorValParameter(
        Clazz clazz,
        KotlinClassKindMetadata kotlinClassKindMetadata,
        KotlinConstructorMetadata kotlinConstructorMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata) {
      visitAnyValueParameter(clazz, kotlinValueParameterMetadata);

      kotlinValueParameterMetadata.typeAccept(
          clazz, kotlinClassKindMetadata, kotlinConstructorMetadata, this);
    }

    @Override
    public void visitPropertyValParameter(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata) {
      visitAnyValueParameter(clazz, kotlinValueParameterMetadata);

      kotlinValueParameterMetadata.typeAccept(
          clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata, this);
    }

    // Implementations for KotlinTypeParameterVisitor
    @Override
    public void visitAnyTypeParameter(
        Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata) {
      kotlinTypeParameterMetadata.accept(this);

      kotlinTypeParameterMetadata.upperBoundsAccept(clazz, this);
    }
  }
}
