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
package proguard.classfile;

import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.util.*;

import java.util.Arrays;

/**
 * This {@link Clazz} is a compact representation of the essential data in a Java class.
 *
 * @author Eric Lafortune
 */
public class LibraryClass
extends      SimpleFeatureNamedProcessable
implements   Clazz
{
    private static final Clazz[]         EMPTY_CLASSES    = new Clazz[0];
    private static final String[]        EMPTY_INTERFACES = new String[0];
    private static final LibraryField[]  EMPTY_FIELDS     = new LibraryField[0];
    private static final LibraryMethod[] EMPTY_METHODS    = new LibraryMethod[0];

    public int             u2accessFlags;
    public String          thisClassName;
    public String          superClassName;
    public String[]        interfaceNames;
    public LibraryField[]  fields;
    public LibraryMethod[] methods;
    public KotlinMetadata  kotlinMetadata;

    /**
     * An extra field pointing to the superclass of this class.
     * This field is filled out by the {@link ClassSuperHierarchyInitializer}.
     */
    public Clazz   superClass;

    /**
     * An extra field pointing to the interfaces of this class.
     * This field is filled out by the {@link ClassSuperHierarchyInitializer}.
     */
    public Clazz[] interfaceClasses = EMPTY_CLASSES;

    /**
     * An extra field pointing to the subclasses of this class.
     * This field is filled out by the {@link ClassSubHierarchyInitializer}.
     */
    public Clazz[] subClasses = EMPTY_CLASSES;
    public int     subClassCount;

    /**
     * Creates an empty LibraryClass.
     */
    public LibraryClass()
    {
    }


    /**
     * Creates an initialized LibraryClass
     * @param u2accessFlags     access flags for the new class.
     * @param thisClassName     the fully qualified name of the new class.
     * @param superClassName    the fully qualified name of the super class.
     */
    public LibraryClass(int    u2accessFlags,
                        String thisClassName,
                        String superClassName)
    {
        this(u2accessFlags,
             thisClassName,
             superClassName,
             null);
    }


    /**
     * Creates an initialized LibraryClass
     * @param u2accessFlags     access flags for the new class.
     * @param thisClassName     the fully qualified name of the new class.
     * @param superClassName    the fully qualified name of the super class.
     * @param kotlinMetadata    the metadata attached to this class if it is a Kotlin class.
     */
    public LibraryClass(int            u2accessFlags,
                        String         thisClassName,
                        String         superClassName,
                        KotlinMetadata kotlinMetadata)
    {
        this(u2accessFlags,
             thisClassName,
             superClassName,
             EMPTY_INTERFACES,
             EMPTY_CLASSES,
             0,
             EMPTY_CLASSES,
             EMPTY_FIELDS,
             EMPTY_METHODS,
             kotlinMetadata);
    }


    /**
     * Creates an initialized LibraryClass
     * @param u2accessFlags     access flags for the new class.
     * @param thisClassName     the fully qualified name of the new class.
     * @param superClassName    the fully qualified name of the super class.
     * @param interfaceNames    the names of the interfaces that are implemented by this class.
     * @param interfaceClasses  references to the interface classes of the interfaces that are implemented by this class.
     * @param subClassCount     the number of subclasses of this class.
     * @param subClasses        references to the subclasses of this class.
     * @param fields            references to the fields of this class.
     * @param methods           references to the methods of this class.
     * @param kotlinMetadata    the metadata attached to this class if it is a Kotlin class
     */
    public LibraryClass(int             u2accessFlags,
                        String          thisClassName,
                        String          superClassName,
                        String[]        interfaceNames,
                        Clazz[]         interfaceClasses,
                        int             subClassCount,
                        Clazz[]         subClasses,
                        LibraryField[]  fields,
                        LibraryMethod[] methods,
                        KotlinMetadata  kotlinMetadata)
    {
        this.u2accessFlags    = u2accessFlags;
        this.thisClassName    = thisClassName;
        this.superClassName   = superClassName;
        this.interfaceNames   = interfaceNames;
        this.interfaceClasses = interfaceClasses;
        this.fields           = fields;
        this.methods          = methods;
        this.subClassCount    = subClassCount;
        this.subClasses       = subClasses;
        this.kotlinMetadata   = kotlinMetadata;
    }


    /**
     * Returns whether this library class is visible to the outside world.
     */
    boolean isVisible()
    {
        return (u2accessFlags & AccessConstants.PUBLIC) != 0;
    }


    // Implementations for Clazz.

    public int getAccessFlags()
    {
        return u2accessFlags;
    }

    public String getName()
    {
        return thisClassName;
    }

    public String getSuperName()
    {
        // This may be java/lang/Object, in which case there is no super.
        return superClassName;
    }

    public int getInterfaceCount()
    {
        return interfaceClasses.length;
    }

    public String getInterfaceName(int index)
    {
        return interfaceNames[index];
    }

    public int getTag(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }

    public String getString(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }

    public String getStringString(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }

    public String getClassName(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }

    public String getName(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }

    public String getType(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }


    public String getRefClassName(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }

    public String getRefName(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }

    public String getRefType(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }

    public String getModuleName(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }

    public String getPackageName(int constantIndex)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store constant pool");
    }


    public void addSubClass(Clazz clazz)
    {
        subClasses = ArrayUtil.add(subClasses, subClassCount++, clazz);
    }


    public void removeSubClass(Clazz clazz)
    {
        int newIndex = 0;
        for (int index = 0; index < subClassCount; index++)
        {
            if (!subClasses[index].equals(clazz))
            {
                subClasses[newIndex++] = subClasses[index];
            }
        }

        // Clear the remaining elements.
        Arrays.fill(subClasses, newIndex, subClassCount, null);

        subClassCount = newIndex;
    }


    public Clazz getSuperClass()
    {
        return superClass;
    }


    public Clazz getInterface(int index)
    {
        return interfaceClasses[index];
    }


    public boolean extends_(Clazz clazz)
    {
        if (this.equals(clazz))
        {
            return true;
        }

        return superClass != null &&
               superClass.extends_(clazz);
    }


    public boolean extends_(String className)
    {
        if (getName().equals(className))
        {
            return true;
        }

        return superClass != null &&
               superClass.extends_(className);
    }


    public boolean extendsOrImplements(Clazz clazz)
    {
        if (this.equals(clazz))
        {
            return true;
        }

        if (superClass != null &&
            superClass.extendsOrImplements(clazz))
        {
            return true;
        }

        if (interfaceClasses != null)
        {
            for (int index = 0; index < interfaceClasses.length; index++)
            {
                Clazz interfaceClass = interfaceClasses[index];
                if (interfaceClass != null &&
                    interfaceClass.extendsOrImplements(clazz))
                {
                    return true;
                }
            }
        }

        return false;
    }


    public boolean extendsOrImplements(String className)
    {
        if (getName().equals(className))
        {
            return true;
        }

        if (superClass != null &&
            superClass.extendsOrImplements(className))
        {
            return true;
        }

        if (interfaceClasses != null)
        {
            for (int index = 0; index < interfaceClasses.length; index++)
            {
                Clazz interfaceClass = interfaceClasses[index];
                if (interfaceClass != null &&
                    interfaceClass.extendsOrImplements(className))
                {
                    return true;
                }
            }
        }

        return false;
    }


    public Field findField(String name, String descriptor)
    {
        for (int index = 0; index < fields.length; index++)
        {
            Field field = fields[index];
            if (field != null &&
                (name       == null || field.getName(this).equals(name)) &&
                (descriptor == null || field.getDescriptor(this).equals(descriptor)))
            {
                return field;
            }
        }

        return null;
    }


    public Method findMethod(String name, String descriptor)
    {
        for (int index = 0; index < methods.length; index++)
        {
            Method method = methods[index];
            if (method != null &&
                (name       == null || method.getName(this).equals(name)) &&
                (descriptor == null || method.getDescriptor(this).equals(descriptor)))
            {
                return method;
            }
        }

        return null;
    }


    public void accept(ClassVisitor classVisitor)
    {
        classVisitor.visitLibraryClass(this);
    }


    public void hierarchyAccept(boolean      visitThisClass,
                                boolean      visitSuperClass,
                                boolean      visitInterfaces,
                                boolean      visitSubclasses,
                                ClassVisitor classVisitor)
    {
        // First visit the current class.
        if (visitThisClass)
        {
            accept(classVisitor);
        }

        // Then visit its superclass, recursively.
        if (visitSuperClass)
        {
            if (superClass != null)
            {
                superClass.hierarchyAccept(true,
                                           true,
                                           visitInterfaces,
                                           false,
                                           classVisitor);
            }
        }

        // Then visit its interfaces, recursively.
        if (visitInterfaces)
        {
            // Visit the interfaces of the superclasses, if we haven't done so yet.
            if (!visitSuperClass)
            {
                if (superClass != null)
                {
                    superClass.hierarchyAccept(false,
                                               false,
                                               true,
                                               false,
                                               classVisitor);
                }
            }

            // Visit the interfaces.
            if (interfaceClasses != null)
            {
                for (int index = 0; index < interfaceClasses.length; index++)
                {
                    Clazz interfaceClass = interfaceClasses[index];
                    if (interfaceClass != null)
                    {
                        interfaceClass.hierarchyAccept(true,
                                                       false,
                                                       true,
                                                       false,
                                                       classVisitor);
                    }
                }
            }
        }

        // Then visit its subclasses, recursively.
        if (visitSubclasses)
        {
            for (int index = 0; index < subClassCount; index++)
            {
                subClasses[index].hierarchyAccept(true,
                                                  false,
                                                  false,
                                                  true,
                                                  classVisitor);
            }
        }
    }


    /**
     * Lets the given class visitor visit the superclass, if it is known.
     * @param classVisitor the <code>ClassVisitor</code> that will visit the
     *                     superclass.
     */
    public void superClassAccept(ClassVisitor classVisitor)
    {
        if (superClass != null)
        {
            superClass.accept(classVisitor);
        }
    }


    /**
     * Lets the given class visitor visit all known direct interfaces.
     * @param classVisitor the <code>ClassVisitor</code> that will visit the
     *                     interfaces.
     */
    public void interfacesAccept(ClassVisitor classVisitor)
    {
        if (interfaceClasses != null)
        {
            for (int index = 0; index < interfaceClasses.length; index++)
            {
                Clazz interfaceClass = interfaceClasses[index];
                if (interfaceClass != null)
                {
                    interfaceClass.accept(classVisitor);
                }
            }
        }
    }


    public void subclassesAccept(ClassVisitor classVisitor)
    {
        for (int index = 0; index < subClassCount; index++)
        {
            subClasses[index].accept(classVisitor);
        }
    }


    public void constantPoolEntriesAccept(ConstantVisitor constantVisitor)
    {
        // This class doesn't keep references to its constant pool entries.
    }


    public void constantPoolEntryAccept(int index, ConstantVisitor constantVisitor)
    {
        // This class doesn't keep references to its constant pool entries.
    }


    public void thisClassConstantAccept(ConstantVisitor constantVisitor)
    {
        // This class doesn't keep references to its constant pool entries.
    }


    public void superClassConstantAccept(ConstantVisitor constantVisitor)
    {
        // This class doesn't keep references to its constant pool entries.
    }


    public void interfaceConstantsAccept(ConstantVisitor constantVisitor)
    {
        // This class doesn't keep references to its constant pool entries.
    }


    public void fieldsAccept(MemberVisitor memberVisitor)
    {
        for (int index = 0; index < fields.length; index++)
        {
            Field field = fields[index];
            if (field != null)
            {
                field.accept(this, memberVisitor);
            }
        }
    }


    public void fieldAccept(String name, String descriptor, MemberVisitor memberVisitor)
    {
        Field field = findField(name, descriptor);
        if (field != null)
        {
            field.accept(this, memberVisitor);
        }
    }


    public void methodsAccept(MemberVisitor memberVisitor)
    {
        for (int index = 0; index < methods.length; index++)
        {
            Method method = methods[index];
            if (method != null)
            {
                method.accept(this, memberVisitor);
            }
        }
    }


    public void methodAccept(String name, String descriptor, MemberVisitor memberVisitor)
    {
        Method method = findMethod(name, descriptor);
        if (method != null)
        {
            method.accept(this, memberVisitor);
        }
    }


    public boolean mayHaveImplementations(Method method)
    {
        return
           (u2accessFlags & AccessConstants.FINAL) == 0 &&
           (method == null ||
            ((method.getAccessFlags() & (AccessConstants.PRIVATE |
                                         AccessConstants.STATIC  |
                                         AccessConstants.FINAL)) == 0 &&
             !method.getName(this).equals(ClassConstants.METHOD_NAME_INIT)));
    }


    public void attributesAccept(AttributeVisitor attributeVisitor)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store attributes");
    }


    public void attributeAccept(String name, AttributeVisitor attributeVisitor)
    {
        throw new UnsupportedOperationException("Library class ["+thisClassName+"] doesn't store attributes");
    }


    public void kotlinMetadataAccept(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        if (kotlinMetadata != null)
        {
            kotlinMetadata.accept(this, kotlinMetadataVisitor);
        }
    }


    // Implementations for Object.

    public String toString()
    {
        return "LibraryClass("+getName()+")";
    }
}
