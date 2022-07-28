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
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.visitor.*;
import proguard.util.*;

import java.util.*;

/**
 * This editor allows to build and/or edit classes (ProgramClass instances).
 * It provides methods to easily add fields and methods to classes.
 *
 * @author Johan Leys
 */
public class InitializerEditor
implements
             // Implementation interface.
             AttributeVisitor
{
    private static final String EXTRA_INIT_METHOD_NAME       = "init$";
    private static final String EXTRA_INIT_METHOD_DESCRIPTOR = "()V";


    private final ProgramClass programClass;


    // A field acting as a parameter for visitor methods.
    private Instruction[] insertInstructions;


    /**
     * Creates a new InitializerEditor for the given class.
     *
     * @param programClass the class to be edited.
     */
    public InitializerEditor(ProgramClass programClass)
    {
        this.programClass = programClass;
    }


    /**
     * Adds the specified static initializer instructions to the edited class.
     * If the class already contains a static initializer, the new instructions
     * will be appended to the existing initializer.
     *
     * @param mergeIntoExistingInitializer indicates whether the instructions should
     *                                     be added to the existing static initializer
     *                                     (if it exists), or if a new method should
     *                                     be created, which is then called from the
     *                                     existing initializer.
     * @param codeBuilder                  the provider of a builder to add
     *                                     instructions. This functional interface
     *                                     can conveniently be implemented as a
     *                                     closure.
     */
    public void addStaticInitializerInstructions(boolean     mergeIntoExistingInitializer,
                                                 CodeBuilder codeBuilder)
    {
        // Create an instruction sequence builder.
        InstructionSequenceBuilder builder =
            new InstructionSequenceBuilder(programClass);

        // Let the code builder add instructions to it.
        codeBuilder.build(builder);

        // Add the instructions.
        addStaticInitializerInstructions(mergeIntoExistingInitializer,
                                         builder.instructions());
    }


    /**
     * Adds the given static initializer instructions to the edited class.
     * If the class already contains a static initializer, the new instructions
     * will be appended to the existing initializer.
     *
     * @param mergeIntoExistingInitializer indicates whether the instructions should
     *                                     be added to the existing static initializer
     *                                     (if it exists), or if a new method should
     *                                     be created, which is then called from the
     *                                     existing initializer.
     * @param instructions                 the instructions to be added.
     */
    public void addStaticInitializerInstructions(boolean       mergeIntoExistingInitializer,
                                                 Instruction[] instructions)
    {
        // Is there a static initializer?
        Method method = programClass.findMethod(ClassConstants.METHOD_NAME_CLINIT,
                                                ClassConstants.METHOD_TYPE_CLINIT);
        if (method == null)
        {
            // Create a new static initializer with the instructions and
            // a return.
            new ClassBuilder(programClass).addMethod(
                AccessConstants.STATIC,
                ClassConstants.METHOD_NAME_CLINIT,
                ClassConstants.METHOD_TYPE_CLINIT,
                2 * instructions.length + 10,
                ____ -> ____
                    .appendInstructions(instructions)
                    .return_());
        }
        else if (mergeIntoExistingInitializer)
        {
            // Insert the instructions at the start of the static initializer.
            insertInstructions = instructions;
            method.accept(programClass, new AllAttributeVisitor(this));
        }
        else
        {
            // Create a new static method with the instructions and
            // a return.
            String methodName =
                uniqueMethodName(ClassConstants.METHOD_TYPE_INIT);

            new ClassBuilder(programClass).addMethod(
                AccessConstants.STATIC,
                methodName,
                ClassConstants.METHOD_TYPE_CLINIT,
                2 * instructions.length + 10,
                ____ -> ____
                    .appendInstructions(instructions)
                    .return_(),

                // Make sure that such methods are not optimized (inlined)
                // to prevent potential overflow errors during conversion.
                new ProcessingFlagSetter(ProcessingFlags.DONT_OPTIMIZE));

            // Retrieve the newly created extra initialization method
            Method extraInitMethod = programClass.findMethod(methodName, ClassConstants.METHOD_TYPE_CLINIT);

            // Call the static method from the static initializer.
            insertInstructions =
                new InstructionSequenceBuilder(programClass)
                    .invokestatic(programClass,
                                  extraInitMethod)
                    .instructions();

            method.accept(programClass, new AllAttributeVisitor(this));
        }
    }


    /**
     * Adds the specified initialization instructions to the edited class.
     *
     * - If the class doesn't contain a constructor yet, it will be created,
     *   and the instructions will be added to this constructor.
     * - If there is a single super-calling constructor, the instructions will
     *   be added at the beginning of it's code attribute.
     * - If there are multiple super-calling constructors, a new private
     *   parameterless helper method will be created, to which the instructions
     *   will be added. An invocation to this new method will be added at the
     *   beginning of the code attribute of all super-calling constructors.
     *
     * @param codeBuilder the provider of a builder to add instructions.
     *                   This functional interface can conveniently be
     *                   implemented as a closure.
     */
    public void addInitializerInstructions(CodeBuilder codeBuilder)
    {
        // Create an instruction sequence builder.
        InstructionSequenceBuilder builder =
            new InstructionSequenceBuilder(programClass);

        // Let the code builder add instructions to it.
        codeBuilder.build(builder);

        // Add the instructions.
        addInitializerInstructions(builder.instructions());
    }


    /**
     * Adds the given initialization instructions to the edited class.
     *
     * - If the class doesn't contain a constructor yet, it will be created,
     *   and the instructions will be added to this constructor.
     * - If there is a single super-calling constructor, the instructions will
     *   be added at the beginning of it's code attribute.
     * - If there are multiple super-calling constructors, a new private
     *   parameterless helper method will be created, to which the instructions
     *   will be added. An invocation to this new method will be added at the
     *   beginning of the code attribute of all super-calling constructors.
     *
     * @param instructions the instructions to be added.
     */
    public void addInitializerInstructions(Instruction[] instructions)
    {
        // Is there any initializer?
        Method method = programClass.findMethod(ClassConstants.METHOD_NAME_INIT,
                                                null);
        if (method == null)
        {
            // Create a new initializer with super(), the instructions, and
            // a return.
            new ClassBuilder(programClass).addMethod(
                AccessConstants.PUBLIC,
                ClassConstants.METHOD_NAME_INIT,
                ClassConstants.METHOD_TYPE_INIT,
                2 * instructions.length + 10,
                ____ -> ____
                    .aload_0()
                    .invokespecial(programClass.getSuperName(),
                                   ClassConstants.METHOD_NAME_INIT,
                                   ClassConstants.METHOD_TYPE_INIT)
                    .appendInstructions(instructions)
                    .return_());
        }
        else
        {
            // Find all super-calling constructors.
            Set<Method> constructors =  new HashSet<Method>();
            programClass.methodsAccept(
                new ConstructorMethodFilter(
                new MethodCollector(constructors), null, null));

            if (constructors.size() == 1)
            {
                // There is only one supper-calling constructor.
                // Add the code to this constructor.
                insertInstructions = instructions;
                constructors.iterator().next().accept(programClass,
                    new AllAttributeVisitor(
                    this));
            }
            else
            {
                // There are multiple super-calling constructors. Add the
                // instructions to a separate, parameterless initialization
                // method, and invoke this method from all super-calling
                // constructors.
                Method initMethod = programClass.findMethod(EXTRA_INIT_METHOD_NAME,
                                                            EXTRA_INIT_METHOD_DESCRIPTOR);
                if (initMethod == null)
                {
                    // Create a new private method with the instructions and
                    // a return.
                    new ClassBuilder(programClass).addMethod(
                        AccessConstants.STATIC,
                        EXTRA_INIT_METHOD_NAME,
                        EXTRA_INIT_METHOD_DESCRIPTOR,
                        2 * instructions.length + 10,
                        ____ -> ____
                            .appendInstructions(instructions)
                            .return_(),

                        // Make sure that this methods is not optimized (inlined)
                        // to prevent potential overflow errors during conversion.
                        new ProcessingFlagSetter(ProcessingFlags.DONT_OPTIMIZE));

                    // Call the private method from all super-calling constructors.
                    insertInstructions =
                        new InstructionSequenceBuilder(programClass)
                            .aload_0()
                            .invokespecial(programClass.getName(),
                                           EXTRA_INIT_METHOD_NAME,
                                           EXTRA_INIT_METHOD_DESCRIPTOR)
                            .instructions();

                    programClass.methodsAccept(
                        new ConstructorMethodFilter(
                        new AllAttributeVisitor(
                        this), null, null));
                }
                else
                {
                    // There already is an init$ method. Add the instructions to this method.
                    insertInstructions = instructions;
                    initMethod.accept(programClass,
                        new AllAttributeVisitor(
                        this));
                }
            }
        }
    }


    /**
     * Returns a unique method name for the given descriptor in the current class.
     */
    private String uniqueMethodName(String methodDescriptor)
    {
        int counter = 0;
        while (true)
        {
            String methodName = EXTRA_INIT_METHOD_NAME + counter++;
            if (programClass.findMethod(methodName, methodDescriptor) == null)
            {
                return methodName;
            }
        }
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Insert the instructions befotre the first instruction of this method.
        CodeAttributeEditor codeAttributeEditor = new CodeAttributeEditor();
        codeAttributeEditor.reset(codeAttribute.u4codeLength);
        codeAttributeEditor.insertBeforeOffset(0, insertInstructions);
        codeAttributeEditor.visitCodeAttribute(clazz, method, codeAttribute);
    }


    /**
     * This functional interface provides an instruction sequence builder to
     * its caller.
     */
    public interface CodeBuilder
    {
        public void build(InstructionSequenceBuilder code);
    }
}
