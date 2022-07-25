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
import proguard.util.ArrayUtil;

import java.util.Objects;

/**
 * This class can add interfaces and class members to a given class.
 * Elements to be added must be filled out beforehand, including their
 * references to the constant pool.
 *
 * @author Eric Lafortune
 * @author Joren Van Hecke
 */
public class LibraryClassEditor
{
    private static final boolean DEBUG = false;

    private final LibraryClass targetClass;


    /**
     * Creates a new ClassEditor that will edit elements in the given
     * target class.
     */
    public LibraryClassEditor(LibraryClass targetClass)
    {
        this.targetClass = targetClass;
    }


    /**
     * Adds the given interface.
     */
    public void addInterface(String interfaceName, Clazz referencedInterface)
    {
        targetClass.interfaceNames =
                ArrayUtil.add(targetClass.interfaceNames,
                              targetClass.interfaceNames.length + 1,
                              interfaceName);
        targetClass.interfaceClasses =
                ArrayUtil.add(targetClass.interfaceClasses,
                              targetClass.interfaceClasses.length + 1,
                              referencedInterface
                        );
        if (DEBUG)
        {
            System.out.println(targetClass.getName()+": adding interface ["+interfaceName+"]");
        }
    }

    /**
     * Removes the given interface.
     */
    public void removeInterface(String interfaceName)
    {
        if (DEBUG)
        {
            System.out.println(targetClass.getName()+": removing interface ["+interfaceName+"]");
        }

        ArrayUtil.remove(targetClass.interfaceNames,
                         targetClass.interfaceNames.length,
                         findInterfaceIndex(interfaceName));
        // Also remove the class reference of the interface
        ArrayUtil.remove(targetClass.interfaceClasses,
                         targetClass.interfaceClasses.length,
                         findInterfaceClazzIndex(interfaceName));
    }


    /**
     * Finds the index of the given interface in the target class.
     */

    private int findInterfaceIndex(String interfaceName)
    {
        String[] interfacesNames = targetClass.interfaceNames;

        for (int index = 0; index < interfacesNames.length; index++)
        {
            if (Objects.equals(interfacesNames[index], interfaceName))
            {
                return index;
            }
        }

        return interfacesNames.length;
    }

    /**
     * Finds the index of the given interface clazz reference in the target class.
     */

    private int findInterfaceClazzIndex(String interfaceName)
    {
        Clazz[] interfacesClasses = targetClass.interfaceClasses;

        for (int index = 0; index < interfacesClasses.length; index++)
        {
            if (interfacesClasses[index] != null
                    && Objects.equals(interfacesClasses[index].getName(), interfaceName))
            {
                return index;
            }
        }

        return interfacesClasses.length;
    }


    /**
     * Adds the given field.
     */
    public void addField(Field field)
    {
        if (DEBUG)
        {
            System.out.println(targetClass.getName()+": adding field ["+field.getName(targetClass)+" "+field.getDescriptor(targetClass)+"]");
        }

        targetClass.fields =
            (LibraryField[])ArrayUtil.add(targetClass.fields,
                                          targetClass.fields.length,
                                          field);
    }


    /**
     * Removes the given field. Note that removing a field that is still being
     * referenced can cause unpredictable effects.
     */
    public void removeField(Field field)
    {
        if (DEBUG)
        {
            System.out.println(targetClass.getName()+": removing field ["+field.getName(targetClass)+" "+field.getDescriptor(targetClass)+"]");
        }

        ArrayUtil.remove(targetClass.fields,
                         targetClass.fields.length,
                         findFieldIndex(field));
    }


    /**
     * Finds the index of the given field in the target class.
     */

    private int findFieldIndex(Field field)
    {
        int     fieldsCount = targetClass.fields.length;
        Field[] fields      = targetClass.fields;

        for (int index = 0; index < fieldsCount; index++)
        {
            if (fields[index] != null && fields[index].equals(field))
            {
                return index;
            }
        }

        return fieldsCount;
    }


    /**
     * Adds the given method.
     */
    public void addMethod(Method method)
    {
        if (DEBUG)
        {
            System.out.println(targetClass.getName()+": adding method ["+method.getName(targetClass)+method.getDescriptor(targetClass)+"]");
        }

        targetClass.methods =
            (LibraryMethod[])ArrayUtil.add(targetClass.methods,
                                           targetClass.methods.length,
                                           method);
    }


    /**
     * Removes the given method. Note that removing a method that is still being
     * referenced can cause unpredictable effects.
     */
    public void removeMethod(Method method)
    {
        if (DEBUG)
        {
            System.out.println(targetClass.getName()+": removing method ["+method.getName(targetClass)+method.getDescriptor(targetClass)+"]");
        }

        ArrayUtil.remove(targetClass.methods,
                         targetClass.methods.length,
                         findMethodIndex(method));
    }


    /**
     * Finds the index of the given method in the target class.
     */

    private int findMethodIndex(Method method)
    {
        int      methodsCount = targetClass.methods.length;
        Method[] methods      = targetClass.methods;

        for (int index = 0; index < methodsCount; index++)
        {
            if (methods[index] != null && methods[index].equals(method))
            {
                return index;
            }
        }

        return methodsCount;
    }
}
