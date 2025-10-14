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
package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.flags.KotlinPropertyAccessorMetadata;

public class AllKotlinAnnotationVisitor
    implements KotlinMetadataVisitor,
        KotlinClassVisitor,
        KotlinConstructorVisitor,
        KotlinEnumEntryVisitor,
        KotlinFunctionVisitor,
        KotlinTypeAliasVisitor,
        KotlinTypeParameterVisitor,
        KotlinTypeVisitor,
        KotlinPropertyVisitor,
        KotlinPropertyAccessorVisitor,
        KotlinValueParameterVisitor {
  private final KotlinAnnotationVisitor delegate;

  public AllKotlinAnnotationVisitor(KotlinAnnotationVisitor delegate) {
    this.delegate = delegate;
  }

  // Implementations for KotlinMetadataVisitor.

  @Override
  public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {
    kotlinMetadata.accept(clazz, new AllTypeVisitor(this));
    kotlinMetadata.accept(clazz, new AllTypeParameterVisitor(this));
  }

  // Implementations for KotlinClassVisitor.

  @Override
  public void visitKotlinClassMetadata(
      Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata) {
    kotlinClassKindMetadata.annotationsAccept(clazz, delegate);
    kotlinClassKindMetadata.constructorsAccept(clazz, this);
    kotlinClassKindMetadata.enumEntriesAccept(clazz, this);
  }

  @Override
  public void visitKotlinDeclarationContainerMetadata(
      Clazz clazz, KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata) {
    kotlinDeclarationContainerMetadata.functionsAccept(clazz, this);
    kotlinDeclarationContainerMetadata.propertiesAccept(clazz, this);
    kotlinDeclarationContainerMetadata.typeAliasesAccept(clazz, this);
    visitAnyKotlinMetadata(clazz, kotlinDeclarationContainerMetadata);
  }

  // Implementations for KotlinTypeVisitor.

  @Override
  public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata) {
    kotlinTypeMetadata.annotationsAccept(clazz, delegate);
  }

  // Implementations for KotlinTypeAliasVisitor.

  @Override
  public void visitTypeAlias(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinTypeAliasMetadata kotlinTypeAliasMetadata) {
    kotlinTypeAliasMetadata.annotationsAccept(clazz, delegate);
  }

  // Implementations for KotlinTypeParameterVisitor.

  @Override
  public void visitAnyTypeParameter(
      Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata) {
    kotlinTypeParameterMetadata.annotationsAccept(clazz, delegate);
  }

  // Implementations for KotlinConstructorVisitor.

  @Override
  public void visitConstructor(
      Clazz clazz,
      KotlinClassKindMetadata kotlinClassKindMetadata,
      KotlinConstructorMetadata kotlinConstructorMetadata) {
    kotlinConstructorMetadata.annotationsAccept(clazz, delegate);
    kotlinConstructorMetadata.valueParametersAccept(clazz, kotlinClassKindMetadata, this);
  }

  @Override
  public void visitAnyFunction(
      Clazz clazz, KotlinMetadata kotlinMetadata, KotlinFunctionMetadata kotlinFunctionMetadata) {
    kotlinFunctionMetadata.annotationsAccept(clazz, delegate);
    kotlinFunctionMetadata.valueParametersAccept(clazz, kotlinMetadata, this);
  }

  @Override
  public void visitAnyProperty(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinPropertyMetadata kotlinPropertyMetadata) {
    kotlinPropertyMetadata.annotationsAccept(clazz, delegate);
    kotlinPropertyMetadata.propertyAccessorsAccept(clazz, kotlinDeclarationContainerMetadata, this);
    kotlinPropertyMetadata.setterParameterAccept(clazz, kotlinDeclarationContainerMetadata, this);
  }

  @Override
  public void visitAnyValueParameter(
      Clazz clazz, KotlinValueParameterMetadata kotlinValueParameterMetadata) {
    kotlinValueParameterMetadata.annotationsAccept(clazz, delegate);
  }

  @Override
  public void visitConstructorValParameter(
      Clazz clazz,
      KotlinClassKindMetadata kotlinClassKindMetadata,
      KotlinConstructorMetadata kotlinConstructorMetadata,
      KotlinValueParameterMetadata kotlinValueParameterMetadata) {
    kotlinValueParameterMetadata.typeAccept(
        clazz, kotlinClassKindMetadata, kotlinConstructorMetadata, this);
    visitAnyValueParameter(clazz, kotlinValueParameterMetadata);
  }

  @Override
  public void visitFunctionValParameter(
      Clazz clazz,
      KotlinMetadata kotlinMetadata,
      KotlinFunctionMetadata kotlinFunctionMetadata,
      KotlinValueParameterMetadata kotlinValueParameterMetadata) {
    kotlinValueParameterMetadata.typeAccept(clazz, kotlinMetadata, kotlinFunctionMetadata, this);
    visitAnyValueParameter(clazz, kotlinValueParameterMetadata);
  }

  @Override
  public void visitPropertyValParameter(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinPropertyMetadata kotlinPropertyMetadata,
      KotlinValueParameterMetadata kotlinValueParameterMetadata) {
    kotlinValueParameterMetadata.typeAccept(
        clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata, this);
    visitAnyValueParameter(clazz, kotlinValueParameterMetadata);
  }

  @Override
  public void visitAnyEnumEntry(
      Clazz clazz,
      KotlinClassKindMetadata kotlinClassKindMetadata,
      KotlinEnumEntryMetadata kotlinEnumEntryMetadata) {
    kotlinEnumEntryMetadata.annotationsAccept(clazz, delegate);
  }

  @Override
  public void visitAnyPropertyAccessor(
      Clazz clazz,
      KotlinMetadata kotlinMetadata,
      KotlinPropertyMetadata kotlinPropertyMetadata,
      KotlinPropertyAccessorMetadata kotlinPropertyAccessorMetadata) {
    kotlinPropertyAccessorMetadata.annotationsAccept(clazz, delegate);
  }
}
