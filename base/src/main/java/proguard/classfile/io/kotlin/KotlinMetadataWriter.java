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
package proguard.classfile.io.kotlin;

import static java.util.stream.Collectors.joining;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_CLASS;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_FILE_FACADE;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_MULTI_FILE_CLASS_FACADE;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_MULTI_FILE_CLASS_PART;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_SYNTHETIC_CLASS;
import static proguard.classfile.kotlin.KotlinConstants.TYPE_KOTLIN_METADATA;

import java.util.Arrays;
import java.util.function.BiConsumer;
import kotlin.Metadata;
import kotlin.metadata.Attributes;
import kotlin.metadata.ClassKind;
import kotlin.metadata.KmClass;
import kotlin.metadata.KmClassifier;
import kotlin.metadata.KmConstantValue;
import kotlin.metadata.KmConstructor;
import kotlin.metadata.KmContract;
import kotlin.metadata.KmDeclarationContainer;
import kotlin.metadata.KmEffect;
import kotlin.metadata.KmEffectExpression;
import kotlin.metadata.KmEffectInvocationKind;
import kotlin.metadata.KmEffectType;
import kotlin.metadata.KmFlexibleTypeUpperBound;
import kotlin.metadata.KmFunction;
import kotlin.metadata.KmLambda;
import kotlin.metadata.KmPackage;
import kotlin.metadata.KmProperty;
import kotlin.metadata.KmPropertyAccessorAttributes;
import kotlin.metadata.KmType;
import kotlin.metadata.KmTypeAlias;
import kotlin.metadata.KmTypeParameter;
import kotlin.metadata.KmTypeProjection;
import kotlin.metadata.KmValueParameter;
import kotlin.metadata.KmVariance;
import kotlin.metadata.KmVersion;
import kotlin.metadata.KmVersionRequirement;
import kotlin.metadata.KmVersionRequirementLevel;
import kotlin.metadata.KmVersionRequirementVersionKind;
import kotlin.metadata.MemberKind;
import kotlin.metadata.Modality;
import kotlin.metadata.Visibility;
import kotlin.metadata.jvm.JvmAttributes;
import kotlin.metadata.jvm.JvmExtensionsKt;
import kotlin.metadata.jvm.JvmFieldSignature;
import kotlin.metadata.jvm.JvmMetadataUtil;
import kotlin.metadata.jvm.JvmMetadataVersion;
import kotlin.metadata.jvm.JvmMethodSignature;
import kotlin.metadata.jvm.KotlinClassMetadata;
import proguard.classfile.Clazz;
import proguard.classfile.FieldSignature;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramClass;
import proguard.classfile.TypeConstants;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.annotation.Annotation;
import proguard.classfile.attribute.annotation.ArrayElementValue;
import proguard.classfile.attribute.annotation.ConstantElementValue;
import proguard.classfile.attribute.annotation.ElementValue;
import proguard.classfile.attribute.annotation.visitor.AllAnnotationVisitor;
import proguard.classfile.attribute.annotation.visitor.AllElementValueVisitor;
import proguard.classfile.attribute.annotation.visitor.AnnotationTypeFilter;
import proguard.classfile.attribute.annotation.visitor.ElementValueVisitor;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.editor.ConstantPoolEditor;
import proguard.classfile.editor.ConstantPoolShrinker;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinConstants;
import proguard.classfile.kotlin.KotlinConstructorMetadata;
import proguard.classfile.kotlin.KotlinContractMetadata;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinEffectExpressionMetadata;
import proguard.classfile.kotlin.KotlinEffectMetadata;
import proguard.classfile.kotlin.KotlinFileFacadeKindMetadata;
import proguard.classfile.kotlin.KotlinFunctionMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinMetadataVersion;
import proguard.classfile.kotlin.KotlinMultiFileFacadeKindMetadata;
import proguard.classfile.kotlin.KotlinMultiFilePartKindMetadata;
import proguard.classfile.kotlin.KotlinPropertyMetadata;
import proguard.classfile.kotlin.KotlinSyntheticClassKindMetadata;
import proguard.classfile.kotlin.KotlinTypeAliasMetadata;
import proguard.classfile.kotlin.KotlinTypeMetadata;
import proguard.classfile.kotlin.KotlinTypeParameterMetadata;
import proguard.classfile.kotlin.KotlinTypeVariance;
import proguard.classfile.kotlin.KotlinValueParameterMetadata;
import proguard.classfile.kotlin.KotlinVersionRequirementMetadata;
import proguard.classfile.kotlin.flags.KotlinClassFlags;
import proguard.classfile.kotlin.flags.KotlinConstructorFlags;
import proguard.classfile.kotlin.flags.KotlinFunctionFlags;
import proguard.classfile.kotlin.flags.KotlinModalityFlags;
import proguard.classfile.kotlin.flags.KotlinPropertyFlags;
import proguard.classfile.kotlin.flags.KotlinValueParameterFlags;
import proguard.classfile.kotlin.flags.KotlinVisibilityFlags;
import proguard.classfile.kotlin.visitor.KotlinConstructorVisitor;
import proguard.classfile.kotlin.visitor.KotlinContractVisitor;
import proguard.classfile.kotlin.visitor.KotlinEffectExprVisitor;
import proguard.classfile.kotlin.visitor.KotlinEffectVisitor;
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeAliasVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor;
import proguard.classfile.kotlin.visitor.KotlinValueParameterVisitor;
import proguard.classfile.kotlin.visitor.KotlinVersionRequirementVisitor;
import proguard.classfile.util.WarningPrinter;
import proguard.classfile.util.kotlin.AnnotationConstructor;
import proguard.classfile.util.kotlin.KotlinMetadataType;
import proguard.classfile.visitor.ClassVisitor;

/**
 * This class visitor writes the information stored in a Clazz's kotlinMetadata field to
 * a @kotlin/Metadata annotation on the class.
 */
public class KotlinMetadataWriter
    implements ClassVisitor, KotlinMetadataVisitor, ElementValueVisitor {
  private final ClassVisitor extraClassVisitor;

  private int metadataKind;
  private int[] metadataVersion;
  private String[] data1;
  private String[] data2;
  private int extraInt;
  private String extraString;
  private String packageName;

  private ConstantPoolEditor constantPoolEditor;
  private final ConstantPoolShrinker constantPoolShrinker = new ConstantPoolShrinker();

  private KotlinMetadataType currentType;

  private final BiConsumer<Clazz, String> errorHandler;

  public static final KotlinMetadataVersion HIGHEST_ALLOWED_TO_WRITE =
      new KotlinMetadataVersion(JvmMetadataVersion.HIGHEST_ALLOWED_TO_WRITE.toIntArray());

  public static final KotlinMetadataVersion LATEST_STABLE_SUPPORTED =
      new KotlinMetadataVersion(JvmMetadataVersion.LATEST_STABLE_SUPPORTED.toIntArray());

  private KotlinMetadataVersion version;

  /**
   * @deprecated Use {@link KotlinMetadataWriter#KotlinMetadataWriter(BiConsumer)} instead.
   */
  @Deprecated
  public KotlinMetadataWriter(WarningPrinter warningPrinter) {
    this(warningPrinter, null);
  }

  /**
   * @deprecated Use {@link KotlinMetadataWriter#KotlinMetadataWriter(BiConsumer, ClassVisitor)}
   *     instead.
   */
  @Deprecated
  public KotlinMetadataWriter(WarningPrinter warningPrinter, ClassVisitor extraClassVisitor) {
    this((clazz, message) -> warningPrinter.print(clazz.getName(), message), extraClassVisitor);
  }

  public KotlinMetadataWriter(BiConsumer<Clazz, String> errorHandler) {
    this(errorHandler, null);
  }

  public KotlinMetadataWriter(
      BiConsumer<Clazz, String> errorHandler, ClassVisitor extraClassVisitor) {
    this.errorHandler = errorHandler;
    this.extraClassVisitor = extraClassVisitor;
  }

  // Implementations for ClassVisitor.

  @Override
  public void visitAnyClass(Clazz clazz) {
    clazz.kotlinMetadataAccept(this);
  }

  // Implementations for KotlinMetadataVisitor.

  @Override
  public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {
    // Set the metadata version we want to write.
    KotlinMetadataVersion originalVersion = new KotlinMetadataVersion(kotlinMetadata.mv);

    version = originalVersion.canBeWritten() ? originalVersion : LATEST_STABLE_SUPPORTED;

    switch (kotlinMetadata.k) {
      case METADATA_KIND_CLASS:
        kotlinMetadata.accept(clazz, new KotlinClassConstructor());
        break;
      case METADATA_KIND_FILE_FACADE:
        kotlinMetadata.accept(clazz, new KotlinFileFacadeConstructor());
        break;
      case METADATA_KIND_SYNTHETIC_CLASS:
        kotlinMetadata.accept(clazz, new KotlinSyntheticClassConstructor());
        break;
      case METADATA_KIND_MULTI_FILE_CLASS_FACADE:
        kotlinMetadata.accept(clazz, new KotlinMultiFileFacadeConstructor());
        break;
      case METADATA_KIND_MULTI_FILE_CLASS_PART:
        kotlinMetadata.accept(clazz, new KotlinMultiFilePartConstructor());
        break;
      default:
    }

    // Pass the new data to the .read() method as a sanity check.
    Metadata metadata =
        JvmMetadataUtil.Metadata(
            metadataKind, metadataVersion, data1, data2, extraString, packageName, extraInt);
    try {
      KotlinClassMetadata.readStrict(metadata);
    } catch (IllegalArgumentException e) {
      String versionString =
          metadataVersion == null
              ? "unknown"
              : Arrays.stream(metadataVersion).mapToObj(Integer::toString).collect(joining("."));
      errorHandler.accept(
          clazz,
          "Encountered corrupt Kotlin metadata in class "
              + clazz.getName()
              + " (version "
              + versionString
              + ")"
              + ". Not processing the metadata for this class.");
      return;
    }

    constantPoolEditor = new ConstantPoolEditor((ProgramClass) clazz);

    try {
      clazz.accept(
          new AllAttributeVisitor(
              new AttributeNameFilter(
                  Attribute.RUNTIME_VISIBLE_ANNOTATIONS,
                  new AllAnnotationVisitor(
                      new AnnotationTypeFilter(
                          TYPE_KOTLIN_METADATA, new AllElementValueVisitor(this))))));
    } catch (IllegalArgumentException e) {
      // It's possible that an exception is thrown by the MetadataType.valueOf calls if
      // the kotlin.Metadata class was accidentally obfuscated.
      errorHandler.accept(
          clazz,
          "Invalid Kotlin metadata annotation for "
              + clazz.getName()
              + " (invalid Kotlin metadata field names)."
              + " Not writing the metadata for this class.");
    }

    // Clean up dangling Strings from the original metadata.
    clazz.accept(constantPoolShrinker);

    if (extraClassVisitor != null) {
      clazz.accept(extraClassVisitor);
    }
  }

  // Implementations for ElementValueVisitor.

  @Override
  public void visitConstantElementValue(
      Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue) {
    this.currentType = KotlinMetadataType.valueOf(constantElementValue.getMethodName(clazz));

    switch (currentType) {
      case k:
        constantElementValue.u2constantValueIndex =
            constantPoolEditor.addIntegerConstant(metadataKind);
        break;
      case xi:
        constantElementValue.u2constantValueIndex = constantPoolEditor.addIntegerConstant(extraInt);
        break;
      case xs:
        constantElementValue.u2constantValueIndex = constantPoolEditor.addUtf8Constant(extraString);
        break;
      case pn:
        constantElementValue.u2constantValueIndex = constantPoolEditor.addUtf8Constant(packageName);
        break;
      default: // Not a constantElementValue. Covered in visitArrayElementValue.
    }
  }

  @Override
  public void visitArrayElementValue(
      Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue) {
    this.currentType = KotlinMetadataType.valueOf(arrayElementValue.getMethodName(clazz));

    switch (currentType) {
      case mv:
        arrayElementValue.u2elementValuesCount = metadataVersion.length;
        ElementValue[] newMvElementValues = new ElementValue[metadataVersion.length];
        for (int k = 0; k < metadataVersion.length; k++) {
          newMvElementValues[k] =
              new ConstantElementValue(
                  'I', 0, constantPoolEditor.addIntegerConstant(metadataVersion[k]));
        }
        arrayElementValue.elementValues = newMvElementValues;
        break;
      case d1:
        arrayElementValue.u2elementValuesCount = data1.length;
        ElementValue[] newD1ElementValues = new ElementValue[data1.length];
        for (int k = 0; k < data1.length; k++) {
          newD1ElementValues[k] =
              new ConstantElementValue('s', 0, constantPoolEditor.addUtf8Constant(data1[k]));
        }
        arrayElementValue.elementValues = newD1ElementValues;
        break;
      case d2:
        arrayElementValue.u2elementValuesCount = data2.length;
        ElementValue[] newD2ElementValues = new ElementValue[data2.length];
        for (int k = 0; k < data2.length; k++) {
          newD2ElementValues[k] =
              new ConstantElementValue('s', 0, constantPoolEditor.addUtf8Constant(data2[k]));
        }
        arrayElementValue.elementValues = newD2ElementValues;
        break;
      default: // Not an arrayElementValue. Covered in visitConstantElementValue.
    }
  }

  // Helper classes.

  private static class ContractConstructor implements KotlinContractVisitor {
    private KmFunction kmFunction;

    ContractConstructor(KmFunction kmFunction) {
      this.kmFunction = kmFunction;
    }

    // Implementations for KotlinContractVisitor.
    @Override
    public void visitContract(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinContractMetadata kotlinContractMetadata) {
      KmContract kmContract = new KmContract();

      kotlinContractMetadata.effectsAccept(
          clazz, kotlinMetadata, kotlinFunctionMetadata, new EffectConstructor(kmContract));
      kmFunction.setContract(kmContract);
    }
  }

  private static class EffectConstructor implements KotlinEffectVisitor {
    private KmContract kmContract;

    private EffectConstructor(KmContract kmContract) {
      this.kmContract = kmContract;
    }

    // Implementations for KotlinEffectVisitor.
    @Override
    public void visitEffect(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinContractMetadata kotlinContractMetadata,
        KotlinEffectMetadata kotlinEffectMetadata) {
      KmEffectType effectType;
      switch (kotlinEffectMetadata.effectType) {
        case CALLS:
          effectType = KmEffectType.CALLS;
          break;
        case RETURNS_CONSTANT:
          effectType = KmEffectType.RETURNS_CONSTANT;
          break;
        case RETURNS_NOT_NULL:
          effectType = KmEffectType.RETURNS_NOT_NULL;
          break;
        default:
          throw new UnsupportedOperationException(
              "Encountered unknown enum value for KotlinEffectType.");
      }

      KmEffectInvocationKind effectInvocationKind = null;
      if (kotlinEffectMetadata.invocationKind != null) {
        switch (kotlinEffectMetadata.invocationKind) {
          case AT_MOST_ONCE:
            effectInvocationKind = KmEffectInvocationKind.AT_MOST_ONCE;
            break;
          case EXACTLY_ONCE:
            effectInvocationKind = KmEffectInvocationKind.EXACTLY_ONCE;
            break;
          case AT_LEAST_ONCE:
            effectInvocationKind = KmEffectInvocationKind.AT_LEAST_ONCE;
            break;
          default:
            throw new UnsupportedOperationException(
                "Encountered unknown enum value for KmEffectInvocationKind.");
        }
      }

      KmEffect kmEffect = new KmEffect(effectType, effectInvocationKind);

      kotlinEffectMetadata.conclusionOfConditionalEffectAccept(
          clazz, new EffectExprConstructor(kmEffect));

      kotlinEffectMetadata.constructorArgumentAccept(clazz, new EffectExprConstructor(kmEffect));

      kmContract.getEffects().add(kmEffect);
    }
  }

  private static class EffectExprConstructor implements KotlinEffectExprVisitor {
    private KmEffectExpression kmEffectExpression;
    private KmEffect kmEffect;

    private EffectExprConstructor(KmEffect kmEffect) {
      this.kmEffect = kmEffect;
    }

    private KmEffectExpression nestedKmEffectExpression;

    private EffectExprConstructor(KmEffectExpression nestedKmEffectExpression) {
      this.nestedKmEffectExpression = nestedKmEffectExpression;
    }

    // Implementations for KotlinEffectExprVisitor.
    @Override
    public void visitAnyEffectExpression(
        Clazz clazz,
        KotlinEffectMetadata kotlinEffectMetadata,
        KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata) {
      kmEffectExpression.setParameterIndex(kotlinEffectExpressionMetadata.parameterIndex);

      Attributes.setNegated(kmEffectExpression, kotlinEffectExpressionMetadata.flags.isNegated);
      Attributes.setNullCheckPredicate(
          kmEffectExpression, kotlinEffectExpressionMetadata.flags.isNullCheckPredicate);

      if (kotlinEffectExpressionMetadata.hasConstantValue) {
        kmEffectExpression.setConstantValue(
            new KmConstantValue(kotlinEffectExpressionMetadata.constantValue));
      }

      kotlinEffectExpressionMetadata.andRightHandSideAccept(
          clazz, kotlinEffectMetadata, new EffectExprConstructor(kmEffectExpression));
      kotlinEffectExpressionMetadata.orRightHandSideAccept(
          clazz, kotlinEffectMetadata, new EffectExprConstructor(kmEffectExpression));

      kotlinEffectExpressionMetadata.typeOfIsAccept(clazz, new TypeConstructor(kmEffectExpression));
    }

    @Override
    public void visitAndRHSExpression(
        Clazz clazz,
        KotlinEffectMetadata kotlinEffectMetadata,
        KotlinEffectExpressionMetadata lhs,
        KotlinEffectExpressionMetadata rhs) {
      kmEffectExpression = new KmEffectExpression();
      visitAnyEffectExpression(clazz, kotlinEffectMetadata, rhs);
      nestedKmEffectExpression.getAndArguments().addAll(kmEffectExpression.getAndArguments());
    }

    @Override
    public void visitOrRHSExpression(
        Clazz clazz,
        KotlinEffectMetadata kotlinEffectMetadata,
        KotlinEffectExpressionMetadata lhs,
        KotlinEffectExpressionMetadata rhs) {
      kmEffectExpression = new KmEffectExpression();
      visitAnyEffectExpression(clazz, kotlinEffectMetadata, rhs);
      nestedKmEffectExpression.getOrArguments().addAll(kmEffectExpression.getOrArguments());
    }

    @Override
    public void visitConstructorArgExpression(
        Clazz clazz,
        KotlinEffectMetadata kotlinEffectMetadata,
        KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata) {
      kmEffectExpression = new KmEffectExpression();
      visitAnyEffectExpression(clazz, kotlinEffectMetadata, kotlinEffectExpressionMetadata);
      kmEffect.getConstructorArguments().add(kmEffectExpression);
    }

    @Override
    public void visitConclusionExpression(
        Clazz clazz,
        KotlinEffectMetadata kotlinEffectMetadata,
        KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata) {
      kmEffectExpression = new KmEffectExpression();
      visitAnyEffectExpression(clazz, kotlinEffectMetadata, kotlinEffectExpressionMetadata);
      kmEffect.setConclusion(kmEffectExpression);
    }
  }

  private static class KotlinDeclarationContainerConstructor
      implements KotlinPropertyVisitor, KotlinFunctionVisitor, KotlinTypeAliasVisitor {
    KmDeclarationContainer kmDeclarationContainer;
    KmProperty kmProperty;

    KotlinDeclarationContainerConstructor(KmDeclarationContainer kmDeclarationContainer) {
      this.kmDeclarationContainer = kmDeclarationContainer;
    }

    // Simplifications for KotlinPropertyVisitor.
    @Override
    public void visitAnyProperty(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata) {
      kotlinPropertyMetadata.typeAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeConstructor(kmProperty));
      kotlinPropertyMetadata.receiverTypeAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeConstructor(kmProperty));
      kotlinPropertyMetadata.contextReceiverTypesAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeConstructor(kmProperty));
      kotlinPropertyMetadata.setterParametersAccept(
          clazz, kotlinDeclarationContainerMetadata, new ValueParameterConstructor(kmProperty));
      kotlinPropertyMetadata.typeParametersAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeParameterConstructor(kmProperty));
      kotlinPropertyMetadata.versionRequirementAccept(
          clazz, kotlinDeclarationContainerMetadata, new VersionRequirementConstructor(kmProperty));

      JvmAttributes.setMovedFromInterfaceCompanion(
          kmProperty, kotlinPropertyMetadata.flags.isMovedFromInterfaceCompanion);
      JvmExtensionsKt.setGetterSignature(
          kmProperty, convertMethodSignature(kotlinPropertyMetadata.getterSignature));
      JvmExtensionsKt.setSetterSignature(
          kmProperty, convertMethodSignature(kotlinPropertyMetadata.setterSignature));
      JvmExtensionsKt.setFieldSignature(
          kmProperty, convertFieldSignature(kotlinPropertyMetadata.backingFieldSignature));

      if (kotlinPropertyMetadata.syntheticMethodForAnnotations != null) {
        JvmExtensionsKt.setSyntheticMethodForAnnotations(
            kmProperty,
            convertMethodSignature(kotlinPropertyMetadata.syntheticMethodForAnnotations));
      }

      if (kotlinPropertyMetadata.syntheticMethodForDelegate != null) {
        JvmExtensionsKt.setSyntheticMethodForDelegate(
            kmProperty, convertMethodSignature(kotlinPropertyMetadata.syntheticMethodForDelegate));
      }
    }

    @Override
    public void visitProperty(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata) {
      kmProperty = convertProperty(kotlinPropertyMetadata);

      visitAnyProperty(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata);
      kmDeclarationContainer.getProperties().add(kmProperty);
    }

    @Override
    public void visitDelegatedProperty(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata) {
      kmProperty = convertProperty(kotlinPropertyMetadata);
      visitAnyProperty(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata);
      kmDeclarationContainer.getProperties().add(kmProperty);
    }

    // Simplifications for KotlinFunctionVisitor.
    @Override
    public void visitAnyFunction(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata) {}

    @Override
    public void visitFunction(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata) {

      KmFunction kmFunction = convertFunction(kotlinFunctionMetadata);

      kotlinFunctionMetadata.valueParametersAccept(
          clazz, kotlinDeclarationContainerMetadata, new ValueParameterConstructor(kmFunction));
      kotlinFunctionMetadata.returnTypeAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeConstructor(kmFunction));
      kotlinFunctionMetadata.receiverTypeAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeConstructor(kmFunction));
      kotlinFunctionMetadata.contextReceiverTypesAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeConstructor(kmFunction));
      kotlinFunctionMetadata.typeParametersAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeParameterConstructor(kmFunction));
      kotlinFunctionMetadata.versionRequirementAccept(
          clazz, kotlinDeclarationContainerMetadata, new VersionRequirementConstructor(kmFunction));
      kotlinFunctionMetadata.contractsAccept(
          clazz, kotlinDeclarationContainerMetadata, new ContractConstructor(kmFunction));

      JvmExtensionsKt.setSignature(
          kmFunction, convertMethodSignature(kotlinFunctionMetadata.jvmSignature));

      if (kotlinFunctionMetadata.lambdaClassOriginName != null) {
        JvmExtensionsKt.setLambdaClassOriginName(
            kmFunction, kotlinFunctionMetadata.lambdaClassOriginName);
      }

      kmDeclarationContainer.getFunctions().add(kmFunction);
    }

    // Implementations for KotlinTypeAliasVisitor
    @Override
    public void visitTypeAlias(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinTypeAliasMetadata kotlinTypeAliasMetadata) {
      KmTypeAlias kmTypeAlias = convertTypeAlias(kotlinTypeAliasMetadata);

      kotlinTypeAliasMetadata.typeParametersAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeParameterConstructor(kmTypeAlias));
      kotlinTypeAliasMetadata.underlyingTypeAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeConstructor(kmTypeAlias));
      kotlinTypeAliasMetadata.expandedTypeAccept(
          clazz, kotlinDeclarationContainerMetadata, new TypeConstructor(kmTypeAlias));
      kotlinTypeAliasMetadata.versionRequirementAccept(
          clazz,
          kotlinDeclarationContainerMetadata,
          new VersionRequirementConstructor(kmTypeAlias));
      kotlinTypeAliasMetadata.annotationsAccept(
          clazz,
          new AnnotationConstructor(
              kmAnnotation -> kmTypeAlias.getAnnotations().add(kmAnnotation)));

      kmDeclarationContainer.getTypeAliases().add(kmTypeAlias);
    }

    // Implementations for KotlinMetadataVisitor.
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    // Helper methods.
    private static KmTypeAlias convertTypeAlias(KotlinTypeAliasMetadata kotlinTypeAliasMetadata) {
      KmTypeAlias kmTypeAlias = new KmTypeAlias(kotlinTypeAliasMetadata.name);

      Attributes.setVisibility(
          kmTypeAlias, convertVisibilityFlags(kotlinTypeAliasMetadata.flags.visibility));
      Attributes.setHasAnnotations(kmTypeAlias, kotlinTypeAliasMetadata.flags.hasAnnotations);

      return kmTypeAlias;
    }

    private static JvmFieldSignature convertFieldSignature(FieldSignature fieldSignature) {
      if (fieldSignature == null) {
        return null;
      }

      return new JvmFieldSignature(fieldSignature.memberName, fieldSignature.descriptor);
    }
  }

  /**
   * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin class (k == 1)
   * metadata.
   */
  private class KotlinClassConstructor extends KotlinDeclarationContainerConstructor
      implements KotlinMetadataVisitor, KotlinConstructorVisitor {
    KmClass kmClass;

    KotlinClassConstructor() {
      this(new KmClass());
    }

    private KotlinClassConstructor(KmClass kmClass) {
      super(kmClass);
      this.kmClass = kmClass;
    }

    // Implementations for KotlinMetadataVisitor.
    @Override
    public void visitKotlinClassMetadata(
        Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata) {
      convertClassFlags(kmClass, kotlinClassKindMetadata.flags);
      kmClass.setName(kotlinClassKindMetadata.className.replace('$', '.'));

      if (kotlinClassKindMetadata.companionObjectName != null) {
        kmClass.setCompanionObject(kotlinClassKindMetadata.companionObjectName);
      }

      kotlinClassKindMetadata.propertiesAccept(clazz, this);
      kotlinClassKindMetadata.functionsAccept(clazz, this);
      kotlinClassKindMetadata.typeAliasesAccept(clazz, this);

      for (String enumEntry : kotlinClassKindMetadata.enumEntryNames) {
        kmClass.getEnumEntries().add(enumEntry);
      }

      for (String nestedClass : kotlinClassKindMetadata.nestedClassNames) {
        kmClass.getNestedClasses().add(nestedClass);
      }

      for (String sealedSubClass : kotlinClassKindMetadata.sealedSubclassNames) {
        kmClass.getSealedSubclasses().add(sealedSubClass.replace('$', '.'));
      }

      kotlinClassKindMetadata.constructorsAccept(clazz, this);
      kotlinClassKindMetadata.superTypesAccept(clazz, new TypeConstructor(kmClass));
      kotlinClassKindMetadata.typeParametersAccept(clazz, new TypeParameterConstructor(kmClass));
      kotlinClassKindMetadata.contextReceiverTypesAccept(clazz, new TypeConstructor(kmClass));
      kotlinClassKindMetadata.versionRequirementAccept(
          clazz, new VersionRequirementConstructor(kmClass));
      kotlinClassKindMetadata.inlineClassUnderlyingPropertyTypeAccept(
          clazz, new TypeConstructor(kmClass));

      for (KotlinPropertyMetadata propertyMetadata :
          kotlinClassKindMetadata.localDelegatedProperties) {
        JvmExtensionsKt.getLocalDelegatedProperties(kmClass).add(convertProperty(propertyMetadata));
      }

      if (kotlinClassKindMetadata.anonymousObjectOriginName != null) {
        JvmExtensionsKt.setAnonymousObjectOriginName(
            kmClass, kotlinClassKindMetadata.anonymousObjectOriginName);
      }

      convertClassFlags(kmClass, kotlinClassKindMetadata.flags);

      JvmAttributes.setCompiledInCompatibilityMode(
          kmClass, kotlinClassKindMetadata.flags.isCompiledInCompatibilityMode);
      JvmAttributes.setHasMethodBodiesInInterface(
          kmClass, kotlinClassKindMetadata.flags.hasMethodBodiesInInterface);

      // Finally store the protobuf contents in the fields of the enclosing class.
      Metadata metadata =
          new KotlinClassMetadata.Class(
                  kmClass, new JvmMetadataVersion(version.toArray()), kotlinClassKindMetadata.xi)
              .write();
      metadataKind = metadata.k();
      metadataVersion = metadata.mv();
      data1 = metadata.d1();
      data2 = metadata.d2();
      extraInt = metadata.xi();
      extraString = metadata.xs();
      packageName = metadata.pn();
    }

    // Implementations for KotlinConstructorVisitor.
    @Override
    public void visitConstructor(
        Clazz clazz,
        KotlinClassKindMetadata kotlinClassKindMetadata,
        KotlinConstructorMetadata kotlinConstructorMetadata) {
      KmConstructor kmConstructor = convertConstructorFlags(kotlinConstructorMetadata.flags);

      kotlinConstructorMetadata.valueParametersAccept(
          clazz, kotlinClassKindMetadata, new ValueParameterConstructor(kmConstructor));

      kotlinConstructorMetadata.versionRequirementAccept(
          clazz, kotlinClassKindMetadata, new VersionRequirementConstructor(kmConstructor));

      // Extensions.
      if (kotlinConstructorMetadata.jvmSignature != null) {
        JvmExtensionsKt.setSignature(
            kmConstructor, convertMethodSignature(kotlinConstructorMetadata.jvmSignature));
      }

      kmClass.getConstructors().add(kmConstructor);
    }

    private void convertClassFlags(KmClass kmClass, KotlinClassFlags flags) {
      Attributes.setHasAnnotations(kmClass, flags.hasAnnotations);
      Attributes.setVisibility(kmClass, convertVisibilityFlags(flags.visibility));
      Attributes.setModality(kmClass, convertModalityFlags(flags.modality));

      if (flags.isUsualClass) {
        Attributes.setKind(kmClass, ClassKind.CLASS);
      }
      if (flags.isInterface) {
        Attributes.setKind(kmClass, ClassKind.INTERFACE);
      }
      if (flags.isEnumClass) {
        Attributes.setKind(kmClass, ClassKind.ENUM_CLASS);
      }
      if (flags.isEnumEntry) {
        Attributes.setKind(kmClass, ClassKind.ENUM_ENTRY);
      }
      if (flags.isAnnotationClass) {
        Attributes.setKind(kmClass, ClassKind.ANNOTATION_CLASS);
      }
      if (flags.isObject) {
        Attributes.setKind(kmClass, ClassKind.OBJECT);
      }
      if (flags.isCompanionObject) {
        Attributes.setKind(kmClass, ClassKind.COMPANION_OBJECT);
      }
      Attributes.setInner(kmClass, flags.isInner);
      Attributes.setData(kmClass, flags.isData);
      Attributes.setExternal(kmClass, flags.isExternal);
      Attributes.setExpect(kmClass, flags.isExpect);
      Attributes.setValue(kmClass, flags.isValue);
      Attributes.setFunInterface(kmClass, flags.isFun);
    }

    private KmConstructor convertConstructorFlags(KotlinConstructorFlags flags) {
      KmConstructor kmConstructor = new KmConstructor();

      Attributes.setHasAnnotations(kmConstructor, flags.hasAnnotations);
      Attributes.setVisibility(kmConstructor, convertVisibilityFlags(flags.visibility));
      Attributes.setSecondary(kmConstructor, flags.isSecondary);
      Attributes.setHasNonStableParameterNames(kmConstructor, flags.hasNonStableParameterNames);

      return kmConstructor;
    }
  }

  private static class ValueParameterConstructor implements KotlinValueParameterVisitor {
    private KmValueParameter kmValueParameter;

    private KmConstructor kmConstructor;

    ValueParameterConstructor(KmConstructor kmConstructor) {
      this.kmConstructor = kmConstructor;
    }

    private KmProperty kmProperty;

    ValueParameterConstructor(KmProperty kmProperty) {
      this.kmProperty = kmProperty;
    }

    private KmFunction kmFunction;

    ValueParameterConstructor(KmFunction kmFunction) {
      this.kmFunction = kmFunction;
    }

    // Implementations for KotlinValueParameterVisitor.
    @Override
    public void visitAnyValueParameter(
        Clazz clazz, KotlinValueParameterMetadata kotlinValueParameterMetadata) {}

    @Override
    public void visitConstructorValParameter(
        Clazz clazz,
        KotlinClassKindMetadata kotlinClassKindMetadata,
        KotlinConstructorMetadata kotlinConstructorMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata) {
      kmValueParameter = new KmValueParameter(kotlinValueParameterMetadata.parameterName);
      convertValueParameterFlags(kmValueParameter, kotlinValueParameterMetadata.flags);

      kotlinValueParameterMetadata.typeAccept(
          clazz,
          kotlinClassKindMetadata,
          kotlinConstructorMetadata,
          new TypeConstructor(kmValueParameter));
      kmConstructor.getValueParameters().add(kmValueParameter);
    }

    @Override
    public void visitPropertyValParameter(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata) {
      kmValueParameter = new KmValueParameter(kotlinValueParameterMetadata.parameterName);
      convertValueParameterFlags(kmValueParameter, kotlinValueParameterMetadata.flags);

      kotlinValueParameterMetadata.typeAccept(
          clazz,
          kotlinDeclarationContainerMetadata,
          kotlinPropertyMetadata,
          new TypeConstructor(kmValueParameter));
      kmProperty.setSetterParameter(kmValueParameter);
    }

    @Override
    public void visitFunctionValParameter(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata) {
      kmValueParameter = new KmValueParameter(kotlinValueParameterMetadata.parameterName);
      convertValueParameterFlags(kmValueParameter, kotlinValueParameterMetadata.flags);

      kotlinValueParameterMetadata.typeAccept(
          clazz, kotlinMetadata, kotlinFunctionMetadata, new TypeConstructor(kmValueParameter));
      kmFunction.getValueParameters().add(kmValueParameter);
    }

    private static void convertValueParameterFlags(
        KmValueParameter kmValueParameter, KotlinValueParameterFlags flags) {
      Attributes.setHasAnnotations(kmValueParameter, flags.hasAnnotations);
      Attributes.setDeclaresDefaultValue(kmValueParameter, flags.hasDefaultValue);
      Attributes.setNoinline(kmValueParameter, flags.isNoInline);
      Attributes.setCrossinline(kmValueParameter, flags.isCrossInline);
    }
  }

  private static class TypeConstructor implements KotlinTypeVisitor {
    private KmType kmType;

    private KmType nestedKmType;

    TypeConstructor(KmType nestedKmType) {
      this.nestedKmType = nestedKmType;
    }

    private KmValueParameter kmValueParameter;

    TypeConstructor(KmValueParameter kmValueParameter) {
      this.kmValueParameter = kmValueParameter;
    }

    private KmClass kmClass;

    TypeConstructor(KmClass kmClass) {
      this.kmClass = kmClass;
    }

    private KmProperty kmProperty;

    TypeConstructor(KmProperty kmProperty) {
      this.kmProperty = kmProperty;
    }

    private KmFunction kmFunction;

    TypeConstructor(KmFunction kmFunction) {
      this.kmFunction = kmFunction;
    }

    private KmTypeAlias kmTypeAlias;

    TypeConstructor(KmTypeAlias kmTypeAlias) {
      this.kmTypeAlias = kmTypeAlias;
    }

    private KmTypeParameter kmTypeParameter;

    TypeConstructor(KmTypeParameter kmTypeParameter) {
      this.kmTypeParameter = kmTypeParameter;
    }

    private KmEffectExpression kmEffectExpression;

    TypeConstructor(KmEffectExpression kmEffectExpression) {
      this.kmEffectExpression = kmEffectExpression;
    }

    // Implementations for KotlinTypeVisitor.

    @Override
    public void visitTypeUpperBound(
        Clazz clazz, KotlinTypeMetadata boundedType, KotlinTypeMetadata upperBound) {
      kmType = convertType(boundedType);
      visitAnyType(clazz, upperBound);

      KmFlexibleTypeUpperBound kmFlexibleTypeUpperBound =
          new KmFlexibleTypeUpperBound(kmType, upperBound.flexibilityID);
      nestedKmType.setFlexibleTypeUpperBound(kmFlexibleTypeUpperBound);
    }

    @Override
    public void visitAbbreviation(
        Clazz clazz, KotlinTypeMetadata abbreviatedType, KotlinTypeMetadata abbreviation) {
      kmType = convertType(abbreviatedType);
      visitAnyType(clazz, abbreviation);

      nestedKmType.setAbbreviatedType(kmType);
    }

    @Override
    public void visitParameterUpperBound(
        Clazz clazz,
        KotlinTypeParameterMetadata boundedTypeParameter,
        KotlinTypeMetadata upperBound) {
      kmType = convertType(upperBound);
      visitAnyType(clazz, upperBound);

      kmTypeParameter.getUpperBounds().add(kmType);
    }

    @Override
    public void visitTypeOfIsExpression(
        Clazz clazz,
        KotlinEffectExpressionMetadata kotlinEffectExprMetadata,
        KotlinTypeMetadata typeOfIs) {
      kmType = convertType(typeOfIs);
      visitAnyType(clazz, typeOfIs);

      kmEffectExpression.setInstanceType(kmType);
    }

    @Override
    public void visitTypeArgument(
        Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata, KotlinTypeMetadata typeArgument) {
      kmType = convertType(typeArgument);
      visitAnyType(clazz, typeArgument);

      KmTypeProjection kmTypeProjection =
          new KmTypeProjection(convertTypeVariance(typeArgument.variance), kmType);
      nestedKmType.getArguments().add(kmTypeProjection);
    }

    @Override
    public void visitStarProjection(Clazz clazz, KotlinTypeMetadata typeWithStarArg) {
      nestedKmType.getArguments().add(KmTypeProjection.STAR);
    }

    @Override
    public void visitOuterClass(
        Clazz clazz, KotlinTypeMetadata innerClass, KotlinTypeMetadata outerClass) {
      kmType = convertType(outerClass);
      visitAnyType(clazz, outerClass);

      nestedKmType.setOuterType(kmType);
    }

    @Override
    public void visitConstructorValParamType(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinConstructorMetadata kotlinConstructorMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmValueParameter.type = kmType;
    }

    @Override
    public void visitConstructorValParamVarArgType(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinConstructorMetadata kotlinConstructorMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmValueParameter.type = kmType;
    }

    @Override
    public void visitInlineClassUnderlyingPropertyType(
        Clazz clazz,
        KotlinClassKindMetadata kotlinMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      if (kotlinMetadata.underlyingPropertyName != null) {
        kmClass.setInlineClassUnderlyingPropertyName(kotlinMetadata.underlyingPropertyName);
      }
      if (kotlinMetadata.underlyingPropertyType != null) {
        kmType = convertType(kotlinMetadata.underlyingPropertyType);
        kmClass.setInlineClassUnderlyingType(kmType);
      }
      visitAnyType(clazz, kotlinTypeMetadata);
    }

    @Override
    public void visitSuperType(
        Clazz clazz,
        KotlinClassKindMetadata kotlinMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmClass.getSupertypes().add(kmType);
    }

    @Override
    public void visitPropertyType(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmProperty.returnType = kmType;
    }

    @Override
    public void visitPropertyReceiverType(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmProperty.setReceiverParameterType(kmType);
    }

    @Override
    public void visitPropertyValParamType(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmValueParameter.type = kmType;
    }

    @Override
    public void visitPropertyValParamVarArgType(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmValueParameter.setVarargElementType(kmType);
    }

    @Override
    public void visitFunctionReturnType(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmFunction.setReturnType(kmType);
    }

    @Override
    public void visitFunctionReceiverType(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmFunction.setReceiverParameterType(kmType);
    }

    @Override
    public void visitFunctionContextReceiverType(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmFunction.getContextReceiverTypes().add(kmType);
    }

    @Override
    public void visitClassContextReceiverType(
        Clazz clazz, KotlinMetadata kotlinMetadata, KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmClass.getContextReceiverTypes().add(kmType);
    }

    @Override
    public void visitPropertyContextReceiverType(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmProperty.getContextReceiverTypes().add(kmType);
    }

    @Override
    public void visitFunctionValParamType(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmValueParameter.type = kmType;
    }

    @Override
    public void visitFunctionValParamVarArgType(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinValueParameterMetadata kotlinValueParameterMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmValueParameter.setVarargElementType(kmType);
    }

    @Override
    public void visitAliasUnderlyingType(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinTypeAliasMetadata kotlinTypeAliasMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmTypeAlias.underlyingType = kmType;
    }

    @Override
    public void visitAliasExpandedType(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinTypeAliasMetadata kotlinTypeAliasMetadata,
        KotlinTypeMetadata kotlinTypeMetadata) {
      kmType = convertType(kotlinTypeMetadata);
      visitAnyType(clazz, kotlinTypeMetadata);

      kmTypeAlias.setExpandedType(kmType);
    }

    // Small helper methods.
    @Override
    public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata) {
      if (kotlinTypeMetadata.className != null) {
        // Transform the class name back to the Kotlin metadata format.
        String className =
            kotlinTypeMetadata.className.replace(
                TypeConstants.INNER_CLASS_SEPARATOR, KotlinConstants.INNER_CLASS_SEPARATOR);

        KmClassifier.Class classifier = new KmClassifier.Class(className);
        kmType.classifier = classifier;
      }

      if (kotlinTypeMetadata.typeParamID >= 0) {
        KmClassifier.TypeParameter classifier =
            new KmClassifier.TypeParameter(kotlinTypeMetadata.typeParamID);
        kmType.classifier = classifier;
      }

      if (kotlinTypeMetadata.aliasName != null) {
        KmClassifier.TypeAlias classifier =
            new KmClassifier.TypeAlias(kotlinTypeMetadata.aliasName);
        kmType.classifier = classifier;
      }

      kotlinTypeMetadata.abbreviationAccept(clazz, new TypeConstructor(kmType));
      kotlinTypeMetadata.outerClassAccept(clazz, new TypeConstructor(kmType));
      kotlinTypeMetadata.typeArgumentsAccept(clazz, new TypeConstructor(kmType));
      kotlinTypeMetadata.upperBoundsAccept(clazz, new TypeConstructor(kmType));

      // Extensions.
      JvmExtensionsKt.setRaw(kmType, kotlinTypeMetadata.isRaw);

      kotlinTypeMetadata.annotationsAccept(
          clazz,
          new AnnotationConstructor(
              kmAnnotation -> JvmExtensionsKt.getAnnotations(kmType).add(kmAnnotation)));
    }

    private static KmType convertType(KotlinTypeMetadata typeMetadata) {
      KmType kmType = new KmType();

      Attributes.setNullable(kmType, typeMetadata.flags.isNullable);
      Attributes.setSuspend(kmType, typeMetadata.flags.isSuspend);
      Attributes.setDefinitelyNonNull(kmType, typeMetadata.flags.isDefinitelyNonNull);

      return kmType;
    }
  }

  private static class TypeParameterConstructor implements KotlinTypeParameterVisitor {
    private KmTypeParameter kmTypeParameter;

    private KmClass kmClass;

    TypeParameterConstructor(KmClass kmClass) {
      this.kmClass = kmClass;
    }

    private KmProperty kmProperty;

    TypeParameterConstructor(KmProperty kmProperty) {
      this.kmProperty = kmProperty;
    }

    private KmFunction kmFunction;

    TypeParameterConstructor(KmFunction kmFunction) {
      this.kmFunction = kmFunction;
    }

    private KmTypeAlias kmTypeAlias;

    TypeParameterConstructor(KmTypeAlias kmTypeAlias) {
      this.kmTypeAlias = kmTypeAlias;
    }

    // Implementations for KotlinTypeParameterVisitor.

    @Override
    public void visitClassTypeParameter(
        Clazz clazz,
        KotlinClassKindMetadata kotlinMetadata,
        KotlinTypeParameterMetadata kotlinTypeParameterMetadata) {
      kmTypeParameter = convertTypeParameter(kotlinTypeParameterMetadata);

      visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);

      kmClass.getTypeParameters().add(kmTypeParameter);
    }

    @Override
    public void visitPropertyTypeParameter(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata,
        KotlinTypeParameterMetadata kotlinTypeParameterMetadata) {
      kmTypeParameter = convertTypeParameter(kotlinTypeParameterMetadata);

      visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);

      kmProperty.getTypeParameters().add(kmTypeParameter);
    }

    @Override
    public void visitFunctionTypeParameter(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinTypeParameterMetadata kotlinTypeParameterMetadata) {
      kmTypeParameter = convertTypeParameter(kotlinTypeParameterMetadata);

      visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);

      kmFunction.getTypeParameters().add(kmTypeParameter);
    }

    @Override
    public void visitAliasTypeParameter(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinTypeAliasMetadata kotlinTypeAliasMetadata,
        KotlinTypeParameterMetadata kotlinTypeParameterMetadata) {
      kmTypeParameter = convertTypeParameter(kotlinTypeParameterMetadata);

      visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);

      kmTypeAlias.getTypeParameters().add(kmTypeParameter);
    }

    // Small helper methods.
    @Override
    public void visitAnyTypeParameter(
        Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata) {
      kotlinTypeParameterMetadata.upperBoundsAccept(clazz, new TypeConstructor(kmTypeParameter));

      // Extensions.
      kotlinTypeParameterMetadata.annotationsAccept(
          clazz,
          new AnnotationConstructor(
              kmAnnotation -> JvmExtensionsKt.getAnnotations(kmTypeParameter).add(kmAnnotation)));
    }

    private static KmTypeParameter convertTypeParameter(
        KotlinTypeParameterMetadata kotlinTypeParameterMetadata) {
      KmTypeParameter kmTypeParameter =
          new KmTypeParameter(
              kotlinTypeParameterMetadata.name,
              kotlinTypeParameterMetadata.id,
              convertTypeVariance(kotlinTypeParameterMetadata.variance));

      Attributes.setReified(kmTypeParameter, kotlinTypeParameterMetadata.flags.isReified);

      return kmTypeParameter;
    }
  }

  /**
   * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin file facade (k == 2)
   * metadata.
   */
  private class KotlinFileFacadeConstructor extends KotlinDeclarationContainerConstructor
      implements KotlinMetadataVisitor {
    private KmPackage kmPackage;

    KotlinFileFacadeConstructor() {
      this(new KmPackage());
    }

    private KotlinFileFacadeConstructor(KmPackage kmPackage) {
      super(kmPackage);
      this.kmPackage = kmPackage;
    }

    // Implementations for KotlinMetadataVisitor.
    @Override
    public void visitKotlinFileFacadeMetadata(
        Clazz clazz, KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata) {
      kotlinFileFacadeKindMetadata.propertiesAccept(clazz, this);
      kotlinFileFacadeKindMetadata.functionsAccept(clazz, this);
      kotlinFileFacadeKindMetadata.typeAliasesAccept(clazz, this);

      for (KotlinPropertyMetadata propertyMetadata :
          kotlinFileFacadeKindMetadata.localDelegatedProperties) {
        JvmExtensionsKt.getLocalDelegatedProperties(kmPackage)
            .add(convertProperty(propertyMetadata));
      }

      // Finally store the protobuf contents in the fields of the enclosing class.
      Metadata metadata =
          new KotlinClassMetadata.FileFacade(
                  kmPackage,
                  new JvmMetadataVersion(version.toArray()),
                  kotlinFileFacadeKindMetadata.xi)
              .write();

      metadataKind = metadata.k();
      metadataVersion = metadata.mv();
      data1 = metadata.d1();
      data2 = metadata.d2();
      extraInt = metadata.xi();
      extraString = metadata.xs();
      packageName = metadata.pn();
    }
  }

  /**
   * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin synthetic class (k ==
   * 3) metadata.
   */
  private class KotlinSyntheticClassConstructor
      implements KotlinMetadataVisitor, KotlinFunctionVisitor {
    private KmLambda kmLambda = null;

    KotlinSyntheticClassConstructor() {}

    // Implementations for KotlinMetadataVisitor.
    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    @Override
    public void visitKotlinSyntheticClassMetadata(
        Clazz clazz, KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata) {

      if (kotlinSyntheticClassKindMetadata.flavor
          == KotlinSyntheticClassKindMetadata.Flavor.LAMBDA) {
        kotlinSyntheticClassKindMetadata.functionsAccept(clazz, this);
      }

      Metadata metadata =
          new KotlinClassMetadata.SyntheticClass(
                  kmLambda,
                  new JvmMetadataVersion(version.toArray()),
                  kotlinSyntheticClassKindMetadata.xi)
              .write();

      metadataKind = metadata.k();
      metadataVersion = metadata.mv();
      data1 = metadata.d1();
      data2 = metadata.d2();
      extraInt = metadata.xi();
      extraString = metadata.xs();
      packageName = metadata.pn();
    }

    // Implementations for KotlinFunctionVisitor.
    @Override
    public void visitAnyFunction(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata) {}

    @Override
    public void visitSyntheticFunction(
        Clazz clazz,
        KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata) {

      KmFunction kmFunction = convertFunction(kotlinFunctionMetadata);

      kotlinFunctionMetadata.valueParametersAccept(
          clazz, kotlinSyntheticClassKindMetadata, new ValueParameterConstructor(kmFunction));
      kotlinFunctionMetadata.returnTypeAccept(
          clazz, kotlinSyntheticClassKindMetadata, new TypeConstructor(kmFunction));
      kotlinFunctionMetadata.receiverTypeAccept(
          clazz, kotlinSyntheticClassKindMetadata, new TypeConstructor(kmFunction));
      kotlinFunctionMetadata.typeParametersAccept(
          clazz, kotlinSyntheticClassKindMetadata, new TypeParameterConstructor(kmFunction));
      kotlinFunctionMetadata.versionRequirementAccept(
          clazz, kotlinSyntheticClassKindMetadata, new VersionRequirementConstructor(kmFunction));
      kotlinFunctionMetadata.contractsAccept(
          clazz, kotlinSyntheticClassKindMetadata, new ContractConstructor(kmFunction));

      JvmExtensionsKt.setSignature(
          kmFunction, convertMethodSignature(kotlinFunctionMetadata.jvmSignature));

      if (kotlinFunctionMetadata.lambdaClassOriginName != null) {
        JvmExtensionsKt.setLambdaClassOriginName(
            kmFunction, kotlinFunctionMetadata.lambdaClassOriginName);
      }

      kmLambda = new KmLambda();
      kmLambda.setFunction(kmFunction);
    }
  }

  /** This utility class constructs the d1 array for Kotlin file facade (k == 2) metadata. */
  private class KotlinMultiFileFacadeConstructor implements KotlinMetadataVisitor {
    // Implementations for KotlinMetadataVisitor.
    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    @Override
    public void visitKotlinMultiFileFacadeMetadata(
        Clazz clazz, KotlinMultiFileFacadeKindMetadata kotlinMultiFileFacadeKindMetadata) {
      Metadata metadata =
          new KotlinClassMetadata.MultiFileClassFacade(
                  kotlinMultiFileFacadeKindMetadata.partClassNames,
                  new JvmMetadataVersion(version.toArray()),
                  kotlinMultiFileFacadeKindMetadata.xi)
              .write();

      metadataKind = metadata.k();
      metadataVersion = metadata.mv();
      data1 = metadata.d1();
      data2 = metadata.d2();
      extraInt = metadata.xi();
      extraString = metadata.xs();
      packageName = metadata.pn();
    }
  }

  /**
   * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin file facade (k == 2)
   * metadata.
   */
  private class KotlinMultiFilePartConstructor extends KotlinDeclarationContainerConstructor
      implements KotlinMetadataVisitor {
    private final KmPackage kmPackage;

    KotlinMultiFilePartConstructor() {
      this(new KmPackage());
    }

    private KotlinMultiFilePartConstructor(KmPackage kmPackage) {
      super(kmPackage);
      this.kmPackage = kmPackage;
    }

    // Implementations for KotlinMetadataVisitor
    @Override
    public void visitKotlinMultiFilePartMetadata(
        Clazz clazz, KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata) {
      kotlinMultiFilePartKindMetadata.propertiesAccept(clazz, this);
      kotlinMultiFilePartKindMetadata.functionsAccept(clazz, this);
      kotlinMultiFilePartKindMetadata.typeAliasesAccept(clazz, this);

      for (KotlinPropertyMetadata propertyMetadata :
          kotlinMultiFilePartKindMetadata.localDelegatedProperties) {
        JvmExtensionsKt.getLocalDelegatedProperties(kmPackage)
            .add(convertProperty(propertyMetadata));
      }

      // Finally store the protobuf contents in the fields of the enclosing class.
      Metadata metadata =
          new KotlinClassMetadata.MultiFileClassPart(
                  kmPackage,
                  kotlinMultiFilePartKindMetadata.facadeName,
                  new JvmMetadataVersion(version.toArray()),
                  kotlinMultiFilePartKindMetadata.xi)
              .write();

      metadataKind = metadata.k();
      metadataVersion = metadata.mv();
      data1 = metadata.d1();
      data2 = metadata.d2();
      extraInt = metadata.xi();
      extraString = metadata.xs();
      packageName = metadata.pn();
    }
  }

  private static class VersionRequirementConstructor implements KotlinVersionRequirementVisitor {
    private KmVersionRequirement kmVersionRequirement;

    private KmConstructor kmConstructor;

    VersionRequirementConstructor(KmConstructor kmConstructor) {
      this.kmConstructor = kmConstructor;
    }

    private KmClass kmClass;

    VersionRequirementConstructor(KmClass kmClass) {
      this.kmClass = kmClass;
    }

    private KmProperty kmProperty;

    VersionRequirementConstructor(KmProperty kmProperty) {
      this.kmProperty = kmProperty;
    }

    private KmFunction kmFunction;

    VersionRequirementConstructor(KmFunction kmFunction) {
      this.kmFunction = kmFunction;
    }

    private KmTypeAlias kmTypeAlias;

    VersionRequirementConstructor(KmTypeAlias kmTypeAlias) {
      this.kmTypeAlias = kmTypeAlias;
    }

    // Implementations for KotlinVersionRequirementVisitor.

    @Override
    public void visitClassVersionRequirement(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata) {
      kmVersionRequirement = new KmVersionRequirement();
      visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

      kmClass.getVersionRequirements().add(kmVersionRequirement);
    }

    @Override
    public void visitConstructorVersionRequirement(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinConstructorMetadata kotlinConstructorMetadata,
        KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata) {
      kmVersionRequirement = new KmVersionRequirement();
      visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

      kmConstructor.getVersionRequirements().add(kmVersionRequirement);
    }

    @Override
    public void visitPropertyVersionRequirement(
        Clazz clazz,
        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
        KotlinPropertyMetadata kotlinPropertyMetadata,
        KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata) {
      kmVersionRequirement = new KmVersionRequirement();
      visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

      kmProperty.getVersionRequirements().add(kmVersionRequirement);
    }

    @Override
    public void visitFunctionVersionRequirement(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinFunctionMetadata kotlinFunctionMetadata,
        KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata) {
      kmVersionRequirement = new KmVersionRequirement();
      visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

      kmFunction.getVersionRequirements().add(kmVersionRequirement);
    }

    @Override
    public void visitTypeAliasVersionRequirement(
        Clazz clazz,
        KotlinMetadata kotlinMetadata,
        KotlinTypeAliasMetadata kotlinTypeAliasMetadata,
        KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata) {
      kmVersionRequirement = new KmVersionRequirement();
      visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

      kmTypeAlias.getVersionRequirements().add(kmVersionRequirement);
    }

    // Small helper methods.
    @Override
    public void visitAnyVersionRequirement(
        Clazz clazz, KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata) {

      switch (kotlinVersionRequirementMetadata.kind) {
        case API_VERSION:
          kmVersionRequirement.kind = KmVersionRequirementVersionKind.API_VERSION;
          break;
        case COMPILER_VERSION:
          kmVersionRequirement.kind = KmVersionRequirementVersionKind.COMPILER_VERSION;
          break;
        case LANGUAGE_VERSION:
          kmVersionRequirement.kind = KmVersionRequirementVersionKind.LANGUAGE_VERSION;
          break;
        default:
          throw new UnsupportedOperationException(
              "Encountered unknown enum value for KotlinVersionRequirementVersionKind.");
      }

      switch (kotlinVersionRequirementMetadata.level) {
        case ERROR:
          kmVersionRequirement.level = KmVersionRequirementLevel.ERROR;
          break;
        case HIDDEN:
          kmVersionRequirement.level = KmVersionRequirementLevel.HIDDEN;
          break;
        case WARNING:
          kmVersionRequirement.level = KmVersionRequirementLevel.WARNING;
          break;
        default:
          throw new UnsupportedOperationException(
              "Encountered unknown enum value for KotlinVersionRequirementLevel.");
      }

      kmVersionRequirement.setErrorCode(kotlinVersionRequirementMetadata.errorCode);
      kmVersionRequirement.setMessage(kotlinVersionRequirementMetadata.message);

      KmVersion kmVersion =
          new KmVersion(
              kotlinVersionRequirementMetadata.major,
              kotlinVersionRequirementMetadata.minor,
              kotlinVersionRequirementMetadata.patch);
      kmVersionRequirement.setVersion(kmVersion);
    }
  }

  // Conversion helper methods.

  private static JvmMethodSignature convertMethodSignature(MethodSignature methodSignature) {
    if (methodSignature == null) {
      return null;
    }

    return new JvmMethodSignature(methodSignature.method, methodSignature.descriptor.toString());
  }

  private static KmVariance convertTypeVariance(KotlinTypeVariance typeVariance) {
    switch (typeVariance) {
      case IN:
        return KmVariance.IN;
      case INVARIANT:
        return KmVariance.INVARIANT;
      case OUT:
        return KmVariance.OUT;
      default:
        throw new UnsupportedOperationException("Encountered unknown enum value for KmVariance.");
    }
  }

  // Flag conversion helper methods.

  private static Visibility convertVisibilityFlags(KotlinVisibilityFlags visibilityFlags) {
    if (visibilityFlags.isInternal) {
      return Visibility.INTERNAL;
    }
    if (visibilityFlags.isLocal) {
      return Visibility.LOCAL;
    }
    if (visibilityFlags.isPrivate) {
      return Visibility.PRIVATE;
    }
    if (visibilityFlags.isProtected) {
      return Visibility.PROTECTED;
    }
    if (visibilityFlags.isPublic) {
      return Visibility.PUBLIC;
    }
    // last option: visibilityFlags.isPrivateToThis
    else {
      return Visibility.PRIVATE_TO_THIS;
    }
  }

  private static Modality convertModalityFlags(KotlinModalityFlags flags) {
    if (flags.isAbstract) {
      return Modality.ABSTRACT;
    }
    if (flags.isFinal) {
      return Modality.FINAL;
    }
    if (flags.isOpen) {
      return Modality.OPEN;
    }
    // last option: flags.isSealed
    else {
      return Modality.SEALED;
    }
  }

  private static KmFunction convertFunction(KotlinFunctionMetadata functionMetadata) {
    KmFunction kmFunction = new KmFunction(functionMetadata.name);

    KotlinFunctionFlags flags = functionMetadata.flags;
    Attributes.setVisibility(kmFunction, convertVisibilityFlags(flags.visibility));
    Attributes.setModality(kmFunction, convertModalityFlags(flags.modality));

    if (flags.isDeclaration) {
      Attributes.setKind(kmFunction, MemberKind.DECLARATION);
    }
    if (flags.isFakeOverride) {
      Attributes.setKind(kmFunction, MemberKind.FAKE_OVERRIDE);
    }
    if (flags.isDelegation) {
      Attributes.setKind(kmFunction, MemberKind.DELEGATION);
    }
    if (flags.isSynthesized) {
      Attributes.setKind(kmFunction, MemberKind.SYNTHESIZED);
    }

    Attributes.setHasAnnotations(kmFunction, flags.hasAnnotations);
    Attributes.setOperator(kmFunction, flags.isOperator);
    Attributes.setInfix(kmFunction, flags.isInfix);
    Attributes.setInline(kmFunction, flags.isInline);
    Attributes.setTailrec(kmFunction, flags.isTailrec);
    Attributes.setExternal(kmFunction, flags.isExternal);
    Attributes.setSuspend(kmFunction, flags.isSuspend);

    return kmFunction;
  }

  private static KmProperty convertProperty(KotlinPropertyMetadata propertyMetadata) {
    KmProperty kmProperty = new KmProperty(propertyMetadata.name);

    KotlinPropertyFlags flags = propertyMetadata.flags;
    Attributes.setVisibility(kmProperty, convertVisibilityFlags(flags.visibility));
    Attributes.setModality(kmProperty, convertModalityFlags(flags.modality));

    if (flags.isDeclared) {
      Attributes.setKind(kmProperty, MemberKind.DECLARATION);
    }
    if (flags.isFakeOverride) {
      Attributes.setKind(kmProperty, MemberKind.FAKE_OVERRIDE);
    }
    if (flags.isDelegation) {
      Attributes.setKind(kmProperty, MemberKind.DELEGATION);
    }
    if (flags.isSynthesized) {
      Attributes.setKind(kmProperty, MemberKind.SYNTHESIZED);
    }

    Attributes.setHasAnnotations(kmProperty, flags.hasAnnotations);
    Attributes.setVar(kmProperty, flags.isVar);
    Attributes.setConst(kmProperty, flags.isConst);
    Attributes.setLateinit(kmProperty, flags.isLateinit);
    Attributes.setHasConstant(kmProperty, flags.hasConstant);
    Attributes.setExternal(kmProperty, flags.isExternal);
    Attributes.setDelegated(kmProperty, flags.isDelegated);
    Attributes.setExpect(kmProperty, flags.isExpect);

    if (propertyMetadata.setterFlags != null) {
      KmPropertyAccessorAttributes kmPropertyAccessorAttributes =
          new KmPropertyAccessorAttributes();

      Attributes.setVisibility(
          kmPropertyAccessorAttributes,
          convertVisibilityFlags(propertyMetadata.setterFlags.visibility));
      Attributes.setModality(
          kmPropertyAccessorAttributes,
          convertModalityFlags(propertyMetadata.setterFlags.modality));

      Attributes.setHasAnnotations(
          kmPropertyAccessorAttributes, propertyMetadata.setterFlags.hasAnnotations);
      Attributes.setNotDefault(
          kmPropertyAccessorAttributes, !propertyMetadata.setterFlags.isDefault);
      Attributes.setInline(kmPropertyAccessorAttributes, propertyMetadata.setterFlags.isInline);
      Attributes.setExternal(kmPropertyAccessorAttributes, propertyMetadata.setterFlags.isExternal);

      kmProperty.setSetter(kmPropertyAccessorAttributes);
    }

    if (propertyMetadata.getterFlags != null) {
      KmPropertyAccessorAttributes kmPropertyAccessorAttributes = kmProperty.getGetter();

      Attributes.setVisibility(
          kmPropertyAccessorAttributes,
          convertVisibilityFlags(propertyMetadata.getterFlags.visibility));
      Attributes.setModality(
          kmPropertyAccessorAttributes,
          convertModalityFlags(propertyMetadata.getterFlags.modality));

      Attributes.setHasAnnotations(
          kmPropertyAccessorAttributes, propertyMetadata.getterFlags.hasAnnotations);
      Attributes.setNotDefault(
          kmPropertyAccessorAttributes, !propertyMetadata.getterFlags.isDefault);
      Attributes.setInline(kmPropertyAccessorAttributes, propertyMetadata.getterFlags.isInline);
      Attributes.setExternal(kmPropertyAccessorAttributes, propertyMetadata.getterFlags.isExternal);
    }

    return kmProperty;
  }
}
