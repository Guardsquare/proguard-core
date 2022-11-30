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
package proguard.classfile.util;

import proguard.classfile.AccessConstants;
import proguard.classfile.Clazz;
import proguard.classfile.LibraryMember;
import proguard.classfile.Member;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.constant.AnyMethodrefConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.visitor.AllInstructionVisitor;
import proguard.classfile.instruction.visitor.InstructionConstantVisitor;
import proguard.classfile.instruction.visitor.InstructionOpCodeFilter;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinMultiFileFacadeKindMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberAccessFilter;
import proguard.classfile.visitor.MemberVisitor;
import proguard.util.Processable;

import java.util.HashMap;
import java.util.Map;

import static proguard.classfile.instruction.Instruction.OP_INVOKESTATIC;

/**
 * This {@link ClassVisitor} links all corresponding non-private, non-static,
 * non-initializer methods in the class hierarchies of all visited classes.
 * Visited classes are typically all class files that are not being subclassed.
 * Chains of links that have been created in previous invocations are merged
 * with new chains of links, in order to create a consistent set of chains.
 *
 * @author Eric Lafortune
 */
public class MethodLinker
implements   ClassVisitor,
             MemberVisitor
{
    // An object that is reset and reused every time.
    // The map: [method name+' '+descriptor - method info]
    private final Map<String, Member> memberMap = new HashMap<>();


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        // Collect all non-private, non-static members in this class hierarchy.
        clazz.hierarchyAccept(true, true, true, false,
            new AllMethodVisitor(
            new MemberAccessFilter(0, AccessConstants.PRIVATE | AccessConstants.STATIC,
            this)));

        // Link all the methods in the MultiFileFacades with their implementations in the corresponding MultiFilePart.
        clazz.kotlinMetadataAccept(new KotlinMultiFileFacadeMethodLinker());

        // Clean up for the next class hierarchy.
        memberMap.clear();
    }


    // Implementations for MemberVisitor.

    @Override
    public void visitAnyMember(Clazz clazz, Member member)
    {
        // Get the method's name and descriptor.
        String name       = member.getName(clazz);
        String descriptor = member.getDescriptor(clazz);

        // Special cases: <clinit> and <init> are always kept unchanged.
        // We can ignore them here.
        if (ClassUtil.isInitializer(name))
        {
            return;
        }

        // See if we've already come across a method with the same name and
        // descriptor.
        String key = name + ' ' + descriptor;
        Member otherMember = memberMap.get(key);

        if (otherMember == null)
        {
            // Get the last method in the chain.
            Member thisLastMember = lastMember(member);

            // Store the new class method in the map.
            memberMap.put(key, thisLastMember);
        }
        else if ((member.getAccessFlags() & AccessConstants.FINAL) == 0)
        {
            // Link both members if the current member is not final.
            link(member, otherMember);
        }
    }


    // Small utility methods.

    /**
     * Links the two given methods.
     */
    private static void link(Member member1, Member member2)
    {
        // Get the last methods in the both chains.
        Member lastMember1 = lastMember(member1);
        Member lastMember2 = lastMember(member2);

        // Check if both link chains aren't already ending in the same element.
        if (!lastMember1.equals(lastMember2))
        {
            // Merge the two chains, with the library members last.
            if (lastMember2 instanceof LibraryMember)
            {
                lastMember1.setProcessingInfo(lastMember2);
            }
            else
            {
                lastMember2.setProcessingInfo(lastMember1);
            }
        }
    }


    /**
     * Finds the last method in the linked list of related methods.
     * @param member The given method.
     * @return The last method in the linked list.
     */
    public static Member lastMember(Member member)
    {
        Member lastMember = member;
        while (lastMember.getProcessingInfo() != null &&
               lastMember.getProcessingInfo() instanceof Member)
        {
            lastMember = (Member)lastMember.getProcessingInfo();
        }

        return lastMember;
    }


    /**
     * Finds the last method in the linked list of related methods.
     * @param processable The given method.
     * @return The last method in the linked list.
     */
    public static Processable lastProcessable(Processable processable)
    {
        Processable lastProcessable = processable;
        while (lastProcessable.getProcessingInfo() != null &&
               lastProcessable.getProcessingInfo() instanceof Processable)
        {
            lastProcessable = (Processable)lastProcessable.getProcessingInfo();
        }

        return lastProcessable;
    }


    private static class KotlinMultiFileFacadeMethodLinker
    implements           KotlinMetadataVisitor,
                         MemberVisitor,
                         ConstantVisitor
    {
        private Member multiFileFacadeMember;

        // Implementations for KotlinMetadataVisitor.

        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinMultiFileFacadeMetadata(Clazz                             clazz,
                                                       KotlinMultiFileFacadeKindMetadata kotlinMultiFileFacadeKindMetadata)
        {
            clazz.methodsAccept(this);
        }

        // Implementations for MemberVisitor.

        @Override
        public void visitAnyMember(Clazz clazz, Member member)
        {
            multiFileFacadeMember = member;
            multiFileFacadeMember.accept(clazz,
                new AllAttributeVisitor(
                new AllInstructionVisitor(
                new InstructionOpCodeFilter(new int[] { OP_INVOKESTATIC },
                new InstructionConstantVisitor(this)))));
        }

        // Implementations for ConstantVisitor.

        @Override
        public void visitAnyConstant(Clazz clazz, Constant constant) {}

        @Override
        public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant)
        {
            if (multiFileFacadeMember.getName(clazz).equals(anyMethodrefConstant.getName(clazz))       &&
                multiFileFacadeMember.getDescriptor(clazz).equals(anyMethodrefConstant.getType(clazz)) &&
                anyMethodrefConstant.referencedMethod != null)
            {
                link(multiFileFacadeMember, anyMethodrefConstant.referencedMethod);
            }
        }
    }
}
