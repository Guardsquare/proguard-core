/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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
package proguard.classfile.kotlin.flags;

import java.util.List;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.kotlin.KotlinAnnotatable;
import proguard.classfile.kotlin.KotlinAnnotation;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinPropertyMetadata;
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor;
import proguard.classfile.kotlin.visitor.KotlinPropertyAccessorVisitor;
import proguard.util.SimpleProcessable;

/** Kotlin property accessor metadata (getters/setters for properties). */
public class KotlinPropertyAccessorMetadata extends SimpleProcessable
    implements KotlinFlags, KotlinAnnotatable {

  public KotlinVisibilityFlags visibility;
  public KotlinModalityFlags modality;

  public Method referencedMethod;
  public MethodSignature signature;

  public List<KotlinAnnotation> annotations;

  /**
   * Signifies that the corresponding property is not default, i.e. it has a body and/or annotations
   * in the source code.
   */
  public boolean isDefault;

  /** Signifies that the corresponding property is `external`. */
  public boolean isExternal;

  /** Signifies that the corresponding property is `inline`. */
  public boolean isInline;

  @Deprecated public boolean hasAnnotations;

  public KotlinPropertyAccessorMetadata(
      KotlinVisibilityFlags visibility, KotlinModalityFlags modality) {
    this.visibility = visibility;
    this.modality = modality;
  }

  public void accept(
      Clazz clazz,
      KotlinMetadata kotlinMetadata,
      KotlinPropertyMetadata kotlinPropertyMetadata,
      KotlinPropertyAccessorVisitor kotlinPropertyAccessorVisitor) {
    kotlinPropertyAccessorVisitor.visitAnyPropertyAccessor(
        clazz, kotlinMetadata, kotlinPropertyMetadata, this);
  }

  @Override
  public void annotationsAccept(Clazz clazz, KotlinAnnotationVisitor kotlinAnnotationVisitor) {
    for (KotlinAnnotation annotation : annotations) {
      kotlinAnnotationVisitor.visitPropertyAccessorAnnotation(clazz, this, annotation);
    }
  }
}
