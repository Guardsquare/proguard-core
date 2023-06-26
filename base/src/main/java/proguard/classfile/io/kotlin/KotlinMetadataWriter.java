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

import kotlin.Metadata;
import kotlinx.metadata.Flag;
import kotlinx.metadata.KmClass;
import kotlinx.metadata.KmClassifier;
import kotlinx.metadata.KmConstantValue;
import kotlinx.metadata.KmConstructor;
import kotlinx.metadata.KmContract;
import kotlinx.metadata.KmDeclarationContainer;
import kotlinx.metadata.KmEffect;
import kotlinx.metadata.KmEffectExpression;
import kotlinx.metadata.KmEffectInvocationKind;
import kotlinx.metadata.KmEffectType;
import kotlinx.metadata.KmFlexibleTypeUpperBound;
import kotlinx.metadata.KmFunction;
import kotlinx.metadata.KmLambda;
import kotlinx.metadata.KmPackage;
import kotlinx.metadata.KmProperty;
import kotlinx.metadata.KmType;
import kotlinx.metadata.KmTypeAlias;
import kotlinx.metadata.KmTypeParameter;
import kotlinx.metadata.KmTypeProjection;
import kotlinx.metadata.KmValueParameter;
import kotlinx.metadata.KmVariance;
import kotlinx.metadata.KmVersion;
import kotlinx.metadata.KmVersionRequirement;
import kotlinx.metadata.KmVersionRequirementLevel;
import kotlinx.metadata.KmVersionRequirementVersionKind;
import kotlinx.metadata.jvm.JvmExtensionsKt;
import kotlinx.metadata.jvm.JvmFieldSignature;
import kotlinx.metadata.jvm.JvmFlag;
import kotlinx.metadata.jvm.JvmMetadataUtil;
import kotlinx.metadata.jvm.JvmMethodSignature;
import kotlinx.metadata.jvm.KotlinClassMetadata;
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
import proguard.classfile.util.kotlin.KotlinMetadataInitializer.MetadataType;
import proguard.classfile.visitor.ClassVisitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.joining;
import static kotlinx.metadata.FlagsKt.flagsOf;
import static kotlinx.metadata.jvm.KotlinClassMetadata.COMPATIBLE_METADATA_VERSION;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_CLASS;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_FILE_FACADE;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_MULTI_FILE_CLASS_FACADE;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_MULTI_FILE_CLASS_PART;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_SYNTHETIC_CLASS;
import static proguard.classfile.kotlin.KotlinConstants.TYPE_KOTLIN_METADATA;

/**
 * This class visitor writes the information stored in a Clazz's kotlinMetadata field
 * to a @kotlin/Metadata annotation on the class.
 */
public class KotlinMetadataWriter
implements ClassVisitor,
           KotlinMetadataVisitor,
           ElementValueVisitor
{
    private final ClassVisitor extraClassVisitor;

    private int      k;
    private int[]    mv;
    private String[] d1;
    private String[] d2;
    private int      xi;
    private String   xs;
    private String   pn;

    private ConstantPoolEditor   constantPoolEditor;
    private ConstantPoolShrinker constantPoolShrinker = new ConstantPoolShrinker();

    private MetadataType currentType;

    private final BiConsumer<Clazz, String> errorHandler;

    private static final KotlinMetadataVersion COMPATIBLE_VERSION = new KotlinMetadataVersion(COMPATIBLE_METADATA_VERSION);
    private KotlinMetadataVersion version;

    @Deprecated
    public KotlinMetadataWriter(WarningPrinter warningPrinter)
    {
        this(warningPrinter, null);
    }

    @Deprecated
    public KotlinMetadataWriter(WarningPrinter warningPrinter, ClassVisitor extraClassVisitor)
    {
        this((clazz, message) -> warningPrinter.print(clazz.getName(), message), extraClassVisitor);
    }

    public KotlinMetadataWriter(BiConsumer<Clazz, String> errorHandler)
    {
        this(errorHandler, null);
    }

    public KotlinMetadataWriter(BiConsumer<Clazz, String> errorHandler, ClassVisitor extraClassVisitor)
    {
        this.errorHandler      = errorHandler;
        this.extraClassVisitor = extraClassVisitor;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        clazz.kotlinMetadataAccept(this);
    }


    // Implementations for KotlinMetadataVisitor.

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
        // Set the metadata version we want to write.
        KotlinMetadataVersion originalVersion = new KotlinMetadataVersion(kotlinMetadata.mv);

        version = originalVersion.canBeWritten() ? originalVersion : COMPATIBLE_VERSION;

        switch (kotlinMetadata.k)
        {
            case METADATA_KIND_CLASS:
                kotlinMetadata.accept(clazz, new KotlinClassConstructor());           break;
            case METADATA_KIND_FILE_FACADE:
                kotlinMetadata.accept(clazz, new KotlinFileFacadeConstructor());      break;
            case METADATA_KIND_SYNTHETIC_CLASS:
                kotlinMetadata.accept(clazz, new KotlinSyntheticClassConstructor());  break;
            case METADATA_KIND_MULTI_FILE_CLASS_FACADE:
                kotlinMetadata.accept(clazz, new KotlinMultiFileFacadeConstructor()); break;
            case METADATA_KIND_MULTI_FILE_CLASS_PART:
                kotlinMetadata.accept(clazz, new KotlinMultiFilePartConstructor());   break;
        }

        // Pass the new data to the .read() method as a sanity check.
        Metadata metadata = JvmMetadataUtil.Metadata(k, mv, d1, d2, xs, pn, xi);
        KotlinClassMetadata md = KotlinClassMetadata.read(metadata);
        if (md == null)
        {
            String version = mv == null ? "unknown" : Arrays.stream(mv).mapToObj(Integer::toString).collect(joining("."));
            errorHandler.accept(clazz,
                                "Encountered corrupt Kotlin metadata in class " +
                                clazz.getName() + " (version " + version + ")" +
                                ". Not processing the metadata for this class.");
            return;
        }

        this.constantPoolEditor = new ConstantPoolEditor((ProgramClass) clazz);

        try
        {
            clazz.accept(new AllAttributeVisitor(
                         new AttributeNameFilter(Attribute.RUNTIME_VISIBLE_ANNOTATIONS,
                         new AllAnnotationVisitor(
                         new AnnotationTypeFilter(TYPE_KOTLIN_METADATA,
                                                  new AllElementValueVisitor(this))))));
        }
        catch (IllegalArgumentException e)
        {
            // It's possible that an exception is thrown by the MetadataType.valueOf calls if
            // the kotlin.Metadata class was accidentally obfuscated.
            errorHandler.accept(clazz, "Invalid Kotlin metadata annotation for " +
                                        clazz.getName() +
                                        " (invalid Kotlin metadata field names)." +
                                        " Not writing the metadata for this class.");
        }

        // Clean up dangling Strings from the original metadata.
        clazz.accept(constantPoolShrinker);

        if (extraClassVisitor != null)
        {
            clazz.accept(extraClassVisitor);
        }
    }


    // Implementations for ElementValueVisitor.

    @Override
    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
        this.currentType   = MetadataType.valueOf(constantElementValue.getMethodName(clazz));

        switch (currentType)
        {
            case k:  constantElementValue.u2constantValueIndex = constantPoolEditor.addIntegerConstant(k);  break;
            case xi: constantElementValue.u2constantValueIndex = constantPoolEditor.addIntegerConstant(xi); break;
            case xs: constantElementValue.u2constantValueIndex = constantPoolEditor.addUtf8Constant(xs);    break;
            case pn: constantElementValue.u2constantValueIndex = constantPoolEditor.addUtf8Constant(pn);    break;
        }
    }

    @Override
    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        this.currentType   = MetadataType.valueOf(arrayElementValue.getMethodName(clazz));

        switch (currentType)
        {
            case mv:
                arrayElementValue.u2elementValuesCount = mv.length;
                ElementValue[] newMvElementValues = new ElementValue[mv.length];
                for (int k = 0; k < mv.length; k++)
                {
                    newMvElementValues[k] =
                        new ConstantElementValue('I',
                                                 0,
                                                 constantPoolEditor.addIntegerConstant(mv[k]));
                }
                arrayElementValue.elementValues = newMvElementValues;
                break;
            case d1:
                arrayElementValue.u2elementValuesCount = d1.length;
                ElementValue[] newD1ElementValues = new ElementValue[d1.length];
                for (int k = 0; k < d1.length; k++)
                {
                    newD1ElementValues[k] =
                        new ConstantElementValue('s',
                                                 0,
                                                 constantPoolEditor.addUtf8Constant(d1[k]));
                }
                arrayElementValue.elementValues = newD1ElementValues;
                break;
            case d2:
                arrayElementValue.u2elementValuesCount = d2.length;
                ElementValue[] newD2ElementValues = new ElementValue[d2.length];
                for (int k = 0; k < d2.length; k++)
                {
                    newD2ElementValues[k] =
                        new ConstantElementValue('s',
                                                 0,
                                                 constantPoolEditor.addUtf8Constant(d2[k]));
                }
                arrayElementValue.elementValues = newD2ElementValues;
                break;
        }
    }


    // Helper classes.

    private class ContractConstructor
    implements KotlinContractVisitor
    {
        private KmFunction kmFunction;

        ContractConstructor(KmFunction kmFunction)
        {
            this.kmFunction = kmFunction;
        }

        // Implementations for KotlinContractVisitor.
        @Override
        public void visitContract(Clazz                  clazz,
                                  KotlinMetadata         kotlinMetadata,
                                  KotlinFunctionMetadata kotlinFunctionMetadata,
                                  KotlinContractMetadata kotlinContractMetadata)
        {
            KmContract kmContract = new KmContract();

            kotlinContractMetadata.effectsAccept(clazz,
                                                 kotlinMetadata,
                                                 kotlinFunctionMetadata,
                                                 new EffectConstructor(kmContract));
            kmFunction.setContract(kmContract);
        }
    }

    private class EffectConstructor
    implements KotlinEffectVisitor
    {
        private KmContract kmContract;
        private EffectConstructor(KmContract kmContract) { this.kmContract = kmContract; }


        // Implementations for KotlinEffectVisitor.
        @Override
        public void visitEffect(Clazz                  clazz,
                                KotlinMetadata         kotlinMetadata,
                                KotlinFunctionMetadata kotlinFunctionMetadata,
                                KotlinContractMetadata kotlinContractMetadata,
                                KotlinEffectMetadata   kotlinEffectMetadata)
        {
            KmEffect kmEffect = new KmEffect(toKmEffectType(kotlinEffectMetadata.effectType),
                                             toKmEffectInvocationKind(kotlinEffectMetadata.invocationKind));

            kotlinEffectMetadata.conclusionOfConditionalEffectAccept(clazz,
                                                                     new EffectExprConstructor(kmEffect));

            kotlinEffectMetadata.constructorArgumentAccept(clazz,
                                                           new EffectExprConstructor(kmEffect));

            kmContract.getEffects().add(kmEffect);
        }
    }

    private class EffectExprConstructor
    implements KotlinEffectExprVisitor
    {
        private KmEffectExpression kmEffectExpression;
        private KmEffect           kmEffect;
        private EffectExprConstructor(KmEffect kmEffect) { this.kmEffect = kmEffect; }

        private KmEffectExpression nestedKmEffectExpression;
        private EffectExprConstructor(KmEffectExpression nestedKmEffectExpression) { this.nestedKmEffectExpression = nestedKmEffectExpression; }


        // Implementations for KotlinEffectExprVisitor.
        @Override
        public void visitAnyEffectExpression(Clazz                          clazz,
                                             KotlinEffectMetadata           kotlinEffectMetadata,
                                             KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata)
        {
            kmEffectExpression.setFlags(convertEffectExpressionFlags(kotlinEffectExpressionMetadata.flags));
            kmEffectExpression.setParameterIndex(kotlinEffectExpressionMetadata.parameterIndex);

            if (kotlinEffectExpressionMetadata.hasConstantValue)
            {
                kmEffectExpression.setConstantValue(new KmConstantValue(kotlinEffectExpressionMetadata.constantValue));
            }

            kotlinEffectExpressionMetadata.andRightHandSideAccept(clazz,
                                                                  kotlinEffectMetadata,
                                                                  new EffectExprConstructor(kmEffectExpression));
            kotlinEffectExpressionMetadata.orRightHandSideAccept(clazz,
                                                                 kotlinEffectMetadata,
                                                                 new EffectExprConstructor(kmEffectExpression));

            kotlinEffectExpressionMetadata.typeOfIsAccept(clazz,
                                                          new TypeConstructor(kmEffectExpression));

        }

        @Override
        public void visitAndRHSExpression(Clazz                          clazz,
                                          KotlinEffectMetadata           kotlinEffectMetadata,
                                          KotlinEffectExpressionMetadata lhs,
                                          KotlinEffectExpressionMetadata rhs)
        {
            kmEffectExpression = new KmEffectExpression();
            visitAnyEffectExpression(clazz, kotlinEffectMetadata, rhs);
            nestedKmEffectExpression.getAndArguments().addAll(kmEffectExpression.getAndArguments());
        }

        @Override
        public void visitOrRHSExpression(Clazz                          clazz,
                                         KotlinEffectMetadata           kotlinEffectMetadata,
                                         KotlinEffectExpressionMetadata lhs,
                                         KotlinEffectExpressionMetadata rhs)
        {
            kmEffectExpression = new KmEffectExpression();
            visitAnyEffectExpression(clazz, kotlinEffectMetadata, rhs);
            nestedKmEffectExpression.getOrArguments().addAll(kmEffectExpression.getOrArguments());
        }

        @Override
        public void visitConstructorArgExpression(Clazz                          clazz,
                                                  KotlinEffectMetadata           kotlinEffectMetadata,
                                                  KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata)
        {
            kmEffectExpression = new KmEffectExpression();
            visitAnyEffectExpression(clazz, kotlinEffectMetadata, kotlinEffectExpressionMetadata);
            kmEffect.getConstructorArguments().add(kmEffectExpression);
        }

        @Override
        public void visitConclusionExpression(Clazz                          clazz,
                                              KotlinEffectMetadata           kotlinEffectMetadata,
                                              KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata)
        {
            kmEffectExpression = new KmEffectExpression();
            visitAnyEffectExpression(clazz, kotlinEffectMetadata, kotlinEffectExpressionMetadata);
            kmEffect.setConclusion(kmEffectExpression);
        }
    }

    private class KotlinDeclarationContainerConstructor
    implements KotlinPropertyVisitor,
               KotlinFunctionVisitor,
               KotlinTypeAliasVisitor
    {
        KmDeclarationContainer kmDeclarationContainer;
        KmProperty             kmProperty;

        KotlinDeclarationContainerConstructor(KmDeclarationContainer kmDeclarationContainer)
        {
            this.kmDeclarationContainer = kmDeclarationContainer;
        }


        // Simplifications for KotlinPropertyVisitor.
        @Override
        public void visitAnyProperty(Clazz                              clazz,
                                     KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                     KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            kotlinPropertyMetadata.typeAccept(clazz,
                                              kotlinDeclarationContainerMetadata,
                                              new TypeConstructor(kmProperty));
            kotlinPropertyMetadata.receiverTypeAccept(clazz,
                                                      kotlinDeclarationContainerMetadata,
                                                      new TypeConstructor(kmProperty));
            kotlinPropertyMetadata.contextReceiverTypesAccept(clazz,
                                                              kotlinDeclarationContainerMetadata,
                                                              new TypeConstructor(kmProperty));
            kotlinPropertyMetadata.setterParametersAccept(clazz,
                                                          kotlinDeclarationContainerMetadata,
                                                          new ValueParameterConstructor(kmProperty));
            kotlinPropertyMetadata.typeParametersAccept(clazz,
                                                        kotlinDeclarationContainerMetadata,
                                                        new TypeParameterConstructor(kmProperty));
            kotlinPropertyMetadata.versionRequirementAccept(clazz,
                                                            kotlinDeclarationContainerMetadata,
                                                            new VersionRequirementConstructor(kmProperty));

            JvmExtensionsKt.setJvmFlags(kmProperty, convertPropertyJvmFlags(kotlinPropertyMetadata.flags));
            JvmExtensionsKt.setGetterSignature(kmProperty, toKotlinJvmMethodSignature(kotlinPropertyMetadata.getterSignature));
            JvmExtensionsKt.setSetterSignature(kmProperty, toKotlinJvmMethodSignature(kotlinPropertyMetadata.setterSignature));
            JvmExtensionsKt.setFieldSignature(kmProperty, toKotlinJvmFieldSignature(kotlinPropertyMetadata.backingFieldSignature));

            if (kotlinPropertyMetadata.syntheticMethodForAnnotations != null)
            {
                JvmExtensionsKt.setSyntheticMethodForAnnotations(kmProperty,
                        toKotlinJvmMethodSignature(kotlinPropertyMetadata.syntheticMethodForAnnotations));
            }

            if (kotlinPropertyMetadata.syntheticMethodForDelegate != null)
            {
                JvmExtensionsKt.setSyntheticMethodForDelegate(kmProperty,
                        toKotlinJvmMethodSignature(kotlinPropertyMetadata.syntheticMethodForDelegate));
            }
        }

        @Override
        public void visitProperty(Clazz                              clazz,
                                  KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                  KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            kmProperty = new KmProperty(convertPropertyFlags(kotlinPropertyMetadata.flags),
                    kotlinPropertyMetadata.name,
                    convertPropertyAccessorFlags(kotlinPropertyMetadata.getterFlags),
                    convertPropertyAccessorFlags(kotlinPropertyMetadata.setterFlags));

            visitAnyProperty(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata);
            kmDeclarationContainer.getProperties().add(kmProperty);
        }

        @Override
        public void visitDelegatedProperty(Clazz                              clazz,
                                           KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                           KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            kmProperty = new KmProperty(convertPropertyFlags(kotlinPropertyMetadata.flags),
                                        kotlinPropertyMetadata.name,
                                        convertPropertyAccessorFlags(kotlinPropertyMetadata.getterFlags),
                                        convertPropertyAccessorFlags(kotlinPropertyMetadata.setterFlags));

            visitAnyProperty(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata);
            kmDeclarationContainer.getProperties().add(kmProperty);
        }


        // Simplifications for KotlinFunctionVisitor.
        @Override
        public void visitAnyFunction(Clazz clazz, KotlinMetadata kotlinMetadata, KotlinFunctionMetadata kotlinFunctionMetadata) {}

        @Override
        public void visitFunction(Clazz                              clazz,
                                  KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                  KotlinFunctionMetadata             kotlinFunctionMetadata)
        {
            KmFunction kmFunction = new KmFunction(convertFunctionFlags(kotlinFunctionMetadata.flags),
                                        kotlinFunctionMetadata.name);

            kotlinFunctionMetadata.valueParametersAccept(clazz,
                                                         kotlinDeclarationContainerMetadata,
                                                         new ValueParameterConstructor(kmFunction));
            kotlinFunctionMetadata.returnTypeAccept(clazz,
                                                    kotlinDeclarationContainerMetadata,
                                                    new TypeConstructor(kmFunction));
            kotlinFunctionMetadata.receiverTypeAccept(clazz,
                                                      kotlinDeclarationContainerMetadata,
                                                      new TypeConstructor(kmFunction));
            kotlinFunctionMetadata.contextReceiverTypesAccept(clazz,
                                                              kotlinDeclarationContainerMetadata,
                                                              new TypeConstructor(kmFunction));
            kotlinFunctionMetadata.typeParametersAccept(clazz,
                                                        kotlinDeclarationContainerMetadata,
                                                        new TypeParameterConstructor(kmFunction));
            kotlinFunctionMetadata.versionRequirementAccept(clazz,
                                                            kotlinDeclarationContainerMetadata,
                                                            new VersionRequirementConstructor(kmFunction));
            kotlinFunctionMetadata.contractsAccept(clazz,
                                                   kotlinDeclarationContainerMetadata,
                                                   new ContractConstructor(kmFunction));

            JvmExtensionsKt.setSignature(kmFunction,
                    toKotlinJvmMethodSignature(kotlinFunctionMetadata.jvmSignature));

            if (kotlinFunctionMetadata.lambdaClassOriginName != null)
            {
                JvmExtensionsKt.setLambdaClassOriginName(kmFunction, kotlinFunctionMetadata.lambdaClassOriginName);
            }

            kmDeclarationContainer.getFunctions().add(kmFunction);
        }


        // Implementations for KotlinTypeAliasVisitor
        @Override
        public void visitTypeAlias(Clazz                              clazz,
                                   KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                   KotlinTypeAliasMetadata            kotlinTypeAliasMetadata)
        {
            KmTypeAlias kmTypeAlias = new KmTypeAlias(convertTypeAliasFlags(kotlinTypeAliasMetadata.flags),
                                          kotlinTypeAliasMetadata.name);

            kotlinTypeAliasMetadata.typeParametersAccept(clazz,
                                                         kotlinDeclarationContainerMetadata,
                                                         new TypeParameterConstructor(kmTypeAlias));
            kotlinTypeAliasMetadata.underlyingTypeAccept(clazz,
                                                         kotlinDeclarationContainerMetadata,
                                                         new TypeConstructor(kmTypeAlias));
            kotlinTypeAliasMetadata.expandedTypeAccept(clazz,
                                                       kotlinDeclarationContainerMetadata,
                                                       new TypeConstructor(kmTypeAlias));
            kotlinTypeAliasMetadata.versionRequirementAccept(clazz,
                                                             kotlinDeclarationContainerMetadata,
                                                             new VersionRequirementConstructor(kmTypeAlias));
            kotlinTypeAliasMetadata.annotationsAccept(clazz,
                                                      new AnnotationConstructor(annotion -> kmTypeAlias.getAnnotations().add(annotion)));

            kmDeclarationContainer.getTypeAliases().add(kmTypeAlias);
        }


        // Implementations for KotlinMetadataVisitor.
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}
    }


    /**
     * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin class (k == 1) metadata.
     */
    private class KotlinClassConstructor
    extends KotlinDeclarationContainerConstructor
    implements KotlinMetadataVisitor,
               KotlinConstructorVisitor
    {
        KmClass kmClass;

        KotlinClassConstructor()
        {
            this(new KmClass());
        }

        private KotlinClassConstructor(KmClass kmClass)
        {
            super(kmClass);
            this.kmClass = kmClass;
        }


        // Implementations for KotlinMetadataVisitor.
        @Override
        public void visitKotlinClassMetadata(Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata)
        {
            kmClass.setFlags(convertClassFlags(kotlinClassKindMetadata.flags));
            kmClass.setName(kotlinClassKindMetadata.className.replace('$','.'));

            if (kotlinClassKindMetadata.companionObjectName != null)
            {
                kmClass.setCompanionObject(kotlinClassKindMetadata.companionObjectName);
            }

            kotlinClassKindMetadata.propertiesAccept(clazz,  this);
            kotlinClassKindMetadata.functionsAccept(clazz,   this);
            kotlinClassKindMetadata.typeAliasesAccept(clazz, this);

            for (String enumEntry : kotlinClassKindMetadata.enumEntryNames)
            {
                kmClass.getEnumEntries().add(enumEntry);
            }

            for (String nestedClass : kotlinClassKindMetadata.nestedClassNames)
            {
                kmClass.getNestedClasses().add(nestedClass);
            }

            for (String sealedSubClass : kotlinClassKindMetadata.sealedSubclassNames)
            {
                kmClass.getSealedSubclasses().add(sealedSubClass.replace('$', '.'));
            }

            kotlinClassKindMetadata.constructorsAccept(                     clazz, this);
            kotlinClassKindMetadata.superTypesAccept(                       clazz, new TypeConstructor(kmClass));
            kotlinClassKindMetadata.typeParametersAccept(                   clazz, new TypeParameterConstructor(kmClass));
            kotlinClassKindMetadata.contextReceiverTypesAccept(             clazz, new TypeConstructor(kmClass));
            kotlinClassKindMetadata.versionRequirementAccept(               clazz, new VersionRequirementConstructor(kmClass));
            kotlinClassKindMetadata.inlineClassUnderlyingPropertyTypeAccept(clazz, new TypeConstructor(kmClass));

            for (KotlinPropertyMetadata propertyMetadata : kotlinClassKindMetadata.localDelegatedProperties)
            {
                JvmExtensionsKt.getLocalDelegatedProperties(kmClass).add(toKmProperty(propertyMetadata));
            }

            if (kotlinClassKindMetadata.anonymousObjectOriginName != null)
            {
                JvmExtensionsKt.setAnonymousObjectOriginName(kmClass, kotlinClassKindMetadata.anonymousObjectOriginName);
            }

            JvmExtensionsKt.setJvmFlags(kmClass, convertClassJvmFlags(kotlinClassKindMetadata.flags));

            // Finally store the protobuf contents in the fields of the enclosing class.
            Metadata metadata = KotlinClassMetadata.Companion.writeClass(kmClass,
                                                                         version.toArray(),
                                                                         kotlinClassKindMetadata.xi).getAnnotationData();

            k  = metadata.k();
            mv = metadata.mv();
            d1 = metadata.d1();
            d2 = metadata.d2();
            xi = metadata.xi();
            xs = metadata.xs();
            pn = metadata.pn();
        }


        // Implementations for KotlinConstructorVisitor.
        @Override
        public void visitConstructor(Clazz                     clazz,
                                     KotlinClassKindMetadata   kotlinClassKindMetadata,
                                     KotlinConstructorMetadata kotlinConstructorMetadata)
        {
            KmConstructor kmConstructor = new KmConstructor(convertConstructorFlags(kotlinConstructorMetadata.flags));

            kotlinConstructorMetadata.valueParametersAccept(clazz,
                                                            kotlinClassKindMetadata,
                                                            new ValueParameterConstructor(kmConstructor));

            kotlinConstructorMetadata.versionRequirementAccept(clazz,
                                                               kotlinClassKindMetadata,
                                                               new VersionRequirementConstructor(kmConstructor));

            // Extensions.
            if (kotlinConstructorMetadata.jvmSignature != null)
            {
                JvmExtensionsKt.setSignature(kmConstructor, toKotlinJvmMethodSignature(kotlinConstructorMetadata.jvmSignature));
            }

            kmClass.getConstructors().add(kmConstructor);
        }


        private int convertClassFlags(KotlinClassFlags flags)
        {
            Set<Flag> flagSet = new HashSet<>();

            flagSet.addAll(convertCommonFlags(flags.common));
            flagSet.addAll(convertVisibilityFlags(flags.visibility));
            flagSet.addAll(convertModalityFlags(flags.modality));

            if (flags.isUsualClass)      flagSet.add(Flag.Class.IS_CLASS);
            if (flags.isInterface)       flagSet.add(Flag.Class.IS_INTERFACE);
            if (flags.isEnumClass)       flagSet.add(Flag.Class.IS_ENUM_CLASS);
            if (flags.isEnumEntry)       flagSet.add(Flag.Class.IS_ENUM_ENTRY);
            if (flags.isAnnotationClass) flagSet.add(Flag.Class.IS_ANNOTATION_CLASS);
            if (flags.isObject)          flagSet.add(Flag.Class.IS_OBJECT);
            if (flags.isCompanionObject) flagSet.add(Flag.Class.IS_COMPANION_OBJECT);
            if (flags.isInner)           flagSet.add(Flag.Class.IS_INNER);
            if (flags.isData)            flagSet.add(Flag.Class.IS_DATA);
            if (flags.isExternal)        flagSet.add(Flag.Class.IS_EXTERNAL);
            if (flags.isExpect)          flagSet.add(Flag.Class.IS_EXPECT);
            if (flags.isInline)          flagSet.add(Flag.Class.IS_INLINE);
            if (flags.isValue)           flagSet.add(Flag.Class.IS_VALUE);
            if (flags.isFun)             flagSet.add(Flag.Class.IS_FUN);

            return flagsOf(flagSet.toArray(new Flag[0]));
        }


        private int convertConstructorFlags(KotlinConstructorFlags flags)
        {
            Set<Flag> flagSet = new HashSet<>();

            flagSet.addAll(convertCommonFlags(flags.common));
            flagSet.addAll(convertVisibilityFlags(flags.visibility));

            if (flags.isPrimary)                  flagSet.add(Flag.Constructor.IS_PRIMARY);
            if (flags.isSecondary)                flagSet.add(Flag.Constructor.IS_SECONDARY);
            if (flags.hasNonStableParameterNames) flagSet.add(Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES);

            return flagsOf(flagSet.toArray(new Flag[0]));
        }
    }


    private class ValueParameterConstructor
    implements KotlinValueParameterVisitor
    {
        private KmValueParameter kmValueParameter;

        private KmConstructor    kmConstructor;
        ValueParameterConstructor(KmConstructor kmConstructor) { this.kmConstructor = kmConstructor; }

        private KmProperty kmProperty;
        ValueParameterConstructor(KmProperty kmProperty) { this.kmProperty = kmProperty; }

        private KmFunction kmFunction;
        ValueParameterConstructor(KmFunction kmFunction) { this.kmFunction = kmFunction; }


        // Implementations for KotlinValueParameterVisitor.
        @Override
        public void visitAnyValueParameter(Clazz clazz,
                                           KotlinValueParameterMetadata kotlinValueParameterMetadata) {}

        @Override
        public void visitConstructorValParameter(Clazz                        clazz,
                                                 KotlinClassKindMetadata      kotlinClassKindMetadata,
                                                 KotlinConstructorMetadata    kotlinConstructorMetadata,
                                                 KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            kmValueParameter = new KmValueParameter(convertValueParameterFlags(kotlinValueParameterMetadata.flags),
                                                    kotlinValueParameterMetadata.parameterName);

            kotlinValueParameterMetadata.typeAccept(clazz,
                                                    kotlinClassKindMetadata,
                                                    kotlinConstructorMetadata,
                                                    new TypeConstructor(kmValueParameter));
            kmConstructor.getValueParameters().add(kmValueParameter);
        }

        @Override
        public void visitPropertyValParameter(Clazz                              clazz,
                                              KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                              KotlinPropertyMetadata             kotlinPropertyMetadata,
                                              KotlinValueParameterMetadata       kotlinValueParameterMetadata)
        {
            kmValueParameter = new KmValueParameter(convertValueParameterFlags(kotlinValueParameterMetadata.flags),
                                                    kotlinValueParameterMetadata.parameterName);

            kotlinValueParameterMetadata.typeAccept(clazz,
                                                    kotlinDeclarationContainerMetadata,
                                                    kotlinPropertyMetadata,
                                                    new TypeConstructor(kmValueParameter));
            kmProperty.setSetterParameter(kmValueParameter);
        }

        @Override
        public void visitFunctionValParameter(Clazz                        clazz,
                                              KotlinMetadata               kotlinMetadata,
                                              KotlinFunctionMetadata       kotlinFunctionMetadata,
                                              KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            kmValueParameter = new KmValueParameter(convertValueParameterFlags(kotlinValueParameterMetadata.flags),
                                                    kotlinValueParameterMetadata.parameterName);

            kotlinValueParameterMetadata.typeAccept(clazz,
                                                    kotlinMetadata,
                                                    kotlinFunctionMetadata,
                                                    new TypeConstructor(kmValueParameter));
            kmFunction.getValueParameters().add(kmValueParameter);
        }
    }


    private class TypeConstructor
    implements KotlinTypeVisitor
    {
        private KmType kmType;

        private KmType nestedKmType;
        TypeConstructor(KmType nestedKmType) { this.nestedKmType = nestedKmType; }

        private KmValueParameter kmValueParameter;
        TypeConstructor(KmValueParameter kmValueParameter) { this.kmValueParameter = kmValueParameter; }

        private KmClass kmClass;
        TypeConstructor(KmClass kmClass) { this.kmClass = kmClass; }

        private KmProperty kmProperty;
        TypeConstructor(KmProperty kmProperty) {this.kmProperty = kmProperty; }

        private KmFunction kmFunction;
        TypeConstructor(KmFunction kmFunction) { this.kmFunction = kmFunction; }

        private KmTypeAlias kmTypeAlias;
        TypeConstructor(KmTypeAlias kmTypeAlias) { this.kmTypeAlias = kmTypeAlias; }

        private KmTypeParameter kmTypeParameter;
        TypeConstructor(KmTypeParameter kmTypeParameter) { this.kmTypeParameter = kmTypeParameter; }

        private KmEffectExpression kmEffectExpression;
        TypeConstructor(KmEffectExpression kmEffectExpression) { this.kmEffectExpression = kmEffectExpression; }


        // Implementations for KotlinTypeVisitor.

        @Override
        public void visitTypeUpperBound(Clazz              clazz,
                                        KotlinTypeMetadata boundedType,
                                        KotlinTypeMetadata upperBound)
        {
            kmType = new KmType(convertTypeFlags(boundedType.flags));
            visitAnyType(clazz, upperBound);

            KmFlexibleTypeUpperBound kmFlexibleTypeUpperBound = new KmFlexibleTypeUpperBound(kmType, upperBound.flexibilityID);
            nestedKmType.setFlexibleTypeUpperBound(kmFlexibleTypeUpperBound);
        }

        @Override
        public void visitAbbreviation(Clazz              clazz,
                                      KotlinTypeMetadata abbreviatedType,
                                      KotlinTypeMetadata abbreviation)
        {
            kmType = new KmType(convertTypeFlags(abbreviatedType.flags));
            visitAnyType(clazz, abbreviation);

            nestedKmType.setAbbreviatedType(kmType);
        }

        @Override
        public void visitParameterUpperBound(Clazz                       clazz,
                                             KotlinTypeParameterMetadata boundedTypeParameter,
                                             KotlinTypeMetadata          upperBound)
        {
            kmType = new KmType(convertTypeFlags(upperBound.flags));
            visitAnyType(clazz, upperBound);

            kmTypeParameter.getUpperBounds().add(kmType);
        }

        @Override
        public void visitTypeOfIsExpression(Clazz                          clazz,
                                            KotlinEffectExpressionMetadata kotlinEffectExprMetadata,
                                            KotlinTypeMetadata             typeOfIs)
        {
            kmType = new KmType(convertTypeFlags(typeOfIs.flags));
            visitAnyType(clazz, typeOfIs);

            kmEffectExpression.setInstanceType(kmType);
        }

        @Override
        public void visitTypeArgument(Clazz              clazz,
                                      KotlinTypeMetadata kotlinTypeMetadata,
                                      KotlinTypeMetadata typeArgument)
        {
            kmType = new KmType(convertTypeFlags(typeArgument.flags));
            visitAnyType(clazz, typeArgument);

            KmTypeProjection kmTypeProjection = new KmTypeProjection(toKmVariance(typeArgument.variance), kmType);
            nestedKmType.getArguments().add(kmTypeProjection);
        }

        @Override
        public void visitStarProjection(Clazz              clazz,
                                        KotlinTypeMetadata typeWithStarArg)
        {
            nestedKmType.getArguments().add(KmTypeProjection.STAR);
        }

        @Override
        public void visitOuterClass(Clazz              clazz,
                                    KotlinTypeMetadata innerClass,
                                    KotlinTypeMetadata outerClass)
        {
            kmType = new KmType(convertTypeFlags(outerClass.flags));
            visitAnyType(clazz, outerClass);

            nestedKmType.setOuterType(kmType);
        }


        @Override
        public void visitConstructorValParamType(Clazz                              clazz,
                                                 KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                 KotlinConstructorMetadata          kotlinConstructorMetadata,
                                                 KotlinValueParameterMetadata       kotlinValueParameterMetadata,
                                                 KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmValueParameter.type = kmType;
        }


        @Override
        public void visitConstructorValParamVarArgType(Clazz                              clazz,
                                                       KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                       KotlinConstructorMetadata          kotlinConstructorMetadata,
                                                       KotlinValueParameterMetadata       kotlinValueParameterMetadata,
                                                       KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmValueParameter.type = kmType;
        }


        @Override
        public void visitInlineClassUnderlyingPropertyType(Clazz clazz,
                                                           KotlinClassKindMetadata kotlinMetadata,
                                                           KotlinTypeMetadata kotlinTypeMetadata)
        {
            if (kotlinMetadata.underlyingPropertyName != null)
            {
                kmClass.setInlineClassUnderlyingPropertyName(kotlinMetadata.underlyingPropertyName);
            }
            if (kotlinMetadata.underlyingPropertyType != null)
            {
                kmType = new KmType(convertTypeFlags(kotlinMetadata.underlyingPropertyType.flags));
                kmClass.setInlineClassUnderlyingType(kmType);
            }
            visitAnyType(clazz, kotlinTypeMetadata);
        }


        @Override
        public void visitSuperType(Clazz                   clazz,
                                   KotlinClassKindMetadata kotlinMetadata,
                                   KotlinTypeMetadata      kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmClass.getSupertypes().add(kmType);
        }

        @Override
        public void visitPropertyType(Clazz                              clazz,
                                      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                      KotlinPropertyMetadata             kotlinPropertyMetadata,
                                      KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmProperty.returnType = kmType;
        }

        @Override
        public void visitPropertyReceiverType(Clazz                              clazz,
                                              KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                              KotlinPropertyMetadata             kotlinPropertyMetadata,
                                              KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmProperty.setReceiverParameterType(kmType);
        }

        @Override
        public void visitPropertyValParamType(Clazz                              clazz,
                                              KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                              KotlinPropertyMetadata             kotlinPropertyMetadata,
                                              KotlinValueParameterMetadata       kotlinValueParameterMetadata,
                                              KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmValueParameter.type = kmType;
        }

        @Override
        public void visitPropertyValParamVarArgType(Clazz                              clazz,
                                                    KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                    KotlinPropertyMetadata             kotlinPropertyMetadata,
                                                    KotlinValueParameterMetadata       kotlinValueParameterMetadata,
                                                    KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmValueParameter.setVarargElementType(kmType);
        }

        @Override
        public void visitFunctionReturnType(Clazz                  clazz,
                                            KotlinMetadata         kotlinMetadata,
                                            KotlinFunctionMetadata kotlinFunctionMetadata,
                                            KotlinTypeMetadata     kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmFunction.setReturnType(kmType);
        }

        @Override
        public void visitFunctionReceiverType(Clazz                  clazz,
                                              KotlinMetadata         kotlinMetadata,
                                              KotlinFunctionMetadata kotlinFunctionMetadata,
                                              KotlinTypeMetadata     kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmFunction.setReceiverParameterType(kmType);
        }

        @Override
        public void visitFunctionContextReceiverType(Clazz                  clazz,
                                                     KotlinMetadata         kotlinMetadata,
                                                     KotlinFunctionMetadata kotlinFunctionMetadata,
                                                     KotlinTypeMetadata     kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmFunction.getContextReceiverTypes().add(kmType);
        }

        @Override
        public void visitClassContextReceiverType(Clazz              clazz,
                                                  KotlinMetadata     kotlinMetadata,
                                                  KotlinTypeMetadata kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmClass.getContextReceiverTypes().add(kmType);
        }

        @Override
        public void visitPropertyContextReceiverType(Clazz clazz,
                                                     KotlinMetadata kotlinMetadata,
                                                     KotlinPropertyMetadata kotlinPropertyMetadata,
                                                     KotlinTypeMetadata kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmProperty.getContextReceiverTypes().add(kmType);
        }

        @Override
        public void visitFunctionValParamType(Clazz                        clazz,
                                              KotlinMetadata               kotlinMetadata,
                                              KotlinFunctionMetadata       kotlinFunctionMetadata,
                                              KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                              KotlinTypeMetadata           kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmValueParameter.type = kmType;
        }

        @Override
        public void visitFunctionValParamVarArgType(Clazz                        clazz,
                                                    KotlinMetadata               kotlinMetadata,
                                                    KotlinFunctionMetadata       kotlinFunctionMetadata,
                                                    KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                                    KotlinTypeMetadata           kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmValueParameter.setVarargElementType(kmType);
        }

        @Override
        public void visitAliasUnderlyingType(Clazz                              clazz,
                                             KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                             KotlinTypeAliasMetadata            kotlinTypeAliasMetadata,
                                             KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmTypeAlias.underlyingType = kmType;
        }

        @Override
        public void visitAliasExpandedType(Clazz                              clazz,
                                           KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                           KotlinTypeAliasMetadata            kotlinTypeAliasMetadata,
                                           KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            kmType = new KmType(convertTypeFlags(kotlinTypeMetadata.flags));
            visitAnyType(clazz, kotlinTypeMetadata);

            kmTypeAlias.setExpandedType(kmType);
        }


        // Small helper methods.
        @Override
        public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata)
        {
            if (kotlinTypeMetadata.className != null)
            {
                // Transform the class name back to the Kotlin metadata format.
                String className = kotlinTypeMetadata.className.replace(TypeConstants.  INNER_CLASS_SEPARATOR,
                                                                        KotlinConstants.INNER_CLASS_SEPARATOR);

                KmClassifier.Class classifier = new KmClassifier.Class(className);
                kmType.classifier = classifier;
            }

            if (kotlinTypeMetadata.typeParamID >= 0)
            {
                KmClassifier.TypeParameter classifier = new KmClassifier.TypeParameter(kotlinTypeMetadata.typeParamID);
                kmType.classifier = classifier;
            }

            if (kotlinTypeMetadata.aliasName != null)
            {
                KmClassifier.TypeAlias classifier = new KmClassifier.TypeAlias(kotlinTypeMetadata.aliasName);
                kmType.classifier = classifier;
            }

            kotlinTypeMetadata.abbreviationAccept( clazz, new TypeConstructor(kmType));
            kotlinTypeMetadata.outerClassAccept(   clazz, new TypeConstructor(kmType));
            kotlinTypeMetadata.typeArgumentsAccept(clazz, new TypeConstructor(kmType));
            kotlinTypeMetadata.upperBoundsAccept(  clazz, new TypeConstructor(kmType));

            // Extensions.
            JvmExtensionsKt.setRaw(kmType, kotlinTypeMetadata.isRaw);

            kotlinTypeMetadata.annotationsAccept(clazz,
                                                 new AnnotationConstructor(kmAnnotation -> JvmExtensionsKt.getAnnotations(kmType).add(kmAnnotation)));
        }
    }


    private class TypeParameterConstructor
    implements KotlinTypeParameterVisitor
    {
        private KmTypeParameter kmTypeParameter;

        private KmClass kmClass;
        TypeParameterConstructor(KmClass kmClass) { this.kmClass = kmClass; }

        private KmProperty kmProperty;
        TypeParameterConstructor(KmProperty kmProperty) { this.kmProperty = kmProperty; }

        private KmFunction kmFunction;
        TypeParameterConstructor(KmFunction kmFunction) { this.kmFunction = kmFunction; }

        private KmTypeAlias kmTypeAlias;
        TypeParameterConstructor(KmTypeAlias kmTypeAlias) { this.kmTypeAlias = kmTypeAlias; }


        // Implementations for KotlinTypeParameterVisitor.

        @Override
        public void visitClassTypeParameter(Clazz                       clazz,
                                            KotlinClassKindMetadata     kotlinMetadata,
                                            KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            kmTypeParameter = new KmTypeParameter(convertTypeParameterFlags(kotlinTypeParameterMetadata.flags),
                                                  kotlinTypeParameterMetadata.name,
                                                  kotlinTypeParameterMetadata.id,
                                                  toKmVariance(kotlinTypeParameterMetadata.variance));
            visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);

            kmClass.getTypeParameters().add(kmTypeParameter);
        }

        @Override
        public void visitPropertyTypeParameter(Clazz                              clazz,
                                               KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                               KotlinPropertyMetadata             kotlinPropertyMetadata,
                                               KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
        {
            kmTypeParameter = new KmTypeParameter(convertTypeParameterFlags(kotlinTypeParameterMetadata.flags),
                                                  kotlinTypeParameterMetadata.name,
                                                  kotlinTypeParameterMetadata.id,
                                                  toKmVariance(kotlinTypeParameterMetadata.variance));
            visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);

            kmProperty.getTypeParameters().add(kmTypeParameter);
        }

        @Override
        public void visitFunctionTypeParameter(Clazz                       clazz,
                                               KotlinMetadata              kotlinMetadata,
                                               KotlinFunctionMetadata      kotlinFunctionMetadata,
                                               KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            kmTypeParameter = new KmTypeParameter(convertTypeParameterFlags(kotlinTypeParameterMetadata.flags),
                                                  kotlinTypeParameterMetadata.name,
                                                  kotlinTypeParameterMetadata.id,
                                                  toKmVariance(kotlinTypeParameterMetadata.variance));
            visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);

            kmFunction.getTypeParameters().add(kmTypeParameter);
        }

        @Override
        public void visitAliasTypeParameter(Clazz                              clazz,
                                            KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                            KotlinTypeAliasMetadata            kotlinTypeAliasMetadata,
                                            KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
        {
            kmTypeParameter = new KmTypeParameter(convertTypeParameterFlags(kotlinTypeParameterMetadata.flags),
                                                  kotlinTypeParameterMetadata.name,
                                                  kotlinTypeParameterMetadata.id,
                                                  toKmVariance(kotlinTypeParameterMetadata.variance));
            visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);

            kmTypeAlias.getTypeParameters().add(kmTypeParameter);
        }


        // Small helper methods.
        @Override
        public void visitAnyTypeParameter(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            kotlinTypeParameterMetadata.upperBoundsAccept(clazz,
                                                          new TypeConstructor(kmTypeParameter));

            // Extensions.
            kotlinTypeParameterMetadata.annotationsAccept(clazz,
                                                          new AnnotationConstructor(kmAnnotation -> JvmExtensionsKt.getAnnotations(kmTypeParameter).add(kmAnnotation)));
        }
    }


    /**
     * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin file facade (k == 2) metadata.
     */
    private class KotlinFileFacadeConstructor
    extends    KotlinDeclarationContainerConstructor
    implements KotlinMetadataVisitor
    {
        private KmPackage kmPackage;

        KotlinFileFacadeConstructor()
        {
            this(new KmPackage());
        }

        private KotlinFileFacadeConstructor(KmPackage kmPackage)
        {
            super(kmPackage);
            this.kmPackage = kmPackage;
        }


        // Implementations for KotlinMetadataVisitor.
        @Override
        public void visitKotlinFileFacadeMetadata(Clazz clazz, KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata)
        {
            kotlinFileFacadeKindMetadata.propertiesAccept(clazz, this);
            kotlinFileFacadeKindMetadata.functionsAccept(clazz, this);
            kotlinFileFacadeKindMetadata.typeAliasesAccept(clazz, this);

            for (KotlinPropertyMetadata propertyMetadata : kotlinFileFacadeKindMetadata.localDelegatedProperties)
            {
                JvmExtensionsKt.getLocalDelegatedProperties(kmPackage).add(toKmProperty(propertyMetadata));
            }

            // Finally store the protobuf contents in the fields of the enclosing class.
            Metadata metadata = KotlinClassMetadata.Companion.writeFileFacade(kmPackage,
                                                                              version.toArray(),
                                                                              kotlinFileFacadeKindMetadata.xi).getAnnotationData();

            k  = metadata.k();
            mv = metadata.mv();
            d1 = metadata.d1();
            d2 = metadata.d2();
            xi = metadata.xi();
            xs = metadata.xs();
            pn = metadata.pn();
        }
    }


    /**
     * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin synthetic class (k == 3) metadata.
     */
    private class KotlinSyntheticClassConstructor
    implements KotlinMetadataVisitor,
               KotlinFunctionVisitor
    {
        private KmLambda kmLambda;

        KotlinSyntheticClassConstructor()
        {
            this.kmLambda = new KmLambda();
        }


        // Implementations for KotlinMetadataVisitor.
        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinSyntheticClassMetadata(Clazz clazz, KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata)
        {
            Metadata metadata;
            if (kotlinSyntheticClassKindMetadata.flavor == KotlinSyntheticClassKindMetadata.Flavor.LAMBDA)
            {
                kotlinSyntheticClassKindMetadata.functionsAccept(clazz, this);
                metadata = KotlinClassMetadata.Companion.writeLambda(kmLambda,
                                                                     version.toArray(),
                                                                     kotlinSyntheticClassKindMetadata.xi).getAnnotationData();
            }
            else
            {
                metadata = KotlinClassMetadata.Companion.writeSyntheticClass(version.toArray(),
                                                                             kotlinSyntheticClassKindMetadata.xi).getAnnotationData();
            }

            k  = metadata.k();
            mv = metadata.mv();
            d1 = metadata.d1();
            d2 = metadata.d2();
            xi = metadata.xi();
            xs = metadata.xs();
            pn = metadata.pn();
        }


        // Implementations for KotlinFunctionVisitor.
        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata) {}

        @Override
        public void visitSyntheticFunction(Clazz                            clazz,
                                           KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata,
                                           KotlinFunctionMetadata           kotlinFunctionMetadata)
        {
            KmFunction kmFunction = new KmFunction(convertFunctionFlags(kotlinFunctionMetadata.flags),
                                                   kotlinFunctionMetadata.name);

            kotlinFunctionMetadata.valueParametersAccept(clazz,
                                                         kotlinSyntheticClassKindMetadata,
                                                         new ValueParameterConstructor(kmFunction));
            kotlinFunctionMetadata.returnTypeAccept(clazz,
                                                    kotlinSyntheticClassKindMetadata,
                                                    new TypeConstructor(kmFunction));
            kotlinFunctionMetadata.receiverTypeAccept(clazz,
                                                      kotlinSyntheticClassKindMetadata,
                                                      new TypeConstructor(kmFunction));
            kotlinFunctionMetadata.typeParametersAccept(clazz,
                                                        kotlinSyntheticClassKindMetadata,
                                                        new TypeParameterConstructor(kmFunction));
            kotlinFunctionMetadata.versionRequirementAccept(clazz,
                                                            kotlinSyntheticClassKindMetadata,
                                                            new VersionRequirementConstructor(kmFunction));
            kotlinFunctionMetadata.contractsAccept(clazz,
                                                   kotlinSyntheticClassKindMetadata,
                                                   new ContractConstructor(kmFunction));

            JvmExtensionsKt.setSignature(kmFunction, toKotlinJvmMethodSignature(kotlinFunctionMetadata.jvmSignature));

            if (kotlinFunctionMetadata.lambdaClassOriginName != null)
            {
                JvmExtensionsKt.setLambdaClassOriginName(kmFunction, kotlinFunctionMetadata.lambdaClassOriginName);
            }

            kmLambda.setFunction(kmFunction);
        }
    }


    /**
     * This utility class constructs the d1 array for Kotlin file facade (k == 2) metadata.
     */
    private class KotlinMultiFileFacadeConstructor
    implements KotlinMetadataVisitor
    {
        // Implementations for KotlinMetadataVisitor.
        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinMultiFileFacadeMetadata(Clazz clazz, KotlinMultiFileFacadeKindMetadata kotlinMultiFileFacadeKindMetadata)
        {
            Metadata metadata = KotlinClassMetadata.Companion
                    .writeMultiFileClassFacade(kotlinMultiFileFacadeKindMetadata.partClassNames,
                                               version.toArray(),
                                               kotlinMultiFileFacadeKindMetadata.xi).getAnnotationData();

            k  = metadata.k();
            mv = metadata.mv();
            d1 = metadata.d1();
            d2 = metadata.d2();
            xi = metadata.xi();
            xs = metadata.xs();
            pn = metadata.pn();
        }
    }


    /**
     * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin file facade (k == 2) metadata.
     */
    private class KotlinMultiFilePartConstructor
    extends    KotlinDeclarationContainerConstructor
    implements KotlinMetadataVisitor
    {
        private KmPackage kmPackage;

        KotlinMultiFilePartConstructor()
        {
            this(new KmPackage());
        }

        private KotlinMultiFilePartConstructor(KmPackage kmPackage)
        {
            super(kmPackage);
            this.kmPackage = kmPackage;
        }


        // Implementations for KotlinMetadataVisitor
        @Override
        public void visitKotlinMultiFilePartMetadata(Clazz clazz, KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata)
        {
            kotlinMultiFilePartKindMetadata.propertiesAccept(clazz, this);
            kotlinMultiFilePartKindMetadata.functionsAccept(clazz, this);
            kotlinMultiFilePartKindMetadata.typeAliasesAccept(clazz, this);

            for (KotlinPropertyMetadata propertyMetadata : kotlinMultiFilePartKindMetadata.localDelegatedProperties)
            {
                JvmExtensionsKt.getLocalDelegatedProperties(kmPackage).add(toKmProperty(propertyMetadata));
            }

            // Finally store the protobuf contents in the fields of the enclosing class.
            Metadata metadata = KotlinClassMetadata.Companion.writeMultiFileClassPart(kmPackage,
                                                                                      kotlinMultiFilePartKindMetadata.facadeName,
                                                                                      version.toArray(),
                                                                                      kotlinMultiFilePartKindMetadata.xi).getAnnotationData();

            k  = metadata.k();
            mv = metadata.mv();
            d1 = metadata.d1();
            d2 = metadata.d2();
            xi = metadata.xi();
            xs = metadata.xs();
            pn = metadata.pn();
        }
    }


    private class VersionRequirementConstructor
    implements KotlinVersionRequirementVisitor
    {
        private KmVersionRequirement kmVersionRequirement;

        private KmConstructor kmConstructor;
        VersionRequirementConstructor(KmConstructor kmConstructor) { this.kmConstructor = kmConstructor; }
        private KmClass kmClass;
        VersionRequirementConstructor(KmClass kmClass) { this.kmClass = kmClass; }

        private KmProperty kmProperty;
        VersionRequirementConstructor(KmProperty kmProperty) { this.kmProperty = kmProperty; }

        private KmFunction kmFunction;
        VersionRequirementConstructor(KmFunction kmFunction) { this.kmFunction = kmFunction; }

        private KmTypeAlias kmTypeAlias;
        VersionRequirementConstructor(KmTypeAlias kmTypeAlias) { this.kmTypeAlias = kmTypeAlias; }


        // Implementations for KotlinVersionRequirementVisitor.

        @Override
        public void visitClassVersionRequirement(Clazz                            clazz,
                                                 KotlinMetadata                   kotlinMetadata,
                                                 KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            kmVersionRequirement = new KmVersionRequirement();
            visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

            kmClass.getVersionRequirements().add(kmVersionRequirement);
        }

        @Override
        public void visitConstructorVersionRequirement(Clazz                            clazz,
                                                       KotlinMetadata                   kotlinMetadata,
                                                       KotlinConstructorMetadata        kotlinConstructorMetadata,
                                                       KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            kmVersionRequirement = new KmVersionRequirement();
            visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

            kmConstructor.getVersionRequirements().add(kmVersionRequirement);
        }

        @Override
        public void visitPropertyVersionRequirement(Clazz                              clazz,
                                                    KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                    KotlinPropertyMetadata             kotlinPropertyMetadata,
                                                    KotlinVersionRequirementMetadata   kotlinVersionRequirementMetadata)
        {
            kmVersionRequirement = new KmVersionRequirement();
            visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

            kmProperty.getVersionRequirements().add(kmVersionRequirement);
        }

        @Override
        public void visitFunctionVersionRequirement(Clazz                            clazz,
                                                    KotlinMetadata                   kotlinMetadata,
                                                    KotlinFunctionMetadata           kotlinFunctionMetadata,
                                                    KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            kmVersionRequirement = new KmVersionRequirement();
            visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

            kmFunction.getVersionRequirements().add(kmVersionRequirement);
        }

        public void visitTypeAliasVersionRequirement(Clazz clazz,
                                                     KotlinMetadata                   kotlinMetadata,
                                                     KotlinTypeAliasMetadata          kotlinTypeAliasMetadata,
                                                     KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            kmVersionRequirement = new KmVersionRequirement();
            visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);

            kmTypeAlias.getVersionRequirements().add(kmVersionRequirement);
        }


        // Small helper methods.
        @Override
        public void visitAnyVersionRequirement(Clazz                            clazz,
                                               KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            kmVersionRequirement.kind = toKmVersionRequirementVersionKind(kotlinVersionRequirementMetadata.kind);
            kmVersionRequirement.level = toKmVersionRequirementLevel(kotlinVersionRequirementMetadata.level);
            kmVersionRequirement.setErrorCode(kotlinVersionRequirementMetadata.errorCode);
            kmVersionRequirement.setMessage(kotlinVersionRequirementMetadata.message);

            KmVersion kmVersion = new KmVersion(kotlinVersionRequirementMetadata.major,
                                                kotlinVersionRequirementMetadata.minor,
                                                kotlinVersionRequirementMetadata.patch);
            kmVersionRequirement.setVersion(kmVersion);
        }
    }


    // Conversion helper methods.

    private static JvmMethodSignature toKotlinJvmMethodSignature(MethodSignature jvmMethodSignature)
    {
        if (jvmMethodSignature == null)
        {
            return null;
        }

        return new JvmMethodSignature(jvmMethodSignature.method, jvmMethodSignature.descriptor.toString());
    }


    private static JvmFieldSignature toKotlinJvmFieldSignature(FieldSignature jvmFieldSignature)
    {
        if (jvmFieldSignature == null)
        {
            return null;
        }

        return new JvmFieldSignature(jvmFieldSignature.memberName, jvmFieldSignature.descriptor);
    }


    private static KmVersionRequirementVersionKind toKmVersionRequirementVersionKind(KotlinVersionRequirementVersionKind kotlinVersionRequirementVersionKind)
    {
        switch(kotlinVersionRequirementVersionKind)
        {
            case API_VERSION:      return KmVersionRequirementVersionKind.API_VERSION;
            case COMPILER_VERSION: return KmVersionRequirementVersionKind.COMPILER_VERSION;
            case LANGUAGE_VERSION: return KmVersionRequirementVersionKind.LANGUAGE_VERSION;
            default: throw new UnsupportedOperationException("Encountered unknown enum value for KotlinVersionRequirementVersionKind.");
        }
    }

    private static KmVersionRequirementLevel toKmVersionRequirementLevel(KotlinVersionRequirementLevel kotlinVersionRequirementLevel)
    {
        switch(kotlinVersionRequirementLevel)
        {
            case ERROR:     return KmVersionRequirementLevel.ERROR;
            case HIDDEN:    return KmVersionRequirementLevel.HIDDEN;
            case WARNING:   return KmVersionRequirementLevel.WARNING;
            default: throw new UnsupportedOperationException("Encountered unknown enum value for KotlinVersionRequirementLevel.");
        }
    }


    private static KmEffectType toKmEffectType(KotlinEffectType effectType)
    {
        switch(effectType)
        {
            case CALLS:             return KmEffectType.CALLS;
            case RETURNS_CONSTANT:  return KmEffectType.RETURNS_CONSTANT;
            case RETURNS_NOT_NULL:  return KmEffectType.RETURNS_NOT_NULL;
            default: throw new UnsupportedOperationException("Encountered unknown enum value for KotlinEffectType.");
        }
    }

    private static KmEffectInvocationKind toKmEffectInvocationKind(KotlinEffectInvocationKind invocationKind)
    {
        if (invocationKind == null)
        {
            return null;
        }
        switch(invocationKind)
        {
            case AT_MOST_ONCE:  return KmEffectInvocationKind.AT_MOST_ONCE;
            case EXACTLY_ONCE:  return KmEffectInvocationKind.EXACTLY_ONCE;
            case AT_LEAST_ONCE: return KmEffectInvocationKind.AT_LEAST_ONCE;
            default: throw new UnsupportedOperationException("Encountered unknown enum value for KmEffectInvocationKind.");
        }
    }


    private static KmVariance toKmVariance(KotlinTypeVariance variance)
    {
        switch(variance)
        {
            case IN:        return KmVariance.IN;
            case INVARIANT: return KmVariance.INVARIANT;
            case OUT:       return KmVariance.OUT;
            default:        throw new UnsupportedOperationException("Encountered unknown enum value for KmVariance.");
        }
    }

    private static KmProperty toKmProperty(KotlinPropertyMetadata kotlinPropertyMetadata)
    {
        return new KmProperty(convertPropertyFlags(kotlinPropertyMetadata.flags),
                kotlinPropertyMetadata.name,
                convertPropertyAccessorFlags(kotlinPropertyMetadata.getterFlags),
                convertPropertyAccessorFlags(kotlinPropertyMetadata.setterFlags));
    }

    private static KmFunction toKmFunction(KotlinFunctionMetadata kotlinFunctionMetadata)
    {
        return new KmFunction(convertFunctionFlags(kotlinFunctionMetadata.flags), kotlinFunctionMetadata.name);
    }

    private static KmTypeAlias toKmTypeAlias(KotlinTypeAliasMetadata kotlinTypeAliasMetadata)
    {
        return new KmTypeAlias(convertTypeAliasFlags(kotlinTypeAliasMetadata.flags), kotlinTypeAliasMetadata.name);
    }


    // Flag conversion helper methods.

    private static Set<Flag> convertCommonFlags(KotlinCommonFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        if (flags.hasAnnotations) flagSet.add(Flag.HAS_ANNOTATIONS);

        return flagSet;
    }


    private static Set<Flag> convertVisibilityFlags(KotlinVisibilityFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        if (flags.isInternal)      flagSet.add(Flag.IS_INTERNAL);
        if (flags.isLocal)         flagSet.add(Flag.IS_LOCAL);
        if (flags.isPrivate)       flagSet.add(Flag.IS_PRIVATE);
        if (flags.isProtected)     flagSet.add(Flag.IS_PROTECTED);
        if (flags.isPublic)        flagSet.add(Flag.IS_PUBLIC);
        if (flags.isPrivateToThis) flagSet.add(Flag.IS_PRIVATE_TO_THIS);

        return flagSet;
    }


    private static Set<Flag> convertModalityFlags(KotlinModalityFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        if (flags.isAbstract) flagSet.add(Flag.IS_ABSTRACT);
        if (flags.isFinal)    flagSet.add(Flag.IS_FINAL);
        if (flags.isOpen)     flagSet.add(Flag.IS_OPEN);
        if (flags.isSealed)   flagSet.add(Flag.IS_SEALED);

        return flagSet;
    }


    private static int convertFunctionFlags(KotlinFunctionFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));
        flagSet.addAll(convertVisibilityFlags(flags.visibility));
        flagSet.addAll(convertModalityFlags(flags.modality));

        if (flags.isDeclaration)  flagSet.add(Flag.Function.IS_DECLARATION);
        if (flags.isFakeOverride) flagSet.add(Flag.Function.IS_FAKE_OVERRIDE);
        if (flags.isDelegation)   flagSet.add(Flag.Function.IS_DELEGATION);
        if (flags.isSynthesized)  flagSet.add(Flag.Function.IS_SYNTHESIZED);
        if (flags.isOperator)     flagSet.add(Flag.Function.IS_OPERATOR);
        if (flags.isInfix)        flagSet.add(Flag.Function.IS_INFIX);
        if (flags.isInline)       flagSet.add(Flag.Function.IS_INLINE);
        if (flags.isTailrec)      flagSet.add(Flag.Function.IS_TAILREC);
        if (flags.isExternal)     flagSet.add(Flag.Function.IS_EXTERNAL);
        if (flags.isSuspend)      flagSet.add(Flag.Function.IS_SUSPEND);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private static int convertTypeFlags(KotlinTypeFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));

        if (flags.isNullable)          flagSet.add(Flag.Type.IS_NULLABLE);
        if (flags.isSuspend)           flagSet.add(Flag.Type.IS_SUSPEND);
        if (flags.isDefinitelyNonNull) flagSet.add(Flag.Type.IS_DEFINITELY_NON_NULL);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private static int convertTypeParameterFlags(KotlinTypeParameterFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));

        if (flags.isReified) flagSet.add(Flag.TypeParameter.IS_REIFIED);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private static int convertTypeAliasFlags(KotlinTypeAliasFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));
        flagSet.addAll(convertVisibilityFlags(flags.visibility));

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private static int convertPropertyFlags(KotlinPropertyFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));
        flagSet.addAll(convertVisibilityFlags(flags.visibility));
        flagSet.addAll(convertModalityFlags(flags.modality));

        if (flags.isDeclared)     flagSet.add(Flag.Property.IS_DECLARATION);
        if (flags.isFakeOverride) flagSet.add(Flag.Property.IS_FAKE_OVERRIDE);
        if (flags.isDelegation)   flagSet.add(Flag.Property.IS_DELEGATION);
        if (flags.isSynthesized)  flagSet.add(Flag.Property.IS_SYNTHESIZED);
        if (flags.isVar)          flagSet.add(Flag.Property.IS_VAR);
        if (flags.hasGetter)      flagSet.add(Flag.Property.HAS_GETTER);
        if (flags.hasSetter)      flagSet.add(Flag.Property.HAS_SETTER);
        if (flags.isConst)        flagSet.add(Flag.Property.IS_CONST);
        if (flags.isLateinit)     flagSet.add(Flag.Property.IS_LATEINIT);
        if (flags.hasConstant)    flagSet.add(Flag.Property.HAS_CONSTANT);
        if (flags.isExternal)     flagSet.add(Flag.Property.IS_EXTERNAL);
        if (flags.isDelegated)    flagSet.add(Flag.Property.IS_DELEGATED);
        if (flags.isExpect)       flagSet.add(Flag.Property.IS_EXPECT);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private static int convertPropertyJvmFlags(KotlinPropertyFlags flags)
    {
        return flags.isMovedFromInterfaceCompanion ?
            flagsOf(JvmFlag.Property.IS_MOVED_FROM_INTERFACE_COMPANION) :
            0;
    }

    private static int convertClassJvmFlags(KotlinClassFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        if (flags.isCompiledInCompatibilityMode) flagSet.add(JvmFlag.Class.IS_COMPILED_IN_COMPATIBILITY_MODE);
        if (flags.hasMethodBodiesInInterface)    flagSet.add(JvmFlag.Class.HAS_METHOD_BODIES_IN_INTERFACE);

        return flagsOf(flagSet.toArray(new Flag[0]));

    }


    private static int convertPropertyAccessorFlags(KotlinPropertyAccessorFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));
        flagSet.addAll(convertVisibilityFlags(flags.visibility));
        flagSet.addAll(convertModalityFlags(flags.modality));

        if (! flags.isDefault) flagSet.add(Flag.PropertyAccessor.IS_NOT_DEFAULT);
        if (flags.isInline)    flagSet.add(Flag.PropertyAccessor.IS_INLINE);
        if (flags.isExternal)  flagSet.add(Flag.PropertyAccessor.IS_EXTERNAL);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private static int convertValueParameterFlags(KotlinValueParameterFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));

        if (flags.hasDefaultValue) flagSet.add(Flag.ValueParameter.DECLARES_DEFAULT_VALUE);
        if (flags.isNoInline)      flagSet.add(Flag.ValueParameter.IS_NOINLINE);
        if (flags.isCrossInline)   flagSet.add(Flag.ValueParameter.IS_CROSSINLINE);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private static int convertEffectExpressionFlags(KotlinEffectExpressionFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        if (flags.isNullCheckPredicate) flagSet.add(Flag.EffectExpression.IS_NULL_CHECK_PREDICATE);
        if (flags.isNegated)            flagSet.add(Flag.EffectExpression.IS_NEGATED);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }
}
