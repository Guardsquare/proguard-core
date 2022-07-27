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

package proguard.classfile.kotlin.reflect.util;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.StringConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.AllInstructionVisitor;
import proguard.classfile.instruction.visitor.InstructionConstantVisitor;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.reflect.*;
import proguard.classfile.kotlin.visitor.*;
import proguard.classfile.kotlin.visitor.filter.KotlinPropertyFilter;
import proguard.classfile.util.InstructionSequenceMatcher;
import proguard.classfile.util.MemberFinder;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberNameFilter;
import proguard.classfile.visitor.MemberVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static proguard.classfile.ClassConstants.METHOD_NAME_INIT;
import static proguard.classfile.kotlin.KotlinConstants.REFLECTION;
import static proguard.classfile.util.InstructionSequenceMatcher.*;

/**
 * Initialize callable reference class information, by visiting synthetic classes that implement (Function|Property|LocalVariable)Reference,
 * then finding Function/Property that they refer to and use this information to initialize a {@link CallableReferenceInfo}
 * inside the synthetic class.
 *
 * FunctionReferences are lambda synthetic classes
 * PropertyReferences are regular synthetic classes
 * LocalVariableReferences extend PropertyReferences
 *
 * @author James Hamilton
 */
public class KotlinCallableReferenceInitializer
implements   KotlinMetadataVisitor
{
    private static final MemberFinder memberFinder = new MemberFinder();

    private final ClassPool programClassPool;
    private final ClassPool libraryClassPool;


    public KotlinCallableReferenceInitializer(ClassPool programClassPool, ClassPool libraryClassPool)
    {
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;
    }

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    @Override
    public void visitKotlinSyntheticClassMetadata(Clazz clazz, KotlinSyntheticClassKindMetadata syntheticClassKindMetadata)
    {
        if (clazz.extendsOrImplements(REFLECTION.CALLABLE_REFERENCE_CLASS_NAME))
        {
            Consumer<InfoLoaderResult> loader = result -> {
                 // First try to initialize a Kotlin reference (requires the owner Kotlin metadata)
                // otherwise try to initialize a Java reference.

                if (clazz.extendsOrImplements(REFLECTION.FUNCTION_REFERENCE_CLASS_NAME))
                {
                    int descriptorStart = result.callableSignature.indexOf('(');
                    Method method = descriptorStart == -1 ?
                                        null :
                                        memberFinder.findMethod(result.callableOwnerClass,
                                                                result.callableName,
                                                                result.callableSignature.substring(descriptorStart));

                    if (method != null)
                    {
                        Clazz methodReferencedClass = memberFinder.correspondingClass();

                        if (result.callableOwnerMetadata != null)
                        {
                            method.accept(methodReferencedClass,
                                new MethodToKotlinFunctionVisitor(
                                new FunctionReferenceInfoInitializer(result.callableOwnerClass,
                                                                     result.callableOwnerMetadata,
                                                                     syntheticClassKindMetadata)));
                        }

                        if (syntheticClassKindMetadata.callableReferenceInfo == null)
                        {
                            // The class didn't have any Kotlin metadata attached
                            // so let's instead initialize a Java reference.
                            syntheticClassKindMetadata.callableReferenceInfo =
                                new JavaMethodReferenceInfo(result.callableOwnerClass, methodReferencedClass, method);
                        }
                    }
                }
                else if (clazz.extendsOrImplements(REFLECTION.LOCALVAR_REFERENCE_CLASS_NAME))
                {
                    // Note: LocalVariableReference extends PropertyReference.
                    if (result.callableOwnerMetadata != null)
                    {
                        result.callableOwnerClass.kotlinMetadataAccept(
                            new LocalVariableReferenceInfoInitializer(
                                result.callableOwnerClass,
                                result.callableOwnerMetadata,
                                syntheticClassKindMetadata,
                                result.callableName,
                                result.callableSignature));
                    }
                }
                else if (clazz.extendsOrImplements(REFLECTION.PROPERTY_REFERENCE_CLASS_NAME))
                {
                    // Search the Kotlin hierarchy for the property, by name.

                    if (result.callableOwnerMetadata != null)
                    {
                        try
                        {
                            result.callableOwnerClass.hierarchyAccept(
                                true, true, false, false,
                                new ReferencedKotlinMetadataVisitor(
                                new AllPropertyVisitor(
                                new KotlinPropertyFilter(
                                    prop -> prop.name.equals(result.callableName),
                                    (_clazz, declarationContainerMetadata, propertyMetadata) -> {
                                        if (syntheticClassKindMetadata.callableReferenceInfo == null)
                                        {
                                            syntheticClassKindMetadata.callableReferenceInfo
                                                = new PropertyReferenceInfo(result.callableOwnerClass,
                                                                            result.callableOwnerMetadata,
                                                                            propertyMetadata);
                                            // Exit the search.
                                            throw new PropertyFoundException();
                                        }
                                    }))));
                        }
                        catch (PropertyFoundException ignored)
                        {
                            // Found.
                        }
                    }

                    if (syntheticClassKindMetadata.callableReferenceInfo == null)
                    {
                        // If we couldn't find any in the Kotlin metadata, we can search for the field by name/descriptor (assuming there is a backing field).

                        int descriptorEnd = result.callableSignature.lastIndexOf(')');
                        Field field = descriptorEnd == -1 ?
                                        null :
                                        memberFinder.findField(result.callableOwnerClass,
                                                               result.callableName,
                                                               result.callableSignature.substring(descriptorEnd + 1));

                        if (field != null)
                        {
                            syntheticClassKindMetadata.callableReferenceInfo =
                                new JavaFieldReferenceInfo(result.callableOwnerClass, memberFinder.correspondingClass(), field);
                        }
                    }
                }
            };

            clazz.accept(
                new OptimizedCallableReferenceFilter(
                    new CallableReferenceInfoLoader1dot4(loader),
                    new CallableReferenceInfoLoader1dot3(loader)));
        }
    }

    private static class InfoLoaderResult
    {
        // the "owner" (for the getOwner() method) is not necessarily
        // where the function/property is declared (e.g. could be in a superclass)
        Clazz                              callableOwnerClass;
        KotlinDeclarationContainerMetadata callableOwnerMetadata;

        String callableName;

        // For functions it's the JVM signature
        // For properties it's the getter signature (even if a getter doesn't exist)
        // For local variables it's like <v#0>
        String callableSignature;
    }

    /**
     * Helper class to load the callable reference information from the existing
     * implementations in a class generated by kotlin 1.4+ compiler
     */
    private class CallableReferenceInfoLoader1dot4
    implements    ClassVisitor,
                  InstructionVisitor
    {
        private static final int OWNER_INDEX     = A;
        private static final int NAME_INDEX      = B;
        private static final int SIGNATURE_INDEX = C;
        private static final int FLAGS_INDEX     = D;

        private final List<InstructionSequenceMatcher> matchers = new ArrayList<>();
        private final Consumer<InfoLoaderResult>       consumer;

        public CallableReferenceInfoLoader1dot4(Consumer<InfoLoaderResult> consumer)
        {
            this.consumer = consumer;
            InstructionSequenceBuilder builder = new InstructionSequenceBuilder(programClassPool, libraryClassPool)
                .ldc_(OWNER_INDEX)
                .ldc_(NAME_INDEX)
                .ldc_(SIGNATURE_INDEX)
                .ldc_(FLAGS_INDEX)
                .invokespecial(X);

            this.matchers.add(new InstructionSequenceMatcher(builder.constants(), builder.instructions()));

            builder
                .ldc_(OWNER_INDEX)
                .ldc_(NAME_INDEX)
                .ldc_(SIGNATURE_INDEX)
                .iconst(I)
                .invokespecial(X);

            this.matchers.add(new InstructionSequenceMatcher(builder.constants(), builder.instructions()));
        }


        @Override
        public void visitAnyClass(Clazz clazz)
        {
            for (InstructionSequenceMatcher matcher : matchers)
            {
                matcher.reset();
            }

            clazz.methodsAccept(
                    new MemberNameFilter(METHOD_NAME_INIT,
                    new AllAttributeVisitor(
                    new AllInstructionVisitor(this))));
        }


        @Override
        public void visitAnyInstruction(Clazz         clazz,
                                        Method        method,
                                        CodeAttribute codeAttribute,
                                        int           offset,
                                        Instruction instruction)
        {
            for (InstructionSequenceMatcher matcher : matchers)
            {
                instruction.accept(clazz, method, codeAttribute, offset, matcher);

                if (matcher.isMatching())
                {
                    InfoLoaderResult result = new InfoLoaderResult();
                    clazz.constantPoolEntryAccept(matcher.matchedConstantIndex(OWNER_INDEX), new ConstantVisitor() {
                        @Override
                        public void visitAnyConstant(Clazz clazz, Constant constant) { }

                        @Override
                        public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
                            result.callableOwnerClass = classConstant.referencedClass;
                        }
                    });

                    clazz.constantPoolEntryAccept(matcher.matchedConstantIndex(NAME_INDEX), new ConstantVisitor() {
                        @Override
                        public void visitAnyConstant(Clazz clazz, Constant constant) { }

                        @Override
                        public void visitStringConstant(Clazz clazz, StringConstant stringConstant) {
                            result.callableName = stringConstant.getString(clazz);
                        }
                    });

                    clazz.constantPoolEntryAccept(matcher.matchedConstantIndex(SIGNATURE_INDEX), new ConstantVisitor() {
                        @Override
                        public void visitAnyConstant(Clazz clazz, Constant constant) { }

                        @Override
                        public void visitStringConstant(Clazz clazz, StringConstant stringConstant) {
                            result.callableSignature = stringConstant.getString(clazz);
                        }
                    });

                    if (result.callableOwnerClass != null) {
                        result.callableOwnerClass.kotlinMetadataAccept(new KotlinMetadataVisitor() {
                            @Override
                            public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) { }

                            @Override
                            public void visitKotlinDeclarationContainerMetadata(Clazz clazz, KotlinDeclarationContainerMetadata declarationContainer) {
                                result.callableOwnerMetadata = declarationContainer;
                            }
                        });

                        if (result.callableName      != null &&
                            result.callableSignature != null)
                        {
                            this.consumer.accept(result);
                        }
                    }

                    break;
                }
            }
        }
    }

    /**
     * Helper class to load the callable reference information from the existing
     * implementations in a class generated by Kotlin <= 1.3.
     */
    private static class CallableReferenceInfoLoader1dot3
    implements           ClassVisitor,
                         MemberVisitor,
                         ConstantVisitor,
                         KotlinMetadataVisitor
    {
        private final Consumer<InfoLoaderResult> infoLoaderResultConsumer;
        private final InfoLoaderResult result = new InfoLoaderResult();
        private String currentMethod;

        public CallableReferenceInfoLoader1dot3(Consumer<InfoLoaderResult> infoLoaderResultConsumer)
        {
            this.infoLoaderResultConsumer = infoLoaderResultConsumer;
        }

        // Implementations for ClassVisitor.

        @Override
        public void visitAnyClass(Clazz clazz)
        {
            clazz.methodsAccept(this);

            if (result.callableOwnerClass != null)
            {
                result.callableOwnerClass.kotlinMetadataAccept(this);

                if (result.callableName      != null &&
                    result.callableSignature != null)
                {
                    this.infoLoaderResultConsumer.accept(result);
                }
            }
        }

        // Implementations for MemberVisitor.

        @Override
        public void visitAnyMember(Clazz clazz, Member member) {}

        @Override
        public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
        {
            this.currentMethod = programMethod.getName(programClass) + programMethod.getDescriptor(programClass);
            programMethod.accept(programClass,
                                 new AllAttributeVisitor(
                                 new AllInstructionVisitor(
                                 new InstructionConstantVisitor(this))));
        }

        // Implementations for ConstantVisitor.

        @Override
        public void visitAnyConstant(Clazz clazz, Constant constant) {}

        @Override
        public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
        {
            if (currentMethod.equals(REFLECTION.GETSIGNATURE_METHOD_NAME + REFLECTION.GETSIGNATURE_METHOD_DESC))
            {
                result.callableSignature = stringConstant.getString(clazz);
            }
            else if (currentMethod.equals(REFLECTION.GETNAME_METHOD_NAME + REFLECTION.GETNAME_METHOD_DESC))
            {
                result.callableName = stringConstant.getString(clazz);
            }
        }

        @Override
        public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
        {
            if (currentMethod.equals(REFLECTION.GETOWNER_METHOD_NAME + REFLECTION.GETOWNER_METHOD_DESC) &&
                result.callableOwnerClass == null)
            {
                // There is a class ref (+ possibly a string for the module name but that's not required
                // because we know the module from the Kotlin class).
                result.callableOwnerClass = classConstant.referencedClass;
            }
        }

        // Implementations for KotlinMetadataVisitor.

        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                            KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
        {
            result.callableOwnerMetadata = kotlinDeclarationContainerMetadata;
        }
    }

    // Helper class to initialize the FunctionCallableReferenceInfo

    private static class FunctionReferenceInfoInitializer
    implements           KotlinFunctionVisitor
    {
        private final Clazz                              ownerClass;
        private final KotlinDeclarationContainerMetadata ownerMetadata;
        private final KotlinSyntheticClassKindMetadata   syntheticClassKindMetadata;

        private FunctionReferenceInfoInitializer(Clazz                              ownerClass,
                                                 KotlinDeclarationContainerMetadata ownerMetadata,
                                                 KotlinSyntheticClassKindMetadata   syntheticClassKindMetadata)
        {
            this.ownerClass                 = ownerClass;
            this.ownerMetadata              = ownerMetadata;
            this.syntheticClassKindMetadata = syntheticClassKindMetadata;
        }

        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata) {}

        @Override
        public void visitFunction(Clazz                              clazz,
                                  KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                  KotlinFunctionMetadata             kotlinFunctionMetadata)
        {
            syntheticClassKindMetadata.callableReferenceInfo
                = new FunctionReferenceInfo(this.ownerClass, this.ownerMetadata, kotlinFunctionMetadata);
        }
    }

    // Helper class to initialize the LocalVariableReferenceInfo.

    public static class LocalVariableReferenceInfoInitializer
    implements          KotlinMetadataVisitor
    {

        private final Clazz                              ownerClass;
        private final KotlinDeclarationContainerMetadata owner;
        private final KotlinSyntheticClassKindMetadata   syntheticClassKindMetadata;
        private final String name;
        private final String signature;

        LocalVariableReferenceInfoInitializer(Clazz                              ownerClass,
                                              KotlinDeclarationContainerMetadata ownerMetadata,
                                              KotlinSyntheticClassKindMetadata   syntheticClassKindMetadata,
                                              String                             name,
                                              String                             signature)
        {
            this.ownerClass                 = ownerClass;
            this.owner                      = ownerMetadata;
            this.syntheticClassKindMetadata = syntheticClassKindMetadata;
            this.name                       = name;
            this.signature                  = signature;
        }

        // Implementations for KotlinMetadataVisitor.

        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                            KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
        {
            syntheticClassKindMetadata.callableReferenceInfo =
                new LocalVariableReferenceInfo(this.ownerClass, this.owner, this.name, this.signature);
        }
    }

    // Helper class to exit early from hierarchy property search.
    private static class PropertyFoundException extends RuntimeException {}
    
    // Optimized callable references don't override the getName, getSignature, getOwner
    // methods. This behaviour is default since Kotlin 1.4 and forcible with the compiler
    // argument "-Xno-optimized-callable-references".
    public static class OptimizedCallableReferenceFilter implements ClassVisitor
    {
        private final ClassVisitor optimizedVisitor;
        private final ClassVisitor nonOptimizedVisitor;

        public OptimizedCallableReferenceFilter(ClassVisitor optimizedVisitor, ClassVisitor nonOptimizedVisitor)
        {
            this.optimizedVisitor    = optimizedVisitor;
            this.nonOptimizedVisitor = nonOptimizedVisitor;
        }

        @Override
        public void visitAnyClass(Clazz clazz)
        {
            boolean isOptimized =
                clazz.findMethod(REFLECTION.GETNAME_METHOD_NAME,      REFLECTION.GETNAME_METHOD_DESC) == null &&
                clazz.findMethod(REFLECTION.GETSIGNATURE_METHOD_NAME, REFLECTION.GETSIGNATURE_METHOD_DESC) == null &&
                clazz.findMethod(REFLECTION.GETOWNER_METHOD_NAME,     REFLECTION.GETSIGNATURE_METHOD_DESC) == null;

            clazz.accept(isOptimized ? optimizedVisitor : nonOptimizedVisitor);
        }
    }
}
