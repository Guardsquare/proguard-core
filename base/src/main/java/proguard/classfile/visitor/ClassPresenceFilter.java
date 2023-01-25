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
package proguard.classfile.visitor;

import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;

/**
 * This {@link ClassVisitor} delegates its visits to one of two
 * {@link ClassVisitor} instances, depending on whether the name of
 * the visited class file is present in a given {@link ClassPool} or not.
 *
 * @author Eric Lafortune
 */
public class ClassPresenceFilter
implements   ClassVisitor
{
    private final ClassPool    classPool;
    private final ClassVisitor presentClassVisitor;
    private final ClassVisitor missingClassVisitor;


    /**
     * Creates a new ClassPresenceFilter.
     * @param classPool           the <code>ClassPool</code> in which the
     *                            presence will be tested.
     * @param presentClassVisitor the <code>ClassVisitor</code> to which visits
     *                            of present class files will be delegated.
     * @param missingClassVisitor the <code>ClassVisitor</code> to which visits
     *                            of missing class files will be delegated.
     */
    public ClassPresenceFilter(ClassPool    classPool,
                               ClassVisitor presentClassVisitor,
                               ClassVisitor missingClassVisitor)
    {
        this.classPool           = classPool;
        this.presentClassVisitor = presentClassVisitor;
        this.missingClassVisitor = missingClassVisitor;
    }

    /**
     * Creates a new ClassPresenceFilter.
     * @param classPool           the <code>ClassPool</code> in which the
     *                            presence will be tested.
     * @param presentClassVisitor the <code>ClassVisitor</code> to which visits
     *                            of present class files will be delegated.
     */
    public ClassPresenceFilter(ClassPool    classPool,
                               ClassVisitor presentClassVisitor)
    {
        this(classPool, presentClassVisitor, null);
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        ClassVisitor classFileVisitor = classFileVisitor(clazz);

        if (classFileVisitor != null)
        {
            clazz.accept(classFileVisitor);
        }
    }


    // Small utility methods.

    /**
     * Returns the appropriate <code>ClassVisitor</code>.
     */
    private ClassVisitor classFileVisitor(Clazz clazz)
    {
        return classPool.getClass(clazz.getName()) != null ?
            presentClassVisitor :
            missingClassVisitor;
    }
}
