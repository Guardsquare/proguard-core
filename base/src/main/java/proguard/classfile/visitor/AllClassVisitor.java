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


/**
 * This {@link ClassPoolVisitor} lets a given {@link ClassVisitor} visit all Clazz
 * instances of the class pools it visits.
 *
 * @author Eric Lafortune
 */
public class AllClassVisitor implements ClassPoolVisitor
{
    private final ClassVisitor classVisitor;


    public AllClassVisitor(ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
    }


    public void visitClassPool(ClassPool classPool)
    {
        classPool.classesAccept(classVisitor);
    }
}
