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
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.visitor.AllElementValueVisitor;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

/**
 * This {@link ClassVisitor} fixes the access modifiers of all classes and class
 * members that are referenced by the classes that it visits.
 *
 * @author Eric Lafortune
 */
public class AccessFixer
implements   ClassVisitor
{
    private final ClassVisitor referencedClassFixer  =
        new ReferencedClassVisitor(
        new MyReferencedClassAccessFixer());

    private final ClassVisitor referencedMemberFixer =
        new AllMethodVisitor(
        new AllAttributeVisitor(
        new AllInstructionVisitor(
        new MyReferencedMemberVisitor(
        new MyReferencedMemberAccessFixer()))));

    private final ClassVisitor referencedAnnotationMethodFixer =
        new AllAttributeVisitor(true,
        new AllElementValueVisitor(
        new MyReferencedMemberVisitor(
        new MyReferencedMemberAccessFixer())));

    private final ClassVisitor methodHierarchyFixer =
        new AllMethodVisitor(
        new MemberAccessFilter(0, AccessConstants.PRIVATE |
                                  AccessConstants.STATIC,
        new InitializerMethodFilter(null,
        new SimilarMemberVisitor(false, true, false, true,
        new MemberAccessFilter(0, AccessConstants.PRIVATE |
                                  AccessConstants.STATIC,
        new MyReferencedMemberAccessFixer())))));


    // Fields acting as parameters for the visitors.

    private Clazz referencingClass;
    private int   referencingMethodAccessFlags;
    private Clazz referencedClass;


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        // Remember the referencing class.
        referencingClass = programClass;

        // Fix the referenced classes.
        referencedClassFixer.visitProgramClass(programClass);

        // Fix the referenced class members.
        referencedMemberFixer.visitProgramClass(programClass);

        // Fix the referenced annotation methods.
        referencedAnnotationMethodFixer.visitProgramClass(programClass);

        // Fix overridden and overriding methods up and down the hierarchy.
        // They are referenced implicitly and need to be accessible too.
        referencingMethodAccessFlags = 0;
        referencedClass              = null;

        methodHierarchyFixer.visitProgramClass(programClass);
    }


    /**
     * This ReferencedMemberVisitor is an InstructionVisitor that also
     * remembers the access flags of the referencing methods, and the
     * referenced class.
     */
    private class MyReferencedMemberVisitor
    extends       ReferencedMemberVisitor
    implements    InstructionVisitor
    {
        public MyReferencedMemberVisitor(MemberVisitor memberVisitor)
        {
            super(memberVisitor);
        }


        // Implementations for InstructionVisitor.

        public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}


        public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
        {
            // Remember the access flags.
            referencingMethodAccessFlags = method.getAccessFlags();

            // Fix the referenced classes and class members.
            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this);
        }


        // Overridden methods for ConstantVisitor.

        public void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
        {
            // Remember the referenced class. Note that we're interested in the
            // class of the invocation, not in the class in which the member was
            // actually found, unless it is an array type.
            if (ClassUtil.isInternalArrayType(refConstant.getClassName(clazz)))
            {
                // For an array type, the class will be java.lang.Object.
                referencedClass = refConstant.referencedClass;
            }
            else
            {
                // Remember the referenced class.
                clazz.constantPoolEntryAccept(refConstant.u2classIndex, this);
            }

            // Fix the access flags of referenced class member.
            super.visitAnyRefConstant(clazz, refConstant);
        }


        public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
        {
            // Remember the referenced class.
            referencedClass = classConstant.referencedClass;
        }


        // Implementations for ElementValueVisitor.

        public void visitAnyElementValue(Clazz clazz, Annotation annotation, ElementValue elementValue)
        {
            // Set the referencing access flags and set the referenced class.
            referencingMethodAccessFlags = AccessConstants.STATIC;
            referencedClass              = elementValue.referencedClass;

            // Fix the access flags of referenced annotation method.
            super.visitAnyElementValue(clazz, annotation, elementValue);
        }
    }


    /**
     * This ClassVisitor fixes the access flags of the classes that it visits,
     * relative to the referencing class.
     */
    private class MyReferencedClassAccessFixer
    implements    ClassVisitor,
                  AttributeVisitor,
                  InnerClassesInfoVisitor
    {
        // Implementations for ClassVisitor.

        @Override
        public void visitAnyClass(Clazz clazz) { }


        @Override
        public void visitProgramClass(ProgramClass programClass)
        {
            // Do we need to update the access flags?
            int currentAccessFlags = programClass.getAccessFlags();
            int currentAccessLevel = AccessUtil.accessLevel(currentAccessFlags);
            if (currentAccessLevel < AccessUtil.PUBLIC)
            {
                // Compute the required access level.
                int requiredAccessLevel =
                    inSamePackage(programClass, referencingClass) ?
                        AccessUtil.PACKAGE_VISIBLE :
                        AccessUtil.PUBLIC;

                // Fix the class access flags if necessary.
                if (currentAccessLevel < requiredAccessLevel)
                {
                    programClass.u2accessFlags =
                        AccessUtil.replaceAccessFlags(currentAccessFlags,
                                                      AccessUtil.accessFlags(requiredAccessLevel));
                }
            }

            // Also check the InnerClasses attribute, if any.
            programClass.attributesAccept(this);
        }


        // Implementations for AttributeVisitor.

        public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


        public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
        {
            innerClassesAttribute.innerClassEntriesAccept(clazz, this);
        }


        // Implementations for InnerClassesInfoVisitor.

        public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo)
        {
            // Is this an inner class?
            int innerClassIndex = innerClassesInfo.u2innerClassIndex;
            if (innerClassIndex != 0)
            {
                String innerClassName = clazz.getClassName(innerClassIndex);
                if (innerClassName.equals(clazz.getName()))
                {
                    // Do we need to update the access flags?
                    int currentAccessFlags = innerClassesInfo.u2innerClassAccessFlags;
                    int currentAccessLevel = AccessUtil.accessLevel(currentAccessFlags);
                    if (currentAccessLevel < AccessUtil.PUBLIC)
                    {
                        // Compute the required access level.
                        int requiredAccessLevel =
                            inSamePackage(clazz, referencingClass) ?
                                AccessUtil.PACKAGE_VISIBLE :
                                AccessUtil.PUBLIC;

                        // Fix the inner class access flags if necessary.
                        if (currentAccessLevel < requiredAccessLevel)
                        {
                            innerClassesInfo.u2innerClassAccessFlags =
                                AccessUtil.replaceAccessFlags(currentAccessFlags,
                                                              AccessUtil.accessFlags(requiredAccessLevel));
                        }
                    }
                }
            }
        }

    }


    /**
     * This MemberVisitor fixes the access flags of the class members that it
     * visits, relative to the referencing class and method.
     */
    private class MyReferencedMemberAccessFixer
    implements    MemberVisitor
    {
        // Implementations for MemberVisitor.

        public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
        {
            // Don't update package-private final methods that are shadowed in a subclass.
            int currentAccessFlags = programMethod.getAccessFlags();
            int currentAccessLevel = AccessUtil.accessLevel(currentAccessFlags);
            if (currentAccessLevel == AccessUtil.PACKAGE_VISIBLE && (currentAccessFlags & AccessConstants.FINAL) != 0)
            {
                MethodCounter counter = new MethodCounter();
                programMethod.accept(programClass,
                    new SimilarMemberVisitor(false, false, false, true,
                    counter));

                if (counter.getCount() > 0)
                {
                    return;
                }
            }

            visitProgramMember(programClass, programMethod);
        }

        public void visitLibraryMember(LibraryClass libraryClass, LibraryMember libraryMember) {}


        public void visitProgramMember(ProgramClass programClass, ProgramMember programMember)
        {
            // Do we need to update the access flags?
            int currentAccessFlags = programMember.getAccessFlags();
            int currentAccessLevel = AccessUtil.accessLevel(currentAccessFlags);
            if (currentAccessLevel < AccessUtil.PUBLIC)
            {
                // Compute the required access level.
                // For protected access:
                // - The referencing method may not be static.
                // - The invoked class must be the referencing class (or a
                //   subclass, which may be counter-intuitive), to avoid
                //   invoking protected super methods on instances that are
                //   not of the referencing type, which the verifier doesn't
                //   allow. (test2172) [DGD-1258]
                // - The class that actually contains the member must be a
                //   super class.
                int requiredAccessLevel =
                    programClass.equals(referencingClass)         ? AccessUtil.PRIVATE         :
                    inSamePackage(programClass, referencingClass) ? AccessUtil.PACKAGE_VISIBLE :
                    (referencingMethodAccessFlags & AccessConstants.STATIC) == 0 &&
                    (referencedClass == null ||
                     referencedClass.extends_(referencingClass))                 &&
                    referencingClass.extends_(programClass)       ? AccessUtil.PROTECTED       :
                                                                    AccessUtil.PUBLIC;

                // Fix the class member access flags if necessary.
                if (currentAccessLevel < requiredAccessLevel)
                {
                    programMember.u2accessFlags =
                        AccessUtil.replaceAccessFlags(currentAccessFlags,
                                                      AccessUtil.accessFlags(requiredAccessLevel));
                }
            }
        }
    }


    // Small utility methods.

    /**
     * Returns whether the two given classes are in the same package.
     */
    private boolean inSamePackage(Clazz class1, Clazz class2)
    {
        return ClassUtil.internalPackageName(class1.getName()).equals(
               ClassUtil.internalPackageName(class2.getName()));
    }
}
