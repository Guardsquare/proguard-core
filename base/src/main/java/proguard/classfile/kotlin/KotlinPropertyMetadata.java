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
import proguard.classfile.*;
import proguard.classfile.kotlin.flags.*;
import proguard.classfile.kotlin.visitor.*;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;
import proguard.util.*;

public class KotlinPropertyMetadata extends SimpleProcessable {
  public String name;

  public List<KotlinTypeParameterMetadata> typeParameters;

  public KotlinTypeMetadata receiverType;

  public List<KotlinTypeMetadata> contextReceivers;

  /**
   * @deprecated Use {@link KotlinPropertyMetadata#setterParameter } instead. There can only be one
   *     setter parameter but this old API used a list.
   */
  @Deprecated public List<KotlinValueParameterMetadata> setterParameters;

  public KotlinValueParameterMetadata setterParameter;

  public KotlinTypeMetadata type;

  public KotlinVersionRequirementMetadata versionRequirement;

  public KotlinPropertyFlags flags;

  public KotlinPropertyAccessorFlags getterFlags;

  public KotlinPropertyAccessorFlags setterFlags;

  // Extensions.
  public FieldSignature backingFieldSignature;
  // Store the class where the referencedBackingField is declared.
  public Clazz referencedBackingFieldClass;
  public Field referencedBackingField;
  public MethodSignature getterSignature;
  public Method referencedGetterMethod;
  public MethodSignature setterSignature;
  public Method referencedSetterMethod;

  public MethodSignature syntheticMethodForAnnotations;

  public Clazz referencedSyntheticMethodClass;
  public Method referencedSyntheticMethodForAnnotations;

  public MethodSignature syntheticMethodForDelegate;

  public Clazz referencedSyntheticMethodForDelegateClass;
  public Method referencedSyntheticMethodForDelegateMethod;

  public KotlinPropertyMetadata(
      KotlinPropertyFlags flags,
      String name,
      KotlinPropertyAccessorFlags getterFlags,
      KotlinPropertyAccessorFlags setterFlags) {
    this.name = name;
    this.flags = flags;
    this.getterFlags = getterFlags;
    this.setterFlags = setterFlags;
  }

  public void accept(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinPropertyVisitor kotlinPropertyVisitor) {
    kotlinPropertyVisitor.visitProperty(clazz, kotlinDeclarationContainerMetadata, this);
  }

  void acceptAsDelegated(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinPropertyVisitor kotlinPropertyVisitor) {
    kotlinPropertyVisitor.visitDelegatedProperty(clazz, kotlinDeclarationContainerMetadata, this);
  }

  public void typeAccept(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinTypeVisitor kotlinTypeVisitor) {
    if (type == null) {
      throw new ProguardCoreException.Builder(
              "Property type is null in class %s.", ErrorId.CLASSFILE_NULL_VALUES)
          .errorParameters(clazz.getName())
          .build();
    }
    kotlinTypeVisitor.visitPropertyType(clazz, kotlinDeclarationContainerMetadata, this, type);
  }

  public void receiverTypeAccept(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinTypeVisitor kotlinTypeVisitor) {
    if (receiverType != null) {
      kotlinTypeVisitor.visitPropertyReceiverType(
          clazz, kotlinDeclarationContainerMetadata, this, receiverType);
    }
  }

  public void contextReceiverTypesAccept(
      Clazz clazz, KotlinMetadata kotlinMetadata, KotlinTypeVisitor kotlinTypeVisitor) {
    if (contextReceivers != null) {
      for (KotlinTypeMetadata contextReceiver : contextReceivers) {
        kotlinTypeVisitor.visitPropertyContextReceiverType(
            clazz, kotlinMetadata, this, contextReceiver);
      }
    }
  }

  /**
   * @deprecated Use {@link #setterParameterAccept(Clazz, KotlinDeclarationContainerMetadata,
   *     KotlinValueParameterVisitor)} instead.
   */
  @Deprecated
  public void setterParametersAccept(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinValueParameterVisitor kotlinValueParameterVisitor) {
    setterParameterAccept(clazz, kotlinDeclarationContainerMetadata, kotlinValueParameterVisitor);
  }

  public void setterParameterAccept(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinValueParameterVisitor kotlinValueParameterVisitor) {
    if (setterParameter != null) {
      setterParameter.accept(
          clazz, kotlinDeclarationContainerMetadata, this, kotlinValueParameterVisitor);
    }
  }

  public void typeParametersAccept(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinTypeParameterVisitor kotlinTypeParameterVisitor) {
    if (typeParameters == null) {
      throw new ProguardCoreException.Builder(
              "Type parameters are null in class %s.", ErrorId.CLASSFILE_NULL_VALUES)
          .errorParameters(clazz.getName())
          .build();
    }
    for (KotlinTypeParameterMetadata typeParameter : typeParameters) {
      typeParameter.accept(
          clazz, kotlinDeclarationContainerMetadata, this, kotlinTypeParameterVisitor);
    }
  }

  public void versionRequirementAccept(
      Clazz clazz,
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
      KotlinVersionRequirementVisitor kotlinVersionRequirementVisitor) {
    if (versionRequirement != null) {
      versionRequirement.accept(
          clazz, kotlinDeclarationContainerMetadata, this, kotlinVersionRequirementVisitor);
    }
  }

  // Implementations for Object.
  @Override
  public String toString() {
    return "Kotlin "
        + (flags.isDelegated ? "delegated " : "")
        + "property ("
        + name
        + " | "
        + (backingFieldSignature != null ? "b" : "")
        + "g"
        + (getterFlags.isDefault ? "" : "+")
        + (flags.isVar ? "s" + (setterFlags.isDefault ? "" : "+") : "")
        + ")";
  }
}
