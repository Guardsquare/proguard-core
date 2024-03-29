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
package proguard.classfile.kotlin;

import java.util.List;
import proguard.classfile.Clazz;
import proguard.classfile.kotlin.visitor.*;
import proguard.util.*;

public class KotlinEffectMetadata extends SimpleProcessable implements Processable {
  public KotlinEffectType effectType;

  public KotlinEffectInvocationKind invocationKind;

  public KotlinEffectExpressionMetadata conclusionOfConditionalEffect;

  public List<KotlinEffectExpressionMetadata> constructorArguments;

  public KotlinEffectMetadata(
      KotlinEffectType effectType, KotlinEffectInvocationKind invocationKind) {
    this.effectType = effectType;
    this.invocationKind = invocationKind;
  }

  public void accept(
      Clazz clazz,
      KotlinMetadata kotlinMetadata,
      KotlinFunctionMetadata kotlinFunctionMetadata,
      KotlinContractMetadata kotlinContractMetadata,
      KotlinEffectVisitor kotlinEffectVisitor) {
    kotlinEffectVisitor.visitEffect(
        clazz, kotlinMetadata, kotlinFunctionMetadata, kotlinContractMetadata, this);
  }

  public void constructorArgumentAccept(
      Clazz clazz, KotlinEffectExprVisitor kotlinEffectExprVisitor) {
    for (KotlinEffectExpressionMetadata constructorArgument : constructorArguments) {
      kotlinEffectExprVisitor.visitConstructorArgExpression(clazz, this, constructorArgument);
    }
  }

  public void conclusionOfConditionalEffectAccept(
      Clazz clazz, KotlinEffectExprVisitor kotlinEffectExprVisitor) {
    if (conclusionOfConditionalEffect != null) {
      kotlinEffectExprVisitor.visitConclusionExpression(clazz, this, conclusionOfConditionalEffect);
    }
  }

  // Implementations for Object.
  @Override
  public String toString() {
    return "Kotlin contract effect";
  }
}
