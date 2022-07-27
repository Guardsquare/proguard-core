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
import proguard.classfile.constant.Constant;
import proguard.classfile.io.ProgramClassWriter;
import proguard.classfile.visitor.MemberVisitor;

import java.io.*;

/**
 * This editor allows to build or extend classes (ProgramClass instances).
 * It provides methods to easily add interfaces, fields, and methods,
 * optionally with method bodies.
 * <p/>
 * If you're adding many fields and methods, it is more efficient to reuse
 * a single instance of this builder for all fields and methods that you add.
 *
 * @author Johan Leys
 * @author Eric Lafortune
 */
public class ClassBuilder
{
    private final ProgramClass                 programClass;
    private final ClassEditor                  classEditor;
    private final ConstantPoolEditor           constantPoolEditor;
    private final CompactCodeAttributeComposer compactCodeAttributeComposer;

    // A flag to make sure we don't use our shared instance of the compact code
    // attribute composer for different code at the same time.
    private boolean compactCodeAttributeComposerInUse;


    /**
     * Creates a new ClassBuilder for the Java class with the given
     * name and super class.
     *
     * @param u4version      the class version.
     * @param u2accessFlags  access flags for the new class.
     * @param className      the fully qualified name of the new class.
     * @param superclassName the fully qualified name of the super class.
     *
     * @see VersionConstants
     * @see AccessConstants
     */
    public ClassBuilder(int    u4version,
                        int    u2accessFlags,
                        String className,
                        String superclassName)
    {
        this(u4version,
             u2accessFlags,
             className,
             superclassName,
             null,
             0,
             null);
    }


    /**
     * Creates a new ClassBuilder for the Java class with the given
     * name and super class.
     *
     * @param u4version       the class version.
     * @param u2accessFlags   access flags for the new class.
     * @param className       the fully qualified name of the new class.
     * @param superclassName  the fully qualified name of the super class.
     * @param featureName     an optional feature name for the new class.
     * @param processingFlags optional processing flags for the new class.
     * @param processingInfo  optional processing info for the new class.
     *
     * @see VersionConstants
     * @see AccessConstants
     */
    public ClassBuilder(int    u4version,
                        int    u2accessFlags,
                        String className,
                        String superclassName,
                        String featureName,
                        int    processingFlags,
                        Object processingInfo)
    {
        this(new ProgramClass(u4version,
                              1,
                              new Constant[ClassEstimates.TYPICAL_CONSTANT_POOL_SIZE],
                              u2accessFlags,
                              0,
                              0,
                              featureName,
                              processingFlags,
                              processingInfo));

        programClass.u2thisClass =
            constantPoolEditor.addClassConstant(className, programClass);

        if (superclassName != null)
        {
            programClass.u2superClass =
                constantPoolEditor.addClassConstant(superclassName, null);
        }
    }


    /**
     * Creates a new ClassBuilder for the given class.
     *
     * @param programClass the class to be edited.
     */
    public ClassBuilder(ProgramClass programClass)
    {
        this(programClass, null, null);
    }


    /**
     * Creates a new ClassBuilder for the given class, that automatically
     * initializes class references and class member references in new
     * constants.
     *
     * @param programClass     the class to be edited.
     * @param programClassPool the program class pool from which new constants
     *                         can be initialized.
     * @param libraryClassPool the library class pool from which new constants
     *                         can be initialized.
     */
    public ClassBuilder(ProgramClass programClass,
                        ClassPool    programClassPool,
                        ClassPool    libraryClassPool)

    {
        this.programClass = programClass;

        classEditor                  = new ClassEditor(programClass);
        constantPoolEditor           = new ConstantPoolEditor(programClass,
                                                              programClassPool,
                                                              libraryClassPool);
        compactCodeAttributeComposer = new CompactCodeAttributeComposer(constantPoolEditor,
                                       new CodeAttributeComposer(false, true, true));
    }


    /**
     * Returns the created or edited ProgramClass instance. This is a live
     * instance; any later calls to the builder will still affect the
     * instance.
     */
    public ProgramClass getProgramClass()
    {
        return programClass;
    }


    /**
     * Returns a ConstantPoolEditor instance for the created or edited class
     * instance. Reusing this instance is more efficient for newly created
     * classes.
     */
    public ConstantPoolEditor getConstantPoolEditor()
    {
        return constantPoolEditor;
    }


    /**
     * Adds a new interface to the edited class.
     *
     * @param interfaceClass the interface class.
     * @return this instance of ClassBuilder.
     */
    public ClassBuilder addInterface(Clazz interfaceClass)
    {
        return addInterface(interfaceClass.getName(), interfaceClass);
    }


    /**
     * Adds a new interface to the edited class.
     *
     * @param interfaceName the name of the interface.
     * @return this instance of ClassBuilder.
     */
    public ClassBuilder addInterface(String interfaceName)
    {
        return addInterface(interfaceName, null);
    }


    /**
     * Adds a new interface to the edited class.
     *
     * @param interfaceName       the name of the interface.
     * @param referencedInterface the referenced interface.
     * @return this instance of ClassBuilder.
     */
    public ClassBuilder addInterface(String interfaceName,
                                     Clazz  referencedInterface)
    {
        // Add it to the class.
        classEditor.addInterface(constantPoolEditor.addClassConstant(interfaceName,
                                                                     referencedInterface));

        return this;
    }


    /**
     * Adds a new field to the edited class.
     *
     * @param u2accessFlags    access flags for the new field.
     * @param fieldName        name of the new field.
     * @param fieldDescriptor  descriptor of the new field.
     * @return this instance of ClassBuilder.
     */
    public ClassBuilder addField(int    u2accessFlags,
                                 String fieldName,
                                 String fieldDescriptor)
    {
        return addField(u2accessFlags,
                        fieldName,
                        fieldDescriptor,
                        null);
    }


    /**
     * Adds a new field to the edited class.
     *
     * @param u2accessFlags    access flags for the new field.
     * @param fieldName        name of the new field.
     * @param fieldDescriptor  descriptor of the new field.
     * @return this instance of ClassBuilder.
     */
    public ClassBuilder addField(int           u2accessFlags,
                                 String        fieldName,
                                 String        fieldDescriptor,
                                 MemberVisitor extraMemberVisitor)
    {
        // Create the field.
        ProgramField programField = addAndReturnField(u2accessFlags,
                                                      fieldName,
                                                      fieldDescriptor);

        // Let the optional visitor visit the new field.
        if (extraMemberVisitor != null)
        {
            extraMemberVisitor.visitProgramField(programClass, programField);
        }

        return this;
    }


    /**
     * Adds a new field to the edited class, and returns it.
     *
     * @param u2accessFlags    access flags for the new field.
     * @param fieldName        name of the new field.
     * @param fieldDescriptor  descriptor of the new field.
     * @return the newly created field.
     */
    public ProgramField addAndReturnField(int     u2accessFlags,
                                           String fieldName,
                                           String fieldDescriptor)
    {
        // Create a simple field.
        ProgramField programField =
            new ProgramField(u2accessFlags,
                             constantPoolEditor.addUtf8Constant(fieldName),
                             constantPoolEditor.addUtf8Constant(fieldDescriptor),
                             null);

        // Add it to the class.
        classEditor.addField(programField);

        return programField;
    }


    /**
     * Adds a new method to the edited class.
     *
     * @param u2accessFlags      the access flags of the new method.
     * @param methodName         the name of the new method.
     * @param methodDescriptor   the descriptor of the new method.
     * @return this instance of ClassBuilder.
     */
    public ClassBuilder addMethod(int    u2accessFlags,
                                  String methodName,
                                  String methodDescriptor)
    {
        return addMethod(u2accessFlags,
                         methodName,
                         methodDescriptor,
                         null);
    }


    /**
     * Adds a new method to the edited class.
     *
     * @param u2accessFlags      the access flags of the new method.
     * @param methodName         the name of the new method.
     * @param methodDescriptor   the descriptor of the new method.
     * @param extraMemberVisitor an optional visitor for the method after
     *                           it has been created and added to the class.
     * @return this instance of ClassBuilder.
     */
    public ClassBuilder addMethod(int           u2accessFlags,
                                  String        methodName,
                                  String        methodDescriptor,
                                  MemberVisitor extraMemberVisitor)
    {
        // Create the method.
        ProgramMethod programMethod =
            addAndReturnMethod(u2accessFlags,
                               methodName,
                               methodDescriptor);

        // Let the optional visitor visit the new method.
        if (extraMemberVisitor != null)
        {
            extraMemberVisitor.visitProgramMethod(programClass, programMethod);
        }

        return this;
    }


    /**
     * Adds a new method to the edited class, and returns it.
     *
     * @param u2accessFlags      the access flags of the new method.
     * @param methodName         the name of the new method.
     * @param methodDescriptor   the descriptor of the new method.
     * @return the newly created method.
     */
    public ProgramMethod addAndReturnMethod(int    u2accessFlags,
                                            String methodName,
                                            String methodDescriptor)
    {
        return addAndReturnMethod(u2accessFlags,
                                  methodName,
                                  methodDescriptor,
                                  0,
                                  null);
    }


    /**
     * Adds a new method with a code attribute to the edited class.
     *
     * @param u2accessFlags         the access flags of the new method.
     * @param methodName            the name of the new method.
     * @param methodDescriptor      the descriptor of the new method.
     * @param maxCodeFragmentLength the maximum length for the code fragment.
     * @param codeBuilder           the provider of a composer to create code
     *                              attributes.
     * @return this instance of ClassBuilder.
     */
    public ClassBuilder addMethod(int         u2accessFlags,
                                  String      methodName,
                                  String      methodDescriptor,
                                  int         maxCodeFragmentLength,
                                  CodeBuilder codeBuilder)
    {
        return addMethod(u2accessFlags,
                         methodName,
                         methodDescriptor,
                         maxCodeFragmentLength,
                         codeBuilder,
                         null);
    }


    /**
     * Adds a new method with a code attribute to the edited class.
     *
     * @param u2accessFlags         the access flags of the new method.
     * @param methodName            the name of the new method.
     * @param methodDescriptor      the descriptor of the new method.
     * @param maxCodeFragmentLength the maximum length for the code fragment.
     * @param codeBuilder           the provider of a composer to create code
     *                              attributes.
     * @param extraMemberVisitor    an optional visitor for the method after
     *                              it has been created and added to the class.
     * @return this instance of ClassBuilder.
     */
    public ClassBuilder addMethod(int           u2accessFlags,
                                  String        methodName,
                                  String        methodDescriptor,
                                  int           maxCodeFragmentLength,
                                  CodeBuilder   codeBuilder,
                                  MemberVisitor extraMemberVisitor)
    {
        // Create the method.
        ProgramMethod programMethod =
            addAndReturnMethod(u2accessFlags,
                               methodName,
                               methodDescriptor,
                               maxCodeFragmentLength,
                               codeBuilder);

        // Let the optional visitor visit the new method.
        if (extraMemberVisitor != null)
        {
            extraMemberVisitor.visitProgramMethod(programClass, programMethod);
        }

        return this;
    }


    /**
     * Adds a new method with a code attribute to the edited class, and returns
     * it.
     *
     * @param u2accessFlags         the access flags of the new method.
     * @param methodName            the name of the new method.
     * @param methodDescriptor      the descriptor of the new method.
     * @param maxCodeFragmentLength the maximum length for the code fragment.
     * @param codeBuilder           the provider of a composer to create code
     *                              attributes.
     * @return the newly created method.
     */
    public ProgramMethod addAndReturnMethod(int           u2accessFlags,
                                            String        methodName,
                                            String        methodDescriptor,
                                            int           maxCodeFragmentLength,
                                            CodeBuilder   codeBuilder)
    {
        // Create an empty method.
        ProgramMethod programMethod =
            new ProgramMethod(u2accessFlags,
                              constantPoolEditor.addUtf8Constant(methodName),
                              constantPoolEditor.addUtf8Constant(methodDescriptor),
                              null);

        if (codeBuilder != null)
        {
            // Is our shared composer in use? This would have to be in a
            // recursive call, inside the compose method below, so it won't
            // be common.
            CompactCodeAttributeComposer compactCodeAttributeComposer;
            if (compactCodeAttributeComposerInUse)
            {
                // Create a new composer that we'll just use temporarily.
                compactCodeAttributeComposer =
                    new CompactCodeAttributeComposer(constantPoolEditor,
                    new CodeAttributeComposer(false, true, true));
            }
            else
            {
                // Reuse our shared composer, for efficiency.
                compactCodeAttributeComposer = this.compactCodeAttributeComposer;
                compactCodeAttributeComposer.reset();

                // Remember that we're using it.
                compactCodeAttributeComposerInUse = true;
            }

            // Start composing the contents.
            compactCodeAttributeComposer.beginCodeFragment(maxCodeFragmentLength);

            // Let the caller add its instructions, exceptions, etc.
            codeBuilder.compose(compactCodeAttributeComposer);

            // End the composer.
            compactCodeAttributeComposer.endCodeFragment();

            // Copy the accumulated code into the attribute.
            compactCodeAttributeComposer.addCodeAttribute(programClass, programMethod);

            // Release our shared composer for the next caller.
            if (compactCodeAttributeComposer == this.compactCodeAttributeComposer)
            {
                compactCodeAttributeComposerInUse = false;
            }
        }

        // Add the method to the class.
        classEditor.addMethod(programMethod);

        return programMethod;
    }


    /**
     * This functional interface provides a code attribute composer to
     * its implementation.
     */
    public interface CodeBuilder
    {
        public void compose(CompactCodeAttributeComposer code);
    }


    /**
     * Small sample application that illustrates the use of this class.
     */
    public static void main(String[] args)
    {
        // Create a class with a simple main method.
        ProgramClass programClass =
            new ClassBuilder(
                VersionConstants.CLASS_VERSION_1_8,
                AccessConstants.PUBLIC,
                "com/example/Test",
                ClassConstants.NAME_JAVA_LANG_OBJECT)

                .addMethod(
                    AccessConstants.PUBLIC |
                    AccessConstants.STATIC,
                    "main",
                    "([Ljava/lang/String;)V",
                    50,

                    // Compose the equivalent of this java code:
                    //     System.out.println("Hello, world!");
                    code -> code
                        .getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                        .ldc("Hello, world!")
                        .invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                        .return_())

                .getProgramClass();

        // Print out the class.
        //programClass.accept(new ClassPrinter());

        // Write out the class.
        try
        {
            DataOutputStream dataOutputStream =
                new DataOutputStream(
                new FileOutputStream("Test.class"));

            try
            {
                programClass.accept(
                    new ProgramClassWriter(dataOutputStream));
            }
            finally
            {
                dataOutputStream.close();
            }
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
        }
    }
}
