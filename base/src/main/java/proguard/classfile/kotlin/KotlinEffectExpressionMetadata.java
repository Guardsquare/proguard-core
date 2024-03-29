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
package proguard.classfile.kotlin;

import java.util.List;
import proguard.classfile.Clazz;
import proguard.classfile.kotlin.flags.KotlinEffectExpressionFlags;
import proguard.classfile.kotlin.visitor.*;
import proguard.util.*;

public class KotlinEffectExpressionMetadata extends SimpleProcessable implements Processable {
  public int parameterIndex = -1;

  public boolean hasConstantValue = false;
  public Object constantValue; // May be intentionally null;

  public KotlinTypeMetadata typeOfIs;

  public List<KotlinEffectExpressionMetadata> andRightHandSides;

  public List<KotlinEffectExpressionMetadata> orRightHandSides;

  public KotlinEffectExpressionFlags flags;

  public void andRightHandSideAccept(
      Clazz clazz,
      KotlinEffectMetadata kotlinEffectMetadata,
      KotlinEffectExprVisitor kotlinEffectExprVisitor) {
    for (KotlinEffectExpressionMetadata rhs : andRightHandSides) {
      kotlinEffectExprVisitor.visitAndRHSExpression(clazz, kotlinEffectMetadata, this, rhs);
    }
  }

  public void orRightHandSideAccept(
      Clazz clazz,
      KotlinEffectMetadata kotlinEffectMetadata,
      KotlinEffectExprVisitor kotlinEffectExprVisitor) {
    for (KotlinEffectExpressionMetadata rhs : andRightHandSides) {
      kotlinEffectExprVisitor.visitOrRHSExpression(clazz, kotlinEffectMetadata, this, rhs);
    }
  }

  public void typeOfIsAccept(Clazz clazz, KotlinTypeVisitor kotlinTypeVisitor) {
    if (typeOfIs != null) {
      kotlinTypeVisitor.visitTypeOfIsExpression(clazz, this, typeOfIs);
    }
  }

  // Implementations for Object.
  @Override
  public String toString() {
    return "Kotlin contract effect";
  }
}
