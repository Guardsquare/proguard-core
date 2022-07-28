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
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.*;
import proguard.resources.file.ResourceFile;
import proguard.util.ArrayUtil;

import java.util.HashMap;

/**
 * This class can add constant pool entries to a given class.
 * <p/>
 * If you're building a class from scratch, it is more efficient to reuse
 * a single instance of this editor for all constants that you add.
 *
 * @author Eric Lafortune
 */
public class ConstantPoolEditor
{
    private static final boolean DEBUG = false;

    private static final int SIZE_INCREMENT = 16;


    private final ProgramClass              targetClass;
    private final ConstantVisitor           constantReferenceInitializer;
    private       HashMap<Constant,Integer> cachedIndices;
    private       int                       cachedCount;


    /**
     * Creates a new ConstantPoolEditor.
     * @param targetClass the target class in which constants are to be edited.
     */
    public ConstantPoolEditor(ProgramClass targetClass)
    {
        this(targetClass, null, null);
    }


    /**
     * Creates a new ConstantPoolEditor that automatically initializes class
     * references and class member references in new constants.
     * @param targetClass      the target class in which constants are to be
     *                         edited.
     * @param programClassPool the program class pool from which new constants
     *                         can be initialized.
     * @param libraryClassPool the library class pool from which new constants
     *                         can be initialized.
     */
    public ConstantPoolEditor(ProgramClass targetClass,
                              ClassPool    programClassPool,
                              ClassPool    libraryClassPool)
    {
        // Automatically start caching indices of constants if we're creating
        // a class from scratch.
        this(targetClass,
             programClassPool,
             libraryClassPool,
             targetClass.u2constantPoolCount <= 1);
    }


    /**
     * Creates a new ConstantPoolEditor that automatically initializes class
     * references and class member references in new constants.
     * @param targetClass      the target class in which constants are to be
     *                         edited.
     * @param programClassPool the program class pool from which new constants
     *                         can be initialized.
     * @param libraryClassPool the library class pool from which new constants
     *                         can be initialized.
     * @param cacheIndices     specifies whether indices of constants should
     *                         be cached.
     */
    ConstantPoolEditor(ProgramClass targetClass,
                       ClassPool    programClassPool,
                       ClassPool    libraryClassPool,
                       boolean      cacheIndices)
    {
        this.targetClass = targetClass;

        constantReferenceInitializer = programClassPool == null ? null :
            new WildcardConstantFilter(
            new ClassReferenceInitializer(programClassPool, libraryClassPool));

        // Should we maintain a cache, for efficiency, if this editor will be
        // used to add many constants?
        if (cacheIndices)
        {
            if (DEBUG)
            {
                System.out.println("ConstantPoolEditor: starting with cache");
            }

            cachedIndices = new HashMap<>();
            cachedCount   = targetClass.u2constantPoolCount;

            // Initialize the cache, should the constant pool already contain
            // any elements (always starting at index 1).
            for (int index = 1; index < cachedCount; index++)
            {
                Constant constant = targetClass.constantPool[index];
                if (constant != null)
                {
                    cachedIndices.put(constant, Integer.valueOf(index));
                }
            }
        }
    }


    /**
     * Returns the target class in which constants are edited.
     */
    public ProgramClass getTargetClass()
    {
        return targetClass;
    }


    /**
     * Finds or creates a IntegerConstant constant pool entry with the given
     * value.
     * @return the constant pool index of the Utf8Constant.
     */
    public int addIntegerConstant(int value)
    {
        return findOrAddConstant(new IntegerConstant(value));
    }


    /**
     * Finds or creates a LongConstant constant pool entry with the given value.
     * @return the constant pool index of the LongConstant.
     */
    public int addLongConstant(long value)
    {
        return findOrAddConstant(new LongConstant(value));
    }


    /**
     * Finds or creates a FloatConstant constant pool entry with the given
     * value.
     * @return the constant pool index of the FloatConstant.
     */
    public int addFloatConstant(float value)
    {
        return findOrAddConstant(new FloatConstant(value));
    }


    /**
     * Finds or creates a DoubleConstant constant pool entry with the given
     * value.
     * @return the constant pool index of the DoubleConstant.
     */
    public int addDoubleConstant(double value)
    {
        return findOrAddConstant(new DoubleConstant(value));
    }


    /**
     * Finds or creates a PrimitiveArrayConstant constant pool entry with the
     * given values.
     * @return the constant pool index of the PrimitiveArrayConstant.
     */
    public int addPrimitiveArrayConstant(Object values)
    {
        return findOrAddConstant(new PrimitiveArrayConstant(values));
    }


    /**
     * Finds or creates a StringConstant constant pool entry with the given
     * value, with optional referenced class/member.
     *
     * @param referencedClass  The class that this string references.
     * @param referencedMember The member that this string references.
     * @return the constant pool index of the StringConstant.
     */
    public int addStringConstant(String string,
                                 Clazz  referencedClass,
                                 Member referencedMember)
    {
        return addStringConstant(string, referencedClass, referencedMember, 0, null);
    }

    /**
     * Finds or creates a StringConstant constant pool entry with the given
     * value.
     * @return the constant pool index of the StringConstant.
     */
    public int addStringConstant(String string)
    {
        return addStringConstant(string, null, null);
    }

    /**
     * Finds or creates a StringConstant constant pool entry with the given
     * value.
     * @return the constant pool index of the StringConstant.
     */
    public int addStringConstant(String       string,
                                 ResourceFile referencedResourceFile)
    {
        return addStringConstant(string, null, null, 0, referencedResourceFile);
    }


    /**
     * Finds or creates a StringConstant constant pool entry with the given
     * value.
     * @return the constant pool index of the StringConstant.
     */
    public int addStringConstant(String       string,
                                 Clazz        referencedClass,
                                 Member       referencedMember,
                                 int          resourceFileId,
                                 ResourceFile resourceFile)
    {
        return  addStringConstant(addUtf8Constant(string),
                                  referencedClass,
                                  referencedMember,
                                  resourceFileId,
                                  resourceFile);
    }


    /**
     * Finds or creates a StringConstant constant pool entry with the given
     * UTF-8 constant index.
     * @return the constant pool index of the StringConstant.
     */
    public int addStringConstant(int          utf8index,
                                 Clazz        referencedClass,
                                 Member       referencedMember,
                                 int          resourceFileId,
                                 ResourceFile resourceFile)
    {
        return findOrAddConstant(new StringConstant(utf8index,
                                              referencedClass,
                                              referencedMember,
                                              resourceFileId,
                                              resourceFile));
    }


    /**
     * Finds or creates a InvokeDynamicConstant constant pool entry with the
     * given bootstrap method constant pool entry index, method name, and
     * descriptor.
     * @return the constant pool index of the InvokeDynamicConstant.
     */
    public int addInvokeDynamicConstant(int     bootstrapMethodIndex,
                                        String  name,
                                        String  descriptor,
                                        Clazz[] referencedClasses)
    {
        return addInvokeDynamicConstant(bootstrapMethodIndex,
                                        addNameAndTypeConstant(name, descriptor),
                                        referencedClasses);
    }


    /**
     * Finds or creates a DynamicConstant constant pool entry with the given
     * class constant pool entry index and name and type constant pool entry
     * index.
     * @return the constant pool index of the DynamicConstant.
     */
    public int addDynamicConstant(int     bootstrapMethodIndex,
                                  int     nameAndTypeIndex,
                                  Clazz[] referencedClasses)
    {
        return findOrAddConstant(new DynamicConstant(bootstrapMethodIndex,
                                               nameAndTypeIndex,
                                               referencedClasses));
    }


    /**
     * Finds or creates an InvokeDynamicConstant constant pool entry with the
     * given class constant pool entry index and name and type constant pool
     * entry index.
     * @return the constant pool index of the InvokeDynamicConstant.
     */
    public int addInvokeDynamicConstant(int     bootstrapMethodIndex,
                                        int     nameAndTypeIndex,
                                        Clazz[] referencedClasses)
    {
        return findOrAddConstant(new InvokeDynamicConstant(bootstrapMethodIndex,
                                                     nameAndTypeIndex,
                                                     referencedClasses));
    }


    /**
     * Finds or creates a MethodHandleConstant constant pool entry of the
     * specified kind and with the given field ref, interface method ref,
     * or method ref constant pool entry index.
     * @return the constant pool index of the MethodHandleConstant.
     */
    public int addMethodHandleConstant(int referenceKind,
                                       int referenceIndex)
    {
        return findOrAddConstant(new MethodHandleConstant(referenceKind,
                                                    referenceIndex));
    }


    /**
     * Finds or creates a ModuleConstant constant pool entry with the given name.
     * @return the constant pool index of the ModuleConstant.
     */
    public int addModuleConstant(String name)
    {
        return  addModuleConstant(addUtf8Constant(name));
    }


    /**
     * Finds or creates a ModuleConstant constant pool entry with the given name
     * constant pool index.
     * @return the constant pool index of the ModuleConstant.
     */
    public int addModuleConstant(int nameIndex)
    {
        return findOrAddConstant(new ModuleConstant(nameIndex));
    }


    /**
     * Finds or creates a PackageConstant constant pool entry with the given name.
     * @return the constant pool index of the PackageConstant.
     */
    public int addPackageConstant(String name)
    {
        return addPackageConstant(addUtf8Constant(name));
    }


    /**
     * Finds or creates a PackageConstant constant pool entry with the given name
     * constant pool index.
     * @return the constant pool index of the PackageConstant.
     */
    public int addPackageConstant(int nameIndex)
    {
        return findOrAddConstant(new PackageConstant(nameIndex));
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry for the given
     * class and field.
     * @return the constant pool index of the FieldrefConstant.
     */
    public int addFieldrefConstant(Clazz referencedClass,
                                   Field referencedField)
    {
        return addFieldrefConstant(referencedClass.getName(),
                                   referencedField.getName(referencedClass),
                                   referencedField.getDescriptor(referencedClass),
                                   referencedClass,
                                   referencedField);
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry with the given
     * class name, field name, and descriptor.
     * @return the constant pool index of the FieldrefConstant.
     */
    public int addFieldrefConstant(String className,
                                   String name,
                                   String descriptor,
                                   Clazz  referencedClass,
                                   Field  referencedField)
    {
        return addFieldrefConstant(className,
                                   addNameAndTypeConstant(name, descriptor),
                                   referencedClass,
                                   referencedField);
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry with the given
     * class name, field name, and descriptor.
     * @return the constant pool index of the FieldrefConstant.
     */
    public int addFieldrefConstant(String className,
                                   int    nameAndTypeIndex,
                                   Clazz  referencedClass,
                                   Field  referencedField)
    {
        return addFieldrefConstant(addClassConstant(className, referencedClass),
                                   nameAndTypeIndex,
                                   referencedClass,
                                   referencedField);
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry with the given
     * class constant pool entry index, field name, and descriptor.
     * @return the constant pool index of the FieldrefConstant.
     */
    public int addFieldrefConstant(int    classIndex,
                                   String name,
                                   String descriptor,
                                   Clazz  referencedClass,
                                   Field  referencedField)
    {
        return addFieldrefConstant(classIndex,
                                   addNameAndTypeConstant(name, descriptor),
                                   referencedClass,
                                   referencedField);
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry with the given
     * class constant pool entry index and name and type constant pool entry
     * index.
     * @return the constant pool index of the FieldrefConstant.
     */
    public int addFieldrefConstant(int   classIndex,
                                   int   nameAndTypeIndex,
                                   Clazz referencedClass,
                                   Field referencedField)
    {
        return findOrAddConstant(new FieldrefConstant(classIndex,
                                                nameAndTypeIndex,
                                                referencedClass,
                                                referencedField));
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry with the
     * given class name, method name, and descriptor.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(String className,
                                             String name,
                                             String descriptor,
                                             Clazz  referencedClass,
                                             Method referencedMethod)
    {
        return addInterfaceMethodrefConstant(className,
                                             addNameAndTypeConstant(name, descriptor),
                                             referencedClass,
                                             referencedMethod);
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry with the
     * given class name, method name, and descriptor.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(String className,
                                             int    nameAndTypeIndex,
                                             Clazz  referencedClass,
                                             Method referencedMethod)
    {
        return addInterfaceMethodrefConstant(addClassConstant(className, referencedClass),
                                             nameAndTypeIndex,
                                             referencedClass,
                                             referencedMethod);
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry for the
     * given class and method.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(Clazz  referencedClass,
                                             Method referencedMethod)
    {
        return addInterfaceMethodrefConstant(referencedClass.getName(),
                                             referencedMethod.getName(referencedClass),
                                             referencedMethod.getDescriptor(referencedClass),
                                             referencedClass,
                                             referencedMethod);
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry with the
     * given class constant pool entry index, method name, and descriptor.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(int    classIndex,
                                             String name,
                                             String descriptor,
                                             Clazz  referencedClass,
                                             Method referencedMethod)
    {
        return addInterfaceMethodrefConstant(classIndex,
                                             addNameAndTypeConstant(name, descriptor),
                                             referencedClass,
                                             referencedMethod);
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry with the
     * given class constant pool entry index and name and type constant pool
     * entry index.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(int    classIndex,
                                             int    nameAndTypeIndex,
                                             Clazz  referencedClass,
                                             Method referencedMethod)
    {
        return findOrAddConstant(new InterfaceMethodrefConstant(classIndex,
                                                          nameAndTypeIndex,
                                                          referencedClass,
                                                          referencedMethod));
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry for the given
     * class and method.
     * @return the constant pool index of the MethodrefConstant.
     */
    public int addMethodrefConstant(Clazz  referencedClass,
                                    Method referencedMethod)
    {
        return addMethodrefConstant(referencedClass.getName(),
                                    referencedMethod.getName(referencedClass),
                                    referencedMethod.getDescriptor(referencedClass),
                                    referencedClass,
                                    referencedMethod);
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry with the given
     * class name, method name, and descriptor.
     * @return the constant pool index of the MethodrefConstant.
     */
    public int addMethodrefConstant(String className,
                                    String name,
                                    String descriptor,
                                    Clazz  referencedClass,
                                    Method referencedMethod)
    {
        return addMethodrefConstant(className,
                                    addNameAndTypeConstant(name, descriptor),
                                    referencedClass,
                                    referencedMethod);
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry with the given
     * class name, method name, and descriptor.
     * @return the constant pool index of the MethodrefConstant.
     */
    public int addMethodrefConstant(String className,
                                    int    nameAndTypeIndex,
                                    Clazz  referencedClass,
                                    Method referencedMethod)
    {
        return addMethodrefConstant(addClassConstant(className, referencedClass),
                                    nameAndTypeIndex,
                                    referencedClass,
                                    referencedMethod);
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry with the given
     * class constant pool entry index, method name, and descriptor.
     * @return the constant pool index of the MethodrefConstant.
     */
    public int addMethodrefConstant(int    classIndex,
                                    String name,
                                    String descriptor,
                                    Clazz  referencedClass,
                                    Method referencedMethod)
    {
        return addMethodrefConstant(classIndex,
                                    addNameAndTypeConstant(name, descriptor),
                                    referencedClass,
                                    referencedMethod);
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry with the given
     * class constant pool entry index and name and type constant pool entry
     * index.
     * @return the constant pool index of the MethodrefConstant.
     */
    public int addMethodrefConstant(int    classIndex,
                                    int    nameAndTypeIndex,
                                    Clazz  referencedClass,
                                    Method referencedMethod)
    {
        return findOrAddConstant(new MethodrefConstant(classIndex,
                                                 nameAndTypeIndex,
                                                 referencedClass,
                                                 referencedMethod));
    }


    /**
     * Finds or creates a ClassConstant constant pool entry for the given class.
     * @return the constant pool index of the ClassConstant.
     */
    public int addClassConstant(Clazz referencedClass)
    {
        return addClassConstant(referencedClass.getName(),
                                referencedClass);
    }


    /**
     * Finds or creates a ClassConstant constant pool entry with the given name.
     * @return the constant pool index of the ClassConstant.
     */
    public int addClassConstant(String name,
                                Clazz  referencedClass)
    {
        return addClassConstant(addUtf8Constant(name),
                                referencedClass);
    }


    /**
     * Finds or creates a ClassConstant constant pool entry with the given name
     * UTF-8 constant pool index.
     * @return the constant pool index of the ClassConstant.
     */
    public int addClassConstant(int    nameIndex,
                                Clazz  referencedClass)
    {
        return findOrAddConstant(new ClassConstant(nameIndex, referencedClass));
    }


    /**
     * Finds or creates a MethodTypeConstant constant pool entry with the given
     * descriptor.
     * @return the constant pool index of the MethodTypeConstant.
     */
    public int addMethodTypeConstant(String  descriptor,
                                     Clazz[] referencedClasses)
    {
        return addMethodTypeConstant(addUtf8Constant(descriptor),
                                     referencedClasses);
    }


    /**
     * Finds or creates a MethodTypeConstant constant pool entry with the given
     * descriptor UTF-8 constant pool index.
     * @return the constant pool index of the MethodTypeConstant.
     */
    public int addMethodTypeConstant(int     descriptorIndex,
                                     Clazz[] referencedClasses)
    {
        return findOrAddConstant(new MethodTypeConstant(descriptorIndex,
                                                  referencedClasses));
    }


    /**
     * Finds or creates a NameAndTypeConstant constant pool entry with the given
     * name and descriptor.
     * @return the constant pool index of the NameAndTypeConstant.
     */
    public int addNameAndTypeConstant(String name,
                                      String descriptor)
    {
        return  addNameAndTypeConstant(addUtf8Constant(name),
                                       addUtf8Constant(descriptor));
    }


    /**
     * Finds or creates a NameAndTypeConstant constant pool entry with the given
     * name and descriptor UTF-8 constant pool indices.
     * @return the constant pool index of the NameAndTypeConstant.
     */
    public int addNameAndTypeConstant(int nameIndex,
                                      int descriptorIndex)
    {
        return findOrAddConstant(new NameAndTypeConstant(nameIndex,
                                                   descriptorIndex));
    }


    /**
     * Finds or creates a Utf8Constant constant pool entry for the given string.
     * @return the constant pool index of the Utf8Constant.
     */
    public int addUtf8Constant(String string)
    {
        return findOrAddConstant(new Utf8Constant(string));
    }


    /**
     * Finds or adds a given constant pool entry.
     * @return the index of the entry in the constant pool.
     */
    public int findOrAddConstant(Constant constant)
    {
        int        constantPoolCount = targetClass.u2constantPoolCount;
        Constant[] constantPool      = targetClass.constantPool;

        if (DEBUG)
        {
            System.out.println("ConstantPoolEditor: ["+(targetClass.u2thisClass > 0 ? targetClass.getName() : "(dummy)")+", "+constantPoolCount+" entries] looking for "+constant);
        }

        // Clear the cache if another editor has added any constants behind our back.
        if (cachedCount > 0 &&
            cachedCount != constantPoolCount)
        {
            if (DEBUG)
            {
                System.out.println("ConstantPoolEditor: ["+(targetClass.u2thisClass > 0 ? targetClass.getName() : "(dummy)")+", "+constantPoolCount+" entries, "+cachedCount+" cached] clearing cache");
            }

            cachedIndices = null;
        }

        // Do we have a cache with constant indices?
        if (cachedIndices != null)
        {
            // Look for the index in the hash map.
            Integer index = cachedIndices.get(constant);
            if (index != null)
            {
                if (DEBUG)
                {
                    System.out.println("ConstantPoolEditor: ["+(targetClass.u2thisClass > 0 ? targetClass.getName() : "(dummy)")+", "+constantPoolCount+" entries] cached ["+index+"] "+constant);
                }

                return index.intValue();
            }

            // It's not in the map yet. Add it now.
            cachedIndices.put(constant, Integer.valueOf(constantPoolCount));
        }
        else
        {
            // Look for the constant in the array (always starting at index 1).
            for (int index = 1; index < constantPoolCount; index++)
            {
                if (constant.equals(constantPool[index]))
                {
                    if (DEBUG)
                    {
                        System.out.println("ConstantPoolEditor: ["+(targetClass.u2thisClass > 0 ? targetClass.getName() : "(dummy)")+", "+constantPoolCount+" entries] found ["+index+"] "+constant);
                    }

                    return index;
                }
            }
        }

        // We haven't found the constant in the pool. Just add it.
        return addConstant(constant);
    }


    /**
     * Adds a given constant pool entry to the end of the constant pool.
     * @return the constant pool index for the added entry.
     *
     * @see #findOrAddConstant(Constant)
     */
    public int addConstant(Constant constant)
    {
        int        constantPoolCount = targetClass.u2constantPoolCount;
        Constant[] constantPool      = targetClass.constantPool;

        if (DEBUG)
        {
            System.out.println("ConstantPoolEditor: ["+(targetClass.u2thisClass > 0 ? targetClass.getName() : "(dummy)")+", "+constantPoolCount+" entries] adding "+constant);
        }

        // Make sure there is enough space for another constant pool entry.
        // Category 2 constants (long and double) take up two entries in the
        // constant pool.
        int constantSize = constant.isCategory2() ? 2 : 1;

        if (constantPool.length < constantPoolCount + constantSize)
        {
            if (DEBUG)
            {
                System.out.println("ConstantPoolEditor: ["+(targetClass.u2thisClass > 0 ? targetClass.getName() : "(dummy)")+", "+constantPoolCount+" entries] extending to "+(constantPoolCount+SIZE_INCREMENT)+" entries");
            }

            targetClass.constantPool =
            constantPool             =
                ArrayUtil.extendArray(constantPool,
                                      constantPoolCount + SIZE_INCREMENT);
        }

        // Add the new entry to the end of the constant pool.
        constantPool[constantPoolCount] = constant;

        // Update the counts.
        targetClass.u2constantPoolCount =
        cachedCount                     = constantPoolCount + constantSize;

        // Initialize the class references and class member references in the
        // constant, if necessary.
        if (constantReferenceInitializer != null)
        {
            constant.accept(targetClass, constantReferenceInitializer);
        }

        // Return the old count as the index.
        return constantPoolCount;
    }
}
