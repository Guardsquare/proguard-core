/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.classfile.kotlin.reflect.util;

import proguard.classfile.*;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.visitor.*;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.reflect.*;
import proguard.classfile.kotlin.visitor.*;
import proguard.classfile.kotlin.visitor.filter.KotlinPropertyFilter;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

import static proguard.classfile.kotlin.KotlinConstants.REFLECTION;

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

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    @Override
    public void visitKotlinSyntheticClassMetadata(Clazz clazz, KotlinSyntheticClassKindMetadata syntheticClassKindMetadata)
    {
        if (clazz.extendsOrImplements(REFLECTION.CALLABLE_REFERENCE_CLASS_NAME))
        {
            CallableReferenceInfoLoader infoLoader = new CallableReferenceInfoLoader();

            clazz.accept(infoLoader);

            if (infoLoader.hasResult())
            {
                // First try to initialize a Kotlin reference (requires the owner Kotlin metadata)
                // otherwise try to initialize a Java reference.

                if (clazz.extendsOrImplements(REFLECTION.FUNCTION_REFERENCE_CLASS_NAME))
                {
                    Method method =
                        memberFinder.findMethod(infoLoader.callableOwnerClass,
                                                infoLoader.callableName,
                                                infoLoader.callableSignature.substring(infoLoader.callableSignature.indexOf('(')));

                    if (method != null)
                    {
                        Clazz methodReferencedClass = memberFinder.correspondingClass();

                        if (infoLoader.hasOwnerMetadata())
                        {
                            method.accept(methodReferencedClass,
                                new MethodToKotlinFunctionVisitor(
                                new FunctionReferenceInfoInitializer(infoLoader.callableOwnerClass,
                                                                     infoLoader.callableOwnerMetadata,
                                                                     syntheticClassKindMetadata)));
                        }

                        if (syntheticClassKindMetadata.callableReferenceInfo == null)
                        {
                            // The class didn't have any Kotlin metadata attached
                            // so let's instead initialize a Java reference.
                            syntheticClassKindMetadata.callableReferenceInfo =
                                new JavaMethodReferenceInfo(infoLoader.callableOwnerClass, methodReferencedClass, method);
                        }
                    }
                }
                else if (clazz.extendsOrImplements(REFLECTION.LOCALVAR_REFERENCE_CLASS_NAME))
                {
                    // Note: LocalVariableReference extends PropertyReference.
                    if (infoLoader.hasOwnerMetadata())
                    {
                        infoLoader.callableOwnerClass.kotlinMetadataAccept(
                            new LocalVariableReferenceInfoInitializer(
                                infoLoader.callableOwnerClass,
                                infoLoader.callableOwnerMetadata,
                                syntheticClassKindMetadata,
                                infoLoader.callableName,
                                infoLoader.callableSignature));
                    }
                }
                else if (clazz.extendsOrImplements(REFLECTION.PROPERTY_REFERENCE_CLASS_NAME))
                {
                    // Search the Kotlin hierarchy for the property, by name.

                    if (infoLoader.hasOwnerMetadata())
                    {
                        try
                        {
                            infoLoader.callableOwnerClass.hierarchyAccept(
                                true, true, false, false,
                                new ReferencedKotlinMetadataVisitor(
                                new AllKotlinPropertiesVisitor(
                                new KotlinPropertyFilter(
                                    prop -> prop.name.equals(infoLoader.callableName),
                                    (_clazz, declarationContainerMetadata, propertyMetadata) -> {
                                        if (syntheticClassKindMetadata.callableReferenceInfo == null)
                                        {
                                            syntheticClassKindMetadata.callableReferenceInfo
                                                = new PropertyReferenceInfo(infoLoader.callableOwnerClass,
                                                                            infoLoader.callableOwnerMetadata,
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

                        Field field =
                            memberFinder.findField(infoLoader.callableOwnerClass,
                                                   infoLoader.callableName,
                                                   infoLoader.callableSignature.substring(infoLoader.callableSignature.lastIndexOf(')') + 1));

                        if (field != null)
                        {
                            syntheticClassKindMetadata.callableReferenceInfo =
                                new JavaFieldReferenceInfo(infoLoader.callableOwnerClass, memberFinder.correspondingClass(), field);
                        }
                    }
                }
            }
        }
    }


    /**
     * Helper class to load the callable reference information from the existing
     * implementations in a class i.e. a class that implements CallableReference.
     */
    private static class CallableReferenceInfoLoader
    extends              SimplifiedVisitor
    implements           ClassVisitor,
                         MemberVisitor,
                         ConstantVisitor,
                         KotlinMetadataVisitor
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

        private String currentMethod;

        // Implementations for ClassVisitor.

        @Override
        public void visitAnyClass(Clazz clazz)
        {
            clazz.methodsAccept(this);
            if (this.callableOwnerClass != null)
            {
                this.callableOwnerClass.kotlinMetadataAccept(this);
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
                this.callableSignature = stringConstant.getString(clazz);
            }
            else if (currentMethod.equals(REFLECTION.GETNAME_METHOD_NAME + REFLECTION.GETNAME_METHOD_DESC))
            {
                this.callableName = stringConstant.getString(clazz);
            }
        }

        @Override
        public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
        {
            if (currentMethod.equals(REFLECTION.GETOWNER_METHOD_NAME + REFLECTION.GETOWNER_METHOD_DESC))
            {
                // There is a class ref (+ possibly a string for the module name but that's not required
                // because we know the module from the Kotlin class).
                this.callableOwnerClass = classConstant.referencedClass;
            }
        }

        // Implementations for KotlinMetadataVisitor.

        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                            KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
        {
            this.callableOwnerMetadata = kotlinDeclarationContainerMetadata;
        }

        // Helper methods.

        boolean hasResult()
        {
            return this.callableOwnerClass != null &&
                   this.callableName       != null &&
                   this.callableSignature  != null;
        }

        boolean hasOwnerMetadata()
        {
            return this.callableOwnerMetadata != null;
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
}
