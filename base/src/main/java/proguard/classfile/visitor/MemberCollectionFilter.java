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
import proguard.classfile.Member;

import java.util.Set;


/**
 * This MemberVisitor delegates its visits to one of two other visitors,
 * depending on whether the member is present in the given member collection or not.
 *
 * @author Johan Leys
 */
public class MemberCollectionFilter
implements   MemberVisitor
{
    private final Set<Member> members;

    private final MemberVisitor acceptedVisitor;
    private final MemberVisitor rejectedVisitor;

    /**
     * Creates a new MemberCollectionFilter.
     *
     * @param members         the members collection to be searched in.
     * @param acceptedVisitor this visitor will be called for members that are
     *                        present in the member collection.
     */
    public MemberCollectionFilter(Set<Member>   members,
                                  MemberVisitor acceptedVisitor)
    {
        this(members, acceptedVisitor, null);
    }


    /**
     * Creates a new MemberCollectionFilter.
     *
     * @param members         the member collection to be searched in.
     * @param acceptedVisitor this visitor will be called for members that are
     *                        present in the member collection.
     * @param rejectedVisitor this visitor will be called otherwise.
     */
    public MemberCollectionFilter(Set<Member>   members,
                                  MemberVisitor acceptedVisitor,
                                  MemberVisitor rejectedVisitor)
    {
        this.members       = members;
        this.acceptedVisitor = acceptedVisitor;
        this.rejectedVisitor = rejectedVisitor;
    }


    // Implementations for MemberVisitor.

    public void visitAnyMember(Clazz clazz, Member member)
    {
        MemberVisitor delegateVisitor = delegateVisitor(member);
        if (delegateVisitor != null)
        {
            member.accept(clazz, delegateVisitor);
        }
    }


    // Small utility methods.

    private MemberVisitor delegateVisitor(Member member)
    {
        return (members.contains(member)) ?
            acceptedVisitor : rejectedVisitor;
    }
}
