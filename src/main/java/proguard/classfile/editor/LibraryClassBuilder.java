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
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This editor allows to build or extend classes ({@link LibraryClass} instances).
 * It provides methods to easily add interfaces, fields, and methods,
 * optionally with method bodies.
 * <p/>
 * If you're adding many fields and methods, it is more efficient to reuse
 * a single instance of this builder for all fields and methods that you add.
 *
 * @author Johan Leys
 * @author Eric Lafortune
 * @author Joren Van Hecke
 */
public class LibraryClassBuilder
{
    private final LibraryClass libraryClass;
    private final LibraryClassEditor libraryClassEditor;



    /**
     * Creates a new ClassBuilder for the Java class with the given
     * name and super class.
     *
     * @param u2accessFlags  access flags for the new class.
     * @param className      the fully qualified name of the new class.
     * @param superclassName the fully qualified name of the super class.
     *
     * @see VersionConstants
     * @see AccessConstants
     */
    public LibraryClassBuilder(int    u2accessFlags,
                               String className,
                               String superclassName)
    {
        this(new LibraryClass(u2accessFlags,
                              className,
                              superclassName
        ));
    }


    /**
     * Creates a new ClassBuilder for the Java class with the given
     * name and super class.
     *
     * @param u2accessFlags    access flags for the new class.
     * @param className        the fully qualified name of the new class.
     * @param superclassName   the fully qualified name of the super class.
     * @param interfaceNames   the names of the interfaces that are implemented by this class.
     * @param interfaceClasses references to the interface classes of the interfaces that are implemented by this class.
     * @param subClassCount    the number of subclasses of this class.
     * @param subClasses       references to the subclasses of this class.
     * @param fields           references to the fields of this class.
     * @param methods          references to the methods of this class.
     * @param kotlinMetadata   the metadata attached to this class if it is a Kotlin class
     *
     * @see VersionConstants
     * @see AccessConstants
     */
    public LibraryClassBuilder(int             u2accessFlags,
                               String          className,
                               String          superclassName,
                               String[]        interfaceNames,
                               Clazz[]         interfaceClasses,
                               int             subClassCount,
                               Clazz[]         subClasses,
                               LibraryField[]  fields,
                               LibraryMethod[] methods,
                               KotlinMetadata  kotlinMetadata)
    {
        this(new LibraryClass(u2accessFlags,
                              className,
                              superclassName,
                              interfaceNames,
                              interfaceClasses,
                              subClassCount,
                              subClasses,
                              fields,
                              methods,
                              kotlinMetadata
                ));
    }


    /**
     * Creates a new ClassBuilder for the given class.
     *
     * @param libraryClass     the class to be edited.
     */
    public LibraryClassBuilder(LibraryClass libraryClass)

    {
        this.libraryClass = libraryClass;

        libraryClassEditor = new LibraryClassEditor(libraryClass);
    }


    /**
     * Returns the created or edited LibraryClass instance. This is a live
     * instance; any later calls to the builder will still affect the
     * instance.
     */
    public LibraryClass getLibraryClass()
    {
        return libraryClass;
    }


    /**
     * Returns a ConstantPoolEditor instance for the created or edited class
     * instance. Reusing this instance is more efficient for newly created
     * classes.
     */
    public ConstantPoolEditor getConstantPoolEditor()
    {
        throw new UnsupportedOperationException("Library class ["+libraryClass.thisClassName+"] doesn't store constant pool");
    }


    /**
     * Adds a new interface to the edited class.
     *
     * @param interfaceClass the interface class.
     * @return this instance of ClassBuilder.
     */
    public LibraryClassBuilder addInterface(Clazz interfaceClass)
    {
        return addInterface(interfaceClass.getName(), interfaceClass);
    }


    /**
     * Adds a new interface to the edited class.
     *
     * @param interfaceName the name of the interface.
     * @return this instance of ClassBuilder.
     */
    public LibraryClassBuilder addInterface(String interfaceName)
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
    public LibraryClassBuilder addInterface(String interfaceName,
                                            Clazz  referencedInterface)
    {
        // Add it to the class.
        libraryClassEditor.addInterface(interfaceName, referencedInterface);

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
    public LibraryClassBuilder addField(int    u2accessFlags,
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
    public LibraryClassBuilder addField(int           u2accessFlags,
                                        String        fieldName,
                                        String        fieldDescriptor,
                                        MemberVisitor extraMemberVisitor)
    {
        // Create the field.
        LibraryField libraryField = addAndReturnField(u2accessFlags,
                                                      fieldName,
                                                      fieldDescriptor);

        // Let the optional visitor visit the new field.
        if (extraMemberVisitor != null)
        {
            extraMemberVisitor.visitLibraryField(libraryClass, libraryField);
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
    public LibraryField addAndReturnField(int     u2accessFlags,
                                          String fieldName,
                                          String fieldDescriptor)
    {
        // Create a field.
        LibraryField libraryField =
            new LibraryField(u2accessFlags,
                             fieldName,
                             fieldDescriptor);

        // Add it to the class.
        libraryClassEditor.addField(libraryField);

        return libraryField;
    }


    /**
     * Adds a new method to the edited class.
     *
     * @param u2accessFlags      the access flags of the new method.
     * @param methodName         the name of the new method.
     * @param methodDescriptor   the descriptor of the new method.
     * @return this instance of ClassBuilder.
     */
    public LibraryClassBuilder addMethod(int    u2accessFlags,
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
    public LibraryClassBuilder addMethod(int           u2accessFlags,
                                         String        methodName,
                                         String        methodDescriptor,
                                         MemberVisitor extraMemberVisitor)
    {
        // Create the method.
        LibraryMethod libraryMethod =
            addAndReturnMethod(u2accessFlags,
                               methodName,
                               methodDescriptor);

        // Let the optional visitor visit the new method.
        if (extraMemberVisitor != null)
        {
            extraMemberVisitor.visitLibraryMethod(libraryClass, libraryMethod);
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
    public LibraryMethod addAndReturnMethod(int    u2accessFlags,
                                            String methodName,
                                            String methodDescriptor)
    {
        // Create a library method.
        LibraryMethod libraryMethod =
                new LibraryMethod(u2accessFlags,
                                  methodName,
                                  methodDescriptor);

        // Add the method to the class.
        libraryClassEditor.addMethod(libraryMethod);

        return libraryMethod;
    }
}
