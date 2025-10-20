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

public interface KotlinAnnotationVisitor {
  void visitAnyAnnotation(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation);

  default void visitTypeAnnotation(
      Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata, KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinTypeMetadata, annotation);
  }

  default void visitTypeParameterAnnotation(
      Clazz clazz,
      KotlinTypeParameterMetadata kotlinTypeParameterMetadata,
      KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinTypeParameterMetadata, annotation);
  }

  default void visitTypeAliasAnnotation(
      Clazz clazz, KotlinTypeAliasMetadata kotlinTypeAliasMetadata, KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinTypeAliasMetadata, annotation);
  }

  default void visitClassAnnotation(
      Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata, KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinClassKindMetadata, annotation);
  }

  default void visitConstructorAnnotation(
      Clazz clazz,
      KotlinConstructorMetadata kotlinConstructorMetadata,
      KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinConstructorMetadata, annotation);
  }

  default void visitFunctionAnnotation(
      Clazz clazz, KotlinFunctionMetadata kotlinFunctionMetadata, KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinFunctionMetadata, annotation);
  }

  default void visitPropertyAccessorAnnotation(
      Clazz clazz,
      KotlinPropertyAccessorMetadata kotlinPropertyAccessorMetadata,
      KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinPropertyAccessorMetadata, annotation);
  }

  default void visitPropertyAnnotation(
      Clazz clazz, KotlinPropertyMetadata kotlinPropertyMetadata, KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinPropertyMetadata, annotation);
  }

  default void visitValueParameterAnnotation(
      Clazz clazz,
      KotlinValueParameterMetadata kotlinValueParameterMetadata,
      KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinValueParameterMetadata, annotation);
  }

  default void visitEnumEntryAnnotation(
      Clazz clazz, KotlinEnumEntryMetadata kotlinEnumEntryMetadata, KotlinAnnotation annotation) {
    visitAnyAnnotation(clazz, kotlinEnumEntryMetadata, annotation);
  }
}
