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
package proguard.util.kotlin.asserter;

import static proguard.classfile.kotlin.KotlinConstants.dummyClassPool;

import java.util.Arrays;
import java.util.Objects;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.visitor.AllFieldVisitor;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.classfile.visitor.MemberVisitor;

public class AssertUtil
{
    private       String    parentElement;
    private final Reporter  reporter;
    private final ClassPool programClassPool;
    private final ClassPool libraryClassPool;

    public AssertUtil(String    parentElement,
                      Reporter  reporter,
                      ClassPool programClassPool,
                      ClassPool libraryClassPool)
    {
        this.parentElement    = parentElement;
        this.reporter         = reporter;
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;
    }

    public void setParentElement(String parentElement)
    {
        this.parentElement = parentElement;
    }

    public void reportIfNull(String checkedElementName, Object ... checkedElement)
    {
        if (Arrays.stream(checkedElement).allMatch(Objects::isNull))
        {
            reporter.report(parentElement + " has no " + checkedElementName + ".");
        }
    }

    public void reportIfNullReference(String checkedElementName, Object checkedElement)
    {
        if (checkedElement == null)
        {
            reporter.report(parentElement + " has no reference for its " + checkedElementName + ".");
        }
    }

    public void reportIfClassDangling(String checkedElementName,
                                      Clazz  clazz)
    {
        if (clazz != null)
        {
            if (!programClassPool.contains(clazz) &&
                !libraryClassPool.contains(clazz) &&
                !dummyClassPool  .contains(clazz))
            {
                reporter.report(parentElement + " has dangling class reference for its " + checkedElementName + ".");
            }
        }
    }

    public void reportIfFieldDangling(String checkedElementName,
                                      Clazz  checkedClass,
                                      Field  field)
    {
        if (checkedClass != null && field != null)
        {
            ExactMemberMatcher match = new ExactMemberMatcher(field);

            checkedClass.accept(new AllFieldVisitor(match));

            if (!match.memberMatched)
            {
                reporter.report(parentElement + " has a dangling reference for its " + checkedElementName + ".");
            }
        }
    }

    public void reportIfMethodDangling(String checkedElementName,
                                       Clazz  checkedClass,
                                       Method method)
    {
        if (checkedClass != null && method != null)
        {
            ExactMemberMatcher match = new ExactMemberMatcher(method);

            checkedClass.accept(new AllMethodVisitor(match));

            if (!match.memberMatched)
            {
                reporter.report(parentElement + " has a dangling reference for its " + checkedElementName + ".");
            }
        }
    }


    // Small helper classes.

    private static class ExactMemberMatcher
    implements           MemberVisitor
    {
        private final Member memberToMatch;

        boolean memberMatched;

        ExactMemberMatcher(Member memberToMatch)
        {
            this.memberToMatch = memberToMatch;
        }


        // Implementations for MemberVisitor.
        @Override
        public void visitAnyMember(Clazz clazz, Member member)
        {
            if (member == memberToMatch)
            {
                memberMatched = true;
            }
        }
    }
}
