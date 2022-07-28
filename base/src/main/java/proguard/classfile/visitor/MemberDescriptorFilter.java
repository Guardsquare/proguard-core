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

import proguard.classfile.*;
import proguard.util.*;


/**
 * This {@link MemberVisitor} delegates its visits to another given
 * {@link MemberVisitor}, but only when the visited member
 * has a descriptor that matches a given regular expression.
 *
 * @author Eric Lafortune
 */
public class MemberDescriptorFilter implements MemberVisitor
{
    private final StringMatcher regularExpressionMatcher;
    private final MemberVisitor memberVisitor;


    /**
     * Creates a new MemberDescriptorFilter.
     * @param regularExpression the regular expression against which member
     *                          descriptors will be matched.
     * @param memberVisitor     the <code>MemberVisitor</code> to which
     *                          visits will be delegated.
     */
    public MemberDescriptorFilter(String        regularExpression,
                                  MemberVisitor memberVisitor)
    {
        this(regularExpression,
             null,
             memberVisitor);
    }


    /**
     * Creates a new MemberDescriptorFilter.
     * @param regularExpression the regular expression against which member
     *                          descriptors will be matched.
     * @param wildcardManager   an optional scope for StringMatcher instances
     *                          that match wildcards.
     * @param memberVisitor     the <code>MemberVisitor</code> to which
     *                          visits will be delegated.
     */
    public MemberDescriptorFilter(String          regularExpression,
                                  WildcardManager wildcardManager,
                                  MemberVisitor   memberVisitor)
    {
        this(new ListParser(new ClassNameParser(wildcardManager)).parse(regularExpression),
             memberVisitor);
    }


    /**
     * Creates a new MemberDescriptorFilter.
     * @param regularExpressionMatcher the regular expression against which
     *                                 member descriptors will be matched.
     * @param memberVisitor            the <code>MemberVisitor</code> to which
     *                                 visits will be delegated.
     */
    public MemberDescriptorFilter(StringMatcher regularExpressionMatcher,
                                  MemberVisitor memberVisitor)
    {
        this.regularExpressionMatcher = regularExpressionMatcher;
        this.memberVisitor            = memberVisitor;
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        if (accepted(programField.getDescriptor(programClass)))
        {
            memberVisitor.visitProgramField(programClass, programField);
        }
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        if (accepted(programMethod.getDescriptor(programClass)))
        {
            memberVisitor.visitProgramMethod(programClass, programMethod);
        }
    }


    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        if (accepted(libraryField.getDescriptor(libraryClass)))
        {
            memberVisitor.visitLibraryField(libraryClass, libraryField);
        }
    }


    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        if (accepted(libraryMethod.getDescriptor(libraryClass)))
        {
            memberVisitor.visitLibraryMethod(libraryClass, libraryMethod);
        }
    }


    // Small utility methods.

    private boolean accepted(String name)
    {
        return regularExpressionMatcher.matches(name);
    }
}
