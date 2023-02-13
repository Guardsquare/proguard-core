/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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
package proguard.classfile.visitor;

import proguard.classfile.AccessConstants;
import proguard.classfile.Clazz;

import java.util.HashSet;
import java.util.Set;

/**
 * This {@link ClassVisitor} delegates its visits to another given
 * {@link ClassVisitor}, but only for functional interfaces, that
 * is, interface classes that have exactly one abstract method.
 *
 * @author Eric Lafortune
 */
public class FunctionalInterfaceFilter implements ClassVisitor
{
    private final ClassVisitor classVisitor;


    /**
     * Creates a new ProgramClassFilter.
     * @param classVisitor the <code>ClassVisitor</code> to which visits
     *                     will be delegated.
     */
    public FunctionalInterfaceFilter(ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        if (isFunctionalInterface(clazz))
        {
            clazz.accept(classVisitor);
        }
    }


    // Small utility methods.

    private boolean isFunctionalInterface(Clazz clazz)
    {
        // Is it an interface?
        if ((clazz.getAccessFlags() & AccessConstants.INTERFACE) == 0)
        {
            return false;
        }

        // Count the abstract methods and the default methods in the
        // interface hierarchy (including Object).
        Set<String> abstractMethods = new HashSet<>();
        Set<String> defaultMethods  = new HashSet<>();

        clazz.hierarchyAccept(true, true, true, false,
                              new AllMethodVisitor(
                              new MultiMemberVisitor(
                                  new MemberAccessFilter(AccessConstants.ABSTRACT, 0,
                                  new MemberCollector(false, true, true, abstractMethods)),

                                  new MemberAccessFilter(0, AccessConstants.ABSTRACT,
                                  new MemberCollector(false, true, true, defaultMethods))
                              )));

        // Ignore marker interfaces.
        if (abstractMethods.size() == 0)
        {
            return false;
        }

        // Subtract default methods, since only abstract methods that don't
        // have default implementations count.
        abstractMethods.removeAll(defaultMethods);

        // Also consider this a functional interface if all abstract methods
        // have default implementations. Dynamic invocations may explicitly
        // specify the intended single abstract method. [PGD-756]
        return abstractMethods.size() <= 1;
    }
}
