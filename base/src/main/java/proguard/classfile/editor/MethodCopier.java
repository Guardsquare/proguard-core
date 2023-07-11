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
package proguard.classfile.editor;

import proguard.classfile.Clazz;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExtendedLineNumberInfo;
import proguard.classfile.attribute.LineNumberInfo;
import proguard.classfile.attribute.LineNumberTableAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;
import proguard.classfile.visitor.MultiMemberVisitor;
import proguard.util.StringFunction;

/**
 * This {@link ClassVisitor} copies a method into a target class. It first checks if a method with the same name
 * and descriptor as the method to be copied already exists in the target class. If that is indeed the case,
 * this visitor is a no-op.
 * This visitor optionally uses a name transformer function to apply to the original method's name before copying.
 * If no name transformer function is provided, the original method's name is used, instead.
 * If the method to be copied is indeed copied into the target class, this visitor passes the resulting method
 * to an extra member visitor, if provided.
 */
public class MethodCopier
implements   ClassVisitor,
             MemberVisitor,
             AttributeVisitor
{
    private final ProgramClass   sourceClass;
    private final ProgramMethod  sourceMethod;
    private final StringFunction nameTransformer;
    private final MemberVisitor  extraMemberVisitor;

    public MethodCopier(ProgramClass sourceClass, ProgramMethod sourceMethod)
    {
        this(sourceClass, sourceMethod, null);
    }

    public MethodCopier(ProgramClass sourceClass, ProgramMethod sourceMethod, StringFunction nameTransformer)
    {
        this(sourceClass, sourceMethod, nameTransformer, null);
    }

    public MethodCopier(ProgramClass sourceClass,
                        ProgramMethod sourceMethod,
                        StringFunction nameTransformer,
                        MemberVisitor extraMemberVisitor)
    {
        this.sourceClass        = sourceClass;
        this.sourceMethod       = sourceMethod;
        this.nameTransformer    = nameTransformer;
        this.extraMemberVisitor = extraMemberVisitor;
    }

    // ClassVisitor

    @Override
    public void visitAnyClass(Clazz clazz) {}

    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        if (methodAlreadyExistsInClass(programClass))
        {
            return;
        }

        MemberVisitor copiedMethodVisitor = extraMemberVisitor == null ? this
                                                                       : new MultiMemberVisitor(this,
                                                                                                extraMemberVisitor);

        new MemberAdder(programClass,
                        nameTransformer,
                        copiedMethodVisitor).visitProgramMethod(sourceClass, sourceMethod);
    }

    private boolean methodAlreadyExistsInClass(ProgramClass programClass)
    {
        String sourceMethodName        = sourceMethod.getName(sourceClass);
        String sourceMethodDescriptor  = sourceMethod.getDescriptor(sourceClass);

        String toCheckName             = nameTransformer == null ? sourceMethodName
                                                                 : nameTransformer.transform(sourceMethodName);

        return programClass.findMethod(toCheckName, sourceMethodDescriptor) != null;
    }

    // MemberVisitor

    @Override
    public void visitAnyMember(Clazz clazz, Member member) {}

    @Override
    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programMethod.accept(programClass,
                             new AllAttributeVisitor(new AttributeNameFilter(Attribute.CODE, this)));
    }

    // AttributeVisitor

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // The source values for the copied method's line numbers should refer to the original method.
        LineNumberTableAttribute copiedMethodLineNumberTableAttribute
                = (LineNumberTableAttribute) codeAttribute.getAttribute(clazz, Attribute.LINE_NUMBER_TABLE);

        if (copiedMethodLineNumberTableAttribute == null || copiedMethodLineNumberTableAttribute.u2lineNumberTableLength == 0)
        {
            return;
        }

        sourceMethod.accept(sourceClass,
                            new AllAttributeVisitor(
                            new AttributeNameFilter(Attribute.CODE,
                            new MyLineFixer(copiedMethodLineNumberTableAttribute))));
    }

    private static class MyLineFixer implements AttributeVisitor
    {
        private final LineNumberTableAttribute copiedMethodLineNumberTableAttribute;

        MyLineFixer(LineNumberTableAttribute copiedMethodLineNumberTableAttribute)
        {
            this.copiedMethodLineNumberTableAttribute = copiedMethodLineNumberTableAttribute;
        }

        public void visitCodeAttribute(Clazz sourceClass, Method sourceMethod, CodeAttribute sourceMethodCodeAttribute) {
            LineNumberTableAttribute sourceMethodLineNumberTableAttribute = (LineNumberTableAttribute) sourceMethodCodeAttribute.getAttribute(sourceClass, Attribute.LINE_NUMBER_TABLE);
            int                      lowestLineNumber                     = sourceMethodLineNumberTableAttribute == null
                                                                                ? 0
                                                                                : sourceMethodLineNumberTableAttribute.getLowestLineNumber();
            int                      highestLineNumber                    = sourceMethodLineNumberTableAttribute == null
                                                                                ? 0
                                                                                : sourceMethodLineNumberTableAttribute.getHighestLineNumber();

            String newSource = initializeLineNumberInfoSource(sourceClass,
                                                              sourceMethod,
                                                              lowestLineNumber,
                                                              highestLineNumber);

            for (int i = 0; i < copiedMethodLineNumberTableAttribute.u2lineNumberTableLength; i++)
            {
                LineNumberInfo         currentLineNumberInfo = copiedMethodLineNumberTableAttribute.lineNumberTable[i];
                ExtendedLineNumberInfo newLineNumberInfo     = new ExtendedLineNumberInfo(currentLineNumberInfo.u2startPC,
                                                                                          currentLineNumberInfo.u2lineNumber,
                                                                                          newSource);
                copiedMethodLineNumberTableAttribute.lineNumberTable[i] = newLineNumberInfo;
            }
        }

        private String initializeLineNumberInfoSource(Clazz  sourceClass,
                                                      Method sourceMethod,
                                                      int    startLineNumber,
                                                      int    endLineNumber)
        {
            return sourceClass.getName()                   + "." +
                   sourceMethod.getName(sourceClass)       +
                   sourceMethod.getDescriptor(sourceClass) + ":" +
                   startLineNumber                         + ":" +
                   endLineNumber;
        }
    }
}
