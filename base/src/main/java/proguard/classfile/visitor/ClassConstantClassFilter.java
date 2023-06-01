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

import proguard.classfile.Clazz;
import proguard.classfile.ProgramClass;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.util.ClassNameParser;
import proguard.util.ListParser;
import proguard.util.StringMatcher;

/**
 * This ClassVisitor delegates all visits to another given visitor, but only
 * if the visited class contains the specified class constant. This can be
 * useful to avoid applying expensive visitors when they aren't necessary.
 */
public class ClassConstantClassFilter
implements   ClassVisitor,

             // Implementation interfaces.
             ConstantVisitor
{
    private final StringMatcher regularExpressionMatcher;
    private final ClassVisitor  classVisitor;

    private boolean found;

    /**
     * Creates a new  ClassConstantClassFilter.
     * @param regularExpression the regular expression against which class
     *                          names of class constants will be matched.
     * @param classVisitor      the class visitor for classes that contain
     *                          the specified class constant.
     */
    public ClassConstantClassFilter(String       regularExpression,
                                    ClassVisitor classVisitor)
    {
        this(new ListParser(new ClassNameParser()).parse(regularExpression),
             classVisitor);
    }


    /**
     * Creates a new  ClassConstantClassFilter.
     * @param regularExpressionMatcher the string matcher against which
     *                                 class names will be matched.
     * @param classVisitor             the class visitor for classes that
     *                                 contain the specified class constant.
     */
    public ClassConstantClassFilter(StringMatcher regularExpressionMatcher,
                                    ClassVisitor  classVisitor)
    {
        this.regularExpressionMatcher = regularExpressionMatcher;
        this.classVisitor             = classVisitor;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        found = false;
        programClass.constantPoolEntriesAccept(this);
        if (found)
        {
            classVisitor.visitProgramClass(programClass);
        }
    }


    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        if (!found && regularExpressionMatcher.matches(classConstant.getName(clazz)))
        {
            found = true;
        }
    }

}
