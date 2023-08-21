/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

import kotlin.Metadata;
import kotlinx.metadata.Flag;
import kotlinx.metadata.InconsistentKotlinMetadataException;
import kotlinx.metadata.KmClass;
import kotlinx.metadata.KmClassifier;
import kotlinx.metadata.KmConstructor;
import kotlinx.metadata.KmContract;
import kotlinx.metadata.KmEffect;
import kotlinx.metadata.KmEffectExpression;
import kotlinx.metadata.KmEffectInvocationKind;
import kotlinx.metadata.KmEffectType;
import kotlinx.metadata.KmFlexibleTypeUpperBound;
import kotlinx.metadata.KmFunction;
import kotlinx.metadata.KmPackage;
import kotlinx.metadata.KmProperty;
import kotlinx.metadata.KmType;
import kotlinx.metadata.KmTypeAlias;
import kotlinx.metadata.KmTypeParameter;
import kotlinx.metadata.KmTypeProjection;
import kotlinx.metadata.KmValueParameter;
import kotlinx.metadata.KmVariance;
import kotlinx.metadata.KmVersionRequirement;
import kotlinx.metadata.KmVersionRequirementLevel;
import kotlinx.metadata.KmVersionRequirementVersionKind;
import kotlinx.metadata.internal.metadata.jvm.deserialization.JvmMetadataVersion;
import kotlinx.metadata.jvm.JvmExtensionsKt;
import kotlinx.metadata.jvm.JvmFieldSignature;
import kotlinx.metadata.jvm.JvmFlag;
import kotlinx.metadata.jvm.JvmMetadataUtil;
import kotlinx.metadata.jvm.JvmMethodSignature;
import kotlinx.metadata.jvm.KotlinClassMetadata;
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
import proguard.classfile.kotlin.flags.KotlinCommonFlags;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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

/**
 * Initializes the kotlin metadata for a Kotlin class.
 * <p>
 * Provides two APIs:
 * <p>
 * - Visitor: use as a ClassVisitor or AnnotationVisitor to initialize the Kotlin metadata
 *            contain within a {@link kotlin.Metadata} annotation.
 *            After initialization, all info from the annotation is represented in the {@link Clazz}'s
 *            {@link ProgramClass#kotlinMetadata} field.
 * <p>
 *            Note: only applicable for {@link ProgramClass}.
 * <p>
 * - `initialize`: provide the {@link Clazz} and {@link kotlin.Metadata} field values
 *                 to the {@link KotlinMetadataInitializer#initialize(Clazz, int, int[], String[], String[], int, String, String)} method
 *                 to initialize Kotlin metadata for the given {@link Clazz}.
 */
public class KotlinMetadataInitializer
implements ClassVisitor,
           AnnotationVisitor,

           // Implementation interfaces.
           ElementValueVisitor,
           ConstantVisitor
{
    // For original definitions see https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/jvm/runtime/kotlin/Metadata.kt
    private int      k;
    private int[]    mv;
    private String[] d1;
    private String[] d2;
    private int      xi;
    private String   xs;
    private String   pn;

    public static final KotlinMetadataVersion MAX_SUPPORTED_VERSION;

    static {
        int[] version = JvmMetadataVersion.INSTANCE.toArray();
        MAX_SUPPORTED_VERSION = new KotlinMetadataVersion(version[0], version[1] + 1);
    }

    // For Constant visiting
    private MetadataType currentType;

    private final BiConsumer<Clazz, String> errorHandler;

    public KotlinMetadataInitializer(WarningPrinter warningPrinter)
    {
        this((clazz, message) -> warningPrinter.print(clazz.getName(), message));
    }

    public KotlinMetadataInitializer(BiConsumer<Clazz, String> errorHandler)
    {
        this.errorHandler = errorHandler;
    }

    // Implementations for ClassVisitor
    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitLibraryClass(LibraryClass libraryClass) {
        // LibraryClass models do not contain constant pools, attributes, so
        // they cannot be initialized by a visitor.
        // They should be initialized instead with the `initialize` method.
    }


    @Override
    public void visitProgramClass(ProgramClass clazz)
    {
        clazz.accept(
                new AllAttributeVisitor(
                new AttributeNameFilter(RUNTIME_VISIBLE_ANNOTATIONS,
                new AllAnnotationVisitor(
                new AnnotationTypeFilter(TYPE_KOTLIN_METADATA,
                        this)))));
    }


    // Implementations for AnnotationVisitor.
    @Override
    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        // Collect the metadata.
        this.k  = -1;
        this.mv = null; //new int[] { 1, 0, 0 };
        this.d1 = null; //new String[0];
        this.d2 = null; //new String[0];
        this.xi = 0; // Optional flags, the `xi` annotation field may not be present so default to none set.
        this.xs = null;
        this.pn = null;

        try
        {
            annotation.elementValuesAccept(clazz, this);
        }
        catch (Exception e)
        {
            this.errorHandler.accept(clazz,
                                     "Encountered corrupt Kotlin metadata in class " +
                                     clazz.getName() +
                                     ". The metadata for this class will not be processed (" + e.getMessage() + ")");
            clazz.accept(new SimpleKotlinMetadataSetter(new UnsupportedKotlinMetadata(k, mv, xi, xs, pn)));
            return;
        }

        initialize(clazz, k, mv, d1, d2, xi, xs, pn);
    }

    /**
     * Initialize Kotlin metadata for a given {@link Clazz}.
     *
     * @param clazz The {@link ProgramClass} or {@link LibraryClass}.
     * @param k
     * @param mv
     * @param d1
     * @param d2
     * @param xi
     * @param xs
     * @param pn
     */
    public void initialize(Clazz clazz, int k, int[] mv, String[] d1, String[] d2, int xi, String xs, String pn)
    {
        // Parse the collected metadata.
        Metadata metadata = JvmMetadataUtil.Metadata(k, mv, d1, d2, xs, pn, xi);
        KotlinClassMetadata md = KotlinClassMetadata.read(metadata);
        if (md == null)
        {
            String version = mv == null ? "unknown" : Arrays.stream(mv).mapToObj(Integer::toString).collect(joining("."));
            this.errorHandler.accept(clazz, "Encountered corrupt @kotlin/Metadata for class " + clazz.getName() + " (version " + version + ").");
            clazz.accept(new SimpleKotlinMetadataSetter(new UnsupportedKotlinMetadata(k, mv, xi, xs, pn)));
            return;
        }

        try
        {
            switch (k)
            {
                case METADATA_KIND_CLASS:
                    KotlinClassKindMetadata kotlinClassKindMetadata = toKotlinClassKindMetadata(md);
                    kotlinClassKindMetadata.ownerClassName = clazz.getName();
                    clazz.accept(new SimpleKotlinMetadataSetter(kotlinClassKindMetadata));
                    break;

                case METADATA_KIND_FILE_FACADE: // For package level functions/properties
                    KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata = toKotlinFileFacadeKindMetadata(md);

                    kotlinFileFacadeKindMetadata.ownerClassName = clazz.getName();
                    clazz.accept(new SimpleKotlinMetadataSetter(kotlinFileFacadeKindMetadata));
                    break;

                case METADATA_KIND_SYNTHETIC_CLASS:
                    KotlinSyntheticClassKindMetadata.Flavor flavor;

                    KotlinClassMetadata.SyntheticClass smd = ((KotlinClassMetadata.SyntheticClass)md);

                    if (smd.isLambda())
                    {
                        flavor = KotlinSyntheticClassKindMetadata.Flavor.LAMBDA;
                    }
                    else if (clazz.getName().endsWith(DEFAULT_IMPLEMENTATIONS_SUFFIX))
                    {
                        flavor = KotlinSyntheticClassKindMetadata.Flavor.DEFAULT_IMPLS;
                    }
                    else if (clazz.getName().endsWith(WHEN_MAPPINGS_SUFFIX))
                    {
                        flavor = KotlinSyntheticClassKindMetadata.Flavor.WHEN_MAPPINGS;
                    }
                    else
                    {
                        flavor = KotlinSyntheticClassKindMetadata.Flavor.REGULAR;
                    }

                    KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata =
                        new KotlinSyntheticClassKindMetadata(mv, xi, xs, pn, flavor);

                    if (smd.isLambda())
                    {
                        // Only lambdas contain exactly 1 function.
                        kotlinSyntheticClassKindMetadata.functions = new ArrayList<>(1);
                        kotlinSyntheticClassKindMetadata.functions.add(
                            toKotlinFunctionMetadata(Objects.requireNonNull(smd.toKmLambda()).getFunction())
                        );
                    }
                    else
                    {
                        // Other synthetic classes never contain any functions.
                        kotlinSyntheticClassKindMetadata.functions = Collections.emptyList();
                    }

                    clazz.accept(new SimpleKotlinMetadataSetter(kotlinSyntheticClassKindMetadata));
                    break;

                case METADATA_KIND_MULTI_FILE_CLASS_FACADE:
                    // The relevant data for this kind is in d1. It is a list of Strings
                    // representing the part class names.
                    clazz.accept(new SimpleKotlinMetadataSetter(
                        new KotlinMultiFileFacadeKindMetadata(mv, d1, xi, xs, pn)));
                    break;

                case METADATA_KIND_MULTI_FILE_CLASS_PART:
                    KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata =
                        toKotlinMultiFilePartKindMetadata(md);

                    kotlinMultiFilePartKindMetadata.ownerClassName = clazz.getName();
                    clazz.accept(new SimpleKotlinMetadataSetter(kotlinMultiFilePartKindMetadata));
                    break;

                default:
                    // This happens when the library is outdated and a newer type of Kotlin class is passed.
                    this.errorHandler.accept(clazz,
                                             "Unknown Kotlin class kind in class " +
                                             clazz.getName() +
                                             ". The metadata for this class will not be processed.");
                    clazz.accept(new SimpleKotlinMetadataSetter(new UnsupportedKotlinMetadata(k, mv, xi, xs, pn)));
                    break;
            }
        }
        catch (InconsistentKotlinMetadataException e)
        {
            this.errorHandler.accept(clazz,
                                     "Encountered corrupt Kotlin metadata in class " +
                                     clazz.getName() +
                                     ". The metadata for this class will not be processed (" + e.getMessage() + ")");
            clazz.accept(new SimpleKotlinMetadataSetter(new UnsupportedKotlinMetadata(k, mv, xi, xs, pn)));
        }
    }


    // Implementations for ElementValueVisitor.
    @Override
    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
        this.currentType = metadataTypeOf(constantElementValue.getMethodName(clazz));
        clazz.constantPoolEntryAccept(constantElementValue.u2constantValueIndex, this);
    }

    @Override
    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        MetadataType arrayElementType = metadataTypeOf(arrayElementValue.getMethodName(clazz));
        switch (arrayElementType)
        {
            case mv: this.mv = new int   [arrayElementValue.u2elementValuesCount]; break;
            case d1: this.d1 = new String[arrayElementValue.u2elementValuesCount]; break;
            case d2: this.d2 = new String[arrayElementValue.u2elementValuesCount]; break;
        }

        arrayElementValue.elementValuesAccept(clazz,
                                              annotation,
                                              new ArrayElementValueCollector(arrayElementType));
    }


    // Implementations for ConstantVisitor
    @Override
    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        if (this.currentType == MetadataType.xs)
        {
            xs = utf8Constant.getString();
        }
        else if (this.currentType == MetadataType.pn)
        {
            pn = utf8Constant.getString();
        }
        else
        {
            throw new UnsupportedOperationException("Cannot store Utf8Constant in int");
        }
    }

    @Override
    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
    {
        if (this.currentType == MetadataType.k)
        {
            k = integerConstant.getValue();
        }
        else if (this.currentType == MetadataType.xi)
        {
            xi = integerConstant.getValue();
        }
        else
        {
            throw new UnsupportedOperationException("Cannot store Utf8Constant in int");
        }
    }


    private class ArrayElementValueCollector
    implements ElementValueVisitor,

               // Implementation interfaces.
               ConstantVisitor
    {
        private final MetadataType arrayType;
        private       int          index;

        ArrayElementValueCollector(MetadataType array)
        {
            this.arrayType = array;
            this.index = 0;
        }


        // Implementations for ElementValueVisitor
        @Override
        public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
        {
            clazz.constantPoolEntryAccept(constantElementValue.u2constantValueIndex, this);
        }


        // Implementations for ConstantVisitor
        @Override
        public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
        {
            if (this.arrayType == MetadataType.d1)
            {
                d1[index++] = utf8Constant.getString();
            }
            else if (this.arrayType == MetadataType.d2)
            {
                d2[index++] = utf8Constant.getString();
            }
            else
            {
                throw new UnsupportedOperationException("Cannot store UTF8Constant in int[]");
            }
        }

        @Override
        public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
        {
            if (this.arrayType == MetadataType.mv)
            {
                mv[index++] = integerConstant.getValue();
            }
            else if (this.arrayType == MetadataType.bv)
            {
                // Deprecated & removed from kotlinx.metadata library, do nothing.
            }
            else
            {
                throw new UnsupportedOperationException("Cannot store IntegerConstant in String[]");
            }
        }
    }


    public enum MetadataType
    {
        k, mv, d1, d2, xi, xs, pn,

        @Deprecated bv // was removed but older metadata will still contain it.
    }


    private static class SimpleKotlinMetadataSetter
    implements           ClassVisitor
    {
        private final KotlinMetadata kmd;

        SimpleKotlinMetadataSetter(KotlinMetadata kmd)
        {
            this.kmd = kmd;
        }


        @Override
        public void visitAnyClass(Clazz clazz)
        {
            throw new UnsupportedOperationException(this.getClass().getName() + " does not support " + clazz.getClass().getName());
        }


        @Override
        public void visitProgramClass(ProgramClass programClass)
        {
            programClass.kotlinMetadata = kmd;
        }


        @Override
        public void visitLibraryClass(LibraryClass libraryClass)
        {
            libraryClass.kotlinMetadata = kmd;
        }
    }

    /**
     * Convert a {@link KotlinClassMetadata} to an internal {@link KotlinClassKindMetadata} model.
     */
    private KotlinClassKindMetadata toKotlinClassKindMetadata(KotlinClassMetadata md)
    {
        KotlinClassKindMetadata kotlinClassKindMetadata =
            new KotlinClassKindMetadata(
                md.getAnnotationData().mv(),
                md.getAnnotationData().xi(),
                md.getAnnotationData().xs(),
                md.getAnnotationData().pn()
            );

        KotlinClassMetadata.Class classMetadata = (KotlinClassMetadata.Class) md;
        KmClass                   kmClass       = classMetadata.toKmClass();

        String className = kmClass.getName();
        if (className.startsWith("."))
        {
            // If the class has a "local class name", the passed String starts with a dot. This appears to be safe to ignore
            className = className.substring(1);
        }

        // Inner classes are marked with a dot after the enclosing class instead
        // of '$' (only here, not in the actual d2 array).
        className = className.replace('.', '$');

        kotlinClassKindMetadata.className = className;
        kotlinClassKindMetadata.flags     = convertClassFlags(kmClass.getFlags());

        kotlinClassKindMetadata.companionObjectName    = kmClass.getCompanionObject();
        kotlinClassKindMetadata.underlyingPropertyName = kmClass.getInlineClassUnderlyingPropertyName();
        kotlinClassKindMetadata.underlyingPropertyType = toKotlinTypeMetadata(kmClass.getInlineClassUnderlyingType());
        kotlinClassKindMetadata.enumEntryNames         = kmClass.getEnumEntries();
        kotlinClassKindMetadata.nestedClassNames       = kmClass.getNestedClasses();
        kotlinClassKindMetadata.sealedSubclassNames    = kmClass.getSealedSubclasses()
                                                                .stream().map(it -> it.replace(".","$"))
                                                                .collect(Collectors.toList());

        kotlinClassKindMetadata.versionRequirement = toKotlinVersionRequirementMetadataFromList(kmClass.getVersionRequirements());

        kotlinClassKindMetadata.typeParameters = kmClass
                .getTypeParameters()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeParameterMetadata)
                .collect(Collectors.toList());

        kotlinClassKindMetadata.contextReceivers = kmClass
                .getContextReceiverTypes()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeMetadata)
                .collect(Collectors.toList());

        kotlinClassKindMetadata.superTypes = kmClass
                .getSupertypes()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeMetadata)
                .collect(Collectors.toList());

        kotlinClassKindMetadata.constructors = kmClass
                .getConstructors()
                .stream()
                .map(it -> toKotlinConstructorMetadata(md.getAnnotationData().mv(), kotlinClassKindMetadata.flags.isAnnotationClass, it))
                .collect(Collectors.toList());

        kotlinClassKindMetadata.functions = kmClass
                .getFunctions()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinFunctionMetadata)
                .collect(Collectors.toList());

        kotlinClassKindMetadata.properties = kmClass
                .getProperties()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinPropertyMetadata)
                .collect(Collectors.toList());

        // Currently only top-level typeAlias declarations are allowed, so this
        // list should normally be empty, but you can disable the compiler
        // error and allow typeAlias declarations here with this annotation:
        // @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
        kotlinClassKindMetadata.typeAliases = kmClass
               .getTypeAliases()
               .stream()
               .map(KotlinMetadataInitializer::toKotlinTypeAliasMetadata)
               .collect(Collectors.toList());

        // JvmExtensions

        setClassJvmFlags(kotlinClassKindMetadata.flags, JvmExtensionsKt.getJvmFlags(kmClass));
        kotlinClassKindMetadata.anonymousObjectOriginName = JvmExtensionsKt.getAnonymousObjectOriginName(kmClass);
        kotlinClassKindMetadata.localDelegatedProperties  = JvmExtensionsKt.getLocalDelegatedProperties(kmClass)
                .stream()
                .map(KotlinMetadataInitializer::toKotlinPropertyMetadata)
                .collect(Collectors.toList());

        return kotlinClassKindMetadata;
    }

    private static KotlinFileFacadeKindMetadata toKotlinFileFacadeKindMetadata(KotlinClassMetadata md)
    {
        KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata = new KotlinFileFacadeKindMetadata(
            md.getAnnotationData().mv(),
            md.getAnnotationData().xi(),
            md.getAnnotationData().xs(),
            md.getAnnotationData().pn()
        );

        KotlinClassMetadata.FileFacade fileFacade = (KotlinClassMetadata.FileFacade) md;
        KmPackage kmPackage = fileFacade.toKmPackage();

        populateFromKmPackage(kotlinFileFacadeKindMetadata, kmPackage);

        return kotlinFileFacadeKindMetadata;
    }

    private static KotlinMultiFilePartKindMetadata toKotlinMultiFilePartKindMetadata(KotlinClassMetadata md)
    {
        KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata = new KotlinMultiFilePartKindMetadata(
            md.getAnnotationData().mv(),
            md.getAnnotationData().xi(),
            md.getAnnotationData().xs(),
            md.getAnnotationData().pn()
        );

        KotlinClassMetadata.MultiFileClassPart fileFacade = (KotlinClassMetadata.MultiFileClassPart) md;
        KmPackage kmPackage = fileFacade.toKmPackage();

        populateFromKmPackage(kotlinMultiFilePartKindMetadata, kmPackage);

        return kotlinMultiFilePartKindMetadata;
    }

    private static void populateFromKmPackage(KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata, KmPackage kmPackage)
    {
        kotlinDeclarationContainerMetadata.functions = kmPackage
                .getFunctions()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinFunctionMetadata)
                .collect(Collectors.toList());

        kotlinDeclarationContainerMetadata.typeAliases = kmPackage
                .getTypeAliases()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeAliasMetadata)
                .collect(Collectors.toList());

        kotlinDeclarationContainerMetadata.properties = kmPackage
                .getProperties()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinPropertyMetadata)
                .collect(Collectors.toList());

        kotlinDeclarationContainerMetadata.localDelegatedProperties = JvmExtensionsKt
                .getLocalDelegatedProperties(kmPackage)
                .stream()
                .map(KotlinMetadataInitializer::toKotlinPropertyMetadata)
                .collect(Collectors.toList());
    }

    private static KotlinFunctionMetadata toKotlinFunctionMetadata(KmFunction kmFunction)
    {
        KotlinFunctionMetadata kotlinFunctionMetadata = new KotlinFunctionMetadata(
            convertFunctionFlags(kmFunction.getFlags()),
            kmFunction.getName()
        );

        // TODO: We previously used a list, but there should be a single contract.
        kotlinFunctionMetadata.contracts = kmFunction.getContract() != null ?
                new ArrayList<>(Collections.singleton(toKotlinContractMetadata(kmFunction.getContract()))) :
                new ArrayList<>();

        kotlinFunctionMetadata.receiverType = toKotlinTypeMetadata(kmFunction.getReceiverParameterType());
        kotlinFunctionMetadata.contextReceivers = kmFunction
                .getContextReceiverTypes()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeMetadata)
                .collect(Collectors.toList());

        kotlinFunctionMetadata.returnType = toKotlinTypeMetadata(kmFunction.returnType);
        kotlinFunctionMetadata.typeParameters = kmFunction
                .getTypeParameters()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeParameterMetadata)
                .collect(Collectors.toList());

        List<KmValueParameter> valueParameters = kmFunction.getValueParameters();
        kotlinFunctionMetadata.valueParameters = new ArrayList<>(valueParameters.size());
        for (int i = 0; i < valueParameters.size(); i++)
        {
            kotlinFunctionMetadata.valueParameters.add(toKotlinValueParameterMetadata(i, valueParameters.get(i)));
        }

        kotlinFunctionMetadata.versionRequirement    = toKotlinVersionRequirementMetadataFromList(kmFunction.getVersionRequirements());
        kotlinFunctionMetadata.jvmSignature          = fromKotlinJvmMethodSignature(JvmExtensionsKt.getSignature(kmFunction));
        kotlinFunctionMetadata.lambdaClassOriginName = JvmExtensionsKt.getLambdaClassOriginName(kmFunction);

        return kotlinFunctionMetadata;
    }

    private static KotlinContractMetadata toKotlinContractMetadata(KmContract kmContract)
    {
        KotlinContractMetadata kotlinContractMetadata = new KotlinContractMetadata();

        kotlinContractMetadata.effects = kmContract
                .getEffects()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinEffectMetadata)
                .collect(Collectors.toList());

        return kotlinContractMetadata;
    }

    private static KotlinEffectMetadata toKotlinEffectMetadata(KmEffect kmEffect)
    {
        KotlinEffectMetadata effect = new KotlinEffectMetadata(
            fromKmEffectType(kmEffect.getType()),
            fromKmEffectInvocationKind(kmEffect.getInvocationKind())
        );

        effect.conclusionOfConditionalEffect = toKotlinEffectExpressionMetadata(kmEffect.getConclusion());
        effect.constructorArguments = kmEffect
                .getConstructorArguments()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinEffectExpressionMetadata)
                .collect(Collectors.toList());

        return effect;
    }

    private static KotlinEffectExpressionMetadata toKotlinEffectExpressionMetadata(KmEffectExpression kmEffectExpression)
    {
        if (kmEffectExpression == null) return null;

        KotlinEffectExpressionMetadata expressionMetadata = new KotlinEffectExpressionMetadata();

        expressionMetadata.flags = convertEffectExpressionFlags(kmEffectExpression.getFlags());

        if (kmEffectExpression.getParameterIndex() != null)
        {
            // Optional 1-based index of the value parameter of the function, for effects which assert something about
            // the function parameters. The index 0 means the extension receiver parameter. May be null
            expressionMetadata.parameterIndex = kmEffectExpression.getParameterIndex();
        }
        
        if (kmEffectExpression.getConstantValue() != null)
        {
            // The constant value used in the effect expression. May be `true`, `false` or `null`.
            expressionMetadata.hasConstantValue = true;
            expressionMetadata.constantValue = kmEffectExpression.getConstantValue().getValue();
        }

        if (kmEffectExpression.isInstanceType() != null)
        {
            expressionMetadata.typeOfIs = toKotlinTypeMetadata(kmEffectExpression.isInstanceType());
        }

        expressionMetadata.andRightHandSides = kmEffectExpression
                .getAndArguments()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinEffectExpressionMetadata)
                .collect(Collectors.toList());

        expressionMetadata.orRightHandSides = kmEffectExpression
                .getOrArguments()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinEffectExpressionMetadata)
                .collect(Collectors.toList());

        return expressionMetadata;
    }

    private static KotlinTypeAliasMetadata toKotlinTypeAliasMetadata(KmTypeAlias kmTypeAlias)
    {
        KotlinTypeAliasMetadata typeAlias = new KotlinTypeAliasMetadata(
            convertTypeAliasFlags(kmTypeAlias.getFlags()),
            kmTypeAlias.getName()
        );

        typeAlias.underlyingType     = toKotlinTypeMetadata(kmTypeAlias.getUnderlyingType());
        typeAlias.expandedType       = toKotlinTypeMetadata(kmTypeAlias.getExpandedType());
        typeAlias.versionRequirement = toKotlinVersionRequirementMetadataFromList(kmTypeAlias.getVersionRequirements());

        typeAlias.annotations = kmTypeAlias
                .getAnnotations()
                .stream()
                .map(KotlinAnnotationUtilKt::convertAnnotation)
                .collect(Collectors.toList());

        typeAlias.typeParameters = kmTypeAlias
                .getTypeParameters()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeParameterMetadata)
                .collect(Collectors.toList());

        return typeAlias;
    }

    private static KotlinPropertyMetadata toKotlinPropertyMetadata(KmProperty kmProperty)
    {
        KotlinPropertyMetadata property = new KotlinPropertyMetadata(
            convertPropertyFlags(kmProperty.getFlags()),
            kmProperty.getName(),
            convertPropertyAccessorFlags(kmProperty.getGetterFlags()),
            convertPropertyAccessorFlags(kmProperty.getSetterFlags())
        );

        property.receiverType = toKotlinTypeMetadata(kmProperty.getReceiverParameterType());

        property.contextReceivers = kmProperty
                .getContextReceiverTypes()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeMetadata)
                .collect(Collectors.toList());

        property.type = toKotlinTypeMetadata(kmProperty.returnType);
        property.versionRequirement = toKotlinVersionRequirementMetadataFromList(kmProperty.getVersionRequirements());

        KmValueParameter setterParameter = kmProperty.getSetterParameter();
        // TODO: There can only be one Setter parameter but previously our API used a list.
        // The name of the value parameter is like `"<set-?>"` for properties emitted by the Kotlin compiler.
        property.setterParameters = setterParameter != null ?
                new ArrayList<>(Collections.singletonList(toKotlinValueParameterMetadata(0, setterParameter))) :
                new ArrayList<>();

        property.typeParameters = kmProperty
                .getTypeParameters()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeParameterMetadata)
                .collect(Collectors.toList());

        property.backingFieldSignature = fromKotlinJvmFieldSignature(JvmExtensionsKt.getFieldSignature(kmProperty));
        property.getterSignature = fromKotlinJvmMethodSignature(JvmExtensionsKt.getGetterSignature(kmProperty));
        property.setterSignature = fromKotlinJvmMethodSignature(JvmExtensionsKt.getSetterSignature(kmProperty));

        setPropertyJvmFlags(property.flags, JvmExtensionsKt.getJvmFlags(kmProperty));

        property.syntheticMethodForAnnotations = fromKotlinJvmMethodSignature(JvmExtensionsKt.getSyntheticMethodForAnnotations(kmProperty));
        property.syntheticMethodForDelegate = fromKotlinJvmMethodSignature(JvmExtensionsKt.getSyntheticMethodForDelegate(kmProperty));

        return property;
    }

    private static KotlinTypeMetadata toKotlinTypeMetadata(KmType kmType)
    {
        return toKotlinTypeMetadata(kmType, null);
    }

    private static KotlinTypeMetadata toKotlinTypeMetadata(KmType kmType, KmVariance kmVariance)
    {
        if (kmType == null) return null;

        KotlinTypeMetadata type = new KotlinTypeMetadata(convertTypeFlags(kmType.getFlags()), fromKmVariance(kmVariance));

        type.abbreviation = toKotlinTypeMetadata(kmType.getAbbreviatedType(), null);
        
        if (kmType.getClassifier() instanceof KmClassifier.Class)
        {
            KmClassifier.Class classifier = (KmClassifier.Class)kmType.getClassifier();
            String className = classifier.getName();
              // Fix this simple case of corrupted metadata.
            if (ClassUtil.isInternalClassType(className))
            {
                className = ClassUtil.internalClassNameFromClassType(className);
            }

            // Transform the class name to a valid Java name.
            // Must be changed back in KotlinMetadataWriter.
            if (className.startsWith("."))
            {
                className = className.substring(1);
            }

            className =
                className.replace(KotlinConstants.INNER_CLASS_SEPARATOR,
                                  TypeConstants. INNER_CLASS_SEPARATOR);

            type.className = className;          
        }
        else if (kmType.getClassifier() instanceof KmClassifier.TypeParameter)
        {
            KmClassifier.TypeParameter classifier = (KmClassifier.TypeParameter)kmType.getClassifier();
            type.typeParamID = classifier.getId();
        }
        else if (kmType.getClassifier() instanceof KmClassifier.TypeAlias)
        {
            // Note that all types are expanded for metadata produced
            // by the Kotlin compiler, so the the type with a type alias
            // classifier may only appear in a call to [visitAbbreviatedType].
            KmClassifier.TypeAlias classifer = (KmClassifier.TypeAlias)kmType.getClassifier();
            type.aliasName = classifer.getName();
        }

        // Outer class type example:
        //
        //      class A<T> { inner class B<U> }
        //
        //      fun foo(a: A<*>.B<Byte?>) {}
        //
        //  The type of the `foo`'s parameter in the metadata is `B<Byte>` (a type whose classifier is class `B`, and it has one type argument,
        //  type `Byte?`), and its outer type is `A<*>` (a type whose classifier is class `A`, and it has one type argument, star projection).
        type.outerClassType = toKotlinTypeMetadata(kmType.getOuterType());

        // For example, in `MutableMap<in String?, *>`, `in String?` is the type projection which is the first type argument of the type.
        type.typeArguments = kmType
                .getArguments()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeMetadataFromKotlinTypeProjection)
                .collect(Collectors.toList());

        //  Flexible types in Kotlin include platform types in Kotlin/JVM and `dynamic` type in Kotlin/JS.
        
        KmFlexibleTypeUpperBound flexibleTypeUpperBound = kmType.getFlexibleTypeUpperBound();
        if (flexibleTypeUpperBound != null)
        {
            // typeFlexibilityId id of the kind of flexibility this type has. For example, "kotlin.jvm.PlatformType" for JVM platform types,
            // or "kotlin.DynamicType" for JS dynamic type, may be null.
            type.flexibilityID = flexibleTypeUpperBound.getTypeFlexibilityId();
            type.upperBounds = new ArrayList<>(Collections.singletonList(toKotlinTypeMetadata(flexibleTypeUpperBound.getType())));
        }
        else
        {
            type.upperBounds = new ArrayList<>();
        }

        type.isRaw = JvmExtensionsKt.isRaw(kmType);

        type.annotations = JvmExtensionsKt
                .getAnnotations(kmType)
                .stream()
                .map(KotlinAnnotationUtilKt::convertAnnotation)
                .collect(Collectors.toList());


        //PROBBUG if a value parameter or a type parameter has an annotation then
        //        the annotation will be stored there but the flag will be
        //        incorrectly set on this type. Sometimes the flag is not set
        //        when there are annotations, sometimes the flag is set but there are no annotations.
        type.flags.common.hasAnnotations = !type.annotations.isEmpty();

        return type;
    }

    private static KotlinTypeMetadata toKotlinTypeMetadataFromKotlinTypeProjection(KmTypeProjection kmTypeProjection)
    {
        if (kmTypeProjection == KmTypeProjection.STAR)
        {
            return KotlinTypeMetadata.starProjection();
        }
        else
        {
            return toKotlinTypeMetadata(kmTypeProjection.getType(), kmTypeProjection.getVariance());
        }
    }
    
    private static KotlinTypeParameterMetadata toKotlinTypeParameterMetadata(KmTypeParameter kmTypeParameter)
    {
        KotlinTypeParameterMetadata kotlinTypeParameterMetadata = new KotlinTypeParameterMetadata(
            convertTypeParameterFlags(kmTypeParameter.getFlags()),
            kmTypeParameter.getName(),
            kmTypeParameter.getId(),
            fromKmVariance(kmTypeParameter.getVariance())
        );

        kotlinTypeParameterMetadata.upperBounds = kmTypeParameter
                .getUpperBounds()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinTypeMetadata)
                .collect(Collectors.toList());

        kotlinTypeParameterMetadata.annotations = JvmExtensionsKt
                .getAnnotations(kmTypeParameter)
                .stream()
                .map(KotlinAnnotationUtilKt::convertAnnotation)
                .collect(Collectors.toList());

        //PROBBUG if a value parameter or a type parameter has an annotation then
        //        the annotation will be stored there but the flag will be
        //        incorrectly set on this type. Sometimes the flag is not set
        //        when there are annotations, sometimes the flag is set but there are no annotations.
        kotlinTypeParameterMetadata.flags.common.hasAnnotations = !kotlinTypeParameterMetadata.annotations.isEmpty();

        return kotlinTypeParameterMetadata;
    }

    private static KotlinConstructorMetadata toKotlinConstructorMetadata(int[] mv, boolean isAnnotationClass, KmConstructor kmConstructor)
    {
        KotlinConstructorMetadata constructor = new KotlinConstructorMetadata(
            convertConstructorFlags(mv, kmConstructor.getFlags())
        );

        List<KmValueParameter> valueParameters = kmConstructor.getValueParameters();
        constructor.valueParameters = new ArrayList<>(valueParameters.size());
        for (int i = 0; i < valueParameters.size(); i++)
        {
            constructor.valueParameters.add(toKotlinValueParameterMetadata(i, valueParameters.get(i)));
        }

        constructor.versionRequirement = toKotlinVersionRequirementMetadataFromList(kmConstructor.getVersionRequirements());

        if (!isAnnotationClass)
        {
            // For annotation classes, the metadata will have a JVM signature for a constructor,
            // while this is impossible to correspond to a real constructor. We set the jvmSignature
            // to null in this case.
            constructor.jvmSignature = fromKotlinJvmMethodSignature(JvmExtensionsKt.getSignature(kmConstructor));
        }

        return constructor;
    }

    private static KotlinValueParameterMetadata toKotlinValueParameterMetadata(int index, KmValueParameter kmValueParameter)
    {
        KotlinValueParameterMetadata valueParameterMetadata = new KotlinValueParameterMetadata(
                convertValueParameterFlags(kmValueParameter.getFlags()),
                index,
                kmValueParameter.getName()
        );

        valueParameterMetadata.type              = toKotlinTypeMetadata(kmValueParameter.getType());
        valueParameterMetadata.varArgElementType = toKotlinTypeMetadata(kmValueParameter.getVarargElementType());

        return valueParameterMetadata;
    }


    private static KotlinVersionRequirementMetadata toKotlinVersionRequirementMetadataFromList(List<KmVersionRequirement> kmVersionRequirement)
    {
        List<KotlinVersionRequirementMetadata> versionRequirementMetadata = kmVersionRequirement
                .stream()
                .map(KotlinMetadataInitializer::toKotlinVersionRequirementMetadata)
                .collect(Collectors.toList());

        if (versionRequirementMetadata.size() > 1)
        {
            // TODO: There can be multiple version requirements; previously we didn't handle it
            //       and we would have used the last visited, since each visit would overwrite the previous.
            return versionRequirementMetadata.get(versionRequirementMetadata.size() - 1);
        }
        else if (versionRequirementMetadata.size() == 1)
        {
            return versionRequirementMetadata.get(0);
        }

        return null;
    }

    private static KotlinVersionRequirementMetadata toKotlinVersionRequirementMetadata(KmVersionRequirement kmVersionRequirement)
    {
        KotlinVersionRequirementMetadata versionReq = new KotlinVersionRequirementMetadata();

        versionReq.kind      = fromKmVersionRequirementVersionKind(kmVersionRequirement.kind);
        versionReq.level     = fromKmVersionRequirementLevel(kmVersionRequirement.level);
        versionReq.errorCode = kmVersionRequirement.getErrorCode();
        versionReq.message   = kmVersionRequirement.getMessage();

        versionReq.major = kmVersionRequirement.version.getMajor();
        versionReq.minor = kmVersionRequirement.version.getMinor();
        versionReq.patch = kmVersionRequirement.version.getPatch();

        return versionReq;
    }


    // Small helper methods.


    private static MethodSignature fromKotlinJvmMethodSignature(JvmMethodSignature jvmMethodSignature)
    {
        if (jvmMethodSignature == null)
        {
            return null;
        }

        try
        {
            return new MethodSignature(null, jvmMethodSignature.getName(), jvmMethodSignature.getDesc());
        }
        catch (Exception e)
        {
            return null;
        }
    }


    private static FieldSignature fromKotlinJvmFieldSignature(JvmFieldSignature jvmFieldSignature)
    {
        if (jvmFieldSignature == null)
        {
            return null;
        }

        return new FieldSignature(null, jvmFieldSignature.getName(), jvmFieldSignature.getDesc());
    }


    private static KotlinTypeVariance fromKmVariance(KmVariance variance)
    {
        if (variance == null) return null;

        switch(variance)
        {
            case IN:        return KotlinTypeVariance.IN;
            case INVARIANT: return KotlinTypeVariance.INVARIANT;
            case OUT:       return KotlinTypeVariance.OUT;
            default:        throw new UnsupportedOperationException("Encountered unknown enum value for KmVariance.");
        }
    }


    private static KotlinVersionRequirementVersionKind fromKmVersionRequirementVersionKind(KmVersionRequirementVersionKind kotlinVersionRequirementVersionKind)
    {
        switch(kotlinVersionRequirementVersionKind)
        {
            case API_VERSION:      return KotlinVersionRequirementVersionKind.API_VERSION;
            case COMPILER_VERSION: return KotlinVersionRequirementVersionKind.COMPILER_VERSION;
            case LANGUAGE_VERSION: return KotlinVersionRequirementVersionKind.LANGUAGE_VERSION;
            default: throw new UnsupportedOperationException("Encountered unknown enum value for KmVersionRequirementVersionKind.");
        }
    }

    private static KotlinVersionRequirementLevel fromKmVersionRequirementLevel(KmVersionRequirementLevel kmVersionRequirementLevel)
    {
        switch(kmVersionRequirementLevel)
        {
            case ERROR:     return KotlinVersionRequirementLevel.ERROR;
            case HIDDEN:    return KotlinVersionRequirementLevel.HIDDEN;
            case WARNING:   return KotlinVersionRequirementLevel.WARNING;
            default: throw new UnsupportedOperationException("Encountered unknown enum value for KmVersionRequirementLevel.");
        }
    }


    private static KotlinEffectType fromKmEffectType(KmEffectType effectType)
    {
        switch(effectType)
        {
            case CALLS:             return KotlinEffectType.CALLS;
            case RETURNS_CONSTANT:  return KotlinEffectType.RETURNS_CONSTANT;
            case RETURNS_NOT_NULL:  return KotlinEffectType.RETURNS_NOT_NULL;
            default: throw new UnsupportedOperationException("Encountered unknown enum value for KmEffectType.");
        }
    }

    private static KotlinEffectInvocationKind fromKmEffectInvocationKind(KmEffectInvocationKind invocationKind)
    {
        if (invocationKind == null)
        {
            return null;
        }
        switch(invocationKind)
        {
            case AT_MOST_ONCE:  return KotlinEffectInvocationKind.AT_MOST_ONCE;
            case EXACTLY_ONCE:  return KotlinEffectInvocationKind.EXACTLY_ONCE;
            case AT_LEAST_ONCE: return KotlinEffectInvocationKind.AT_LEAST_ONCE;
            default: throw new UnsupportedOperationException("Encountered unknown enum value for KmEffectInvocationKind.");
        }
    }


    private static KotlinCommonFlags convertCommonFlags(int kotlinFlags)
    {
        KotlinCommonFlags flags = new KotlinCommonFlags();

        flags.hasAnnotations = Flag.HAS_ANNOTATIONS.invoke(kotlinFlags);

        return flags;
    }


    private static KotlinVisibilityFlags convertVisibilityFlags(int kotlinFlags)
    {
        KotlinVisibilityFlags flags = new KotlinVisibilityFlags();

        flags.isInternal      = Flag.IS_INTERNAL.invoke(kotlinFlags);
        flags.isLocal         = Flag.IS_LOCAL.invoke(kotlinFlags);
        flags.isPrivate       = Flag.IS_PRIVATE.invoke(kotlinFlags);
        flags.isProtected     = Flag.IS_PROTECTED.invoke(kotlinFlags);
        flags.isPublic        = Flag.IS_PUBLIC.invoke(kotlinFlags);
        flags.isPrivateToThis = Flag.IS_PRIVATE_TO_THIS.invoke(kotlinFlags);

        return flags;
    }


    private static KotlinModalityFlags convertModalityFlags(int kotlinFlags)
    {
        KotlinModalityFlags flags = new KotlinModalityFlags();

        flags.isAbstract = Flag.IS_ABSTRACT.invoke(kotlinFlags);
        flags.isFinal    = Flag.IS_FINAL.invoke(kotlinFlags);
        flags.isOpen     = Flag.IS_OPEN.invoke(kotlinFlags);
        flags.isSealed   = Flag.IS_SEALED.invoke(kotlinFlags);

        return flags;
    }

    private static KotlinClassFlags convertClassFlags(int kotlinFlags)
    {
        KotlinClassFlags flags = new KotlinClassFlags(
            convertCommonFlags(kotlinFlags),
            convertVisibilityFlags(kotlinFlags),
            convertModalityFlags(kotlinFlags)
        );

        flags.isUsualClass      = Flag.Class.IS_CLASS.invoke(kotlinFlags);
        flags.isInterface       = Flag.Class.IS_INTERFACE.invoke(kotlinFlags);
        flags.isEnumClass       = Flag.Class.IS_ENUM_CLASS.invoke(kotlinFlags);
        flags.isEnumEntry       = Flag.Class.IS_ENUM_ENTRY.invoke(kotlinFlags);
        flags.isAnnotationClass = Flag.Class.IS_ANNOTATION_CLASS.invoke(kotlinFlags);
        flags.isObject          = Flag.Class.IS_OBJECT.invoke(kotlinFlags);
        flags.isCompanionObject = Flag.Class.IS_COMPANION_OBJECT.invoke(kotlinFlags);
        flags.isInner           = Flag.Class.IS_INNER.invoke(kotlinFlags);
        flags.isData            = Flag.Class.IS_DATA.invoke(kotlinFlags);
        flags.isExternal        = Flag.Class.IS_EXTERNAL.invoke(kotlinFlags);
        flags.isExpect          = Flag.Class.IS_EXPECT.invoke(kotlinFlags);
        flags.isInline          = Flag.Class.IS_INLINE.invoke(kotlinFlags);
        flags.isValue           = Flag.Class.IS_VALUE.invoke(kotlinFlags);
        flags.isFun             = Flag.Class.IS_FUN.invoke(kotlinFlags);

        return flags;
    }

    private static KotlinConstructorFlags convertConstructorFlags(int[] mv, int kotlinFlags)
    {
        KotlinConstructorFlags flags = new KotlinConstructorFlags(
            convertCommonFlags(kotlinFlags),
            convertVisibilityFlags(kotlinFlags)
        );

        flags.isPrimary = Flag.Constructor.IS_PRIMARY.invoke(kotlinFlags);

        // When reading older metadata where the isSecondary flag was not yet introduced,
        // we initialize isSecondary based on isPrimary.
        if (mv[0] == 1 && mv[1] == 1)
        {
            flags.isSecondary = !flags.isPrimary;
        }
        else
        {
            flags.isSecondary = Flag.Constructor.IS_SECONDARY.invoke(kotlinFlags);
        }

        flags.hasNonStableParameterNames = Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES.invoke(kotlinFlags);

        return flags;
    }

    private static KotlinFunctionFlags convertFunctionFlags(int kotlinFlags)
    {
        KotlinFunctionFlags flags = new KotlinFunctionFlags(
            convertCommonFlags(kotlinFlags),
            convertVisibilityFlags(kotlinFlags),
            convertModalityFlags(kotlinFlags)
        );

        flags.isDeclaration  = Flag.Function.IS_DECLARATION.invoke(kotlinFlags);
        flags.isFakeOverride = Flag.Function.IS_FAKE_OVERRIDE.invoke(kotlinFlags);
        flags.isDelegation   = Flag.Function.IS_DELEGATION.invoke(kotlinFlags);
        flags.isSynthesized  = Flag.Function.IS_SYNTHESIZED.invoke(kotlinFlags);
        flags.isOperator     = Flag.Function.IS_OPERATOR.invoke(kotlinFlags);
        flags.isInfix        = Flag.Function.IS_INFIX.invoke(kotlinFlags);
        flags.isInline       = Flag.Function.IS_INLINE.invoke(kotlinFlags);
        flags.isTailrec      = Flag.Function.IS_TAILREC.invoke(kotlinFlags);
        flags.isExternal     = Flag.Function.IS_EXTERNAL.invoke(kotlinFlags);
        flags.isSuspend      = Flag.Function.IS_SUSPEND.invoke(kotlinFlags);
        flags.isExpect       = Flag.Function.IS_EXPECT.invoke(kotlinFlags);

        return flags;
    }


    private static KotlinTypeFlags convertTypeFlags(int kotlinFlags)
    {
        KotlinTypeFlags flags = new KotlinTypeFlags(
            convertCommonFlags(kotlinFlags)
        );

        flags.isNullable          = Flag.Type.IS_NULLABLE.invoke(kotlinFlags);
        flags.isSuspend           = Flag.Type.IS_SUSPEND.invoke(kotlinFlags);
        flags.isDefinitelyNonNull = Flag.Type.IS_DEFINITELY_NON_NULL.invoke(kotlinFlags);

        return flags;
    }


    private static KotlinTypeParameterFlags convertTypeParameterFlags(int kotlinFlags)
    {
        KotlinTypeParameterFlags flags = new KotlinTypeParameterFlags(
            convertCommonFlags(kotlinFlags)
        );

        flags.isReified = Flag.TypeParameter.IS_REIFIED.invoke(kotlinFlags);

        return flags;
    }


    private static KotlinTypeAliasFlags convertTypeAliasFlags(int kotlinFlags)
    {
        return new KotlinTypeAliasFlags(
                convertCommonFlags(kotlinFlags),
                convertVisibilityFlags(kotlinFlags)
        );
    }


    private static KotlinPropertyFlags convertPropertyFlags(int kotlinFlags)
    {
        KotlinPropertyFlags flags = new KotlinPropertyFlags(
            convertCommonFlags(kotlinFlags),
            convertVisibilityFlags(kotlinFlags),
            convertModalityFlags(kotlinFlags)
        );

        flags.isDeclared     = Flag.Property.IS_DECLARATION.invoke(kotlinFlags);
        flags.isFakeOverride = Flag.Property.IS_FAKE_OVERRIDE.invoke(kotlinFlags);
        flags.isDelegation   = Flag.Property.IS_DELEGATION.invoke(kotlinFlags);
        flags.isSynthesized  = Flag.Property.IS_SYNTHESIZED.invoke(kotlinFlags);
        flags.isVar          = Flag.Property.IS_VAR.invoke(kotlinFlags);
        flags.hasGetter      = Flag.Property.HAS_GETTER.invoke(kotlinFlags);
        flags.hasSetter      = Flag.Property.HAS_SETTER.invoke(kotlinFlags);
        flags.isConst        = Flag.Property.IS_CONST.invoke(kotlinFlags);
        flags.isLateinit     = Flag.Property.IS_LATEINIT.invoke(kotlinFlags);
        flags.hasConstant    = Flag.Property.HAS_CONSTANT.invoke(kotlinFlags);
        flags.isExternal     = Flag.Property.IS_EXTERNAL.invoke(kotlinFlags);
        flags.isDelegated    = Flag.Property.IS_DELEGATED.invoke(kotlinFlags);
        flags.isExpect       = Flag.Property.IS_EXPECT.invoke(kotlinFlags);

        return flags;
    }


    private static void setPropertyJvmFlags(KotlinPropertyFlags flags, int jvmFlags)
    {
        flags.isMovedFromInterfaceCompanion =
            JvmFlag.Property.IS_MOVED_FROM_INTERFACE_COMPANION.invoke(jvmFlags);
    }


    private static void setClassJvmFlags(KotlinClassFlags flags, int jvmFlags)
    {
        flags.hasMethodBodiesInInterface    = JvmFlag.Class.HAS_METHOD_BODIES_IN_INTERFACE.invoke(jvmFlags);
        flags.isCompiledInCompatibilityMode = JvmFlag.Class.IS_COMPILED_IN_COMPATIBILITY_MODE.invoke(jvmFlags);
    }


    private static KotlinPropertyAccessorFlags convertPropertyAccessorFlags(int kotlinFlags)
    {
        KotlinPropertyAccessorFlags flags = new KotlinPropertyAccessorFlags(
            convertCommonFlags(kotlinFlags),
            convertVisibilityFlags(kotlinFlags),
            convertModalityFlags(kotlinFlags)
        );

        flags.isDefault  = !Flag.PropertyAccessor.IS_NOT_DEFAULT.invoke(kotlinFlags);
        flags.isExternal = Flag.PropertyAccessor.IS_EXTERNAL.invoke(kotlinFlags);
        flags.isInline   = Flag.PropertyAccessor.IS_INLINE.invoke(kotlinFlags);

        return flags;
    }


    private static KotlinValueParameterFlags convertValueParameterFlags(int kotlinFlags)
    {
        KotlinValueParameterFlags flags = new KotlinValueParameterFlags(
            convertCommonFlags(kotlinFlags)
        );

        flags.hasDefaultValue = Flag.ValueParameter.DECLARES_DEFAULT_VALUE.invoke(kotlinFlags);
        flags.isCrossInline   = Flag.ValueParameter.IS_CROSSINLINE.invoke(kotlinFlags);
        flags.isNoInline      = Flag.ValueParameter.IS_NOINLINE.invoke(kotlinFlags);

        return flags;
    }


    private static KotlinEffectExpressionFlags convertEffectExpressionFlags(int kotlinFlags)
    {
        KotlinEffectExpressionFlags flags = new KotlinEffectExpressionFlags();

        flags.isNullCheckPredicate = Flag.EffectExpression.IS_NULL_CHECK_PREDICATE.invoke(kotlinFlags);
        flags.isNegated            = Flag.EffectExpression.IS_NEGATED.invoke(kotlinFlags);

        return flags;
    }


    public static boolean isSupportedMetadataVersion(KotlinMetadataVersion mv)
    {
        return new JvmMetadataVersion(mv.major, mv.minor, mv.patch).isCompatible();
    }
    
    public static boolean isValidKotlinMetadataAnnotationField(String name)
    {
        switch (name)
        {
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

    public static MetadataType metadataTypeOf(String name)
    {
        switch (name)
        {
            case KOTLIN_METADATA_FIELD_K:  return MetadataType.k;
            case KOTLIN_METADATA_FIELD_BV: return MetadataType.bv;
            case KOTLIN_METADATA_FIELD_MV: return MetadataType.mv;
            case KOTLIN_METADATA_FIELD_D1: return MetadataType.d1;
            case KOTLIN_METADATA_FIELD_D2: return MetadataType.d2;
            case KOTLIN_METADATA_FIELD_XI: return MetadataType.xi;
            case KOLTIN_METADATA_FIELD_XS: return MetadataType.xs;
            case KOTLIN_METADATA_FIELD_PN: return MetadataType.pn;
            default: throw new IllegalArgumentException("Unknown Kotlin metadata field '" + name + "'");
        }
    }
}
