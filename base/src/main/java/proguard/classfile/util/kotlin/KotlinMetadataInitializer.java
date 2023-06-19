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
import kotlinx.metadata.KmAnnotation;
import kotlinx.metadata.KmClass;
import kotlinx.metadata.KmConstructor;
import kotlinx.metadata.KmConstructorExtensionVisitor;
import kotlinx.metadata.KmConstructorVisitor;
import kotlinx.metadata.KmContractVisitor;
import kotlinx.metadata.KmEffectExpressionVisitor;
import kotlinx.metadata.KmEffectInvocationKind;
import kotlinx.metadata.KmEffectType;
import kotlinx.metadata.KmEffectVisitor;
import kotlinx.metadata.KmExtensionType;
import kotlinx.metadata.KmFunction;
import kotlinx.metadata.KmFunctionExtensionVisitor;
import kotlinx.metadata.KmFunctionVisitor;
import kotlinx.metadata.KmLambdaVisitor;
import kotlinx.metadata.KmPackageExtensionVisitor;
import kotlinx.metadata.KmPackageVisitor;
import kotlinx.metadata.KmProperty;
import kotlinx.metadata.KmPropertyExtensionVisitor;
import kotlinx.metadata.KmPropertyVisitor;
import kotlinx.metadata.KmType;
import kotlinx.metadata.KmTypeAlias;
import kotlinx.metadata.KmTypeAliasVisitor;
import kotlinx.metadata.KmTypeExtensionVisitor;
import kotlinx.metadata.KmTypeParameter;
import kotlinx.metadata.KmTypeParameterExtensionVisitor;
import kotlinx.metadata.KmTypeParameterVisitor;
import kotlinx.metadata.KmTypeVisitor;
import kotlinx.metadata.KmValueParameterVisitor;
import kotlinx.metadata.KmVariance;
import kotlinx.metadata.KmVersionRequirement;
import kotlinx.metadata.KmVersionRequirementLevel;
import kotlinx.metadata.KmVersionRequirementVersionKind;
import kotlinx.metadata.KmVersionRequirementVisitor;
import kotlinx.metadata.internal.metadata.jvm.deserialization.JvmMetadataVersion;
import kotlinx.metadata.jvm.JvmConstructorExtensionVisitor;
import kotlinx.metadata.jvm.JvmExtensionsKt;
import kotlinx.metadata.jvm.JvmFieldSignature;
import kotlinx.metadata.jvm.JvmFieldSignature;
import kotlinx.metadata.jvm.JvmFlag;
import kotlinx.metadata.jvm.JvmFunctionExtensionVisitor;
import kotlinx.metadata.jvm.JvmMetadataUtil;
import kotlinx.metadata.jvm.JvmMethodSignature;
import kotlinx.metadata.jvm.JvmMethodSignature;
import kotlinx.metadata.jvm.JvmPackageExtensionVisitor;
import kotlinx.metadata.jvm.JvmPropertyExtensionVisitor;
import kotlinx.metadata.jvm.JvmTypeExtensionVisitor;
import kotlinx.metadata.jvm.JvmTypeParameterExtensionVisitor;
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
import proguard.classfile.kotlin.KotlinAnnotation;
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
import java.util.List;
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
import static proguard.classfile.util.kotlin.KotlinAnnotationUtilKt.convertAnnotation;

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
                    KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata = new KotlinFileFacadeKindMetadata(mv,
                                                                                                                 xi,
                                                                                                                 xs,
                                                                                                                 pn);

                    ((KotlinClassMetadata.FileFacade)md).accept(new PackageReader(kotlinFileFacadeKindMetadata));

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
                        smd.accept(new LambdaReader(kotlinSyntheticClassKindMetadata));
                    }
                    else
                    {
                        kotlinSyntheticClassKindMetadata.functions = trimmed(new ArrayList<>(0));
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
                        new KotlinMultiFilePartKindMetadata(mv, xi, xs, pn);

                    ((KotlinClassMetadata.MultiFileClassPart)md).accept(new PackageReader(
                        kotlinMultiFilePartKindMetadata));

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

        List<KotlinVersionRequirementMetadata> versionRequirementMetadata = kmClass
                .getVersionRequirements()
                .stream()
                .map(KotlinMetadataInitializer::toKotlinVersionRequirementMetadata)
                .collect(Collectors.toList());
        if (versionRequirementMetadata.size() > 1)
        {
            // TODO: There can be multiple version requirements; previously we didn't handle it
            //       and we would have used the last visited, since each visit would overwrite the previous.
            kotlinClassKindMetadata.versionRequirement = versionRequirementMetadata.get(versionRequirementMetadata.size() - 1);
        }
        else if (versionRequirementMetadata.size() == 1)
        {
            kotlinClassKindMetadata.versionRequirement = versionRequirementMetadata.get(0);
        }

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

    private static KotlinFunctionMetadata toKotlinFunctionMetadata(KmFunction kmFunction)
    {
        KotlinFunctionMetadata kotlinFunctionMetadata = new KotlinFunctionMetadata(
            convertFunctionFlags(kmFunction.getFlags()),
            kmFunction.getName()
        );
        FunctionReader functionReader = new FunctionReader(kotlinFunctionMetadata);
        // TODO: remove visitor.
        kmFunction.accept(functionReader);
        return kotlinFunctionMetadata;
    }

    private static KotlinTypeAliasMetadata toKotlinTypeAliasMetadata(KmTypeAlias kmTypeAlias)
    {
        KotlinTypeAliasMetadata typeAlias = new KotlinTypeAliasMetadata(
            convertTypeAliasFlags(kmTypeAlias.getFlags()),
            kmTypeAlias.getName()
        );
        TypeAliasReader typeAliasReader = new TypeAliasReader(typeAlias);
        // TODO: remove visitor.
        kmTypeAlias.accept(typeAliasReader);
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

        PropertyReader propertyReader = new PropertyReader(property);
        // TODO: remove visitor.
        kmProperty.accept(propertyReader);
        return property;
    }

    private static KotlinTypeMetadata toKotlinTypeMetadata(KmType kmType)
    {
        if (kmType == null) return null;

        KotlinTypeMetadata type = new KotlinTypeMetadata(convertTypeFlags(kmType.getFlags()));
        TypeReader typeReader = new TypeReader(type);
        // TODO: remove visitor.
        kmType.accept(typeReader);
        return type;
    }
    
    private static KotlinTypeParameterMetadata toKotlinTypeParameterMetadata(KmTypeParameter kmTypeParameter)
    {
        KotlinTypeParameterMetadata kotlinTypeParameterMetadata = new KotlinTypeParameterMetadata(
            convertTypeParameterFlags(kmTypeParameter.getFlags()),
            kmTypeParameter.getName(),
            kmTypeParameter.getId(),
            fromKmVariance(kmTypeParameter.getVariance())
        );

        TypeParameterReader typeParameterReader = new TypeParameterReader(kotlinTypeParameterMetadata);
        // TODO: remove visitor.
        kmTypeParameter.accept(typeParameterReader);

        return kotlinTypeParameterMetadata;
    }

    private static KotlinConstructorMetadata toKotlinConstructorMetadata(int[] mv, boolean isAnnotationClass, KmConstructor kmConstructor)
    {
        KotlinConstructorMetadata constructor = new KotlinConstructorMetadata(
            convertConstructorFlags(mv, kmConstructor.getFlags())
        );

        ConstructorReader constructorReader = new ConstructorReader(!isAnnotationClass, constructor);
        // TODO: remove visitor.
        kmConstructor.accept(constructorReader);
        return constructor;
    }

    private static KotlinVersionRequirementMetadata toKotlinVersionRequirementMetadata(KmVersionRequirement kmVersionRequirement)
    {
        KotlinVersionRequirementMetadata versionReq = new KotlinVersionRequirementMetadata();
        VersionRequirementReader versionRequirementReader = new VersionRequirementReader(versionReq);
        // TODO: remove visitor.
        kmVersionRequirement.accept(versionRequirementReader);
        return versionReq;
    }


    private class LambdaReader
    extends KmLambdaVisitor
    {
        private final KotlinSyntheticClassKindMetadata  kotlinSyntheticClassKindMetadata;
        private final ArrayList<KotlinFunctionMetadata> functions;

        LambdaReader(KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata)
        {
            this.kotlinSyntheticClassKindMetadata = kotlinSyntheticClassKindMetadata;

            this.functions = new ArrayList<>(1);
        }


        /**
         * @param name the name of the function (usually `"<anonymous>"` or `"<no name provided>"` for lambdas emitted by the Kotlin compiler)
         */
        @Override
        public KmFunctionVisitor visitFunction(int flags, String name)
        {
            KotlinFunctionMetadata function = new KotlinFunctionMetadata(convertFunctionFlags(flags), name);
            functions.add(function);

            return new FunctionReader(function);
        }

        @Override
        public void visitEnd()
        {
            kotlinSyntheticClassKindMetadata.functions = trimmed(this.functions);
        }
    }


    private static class ConstructorReader
    extends KmConstructorVisitor
    {
        private final boolean                   hasValidJvmSignature;
        private final KotlinConstructorMetadata kotlinConstructorMetadata;

        private final ArrayList<KotlinValueParameterMetadata> valueParameters;

        ConstructorReader(boolean                   hasValidJvmSignature,
                          KotlinConstructorMetadata kotlinConstructorMetadata)
        {
            this.hasValidJvmSignature          = hasValidJvmSignature;
            this.kotlinConstructorMetadata     = kotlinConstructorMetadata;

            this.valueParameters = new ArrayList<>(4);
        }

        @Override
        public KmValueParameterVisitor visitValueParameter(int flags, String name)
        {
            KotlinValueParameterMetadata valueParameter =
                    new KotlinValueParameterMetadata(convertValueParameterFlags(flags), valueParameters.size(), name);
            valueParameters.add(valueParameter);

            return new ValueParameterReader(valueParameter);
        }

        @Override
        public KmVersionRequirementVisitor visitVersionRequirement()
        {
            KotlinVersionRequirementMetadata versionReq = new KotlinVersionRequirementMetadata();
            kotlinConstructorMetadata.versionRequirement = versionReq;

            return new VersionRequirementReader(versionReq);
        }

        @Override
        public KmConstructorExtensionVisitor visitExtensions(KmExtensionType extensionType)
        {
            return new ConstructorExtensionReader();
        }

        @Override
        public void visitEnd()
        {
            kotlinConstructorMetadata.valueParameters = trimmed(this.valueParameters);
        }


        private class ConstructorExtensionReader
        extends JvmConstructorExtensionVisitor
        {
            /**
             * For annotation classes, the metadata will have a JVM signature for a constructor,
             * while this is impossible to correspond to a real constructor. We set the jvmSignature
             * to null in this case.
             *
             * @param jvmSignature may be null
             */
            @Override
            public void visit(JvmMethodSignature jvmSignature)
            {
                if (hasValidJvmSignature)
                {
                    kotlinConstructorMetadata.jvmSignature = fromKotlinJvmMethodSignature(jvmSignature);
                }
            }
        }
    }


    private static class PropertyReader
    extends KmPropertyVisitor
    {
        private final KotlinPropertyMetadata kotlinPropertyMetadata;

        private final ArrayList<KotlinValueParameterMetadata> setterParameters;
        private final ArrayList<KotlinTypeParameterMetadata>  typeParameters;
        private final ArrayList<KotlinTypeMetadata>           contextReceivers;


        PropertyReader(KotlinPropertyMetadata kotlinPropertyMetadata)
        {
            this.kotlinPropertyMetadata = kotlinPropertyMetadata;

            this.setterParameters = new ArrayList<>(4);
            this.typeParameters   = new ArrayList<>(1);
            this.contextReceivers = new ArrayList<>();
        }

        /**
         * This method is called for extension properties.
         */
        @Override
        public KmTypeVisitor visitReceiverParameterType(int flags)
        {
            KotlinTypeMetadata receiverType = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinPropertyMetadata.receiverType = receiverType;

            return new TypeReader(receiverType);
        }

        @Override
        public KmTypeVisitor visitContextReceiverType(int flags)
        {
            KotlinTypeMetadata receiverType = new KotlinTypeMetadata(convertTypeFlags(flags));
            contextReceivers.add(receiverType);
            return new TypeReader(receiverType);
        }

        /**
         * Visits the type of the property.
         */
        @Override
        public KmTypeVisitor visitReturnType(int flags)
        {
            KotlinTypeMetadata returnType = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinPropertyMetadata.type = returnType;

            return new TypeReader(returnType);
        }

        /**
         * Visits a value parameter of the setter of this property, if this is a `var` property.
         *
         * @param name the name of the value parameter (`"<set-?>"` for properties emitted by the Kotlin compiler)
         */
        @Override
        public KmValueParameterVisitor visitSetterParameter(int flags, String name)
        {
            KotlinValueParameterMetadata valueParameter =
                    new KotlinValueParameterMetadata(convertValueParameterFlags(flags), setterParameters.size(), name);
            setterParameters.add(valueParameter);

            return new ValueParameterReader(valueParameter);
        }

        @Override
        public KmTypeParameterVisitor visitTypeParameter(int flags, String name, int id, KmVariance variance)
        {
            KotlinTypeParameterMetadata kotlinTypeParameterMetadata =
                new KotlinTypeParameterMetadata(convertTypeParameterFlags(flags), name, id, fromKmVariance(variance));
            typeParameters.add(kotlinTypeParameterMetadata);

            return new TypeParameterReader(kotlinTypeParameterMetadata);
        }

        @Override
        public KmVersionRequirementVisitor visitVersionRequirement()
        {
            KotlinVersionRequirementMetadata versionReq = new KotlinVersionRequirementMetadata();
            kotlinPropertyMetadata.versionRequirement = versionReq;

            return new VersionRequirementReader(versionReq);
        }

        @Override
        public KmPropertyExtensionVisitor visitExtensions(KmExtensionType type)
        {
            return new PropertyExtensionReader();
        }

        @Override
        public void visitEnd()
        {
            kotlinPropertyMetadata.setterParameters = trimmed(this.setterParameters);
            kotlinPropertyMetadata.typeParameters   = trimmed(this.typeParameters);
            kotlinPropertyMetadata.contextReceivers = trimmed(this.contextReceivers);
        }


        private class PropertyExtensionReader
        extends JvmPropertyExtensionVisitor
        {
            /**
             * @param jvmFlags        JVM specific flags, in addition to standard property flags
             * @param fieldSignature  may be null.
             * @param getterSignature may be null. May have a parameter if it is an extension property.
             * @param setterSignature may be null.
             */
            @Override
            public void visit(int jvmFlags, JvmFieldSignature fieldSignature, JvmMethodSignature getterSignature, JvmMethodSignature setterSignature)
            {
                kotlinPropertyMetadata.backingFieldSignature = fromKotlinJvmFieldSignature(fieldSignature);

                kotlinPropertyMetadata.getterSignature = fromKotlinJvmMethodSignature(getterSignature);
                kotlinPropertyMetadata.setterSignature = fromKotlinJvmMethodSignature(setterSignature);

                setPropertyJvmFlags(kotlinPropertyMetadata.flags, jvmFlags);
            }

            /**
             * @param jvmMethodSignature may be null
             */
            @Override
            public void visitSyntheticMethodForAnnotations(JvmMethodSignature jvmMethodSignature)
            {
                kotlinPropertyMetadata.syntheticMethodForAnnotations = fromKotlinJvmMethodSignature(jvmMethodSignature);
            }

            @Override
            public void visitSyntheticMethodForDelegate(JvmMethodSignature jvmMethodSignature) {
                kotlinPropertyMetadata.syntheticMethodForDelegate = fromKotlinJvmMethodSignature(jvmMethodSignature);
            }

            @Override
            public void visitEnd() {}
        }
    }


    private static class TypeAliasReader
    extends KmTypeAliasVisitor
    {
        private final KotlinTypeAliasMetadata kotlinTypeAliasMetadata;

        private final ArrayList<KotlinAnnotation>            annotations;
        private final ArrayList<KotlinTypeParameterMetadata> typeParameters;

        TypeAliasReader(KotlinTypeAliasMetadata kotlinTypeAliasMetadata)
        {
            this.kotlinTypeAliasMetadata = kotlinTypeAliasMetadata;

            this.annotations    = new ArrayList<>(1);
            this.typeParameters = new ArrayList<>(1);
        }

        @Override
        public void visitAnnotation(KmAnnotation annotation)
        {
            annotations.add(convertAnnotation(annotation));
        }

        /**
         * @param id the id of the type parameter, useful to be able to uniquely identify the type parameter in different contexts where
         *           the name isn't enough (e.g. `class A<T> { fun <T> foo(t: T) }`)
         * @param variance the declaration-site variance of the type parameter
         */
        @Override
        public KmTypeParameterVisitor visitTypeParameter(int flags, String name, int id, KmVariance variance)
        {
            KotlinTypeParameterMetadata kotlinTypeParameterMetadata =
                new KotlinTypeParameterMetadata(convertTypeParameterFlags(flags), name, id, fromKmVariance(variance));
            typeParameters.add(kotlinTypeParameterMetadata);

            return new TypeParameterReader(kotlinTypeParameterMetadata);
        }

        /**
         * Visit the right-hand side of the type alias declaration.
         */
        @Override
        public KmTypeVisitor visitUnderlyingType(int flags)
        {
            KotlinTypeMetadata underlyingType = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinTypeAliasMetadata.underlyingType = underlyingType;

            return new TypeReader(underlyingType);
        }

        /**
         * Visit the full expansion of the underlying type.
         */
        @Override
        public KmTypeVisitor visitExpandedType(int flags)
        {
            KotlinTypeMetadata expandedType = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinTypeAliasMetadata.expandedType = expandedType;

            return new TypeReader(expandedType);
        }

        @Override
        public KmVersionRequirementVisitor visitVersionRequirement()
        {
            KotlinVersionRequirementMetadata versionReq = new KotlinVersionRequirementMetadata();
            kotlinTypeAliasMetadata.versionRequirement = versionReq;

            return new VersionRequirementReader(versionReq);
        }

        @Override
        public void visitEnd()
        {
            kotlinTypeAliasMetadata.annotations    = trimmed(this.annotations);
            kotlinTypeAliasMetadata.typeParameters = trimmed(this.typeParameters);
        }
    }


    private class PackageReader
    extends KmPackageVisitor
    {
        private final KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata;

        private final ArrayList<KotlinPropertyMetadata>  properties;
        private final ArrayList<KotlinFunctionMetadata>  functions;
        private final ArrayList<KotlinTypeAliasMetadata> typeAliases;
        private final ArrayList<KotlinPropertyMetadata>  localDelegatedProperties;

        PackageReader(KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
        {
            this.kotlinDeclarationContainerMetadata = kotlinDeclarationContainerMetadata;

            this.properties               = new ArrayList<>(8);
            this.functions                = new ArrayList<>(8);
            this.typeAliases              = new ArrayList<>(2);
            this.localDelegatedProperties = new ArrayList<>(2);
        }


        // Implementations for KmDeclarationContainerVisitor
        @Override
        public KmFunctionVisitor visitFunction(int flags, String name)
        {
            KotlinFunctionMetadata function = new KotlinFunctionMetadata(convertFunctionFlags(flags), name);
            functions.add(function);

            return new FunctionReader(function);
        }

        @Override
        public KmPropertyVisitor visitProperty(int flags, String name, int getterFlags, int setterFlags)
        {
            KotlinPropertyMetadata property = new KotlinPropertyMetadata(convertPropertyFlags(flags),
                                                                         name,
                                                                         convertPropertyAccessorFlags(getterFlags),
                                                                         convertPropertyAccessorFlags(setterFlags));
            properties.add(property);

            return new PropertyReader(property);
        }

        @Override
        public KmTypeAliasVisitor visitTypeAlias(int flags, String name)
        {
            KotlinTypeAliasMetadata typeAlias = new KotlinTypeAliasMetadata(convertTypeAliasFlags(flags), name);
            typeAliases.add(typeAlias);

            return new TypeAliasReader(typeAlias);
        }

        @Override
        public KmPackageExtensionVisitor visitExtensions(KmExtensionType type)
        {
            return new PackageExtensionReader();
        }

        @Override
        public void visitEnd()
        {
            kotlinDeclarationContainerMetadata.properties               = trimmed(this.properties);
            kotlinDeclarationContainerMetadata.functions                = trimmed(this.functions);
            kotlinDeclarationContainerMetadata.typeAliases              = trimmed(this.typeAliases);
            kotlinDeclarationContainerMetadata.localDelegatedProperties = trimmed(this.localDelegatedProperties);
        }


        private class PackageExtensionReader
        extends JvmPackageExtensionVisitor
        {
            @Override
            public KmPropertyVisitor visitLocalDelegatedProperty(int flags, String name, int getterFlags, int setterFlags)
            {
                KotlinPropertyMetadata delegatedProperty =
                    new KotlinPropertyMetadata(convertPropertyFlags(flags),
                                               name,
                                               convertPropertyAccessorFlags(getterFlags),
                                               convertPropertyAccessorFlags(setterFlags));
                localDelegatedProperties.add(delegatedProperty);

                return new PropertyReader(delegatedProperty);
            }

            @Override
            public void visitEnd() {}
        }
    }


    private static class FunctionReader
    extends KmFunctionVisitor
    {
        private final KotlinFunctionMetadata  kotlinFunctionMetadata;

        private final ArrayList<KotlinContractMetadata>       contracts;
        private final ArrayList<KotlinValueParameterMetadata> valueParameters;
        private final ArrayList<KotlinTypeParameterMetadata>  typeParameters;
        private final ArrayList<KotlinTypeMetadata> contextReceivers;

        FunctionReader(KotlinFunctionMetadata kotlinFunctionMetadata)
        {
            this.kotlinFunctionMetadata = kotlinFunctionMetadata;

            this.contracts        = new ArrayList<>(1);
            this.valueParameters  = new ArrayList<>(4);
            this.typeParameters   = new ArrayList<>(1);
            this.contextReceivers = new ArrayList<>();
        }

        @Override
        public KmContractVisitor visitContract()
        {
            KotlinContractMetadata contract = new KotlinContractMetadata();
            contracts.add(contract);

            return new ContractReader(contract);
        }

        @Override
        public KmTypeVisitor visitReceiverParameterType(int flags)
        {
            KotlinTypeMetadata receiverType = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinFunctionMetadata.receiverType = receiverType;

            return new TypeReader(receiverType);
        }

        @Override
        public KmTypeVisitor visitContextReceiverType(int flags)
        {
            KotlinTypeMetadata receiverType = new KotlinTypeMetadata(convertTypeFlags(flags));
            contextReceivers.add(receiverType);
            return new TypeReader(receiverType);
        }

        @Override
        public KmTypeVisitor visitReturnType(int flags)
        {
            KotlinTypeMetadata returnType = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinFunctionMetadata.returnType = returnType;

            return new TypeReader(returnType);
        }

        @Override
        public KmTypeParameterVisitor visitTypeParameter(int flags, String name, int id, KmVariance variance)
        {
            KotlinTypeParameterMetadata kotlinTypeParameterMetadata =
                new KotlinTypeParameterMetadata(convertTypeParameterFlags(flags), name, id, fromKmVariance(variance));
            typeParameters.add(kotlinTypeParameterMetadata);

            return new TypeParameterReader(kotlinTypeParameterMetadata);
        }

        @Override
        public KmValueParameterVisitor visitValueParameter(int flags, String name)
        {
            KotlinValueParameterMetadata valueParameter =
                    new KotlinValueParameterMetadata(convertValueParameterFlags(flags), valueParameters.size(), name);
            valueParameters.add(valueParameter);

            return new ValueParameterReader(valueParameter);
        }

        @Override
        public KmVersionRequirementVisitor visitVersionRequirement()
        {
            KotlinVersionRequirementMetadata versionReq = new KotlinVersionRequirementMetadata();
            kotlinFunctionMetadata.versionRequirement = versionReq;

            return new VersionRequirementReader(versionReq);
        }

        @Override
        public KmFunctionExtensionVisitor visitExtensions(KmExtensionType extensionType)
        {
            return new FunctionExtensionReader();
        }

        @Override
        public void visitEnd()
        {
            kotlinFunctionMetadata.contracts        = trimmed(this.contracts);
            kotlinFunctionMetadata.valueParameters  = trimmed(this.valueParameters);
            kotlinFunctionMetadata.typeParameters   = trimmed(this.typeParameters);
            kotlinFunctionMetadata.contextReceivers = trimmed(this.contextReceivers);
        }


        private class FunctionExtensionReader
        extends JvmFunctionExtensionVisitor
        {
            /**
             * @param signature may be null
             */
            @Override
            public void visit(JvmMethodSignature signature)
            {
                kotlinFunctionMetadata.jvmSignature = fromKotlinJvmMethodSignature(signature);
            }


            /**
             * Visit the JVM internal name of the original class the lambda class for this function is copied from.
             * This information is present for lambdas copied from bodies of inline functions to the use site by the Kotlin compiler.
             */
            @Override
            public void visitLambdaClassOriginName(String internalName)
            {
                kotlinFunctionMetadata.lambdaClassOriginName = internalName;
            }

            @Override
            public void visitEnd() {}
        }
    }


    private static class ContractReader
    extends KmContractVisitor
    {
        private final KotlinContractMetadata kotlinContractMetadata;

        private final ArrayList<KotlinEffectMetadata> effects;

        ContractReader(KotlinContractMetadata kotlinContractMetadata)
        {
            this.kotlinContractMetadata = kotlinContractMetadata;

            this.effects = new ArrayList<>(2);
        }

        /**
         * @param invocationKind number of invocations of the lambda parameter of this function, may be null
         */
        @Override
        public KmEffectVisitor visitEffect(KmEffectType effectType, KmEffectInvocationKind invocationKind)
        {
            KotlinEffectMetadata effect = new KotlinEffectMetadata(
                fromKmEffectType(effectType),
                fromKmEffectInvocationKind(invocationKind));
            effects.add(effect);

            return new EffectReader(effect);
        }

        @Override
        public void visitEnd()
        {
            kotlinContractMetadata.effects = trimmed(this.effects);
        }
    }


    private static class EffectReader
    extends KmEffectVisitor
    {
        private final KotlinEffectMetadata kotlinEffectMetadata;
        private final ArrayList<KotlinEffectExpressionMetadata> constructorArguments = new ArrayList<>();

        EffectReader(KotlinEffectMetadata kotlinEffectMetadata)
        {
            this.kotlinEffectMetadata = kotlinEffectMetadata;
        }

        /**
         * Visits the optional conclusion of the effect. If this method is called, the effect represents an implication with the
         * right-hand side handled by the returned visitor.
         */
        @Override
        public KmEffectExpressionVisitor visitConclusionOfConditionalEffect()
        {
            KotlinEffectExpressionMetadata conclusion = new KotlinEffectExpressionMetadata();
            kotlinEffectMetadata.conclusionOfConditionalEffect = conclusion;

            return new EffectExpressionReader(conclusion);
        }

        /**
         * Visits the optional argument of the effect constructor, i.e. the constant value for the [KmEffectType.RETURNS_CONSTANT] effect,
         * or the parameter reference for the [KmEffectType.CALLS] effect.
         */
        @Override
        public KmEffectExpressionVisitor visitConstructorArgument()
        {
            KotlinEffectExpressionMetadata constructorArg = new KotlinEffectExpressionMetadata();
            constructorArguments.add(constructorArg);

            return new EffectExpressionReader(constructorArg);
        }

        @Override
        public void visitEnd()
        {
            kotlinEffectMetadata.constructorArguments = trimmed(this.constructorArguments);
        }
    }


    private static class EffectExpressionReader
    extends KmEffectExpressionVisitor
    {
        private final KotlinEffectExpressionMetadata       kotlinEffectExpressionMetadata;
        private final List<KotlinEffectExpressionMetadata> andRightHandSides = new ArrayList<>();
        private final List<KotlinEffectExpressionMetadata> orRightHandSides  = new ArrayList<>();

        EffectExpressionReader(KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata)
        {
            this.kotlinEffectExpressionMetadata = kotlinEffectExpressionMetadata;
        }

        /**
         * @param parameterIndex optional 1-based index of the value parameter of the function, for effects which assert something about
         *                       the function parameters. The index 0 means the extension receiver parameter. May be null
         */
        @Override
        public void visit(int flags, Integer parameterIndex)
        {
            kotlinEffectExpressionMetadata.flags = convertEffectExpressionFlags(flags);

            if (parameterIndex != null)
            {
                kotlinEffectExpressionMetadata.parameterIndex = parameterIndex;
            }
        }

        /**
         * Visits the constant value used in the effect expression. May be `true`, `false` or `null`.
         * @param o may be null
         */
        @Override
        public void visitConstantValue(Object o)
        {
            kotlinEffectExpressionMetadata.hasConstantValue = true;
            kotlinEffectExpressionMetadata.constantValue    = o;
        }

        /**
         * Visits the type used as the target of an `is`-expression in the effect expression.
         */
        @Override
        public KmTypeVisitor visitIsInstanceType(int flags)
        {
            KotlinTypeMetadata typeOfIs = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinEffectExpressionMetadata.typeOfIs = typeOfIs;

            return new TypeReader(typeOfIs);
        }

        /**
         * Visits the argument of an `&&`-expression. If this method is called, the expression represents the left-hand side and
         * the returned visitor handles the right-hand side.
         */
        @Override
        public KmEffectExpressionVisitor visitAndArgument()
        {
            KotlinEffectExpressionMetadata andRHS = new KotlinEffectExpressionMetadata();
            this.andRightHandSides.add(andRHS);
            return new EffectExpressionReader(andRHS);
        }


        /**
         * Visits the argument of an `||`-expression. If this method is called, the expression represents the left-hand side and
         * the returned visitor handles the right-hand side.
         */
        @Override
        public KmEffectExpressionVisitor visitOrArgument()
        {
            KotlinEffectExpressionMetadata orRHS = new KotlinEffectExpressionMetadata();
            this.orRightHandSides.add(orRHS);
            return new EffectExpressionReader(orRHS);
        }

        @Override
        public void visitEnd() {
            kotlinEffectExpressionMetadata.andRightHandSides = andRightHandSides;
            kotlinEffectExpressionMetadata.orRightHandSides = orRightHandSides;
        }
    }


    /**
     * Note: visitType will always be called, and visitVararg on top of that if the val parameter is a valarg
     */
    private static class ValueParameterReader
    extends KmValueParameterVisitor
    {
        private final KotlinValueParameterMetadata kotlinValueParameterMetadata;

        ValueParameterReader(KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            this.kotlinValueParameterMetadata = kotlinValueParameterMetadata;
        }

        @Override
        public KmTypeVisitor visitType(int flags)
        {
            KotlinTypeMetadata type = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinValueParameterMetadata.type = type;

            return new TypeReader(type);
        }

        @Override
        public KmTypeVisitor visitVarargElementType(int flags)
        {
            KotlinTypeMetadata varArgType = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinValueParameterMetadata.varArgElementType = varArgType;

            return new TypeReader(varArgType);
        }

        @Override
        public void visitEnd() {}
    }


    /**
     * A visitor to visit a type. The type must have a classifier which is one of: a class [visitClass], type parameter [visitTypeParameter]
     * or type alias [visitTypeAlias]. If the type's classifier is a class or a type alias, it can have type arguments ([visitArgument] and
     * [visitStarProjection]). If the type's classifier is an inner class, it can have the outer type ([visitOuterType]), which captures
     * the generic type arguments of the outer class. Also, each type can have an abbreviation ([visitAbbreviatedType]) in case a type alias
     * was used originally at this site in the declaration (all types are expanded by default for metadata produced by the Kotlin compiler).
     * If [visitFlexibleTypeUpperBound] is called, this type is regarded as a flexible type, and its contents represent the lower bound,
     * and the result of the call represents the upper bound.
     *
     * When using this class, [visitEnd] must be called exactly once and after calls to all other visit* methods.
     */
    private static class TypeReader
    extends KmTypeVisitor
    {
        private KotlinTypeMetadata kotlinTypeMetadata;

        private final ArrayList<KotlinTypeMetadata> typeArguments;
        private final ArrayList<KotlinTypeMetadata> upperBounds;
        private final ArrayList<KotlinAnnotation>   annotations;

        TypeReader(KotlinTypeMetadata kotlinTypeMetadata)
        {
            this.kotlinTypeMetadata = kotlinTypeMetadata;

            this.typeArguments = new ArrayList<>(2);
            this.annotations   = new ArrayList<>(1);
            this.upperBounds   = new ArrayList<>();
        }

        /**
         * Visits the abbreviation of this type. Note that all types are expanded for metadata produced by the Kotlin compiler. For example:
         *
         *     typealias A<T> = MutableList<T>
         *
         *     fun foo(a: A<Any>) {}
         *
         * The type of the `foo`'s parameter in the metadata is actually `MutableList<Any>`, and its abbreviation is `A<Any>`.
         */
        @Override
        public KmTypeVisitor visitAbbreviatedType(int flags)
        {
            KotlinTypeMetadata abbreviatedType = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinTypeMetadata.abbreviation = abbreviatedType;

            return new TypeReader(abbreviatedType);
        }

        /**
         * Visits the name of the class, if this type's classifier is a class.
         */
        @Override
        public void visitClass(String className)
        {
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

            kotlinTypeMetadata.className = className;
        }

        @Override
        public void visitTypeParameter(int id)
        {
            kotlinTypeMetadata.typeParamID = id;
        }

        /**
         * Visits the name of the type alias, if this type's classifier is a type alias. Note that all types are expanded for metadata produced
         * by the Kotlin compiler, so the the type with a type alias classifier may only appear in a call to [visitAbbreviatedType].
         */
        @Override
        public void visitTypeAlias(String aliasName)
        {
            kotlinTypeMetadata.aliasName = aliasName;
        }

        /**
         * Visits the outer type, if this type's classifier is an inner class. For example:
         *
         *     class A<T> { inner class B<U> }
         *
         *     fun foo(a: A<*>.B<Byte?>) {}
         *
         * The type of the `foo`'s parameter in the metadata is `B<Byte>` (a type whose classifier is class `B`, and it has one type argument,
         * type `Byte?`), and its outer type is `A<*>` (a type whose classifier is class `A`, and it has one type argument, star projection).
         */
        @Override
        public KmTypeVisitor visitOuterType(int flags)
        {
            KotlinTypeMetadata outerType = new KotlinTypeMetadata(convertTypeFlags(flags));
            kotlinTypeMetadata.outerClassType = outerType;

            return new TypeReader(outerType);
        }

        /**
         * Visits the type projection used in a type argument of the type based on a class or on a type alias.
         * For example, in `MutableMap<in String?, *>`, `in String?` is the type projection which is the first type argument of the type.
         */
        @Override
        public KmTypeVisitor visitArgument(int flags, KmVariance variance)
        {
            KotlinTypeMetadata typeArgument = new KotlinTypeMetadata(convertTypeFlags(flags), fromKmVariance(variance));
            typeArguments.add(typeArgument);

            return new TypeReader(typeArgument);
        }

        @Override
        public void visitStarProjection()
        {
            typeArguments.add(KotlinTypeMetadata.starProjection());
        }

        /**
         * Visits the upper bound of the type, marking it as flexible and its contents as the lower bound. Flexible types in Kotlin include
         * platform types in Kotlin/JVM and `dynamic` type in Kotlin/JS.
         *
         * @param typeFlexibilityId id of the kind of flexibility this type has. For example, "kotlin.jvm.PlatformType" for JVM platform types,
         *                          or "kotlin.DynamicType" for JS dynamic type, may be null
         */
        @Override
        public KmTypeVisitor visitFlexibleTypeUpperBound(int flags, String typeFlexibilityId)
        {
            kotlinTypeMetadata.flexibilityID = typeFlexibilityId;

            KotlinTypeMetadata upperBound = new KotlinTypeMetadata(convertTypeFlags(flags));
            this.upperBounds.add(upperBound);

            return new TypeReader(upperBound);
        }

        @Override
        public KmTypeExtensionVisitor visitExtensions(KmExtensionType extensionType)
        {
            return new TypeExtensionReader();
        }

        @Override
        public void visitEnd()
        {
            kotlinTypeMetadata.typeArguments = trimmed(this.typeArguments);
            kotlinTypeMetadata.annotations   = trimmed(this.annotations);
            kotlinTypeMetadata.upperBounds   = trimmed(this.upperBounds);

            //PROBBUG if a value parameter or a type parameter has an annotation then
            //        the annotation will be stored there but the flag will be
            //        incorrectly set on this type. Sometimes the flag is not set
            //        when there are annotations, sometimes the flag is set but there are no annotations.
            kotlinTypeMetadata.flags.common.hasAnnotations = !annotations.isEmpty();
        }


        private class TypeExtensionReader
        extends JvmTypeExtensionVisitor
        {
            /**
             * @param isRaw whether the type is seen as a raw type in Java
             */
            @Override
            public void visit(boolean isRaw)
            {
                kotlinTypeMetadata.isRaw = isRaw;
            }

            @Override
            public void visitAnnotation(KmAnnotation annotation)
            {
                // e.g. @ParameterName("prefix") [map, throw away if shrunk], @UnsafeVariance [throw away?]
                annotations.add(convertAnnotation(annotation));
            }

            @Override
            public void visitEnd() {}
        }
    }


    private static class TypeParameterReader
    extends KmTypeParameterVisitor
    {
        private final KotlinTypeParameterMetadata    kotlinTypeParameterMetadata;
        private final ArrayList<KotlinTypeMetadata>  upperBounds;

        TypeParameterReader(KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            this.kotlinTypeParameterMetadata = kotlinTypeParameterMetadata;
            this.upperBounds = new ArrayList<>();
        }

        @Override
        public KmTypeParameterExtensionVisitor visitExtensions(KmExtensionType type)
        {
            return new TypeParameterExtensionReader();
        }

        @Override
        public KmTypeVisitor visitUpperBound(int flags)
        {
            KotlinTypeMetadata upperBound = new KotlinTypeMetadata(convertTypeFlags(flags));
            this.upperBounds.add(upperBound);

            return new TypeReader(upperBound);
        }

        @Override
        public void visitEnd() {
            kotlinTypeParameterMetadata.upperBounds = this.upperBounds;
        }


        private class TypeParameterExtensionReader
        extends JvmTypeParameterExtensionVisitor
        {
            private final ArrayList<KotlinAnnotation> annotations = new ArrayList<>(1);

            @Override
            public void visitAnnotation(KmAnnotation annotation)
            {
                annotations.add(convertAnnotation(annotation));
            }

            @Override
            public void visitEnd()
            {
                kotlinTypeParameterMetadata.annotations = trimmed(this.annotations);

                //PROBBUG if a value parameter or a type parameter has an annotation then
                //        the annotation will be stored there but the flag will be
                //        incorrectly set on this type. Sometimes the flag is not set
                //        when there are annotations, sometimes the flag is set but there are no annotations.
                kotlinTypeParameterMetadata.flags.common.hasAnnotations = !annotations.isEmpty();
            }
        }
    }


    private static class VersionRequirementReader
    extends KmVersionRequirementVisitor
    {
        private final KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata;

        VersionRequirementReader(KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            this.kotlinVersionRequirementMetadata = kotlinVersionRequirementMetadata;
        }

        /**
         * @param errorCode may be null
         * @param message   may be null
         */
        @Override
        public void visit(KmVersionRequirementVersionKind kind, KmVersionRequirementLevel level, Integer errorCode, String message)
        {
            kotlinVersionRequirementMetadata.kind      = fromKmVersionRequirementVersionKind(kind);
            kotlinVersionRequirementMetadata.level     = fromKmVersionRequirementLevel(level);
            kotlinVersionRequirementMetadata.errorCode = errorCode;
            kotlinVersionRequirementMetadata.message   = message;
        }

        @Override
        public void visitVersion(int major, int minor, int patch)
        {
            kotlinVersionRequirementMetadata.major = major;
            kotlinVersionRequirementMetadata.minor = minor;
            kotlinVersionRequirementMetadata.patch = patch;
        }

        @Override
        public void visitEnd() {}
    }


    // Small helper methods.

    private static <K> List<K> trimmed(ArrayList<K> list)
    {
        list.trimToSize();
        return list;
    }


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
