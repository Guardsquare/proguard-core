/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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
package proguard.classfile.editor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import proguard.classfile.ClassConstants;
import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.FieldSignature;
import proguard.classfile.JavaTypeConstants;
import proguard.classfile.LibraryClass;
import proguard.classfile.LibraryField;
import proguard.classfile.LibraryMethod;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramField;
import proguard.classfile.ProgramMember;
import proguard.classfile.ProgramMethod;
import proguard.classfile.TypeConstants;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.InnerClassesAttribute;
import proguard.classfile.attribute.InnerClassesInfo;
import proguard.classfile.attribute.LocalVariableInfo;
import proguard.classfile.attribute.LocalVariableTableAttribute;
import proguard.classfile.attribute.LocalVariableTypeInfo;
import proguard.classfile.attribute.LocalVariableTypeTableAttribute;
import proguard.classfile.attribute.RecordAttribute;
import proguard.classfile.attribute.RecordComponentInfo;
import proguard.classfile.attribute.SignatureAttribute;
import proguard.classfile.attribute.annotation.Annotation;
import proguard.classfile.attribute.annotation.AnnotationDefaultAttribute;
import proguard.classfile.attribute.annotation.AnnotationElementValue;
import proguard.classfile.attribute.annotation.AnnotationsAttribute;
import proguard.classfile.attribute.annotation.ArrayElementValue;
import proguard.classfile.attribute.annotation.ClassElementValue;
import proguard.classfile.attribute.annotation.ConstantElementValue;
import proguard.classfile.attribute.annotation.ElementValue;
import proguard.classfile.attribute.annotation.EnumConstantElementValue;
import proguard.classfile.attribute.annotation.ParameterAnnotationsAttribute;
import proguard.classfile.attribute.annotation.RuntimeInvisibleAnnotationsAttribute;
import proguard.classfile.attribute.annotation.visitor.AllAnnotationVisitor;
import proguard.classfile.attribute.annotation.visitor.AnnotationTypeFilter;
import proguard.classfile.attribute.annotation.visitor.AnnotationVisitor;
import proguard.classfile.attribute.annotation.visitor.ElementValueVisitor;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.attribute.visitor.InnerClassesInfoVisitor;
import proguard.classfile.attribute.visitor.LocalVariableInfoVisitor;
import proguard.classfile.attribute.visitor.LocalVariableTypeInfoVisitor;
import proguard.classfile.attribute.visitor.RecordComponentInfoVisitor;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.DynamicConstant;
import proguard.classfile.constant.InvokeDynamicConstant;
import proguard.classfile.constant.MethodTypeConstant;
import proguard.classfile.constant.StringConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.kotlin.KotlinAnnotatable;
import proguard.classfile.kotlin.KotlinAnnotation;
import proguard.classfile.kotlin.KotlinAnnotationArgument;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinConstructorMetadata;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinFileFacadeKindMetadata;
import proguard.classfile.kotlin.KotlinFunctionMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinMultiFileFacadeKindMetadata;
import proguard.classfile.kotlin.KotlinMultiFilePartKindMetadata;
import proguard.classfile.kotlin.KotlinPropertyMetadata;
import proguard.classfile.kotlin.KotlinSyntheticClassKindMetadata;
import proguard.classfile.kotlin.KotlinTypeAliasMetadata;
import proguard.classfile.kotlin.KotlinTypeMetadata;
import proguard.classfile.kotlin.KotlinTypeParameterMetadata;
import proguard.classfile.kotlin.KotlinValueParameterMetadata;
import proguard.classfile.kotlin.flags.KotlinPropertyAccessorFlags;
import proguard.classfile.kotlin.visitor.AllTypeVisitor;
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor;
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor;
import proguard.classfile.kotlin.visitor.KotlinConstructorVisitor;
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeAliasVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor;
import proguard.classfile.kotlin.visitor.KotlinValueParameterVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.DescriptorClassEnumeration;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;

import static proguard.classfile.kotlin.KotlinAnnotationArgument.EnumValue;
import static proguard.classfile.kotlin.KotlinAnnotationArgument.Value;
import static proguard.classfile.kotlin.KotlinConstants.FUNCTION_NAME_MANGLE_SEPARATOR;
import static proguard.classfile.kotlin.KotlinConstants.TYPE_KOTLIN_JVM_JVMNAME;
import static proguard.classfile.util.ClassUtil.internalSimpleClassName;


/**
 * This {@link ClassVisitor} fixes references of constant pool entries, fields,
 * methods, attributes and kotlin metadata to classes whose names have
 * changed. Descriptors of programMember references are not updated yet.
 *
 * @see MemberReferenceFixer
 * @author Eric Lafortune
 */
public class ClassReferenceFixer
implements   ClassVisitor,

             // Implementation interfaces.
             ConstantVisitor,
             MemberVisitor,
             AttributeVisitor,
             RecordComponentInfoVisitor,
             InnerClassesInfoVisitor,
             LocalVariableInfoVisitor,
             LocalVariableTypeInfoVisitor,
             AnnotationVisitor,
             ElementValueVisitor
{
    private static final Logger logger = LogManager.getLogger(ClassReferenceFixer.class);

    private NameGenerationStrategy newNameStrategy;

    private final KotlinReferenceFixer kotlinReferenceFixer = new KotlinReferenceFixer();

    /**
     * Creates a new ClassReferenceFixer.
     * @param ensureUniqueMemberNames specifies whether class members whose
     *                                descriptor changes should get new, unique
     *                                names, in order to avoid naming conflicts
     *                                with similar methods.
     */
    public ClassReferenceFixer(boolean ensureUniqueMemberNames)
    {
        newNameStrategy = ensureUniqueMemberNames ?
            (programClass, programMember, originalName, originalDescriptor) -> originalName.equals(ClassConstants.METHOD_NAME_INIT) ?
                ClassConstants.METHOD_NAME_INIT :
                originalName + TypeConstants.SPECIAL_MEMBER_SEPARATOR + Long.toHexString(Math.abs(originalDescriptor.hashCode())) : null;
    }

    /**
     * Creates a new ClassReferenceFixer.
     * @param newNameStrategy specifies how class members whose descriptor
     *                        changes should get a new name in order to avoid
     *                        naming conflicts with similar members.
     */
    public ClassReferenceFixer(NameGenerationStrategy newNameStrategy)
    {
        this.newNameStrategy = newNameStrategy;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support " + clazz.getClass().getName());
    }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        // Fix the constant pool.
        programClass.constantPoolEntriesAccept(this);

        // Fix the class members.
        programClass.fieldsAccept(this);
        programClass.methodsAccept(this);

        // Fix the attributes.
        programClass.attributesAccept(this);

        // Fix the Kotlin Metadata.
        programClass.kotlinMetadataAccept(kotlinReferenceFixer);
    }


    @Override
    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Fix class members.
        libraryClass.fieldsAccept(this);
        libraryClass.methodsAccept(this);
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        // Has the descriptor changed?
        String descriptor    = programField.getDescriptor(programClass);
        String newDescriptor = newDescriptor(descriptor,
                                             programField.referencedClass);

        visitProgramMember(programClass, programField, descriptor, newDescriptor);
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        String descriptor    = programMethod.getDescriptor(programClass);
        String newDescriptor = newDescriptor(descriptor,
                                             programMethod.referencedClasses);

        visitProgramMember(programClass, programMethod, descriptor, newDescriptor);
    }


    private void visitProgramMember(ProgramClass programClass,
                                    ProgramMember programMember,
                                    String descriptor,
                                    String newDescriptor)
    {
        // Has the descriptor changed?
        if (!descriptor.equals(newDescriptor))
        {
            ConstantPoolEditor constantPoolEditor =
                new ConstantPoolEditor(programClass);

            // Update the descriptor.
            programMember.u2descriptorIndex =
                constantPoolEditor.addUtf8Constant(newDescriptor);

            // Update the name, if requested.
            if (newNameStrategy != null)
            {
                String name    = programMember.getName(programClass);
                String newName = newNameStrategy.getNewName(programClass, programMember, name, descriptor);
                programMember.u2nameIndex =
                    constantPoolEditor.addUtf8Constant(newName);
            }
        }

        // Fix the attributes.
        programMember.attributesAccept(programClass, this);
    }


    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        // Has the descriptor changed?
        String descriptor    = libraryField.getDescriptor(libraryClass);
        String newDescriptor = newDescriptor(descriptor,
                                             libraryField.referencedClass);

        // Update the descriptor.
        libraryField.descriptor = newDescriptor;
    }


    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        // Has the descriptor changed?
        String descriptor    = libraryMethod.getDescriptor(libraryClass);
        String newDescriptor = newDescriptor(descriptor,
                                             libraryMethod.referencedClasses);

        if (!descriptor.equals(newDescriptor))
        {
            // Update the descriptor.
            libraryMethod.descriptor = newDescriptor;
        }
    }


    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        // Does the string refer to a class, due to a Class.forName construct?
        Clazz  referencedClass  = stringConstant.referencedClass;
        Member referencedMember = stringConstant.referencedMember;
        if (referencedClass  != null &&
            referencedMember == null)
        {
            // Reconstruct the new class name.
            String externalClassName    = stringConstant.getString(clazz);
            String internalClassName    = ClassUtil.internalClassName(externalClassName);
            String newInternalClassName = newClassName(internalClassName,
                                                       referencedClass);

            // Update the String entry if required.
            if (!newInternalClassName.equals(internalClassName))
            {
                // Only convert to an external class name if the original was
                // an external class name too.
                String newExternalClassName =
                    externalClassName.indexOf(JavaTypeConstants.PACKAGE_SEPARATOR) >= 0 ?
                        ClassUtil.externalClassName(newInternalClassName) :
                        newInternalClassName;

                // Refer to a new Utf8 entry.
                stringConstant.u2stringIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newExternalClassName);
            }
        }
    }


    public void visitDynamicConstant(Clazz clazz, DynamicConstant dynamicConstant)
    {
        // Has the descriptor changed?
        String descriptor    = dynamicConstant.getType(clazz);
        String newDescriptor = newDescriptor(descriptor,
                                             dynamicConstant.referencedClasses);

        if (!descriptor.equals(newDescriptor))
        {
            String name = dynamicConstant.getName(clazz);

            // Refer to a new NameAndType entry.
            dynamicConstant.u2nameAndTypeIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addNameAndTypeConstant(name, newDescriptor);
        }
    }


    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        // Has the descriptor changed?
        String descriptor    = invokeDynamicConstant.getType(clazz);
        String newDescriptor = newDescriptor(descriptor,
                                             invokeDynamicConstant.referencedClasses);

        if (!descriptor.equals(newDescriptor))
        {
            String name = invokeDynamicConstant.getName(clazz);

            // Refer to a new NameAndType entry.
            invokeDynamicConstant.u2nameAndTypeIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addNameAndTypeConstant(name, newDescriptor);
        }
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Do we know the referenced class?
        Clazz referencedClass = classConstant.referencedClass;
        if (referencedClass != null)
        {
            // Has the class name changed?
            String className    = classConstant.getName(clazz);
            String newClassName = newClassName(className, referencedClass);
            if (!className.equals(newClassName))
            {
                // Refer to a new Utf8 entry.
                classConstant.u2nameIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newClassName);
            }
        }
    }


    public void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant methodTypeConstant)
    {
        // Has the descriptor changed?
        String descriptor    = methodTypeConstant.getType(clazz);
        String newDescriptor = newDescriptor(descriptor,
                                             methodTypeConstant.referencedClasses);

        if (!descriptor.equals(newDescriptor))
        {
            // Update the descriptor.
            methodTypeConstant.u2descriptorIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newDescriptor);
        }
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitRecordAttribute(Clazz clazz, RecordAttribute recordAttribute)
    {
        // Fix the components.
        recordAttribute.componentsAccept(clazz, this);
    }


    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        // Fix the inner class names.
        innerClassesAttribute.innerClassEntriesAccept(clazz, this);
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Fix the attributes.
        codeAttribute.attributesAccept(clazz, method, this);
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Fix the types of the local variables.
        localVariableTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Fix the signatures of the local variables.
        localVariableTypeTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        // Has the signature changed?
        String signature    = signatureAttribute.getSignature(clazz);
        String newSignature = newDescriptor(signature,
                                            signatureAttribute.referencedClasses);

        if (!signature.equals(newSignature))
        {
            // Update the signature.
            signatureAttribute.u2signatureIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newSignature);
        }
    }


    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        // Fix the annotations.
        annotationsAttribute.annotationsAccept(clazz, this);
    }


    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        // Fix the annotations.
        parameterAnnotationsAttribute.annotationsAccept(clazz, method, this);
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        // Fix the annotation.
        annotationDefaultAttribute.defaultValueAccept(clazz, this);
    }


    // Implementations for RecordComponentInfoVisitor.

    public void visitRecordComponentInfo(Clazz clazz, RecordComponentInfo recordComponentInfo)
    {
        // Fix the attributes.
        recordComponentInfo.attributesAccept(clazz, this);
    }


    // Implementations for InnerClassesInfoVisitor.

    public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo)
    {
        // Fix the inner class name.
        int innerClassIndex = innerClassesInfo.u2innerClassIndex;
        int innerNameIndex  = innerClassesInfo.u2innerNameIndex;
        if (innerClassIndex != 0 &&
            innerNameIndex  != 0)
        {
            String newInnerName = clazz.getClassName(innerClassIndex);
            int index = newInnerName.lastIndexOf(TypeConstants.INNER_CLASS_SEPARATOR);
            // Prevent incorrect renaming of kotlin inner classes starting with `$` (e.g. `Foo$$Bar`).
            // TODO(T17925) Correctly handle `$` occurring in the middle of kotlin class names.
            while (index > 0 && newInnerName.charAt(index-1) == TypeConstants.INNER_CLASS_SEPARATOR)
            {
                index -= 1;
            }
            if (index >= 0)
            {
                innerClassesInfo.u2innerNameIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newInnerName.substring(index + 1));
            }
        }
    }


    // Implementations for LocalVariableInfoVisitor.

    public void visitLocalVariableInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableInfo localVariableInfo)
    {
        // Has the descriptor changed?
        String descriptor    = localVariableInfo.getDescriptor(clazz);
        String newDescriptor = newDescriptor(descriptor,
                                             localVariableInfo.referencedClass);

        if (!descriptor.equals(newDescriptor))
        {
            // Refer to a new Utf8 entry.
            localVariableInfo.u2descriptorIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newDescriptor);
        }
    }

    // Implementations for LocalVariableTypeInfoVisitor.

    public void visitLocalVariableTypeInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeInfo localVariableTypeInfo)
    {
        // Has the signature changed?
        String signature    = localVariableTypeInfo.getSignature(clazz);
        String newSignature = newDescriptor(signature,
                                            localVariableTypeInfo.referencedClasses);

        if (!signature.equals(newSignature))
        {
            // Update the signature.
            localVariableTypeInfo.u2signatureIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newSignature);
        }
    }

    // Implementations for AnnotationVisitor.

    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        // Has the type changed?
        String typeName    = annotation.getType(clazz);
        String newTypeName = newDescriptor(typeName,
                                           annotation.referencedClasses);

        if (!typeName.equals(newTypeName))
        {
            // Update the type.
            annotation.u2typeIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newTypeName);
        }

        // Fix the element values.
        annotation.elementValuesAccept(clazz, this);
    }


    // Implementations for ElementValueVisitor.

    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
    }


    public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        // Has the type name changed?
        String typeName    = enumConstantElementValue.getTypeName(clazz);
        String newTypeName = newDescriptor(typeName,
                                           enumConstantElementValue.referencedClasses);

        if (!typeName.equals(newTypeName))
        {
            // Update the type name.
            enumConstantElementValue.u2typeNameIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newTypeName);
        }
    }


    public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        // Has the class info changed?
        String className    = classElementValue.getClassName(clazz);
        String newClassName = newDescriptor(className,
                                            classElementValue.referencedClasses);

        if (!className.equals(newClassName))
        {
            // Update the class info.
            classElementValue.u2classInfoIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newClassName);
        }
    }


    public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        // Fix the annotation.
        annotationElementValue.annotationAccept(clazz, this);
    }


    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        // Fix the element values.
        arrayElementValue.elementValuesAccept(clazz, annotation, this);
    }


    private static class KotlinReferenceFixer
    implements           KotlinMetadataVisitor,
                         KotlinPropertyVisitor,
                         KotlinFunctionVisitor,
                         KotlinConstructorVisitor,
                         KotlinTypeVisitor,
                         KotlinTypeAliasVisitor,
                         KotlinValueParameterVisitor,
                         KotlinTypeParameterVisitor,
                         KotlinAnnotationVisitor,
                         KotlinAnnotationArgumentVisitor
    {
        // Implementations for KotlinMetadataVisitor.
        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                            KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
        {
            kotlinDeclarationContainerMetadata.ownerClassName = newClassName(kotlinDeclarationContainerMetadata.ownerClassName,
                                                                             kotlinDeclarationContainerMetadata.ownerReferencedClass);
            kotlinDeclarationContainerMetadata.propertiesAccept(clazz, this);
            kotlinDeclarationContainerMetadata.delegatedPropertiesAccept(clazz, this);
            kotlinDeclarationContainerMetadata.functionsAccept(clazz, this);
            kotlinDeclarationContainerMetadata.typeAliasesAccept(clazz, this);
        }


        @Override
        public void visitKotlinClassMetadata(Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata)
        {
            kotlinClassKindMetadata.className = newClassName(kotlinClassKindMetadata.className,
                                                             kotlinClassKindMetadata.referencedClass);

            if (kotlinClassKindMetadata.anonymousObjectOriginName != null)
            {
                kotlinClassKindMetadata.anonymousObjectOriginName =
                    newClassName(kotlinClassKindMetadata.anonymousObjectOriginName,
                                 kotlinClassKindMetadata.anonymousObjectOriginClass);
            }

            if (kotlinClassKindMetadata.companionObjectName != null)
            {
                kotlinClassKindMetadata.companionObjectName =
                    shortKotlinNestedClassName(kotlinClassKindMetadata.className,
                                               kotlinClassKindMetadata.companionObjectName,
                                               kotlinClassKindMetadata.referencedCompanionClass);
            }

            for (int k = 0; k < kotlinClassKindMetadata.enumEntryNames.size(); k++)
            {
                kotlinClassKindMetadata.enumEntryNames.set(k,
                    kotlinClassKindMetadata.referencedEnumEntries.get(k).getName(clazz));
            }

            for (int k = 0; k < kotlinClassKindMetadata.nestedClassNames.size(); k++)
            {
                kotlinClassKindMetadata.nestedClassNames.set(k,
                     shortKotlinNestedClassName(clazz.getName(),
                                        kotlinClassKindMetadata.nestedClassNames       .get(k),
                                        kotlinClassKindMetadata.referencedNestedClasses.get(k)));
            }

            for (int k = 0; k < kotlinClassKindMetadata.sealedSubclassNames.size(); k++)
            {
                kotlinClassKindMetadata.sealedSubclassNames.set(k,
                     newClassName(kotlinClassKindMetadata.sealedSubclassNames       .get(k),
                                  kotlinClassKindMetadata.referencedSealedSubClasses.get(k)));
            }

            kotlinClassKindMetadata.typeParametersAccept(                   clazz, this);
            kotlinClassKindMetadata.contextReceiverTypesAccept(             clazz, this);
            kotlinClassKindMetadata.superTypesAccept(                       clazz, this);
            kotlinClassKindMetadata.constructorsAccept(                     clazz, this);
            kotlinClassKindMetadata.inlineClassUnderlyingPropertyTypeAccept(clazz, this);

            visitKotlinDeclarationContainerMetadata(clazz, kotlinClassKindMetadata);
        }

        @Override
        public void visitKotlinFileFacadeMetadata(Clazz clazz, KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata)
        {
            visitKotlinDeclarationContainerMetadata(clazz, kotlinFileFacadeKindMetadata);
        }

        @Override
        public void visitKotlinSyntheticClassMetadata(Clazz clazz, KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata)
        {
            kotlinSyntheticClassKindMetadata.functionsAccept(clazz, this);
        }

        @Override
        public void visitKotlinMultiFileFacadeMetadata(Clazz clazz, KotlinMultiFileFacadeKindMetadata kotlinMultiFileFacadeKindMetadata)
        {
            for (int k = 0; k < kotlinMultiFileFacadeKindMetadata.partClassNames.size(); k++)
            {
                kotlinMultiFileFacadeKindMetadata.partClassNames.set(k,
                    newClassName(kotlinMultiFileFacadeKindMetadata.partClassNames.get(k),
                                 kotlinMultiFileFacadeKindMetadata.referencedPartClasses.get(k)));
            }
        }

        @Override
        public void visitKotlinMultiFilePartMetadata(Clazz clazz, KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata)
        {
            kotlinMultiFilePartKindMetadata.facadeName =
                newClassName(kotlinMultiFilePartKindMetadata.facadeName,
                             kotlinMultiFilePartKindMetadata.referencedFacadeClass);

            visitKotlinDeclarationContainerMetadata(clazz, kotlinMultiFilePartKindMetadata);
        }


        // Implementations for KotlinPropertyVisitor.
        @Override
        public void visitAnyProperty(Clazz                              clazz,
                                     KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                     KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            ProgramClass programClass = (ProgramClass)clazz;
            if (kotlinPropertyMetadata.backingFieldSignature != null)
            {
                Clazz backingFieldClass = kotlinPropertyMetadata.referencedBackingFieldClass;
                Field backingField      = kotlinPropertyMetadata.referencedBackingField;
                kotlinPropertyMetadata.backingFieldSignature = new FieldSignature(backingFieldClass, backingField);
            }

            kotlinPropertyMetadata.getterSignature =
                fixPropertyMethod(programClass,
                                  kotlinPropertyMetadata.referencedGetterMethod,
                                  kotlinPropertyMetadata.getterFlags,
                                  kotlinPropertyMetadata.getterSignature);

            kotlinPropertyMetadata.setterSignature =
                fixPropertyMethod(programClass,
                                  kotlinPropertyMetadata.referencedSetterMethod,
                                  kotlinPropertyMetadata.setterFlags,
                                  kotlinPropertyMetadata.setterSignature);

            kotlinPropertyMetadata.syntheticMethodForAnnotations =
                fixPropertyMethod(kotlinPropertyMetadata.referencedSyntheticMethodClass,
                                  kotlinPropertyMetadata.referencedSyntheticMethodForAnnotations,
                                  null,
                                  kotlinPropertyMetadata.syntheticMethodForAnnotations);

            kotlinPropertyMetadata.syntheticMethodForDelegate =
                fixPropertyMethod(kotlinPropertyMetadata.referencedSyntheticMethodForDelegateClass,
                                  kotlinPropertyMetadata.referencedSyntheticMethodForDelegateMethod,
                                  null,
                                  kotlinPropertyMetadata.syntheticMethodForDelegate);

            kotlinPropertyMetadata.typeParametersAccept(      clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.receiverTypeAccept(        clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.contextReceiverTypesAccept(clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.typeAccept(                clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.setterParametersAccept(    clazz, kotlinDeclarationContainerMetadata, this);
        }


        // Implementations for KotlinFunctionVisitor.
        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata)
        {
            if (kotlinFunctionMetadata.lambdaClassOriginName != null)
            {
                kotlinFunctionMetadata.lambdaClassOriginName       = null;
                kotlinFunctionMetadata.referencedLambdaClassOrigin = null;
            }

            // Fix the JVM signatures.
            if (kotlinFunctionMetadata.jvmSignature != null)
            {
                Clazz jvmClass = kotlinFunctionMetadata.referencedMethodClass;
                Method jvmMethod = kotlinFunctionMetadata.referencedMethod;
                kotlinFunctionMetadata.jvmSignature = new MethodSignature(jvmClass, jvmMethod);
            }

            kotlinFunctionMetadata.typeParametersAccept(      clazz, kotlinMetadata, this);
            kotlinFunctionMetadata.receiverTypeAccept(        clazz, kotlinMetadata, this);
            kotlinFunctionMetadata.contextReceiverTypesAccept(clazz, kotlinMetadata, this);
            kotlinFunctionMetadata.valueParametersAccept(     clazz, kotlinMetadata, this);
            kotlinFunctionMetadata.returnTypeAccept(          clazz, kotlinMetadata, this);
            kotlinFunctionMetadata.contractsAccept(           clazz, kotlinMetadata, new AllTypeVisitor(this));

            String newFunctionName = newKotlinFunctionName(
                    kotlinFunctionMetadata.name,
                    kotlinFunctionMetadata.referencedMethod.getName(kotlinFunctionMetadata.referencedMethodClass)
            );

            if (!kotlinFunctionMetadata.name.equals(newFunctionName))
            {
                kotlinFunctionMetadata.name = newFunctionName;
            }
        }


        // Implementations for KotlinConstructorVisitor.
        @Override
        public void visitConstructor(Clazz                     clazz,
                                     KotlinClassKindMetadata   kotlinClassKindMetadata,
                                     KotlinConstructorMetadata kotlinConstructorMetadata)
        {
            if (kotlinConstructorMetadata.jvmSignature != null)
            {
                Method jvmMethod = kotlinConstructorMetadata.referencedMethod;
                kotlinConstructorMetadata.jvmSignature = new MethodSignature(clazz, jvmMethod);
            }

            kotlinConstructorMetadata.valueParametersAccept(clazz, kotlinClassKindMetadata, this);
        }


        // Implementations for KotlinTypeVisitor.
        @Override
        public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata)
        {

            if (kotlinTypeMetadata.className != null)
            {
                kotlinTypeMetadata.className = kotlinTypeMetadata.referencedClass.getName();
            }

            // We fix aliasName using KotlinAliasReferenceFixer after ClassReferenceFixer is finished.

            kotlinTypeMetadata.annotationsAccept(  clazz, this);
            kotlinTypeMetadata.typeArgumentsAccept(clazz, this);
            kotlinTypeMetadata.outerClassAccept(   clazz, this);
            kotlinTypeMetadata.upperBoundsAccept(  clazz, this);
            kotlinTypeMetadata.abbreviationAccept( clazz, this);
        }


        // Implementations for KotlinTypeAliasVisitor.
        @Override
        public void visitTypeAlias(Clazz                              clazz,
                                   KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                   KotlinTypeAliasMetadata            kotlinTypeAliasMetadata)
        {

            kotlinTypeAliasMetadata.annotationsAccept(   clazz, this);
            kotlinTypeAliasMetadata.underlyingTypeAccept(clazz, kotlinDeclarationContainerMetadata, this);
            kotlinTypeAliasMetadata.expandedTypeAccept(  clazz, kotlinDeclarationContainerMetadata, this);
            kotlinTypeAliasMetadata.typeParametersAccept(clazz, kotlinDeclarationContainerMetadata, this);
        }


        // Implementations for KotlinValueParameterVisitor
        @Override
        public void visitAnyValueParameter(Clazz                        clazz,
                                           KotlinValueParameterMetadata kotlinValueParameterMetadata) {}

        @Override
        public void visitFunctionValParameter(Clazz                        clazz,
                                              KotlinMetadata               kotlinMetadata,
                                              KotlinFunctionMetadata       kotlinFunctionMetadata,
                                              KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinMetadata, kotlinFunctionMetadata, this);
        }

        @Override
        public void visitConstructorValParameter(Clazz                        clazz,
                                                 KotlinClassKindMetadata      kotlinClassKindMetadata,
                                                 KotlinConstructorMetadata    kotlinConstructorMetadata,
                                                 KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinClassKindMetadata, kotlinConstructorMetadata, this);
        }

        @Override
        public void visitPropertyValParameter(Clazz                              clazz,
                                              KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                              KotlinPropertyMetadata             kotlinPropertyMetadata,
                                              KotlinValueParameterMetadata       kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata, this);
        }


        // Implementations for KotlinTypeParameterVisitor
        @Override
        public void visitAnyTypeParameter(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            kotlinTypeParameterMetadata.annotationsAccept(clazz, this);
            kotlinTypeParameterMetadata.upperBoundsAccept(clazz, this);
        }


        // Implementations for KotlinAnnotationVisitor

        @Override
        public void visitAnyAnnotation(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation)
        {
            annotation.className = annotation.referencedAnnotationClass.getName();

            annotation.argumentsAccept(clazz, annotatable, this);
        }

        // Implementation for KotlinAnnotationArgumentVisitor

        @Override
        public void visitAnyArgument(Clazz                    clazz,
                                     KotlinAnnotatable        annotatable,
                                     KotlinAnnotation         annotation,
                                     KotlinAnnotationArgument argument,
                                     Value                    value)
        {
            argument.name =
                    argument.referencedAnnotationMethod.getName(annotation.referencedAnnotationClass);
        }

        @Override
        public void visitClassArgument(Clazz                               clazz,
                                       KotlinAnnotatable                   annotatable,
                                       KotlinAnnotation                    annotation,
                                       KotlinAnnotationArgument            argument,
                                       KotlinAnnotationArgument.ClassValue value)
        {
            this.visitAnyArgument(clazz, annotatable, annotation, argument, value);

            value.className = value.referencedClass.getName();
        }

        @Override
        public void visitEnumArgument(Clazz                    clazz,
                                      KotlinAnnotatable        annotatable,
                                      KotlinAnnotation         annotation,
                                      KotlinAnnotationArgument argument,
                                      EnumValue                value)
        {
            this.visitAnyArgument(clazz, annotatable, annotation, argument, value);

            value.className = value.referencedClass.getName();
        }
    }

    // Small utility methods.

    /**
     * Returns the short classname to be used as the nested classname.
     */
    public static String shortKotlinNestedClassName(String enclosingClassName,
                                                    String shortInnerClassName,
                                                    Clazz  referencedClass)
    {
        String newFulllName = newClassName(enclosingClassName + "$" + shortInnerClassName,
                                           referencedClass);

        if (newFulllName.equals(enclosingClassName + "$" + shortInnerClassName))
        {
            // If the name has not changed, no need to recompute the short name.
            // Original names may contain `$` so reusing the name here avoids the problem of
            // finding the short name from the full name.
            return shortInnerClassName;
        }
        else
        {
            return internalSimpleClassName(newFulllName);
        }
    }


    /**
     * Returns a new Kotlin function name, taking into account mangling.
     *
     * Mangled names are generated by the Kotlin compiler to avoid name clashes;
     * these names are the same as the original names, but with a suffix appended.
     *
     * The function name in the Kotlin metadata should use the demangled name.
     */
    private static String newKotlinFunctionName(String original, String name)
    {
        return name.startsWith(original + FUNCTION_NAME_MANGLE_SEPARATOR) ? original : name;
    }


    /**
     * Returns the new descriptor of a field after applying the obfuscation,
     * given the old descriptor and the referenced classes.
     */
    private static String newDescriptor(String descriptor,
                                        Clazz  referencedClass)
    {
        // If there is no referenced class, the descriptor won't change.
        if (referencedClass == null)
        {
            return descriptor;
        }

        // Unravel and reconstruct the class element of the descriptor.
        DescriptorClassEnumeration descriptorClassEnumeration =
            new DescriptorClassEnumeration(descriptor);

        StringBuffer newDescriptorBuffer = new StringBuffer(descriptor.length());
        newDescriptorBuffer.append(descriptorClassEnumeration.nextFluff());

        // Only if the descriptor contains a class name (e.g. with an array of
        // primitive types), the descriptor can change.
        if (descriptorClassEnumeration.hasMoreClassNames())
        {
            String className = descriptorClassEnumeration.nextClassName();
            String fluff     = descriptorClassEnumeration.nextFluff();

            String newClassName = newClassName(className,
                                               referencedClass);

            newDescriptorBuffer.append(newClassName);
            newDescriptorBuffer.append(fluff);
        }

        return newDescriptorBuffer.toString();
    }


    /**
     * Returns the new descriptor of a method after applying the obfuscation,
     * given the old descriptor and the referenced classes.
     */
    public static String newDescriptor(String  descriptor,
                                       Clazz[] referencedClasses)
    {
        // If there are no referenced classes, the descriptor won't change.
        if (referencedClasses == null ||
            referencedClasses.length == 0)
        {
            return descriptor;
        }

        // TODO: Remove the try/catch when the code has stabilized.
        // Catch any unexpected exceptions.
        try
        {
            // Unravel and reconstruct the class elements of the descriptor.
            DescriptorClassEnumeration descriptorClassEnumeration =
                new DescriptorClassEnumeration(descriptor);

            StringBuffer newDescriptorBuffer = new StringBuffer(descriptor.length());
            newDescriptorBuffer.append(descriptorClassEnumeration.nextFluff());

            int index = 0;
            while (descriptorClassEnumeration.hasMoreClassNames())
            {
                String  className        = descriptorClassEnumeration.nextClassName();
                boolean isInnerClassName = descriptorClassEnumeration.isInnerClassName();
                String  fluff            = descriptorClassEnumeration.nextFluff();

                String newClassName = newClassName(className,
                                                   referencedClasses[index++]);

                // Strip the outer class name again, if it's an inner class.
                if (isInnerClassName)
                {
                    newClassName =
                        newClassName.substring(newClassName.lastIndexOf(TypeConstants.INNER_CLASS_SEPARATOR)+1);
                }

                newDescriptorBuffer.append(newClassName);
                newDescriptorBuffer.append(fluff);
            }

            return newDescriptorBuffer.toString();
        }
        catch (RuntimeException e)
        {
            logger.error("Unexpected error while updating descriptor:");
            logger.error("  Descriptor = [{}]", descriptor);
            logger.error("  Referenced classes: {}", referencedClasses.length);
            for (int index = 0; index < referencedClasses.length; index++)
            {
                Clazz referencedClass = referencedClasses[index];
                if (referencedClass != null)
                {
                    logger.error("    #{}: [{}]", index, referencedClass.getName());
                }
            }

            throw e;
        }
    }


    /**
     * Returns the new class name based on the given class name and the new
     * name of the given referenced class. Class names of array types
     * are handled properly.
     */
    private static String newClassName(String className,
                                       Clazz  referencedClass)
    {
        // If there is no referenced class, the class name won't change.
        if (referencedClass == null)
        {
            return className;
        }

        // Reconstruct the class name.
        String newClassName = referencedClass.getName();

        // Is it an array type?
        if (className.charAt(0) == TypeConstants.ARRAY)
        {
            // Add the array prefixes and suffix "[L...;".
            newClassName =
                 className.substring(0, className.indexOf(TypeConstants.CLASS_START)+1) +
                 newClassName +
                 TypeConstants.CLASS_END;
        }

        return newClassName;
    }


    private static String newInnerClassName(String enclosingClassName,
                                            String shortInnerClassName,
                                            Clazz referencedClass)
    {
        String newFulllName = newClassName(enclosingClassName + "$" + shortInnerClassName,
                                           referencedClass);

        return newFulllName.substring(newFulllName.indexOf('$') + 1);
    }


    // Small utility helper methods for KotlinReferenceFixer.

    private static MethodSignature fixPropertyMethod(Clazz                       referencedMethodClass,
                                                     Method                      referencedMethod,
                                                     KotlinPropertyAccessorFlags flags,
                                                     MethodSignature             oldSignature)
    {
        if (oldSignature == null)
        {
            return null;
        }

        MethodSignature newSignature = new MethodSignature(referencedMethodClass, referencedMethod);

        if (!oldSignature.equals(newSignature) &&
            flags != null)
        {
            addJvmNameAnnotation((ProgramClass) referencedMethodClass,
                                 (ProgramMethod)referencedMethod);
            flags.common.hasAnnotations = true;
        }

        return newSignature;
    }


    private static void addJvmNameAnnotation(ProgramClass programClass, ProgramMethod programMethod)
    {
        ConstantPoolEditor editor = new ConstantPoolEditor(programClass);

        Annotation jvmName = new Annotation(editor.addUtf8Constant(TYPE_KOTLIN_JVM_JVMNAME),
                                            1,
                                            new ElementValue[]
                                            {
                                                new ConstantElementValue('s',
                                                                         editor.addUtf8Constant("name"),
                                                                         editor.addUtf8Constant(programMethod.getName(programClass)))
                                            });

        AttributesEditor attributesEditor     = new AttributesEditor(programClass, programMethod, false);
        Attribute        annotationsAttribute = attributesEditor.findAttribute(Attribute.RUNTIME_INVISIBLE_ANNOTATIONS);

        if (annotationsAttribute == null)
        {
             attributesEditor.addAttribute(
                new RuntimeInvisibleAnnotationsAttribute(
                    editor.addUtf8Constant(Attribute.RUNTIME_INVISIBLE_ANNOTATIONS), 1, new Annotation[] { jvmName })
             );
        }
        else
        {
            AnnotationsAttributeEditor annotationsAttributeEditor =
                new AnnotationsAttributeEditor((AnnotationsAttribute) annotationsAttribute);

            programMethod.attributesAccept(
                programClass,
                new AllAnnotationVisitor(
                new AnnotationTypeFilter(
                    TYPE_KOTLIN_JVM_JVMNAME,
                    new AnnotationVisitor() {
                        @Override
                        public void visitAnnotation(Clazz clazz, Annotation annotation) {
                            annotationsAttributeEditor.deleteAnnotation(annotation);
                        }
                    }
                )
            ));

            annotationsAttributeEditor.addAnnotation(jvmName);
        }
    }


    /**
     * This interface provides an abstraction on how a {@link Member} should be renamed when its descriptor
     * need to be updated. 
     */
    public interface NameGenerationStrategy
    {
        /**
         * A method for generating a new name for a program member.
         * @param programClass       The program class that the ClassReferenceFixer is visiting.
         * @param programMember      The program member that the ClassReferenceFixer is visiting.
         * @param originalName       The original name of the program member.
         * @param originalDescriptor The original descriptor of the program member.
         * @return The new name of the program member.
         */
        String getNewName(ProgramClass programClass, ProgramMember programMember, String originalName, String originalDescriptor);
    }
}
