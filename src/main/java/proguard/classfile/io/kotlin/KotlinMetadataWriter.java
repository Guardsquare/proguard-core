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

import kotlinx.metadata.*;
import kotlinx.metadata.jvm.JvmFieldSignature;
import kotlinx.metadata.jvm.JvmMethodSignature;
import kotlinx.metadata.jvm.*;
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
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.flags.*;
import proguard.classfile.kotlin.visitor.*;
import proguard.classfile.util.WarningPrinter;
import proguard.classfile.util.kotlin.AnnotationConstructor;
import proguard.classfile.util.kotlin.KotlinMetadataInitializer.MetadataType;
import proguard.classfile.visitor.ClassVisitor;

import java.util.*;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.joining;
import static kotlinx.metadata.FlagsKt.flagsOf;
import static kotlinx.metadata.jvm.KotlinClassHeader.COMPATIBLE_METADATA_VERSION;
import static proguard.classfile.kotlin.KotlinConstants.*;

/**
 * This class visitor writes the information stored in a Clazz's kotlinMetadata field
 * to a @kotlin/Metadata annotation on the class.
 */
public class KotlinMetadataWriter
implements ClassVisitor,
           KotlinMetadataVisitor,

           // Implementation interfaces.
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

    private ConstantPoolEditor constantPoolEditor;

    private static ConstantPoolShrinker constantPoolShrinker = new ConstantPoolShrinker();

    private MetadataType currentType;

    private final BiConsumer<Clazz, String> errorHandler;

    private boolean hasVisitedAny = false;

    public KotlinMetadataWriter(WarningPrinter warningPrinter)
    {
        this(warningPrinter, null);
    }

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

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        clazz.kotlinMetadataAccept(this);
    }



    // Implementations for KotlinMetadataVisitor.
    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
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
        KotlinClassMetadata md = KotlinClassMetadata.read(new KotlinClassHeader(k, mv, d1, d2, xs, pn, xi));
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
        this.hasVisitedAny      = false;

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
        this.hasVisitedAny = true;
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
        this.hasVisitedAny = true;
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

    private class ContractConstructor
    implements KotlinContractVisitor
    {
        private KmFunctionVisitor kmdFunctionVisitor;


        ContractConstructor(KmFunctionVisitor kmdFunctionVisitor)
        {
            this.kmdFunctionVisitor = kmdFunctionVisitor;
        }

        // Implementations for KotlinContractVisitor.
        @Override
        public void visitContract(Clazz                  clazz,
                                  KotlinMetadata         kotlinMetadata,
                                  KotlinFunctionMetadata kotlinFunctionMetadata,
                                  KotlinContractMetadata kotlinContractMetadata)
        {
            KmContractVisitor kmContractVisitor = kmdFunctionVisitor.visitContract();

            kotlinContractMetadata.effectsAccept(clazz,
                                                 kotlinMetadata,
                                                 kotlinFunctionMetadata,
                                                 new EffectConstructor(kmContractVisitor));

            kmContractVisitor.visitEnd();
        }
    }

    private class EffectConstructor
    implements KotlinEffectVisitor
    {
        private final KmContractVisitor kmContractVisitor;
        private EffectConstructor(KmContractVisitor kmContractVisitor) { this.kmContractVisitor = kmContractVisitor; }


        // Implementations for KotlinEffectVisitor.
        @Override
        public void visitEffect(Clazz                  clazz,
                                KotlinMetadata         kotlinMetadata,
                                KotlinFunctionMetadata kotlinFunctionMetadata,
                                KotlinContractMetadata kotlinContractMetadata,
                                KotlinEffectMetadata   kotlinEffectMetadata)
        {
            KmEffectVisitor kmEffectVisitor = kmContractVisitor.visitEffect(toKmEffectType(kotlinEffectMetadata.effectType),
                                                                            toKmEffectInvocationKind(kotlinEffectMetadata.invocationKind));

            kotlinEffectMetadata.conclusionOfConditionalEffectAccept(clazz,
                                                                     new EffectExprConstructor(kmEffectVisitor));

            kotlinEffectMetadata.constructorArgumentAccept(clazz,
                                                           new EffectExprConstructor(kmEffectVisitor));

            kmEffectVisitor.visitEnd();
        }
    }

    private class EffectExprConstructor
    implements KotlinEffectExprVisitor
    {
        private KmEffectExpressionVisitor effectExprVis;

        private KmEffectVisitor effectVis;
        private EffectExprConstructor(KmEffectVisitor effectVis) { this.effectVis = effectVis; }

        private KmEffectExpressionVisitor nestedExprVis;
        private EffectExprConstructor(KmEffectExpressionVisitor nestedExprVis) { this.nestedExprVis = nestedExprVis; }


        // Implementations for KotlinEffectExprVisitor.
        @Override
        public void visitAnyEffectExpression(Clazz                          clazz,
                                             KotlinEffectMetadata           kotlinEffectMetadata,
                                             KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata)
        {
            effectExprVis.visit(convertEffectExpressionFlags(kotlinEffectExpressionMetadata.flags),
                                kotlinEffectExpressionMetadata.parameterIndex);

            if (kotlinEffectExpressionMetadata.hasConstantValue)
            {
                effectExprVis.visitConstantValue(kotlinEffectExpressionMetadata.constantValue);
            }

            kotlinEffectExpressionMetadata.andRightHandSideAccept(clazz,
                                                                  kotlinEffectMetadata,
                                                                  new EffectExprConstructor(effectExprVis));
            kotlinEffectExpressionMetadata.orRightHandSideAccept(clazz,
                                                                 kotlinEffectMetadata,
                                                                 new EffectExprConstructor(effectExprVis));

            kotlinEffectExpressionMetadata.typeOfIsAccept(clazz,
                                                          new TypeConstructor(effectExprVis));

            effectExprVis.visitEnd();
        }

        @Override
        public void visitAndRHSExpression(Clazz                          clazz,
                                          KotlinEffectMetadata           kotlinEffectMetadata,
                                          KotlinEffectExpressionMetadata lhs,
                                          KotlinEffectExpressionMetadata rhs)
        {
            effectExprVis = nestedExprVis.visitAndArgument();

            visitAnyEffectExpression(clazz, kotlinEffectMetadata, rhs);
        }

        @Override
        public void visitOrRHSExpression(Clazz                          clazz,
                                         KotlinEffectMetadata           kotlinEffectMetadata,
                                         KotlinEffectExpressionMetadata lhs,
                                         KotlinEffectExpressionMetadata rhs)
        {
            effectExprVis = nestedExprVis.visitOrArgument();

            visitAnyEffectExpression(clazz, kotlinEffectMetadata, rhs);
        }

        @Override
        public void visitConstructorArgExpression(Clazz                          clazz,
                                                  KotlinEffectMetadata           kotlinEffectMetadata,
                                                  KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata)
        {
            effectExprVis = effectVis.visitConstructorArgument();

            visitAnyEffectExpression(clazz, kotlinEffectMetadata, kotlinEffectExpressionMetadata);
        }

        @Override
        public void visitConclusionExpression(Clazz                          clazz,
                                              KotlinEffectMetadata           kotlinEffectMetadata,
                                              KotlinEffectExpressionMetadata kotlinEffectExpressionMetadata)
        {
            effectExprVis = effectVis.visitConclusionOfConditionalEffect();

            visitAnyEffectExpression(clazz, kotlinEffectMetadata, kotlinEffectExpressionMetadata);
        }
    }

    private class KotlinDeclarationContainerConstructor
    implements KotlinPropertyVisitor,
               KotlinFunctionVisitor,
               KotlinTypeAliasVisitor
    {
        KmDeclarationContainerVisitor kmdWriter;

        KmPropertyVisitor                       kmdPropertyVisitor;
        JvmDeclarationContainerExtensionVisitor extensionVisitor;

        KotlinDeclarationContainerConstructor(KmDeclarationContainerVisitor classKmdWriter)
        {
            kmdWriter = classKmdWriter;
        }


        // Simplifications for KotlinPropertyVisitor.
        @Override
        public void visitAnyProperty(Clazz                              clazz,
                                     KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                     KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            kotlinPropertyMetadata.typeAccept(clazz,
                                              kotlinDeclarationContainerMetadata,
                                              new TypeConstructor(kmdPropertyVisitor));
            kotlinPropertyMetadata.receiverTypeAccept(clazz,
                                                      kotlinDeclarationContainerMetadata,
                                                      new TypeConstructor(kmdPropertyVisitor));
            kotlinPropertyMetadata.setterParametersAccept(clazz,
                                                          kotlinDeclarationContainerMetadata,
                                                          new ValueParameterConstructor(kmdPropertyVisitor));
            kotlinPropertyMetadata.typeParametersAccept(clazz,
                                                        kotlinDeclarationContainerMetadata,
                                                        new TypeParameterConstructor(kmdPropertyVisitor));
            kotlinPropertyMetadata.versionRequirementAccept(clazz,
                                                            kotlinDeclarationContainerMetadata,
                                                            new VersionRequirementConstructor(kmdPropertyVisitor));

            JvmPropertyExtensionVisitor ext =
                (JvmPropertyExtensionVisitor) kmdPropertyVisitor.visitExtensions(JvmPropertyExtensionVisitor.TYPE);

            JvmMethodSignature getterSignature = toKotlinJvmMethodSignature(kotlinPropertyMetadata.getterSignature);
            JvmMethodSignature setterSignature = toKotlinJvmMethodSignature(kotlinPropertyMetadata.setterSignature);
            JvmFieldSignature backingFieldSignature = toKotlinJvmFieldSignature(kotlinPropertyMetadata.backingFieldSignature);

            ext.visit(convertPropertyJvmFlags(kotlinPropertyMetadata.flags),
                      backingFieldSignature,
                      getterSignature,
                      setterSignature);

            if (kotlinPropertyMetadata.syntheticMethodForAnnotations != null)
            {
                ext.visitSyntheticMethodForAnnotations(
                        toKotlinJvmMethodSignature(kotlinPropertyMetadata.syntheticMethodForAnnotations)
                );
            }

            ext.visitEnd();

            kmdPropertyVisitor.visitEnd();
        }

        @Override
        public void visitProperty(Clazz                              clazz,
                                  KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                  KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            kmdPropertyVisitor =
                kmdWriter.visitProperty(convertPropertyFlags(kotlinPropertyMetadata.flags),
                                        kotlinPropertyMetadata.name,
                                        convertPropertyAccessorFlags(kotlinPropertyMetadata.getterFlags),
                                        convertPropertyAccessorFlags(kotlinPropertyMetadata.setterFlags));

            visitAnyProperty(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata);
        }

        @Override
        public void visitDelegatedProperty(Clazz                              clazz,
                                           KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                           KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            kmdPropertyVisitor =
                extensionVisitor.visitLocalDelegatedProperty(convertPropertyFlags(kotlinPropertyMetadata.flags),
                                                             kotlinPropertyMetadata.name,
                                                             convertPropertyAccessorFlags(kotlinPropertyMetadata.getterFlags),
                                                             convertPropertyAccessorFlags(kotlinPropertyMetadata.setterFlags));

            visitAnyProperty(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata);
        }


        // Simplifications for KotlinFunctionVisitor.
        @Override
        public void visitAnyFunction(Clazz clazz, KotlinMetadata kotlinMetadata, KotlinFunctionMetadata kotlinFunctionMetadata) {}

        @Override
        public void visitFunction(Clazz                              clazz,
                                  KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                  KotlinFunctionMetadata             kotlinFunctionMetadata)
        {
            KmFunctionVisitor kmdFunctionVisitor =
                kmdWriter.visitFunction(convertFunctionFlags(kotlinFunctionMetadata.flags),
                                        kotlinFunctionMetadata.name);

            kotlinFunctionMetadata.valueParametersAccept(clazz,
                                                         kotlinDeclarationContainerMetadata,
                                                         new ValueParameterConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.returnTypeAccept(clazz,
                                                    kotlinDeclarationContainerMetadata,
                                                    new TypeConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.receiverTypeAccept(clazz,
                                                      kotlinDeclarationContainerMetadata,
                                                      new TypeConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.typeParametersAccept(clazz,
                                                        kotlinDeclarationContainerMetadata,
                                                        new TypeParameterConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.versionRequirementAccept(clazz,
                                                            kotlinDeclarationContainerMetadata,
                                                            new VersionRequirementConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.contractsAccept(clazz,
                                                   kotlinDeclarationContainerMetadata,
                                                   new ContractConstructor(kmdFunctionVisitor));

            JvmFunctionExtensionVisitor ext =
                (JvmFunctionExtensionVisitor) kmdFunctionVisitor.visitExtensions(JvmFunctionExtensionVisitor.TYPE);

            JvmMethodSignature jvmMethodSignature = toKotlinJvmMethodSignature(kotlinFunctionMetadata.jvmSignature);

            ext.visit(jvmMethodSignature);

            if (kotlinFunctionMetadata.lambdaClassOriginName != null)
            {
                ext.visitLambdaClassOriginName(kotlinFunctionMetadata.lambdaClassOriginName);
            }
            ext.visitEnd();

            kmdFunctionVisitor.visitEnd();
        }


        // Implementations for KotlinTypeAliasVisitor
        @Override
        public void visitTypeAlias(Clazz                              clazz,
                                   KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                   KotlinTypeAliasMetadata            kotlinTypeAliasMetadata)
        {
            KmTypeAliasVisitor kmdAliasVisitor =
                kmdWriter.visitTypeAlias(convertTypeAliasFlags(kotlinTypeAliasMetadata.flags),
                                         kotlinTypeAliasMetadata.name);

            kotlinTypeAliasMetadata.typeParametersAccept(clazz,
                                                         kotlinDeclarationContainerMetadata,
                                                         new TypeParameterConstructor(kmdAliasVisitor));
            kotlinTypeAliasMetadata.underlyingTypeAccept(clazz,
                                                         kotlinDeclarationContainerMetadata,
                                                         new TypeConstructor(kmdAliasVisitor));
            kotlinTypeAliasMetadata.expandedTypeAccept(clazz,
                                                       kotlinDeclarationContainerMetadata,
                                                       new TypeConstructor(kmdAliasVisitor));
            kotlinTypeAliasMetadata.versionRequirementAccept(clazz,
                                                             kotlinDeclarationContainerMetadata,
                                                             new VersionRequirementConstructor(kmdAliasVisitor));
            kotlinTypeAliasMetadata.annotationsAccept(clazz,
                                                      new AnnotationConstructor(kmdAliasVisitor::visitAnnotation));

            kmdAliasVisitor.visitEnd();
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

               // Implementation interfaces.
               KotlinConstructorVisitor
    {
        KotlinClassMetadata.Class.Writer classKmdWriter;

        KotlinClassConstructor()
        {
            this(new KotlinClassMetadata.Class.Writer());
        }

        private KotlinClassConstructor(KotlinClassMetadata.Class.Writer classKmdWriter)
        {
            super(classKmdWriter);
            this.classKmdWriter = classKmdWriter;
        }


        // Implementations for KotlinMetadataVisitor.
        @Override
        public void visitKotlinClassMetadata(Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata)
        {
            classKmdWriter.visit(convertClassFlags(kotlinClassKindMetadata.flags),
                                 kotlinClassKindMetadata.className.replace('$','.'));

            if (kotlinClassKindMetadata.companionObjectName != null)
            {
                classKmdWriter.visitCompanionObject(kotlinClassKindMetadata.companionObjectName);
            }

            kotlinClassKindMetadata.propertiesAccept(clazz,  this);
            kotlinClassKindMetadata.functionsAccept(clazz,   this);
            kotlinClassKindMetadata.typeAliasesAccept(clazz, this);

            for (String enumEntry : kotlinClassKindMetadata.enumEntryNames)
            {
                classKmdWriter.visitEnumEntry(enumEntry);
            }

            for (String nestedClass : kotlinClassKindMetadata.nestedClassNames)
            {
                classKmdWriter.visitNestedClass(nestedClass);
            }

            for (String sealedSubClass : kotlinClassKindMetadata.sealedSubclassNames)
            {
                classKmdWriter.visitSealedSubclass(sealedSubClass.replace('$', '.'));
            }

            kotlinClassKindMetadata.constructorsAccept(                     clazz, this);
            kotlinClassKindMetadata.superTypesAccept(                       clazz, new TypeConstructor(classKmdWriter));
            kotlinClassKindMetadata.typeParametersAccept(                   clazz, new TypeParameterConstructor(classKmdWriter));
            kotlinClassKindMetadata.versionRequirementAccept(               clazz, new VersionRequirementConstructor(classKmdWriter));
            kotlinClassKindMetadata.inlineClassUnderlyingPropertyTypeAccept(clazz, new TypeConstructor(classKmdWriter));

            // Extensions.
            JvmClassExtensionVisitor ext =
                (JvmClassExtensionVisitor) classKmdWriter.visitExtensions(JvmClassExtensionVisitor.TYPE);

            extensionVisitor = ext;
            kotlinClassKindMetadata.delegatedPropertiesAccept(clazz, this);

            if (kotlinClassKindMetadata.anonymousObjectOriginName != null)
            {
                ext.visitAnonymousObjectOriginName(kotlinClassKindMetadata.anonymousObjectOriginName);
            }

            ext.visitEnd();

            // Finish.
            classKmdWriter.visitEnd();

            // Finally store the protobuf contents in the fields of the enclosing class.
            KotlinClassHeader header = classKmdWriter.write(COMPATIBLE_METADATA_VERSION,
                                                            kotlinClassKindMetadata.xi).getHeader();

            k  = header.getKind();
            mv = header.getMetadataVersion();
            d1 = header.getData1();
            d2 = header.getData2();
            xi = header.getExtraInt();
            xs = header.getExtraString();
            pn = header.getPackageName();
        }


        // Implementations for KotlinConstructorVisitor.
        @Override
        public void visitConstructor(Clazz                     clazz,
                                     KotlinClassKindMetadata   kotlinClassKindMetadata,
                                     KotlinConstructorMetadata kotlinConstructorMetadata)
        {
            KmConstructorVisitor constructorVis =
                classKmdWriter.visitConstructor(convertConstructorFlags(kotlinConstructorMetadata.flags));

            kotlinConstructorMetadata.valueParametersAccept(clazz,
                                                            kotlinClassKindMetadata,
                                                            new ValueParameterConstructor(constructorVis));

            kotlinConstructorMetadata.versionRequirementAccept(clazz,
                                                               kotlinClassKindMetadata,
                                                               new VersionRequirementConstructor(constructorVis));

            // Extensions.
            if (kotlinConstructorMetadata.jvmSignature != null)
            {
                JvmConstructorExtensionVisitor constExtVis =
                    (JvmConstructorExtensionVisitor)constructorVis.visitExtensions(JvmConstructorExtensionVisitor.TYPE);

                JvmMethodSignature jvmMethodSignature = toKotlinJvmMethodSignature(kotlinConstructorMetadata.jvmSignature);

                constExtVis.visit(jvmMethodSignature);
            }

            // Finish.
            constructorVis.visitEnd();
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
        private KmValueParameterVisitor valParamVis;

        private KmConstructorVisitor constructorVis;
        ValueParameterConstructor(KmConstructorVisitor constructorVis) { this.constructorVis = constructorVis; }

        private KmPropertyVisitor propertyVis;
        ValueParameterConstructor(KmPropertyVisitor propertyVis) { this.propertyVis = propertyVis; }

        private KmFunctionVisitor functionVis;
        ValueParameterConstructor(KmFunctionVisitor functionVis) { this.functionVis = functionVis; }


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
            valParamVis =
                constructorVis.visitValueParameter(convertValueParameterFlags(kotlinValueParameterMetadata.flags),
                                                   kotlinValueParameterMetadata.parameterName);

            kotlinValueParameterMetadata.typeAccept(clazz,
                                                    kotlinClassKindMetadata,
                                                    kotlinConstructorMetadata,
                                                    new TypeConstructor(valParamVis));

            valParamVis.visitEnd();
        }

        @Override
        public void visitPropertyValParameter(Clazz                              clazz,
                                              KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                              KotlinPropertyMetadata             kotlinPropertyMetadata,
                                              KotlinValueParameterMetadata       kotlinValueParameterMetadata)
        {
            valParamVis =
                propertyVis.visitSetterParameter(convertValueParameterFlags(kotlinValueParameterMetadata.flags),
                                                 kotlinValueParameterMetadata.parameterName);

            kotlinValueParameterMetadata.typeAccept(clazz,
                                                    kotlinDeclarationContainerMetadata,
                                                    kotlinPropertyMetadata,
                                                    new TypeConstructor(valParamVis));

            valParamVis.visitEnd();
        }

        @Override
        public void visitFunctionValParameter(Clazz                        clazz,
                                              KotlinMetadata               kotlinMetadata,
                                              KotlinFunctionMetadata       kotlinFunctionMetadata,
                                              KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            valParamVis =
                functionVis.visitValueParameter(convertValueParameterFlags(kotlinValueParameterMetadata.flags),
                                                kotlinValueParameterMetadata.parameterName);

            kotlinValueParameterMetadata.typeAccept(clazz,
                                                    kotlinMetadata,
                                                    kotlinFunctionMetadata,
                                                    new TypeConstructor(valParamVis));

            valParamVis.visitEnd();
        }
    }


    private class TypeConstructor
    implements KotlinTypeVisitor
    {
        private KmTypeVisitor typeVis;

        private KmTypeVisitor nestedTypeVis;
        TypeConstructor(KmTypeVisitor nestedTypeVis) { this.nestedTypeVis = nestedTypeVis; }

        private KmValueParameterVisitor valParamVis;
        TypeConstructor(KmValueParameterVisitor valParamVis) { this.valParamVis = valParamVis; }

        private KmClassVisitor classVis;
        TypeConstructor(KmClassVisitor classVis) { this.classVis = classVis; }

        private KmPropertyVisitor propertyVis;
        TypeConstructor(KmPropertyVisitor propertyVis) { this.propertyVis = propertyVis; }

        private KmFunctionVisitor functionVis;
        TypeConstructor(KmFunctionVisitor functionVis) { this.functionVis = functionVis; }

        private KmTypeAliasVisitor aliasVis;
        TypeConstructor(KmTypeAliasVisitor aliasVis) { this.aliasVis = aliasVis; }

        private KmTypeParameterVisitor typeParamVis;
        TypeConstructor(KmTypeParameterVisitor typeParamVis) { this.typeParamVis = typeParamVis; }

        private KmEffectExpressionVisitor effectExpressionVis;
        TypeConstructor(KmEffectExpressionVisitor effectExpressionVis) { this.effectExpressionVis = effectExpressionVis; }


        // Implementations for KotlinTypeVisitor.

        @Override
        public void visitTypeUpperBound(Clazz              clazz,
                                        KotlinTypeMetadata boundedType,
                                        KotlinTypeMetadata upperBound)
        {
            typeVis = nestedTypeVis.visitFlexibleTypeUpperBound(convertTypeFlags(boundedType.flags), upperBound.flexibilityID);

            visitAnyType(clazz, upperBound);
        }

        @Override
        public void visitAbbreviation(Clazz              clazz,
                                      KotlinTypeMetadata abbreviatedType,
                                      KotlinTypeMetadata abbreviation)
        {
            typeVis = nestedTypeVis.visitAbbreviatedType(convertTypeFlags(abbreviatedType.flags));

            visitAnyType(clazz, abbreviation);
        }

        @Override
        public void visitParameterUpperBound(Clazz                       clazz,
                                             KotlinTypeParameterMetadata boundedTypeParameter,
                                             KotlinTypeMetadata          upperBound)
        {
            typeVis = typeParamVis.visitUpperBound(convertTypeFlags(upperBound.flags));

            visitAnyType(clazz, upperBound);
        }

        @Override
        public void visitTypeOfIsExpression(Clazz                          clazz,
                                            KotlinEffectExpressionMetadata kotlinEffectExprMetadata,
                                            KotlinTypeMetadata             typeOfIs)
        {
            typeVis = effectExpressionVis.visitIsInstanceType(convertTypeFlags(typeOfIs.flags));

            visitAnyType(clazz, typeOfIs);
        }

        @Override
        public void visitTypeArgument(Clazz              clazz,
                                      KotlinTypeMetadata kotlinTypeMetadata,
                                      KotlinTypeMetadata typeArgument)
        {
            typeVis = nestedTypeVis.visitArgument(convertTypeFlags(typeArgument.flags), toKmVariance(typeArgument.variance));

            visitAnyType(clazz, typeArgument);
        }

        @Override
        public void visitStarProjection(Clazz              clazz,
                                        KotlinTypeMetadata typeWithStarArg)
        {
            nestedTypeVis.visitStarProjection();
        }

        @Override
        public void visitOuterClass(Clazz              clazz,
                                    KotlinTypeMetadata innerClass,
                                    KotlinTypeMetadata outerClass)
        {
            typeVis = nestedTypeVis.visitOuterType(convertTypeFlags(outerClass.flags));

            visitAnyType(clazz, outerClass);
        }


        @Override
        public void visitConstructorValParamType(Clazz                              clazz,
                                                 KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                 KotlinConstructorMetadata          kotlinConstructorMetadata,
                                                 KotlinValueParameterMetadata       kotlinValueParameterMetadata,
                                                 KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            typeVis = valParamVis.visitType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }


        @Override
        public void visitConstructorValParamVarArgType(Clazz                              clazz,
                                                       KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                       KotlinConstructorMetadata          kotlinConstructorMetadata,
                                                       KotlinValueParameterMetadata       kotlinValueParameterMetadata,
                                                       KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            typeVis = valParamVis.visitType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }


        @Override
        public void visitInlineClassUnderlyingPropertyType(Clazz clazz,
                                                           KotlinClassKindMetadata kotlinMetadata,
                                                           KotlinTypeMetadata kotlinTypeMetadata)
        {
            if (kotlinMetadata.underlyingPropertyName != null)
            {
                classVis.visitInlineClassUnderlyingPropertyName(kotlinMetadata.underlyingPropertyName);
            }
            if (kotlinMetadata.underlyingPropertyType != null)
            {
                typeVis = classVis.visitInlineClassUnderlyingType(convertTypeFlags(kotlinMetadata.underlyingPropertyType.flags));
            }
            visitAnyType(clazz, kotlinTypeMetadata);
        }


        @Override
        public void visitSuperType(Clazz                   clazz,
                                   KotlinClassKindMetadata kotlinMetadata,
                                   KotlinTypeMetadata      kotlinTypeMetadata)
        {
            typeVis = classVis.visitSupertype(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitPropertyType(Clazz                              clazz,
                                      KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                      KotlinPropertyMetadata             kotlinPropertyMetadata,
                                      KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            typeVis = propertyVis.visitReturnType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitPropertyReceiverType(Clazz                              clazz,
                                              KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                              KotlinPropertyMetadata             kotlinPropertyMetadata,
                                              KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            typeVis = propertyVis.visitReceiverParameterType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitPropertyValParamType(Clazz                              clazz,
                                              KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                              KotlinPropertyMetadata             kotlinPropertyMetadata,
                                              KotlinValueParameterMetadata       kotlinValueParameterMetadata,
                                              KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            typeVis = valParamVis.visitType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitPropertyValParamVarArgType(Clazz                              clazz,
                                                    KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                    KotlinPropertyMetadata             kotlinPropertyMetadata,
                                                    KotlinValueParameterMetadata       kotlinValueParameterMetadata,
                                                    KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            typeVis = valParamVis.visitVarargElementType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitFunctionReturnType(Clazz                  clazz,
                                            KotlinMetadata         kotlinMetadata,
                                            KotlinFunctionMetadata kotlinFunctionMetadata,
                                            KotlinTypeMetadata     kotlinTypeMetadata)
        {
            typeVis = functionVis.visitReturnType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitFunctionReceiverType(Clazz                  clazz,
                                              KotlinMetadata         kotlinMetadata,
                                              KotlinFunctionMetadata kotlinFunctionMetadata,
                                              KotlinTypeMetadata     kotlinTypeMetadata)
        {
            typeVis = functionVis.visitReceiverParameterType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitFunctionValParamType(Clazz                        clazz,
                                              KotlinMetadata               kotlinMetadata,
                                              KotlinFunctionMetadata       kotlinFunctionMetadata,
                                              KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                              KotlinTypeMetadata           kotlinTypeMetadata)
        {
            typeVis = valParamVis.visitType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitFunctionValParamVarArgType(Clazz                        clazz,
                                                    KotlinMetadata               kotlinMetadata,
                                                    KotlinFunctionMetadata       kotlinFunctionMetadata,
                                                    KotlinValueParameterMetadata kotlinValueParameterMetadata,
                                                    KotlinTypeMetadata           kotlinTypeMetadata)
        {
            typeVis = valParamVis.visitVarargElementType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitAliasUnderlyingType(Clazz                              clazz,
                                             KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                             KotlinTypeAliasMetadata            kotlinTypeAliasMetadata,
                                             KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            typeVis = aliasVis.visitUnderlyingType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
        }

        @Override
        public void visitAliasExpandedType(Clazz                              clazz,
                                           KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                           KotlinTypeAliasMetadata            kotlinTypeAliasMetadata,
                                           KotlinTypeMetadata                 kotlinTypeMetadata)
        {
            typeVis = aliasVis.visitExpandedType(convertTypeFlags(kotlinTypeMetadata.flags));

            visitAnyType(clazz, kotlinTypeMetadata);
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

                typeVis.visitClass(className);
            }

            if (kotlinTypeMetadata.typeParamID >= 0)
            {
                typeVis.visitTypeParameter(kotlinTypeMetadata.typeParamID);
            }

            if (kotlinTypeMetadata.aliasName != null)
            {
                typeVis.visitTypeAlias(kotlinTypeMetadata.aliasName);
            }

            kotlinTypeMetadata.abbreviationAccept( clazz, new TypeConstructor(typeVis));
            kotlinTypeMetadata.outerClassAccept(   clazz, new TypeConstructor(typeVis));
            kotlinTypeMetadata.typeArgumentsAccept(clazz, new TypeConstructor(typeVis));
            kotlinTypeMetadata.upperBoundsAccept(  clazz, new TypeConstructor(typeVis));

            // Extensions.
            JvmTypeExtensionVisitor typeExtVis =
                (JvmTypeExtensionVisitor)typeVis.visitExtensions(JvmTypeExtensionVisitor.TYPE);

            typeExtVis.visit(kotlinTypeMetadata.isRaw);

            kotlinTypeMetadata.annotationsAccept(clazz,
                                                 new AnnotationConstructor(typeExtVis::visitAnnotation));

            typeExtVis.visitEnd();

            typeVis.visitEnd();
        }
    }


    private class TypeParameterConstructor
    implements KotlinTypeParameterVisitor
    {
        private KmTypeParameterVisitor typeParamVis;

        private KmClassVisitor classVis;
        TypeParameterConstructor(KmClassVisitor classVis) { this.classVis = classVis; }

        private KmPropertyVisitor propertyVis;
        TypeParameterConstructor(KmPropertyVisitor propertyVis) { this.propertyVis = propertyVis; }

        private KmFunctionVisitor functionVis;
        TypeParameterConstructor(KmFunctionVisitor functionVis) { this.functionVis = functionVis; }

        private KmTypeAliasVisitor aliasVis;
        TypeParameterConstructor(KmTypeAliasVisitor aliasVis) { this.aliasVis = aliasVis; }


        // Implementations for KotlinTypeParameterVisitor.

        @Override
        public void visitClassTypeParameter(Clazz                       clazz,
                                            KotlinClassKindMetadata     kotlinMetadata,
                                            KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            typeParamVis = classVis.visitTypeParameter(convertTypeParameterFlags(kotlinTypeParameterMetadata.flags),
                                                       kotlinTypeParameterMetadata.name,
                                                       kotlinTypeParameterMetadata.id,
                                                       toKmVariance(kotlinTypeParameterMetadata.variance));

            visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);
        }

        @Override
        public void visitPropertyTypeParameter(Clazz                              clazz,
                                               KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                               KotlinPropertyMetadata             kotlinPropertyMetadata,
                                               KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
        {
            typeParamVis = propertyVis.visitTypeParameter(convertTypeParameterFlags(kotlinTypeParameterMetadata.flags),
                                                          kotlinTypeParameterMetadata.name,
                                                          kotlinTypeParameterMetadata.id,
                                                          toKmVariance(kotlinTypeParameterMetadata.variance));

            visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);
        }

        @Override
        public void visitFunctionTypeParameter(Clazz                       clazz,
                                               KotlinMetadata              kotlinMetadata,
                                               KotlinFunctionMetadata      kotlinFunctionMetadata,
                                               KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            typeParamVis = functionVis.visitTypeParameter(convertTypeParameterFlags(kotlinTypeParameterMetadata.flags),
                                                          kotlinTypeParameterMetadata.name,
                                                          kotlinTypeParameterMetadata.id,
                                                          toKmVariance(kotlinTypeParameterMetadata.variance));

            visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);
        }

        @Override
        public void visitAliasTypeParameter(Clazz                              clazz,
                                            KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                            KotlinTypeAliasMetadata            kotlinTypeAliasMetadata,
                                            KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
        {
            typeParamVis = aliasVis.visitTypeParameter(convertTypeParameterFlags(kotlinTypeParameterMetadata.flags),
                                                       kotlinTypeParameterMetadata.name,
                                                       kotlinTypeParameterMetadata.id,
                                                       toKmVariance(kotlinTypeParameterMetadata.variance));

            visitAnyTypeParameter(clazz, kotlinTypeParameterMetadata);
        }


        // Small helper methods.
        @Override
        public void visitAnyTypeParameter(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            kotlinTypeParameterMetadata.upperBoundsAccept(clazz,
                                                          new TypeConstructor(typeParamVis));

            // Extensions.
            JvmTypeParameterExtensionVisitor typeParamExtVis =
                (JvmTypeParameterExtensionVisitor)typeParamVis.visitExtensions(JvmTypeParameterExtensionVisitor.TYPE);

            kotlinTypeParameterMetadata.annotationsAccept(clazz,
                                                         new AnnotationConstructor(typeParamExtVis::visitAnnotation));

            typeParamExtVis.visitEnd();

            typeParamVis.visitEnd();
        }
    }


    /**
     * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin file facade (k == 2) metadata.
     */
    private class KotlinFileFacadeConstructor
    extends    KotlinDeclarationContainerConstructor
    implements KotlinMetadataVisitor
    {
        private final KotlinClassMetadata.FileFacade.Writer facadeKmdWriter;

        KotlinFileFacadeConstructor()
        {
            this(new KotlinClassMetadata.FileFacade.Writer());
        }

        private KotlinFileFacadeConstructor(KotlinClassMetadata.FileFacade.Writer facadeKmdWriter)
        {
            super(facadeKmdWriter);
            this.facadeKmdWriter = facadeKmdWriter;
        }


        // Implementations for KotlinMetadataVisitor.
        @Override
        public void visitKotlinFileFacadeMetadata(Clazz clazz, KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata)
        {
            kotlinFileFacadeKindMetadata.propertiesAccept(clazz, this);
            kotlinFileFacadeKindMetadata.functionsAccept(clazz, this);
            kotlinFileFacadeKindMetadata.typeAliasesAccept(clazz, this);

            JvmPackageExtensionVisitor ext =
                (JvmPackageExtensionVisitor) kmdWriter.visitExtensions(JvmPackageExtensionVisitor.TYPE);

            extensionVisitor = ext;
            kotlinFileFacadeKindMetadata.delegatedPropertiesAccept(clazz, this);

            ext.visitEnd();

            facadeKmdWriter.visitEnd();

            // Finally store the protobuf contents in the fields of the enclosing class.
            KotlinClassHeader header = facadeKmdWriter.write(COMPATIBLE_METADATA_VERSION,
                                                             kotlinFileFacadeKindMetadata.xi).getHeader();

            k  = header.getKind();
            mv = header.getMetadataVersion();
            d1 = header.getData1();
            d2 = header.getData2();
            xi = header.getExtraInt();
            xs = header.getExtraString();
            pn = header.getPackageName();
        }
    }


    /**
     * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin synthetic class (k == 3) metadata.
     */
    private class KotlinSyntheticClassConstructor
    implements KotlinMetadataVisitor,

               // Implementation interfaces.
               KotlinFunctionVisitor
    {
        private       KotlinSyntheticClassKindMetadata          md;
        private final KotlinClassMetadata.SyntheticClass.Writer kmdWriter;


        KotlinSyntheticClassConstructor()
        {
            this.kmdWriter = new KotlinClassMetadata.SyntheticClass.Writer();
        }


        // Implementations for KotlinMetadataVisitor.
        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinSyntheticClassMetadata(Clazz clazz, KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata)
        {
            this.md = kotlinSyntheticClassKindMetadata;

            md.functionsAccept(clazz, this);

            kmdWriter.visitEnd();

            // Finally store the protobuf contents in the fields of the enclosing class.
            KotlinClassHeader header = kmdWriter.write(COMPATIBLE_METADATA_VERSION,
                                                       kotlinSyntheticClassKindMetadata.xi).getHeader();

            k  = header.getKind();
            mv = header.getMetadataVersion();
            d1 = header.getData1();
            d2 = header.getData2();
            xi = header.getExtraInt();
            xs = header.getExtraString();
            pn = header.getPackageName();
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
            KmFunctionVisitor kmdFunctionVisitor =
                kmdWriter.visitFunction(convertFunctionFlags(kotlinFunctionMetadata.flags),
                                        kotlinFunctionMetadata.name);

            kotlinFunctionMetadata.valueParametersAccept(clazz,
                                                         kotlinSyntheticClassKindMetadata,
                                                         new ValueParameterConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.returnTypeAccept(clazz,
                                                    kotlinSyntheticClassKindMetadata,
                                                    new TypeConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.receiverTypeAccept(clazz,
                                                      kotlinSyntheticClassKindMetadata,
                                                      new TypeConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.typeParametersAccept(clazz,
                                                        kotlinSyntheticClassKindMetadata,
                                                        new TypeParameterConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.versionRequirementAccept(clazz,
                                                            kotlinSyntheticClassKindMetadata,
                                                            new VersionRequirementConstructor(kmdFunctionVisitor));
            kotlinFunctionMetadata.contractsAccept(clazz,
                                                   kotlinSyntheticClassKindMetadata,
                                                   new ContractConstructor(kmdFunctionVisitor));

            JvmFunctionExtensionVisitor ext =
                (JvmFunctionExtensionVisitor) kmdFunctionVisitor.visitExtensions(JvmFunctionExtensionVisitor.TYPE);

            JvmMethodSignature jvmMethodSignature = toKotlinJvmMethodSignature(kotlinFunctionMetadata.jvmSignature);

            ext.visit(jvmMethodSignature);

            if (kotlinFunctionMetadata.lambdaClassOriginName != null)
            {
                ext.visitLambdaClassOriginName(kotlinFunctionMetadata.lambdaClassOriginName);
            }

            ext.visitEnd();

            kmdFunctionVisitor.visitEnd();
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
            KotlinClassHeader header =
                new KotlinClassMetadata.MultiFileClassFacade.Writer()
                    .write(kotlinMultiFileFacadeKindMetadata.partClassNames,
                           COMPATIBLE_METADATA_VERSION,
                           kotlinMultiFileFacadeKindMetadata.xi).getHeader();

            k  = header.getKind();
            mv = header.getMetadataVersion();
            d1 = header.getData1();
            d2 = header.getData2();
            xi = header.getExtraInt();
            xs = header.getExtraString();
            pn = header.getPackageName();
        }
    }


    /**
     * This utility class constructs the protobuf (d1 and d2 arrays) for Kotlin file facade (k == 2) metadata.
     */
    private class KotlinMultiFilePartConstructor
    extends    KotlinDeclarationContainerConstructor
    implements KotlinMetadataVisitor
    {
        private final KotlinClassMetadata.MultiFileClassPart.Writer multiPartKmdWriter;

        KotlinMultiFilePartConstructor()
        {
            this(new KotlinClassMetadata.MultiFileClassPart.Writer());
        }

        private KotlinMultiFilePartConstructor(KotlinClassMetadata.MultiFileClassPart.Writer multiPartKmdWriter)
        {
            super(multiPartKmdWriter);
            this.multiPartKmdWriter = multiPartKmdWriter;
        }


        // Implementations for KotlinMetadataVisitor
        @Override
        public void visitKotlinMultiFilePartMetadata(Clazz clazz, KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata)
        {
            kotlinMultiFilePartKindMetadata.propertiesAccept( clazz, this);
            kotlinMultiFilePartKindMetadata.functionsAccept(  clazz, this);
            kotlinMultiFilePartKindMetadata.typeAliasesAccept(clazz, this);

            JvmPackageExtensionVisitor ext =
                (JvmPackageExtensionVisitor) multiPartKmdWriter.visitExtensions(JvmPackageExtensionVisitor.TYPE);

            extensionVisitor = ext;
            kotlinMultiFilePartKindMetadata.delegatedPropertiesAccept(clazz, this);

            ext.visitEnd();

            multiPartKmdWriter.visitEnd();

            // Finally store the protobuf contents in the fields of the enclosing class.
            KotlinClassHeader header = multiPartKmdWriter.write(kotlinMultiFilePartKindMetadata.facadeName,
                                                                COMPATIBLE_METADATA_VERSION,
                                                                kotlinMultiFilePartKindMetadata.xi).getHeader();

            k  = header.getKind();
            mv = header.getMetadataVersion();
            d1 = header.getData1();
            d2 = header.getData2();
            xi = header.getExtraInt();
            xs = header.getExtraString();
            pn = header.getPackageName();
        }
    }


    private class VersionRequirementConstructor
    implements KotlinVersionRequirementVisitor
    {
        private KmVersionRequirementVisitor versionReqVis;

        private KmConstructorVisitor constructorVis;
        VersionRequirementConstructor(KmConstructorVisitor constructorVis) { this.constructorVis = constructorVis; }

        private KmClassVisitor classVis;
        VersionRequirementConstructor(KmClassVisitor classVis) { this.classVis = classVis; }

        private KmPropertyVisitor propertyVis;
        VersionRequirementConstructor(KmPropertyVisitor propertyVis) { this.propertyVis = propertyVis; }

        private KmFunctionVisitor functionVis;
        VersionRequirementConstructor(KmFunctionVisitor functionVis) { this.functionVis = functionVis; }

        private KmTypeAliasVisitor aliasVis;
        VersionRequirementConstructor(KmTypeAliasVisitor aliasVis) { this.aliasVis = aliasVis; }


        // Implementations for KotlinVersionRequirementVisitor.

        @Override
        public void visitClassVersionRequirement(Clazz                            clazz,
                                                 KotlinMetadata                   kotlinMetadata,
                                                 KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            versionReqVis = classVis.visitVersionRequirement();
        }

        @Override
        public void visitConstructorVersionRequirement(Clazz                            clazz,
                                                       KotlinMetadata                   kotlinMetadata,
                                                       KotlinConstructorMetadata        kotlinConstructorMetadata,
                                                       KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            versionReqVis = constructorVis.visitVersionRequirement();

            visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);
        }

        @Override
        public void visitPropertyVersionRequirement(Clazz                              clazz,
                                                    KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                                    KotlinPropertyMetadata             kotlinPropertyMetadata,
                                                    KotlinVersionRequirementMetadata   kotlinVersionRequirementMetadata)
        {
            versionReqVis = propertyVis.visitVersionRequirement();

            visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);
        }

        @Override
        public void visitFunctionVersionRequirement(Clazz                            clazz,
                                                    KotlinMetadata                   kotlinMetadata,
                                                    KotlinFunctionMetadata           kotlinFunctionMetadata,
                                                    KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            versionReqVis = functionVis.visitVersionRequirement();

            visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);
        }

        public void visitTypeAliasVersionRequirement(Clazz clazz,
                                                     KotlinMetadata                   kotlinMetadata,
                                                     KotlinTypeAliasMetadata          kotlinTypeAliasMetadata,
                                                     KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            versionReqVis = aliasVis.visitVersionRequirement();

            visitAnyVersionRequirement(clazz, kotlinVersionRequirementMetadata);
        }


        // Small helper methods.
        @Override
        public void visitAnyVersionRequirement(Clazz                            clazz,
                                               KotlinVersionRequirementMetadata kotlinVersionRequirementMetadata)
        {
            versionReqVis.visit(toKmVersionRequirementVersionKind(kotlinVersionRequirementMetadata.kind),
                                toKmVersionRequirementLevel(kotlinVersionRequirementMetadata.level),
                                kotlinVersionRequirementMetadata.errorCode,
                                kotlinVersionRequirementMetadata.message);

            versionReqVis.visitVersion(kotlinVersionRequirementMetadata.major,
                                       kotlinVersionRequirementMetadata.minor,
                                       kotlinVersionRequirementMetadata.patch);

            versionReqVis.visitEnd();
        }
    }


    // Small helper methods.

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


    private Set<Flag> convertCommonFlags(KotlinCommonFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        if (flags.hasAnnotations) flagSet.add(Flag.HAS_ANNOTATIONS);

        return flagSet;
    }


    private Set<Flag> convertVisibilityFlags(KotlinVisibilityFlags flags)
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


    private Set<Flag> convertModalityFlags(KotlinModalityFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        if (flags.isAbstract) flagSet.add(Flag.IS_ABSTRACT);
        if (flags.isFinal)    flagSet.add(Flag.IS_FINAL);
        if (flags.isOpen)     flagSet.add(Flag.IS_OPEN);
        if (flags.isSealed)   flagSet.add(Flag.IS_SEALED);

        return flagSet;
    }


    private int convertFunctionFlags(KotlinFunctionFlags flags)
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


    private int convertTypeFlags(KotlinTypeFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));

        if (flags.isNullable) flagSet.add(Flag.Type.IS_NULLABLE);
        if (flags.isSuspend)  flagSet.add(Flag.Type.IS_SUSPEND);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private int convertTypeParameterFlags(KotlinTypeParameterFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));

        if (flags.isReified) flagSet.add(Flag.TypeParameter.IS_REIFIED);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private int convertTypeAliasFlags(KotlinTypeAliasFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));
        flagSet.addAll(convertVisibilityFlags(flags.visibility));

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private int convertPropertyFlags(KotlinPropertyFlags flags)
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


    private int convertPropertyJvmFlags(KotlinPropertyFlags flags)
    {
        return flags.isMovedFromInterfaceCompanion ?
            flagsOf(JvmFlag.Property.IS_MOVED_FROM_INTERFACE_COMPANION) :
            0;
    }


    private int convertPropertyAccessorFlags(KotlinPropertyAccessorFlags flags)
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


    private int convertValueParameterFlags(KotlinValueParameterFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        flagSet.addAll(convertCommonFlags(flags.common));

        if (flags.hasDefaultValue) flagSet.add(Flag.ValueParameter.DECLARES_DEFAULT_VALUE);
        if (flags.isNoInline)      flagSet.add(Flag.ValueParameter.IS_NOINLINE);
        if (flags.isCrossInline)   flagSet.add(Flag.ValueParameter.IS_CROSSINLINE);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }


    private int convertEffectExpressionFlags(KotlinEffectExpressionFlags flags)
    {
        Set<Flag> flagSet = new HashSet<>();

        if (flags.isNullCheckPredicate) flagSet.add(Flag.EffectExpression.IS_NULL_CHECK_PREDICATE);
        if (flags.isNegated)            flagSet.add(Flag.EffectExpression.IS_NEGATED);

        return flagsOf(flagSet.toArray(new Flag[0]));
    }
}
