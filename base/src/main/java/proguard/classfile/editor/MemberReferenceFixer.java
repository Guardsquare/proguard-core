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
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

/**
 * This {@link ClassVisitor} fixes constant pool field and method references to fields
 * and methods whose names or descriptors have changed.
 *
 * @author Eric Lafortune
 */
public class MemberReferenceFixer
implements   ClassVisitor,
             ConstantVisitor,
             MemberVisitor,
             RecordComponentInfoVisitor,
             AttributeVisitor,
             AnnotationVisitor,
             ElementValueVisitor
{
    private static final boolean DEBUG = false;


    private final boolean android;

    private final StackSizeUpdater stackSizeUpdater = new StackSizeUpdater();

    // Parameter for the visitor methods.
    private int constantIndex;

    // Return values for the visitor methods.
    private boolean isInterfaceMethod;
    private boolean stackSizesMayHaveChanged;


    /**
     * Creates a new MemberReferenceFixer.
     *
     * @param android specifies whether the target is Android. This has subtle
     *                implications when fixing enum annotations.
     */
    public MemberReferenceFixer(boolean android)
    {
        this.android = android;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        stackSizesMayHaveChanged = false;

        // Fix the constant pool entries.
        for (int index = 1; index < programClass.u2constantPoolCount; index++)
        {
            Constant constant = programClass.constantPool[index];
            if (constant != null)
            {
                // Fix the entry, replacing it entirely if needed.
                this.constantIndex = index;

                constant.accept(programClass, this);
            }
        }

        // Fix the class members.
        programClass.fieldsAccept(this);
        programClass.methodsAccept(this);

        // Fix the attributes.
        programClass.attributesAccept(this);
    }


    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        // Does the string refer to a class member, due to a
        // Class.get[Declared]{Field,Method} construct?
        Member referencedMember = stringConstant.referencedMember;
        if (referencedMember != null)
        {
            Clazz referencedClass = stringConstant.referencedClass;

            // Is it a descriptor or member name?
            // Does it have a new name?
            String newName = stringConstant.getString(clazz).startsWith("(") ?
                referencedMember.getDescriptor(referencedClass) :
                referencedMember.getName(referencedClass)       ;

            if (!stringConstant.getString(clazz).equals(newName))
            {
                if (DEBUG)
                {
                    debug(clazz, stringConstant, referencedClass, referencedMember);
                }

                // Update the name.
                stringConstant.u2stringIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newName);
            }
        }
    }


    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        // Do we know the referenced field?
        Field referencedField = fieldrefConstant.referencedField;
        if (referencedField != null)
        {
            Clazz referencedClass = fieldrefConstant.referencedClass;

            // Does it have a new name or type?
            String newName = referencedField.getName(referencedClass);
            String newType = referencedField.getDescriptor(referencedClass);

            if (!fieldrefConstant.getName(clazz).equals(newName) ||
                !fieldrefConstant.getType(clazz).equals(newType))
            {
                if (DEBUG)
                {
                    debug(clazz, fieldrefConstant, referencedClass, referencedField);
                }

                // Update the name and type index.
                fieldrefConstant.u2nameAndTypeIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addNameAndTypeConstant(newName, newType);
            }
        }
    }


    public void visitInterfaceMethodrefConstant(Clazz clazz, InterfaceMethodrefConstant interfaceMethodrefConstant)
    {
        // Do we know the referenced interface method?
        Method referencedMethod = interfaceMethodrefConstant.referencedMethod;
        if (referencedMethod != null)
        {
            Clazz referencedClass = interfaceMethodrefConstant.referencedClass;

            // Does it have a new name or type?
            String newName = referencedMethod.getName(referencedClass);
            String newType = referencedMethod.getDescriptor(referencedClass);

            if (!interfaceMethodrefConstant.getName(clazz).equals(newName) ||
                !interfaceMethodrefConstant.getType(clazz).equals(newType))
            {
                if (DEBUG)
                {
                    debug(clazz, interfaceMethodrefConstant, referencedClass, referencedMethod);
                }

                // Update the name and type index.
                interfaceMethodrefConstant.u2nameAndTypeIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addNameAndTypeConstant(newName, newType);

                // Remember that the stack sizes of the methods in this class
                // may have changed.
                stackSizesMayHaveChanged = true;
            }

            // Check if this is an interface method.
            isInterfaceMethod = true;
            clazz.constantPoolEntryAccept(interfaceMethodrefConstant.u2classIndex, this);

            // Has the method become a non-interface method?
            if (!isInterfaceMethod)
            {
                if (DEBUG)
                {
                    System.out.println("MemberReferenceFixer:");
                    System.out.println("  Class file     = "+clazz.getName());
                    System.out.println("  Ref class      = "+referencedClass.getName());
                    System.out.println("  Ref method     = "+interfaceMethodrefConstant.getName(clazz)+interfaceMethodrefConstant.getType(clazz));
                    System.out.println("    -> ordinary method");
                }

                // Replace the interface method reference by a method reference.
                ((ProgramClass)clazz).constantPool[this.constantIndex] =
                    new MethodrefConstant(interfaceMethodrefConstant.u2classIndex,
                                          interfaceMethodrefConstant.u2nameAndTypeIndex,
                                          referencedClass,
                                          referencedMethod);
            }
        }
    }


    public void visitMethodrefConstant(Clazz clazz, MethodrefConstant methodrefConstant)
    {
        // Do we know the referenced method?
        Method referencedMethod = methodrefConstant.referencedMethod;
        if (referencedMethod != null)
        {
            Clazz referencedClass = methodrefConstant.referencedClass;

            // Does it have a new name or type?
            String newName = referencedMethod.getName(referencedClass);
            String newType = referencedMethod.getDescriptor(referencedClass);

            if (!methodrefConstant.getName(clazz).equals(newName) ||
                !methodrefConstant.getType(clazz).equals(newType))
            {
                if (DEBUG)
                {
                    debug(clazz, methodrefConstant, referencedClass, referencedMethod);
                }

                // Update the name and type index.
                methodrefConstant.u2nameAndTypeIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addNameAndTypeConstant(newName, newType);

                // Remember that the stack sizes of the methods in this class
                // may have changed.
                stackSizesMayHaveChanged = true;
            }

            // Check if this is an interface method.
            isInterfaceMethod = false;
            clazz.constantPoolEntryAccept(methodrefConstant.u2classIndex, this);

            // Has the method become an interface method?
            if (isInterfaceMethod)
            {
                if (DEBUG)
                {
                    System.out.println("MemberReferenceFixer:");
                    System.out.println("  Class file     = "+clazz.getName());
                    System.out.println("  Ref class      = "+referencedClass.getName());
                    System.out.println("  Ref method     = "+methodrefConstant.getName(clazz)+methodrefConstant.getType(clazz));
                    System.out.println("    -> interface method");
                }

                // Replace the method reference by an interface method reference.
                ((ProgramClass)clazz).constantPool[this.constantIndex] =
                    new InterfaceMethodrefConstant(methodrefConstant.u2classIndex,
                                                   methodrefConstant.u2nameAndTypeIndex,
                                                   referencedClass,
                                                   referencedMethod);
            }
        }
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Check if this class entry is an array type.
        if (ClassUtil.isInternalArrayType(classConstant.getName(clazz)))
        {
            isInterfaceMethod = false;
        }
        else
        {
            // Check if this class entry refers to an interface class.
            Clazz referencedClass = classConstant.referencedClass;
            if (referencedClass != null)
            {
                isInterfaceMethod = (referencedClass.getAccessFlags() & AccessConstants.INTERFACE) != 0;
            }
        }
    }


    // Implementations for MemberVisitor.

    public void visitProgramMember(ProgramClass programClass, ProgramMember programMember)
    {
        // Fix the attributes.
        programMember.attributesAccept(programClass, this);
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitRecordAttribute(Clazz clazz, RecordAttribute recordAttribute)
    {
        // Fix the components.
        recordAttribute.componentsAccept(clazz, this);
    }


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        Member referencedMember = enclosingMethodAttribute.referencedMethod;
        if (referencedMember != null)
        {
            Clazz referencedClass = enclosingMethodAttribute.referencedClass;

            // Does it have a new name or type?
            String newName = referencedMember.getName(referencedClass);
            String newType = referencedMember.getDescriptor(referencedClass);

            if (!enclosingMethodAttribute.getName(clazz).equals(newName) ||
                !enclosingMethodAttribute.getType(clazz).equals(newType))
            {
                // Update the name and type index.
                enclosingMethodAttribute.u2nameAndTypeIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addNameAndTypeConstant(newName,
                                                                                       newType);
            }
        }
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Recompute the maximum stack size if necessary.
        if (stackSizesMayHaveChanged)
        {
            stackSizeUpdater.visitCodeAttribute(clazz, method, codeAttribute);
        }

        // Fix the nested attributes.
        codeAttribute.attributesAccept(clazz, method, this);
    }


    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        // Fix the annotations.
        annotationsAttribute.annotationsAccept(clazz, this);
    }


    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        // Fix the annotations.
        parameterAnnotationsAttribute.annotationsAccept(clazz, method, this);
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        // Fix the annotation.
        annotationDefaultAttribute.defaultValueAccept(clazz, this);
    }


    // Implementations for RecordComponentInfoVisitor.

    public void visitRecordComponentInfo(Clazz clazz, RecordComponentInfo recordComponentInfo)
    {
        // Do we know the referenced field?
        Field referencedField = recordComponentInfo.referencedField;
        if (referencedField != null)
        {
            // Does it have a new name?
            String newName = referencedField.getName(clazz);
            if (!recordComponentInfo.getName(clazz).equals(newName))
            {
                if (DEBUG)
                {
                    debug(clazz, recordComponentInfo, referencedField);
                }

                // Update the nameindex.
                recordComponentInfo.u2nameIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newName);
            }

            // Does it have a new descriptor?
            String newDescriptor = referencedField.getDescriptor(clazz);
            if (!recordComponentInfo.getDescriptor(clazz).equals(newDescriptor))
            {
                if (DEBUG)
                {
                    debug(clazz, recordComponentInfo, referencedField);
                }

                // Update the descriptor index.
                recordComponentInfo.u2descriptorIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newDescriptor);
            }
        }

        // Fix the attributes.
        recordComponentInfo.attributesAccept(clazz, this);
    }


    // Implementations for AnnotationVisitor.

    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        // Fix the element values.
        annotation.elementValuesAccept(clazz, this);
    }


    // Implementations for ElementValueVisitor.

    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
        fixElementValue(clazz, annotation, constantElementValue);
    }


    public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        fixElementValue(clazz, annotation, enumConstantElementValue);

        // The Java VM expects the original enum constant name, i.e. the
        // name string stored in the enum constant.
        // The Android tools (dx, D8,...) expect the updated enum constant
        // name, i.e. the name of the static field in the enum class.
        if (android)
        {
            // Do we know the referenced enum field?
            Member referencedField = enumConstantElementValue.referencedField;
            if (referencedField != null)
            {
                Clazz referencedClass = enumConstantElementValue.referencedClasses[0];

                // Does it have a new name?
                String newName = referencedField.getName(referencedClass);

                if (!enumConstantElementValue.getConstantName(clazz).equals(newName))
                {
                    // Update the name index.
                    enumConstantElementValue.u2constantNameIndex =
                        new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newName);
                }
            }
        }
    }


    public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        fixElementValue(clazz, annotation, classElementValue);
    }


    public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        fixElementValue(clazz, annotation, annotationElementValue);

        // Fix the annotation.
        annotationElementValue.annotationAccept(clazz, this);
    }


    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        fixElementValue(clazz, annotation, arrayElementValue);

        // Fix the element values.
        arrayElementValue.elementValuesAccept(clazz, annotation, this);
    }


    // Small utility methods.

    /**
     * Fixes the method reference of the element value, if any.
     */
    private void fixElementValue(Clazz        clazz,
                                 Annotation   annotation,
                                 ElementValue elementValue)
    {
        // Do we know the referenced method?
        Member referencedMember = elementValue.referencedMethod;
        if (referencedMember != null)
        {
            // Does it have a new name or type?
            String methodName    = elementValue.getMethodName(clazz);
            String newMethodName = referencedMember.getName(elementValue.referencedClass);

            if (!methodName.equals(newMethodName))
            {
                // Update the element name index.
                elementValue.u2elementNameIndex =
                    new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newMethodName);
            }
        }
    }


    private void debug(Clazz          clazz,
                       StringConstant stringConstant,
                       Clazz          referencedClass,
                       Member         referencedMember)
    {
        System.out.println("MemberReferenceFixer:");
        System.out.println("  ["+clazz.getName()+"]: String ["+
                           stringConstant.getString(clazz)+"] -> ["+
                           referencedClass.getName()+"."+referencedMember.getName(referencedClass)+" "+referencedMember.getDescriptor(referencedClass)+"]");
    }


    private void debug(Clazz       clazz,
                       RefConstant refConstant,
                       Clazz       referencedClass,
                       Member      referencedMember)
    {
        System.out.println("MemberReferenceFixer:");
        System.out.println("  ["+clazz.getName()+"]: ["+
                           refConstant.getClassName(clazz)+"."+refConstant.getName(clazz)+" "+refConstant.getType(clazz)+"] -> ["+
                           referencedClass.getName()+"."+referencedMember.getName(referencedClass)+" "+referencedMember.getDescriptor(referencedClass)+"]");
    }


    private void debug(Clazz               clazz,
                       RecordComponentInfo recordComponentInfo,
                       Field               referencedField)
    {
        System.out.println("MemberReferenceFixer:");
        System.out.println("  ["+clazz.getName()+"]: ["+
                           recordComponentInfo.getName(clazz)+" "+recordComponentInfo.getDescriptor(clazz)+"] -> ["+
                           referencedField.getName(clazz)+" "+referencedField.getDescriptor(clazz)+"]");
    }
}
