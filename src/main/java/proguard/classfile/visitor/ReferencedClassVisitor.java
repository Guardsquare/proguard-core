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
package proguard.classfile.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.*;

import java.util.*;

/**
 * This {@link ClassVisitor}, {@link MemberVisitor}, {@link ConstantVisitor}, {@link AttributeVisitor}, etc.
 * lets a given {@link ClassVisitor} visit all the referenced classes of the elements
 * that it visits. Only downstream elements are considered (in order to avoid
 * loops and repeated visits)
 *
 * It also considers references in the Kotlin metadata of this class, if includeKotlinMetadata = true.
 * So that, for example, a Kotlin function referencing a type alias
 * will execute the delegate class visitor for the referencedClass of the alias.
 *
 * @author Eric Lafortune
 * @author James Hamilton
 */
public class ReferencedClassVisitor
implements   ClassVisitor,
             MemberVisitor,
             ConstantVisitor,
             AttributeVisitor,
             LocalVariableInfoVisitor,
             LocalVariableTypeInfoVisitor,
             AnnotationVisitor,
             ElementValueVisitor
{
    private final ClassVisitor                 classVisitor;
    private final KotlinReferencedClassVisitor kotlinReferencedClassVisitor;


    public ReferencedClassVisitor(ClassVisitor classVisitor)
    {
        this(false, classVisitor);
    }


    public ReferencedClassVisitor(boolean      includeKotlinMetadata,
                                  ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
        this.kotlinReferencedClassVisitor = includeKotlinMetadata ? new KotlinReferencedClassVisitor() : null;
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
        // Visit the constant pool entries.
        programClass.constantPoolEntriesAccept(this);

        // Visit the fields and methods.
        programClass.fieldsAccept(this);
        programClass.methodsAccept(this);

        // Visit the attributes.
        programClass.attributesAccept(this);

        if (kotlinReferencedClassVisitor != null)
        {
            programClass.kotlinMetadataAccept(kotlinReferencedClassVisitor);
        }
    }


    @Override
    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Visit the superclass and interfaces.
        libraryClass.superClassAccept(classVisitor);
        libraryClass.interfacesAccept(classVisitor);

        // Visit the fields and methods.
        libraryClass.fieldsAccept(this);
        libraryClass.methodsAccept(this);

        if (kotlinReferencedClassVisitor != null)
        {
            libraryClass.kotlinMetadataAccept(kotlinReferencedClassVisitor);
        }
    }


    // Implementations for MemberVisitor.

    @Override
    public void visitProgramMember(ProgramClass programClass, ProgramMember programMember)
    {
        // Let the visitor visit the classes referenced in the descriptor string.
        programMember.referencedClassesAccept(classVisitor);

        // Visit the attributes.
        programMember.attributesAccept(programClass, this);
    }


    @Override
    public void visitLibraryMember(LibraryClass programClass, LibraryMember libraryMember)
    {
        // Let the visitor visit the classes referenced in the descriptor string.
        libraryMember.referencedClassesAccept(classVisitor);
    }


    // Implementations for ConstantVisitor.

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    @Override
    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        // Let the visitor visit the class referenced in the string constant.
        stringConstant.referencedClassAccept(classVisitor);
    }


    @Override
    public void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
    {
        // Let the visitor visit the class referenced in the reference constant.
        refConstant.referencedClassAccept(classVisitor);
    }


    @Override
    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        // Let the visitor visit the class referenced in the reference constant.
        invokeDynamicConstant.referencedClassesAccept(classVisitor);
    }


    @Override
    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Let the visitor visit the class referenced in the class constant.
        classConstant.referencedClassAccept(classVisitor);
    }


    @Override
    public void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant methodTypeConstant)
    {
        // Let the visitor visit the classes referenced in the method type constant.
        methodTypeConstant.referencedClassesAccept(classVisitor);
    }


    // Implementations for AttributeVisitor.

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    @Override
    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        // Let the visitor visit the class of the enclosing method.
        enclosingMethodAttribute.referencedClassAccept(classVisitor);
    }


    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Visit the attributes of the code attribute.
        codeAttribute.attributesAccept(clazz, method, this);
    }


    @Override
    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Visit the local variables.
        localVariableTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }


    @Override
    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Visit the local variable types.
        localVariableTypeTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }


    @Override
    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        // Let the visitor visit the classes referenced in the signature string.
        signatureAttribute.referencedClassesAccept(classVisitor);
    }


    @Override
    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        // Visit the annotations.
        annotationsAttribute.annotationsAccept(clazz, this);
    }


    @Override
    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        // Visit the parameter annotations.
        parameterAnnotationsAttribute.annotationsAccept(clazz, method, this);
    }


    @Override
    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        // Visit the default element value.
        annotationDefaultAttribute.defaultValueAccept(clazz, this);
    }


    // Implementations for LocalVariableInfoVisitor.

    @Override
    public void visitLocalVariableInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableInfo localVariableInfo)
    {
        // Let the visitor visit the class referenced in the local variable.
        localVariableInfo.referencedClassAccept(classVisitor);
    }


    // Implementations for LocalVariableTypeInfoVisitor.

    @Override
    public void visitLocalVariableTypeInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeInfo localVariableTypeInfo)
    {
        // Let the visitor visit the classes referenced in the local variable type.
        localVariableTypeInfo.referencedClassesAccept(classVisitor);
    }


    // Implementations for AnnotationVisitor.

    @Override
    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        // Let the visitor visit the classes referenced in the annotation.
        annotation.referencedClassesAccept(classVisitor);

        // Visit the element values.
        annotation.elementValuesAccept(clazz, this);
    }


    // Implementations for ElementValueVisitor.

    @Override
    public void visitAnyElementValue(Clazz clazz, Annotation annotation, ElementValue elementValue) {}


    @Override
    public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        // Let the visitor visit the classes referenced in the constant element value.
        enumConstantElementValue.referencedClassesAccept(classVisitor);
    }


    @Override
    public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        // Let the visitor visit the classes referenced in the class element value.
        classElementValue.referencedClassesAccept(classVisitor);
    }


    @Override
    public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        // Visit the contained annotation.
        annotationElementValue.annotationAccept(clazz, this);
    }


    @Override
    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        // Visit the element values.
        arrayElementValue.elementValuesAccept(clazz, annotation, this);
    }

    private class KotlinReferencedClassVisitor
    implements    KotlinMetadataVisitor,
                  KotlinTypeVisitor,
                  KotlinAnnotationVisitor,
                  KotlinTypeAliasVisitor,
                  KotlinFunctionVisitor,
                  KotlinTypeParameterVisitor,
                  KotlinValueParameterVisitor,
                  KotlinPropertyVisitor
    {

        // Implementations for KotlinTypeVisitor.

        @Override
        public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata)
        {
            if (kotlinTypeMetadata.referencedClass != null)
            {
                kotlinTypeMetadata.referencedClass.accept(classVisitor);
            }
        }


        // Implementations for KotlinAnnotationVisitor.

        @Override
        public void visitAnyAnnotation(Clazz clazz, KotlinAnnotatable annotatable, KotlinMetadataAnnotation annotation)
        {
            if (annotation.referencedAnnotationClass != null)
            {
                annotation.referencedAnnotationClass.accept(classVisitor);
                annotation.referencedArgumentMethods.values().stream()
                    .filter(Objects::nonNull)
                    .forEach(method -> method.referencedClassesAccept(classVisitor));
            }
        }


        // Implementations for KotlinMetadataVisitor.

        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) { }


        @Override
        public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                            KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
        {
            kotlinDeclarationContainerMetadata.typeAliasesAccept(clazz, this);
            kotlinDeclarationContainerMetadata.functionsAccept(clazz, this);
            kotlinDeclarationContainerMetadata.propertiesAccept(clazz, this);
            kotlinDeclarationContainerMetadata.delegatedPropertiesAccept(clazz, this);
        }


        @Override
        public void visitKotlinSyntheticClassMetadata(Clazz                            clazz,
                                                      KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata)
        {
            kotlinSyntheticClassKindMetadata.functionsAccept(clazz, this);
        }


        // Implementations for KotlinTypeAliasVisitor.

        @Override
        public void visitTypeAlias(Clazz                              clazz,
                                   KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                   KotlinTypeAliasMetadata            kotlinTypeAliasMetadata)
        {
            kotlinTypeAliasMetadata.annotationsAccept(   clazz, this);
            kotlinTypeAliasMetadata.typeParametersAccept(clazz, kotlinDeclarationContainerMetadata, this);
            kotlinTypeAliasMetadata.expandedTypeAccept(  clazz, kotlinDeclarationContainerMetadata, this);
            kotlinTypeAliasMetadata.underlyingTypeAccept(clazz, kotlinDeclarationContainerMetadata, this);
        }


        // Implementations for KotlinFunctionVisitor.

        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata)
        {
            kotlinFunctionMetadata.receiverTypeAccept(clazz,    kotlinMetadata, this);
            kotlinFunctionMetadata.returnTypeAccept(clazz,      kotlinMetadata, this);
            kotlinFunctionMetadata.typeParametersAccept(clazz,  kotlinMetadata, this);
            kotlinFunctionMetadata.valueParametersAccept(clazz, kotlinMetadata, this);
        }


        // Implementations for KotlinTypeParameterVisitor.

        @Override
        public void visitAnyTypeParameter(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            kotlinTypeParameterMetadata.annotationsAccept(clazz, this);
            kotlinTypeParameterMetadata.upperBoundsAccept(clazz, this);
        }


        // Implementations for KotlinValueParameterVisitor.

        @Override
        public void visitAnyValueParameter(Clazz clazz, KotlinValueParameterMetadata kotlinValueParameterMetadata) { }


        @Override
        public void visitConstructorValParameter(Clazz                        clazz,
                                                 KotlinClassKindMetadata      kotlinClassKindMetadata,
                                                 KotlinConstructorMetadata    kotlinConstructorMetadata,
                                                 KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinClassKindMetadata, kotlinConstructorMetadata, this);
        }


        @Override
        public void visitFunctionValParameter(Clazz                        clazz,
                                              KotlinMetadata               kotlinMetadata,
                                              KotlinFunctionMetadata       kotlinFunctionMetadata,
                                              KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinMetadata, kotlinFunctionMetadata, this);
        }


        @Override
        public void visitPropertyValParameter(Clazz                              clazz,
                                              KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                              KotlinPropertyMetadata             kotlinPropertyMetadata,
                                              KotlinValueParameterMetadata       kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata, this);
        }


        // Implementations for KotlinPropertyVisitor.

        @Override
        public void visitAnyProperty(Clazz                               clazz,
                                     KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                     KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            kotlinPropertyMetadata.receiverTypeAccept(    clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.typeAccept(            clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.setterParametersAccept(clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.typeParametersAccept(  clazz, kotlinDeclarationContainerMetadata, this);
        }
    }
}
