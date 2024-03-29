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
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;

/**
 * Delegate to another {@link KotlinMetadataVisitor} if the predicate returns true, or if there's no
 * predicate.
 *
 * <p>Note: only for KotlinClassKindMetadata i.e. does not visit synthetic classes.
 *
 * <p>For example, visit only abstract classes:
 *
 * <p>programClassPool.classesAccept( new ClazzToKotlinMetadataVisitor( new KotlinClassKindFilter(
 * clazz -> clazz.flags.isAbstract, new MyOtherKotlinMetadataVisitor())));
 */
public class KotlinClassKindFilter implements KotlinMetadataVisitor {
  private final Predicate<KotlinClassKindMetadata> predicate;
  private final KotlinMetadataVisitor kotlinMetadataVisitor;

  public KotlinClassKindFilter(KotlinMetadataVisitor kotlinMetadataVisitor) {
    this(__ -> true, kotlinMetadataVisitor);
  }

  public KotlinClassKindFilter(
      Predicate<KotlinClassKindMetadata> predicate, KotlinMetadataVisitor kotlinMetadataVisitor) {
    this.predicate = predicate;
    this.kotlinMetadataVisitor = kotlinMetadataVisitor;
  }

  @Override
  public void visitKotlinClassMetadata(
      Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata) {
    if (this.predicate.test(kotlinClassKindMetadata)) {
      this.kotlinMetadataVisitor.visitKotlinClassMetadata(clazz, kotlinClassKindMetadata);
    }
  }

  @Override
  public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}
}
