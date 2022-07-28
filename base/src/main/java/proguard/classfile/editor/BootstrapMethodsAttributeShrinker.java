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
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.visitor.*;
import proguard.util.Processable;

import java.util.Arrays;

/**
 * This {@link ClassVisitor} removes all unused entries from the bootstrap method attribute.
 * <p/>
 * If all bootstrap methods are removed, it also removes the {@link BootstrapMethodsAttribute} from
 * the visited class. Additionally, the <code>java/lang/MethodHandles$Lookup</code> class will be
 * removed from the {@link InnerClassesAttribute}, and the {@link InnerClassesAttribute} will be removed if
 * it was the only entry.
 *
 * @author Tim Van Den Broecke
 */
public class BootstrapMethodsAttributeShrinker
implements   ClassVisitor,

             // Implementation interfaces.
             MemberVisitor,
             AttributeVisitor,
             InstructionVisitor,
             BootstrapMethodInfoVisitor
{
    // A processing info flag to indicate the bootstrap method is being used.
    private static final Object USED = new Object();

    private       int[]                   bootstrapMethodIndexMap = new int[ClassEstimates.TYPICAL_BOOTSTRAP_METHODS_ATTRIBUTE_SIZE];
    private final BootstrapMethodRemapper bootstrapMethodRemapper = new BootstrapMethodRemapper(true);

    private int     referencedBootstrapMethodIndex = -1;
    private boolean modified                       = false;


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        // Clear the fields from any previous runs.
        modified                = false;
        bootstrapMethodIndexMap = new int[ClassEstimates.TYPICAL_BOOTSTRAP_METHODS_ATTRIBUTE_SIZE];

        // Remove any previous processing info.
        programClass.accept(new ClassCleaner());

        // Mark the bootstrap methods referenced by invokeDynamic instructions.
        programClass.methodsAccept(this);

        // Shrink the bootstrap methods attribute
        programClass.attributesAccept(this);

        if (modified)
        {
            // Clean up dangling and freed up constants
            programClass.accept(new ConstantPoolShrinker());
        }
    }


    // Implementations for MemberVisitor.

    @Override
    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programMethod.attributesAccept(programClass, this);
    }


    // Implementations for AttributeVisitor.

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        codeAttribute.instructionsAccept(clazz, method, this);
    }


    @Override
    public void visitBootstrapMethodsAttribute(Clazz clazz, BootstrapMethodsAttribute bootstrapMethodsAttribute)
    {
        if (referencedBootstrapMethodIndex > -1)
        {
            // We're marking bootstrap methods
            bootstrapMethodsAttribute.bootstrapMethodEntryAccept(clazz, referencedBootstrapMethodIndex, this);
        }
        else
        {
            // The bootstrap methods have been marked, so now we shrink the array of BootstrapMethodInfo objects.
            int newBootstrapMethodsCount =
                shrinkBootstrapMethodArray(bootstrapMethodsAttribute.bootstrapMethods,
                                           bootstrapMethodsAttribute.u2bootstrapMethodsCount);

            if (newBootstrapMethodsCount < bootstrapMethodsAttribute.u2bootstrapMethodsCount)
            {
                modified = true;

                bootstrapMethodsAttribute.u2bootstrapMethodsCount = newBootstrapMethodsCount;

                if (bootstrapMethodsAttribute.u2bootstrapMethodsCount == 0)
                {
                    // Remove the entire attribute.
                    AttributesEditor attributesEditor = new AttributesEditor((ProgramClass)clazz, false);
                    attributesEditor.deleteAttribute(Attribute.BOOTSTRAP_METHODS);

                    // Only bootstrap methods require the java/lang/MethodHandles$Lookup
                    // inner class, so we can remove it.
                    clazz.attributesAccept(new MethodHandlesLookupInnerClassRemover(attributesEditor));
                }
                else
                {
                    // Remap all constant pool references to remaining bootstrap methods.
                    bootstrapMethodRemapper.setBootstrapMethodIndexMap(bootstrapMethodIndexMap);
                    clazz.constantPoolEntriesAccept(bootstrapMethodRemapper);
                }
            }
        }
    }


    // Implementations for InstructionVisitor.

    @Override
    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}


    @Override
    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        if (constantInstruction.opcode == Instruction.OP_INVOKEDYNAMIC)
        {
            ProgramClass programClass = (ProgramClass)clazz;

            InvokeDynamicConstant invokeDynamicConstant =
                (InvokeDynamicConstant)programClass.getConstant(constantInstruction.constantIndex);

            referencedBootstrapMethodIndex = invokeDynamicConstant.getBootstrapMethodAttributeIndex();

            programClass.attributesAccept(this);

            referencedBootstrapMethodIndex = -1;
        }
    }


    // Implementations for BootstrapMethodInfoVisitor.

    @Override
    public void visitBootstrapMethodInfo(Clazz clazz, BootstrapMethodInfo bootstrapMethodInfo)
    {
        markAsUsed(bootstrapMethodInfo);
    }


    // Small utility methods.

    /**
     * Marks the given processable as being used.
     */
    private void markAsUsed(BootstrapMethodInfo bootstrapMethodInfo)
    {
        bootstrapMethodInfo.setProcessingInfo(USED);
    }


    /**
     * Returns whether the given processable has been marked as being used.
     */
    private boolean isUsed(Processable processable)
    {
        return processable.getProcessingInfo() == USED;
    }


    /**
     * Removes all entries that are not marked as being used from the given
     * array of bootstrap methods. Creates a map from the old indices to the
     * new indices as a side effect.
     * @return the new number of entries.
     */
    private int shrinkBootstrapMethodArray(BootstrapMethodInfo[] bootstrapMethods, int length)
    {
        if (bootstrapMethodIndexMap.length < length)
        {
            bootstrapMethodIndexMap = new int[length];
        }

        int counter = 0;

        // Shift the used bootstrap methods together.
        for (int index = 0; index < length; index++)
        {
            BootstrapMethodInfo bootstrapMethod = bootstrapMethods[index];

            // Is the entry being used?
            if (isUsed(bootstrapMethod))
            {
                // Remember the new index.
                bootstrapMethodIndexMap[index] = counter;

                // Shift the entry.
                bootstrapMethods[counter++] = bootstrapMethod;
            }
            else
            {
                // Remember an invalid index.
                bootstrapMethodIndexMap[index] = -1;
            }
        }

        // Clear the remaining bootstrap methods.
        Arrays.fill(bootstrapMethods, counter, length, null);

        return counter;
    }



    private class MethodHandlesLookupInnerClassRemover
    implements AttributeVisitor,

               // Implementation interfaces.
               InnerClassesInfoVisitor
    {
        private static final String METHOD_HANDLES_CLASS = "java/lang/invoke/MethodHandles";

        private final Object methodHandleLookupMarker = new Object();

        private final AttributesEditor attributesEditor;

        public MethodHandlesLookupInnerClassRemover(AttributesEditor attributesEditor)
        {
            this.attributesEditor = attributesEditor;
        }

        // Implementations for AttributeVisitor

        public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

        public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
        {
            // Mark inner class infos that refer to Lookup.
            innerClassesAttribute.innerClassEntriesAccept(clazz, this);

            // Remove all marked inner classes.
            InnerClassesAttributeEditor editor =
                new InnerClassesAttributeEditor(innerClassesAttribute);
            for (int index = innerClassesAttribute.u2classesCount - 1; index >= 0; index--)
            {
                InnerClassesInfo innerClassesInfo = innerClassesAttribute.classes[index];
                if (shouldBeRemoved(innerClassesInfo))
                {
                    editor.removeInnerClassesInfo(innerClassesInfo);
                }
            }

            // Remove the attribute if it is empty.
            if (innerClassesAttribute.u2classesCount == 0)
            {
                attributesEditor.deleteAttribute(Attribute.INNER_CLASSES);
            }
        }


        // Implementations for InnerClassesInfoVisitor.

        public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo)
        {
            ProgramClass programClass = (ProgramClass) clazz;

            ClassConstant innerClass =
                (ClassConstant) programClass.getConstant(innerClassesInfo.u2innerClassIndex);
            ClassConstant outerClass =
                (ClassConstant) programClass.getConstant(innerClassesInfo.u2outerClassIndex);

            if (isMethodHandleClass(innerClass, clazz) ||
                isMethodHandleClass(outerClass, clazz))
            {
                markForRemoval(innerClassesInfo);
            }
        }


        // Small utility methods.

        private void markForRemoval(InnerClassesInfo innerClassesInfo)
        {
            innerClassesInfo.setProcessingInfo(methodHandleLookupMarker);
        }

        private boolean shouldBeRemoved(InnerClassesInfo innerClassesInfo)
        {
            return innerClassesInfo.getProcessingInfo() == methodHandleLookupMarker;
        }

        public boolean isMethodHandleClass(ClassConstant classConstant, Clazz clazz)
        {
            return classConstant != null &&
                   classConstant.getName(clazz).startsWith(METHOD_HANDLES_CLASS);
        }
    }
}
