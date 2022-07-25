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
package proguard.classfile.util;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.visitor.*;

/**
 * This utility class can find the nest host class names of given classes.
 *
 * @author Eric Lafortune
 */
public class NestHostFinder
implements   ClassVisitor,
             AttributeVisitor
{
    private String nestHostClassName;


    /**
     * Returns whether the two given classes are in the same nest.
     */
    public boolean inSameNest(Clazz class1, Clazz class2)
    {
        // Are the classes the same?
        if (class1.equals(class2))
        {
            return true;
        }

        // Do the classes have the same nest host?
        String nestHostClassName1 = findNestHostClassName(class1);
        String nestHostClassName2 = findNestHostClassName(class2);

        return nestHostClassName1.equals(nestHostClassName2);
    }


    /**
     * Returns the class name of the nest host of the given class.
     * This may be the class itself, if the class doesn't have a nest host
     * attribute (including for class versions below Java 11 and for library
     * classes).
     */
    public String findNestHostClassName(Clazz clazz)
    {
        // The default is the name of the class itself.
        nestHostClassName = clazz.getName();

        // Look for an explicit attribute.
        clazz.accept(this);

        // Return the found name.
        return nestHostClassName;
    }


    // Implementations for ClassVisitor.


    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        // Only program classes store their versions and attributes.
        // The nest host attribute only exists since Java 10.
        if (programClass.u4version >= VersionConstants.CLASS_VERSION_10)
        {
            programClass.attributesAccept(this);
        }
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitNestHostAttribute(Clazz clazz, NestHostAttribute nestHostAttribute)
    {
        // Remember the class name of the nest host.
        nestHostClassName = clazz.getClassName(nestHostAttribute.u2hostClassIndex);
    }
}
