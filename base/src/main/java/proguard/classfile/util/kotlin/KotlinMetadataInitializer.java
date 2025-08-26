/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2024 Guardsquare NV
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
package proguard.classfile.util.kotlin;

import static java.util.stream.Collectors.joining;
import static proguard.classfile.attribute.Attribute.RUNTIME_VISIBLE_ANNOTATIONS;
import static proguard.classfile.kotlin.KotlinConstants.DEFAULT_IMPLEMENTATIONS_SUFFIX;
import static proguard.classfile.kotlin.KotlinConstants.KOLTIN_METADATA_FIELD_XS;
import static proguard.classfile.kotlin.KotlinConstants.KOTLIN_METADATA_FIELD_BV;
import static proguard.classfile.kotlin.KotlinConstants.KOTLIN_METADATA_FIELD_D1;
import static proguard.classfile.kotlin.KotlinConstants.KOTLIN_METADATA_FIELD_D2;
import static proguard.classfile.kotlin.KotlinConstants.KOTLIN_METADATA_FIELD_K;
import static proguard.classfile.kotlin.KotlinConstants.KOTLIN_METADATA_FIELD_MV;
import static proguard.classfile.kotlin.KotlinConstants.KOTLIN_METADATA_FIELD_PN;
import static proguard.classfile.kotlin.KotlinConstants.KOTLIN_METADATA_FIELD_XI;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_CLASS;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_FILE_FACADE;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_MULTI_FILE_CLASS_FACADE;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_MULTI_FILE_CLASS_PART;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_SYNTHETIC_CLASS;
import static proguard.classfile.kotlin.KotlinConstants.TYPE_KOTLIN_METADATA;
import static proguard.classfile.kotlin.KotlinConstants.WHEN_MAPPINGS_SUFFIX;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import kotlin.Metadata;
import kotlin.metadata.Attributes;
import kotlin.metadata.ClassKind;
import kotlin.metadata.InconsistentKotlinMetadataException;
import kotlin.metadata.KmClass;
import kotlin.metadata.KmClassifier;
import kotlin.metadata.KmConstructor;
import kotlin.metadata.KmContract;
import kotlin.metadata.KmEffect;
import kotlin.metadata.KmEffectExpression;
import kotlin.metadata.KmEffectInvocationKind;
import kotlin.metadata.KmEffectType;
import kotlin.metadata.KmEnumEntry;
import kotlin.metadata.KmFlexibleTypeUpperBound;
import kotlin.metadata.KmFunction;
import kotlin.metadata.KmPackage;
import kotlin.metadata.KmProperty;
import kotlin.metadata.KmPropertyAccessorAttributes;
import kotlin.metadata.KmType;
import kotlin.metadata.KmTypeAlias;
import kotlin.metadata.KmTypeParameter;
import kotlin.metadata.KmTypeProjection;
import kotlin.metadata.KmValueParameter;
import kotlin.metadata.KmVariance;
import kotlin.metadata.KmVersionRequirement;
import kotlin.metadata.MemberKind;
import kotlin.metadata.Modality;
import kotlin.metadata.Visibility;
import kotlin.metadata.jvm.JvmAttributes;
import kotlin.metadata.jvm.JvmExtensionsKt;
import kotlin.metadata.jvm.JvmFieldSignature;
import kotlin.metadata.jvm.JvmMetadataUtil;
import kotlin.metadata.jvm.JvmMethodSignature;
import kotlin.metadata.jvm.KotlinClassMetadata;
import proguard.classfile.Clazz;
import proguard.classfile.FieldSignature;
import proguard.classfile.LibraryClass;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramClass;
import proguard.classfile.TypeConstants;
import proguard.classfile.attribute.annotation.Annotation;
import proguard.classfile.attribute.annotation.ArrayElementValue;
import proguard.classfile.attribute.annotation.ConstantElementValue;
import proguard.classfile.attribute.annotation.visitor.AllAnnotationVisitor;
import proguard.classfile.attribute.annotation.visitor.AnnotationTypeFilter;
import proguard.classfile.attribute.annotation.visitor.AnnotationVisitor;
import proguard.classfile.attribute.annotation.visitor.ElementValueVisitor;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.constant.IntegerConstant;
import proguard.classfile.constant.Utf8Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinConstants;
import proguard.classfile.kotlin.KotlinConstructorMetadata;
import proguard.classfile.kotlin.KotlinContractMetadata;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinEffectExpressionMetadata;
import proguard.classfile.kotlin.KotlinEffectInvocationKind;
import proguard.classfile.kotlin.KotlinEffectMetadata;
import proguard.classfile.kotlin.KotlinEffectType;
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
import proguard.classfile.kotlin.KotlinVersionRequirementLevel;
import proguard.classfile.kotlin.KotlinVersionRequirementMetadata;
import proguard.classfile.kotlin.KotlinVersionRequirementVersionKind;
import proguard.classfile.kotlin.UnsupportedKotlinMetadata;
import proguard.classfile.kotlin.flags.KotlinClassFlags;
import proguard.classfile.kotlin.flags.KotlinConstructorFlags;
import proguard.classfile.kotlin.flags.KotlinEffectExpressionFlags;
import proguard.classfile.kotlin.flags.KotlinFunctionFlags;
import proguard.classfile.kotlin.flags.KotlinModalityFlags;
import proguard.classfile.kotlin.flags.KotlinPropertyAccessorFlags;
import proguard.classfile.kotlin.flags.KotlinPropertyFlags;
import proguard.classfile.kotlin.flags.KotlinTypeAliasFlags;
import proguard.classfile.kotlin.flags.KotlinTypeFlags;
import proguard.classfile.kotlin.flags.KotlinTypeParameterFlags;
import proguard.classfile.kotlin.flags.KotlinValueParameterFlags;
import proguard.classfile.kotlin.flags.KotlinVisibilityFlags;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.WarningPrinter;
import proguard.classfile.visitor.ClassVisitor;

/**
 * Initializes the kotlin metadata for a Kotlin class.
 *
 * <p>Provides two APIs:
 *
 * <p>- Visitor: use as a ClassVisitor or AnnotationVisitor to initialize the Kotlin metadata
 * contain within a {@link kotlin.Metadata} annotation. After initialization, all info from the
 * annotation is represented in the {@link Clazz}'s {@link ProgramClass#kotlinMetadata} field.
 *
 * <p>Note: only applicable for {@link ProgramClass}.
 *
 * <p>- `initialize`: provide the {@link Clazz} and {@link kotlin.Metadata} field values to the
 * {@link KotlinMetadataInitializer#initialize(Clazz, int, int[], String[], String[], int, String,
 * String)} method to initialize Kotlin metadata for the given {@link Clazz}.
 */
public class KotlinMetadataInitializer
    implements ClassVisitor,
        AnnotationVisitor,

        // Implementation interfaces.
        ElementValueVisitor,
        ConstantVisitor {
  // For original definitions see
  // https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/jvm/runtime/kotlin/Metadata.kt
  private int k;
  private int[] mv;
  private String[] d1;
  private String[] d2;
  private int xi;
  private String xs;
  private String pn;

  private final BiConsumer<Clazz, String> errorHandler;

  public KotlinMetadataInitializer(WarningPrinter warningPrinter) {
    this((clazz, message) -> warningPrinter.print(clazz.getName(), message));
  }

  public KotlinMetadataInitializer(BiConsumer<Clazz, String> errorHandler) {
    this.errorHandler = errorHandler;
  }

  // Implementations for ClassVisitor
  @Override
  public void visitAnyClass(Clazz clazz) {}

  @Override
  public void visitLibraryClass(LibraryClass libraryClass) {
    // LibraryClass models do not contain constant pools, attributes, so
    // they cannot be initialized by a visitor.
    // They should be initialized instead with the `initialize` method.
  }

  @Override
  public void visitProgramClass(ProgramClass clazz) {
    clazz.accept(
        new AllAttributeVisitor(
            new AttributeNameFilter(
                RUNTIME_VISIBLE_ANNOTATIONS,
                new AllAnnotationVisitor(new AnnotationTypeFilter(TYPE_KOTLIN_METADATA, this)))));
  }

  // Implementations for AnnotationVisitor.
  @Override
  public void visitAnnotation(Clazz clazz, Annotation annotation) {
    // Collect the metadata.
    this.k = -1;
    this.mv = null; // new int[] { 1, 0, 0 };
    this.d1 = null; // new String[0];
    this.d2 = null; // new String[0];
    this.xi =
        0; // Optional flags, the `xi` annotation field may not be present so default to none set.
    this.xs = null;
    this.pn = null;

    try {
      annotation.elementValuesAccept(clazz, this);
    } catch (Exception e) {
      errorHandler.accept(
          clazz,
          "Encountered corrupt Kotlin metadata in class "
              + clazz.getName()
              + ". The metadata for this class will not be processed ("
              + e.getMessage()
              + ")");
      clazz.accept(
          new SimpleKotlinMetadataSetter(new UnsupportedKotlinMetadata(k, mv, xi, xs, pn)));
      return;
    }

    initialize(clazz, k, mv, d1, d2, xi, xs, pn);
  }

  /**
   * Initialize Kotlin metadata for a given {@link Clazz}.
   *
   * @param clazz The {@link ProgramClass} or {@link LibraryClass}.
   */
  public void initialize(
      Clazz clazz, int k, int[] mv, String[] d1, String[] d2, int xi, String xs, String pn) {
    // Parse the collected metadata.
    Metadata metadata = JvmMetadataUtil.Metadata(k, mv, d1, d2, xs, pn, xi);
    KotlinClassMetadata md;
    try {
      md = KotlinClassMetadata.readStrict(metadata);
    } catch (IllegalArgumentException e) {
      String version =
          mv == null
              ? "unknown"
              : Arrays.stream(mv).mapToObj(Integer::toString).collect(joining("."));
      errorHandler.accept(
          clazz,
          "Encountered corrupt @kotlin/Metadata for class "
              + clazz.getName()
              + " (version "
              + version
              + ").");
      clazz.accept(
          new SimpleKotlinMetadataSetter(new UnsupportedKotlinMetadata(k, mv, xi, xs, pn)));
      return;
    }

    try {
      switch (k) {
        case METADATA_KIND_CLASS:
          KotlinClassKindMetadata kotlinClassKindMetadata = convertClassKindMetadata(metadata, md);
          kotlinClassKindMetadata.ownerClassName = clazz.getName();
          clazz.accept(new SimpleKotlinMetadataSetter(kotlinClassKindMetadata));
          break;

        case METADATA_KIND_FILE_FACADE: // For package level functions/properties
          KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata =
              convertFileFacadeKindMetadata(metadata, md);

          kotlinFileFacadeKindMetadata.ownerClassName = clazz.getName();
          clazz.accept(new SimpleKotlinMetadataSetter(kotlinFileFacadeKindMetadata));
          break;

        case METADATA_KIND_SYNTHETIC_CLASS:
          KotlinSyntheticClassKindMetadata.Flavor flavor;

          KotlinClassMetadata.SyntheticClass smd = ((KotlinClassMetadata.SyntheticClass) md);

          if (smd.isLambda()) {
            flavor = KotlinSyntheticClassKindMetadata.Flavor.LAMBDA;
          } else if (clazz.getName().endsWith(DEFAULT_IMPLEMENTATIONS_SUFFIX)) {
            flavor = KotlinSyntheticClassKindMetadata.Flavor.DEFAULT_IMPLS;
          } else if (clazz.getName().endsWith(WHEN_MAPPINGS_SUFFIX)) {
            flavor = KotlinSyntheticClassKindMetadata.Flavor.WHEN_MAPPINGS;
          } else {
            flavor = KotlinSyntheticClassKindMetadata.Flavor.REGULAR;
          }

          KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata =
              new KotlinSyntheticClassKindMetadata(mv, xi, xs, pn, flavor);

          if (smd.isLambda()) {
            // Only lambdas contain exactly 1 function.
            kotlinSyntheticClassKindMetadata.functions = new ArrayList<>(1);
            kotlinSyntheticClassKindMetadata.functions.add(
                convertKmFunction(Objects.requireNonNull(smd.getKmLambda()).getFunction()));
          } else {
            // Other synthetic classes never contain any functions.
            kotlinSyntheticClassKindMetadata.functions = Collections.emptyList();
          }

          clazz.accept(new SimpleKotlinMetadataSetter(kotlinSyntheticClassKindMetadata));
          break;

        case METADATA_KIND_MULTI_FILE_CLASS_FACADE:
          // The relevant data for this kind is in d1. It is a list of Strings
          // representing the part class names.
          clazz.accept(
              new SimpleKotlinMetadataSetter(
                  new KotlinMultiFileFacadeKindMetadata(mv, d1, xi, xs, pn)));
          break;

        case METADATA_KIND_MULTI_FILE_CLASS_PART:
          KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata =
              convertMultiFilePartKindMetadata(metadata, md);

          kotlinMultiFilePartKindMetadata.ownerClassName = clazz.getName();
          clazz.accept(new SimpleKotlinMetadataSetter(kotlinMultiFilePartKindMetadata));
          break;

        default:
          // This happens when the library is outdated and a newer type of Kotlin class is passed.
          errorHandler.accept(
              clazz,
              "Unknown Kotlin class kind in class "
                  + clazz.getName()
                  + ". The metadata for this class will not be processed.");
          clazz.accept(
              new SimpleKotlinMetadataSetter(new UnsupportedKotlinMetadata(k, mv, xi, xs, pn)));
          break;
      }
    } catch (InconsistentKotlinMetadataException e) {
      errorHandler.accept(
          clazz,
          "Encountered corrupt Kotlin metadata in class "
              + clazz.getName()
              + ". The metadata for this class will not be processed ("
              + e.getMessage()
              + ")");
      clazz.accept(
          new SimpleKotlinMetadataSetter(new UnsupportedKotlinMetadata(k, mv, xi, xs, pn)));
    }
  }

  // Implementations for ElementValueVisitor.
  @Override
  public void visitConstantElementValue(
      Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue) {
    this.currentType = metadataTypeOf(constantElementValue.getMethodName(clazz));
    clazz.constantPoolEntryAccept(constantElementValue.u2constantValueIndex, this);
  }

  @Override
  public void visitArrayElementValue(
      Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue) {
    KotlinMetadataType arrayElementType = metadataTypeOf(arrayElementValue.getMethodName(clazz));
    switch (arrayElementType) {
      case mv:
        this.mv = new int[arrayElementValue.u2elementValuesCount];
        break;
      case d1:
        this.d1 = new String[arrayElementValue.u2elementValuesCount];
        break;
      case d2:
        this.d2 = new String[arrayElementValue.u2elementValuesCount];
        break;
      default:
        break;
    }

    arrayElementValue.elementValuesAccept(
        clazz, annotation, new ArrayElementValueCollector(arrayElementType));
  }

  // Implementations for ConstantVisitor
  private KotlinMetadataType currentType;

  @Override
  public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
    if (this.currentType == KotlinMetadataType.xs) {
      xs = utf8Constant.getString();
    } else if (this.currentType == KotlinMetadataType.pn) {
      pn = utf8Constant.getString();
    } else {
      throw new UnsupportedOperationException("Cannot store Utf8Constant in int");
    }
  }

  @Override
  public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant) {
    if (this.currentType == KotlinMetadataType.k) {
      k = integerConstant.getValue();
    } else if (this.currentType == KotlinMetadataType.xi) {
      xi = integerConstant.getValue();
    } else {
      throw new UnsupportedOperationException("Cannot store Utf8Constant in int");
    }
  }

  private class ArrayElementValueCollector
      implements ElementValueVisitor,

          // Implementation interfaces.
          ConstantVisitor {
    private final KotlinMetadataType arrayType;
    private int index;

    ArrayElementValueCollector(KotlinMetadataType array) {
      this.arrayType = array;
      this.index = 0;
    }

    // Implementations for ElementValueVisitor
    @Override
    public void visitConstantElementValue(
        Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue) {
      clazz.constantPoolEntryAccept(constantElementValue.u2constantValueIndex, this);
    }

    // Implementations for ConstantVisitor
    @Override
    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
      if (this.arrayType == KotlinMetadataType.d1) {
        d1[index++] = utf8Constant.getString();
      } else if (this.arrayType == KotlinMetadataType.d2) {
        d2[index++] = utf8Constant.getString();
      } else {
        throw new UnsupportedOperationException("Cannot store UTF8Constant in int[]");
      }
    }

    @Override
    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant) {
      switch (arrayType) {
        case mv:
          mv[index++] = integerConstant.getValue();
          break;
        case bv: // Deprecated & removed from kotlin.metadata library, do nothing.
          break;
        default:
          throw new UnsupportedOperationException("Cannot store IntegerConstant in String[]");
      }
    }
  }

  private static class SimpleKotlinMetadataSetter implements ClassVisitor {
    private final KotlinMetadata kmd;

    SimpleKotlinMetadataSetter(KotlinMetadata kmd) {
      this.kmd = kmd;
    }

    @Override
    public void visitAnyClass(Clazz clazz) {
      throw new UnsupportedOperationException(
          this.getClass().getName() + " does not support " + clazz.getClass().getName());
    }

    @Override
    public void visitProgramClass(ProgramClass programClass) {
      programClass.kotlinMetadata = kmd;
    }

    @Override
    public void visitLibraryClass(LibraryClass libraryClass) {
      libraryClass.kotlinMetadata = kmd;
    }
  }

  /** Convert a {@link KotlinClassMetadata} to an internal {@link KotlinClassKindMetadata} model. */
  private KotlinClassKindMetadata convertClassKindMetadata(
      Metadata metadata, KotlinClassMetadata md) {
    KotlinClassKindMetadata kotlinClassKindMetadata =
        new KotlinClassKindMetadata(metadata.mv(), metadata.xi(), metadata.xs(), metadata.pn());

    KotlinClassMetadata.Class classMetadata = (KotlinClassMetadata.Class) md;
    KmClass kmClass = classMetadata.getKmClass();

    String className = kmClass.getName();
    if (className.startsWith(".")) {
      // If the class has a "local class name", the passed String starts with a dot. This appears to
      // be safe to ignore
      className = className.substring(1);
    }

    // Inner classes are marked with a dot after the enclosing class instead
    // of '$' (only here, not in the actual d2 array).
    className = className.replace('.', '$');

    kotlinClassKindMetadata.className = className;
    kotlinClassKindMetadata.flags = convertClassFlags(kmClass);

    kotlinClassKindMetadata.companionObjectName = kmClass.getCompanionObject();
    kotlinClassKindMetadata.underlyingPropertyName = kmClass.getInlineClassUnderlyingPropertyName();
    kotlinClassKindMetadata.underlyingPropertyType =
        convertKmType(kmClass.getInlineClassUnderlyingType());
    kotlinClassKindMetadata.enumEntryNames =
        kmClass.getKmEnumEntries().stream().map(KmEnumEntry::getName).collect(Collectors.toList());
    kotlinClassKindMetadata.nestedClassNames = kmClass.getNestedClasses();
    kotlinClassKindMetadata.sealedSubclassNames =
        kmClass.getSealedSubclasses().stream()
            .map(it -> it.replace(".", "$"))
            .collect(Collectors.toList());

    kotlinClassKindMetadata.versionRequirement =
        convertKmVersionRequirement(kmClass.getVersionRequirements());

    kotlinClassKindMetadata.typeParameters =
        kmClass.getTypeParameters().stream()
            .map(KotlinMetadataInitializer::convertKmTypeParameter)
            .collect(Collectors.toList());

    kotlinClassKindMetadata.contextReceivers =
        kmClass.getContextReceiverTypes().stream()
            .map(KotlinMetadataInitializer::convertKmType)
            .collect(Collectors.toList());

    kotlinClassKindMetadata.superTypes =
        kmClass.getSupertypes().stream()
            .map(KotlinMetadataInitializer::convertKmType)
            .collect(Collectors.toList());

    kotlinClassKindMetadata.constructors =
        kmClass.getConstructors().stream()
            .map(it -> convertKmConstructor(kotlinClassKindMetadata.flags.isAnnotationClass, it))
            .collect(Collectors.toList());

    kotlinClassKindMetadata.functions =
        kmClass.getFunctions().stream()
            .map(KotlinMetadataInitializer::convertKmFunction)
            .collect(Collectors.toList());

    kotlinClassKindMetadata.properties =
        kmClass.getProperties().stream()
            .map(KotlinMetadataInitializer::convertKmProperty)
            .collect(Collectors.toList());

    // Currently only top-level typeAlias declarations are allowed, so this
    // list should normally be empty, but you can disable the compiler
    // error and allow typeAlias declarations here with this annotation:
    // @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    kotlinClassKindMetadata.typeAliases =
        kmClass.getTypeAliases().stream()
            .map(KotlinMetadataInitializer::convertKmTypeAlias)
            .collect(Collectors.toList());

    // JvmExtensions

    kotlinClassKindMetadata.flags.hasMethodBodiesInInterface =
        JvmAttributes.getHasMethodBodiesInInterface(kmClass);
    kotlinClassKindMetadata.flags.isCompiledInCompatibilityMode =
        JvmAttributes.isCompiledInCompatibilityMode(kmClass);

    kotlinClassKindMetadata.anonymousObjectOriginName =
        JvmExtensionsKt.getAnonymousObjectOriginName(kmClass);
    kotlinClassKindMetadata.localDelegatedProperties =
        JvmExtensionsKt.getLocalDelegatedProperties(kmClass).stream()
            .map(KotlinMetadataInitializer::convertKmProperty)
            .collect(Collectors.toList());

    return kotlinClassKindMetadata;
  }

  private static KotlinFileFacadeKindMetadata convertFileFacadeKindMetadata(
      Metadata metadata, KotlinClassMetadata md) {
    KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata =
        new KotlinFileFacadeKindMetadata(
            metadata.mv(), metadata.xi(), metadata.xs(), metadata.pn());

    KotlinClassMetadata.FileFacade fileFacade = (KotlinClassMetadata.FileFacade) md;
    KmPackage kmPackage = fileFacade.getKmPackage();

    populateFromKmPackage(kotlinFileFacadeKindMetadata, kmPackage);

    return kotlinFileFacadeKindMetadata;
  }

  private static KotlinMultiFilePartKindMetadata convertMultiFilePartKindMetadata(
      Metadata metadata, KotlinClassMetadata md) {
    KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata =
        new KotlinMultiFilePartKindMetadata(
            metadata.mv(), metadata.xi(), metadata.xs(), metadata.pn());

    KotlinClassMetadata.MultiFileClassPart fileFacade = (KotlinClassMetadata.MultiFileClassPart) md;
    KmPackage kmPackage = fileFacade.getKmPackage();

    populateFromKmPackage(kotlinMultiFilePartKindMetadata, kmPackage);

    return kotlinMultiFilePartKindMetadata;
  }

  private static KotlinFunctionMetadata convertKmFunction(KmFunction kmFunction) {
    KotlinFunctionMetadata kotlinFunctionMetadata =
        new KotlinFunctionMetadata(convertFunctionFlags(kmFunction), kmFunction.getName());

    // TODO: We previously used a list, but there should be a single contract.
    kotlinFunctionMetadata.contracts =
        kmFunction.getContract() != null
            ? new ArrayList<>(Collections.singleton(convertKmContract(kmFunction.getContract())))
            : new ArrayList<>();

    kotlinFunctionMetadata.receiverType = convertKmType(kmFunction.getReceiverParameterType());
    kotlinFunctionMetadata.contextReceivers =
        kmFunction.getContextReceiverTypes().stream()
            .map(KotlinMetadataInitializer::convertKmType)
            .collect(Collectors.toList());

    kotlinFunctionMetadata.returnType = convertKmType(kmFunction.returnType);
    kotlinFunctionMetadata.typeParameters =
        kmFunction.getTypeParameters().stream()
            .map(KotlinMetadataInitializer::convertKmTypeParameter)
            .collect(Collectors.toList());

    List<KmValueParameter> valueParameters = kmFunction.getValueParameters();
    kotlinFunctionMetadata.valueParameters = new ArrayList<>(valueParameters.size());
    for (int i = 0; i < valueParameters.size(); i++) {
      kotlinFunctionMetadata.valueParameters.add(
          convertKmValueParameter(i, valueParameters.get(i)));
    }

    kotlinFunctionMetadata.versionRequirement =
        convertKmVersionRequirement(kmFunction.getVersionRequirements());
    kotlinFunctionMetadata.jvmSignature =
        convertJvmMethodSignature(JvmExtensionsKt.getSignature(kmFunction));
    kotlinFunctionMetadata.lambdaClassOriginName =
        JvmExtensionsKt.getLambdaClassOriginName(kmFunction);

    return kotlinFunctionMetadata;
  }

  private static KotlinContractMetadata convertKmContract(KmContract kmContract) {
    KotlinContractMetadata kotlinContractMetadata = new KotlinContractMetadata();

    kotlinContractMetadata.effects =
        kmContract.getEffects().stream()
            .map(KotlinMetadataInitializer::convertKmEffect)
            .collect(Collectors.toList());

    return kotlinContractMetadata;
  }

  private static KotlinEffectMetadata convertKmEffect(KmEffect kmEffect) {
    KotlinEffectMetadata effect =
        new KotlinEffectMetadata(
            convertKmEffectType(kmEffect.getType()),
            convertKmEffectInvocationKind(kmEffect.getInvocationKind()));

    effect.conclusionOfConditionalEffect = convertKmEffectExpression(kmEffect.getConclusion());
    effect.constructorArguments =
        kmEffect.getConstructorArguments().stream()
            .map(KotlinMetadataInitializer::convertKmEffectExpression)
            .collect(Collectors.toList());

    return effect;
  }

  private static KotlinEffectExpressionMetadata convertKmEffectExpression(
      KmEffectExpression kmEffectExpression) {
    if (kmEffectExpression == null) {
      return null;
    }

    KotlinEffectExpressionMetadata expressionMetadata = new KotlinEffectExpressionMetadata();

    expressionMetadata.flags = convertEffectExpressionFlags(kmEffectExpression);

    if (kmEffectExpression.getParameterIndex() != null) {
      // Optional 1-based index of the value parameter of the function, for effects which assert
      // something about
      // the function parameters. The index 0 means the extension receiver parameter. May be null
      expressionMetadata.parameterIndex = kmEffectExpression.getParameterIndex();
    }

    if (kmEffectExpression.getConstantValue() != null) {
      // The constant value used in the effect expression. May be `true`, `false` or `null`.
      expressionMetadata.hasConstantValue = true;
      expressionMetadata.constantValue = kmEffectExpression.getConstantValue().getValue();
    }

    if (kmEffectExpression.isInstanceType() != null) {
      expressionMetadata.typeOfIs = convertKmType(kmEffectExpression.isInstanceType());
    }

    expressionMetadata.andRightHandSides =
        kmEffectExpression.getAndArguments().stream()
            .map(KotlinMetadataInitializer::convertKmEffectExpression)
            .collect(Collectors.toList());

    expressionMetadata.orRightHandSides =
        kmEffectExpression.getOrArguments().stream()
            .map(KotlinMetadataInitializer::convertKmEffectExpression)
            .collect(Collectors.toList());

    return expressionMetadata;
  }

  private static KotlinTypeAliasMetadata convertKmTypeAlias(KmTypeAlias kmTypeAlias) {
    KotlinTypeAliasMetadata typeAlias =
        new KotlinTypeAliasMetadata(convertTypeAliasFlags(kmTypeAlias), kmTypeAlias.getName());

    typeAlias.underlyingType = convertKmType(kmTypeAlias.getUnderlyingType());
    typeAlias.expandedType = convertKmType(kmTypeAlias.getExpandedType());
    typeAlias.versionRequirement =
        convertKmVersionRequirement(kmTypeAlias.getVersionRequirements());

    typeAlias.annotations =
        kmTypeAlias.getAnnotations().stream()
            .map(KotlinAnnotationUtilKt::convertAnnotation)
            .collect(Collectors.toList());

    typeAlias.typeParameters =
        kmTypeAlias.getTypeParameters().stream()
            .map(KotlinMetadataInitializer::convertKmTypeParameter)
            .collect(Collectors.toList());

    return typeAlias;
  }

  private static KotlinPropertyMetadata convertKmProperty(KmProperty kmProperty) {
    // We are checking whether getSetter is null because we have encountered occurrences where
    // Attributes.isVar returns true even though there is no setter for the given property.
    KotlinPropertyAccessorFlags setterFlags =
        kmProperty.getSetter() != null
            ? convertPropertyAccessorFlags(kmProperty.getSetter())
            : null;
    KotlinPropertyMetadata property =
        new KotlinPropertyMetadata(
            convertPropertyFlags(kmProperty),
            kmProperty.getName(),
            convertPropertyAccessorFlags(kmProperty.getGetter()),
            setterFlags);

    property.receiverType = convertKmType(kmProperty.getReceiverParameterType());

    property.contextReceivers =
        kmProperty.getContextReceiverTypes().stream()
            .map(KotlinMetadataInitializer::convertKmType)
            .collect(Collectors.toList());

    property.type = convertKmType(kmProperty.returnType);
    property.versionRequirement = convertKmVersionRequirement(kmProperty.getVersionRequirements());

    KmValueParameter setterParameter = kmProperty.getSetterParameter();
    // TODO(deprecation): Remove the deprecated setterParameters initialisation.
    property.setterParameters =
        setterParameter != null
            ? new ArrayList<>(
                Collections.singletonList(convertKmValueParameter(0, setterParameter)))
            : new ArrayList<>();

    property.setterParameter =
        setterParameter != null ? convertKmValueParameter(0, setterParameter) : null;

    property.typeParameters =
        kmProperty.getTypeParameters().stream()
            .map(KotlinMetadataInitializer::convertKmTypeParameter)
            .collect(Collectors.toList());

    property.backingFieldSignature =
        convertJvmFieldSignature(JvmExtensionsKt.getFieldSignature(kmProperty));
    property.getterSignature =
        convertJvmMethodSignature(JvmExtensionsKt.getGetterSignature(kmProperty));
    property.setterSignature =
        convertJvmMethodSignature(JvmExtensionsKt.getSetterSignature(kmProperty));

    property.flags.isMovedFromInterfaceCompanion =
        JvmAttributes.isMovedFromInterfaceCompanion(kmProperty);

    property.syntheticMethodForAnnotations =
        convertJvmMethodSignature(JvmExtensionsKt.getSyntheticMethodForAnnotations(kmProperty));
    property.syntheticMethodForDelegate =
        convertJvmMethodSignature(JvmExtensionsKt.getSyntheticMethodForDelegate(kmProperty));

    return property;
  }

  private static KotlinTypeMetadata convertKmType(KmType kmType) {
    return convertKmType(kmType, null);
  }

  private static KotlinTypeMetadata convertKmType(KmType kmType, KmVariance kmVariance) {
    if (kmType == null) {
      return null;
    }

    KotlinTypeMetadata type =
        new KotlinTypeMetadata(convertTypeFlags(kmType), convertKmVariance(kmVariance));

    type.abbreviation = convertKmType(kmType.getAbbreviatedType(), null);

    if (kmType.getClassifier() instanceof KmClassifier.Class) {
      KmClassifier.Class classifier = (KmClassifier.Class) kmType.getClassifier();
      String className = classifier.getName();
      // Fix this simple case of corrupted metadata.
      if (ClassUtil.isInternalClassType(className)) {
        className = ClassUtil.internalClassNameFromClassType(className);
      }

      // Transform the class name to a valid Java name.
      // Must be changed back in KotlinMetadataWriter.
      if (className.startsWith(".")) {
        className = className.substring(1);
      }

      className =
          className.replace(
              KotlinConstants.INNER_CLASS_SEPARATOR, TypeConstants.INNER_CLASS_SEPARATOR);

      type.className = className;
    } else if (kmType.getClassifier() instanceof KmClassifier.TypeParameter) {
      KmClassifier.TypeParameter classifier = (KmClassifier.TypeParameter) kmType.getClassifier();
      type.typeParamID = classifier.getId();
    } else if (kmType.getClassifier() instanceof KmClassifier.TypeAlias) {
      // Note that all types are expanded for metadata produced
      // by the Kotlin compiler, so the type with a type alias
      // classifier may only appear in a call to [visitAbbreviatedType].
      KmClassifier.TypeAlias classifier = (KmClassifier.TypeAlias) kmType.getClassifier();
      type.aliasName = classifier.getName();
    }

    // Outer class type example:
    //
    //      class A<T> { inner class B<U> }
    //
    //      fun foo(a: A<*>.B<Byte?>) {}
    //
    //  The type of the `foo`'s parameter in the metadata is `B<Byte>` (a type whose classifier is
    // class `B`, and it has one type argument,
    //  type `Byte?`), and its outer type is `A<*>` (a type whose classifier is class `A`, and it
    // has one type argument, star projection).
    type.outerClassType = convertKmType(kmType.getOuterType());

    // For example, in `MutableMap<in String?, *>`, `in String?` is the type projection which is the
    // first type argument of the type.
    type.typeArguments =
        kmType.getArguments().stream()
            .map(KotlinMetadataInitializer::convertKmTypeProjection)
            .collect(Collectors.toList());

    //  Flexible types in Kotlin include platform types in Kotlin/JVM and `dynamic` type in
    // Kotlin/JS.

    KmFlexibleTypeUpperBound flexibleTypeUpperBound = kmType.getFlexibleTypeUpperBound();
    if (flexibleTypeUpperBound != null) {
      // typeFlexibilityId id of the kind of flexibility this type has. For example,
      // "kotlin.jvm.PlatformType" for JVM platform types,
      // or "kotlin.DynamicType" for JS dynamic type, may be null.
      type.flexibilityID = flexibleTypeUpperBound.getTypeFlexibilityId();
      type.upperBounds =
          new ArrayList<>(
              Collections.singletonList(convertKmType(flexibleTypeUpperBound.getType())));
    } else {
      type.upperBounds = new ArrayList<>();
    }

    type.isRaw = JvmExtensionsKt.isRaw(kmType);

    type.annotations =
        JvmExtensionsKt.getAnnotations(kmType).stream()
            .map(KotlinAnnotationUtilKt::convertAnnotation)
            .collect(Collectors.toList());

    return type;
  }

  private static KotlinTypeMetadata convertKmTypeProjection(KmTypeProjection kmTypeProjection) {
    if (kmTypeProjection == KmTypeProjection.STAR) {
      return KotlinTypeMetadata.starProjection();
    } else {
      return convertKmType(kmTypeProjection.getType(), kmTypeProjection.getVariance());
    }
  }

  private static KotlinTypeParameterMetadata convertKmTypeParameter(
      KmTypeParameter kmTypeParameter) {
    KotlinTypeParameterMetadata kotlinTypeParameterMetadata =
        new KotlinTypeParameterMetadata(
            convertTypeParameterFlags(kmTypeParameter),
            kmTypeParameter.getName(),
            kmTypeParameter.getId(),
            convertKmVariance(kmTypeParameter.getVariance()));

    kotlinTypeParameterMetadata.upperBounds =
        kmTypeParameter.getUpperBounds().stream()
            .map(KotlinMetadataInitializer::convertKmType)
            .collect(Collectors.toList());

    kotlinTypeParameterMetadata.annotations =
        JvmExtensionsKt.getAnnotations(kmTypeParameter).stream()
            .map(KotlinAnnotationUtilKt::convertAnnotation)
            .collect(Collectors.toList());

    return kotlinTypeParameterMetadata;
  }

  private static KotlinConstructorMetadata convertKmConstructor(
      boolean isAnnotationClass, KmConstructor kmConstructor) {
    KotlinConstructorMetadata constructor =
        new KotlinConstructorMetadata(convertConstructorFlags(kmConstructor));

    List<KmValueParameter> valueParameters = kmConstructor.getValueParameters();
    constructor.valueParameters = new ArrayList<>(valueParameters.size());
    for (int i = 0; i < valueParameters.size(); i++) {
      constructor.valueParameters.add(convertKmValueParameter(i, valueParameters.get(i)));
    }

    constructor.versionRequirement =
        convertKmVersionRequirement(kmConstructor.getVersionRequirements());

    if (!isAnnotationClass) {
      // For annotation classes, the metadata will have a JVM signature for a constructor,
      // while this is impossible to correspond to a real constructor. We set the jvmSignature
      // to null in this case.
      constructor.jvmSignature =
          convertJvmMethodSignature(JvmExtensionsKt.getSignature(kmConstructor));
    }

    return constructor;
  }

  private static KotlinValueParameterMetadata convertKmValueParameter(
      int index, KmValueParameter kmValueParameter) {
    KotlinValueParameterMetadata valueParameterMetadata =
        new KotlinValueParameterMetadata(
            convertValueParameterFlags(kmValueParameter), index, kmValueParameter.getName());

    valueParameterMetadata.type = convertKmType(kmValueParameter.getType());
    valueParameterMetadata.varArgElementType =
        convertKmType(kmValueParameter.getVarargElementType());

    return valueParameterMetadata;
  }

  private static KotlinVersionRequirementMetadata convertKmVersionRequirement(
      List<KmVersionRequirement> kmVersionRequirement) {
    List<KotlinVersionRequirementMetadata> versionRequirementMetadata =
        kmVersionRequirement.stream()
            .map(KotlinMetadataInitializer::convertKmVersionRequirement)
            .collect(Collectors.toList());

    if (versionRequirementMetadata.size() > 1) {
      // TODO: There can be multiple version requirements; previously we didn't handle it
      //       and we would have used the last visited, since each visit would overwrite the
      // previous.
      return versionRequirementMetadata.get(versionRequirementMetadata.size() - 1);
    } else if (versionRequirementMetadata.size() == 1) {
      return versionRequirementMetadata.get(0);
    }

    return null;
  }

  private static KotlinVersionRequirementMetadata convertKmVersionRequirement(
      KmVersionRequirement kmVersionRequirement) {
    KotlinVersionRequirementMetadata versionReq = new KotlinVersionRequirementMetadata();

    switch (kmVersionRequirement.kind) {
      case API_VERSION:
        versionReq.kind = KotlinVersionRequirementVersionKind.API_VERSION;
        break;
      case COMPILER_VERSION:
        versionReq.kind = KotlinVersionRequirementVersionKind.COMPILER_VERSION;
        break;
      case LANGUAGE_VERSION:
        versionReq.kind = KotlinVersionRequirementVersionKind.LANGUAGE_VERSION;
        break;
      case UNKNOWN:
        versionReq.kind = KotlinVersionRequirementVersionKind.UNKNOWN;
        break;
      default:
        throw new UnsupportedOperationException(
            "Encountered unknown enum value for KmVersionRequirementVersionKind.");
    }

    switch (kmVersionRequirement.level) {
      case ERROR:
        versionReq.level = KotlinVersionRequirementLevel.ERROR;
        break;
      case HIDDEN:
        versionReq.level = KotlinVersionRequirementLevel.HIDDEN;
        break;
      case WARNING:
        versionReq.level = KotlinVersionRequirementLevel.WARNING;
        break;
      default:
        throw new UnsupportedOperationException(
            "Encountered unknown enum value for KmVersionRequirementLevel.");
    }

    versionReq.errorCode = kmVersionRequirement.getErrorCode();
    versionReq.message = kmVersionRequirement.getMessage();

    versionReq.major = kmVersionRequirement.version.getMajor();
    versionReq.minor = kmVersionRequirement.version.getMinor();
    versionReq.patch = kmVersionRequirement.version.getPatch();

    return versionReq;
  }

  private static MethodSignature convertJvmMethodSignature(JvmMethodSignature jvmMethodSignature) {
    if (jvmMethodSignature == null) {
      return null;
    }

    try {
      return new MethodSignature(
          null, jvmMethodSignature.getName(), jvmMethodSignature.getDescriptor());
    } catch (Exception e) {
      return null;
    }
  }

  private static FieldSignature convertJvmFieldSignature(JvmFieldSignature jvmFieldSignature) {
    if (jvmFieldSignature == null) {
      return null;
    }

    return new FieldSignature(null, jvmFieldSignature.getName(), jvmFieldSignature.getDescriptor());
  }

  private static KotlinTypeVariance convertKmVariance(KmVariance variance) {
    if (variance == null) {
      return null;
    }

    switch (variance) {
      case IN:
        return KotlinTypeVariance.IN;
      case INVARIANT:
        return KotlinTypeVariance.INVARIANT;
      case OUT:
        return KotlinTypeVariance.OUT;
      default:
        throw new UnsupportedOperationException("Encountered unknown enum value for KmVariance.");
    }
  }

  private static KotlinEffectType convertKmEffectType(KmEffectType effectType) {
    switch (effectType) {
      case CALLS:
        return KotlinEffectType.CALLS;
      case RETURNS_CONSTANT:
        return KotlinEffectType.RETURNS_CONSTANT;
      case RETURNS_NOT_NULL:
        return KotlinEffectType.RETURNS_NOT_NULL;
      default:
        throw new UnsupportedOperationException("Encountered unknown enum value for KmEffectType.");
    }
  }

  private static KotlinEffectInvocationKind convertKmEffectInvocationKind(
      KmEffectInvocationKind invocationKind) {
    if (invocationKind == null) {
      return null;
    }
    switch (invocationKind) {
      case AT_MOST_ONCE:
        return KotlinEffectInvocationKind.AT_MOST_ONCE;
      case EXACTLY_ONCE:
        return KotlinEffectInvocationKind.EXACTLY_ONCE;
      case AT_LEAST_ONCE:
        return KotlinEffectInvocationKind.AT_LEAST_ONCE;
      default:
        throw new UnsupportedOperationException(
            "Encountered unknown enum value for KmEffectInvocationKind.");
    }
  }

  // Flag conversion methods.

  private static KotlinVisibilityFlags convertVisibilityFlags(Visibility visibility) {
    KotlinVisibilityFlags flags = new KotlinVisibilityFlags();

    flags.isInternal = visibility == Visibility.INTERNAL;
    flags.isLocal = visibility == Visibility.LOCAL;
    flags.isPrivate = visibility == Visibility.PRIVATE;
    flags.isProtected = visibility == Visibility.PROTECTED;
    flags.isPublic = visibility == Visibility.PUBLIC;
    flags.isPrivateToThis = visibility == Visibility.PRIVATE_TO_THIS;

    return flags;
  }

  private static KotlinModalityFlags convertModalityFlags(Modality modality) {
    KotlinModalityFlags flags = new KotlinModalityFlags();

    flags.isAbstract = modality == Modality.ABSTRACT;
    flags.isFinal = modality == Modality.FINAL;
    flags.isOpen = modality == Modality.OPEN;
    flags.isSealed = modality == Modality.SEALED;

    return flags;
  }

  private static KotlinClassFlags convertClassFlags(KmClass kmClass) {
    KotlinClassFlags flags =
        new KotlinClassFlags(
            convertVisibilityFlags(Attributes.getVisibility(kmClass)),
            convertModalityFlags(Attributes.getModality(kmClass)));

    flags.isUsualClass = Attributes.getKind(kmClass) == ClassKind.CLASS;
    flags.isInterface = Attributes.getKind(kmClass) == ClassKind.INTERFACE;
    flags.isEnumClass = Attributes.getKind(kmClass) == ClassKind.ENUM_CLASS;
    flags.isEnumEntry = Attributes.getKind(kmClass) == ClassKind.ENUM_ENTRY;
    flags.isAnnotationClass = Attributes.getKind(kmClass) == ClassKind.ANNOTATION_CLASS;
    flags.isObject = Attributes.getKind(kmClass) == ClassKind.OBJECT;
    flags.isCompanionObject = Attributes.getKind(kmClass) == ClassKind.COMPANION_OBJECT;
    flags.hasAnnotations = Attributes.getHasAnnotations(kmClass);
    flags.isInner = Attributes.isInner(kmClass);
    flags.isData = Attributes.isData(kmClass);
    flags.isExternal = Attributes.isExternal(kmClass);
    flags.isExpect = Attributes.isExpect(kmClass);
    flags.isValue = Attributes.isValue(kmClass);
    flags.isFun = Attributes.isFunInterface(kmClass);

    return flags;
  }

  private static KotlinConstructorFlags convertConstructorFlags(KmConstructor kmConstructor) {
    KotlinConstructorFlags flags =
        new KotlinConstructorFlags(convertVisibilityFlags(Attributes.getVisibility(kmConstructor)));

    flags.hasAnnotations = Attributes.getHasAnnotations(kmConstructor);
    flags.hasNonStableParameterNames = Attributes.getHasNonStableParameterNames(kmConstructor);
    flags.isSecondary = Attributes.isSecondary(kmConstructor);

    return flags;
  }

  private static KotlinFunctionFlags convertFunctionFlags(KmFunction kmFunction) {

    KotlinFunctionFlags flags =
        new KotlinFunctionFlags(
            convertVisibilityFlags(Attributes.getVisibility(kmFunction)),
            convertModalityFlags(Attributes.getModality(kmFunction)));

    flags.isDeclaration = Attributes.getKind(kmFunction) == MemberKind.DECLARATION;
    flags.isFakeOverride = Attributes.getKind(kmFunction) == MemberKind.FAKE_OVERRIDE;
    flags.isDelegation = Attributes.getKind(kmFunction) == MemberKind.DELEGATION;
    flags.isSynthesized = Attributes.getKind(kmFunction) == MemberKind.SYNTHESIZED;
    flags.hasAnnotations = Attributes.getHasAnnotations(kmFunction);
    flags.isOperator = Attributes.isOperator(kmFunction);
    flags.isInfix = Attributes.isInfix(kmFunction);
    flags.isInline = Attributes.isInline(kmFunction);
    flags.isTailrec = Attributes.isTailrec(kmFunction);
    flags.isExternal = Attributes.isExternal(kmFunction);
    flags.isSuspend = Attributes.isSuspend(kmFunction);
    flags.isExpect = Attributes.isExpect(kmFunction);

    return flags;
  }

  private static KotlinTypeFlags convertTypeFlags(KmType kmType) {
    KotlinTypeFlags flags = new KotlinTypeFlags();

    flags.isNullable = Attributes.isNullable(kmType);
    flags.isSuspend = Attributes.isSuspend(kmType);
    flags.isDefinitelyNonNull = Attributes.isDefinitelyNonNull(kmType);

    return flags;
  }

  private static KotlinTypeParameterFlags convertTypeParameterFlags(
      KmTypeParameter kmTypeParameter) {
    KotlinTypeParameterFlags flags = new KotlinTypeParameterFlags();
    flags.isReified = Attributes.isReified(kmTypeParameter);

    return flags;
  }

  private static KotlinTypeAliasFlags convertTypeAliasFlags(KmTypeAlias kmTypeAlias) {
    KotlinTypeAliasFlags flags =
        new KotlinTypeAliasFlags(convertVisibilityFlags(Attributes.getVisibility(kmTypeAlias)));

    flags.hasAnnotations = Attributes.getHasAnnotations(kmTypeAlias);

    return flags;
  }

  private static KotlinPropertyFlags convertPropertyFlags(KmProperty kmProperty) {

    KotlinPropertyFlags flags =
        new KotlinPropertyFlags(
            convertVisibilityFlags(Attributes.getVisibility(kmProperty)),
            convertModalityFlags(Attributes.getModality(kmProperty)));

    flags.isDeclared = Attributes.getKind(kmProperty) == MemberKind.DECLARATION;
    flags.isFakeOverride = Attributes.getKind(kmProperty) == MemberKind.FAKE_OVERRIDE;
    flags.isDelegation = Attributes.getKind(kmProperty) == MemberKind.DELEGATION;
    flags.isSynthesized = Attributes.getKind(kmProperty) == MemberKind.SYNTHESIZED;
    flags.hasAnnotations = Attributes.getHasAnnotations(kmProperty);
    flags.isVar = Attributes.isVar(kmProperty);
    flags.isConst = Attributes.isConst(kmProperty);
    flags.isLateinit = Attributes.isLateinit(kmProperty);
    flags.hasConstant = Attributes.getHasConstant(kmProperty);
    flags.isExternal = Attributes.isExternal(kmProperty);
    flags.isDelegated = Attributes.isDelegated(kmProperty);
    flags.isExpect = Attributes.isExpect(kmProperty);

    return flags;
  }

  private static KotlinPropertyAccessorFlags convertPropertyAccessorFlags(
      KmPropertyAccessorAttributes kmPropertyAccessorAttributes) {
    KotlinPropertyAccessorFlags flags =
        new KotlinPropertyAccessorFlags(
            convertVisibilityFlags(Attributes.getVisibility(kmPropertyAccessorAttributes)),
            convertModalityFlags(Attributes.getModality(kmPropertyAccessorAttributes)));

    flags.hasAnnotations = Attributes.getHasAnnotations(kmPropertyAccessorAttributes);
    flags.isDefault = !Attributes.isNotDefault(kmPropertyAccessorAttributes);
    flags.isExternal = Attributes.isExternal(kmPropertyAccessorAttributes);
    flags.isInline = Attributes.isInline(kmPropertyAccessorAttributes);

    return flags;
  }

  private static KotlinValueParameterFlags convertValueParameterFlags(
      KmValueParameter kmValueParameter) {

    KotlinValueParameterFlags flags = new KotlinValueParameterFlags();

    flags.hasAnnotations = Attributes.getHasAnnotations(kmValueParameter);
    flags.hasDefaultValue = Attributes.getDeclaresDefaultValue(kmValueParameter);
    flags.isCrossInline = Attributes.isCrossinline(kmValueParameter);
    flags.isNoInline = Attributes.isNoinline(kmValueParameter);

    return flags;
  }

  private static KotlinEffectExpressionFlags convertEffectExpressionFlags(
      KmEffectExpression kmEffectExpression) {
    KotlinEffectExpressionFlags flags = new KotlinEffectExpressionFlags();

    flags.isNullCheckPredicate = Attributes.isNullCheckPredicate(kmEffectExpression);
    flags.isNegated = Attributes.isNegated(kmEffectExpression);

    return flags;
  }

  // Helper methods.

  private static void populateFromKmPackage(
      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata, KmPackage kmPackage) {
    kotlinDeclarationContainerMetadata.functions =
        kmPackage.getFunctions().stream()
            .map(KotlinMetadataInitializer::convertKmFunction)
            .collect(Collectors.toList());

    kotlinDeclarationContainerMetadata.typeAliases =
        kmPackage.getTypeAliases().stream()
            .map(KotlinMetadataInitializer::convertKmTypeAlias)
            .collect(Collectors.toList());

    kotlinDeclarationContainerMetadata.properties =
        kmPackage.getProperties().stream()
            .map(KotlinMetadataInitializer::convertKmProperty)
            .collect(Collectors.toList());

    kotlinDeclarationContainerMetadata.localDelegatedProperties =
        JvmExtensionsKt.getLocalDelegatedProperties(kmPackage).stream()
            .map(KotlinMetadataInitializer::convertKmProperty)
            .collect(Collectors.toList());
  }

  public static boolean isSupportedMetadataVersion(KotlinMetadataVersion mv) {
    return mv.major == 1 && mv.minor >= 4 || mv.major == 2;
  }

  public static boolean isValidKotlinMetadataAnnotationField(String name) {
    switch (name) {
      case KOTLIN_METADATA_FIELD_K:
      case KOTLIN_METADATA_FIELD_BV:
      case KOTLIN_METADATA_FIELD_MV:
      case KOTLIN_METADATA_FIELD_D1:
      case KOTLIN_METADATA_FIELD_D2:
      case KOTLIN_METADATA_FIELD_XI:
      case KOLTIN_METADATA_FIELD_XS:
      case KOTLIN_METADATA_FIELD_PN:
        return true;
      default:
        return false;
    }
  }

  public static KotlinMetadataType metadataTypeOf(String name) {
    switch (name) {
      case KOTLIN_METADATA_FIELD_K:
        return KotlinMetadataType.k;
      case KOTLIN_METADATA_FIELD_BV:
        return KotlinMetadataType.bv;
      case KOTLIN_METADATA_FIELD_MV:
        return KotlinMetadataType.mv;
      case KOTLIN_METADATA_FIELD_D1:
        return KotlinMetadataType.d1;
      case KOTLIN_METADATA_FIELD_D2:
        return KotlinMetadataType.d2;
      case KOTLIN_METADATA_FIELD_XI:
        return KotlinMetadataType.xi;
      case KOLTIN_METADATA_FIELD_XS:
        return KotlinMetadataType.xs;
      case KOTLIN_METADATA_FIELD_PN:
        return KotlinMetadataType.pn;
      default:
        throw new IllegalArgumentException("Unknown Kotlin metadata field '" + name + "'");
    }
  }
}
