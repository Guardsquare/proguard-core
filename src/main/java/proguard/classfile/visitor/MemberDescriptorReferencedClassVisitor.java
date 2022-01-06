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
package proguard.classfile.visitor;

import proguard.classfile.*;
import proguard.classfile.kotlin.KotlinFunctionMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinTypeMetadata;
import proguard.classfile.kotlin.KotlinValueParameterMetadata;
import proguard.classfile.kotlin.visitor.*;
import proguard.classfile.kotlin.visitor.filter.KotlinClassKindFilter;

/**
 * This {@link MemberVisitor} lets a given {@link ClassVisitor} visit all the classes
 * referenced by the descriptors of the class members that it visits.
 *
 * It also takes into account functions with Kotlin inline class parameters, if
 * includeKotlinMetadata = true: in the case of inline classes, in the underlying JVM
 * method the actual class will not be referenced since the Kotlin compiler inlines uses.
 *
 * @author Eric Lafortune
 */
public class MemberDescriptorReferencedClassVisitor
implements   MemberVisitor
{
    private final ClassVisitor classVisitor;
    private final KotlinFunctionDescriptorReferenceVisitor kotlinFunRefVisitor;

    public MemberDescriptorReferencedClassVisitor(ClassVisitor classVisitor)
    {
        this(false, classVisitor);
    }

    public MemberDescriptorReferencedClassVisitor(boolean includeKotlinMetadata, ClassVisitor classVisitor)
    {
        this.classVisitor        = classVisitor;
        this.kotlinFunRefVisitor = includeKotlinMetadata ? new KotlinFunctionDescriptorReferenceVisitor() : null;
    }


    // Implementations for MemberVisitor.

    public void visitProgramMember(ProgramClass programClass, ProgramMember programMember)
    {
        // Let the visitor visit the classes referenced in the descriptor string.
        programMember.referencedClassesAccept(classVisitor);
        if (this.kotlinFunRefVisitor != null)
        {
            programMember.accept(programClass, new MethodToKotlinFunctionVisitor(this.kotlinFunRefVisitor));
        }
    }


    public void visitLibraryMember(LibraryClass libraryClass, LibraryMember libraryMember)
    {
        // Let the visitor visit the classes referenced in the descriptor string.
        libraryMember.referencedClassesAccept(classVisitor);
        if (this.kotlinFunRefVisitor != null)
        {
            libraryMember.accept(libraryClass, new MethodToKotlinFunctionVisitor(this.kotlinFunRefVisitor));
        }
    }


    private class KotlinFunctionDescriptorReferenceVisitor
    implements KotlinFunctionVisitor,
               KotlinValueParameterVisitor,
               KotlinTypeVisitor
    {

        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata)
        {
            kotlinFunctionMetadata.valueParametersAccept(clazz, kotlinMetadata, this);
        }

        @Override
        public void visitAnyValueParameter(Clazz clazz, KotlinValueParameterMetadata kotlinValueParameterMetadata) { }

        @Override
        public void visitFunctionValParameter(Clazz                        clazz,
                                              KotlinMetadata               kotlinMetadata,
                                              KotlinFunctionMetadata       kotlinFunctionMetadata,
                                              KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinMetadata, kotlinFunctionMetadata, this);
        }

        @Override
        public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata)
        {
            kotlinTypeMetadata.referencedClassAccept(
                new ReferencedKotlinMetadataVisitor(
                    new KotlinClassKindFilter(
                        metadata -> metadata.flags.isValue,
                        new KotlinMetadataToClazzVisitor(classVisitor)
                    )
                )
            );
        }
    }
}
