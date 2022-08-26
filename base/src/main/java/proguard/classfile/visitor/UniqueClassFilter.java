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
package proguard.classfile.visitor;

import proguard.classfile.Clazz;

import java.util.HashSet;
import java.util.Set;

/**
 * This <code>ClassVisitor</code> delegates its visits to another given
 * <code>ClassVisitor</code>, but at most a single time.
 *
 * @author Eric Lafortune
 */
public class UniqueClassFilter
implements ClassVisitor
{
    private final ClassVisitor classVisitor;

    private final Set<String> classNames = new HashSet<>();


    /**
     * Creates a new UniqueClassFilter.
     * @param classVisitor the <code>ClassVisitor</code> to which visits
     *                     will be delegated.
     */
    public UniqueClassFilter(ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        // Only visit the class if it hasn't been visited before.
        if (classNames.add(clazz.getName()))
        {
            clazz.accept(classVisitor);
        }
    }
}
