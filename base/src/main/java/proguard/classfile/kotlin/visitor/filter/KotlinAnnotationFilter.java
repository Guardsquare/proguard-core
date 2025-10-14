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

package proguard.classfile.kotlin.visitor.filter;

import java.util.function.Predicate;
import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.flags.KotlinPropertyAccessorMetadata;
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor;

/**
 * Delegates to a given {@link KotlinAnnotationVisitor} if the predicate succeeds.
 *
 * @author James Hamilton
 */
public class KotlinAnnotationFilter implements KotlinAnnotationVisitor {
  private final Predicate<KotlinAnnotation> predicate;
  private final KotlinAnnotationVisitor acceptedKotlinAnnotationVisitor;
  private final KotlinAnnotationVisitor rejectedKotlinAnnotationVisitor;

  public KotlinAnnotationFilter(
      Predicate<KotlinAnnotation> predicate,
      KotlinAnnotationVisitor acceptedKotlinAnnotationVisitor) {
    this(predicate, acceptedKotlinAnnotationVisitor, null);
  }

  public KotlinAnnotationFilter(
      Predicate<KotlinAnnotation> predicate,
      KotlinAnnotationVisitor acceptedKotlinAnnotationVisitor,
      KotlinAnnotationVisitor rejectedKotlinAnnotationVisitor) {
    this.predicate = predicate;
    this.acceptedKotlinAnnotationVisitor = acceptedKotlinAnnotationVisitor;
    this.rejectedKotlinAnnotationVisitor = rejectedKotlinAnnotationVisitor;
  }

  @Override
  public void visitAnyAnnotation(
      Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation) {}

  @Override
  public void visitClassAnnotation(
      Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata, KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinClassKindMetadata, delegate);
    }
  }

  @Override
  public void visitConstructorAnnotation(
      Clazz clazz,
      KotlinConstructorMetadata kotlinConstructorMetadata,
      KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinConstructorMetadata, delegate);
    }
  }

  @Override
  public void visitEnumEntryAnnotation(
      Clazz clazz, KotlinEnumEntryMetadata kotlinEnumEntryMetadata, KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinEnumEntryMetadata, delegate);
    }
  }

  @Override
  public void visitFunctionAnnotation(
      Clazz clazz, KotlinFunctionMetadata kotlinFunctionMetadata, KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinFunctionMetadata, delegate);
    }
  }

  @Override
  public void visitPropertyAnnotation(
      Clazz clazz, KotlinPropertyMetadata kotlinPropertyMetadata, KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinPropertyMetadata, delegate);
    }
  }

  @Override
  public void visitPropertyAccessorAnnotation(
      Clazz clazz,
      KotlinPropertyAccessorMetadata kotlinPropertyAccessorMetadata,
      KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinPropertyAccessorMetadata, delegate);
    }
  }

  @Override
  public void visitTypeAnnotation(
      Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata, KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinTypeMetadata, delegate);
    }
  }

  @Override
  public void visitTypeParameterAnnotation(
      Clazz clazz,
      KotlinTypeParameterMetadata kotlinTypeParameterMetadata,
      KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinTypeParameterMetadata, delegate);
    }
  }

  @Override
  public void visitTypeAliasAnnotation(
      Clazz clazz, KotlinTypeAliasMetadata kotlinTypeAliasMetadata, KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinTypeAliasMetadata, delegate);
    }
  }

  private KotlinAnnotationVisitor getDelegate(KotlinAnnotation kotlinMetadataAnnotation) {
    return this.predicate.test(kotlinMetadataAnnotation)
        ? this.acceptedKotlinAnnotationVisitor
        : this.rejectedKotlinAnnotationVisitor;
  }

  @Override
  public void visitValueParameterAnnotation(
      Clazz clazz,
      KotlinValueParameterMetadata kotlinValueParameterMetadata,
      KotlinAnnotation annotation) {
    KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
    if (delegate != null) {
      annotation.accept(clazz, kotlinValueParameterMetadata, delegate);
    }
  }
}
