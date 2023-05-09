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
package proguard.classfile.util;

import proguard.classfile.AccessConstants;
import proguard.classfile.ClassConstants;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.LibraryClass;
import proguard.classfile.LibraryField;
import proguard.classfile.LibraryMethod;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramField;
import proguard.classfile.ProgramMethod;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.EnclosingMethodAttribute;
import proguard.classfile.attribute.InnerClassesAttribute;
import proguard.classfile.attribute.InnerClassesInfo;
import proguard.classfile.attribute.LocalVariableInfo;
import proguard.classfile.attribute.LocalVariableTableAttribute;
import proguard.classfile.attribute.LocalVariableTypeInfo;
import proguard.classfile.attribute.LocalVariableTypeTableAttribute;
import proguard.classfile.attribute.RecordAttribute;
import proguard.classfile.attribute.RecordComponentInfo;
import proguard.classfile.attribute.SignatureAttribute;
import proguard.classfile.attribute.annotation.Annotation;
import proguard.classfile.attribute.annotation.AnnotationDefaultAttribute;
import proguard.classfile.attribute.annotation.AnnotationElementValue;
import proguard.classfile.attribute.annotation.AnnotationsAttribute;
import proguard.classfile.attribute.annotation.ArrayElementValue;
import proguard.classfile.attribute.annotation.ClassElementValue;
import proguard.classfile.attribute.annotation.ConstantElementValue;
import proguard.classfile.attribute.annotation.ElementValue;
import proguard.classfile.attribute.annotation.EnumConstantElementValue;
import proguard.classfile.attribute.annotation.ParameterAnnotationsAttribute;
import proguard.classfile.attribute.annotation.visitor.AnnotationVisitor;
import proguard.classfile.attribute.annotation.visitor.ElementValueVisitor;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.attribute.visitor.InnerClassesInfoVisitor;
import proguard.classfile.attribute.visitor.LocalVariableInfoVisitor;
import proguard.classfile.attribute.visitor.LocalVariableTypeInfoVisitor;
import proguard.classfile.attribute.visitor.RecordComponentInfoVisitor;
import proguard.classfile.constant.AnyMethodrefConstant;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.DynamicConstant;
import proguard.classfile.constant.FieldrefConstant;
import proguard.classfile.constant.InvokeDynamicConstant;
import proguard.classfile.constant.MethodHandleConstant;
import proguard.classfile.constant.MethodTypeConstant;
import proguard.classfile.constant.StringConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.NamedAttributeDeleter;
import proguard.classfile.kotlin.KotlinAnnotatable;
import proguard.classfile.kotlin.KotlinAnnotation;
import proguard.classfile.kotlin.KotlinAnnotationArgument;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinConstants;
import proguard.classfile.kotlin.KotlinConstructorMetadata;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinFileFacadeKindMetadata;
import proguard.classfile.kotlin.KotlinFunctionMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinMultiFileFacadeKindMetadata;
import proguard.classfile.kotlin.KotlinMultiFilePartKindMetadata;
import proguard.classfile.kotlin.KotlinPropertyMetadata;
import proguard.classfile.kotlin.KotlinSyntheticClassKindMetadata;
import proguard.classfile.kotlin.KotlinTypeAliasMetadata;
import proguard.classfile.kotlin.KotlinTypeMetadata;
import proguard.classfile.kotlin.KotlinTypeParameterMetadata;
import proguard.classfile.kotlin.KotlinValueParameterMetadata;
import proguard.classfile.kotlin.reflect.util.KotlinCallableReferenceInitializer;
import proguard.classfile.kotlin.visitor.AllPropertyVisitor;
import proguard.classfile.kotlin.visitor.AllTypeVisitor;
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor;
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor;
import proguard.classfile.kotlin.visitor.KotlinConstructorVisitor;
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeAliasVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor;
import proguard.classfile.kotlin.visitor.KotlinValueParameterVisitor;
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor;
import proguard.classfile.visitor.ClassNameFilter;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;

import java.util.Collections;
import java.util.stream.Collectors;

import static proguard.classfile.TypeConstants.INNER_CLASS_SEPARATOR;
import static proguard.classfile.kotlin.KotlinAnnotationArgument.EnumValue;
import static proguard.classfile.kotlin.KotlinAnnotationArgument.Value;
import static proguard.classfile.kotlin.KotlinConstants.DEFAULT_IMPLEMENTATIONS_SUFFIX;
import static proguard.classfile.kotlin.KotlinConstants.DEFAULT_METHOD_SUFFIX;
import static proguard.classfile.kotlin.KotlinConstants.FUNCTION_NAME_ANONYMOUS;
import static proguard.classfile.kotlin.KotlinConstants.METHOD_NAME_LAMBDA_INVOKE;

/**
 * This {@link ClassVisitor} initializes the references of all classes that
 * it visits.
 * <p/>
 * All class constant pool entries get direct references to the corresponding
 * classes. These references make it more convenient to travel up and across
 * the class hierarchy.
 * <p/>
 * All field and method reference constant pool entries get direct references
 * to the corresponding classes, fields, and methods.
 * <p/>
 * All name and type constant pool entries get a list of direct references to
 * the classes listed in the type.
 * <p/>
 * The classpools are searched by keys and not by Clazz.getName(), so after
 * obfuscation make sure to pass a ClassPool.refreshedCopy() to this class.
 * <p/>
 * All Kotlin metadata elements get references to their corresponding Java
 * implementation elements.
 * <p/>
 * This visitor optionally prints warnings if some items can't be found.
 * <p/>
 * The class hierarchy must be initialized before using this visitor.
 *
 * @author Eric Lafortune
 */
public class ClassReferenceInitializer
implements   ClassVisitor,

             // Implementation interfaces.
             MemberVisitor,
             ConstantVisitor,
             AttributeVisitor,
             RecordComponentInfoVisitor,
             LocalVariableInfoVisitor,
             LocalVariableTypeInfoVisitor,
             AnnotationVisitor,
             ElementValueVisitor
{
    private final ClassPool      programClassPool;
    private final ClassPool      libraryClassPool;
    private final boolean        checkAccessRules;
    private final WarningPrinter missingClassWarningPrinter;
    private final WarningPrinter missingProgramMemberWarningPrinter;
    private final WarningPrinter missingLibraryMemberWarningPrinter;
    private final WarningPrinter dependencyWarningPrinter;

    private final MemberFinder memberFinder       = new MemberFinder();
    private final MemberFinder strictMemberFinder = new MemberFinder(false);

    private final KotlinReferenceInitializer kotlinReferenceInitializer;


    /**
     * Creates a new ClassReferenceInitializer that initializes the references
     * of all visited class files.
     */
    public ClassReferenceInitializer(ClassPool programClassPool,
                                     ClassPool libraryClassPool)
    {
        this(programClassPool,
             libraryClassPool,
             true);
    }


    /**
     * Creates a new ClassReferenceInitializer that initializes the references
     * of all visited class files.
     */
    public ClassReferenceInitializer(ClassPool programClassPool,
                                     ClassPool libraryClassPool,
                                     boolean   checkAccessRules)
    {
        this(programClassPool,
             libraryClassPool,
             checkAccessRules,
             null,
             null,
             null,
             null);
    }


    /**
     * Creates a new ClassReferenceInitializer that initializes the references
     * of all visited class files, optionally printing warnings if some classes
     * or class members can't be found or if they are in the program class pool.
     */
    public ClassReferenceInitializer(ClassPool      programClassPool,
                                     ClassPool      libraryClassPool,
                                     WarningPrinter missingClassWarningPrinter,
                                     WarningPrinter missingProgramMemberWarningPrinter,
                                     WarningPrinter missingLibraryMemberWarningPrinter,
                                     WarningPrinter dependencyWarningPrinter)
    {
        this(programClassPool,
             libraryClassPool,
             true,
             missingClassWarningPrinter,
             missingProgramMemberWarningPrinter,
             missingLibraryMemberWarningPrinter,
             dependencyWarningPrinter);
    }


    /**
     * Creates a new ClassReferenceInitializer that initializes the references
     * of all visited class files, optionally printing warnings if some classes
     * or class members can't be found or if they are in the program class pool.
     */
    public ClassReferenceInitializer(ClassPool      programClassPool,
                                     ClassPool      libraryClassPool,
                                     boolean        checkAccessRules,
                                     WarningPrinter missingClassWarningPrinter,
                                     WarningPrinter missingProgramMemberWarningPrinter,
                                     WarningPrinter missingLibraryMemberWarningPrinter,
                                     WarningPrinter dependencyWarningPrinter)
    {
        this.programClassPool                   = programClassPool;
        this.libraryClassPool                   = libraryClassPool;
        this.checkAccessRules                   = checkAccessRules;
        this.missingClassWarningPrinter         = missingClassWarningPrinter;
        this.missingProgramMemberWarningPrinter = missingProgramMemberWarningPrinter;
        this.missingLibraryMemberWarningPrinter = missingLibraryMemberWarningPrinter;
        this.dependencyWarningPrinter           = dependencyWarningPrinter;
        this.kotlinReferenceInitializer         = new KotlinReferenceInitializer();
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support " + clazz.getClass().getName());
    }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        // Initialize the constant pool entries.
        programClass.constantPoolEntriesAccept(this);

        // Initialize all fields and methods.
        programClass.fieldsAccept(this);
        programClass.methodsAccept(this);

        // Initialize the attributes.
        programClass.attributesAccept(this);

        // Initialize the Kotlin metadata.
        programClass.kotlinMetadataAccept(kotlinReferenceInitializer);
    }


    @Override
    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Initialize all fields and methods.
        libraryClass.fieldsAccept(this);
        libraryClass.methodsAccept(this);

        // Initialize the Kotlin metadata.
        libraryClass.kotlinMetadataAccept(kotlinReferenceInitializer);
    }


    // Implementations for MemberVisitor.

    @Override
    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        programField.referencedClass =
            findReferencedClass(programClass,
                                programField.getDescriptor(programClass));

        // Initialize the attributes.
        programField.attributesAccept(programClass, this);
    }

    @Override
    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programMethod.referencedClasses =
            findReferencedClasses(programClass,
                                  programMethod.getDescriptor(programClass));

        // Initialize the attributes.
        programMethod.attributesAccept(programClass, this);
    }

    @Override
    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        libraryField.referencedClass =
            findReferencedClass(libraryClass,
                                libraryField.getDescriptor(libraryClass));
    }

    @Override
    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        libraryMethod.referencedClasses =
            findReferencedClasses(libraryClass,
                                  libraryMethod.getDescriptor(libraryClass));
    }


    // Implementations for ConstantVisitor.

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {}

    @Override
    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        // Fill out the String class.
        stringConstant.javaLangStringClass =
            findClass(clazz, ClassConstants.NAME_JAVA_LANG_STRING);
    }

    @Override
    public void visitDynamicConstant(Clazz clazz, DynamicConstant dynamicConstant)
    {
        dynamicConstant.referencedClasses =
            findReferencedClasses(clazz,
                                  dynamicConstant.getType(clazz));
    }

    @Override
    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        invokeDynamicConstant.referencedClasses =
            findReferencedClasses(clazz,
                                  invokeDynamicConstant.getType(clazz));
    }

    @Override
    public void visitMethodHandleConstant(Clazz clazz, MethodHandleConstant methodHandleConstant)
    {
        // Fill out the MethodHandle class.
        methodHandleConstant.javaLangInvokeMethodHandleClass =
            findClass(clazz, ClassConstants.NAME_JAVA_LANG_INVOKE_METHOD_HANDLE);
    }

    @Override
    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        String className = fieldrefConstant.getClassName(clazz);

        // Methods for array types should be found in the Object class.
        if (ClassUtil.isInternalArrayType(className))
        {
            className = ClassConstants.NAME_JAVA_LANG_OBJECT;
        }

        // See if we can find the referenced class.
        // Unresolved references are assumed to refer to library classes
        // that will not change anyway.
        Clazz referencedClass = findClass(clazz, className);

        if (referencedClass != null)
        {
            String name = fieldrefConstant.getName(clazz);
            String type = fieldrefConstant.getType(clazz);

            // See if we can find the referenced class member somewhere in the
            // hierarchy.
            Clazz referencingClass = checkAccessRules ? clazz : null;

            fieldrefConstant.referencedField = memberFinder.findField(referencingClass,
                                                                      referencedClass,
                                                                      name,
                                                                      type);
            fieldrefConstant.referencedClass = memberFinder.correspondingClass();

            if (fieldrefConstant.referencedField == null)
            {
                // We haven't found the class member anywhere in the hierarchy.
                boolean isProgramClass = referencedClass instanceof ProgramClass;

                WarningPrinter missingMemberWarningPrinter = isProgramClass ?
                    missingProgramMemberWarningPrinter :
                    missingLibraryMemberWarningPrinter;

                if (missingMemberWarningPrinter != null)
                {
                    missingMemberWarningPrinter.print(clazz.getName(),
                                                      className,
                                                      "Warning: " +
                                                      ClassUtil.externalClassName(clazz.getName()) +
                                                      ": can't find referenced field '"  +
                                                      ClassUtil.externalFullFieldDescription(0, name, type) +
                                                      "' in " +
                                                      (isProgramClass ?
                                                          "program" :
                                                          "library") +
                                                      " class " +
                                                      ClassUtil.externalClassName(className));
                }
            }
        }
    }

    @Override
    public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant)
    {
        String className = anyMethodrefConstant.getClassName(clazz);

        // Methods for array types should be found in the Object class.
        if (ClassUtil.isInternalArrayType(className))
        {
            className = ClassConstants.NAME_JAVA_LANG_OBJECT;
        }

        // See if we can find the referenced class.
        // Unresolved references are assumed to refer to library classes
        // that will not change anyway.
        Clazz referencedClass = findClass(clazz, className);

        if (referencedClass != null)
        {
            String name = anyMethodrefConstant.getName(clazz);
            String type = anyMethodrefConstant.getType(clazz);

            boolean isFieldRef = anyMethodrefConstant.getTag() == Constant.FIELDREF;

            // See if we can find the referenced class member somewhere in the
            // hierarchy.
            Clazz referencingClass = checkAccessRules ? clazz : null;

            anyMethodrefConstant.referencedMethod = memberFinder.findMethod(referencingClass,
                                                                            referencedClass,
                                                                            name,
                                                                            type);
            anyMethodrefConstant.referencedClass  = memberFinder.correspondingClass();

            if (anyMethodrefConstant.referencedMethod == null)
            {
                // We haven't found the class member anywhere in the hierarchy.
                boolean isProgramClass = referencedClass instanceof ProgramClass;

                WarningPrinter missingMemberWarningPrinter = isProgramClass ?
                    missingProgramMemberWarningPrinter :
                    missingLibraryMemberWarningPrinter;

                if (missingMemberWarningPrinter != null)
                {
                    missingMemberWarningPrinter.print(clazz.getName(),
                                                      className,
                                                      "Warning: " +
                                                      ClassUtil.externalClassName(clazz.getName()) +
                                                      ": can't find referenced method '" +
                                                      ClassUtil.externalFullMethodDescription(className, 0, name, type) +
                                                      "' in " +
                                                      (isProgramClass ?
                                                          "program" :
                                                          "library") +
                                                      " class " +
                                                      ClassUtil.externalClassName(className));
                }
            }
        }
    }

    @Override
    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Fill out the referenced class.
        classConstant.referencedClass =
            findClass(clazz, ClassUtil.internalClassNameFromClassType(classConstant.getName(clazz)));

        // Fill out the Class class.
        classConstant.javaLangClassClass =
            findClass(clazz, ClassConstants.NAME_JAVA_LANG_CLASS);
    }

    @Override
    public void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant methodTypeConstant)
    {
        // Fill out the MethodType class.
        methodTypeConstant.javaLangInvokeMethodTypeClass =
            findClass(clazz, ClassConstants.NAME_JAVA_LANG_INVOKE_METHOD_TYPE);

        methodTypeConstant.referencedClasses =
            findReferencedClasses(clazz,
                                  methodTypeConstant.getType(clazz));
    }


    // Implementations for AttributeVisitor.

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

    @Override
    public void visitRecordAttribute(Clazz clazz, RecordAttribute recordAttributeAttribute)
    {
        recordAttributeAttribute.componentsAccept(clazz, this);
    }

    @Override
    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        String enclosingClassName = enclosingMethodAttribute.getClassName(clazz);

        // See if we can find the referenced class.
        enclosingMethodAttribute.referencedClass =
            findClass(clazz, enclosingClassName);

        if (enclosingMethodAttribute.referencedClass != null)
        {
            // Is there an enclosing method? Otherwise it's just initialization
            // code outside of the constructors.
            if (enclosingMethodAttribute.u2nameAndTypeIndex != 0)
            {
                String name = enclosingMethodAttribute.getName(clazz);
                String type = enclosingMethodAttribute.getType(clazz);

                // See if we can find the method in the referenced class.
                enclosingMethodAttribute.referencedMethod =
                    enclosingMethodAttribute.referencedClass.findMethod(name, type);

                if (enclosingMethodAttribute.referencedMethod == null &&
                    missingProgramMemberWarningPrinter != null)
                {
                    // We couldn't find the enclosing method.
                    String className = clazz.getName();

                    missingProgramMemberWarningPrinter.print(className,
                                                             enclosingClassName,
                                                             "Warning: " +
                                                             ClassUtil.externalClassName(className) +
                                                             ": can't find enclosing method '" +
                                                             ClassUtil.externalFullMethodDescription(enclosingClassName, 0, name, type) +
                                                             "' in program class " +
                                                             ClassUtil.externalClassName(enclosingClassName));
                }
            }
        }
        else
        {
            if (enclosingClassName                                != null &&
                enclosingClassName.indexOf(INNER_CLASS_SEPARATOR) != -1)
            {
                String myEnclosingClassName = enclosingClassName;

                do
                {
                    // [DGD-1462] Truncate the name until we find the first
                    // non-anonymous class, i.e. the first class that exists in
                    // a class pool.
                    myEnclosingClassName = myEnclosingClassName.substring(0, myEnclosingClassName.lastIndexOf("$"));

                    enclosingMethodAttribute.referencedClass =
                        findClass(clazz, myEnclosingClassName, false);
                }
                while (enclosingMethodAttribute.referencedClass            == null &&
                       myEnclosingClassName.indexOf(INNER_CLASS_SEPARATOR) != -1);

                if (enclosingMethodAttribute.referencedClass == null &&
                         clazz != null &&
                         missingClassWarningPrinter != null)
                {
                    reportMissingClass(clazz.getName(), enclosingClassName);
                }
            }
        }
    }

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Initialize the nested attributes.
        codeAttribute.attributesAccept(clazz, method, this);
    }

    @Override
    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Initialize the local variables.
        localVariableTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }

    @Override
    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Initialize the local variable types.
        localVariableTypeTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }


    @Override
    public void visitSignatureAttribute(Clazz clazz, Member member, SignatureAttribute signatureAttribute)
    {
        try
        {
            signatureAttribute.referencedClasses =
                findReferencedClasses(clazz,
                                      signatureAttribute.getSignature(clazz));
        }
        catch (Exception corruptSignature)
        {
            // #2468: delete corrupt signature attributes, since they
            // cannot be otherwise worked around.
            member.accept(clazz, new NamedAttributeDeleter(Attribute.SIGNATURE));
        }
    }

    @Override
    public void visitSignatureAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, SignatureAttribute signatureAttribute)
    {
        try
        {
            signatureAttribute.referencedClasses =
                findReferencedClasses(clazz,
                                      signatureAttribute.getSignature(clazz));
        }
        catch (Exception corruptSignature)
        {
            // #2468: delete corrupt signature attributes, since they
            // cannot be otherwise worked around.
            recordComponentInfo.attributesAccept(clazz, new NamedAttributeDeleter(Attribute.SIGNATURE));
        }
    }

    @Override
    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        if (isValidClassSignature(clazz, signatureAttribute.getSignature(clazz)))
        {
            signatureAttribute.referencedClasses =
                findReferencedClasses(clazz,
                                      signatureAttribute.getSignature(clazz));
        }
        else
        {
            clazz.accept(new NamedAttributeDeleter(Attribute.SIGNATURE));
        }
    }

    @Override
    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        // Initialize the annotations.
        annotationsAttribute.annotationsAccept(clazz, this);
    }

    @Override
    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        // Initialize the annotations.
        parameterAnnotationsAttribute.annotationsAccept(clazz, method, this);
    }

    @Override
    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        // Initialize the annotation.
        annotationDefaultAttribute.defaultValueAccept(clazz, this);
    }


    // Implementations for RecordComponentInfoVisitor.

    @Override
    public void visitRecordComponentInfo(Clazz clazz, RecordComponentInfo recordComponentInfo)
    {
        String name       = recordComponentInfo.getName(clazz);
        String descriptor = recordComponentInfo.getDescriptor(clazz);

        recordComponentInfo.referencedField = memberFinder.findField(clazz,
                                                                     name,
                                                                     descriptor);

        // Initialize the attributes.
        recordComponentInfo.attributesAccept(clazz, this);
    }


    // Implementations for LocalVariableInfoVisitor.

    @Override
    public void visitLocalVariableInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableInfo localVariableInfo)
    {
        localVariableInfo.referencedClass =
            findReferencedClass(clazz,
                                localVariableInfo.getDescriptor(clazz));
    }


    // Implementations for LocalVariableTypeInfoVisitor.

    @Override
    public void visitLocalVariableTypeInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeInfo localVariableTypeInfo)
    {
        localVariableTypeInfo.referencedClasses =
            findReferencedClasses(clazz,
                                  localVariableTypeInfo.getSignature(clazz));
    }


    // Implementations for AnnotationVisitor.

    @Override
    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        annotation.referencedClasses =
            findReferencedClasses(clazz,
                                  annotation.getType(clazz));

        // Initialize the element values.
        annotation.elementValuesAccept(clazz, this);
    }


    // Implementations for ElementValueVisitor.

    @Override
    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
        initializeElementValue(clazz, annotation, constantElementValue);
    }

    @Override
    public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        initializeElementValue(clazz, annotation, enumConstantElementValue);

        enumConstantElementValue.referencedClasses =
            findReferencedClasses(clazz,
                                  enumConstantElementValue.getTypeName(clazz));
    }

    @Override
    public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        initializeElementValue(clazz, annotation, classElementValue);

        classElementValue.referencedClasses =
            findReferencedClasses(clazz,
                                  classElementValue.getClassName(clazz));
    }

    @Override
    public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        initializeElementValue(clazz, annotation, annotationElementValue);

        // Initialize the annotation.
        annotationElementValue.annotationAccept(clazz, this);
    }

    @Override
    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        initializeElementValue(clazz, annotation, arrayElementValue);

        // Initialize the element values.
        arrayElementValue.elementValuesAccept(clazz, annotation, this);
    }

    /**
     * Initializes the referenced method of an element value, if any.
     */
    private void initializeElementValue(Clazz clazz, Annotation annotation, ElementValue elementValue)
    {
        // See if we have a referenced class.
        if (annotation                      != null &&
            annotation.referencedClasses    != null &&
            elementValue.u2elementNameIndex != 0)
        {
            // See if we can find the method in the referenced class
            // (ignoring the descriptor).
            String name = elementValue.getMethodName(clazz);

            Clazz referencedClass = annotation.referencedClasses[0];
            elementValue.referencedClass  = referencedClass;
            elementValue.referencedMethod = referencedClass.findMethod(name, null);
        }
    }

    // Kotlin initializer class, used to initialize Kotlin metadata references.

    private class KotlinReferenceInitializer
    implements    KotlinMetadataVisitor,
                  KotlinPropertyVisitor,
                  KotlinFunctionVisitor,
                  KotlinConstructorVisitor,
                  KotlinTypeVisitor,
                  KotlinTypeAliasVisitor,
                  KotlinValueParameterVisitor,
                  KotlinTypeParameterVisitor,
                  KotlinAnnotationVisitor,
                  KotlinAnnotationArgumentVisitor
    {
        private final KotlinDefaultImplsInitializer                kotlinDefaultImplsInitializer          = new KotlinDefaultImplsInitializer();
        private final KotlinDefaultMethodInitializer               kotlinDefaultMethodInitializer         = new KotlinDefaultMethodInitializer();
        private final KotlinInterClassSyntheticFunctionInitializer interClassSyntheticFunctionInitializer = new KotlinInterClassSyntheticFunctionInitializer();
        private final KotlinCallableReferenceInitializer           callableReferenceInitializer           = new KotlinCallableReferenceInitializer(programClassPool, libraryClassPool);

        // Initialize lazily, since the copy will only be required if there are type aliases.
        private ClassPool programClassPoolCopy;

        private ClassPool getProgramClassPoolCopy()
        {
            if (programClassPoolCopy == null)
            {
                programClassPoolCopy = programClassPool.refreshedCopy();
            }

            return programClassPoolCopy;
        }

        // Implementations for KotlinMetadataVisitor.

        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                            KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
        {
            kotlinDeclarationContainerMetadata.ownerReferencedClass = clazz;

            kotlinDeclarationContainerMetadata.propertiesAccept(         clazz, this);
            kotlinDeclarationContainerMetadata.delegatedPropertiesAccept(clazz, this);
            kotlinDeclarationContainerMetadata.functionsAccept(          clazz, this);
            kotlinDeclarationContainerMetadata.typeAliasesAccept(        clazz,this);
        }


        @Override
        public void visitKotlinClassMetadata(Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata)
        {
            kotlinClassKindMetadata.referencedClass = findClass(clazz, kotlinClassKindMetadata.className);

            if (kotlinClassKindMetadata.anonymousObjectOriginName != null)
            {
                kotlinClassKindMetadata.anonymousObjectOriginClass =
                    findClass(clazz, kotlinClassKindMetadata.anonymousObjectOriginName, false);

                if (kotlinClassKindMetadata.anonymousObjectOriginClass == null)
                {
                    kotlinClassKindMetadata.anonymousObjectOriginName = null;
                }
            }

            if (kotlinClassKindMetadata.companionObjectName != null)
            {
                String name = clazz.getName() + "$" + kotlinClassKindMetadata.companionObjectName;
                kotlinClassKindMetadata.referencedCompanionClass = findClass(clazz, name);
                kotlinClassKindMetadata.referencedCompanionField = memberFinder.findField(clazz,
                                                                                          kotlinClassKindMetadata.companionObjectName,
                                                                                          ClassUtil.internalTypeFromClassName(name));
            }

            kotlinClassKindMetadata.referencedEnumEntries =
                kotlinClassKindMetadata.enumEntryNames
                    .stream()
                    .map(enumEntry ->
                         strictMemberFinder.findField(clazz, enumEntry, null))
                    .collect(Collectors.toList());

            kotlinClassKindMetadata.referencedNestedClasses =
                kotlinClassKindMetadata.nestedClassNames
                    .stream()
                    .map(nestedName ->
                         findClass(clazz, clazz.getName() + "$" + nestedName))
                    .collect(Collectors.toList());

            kotlinClassKindMetadata.referencedSealedSubClasses=
                kotlinClassKindMetadata.sealedSubclassNames
                    .stream()
                    .map(sealedSubName ->
                         findClass(clazz, sealedSubName))
                    .collect(Collectors.toList());


            kotlinClassKindMetadata.typeParametersAccept(                   clazz, this);
            kotlinClassKindMetadata.contextReceiverTypesAccept(             clazz, this);
            kotlinClassKindMetadata.superTypesAccept(                       clazz, this);
            kotlinClassKindMetadata.constructorsAccept(                     clazz, this);
            kotlinClassKindMetadata.inlineClassUnderlyingPropertyTypeAccept(clazz, this);

            visitKotlinDeclarationContainerMetadata(clazz, kotlinClassKindMetadata);

            if (kotlinClassKindMetadata.flags.isInterface || kotlinClassKindMetadata.flags.isAnnotationClass)
            {
                // Initialize the default implementations class of interfaces.
                // The class will be an inner class and have a name like MyInterface$DefaultImpls.
                kotlinClassKindMetadata.referencedDefaultImplsClass =
                    findClass(clazz,
                        kotlinClassKindMetadata.className +
                        KotlinConstants.DEFAULT_IMPLEMENTATIONS_SUFFIX,
                        false
                    );

                if (kotlinClassKindMetadata.referencedDefaultImplsClass != null)
                {
                    // Initialize references from interface functions to
                    // their default implementations.
                    kotlinDefaultImplsInitializer.defaultImplsClass = kotlinClassKindMetadata.referencedDefaultImplsClass;
                    kotlinClassKindMetadata.functionsAccept(clazz, kotlinDefaultImplsInitializer);

                    // Initialize missing references from interface properties
                    // to their backing field.
                    kotlinClassKindMetadata.accept(clazz,
                                                   new AllPropertyVisitor(
                                                   new KotlinInterClassPropertyReferenceInitializer(
                                                       kotlinClassKindMetadata.referencedDefaultImplsClass)));
                }
            }

            // Initialize references from functions to their utility methods
            // for default parameter values.
            kotlinDefaultMethodInitializer.isInterface = kotlinClassKindMetadata.flags.isInterface;
            kotlinClassKindMetadata.functionsAccept(clazz, kotlinDefaultMethodInitializer);

            if (kotlinClassKindMetadata.flags.isCompanionObject)
            {
                // Initialize missing references from properties in the Companion class
                // that have their backing field on the parent class.
                clazz.accept(new KotlinCompanionParentPropertyInitializer());
            }
        }

        @Override
        public void visitKotlinFileFacadeMetadata(Clazz clazz, KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata)
        {
            visitKotlinDeclarationContainerMetadata(clazz, kotlinFileFacadeKindMetadata);

            // Initialize references from functions to their utility methods
            // for default parameter values.
            kotlinDefaultMethodInitializer.isInterface = false;
            kotlinFileFacadeKindMetadata.functionsAccept(clazz, kotlinDefaultMethodInitializer);
        }

        @Override
        public void visitKotlinSyntheticClassMetadata(Clazz clazz, KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata)
        {
            kotlinSyntheticClassKindMetadata.functionsAccept(clazz, this);

            // Initialize references from functions to their utility methods
            // for default parameter values.
            kotlinDefaultMethodInitializer.isInterface = false;
            kotlinSyntheticClassKindMetadata.functionsAccept(clazz, kotlinDefaultMethodInitializer);

            // Initialize missing references from synthetic functions by
            // searching in the enclosing method attribute.
            kotlinSyntheticClassKindMetadata.functionsAccept(clazz, interClassSyntheticFunctionInitializer);

            // Initialize callable references.
            kotlinSyntheticClassKindMetadata.accept(clazz, callableReferenceInitializer);
        }

        @Override
        public void visitKotlinMultiFileFacadeMetadata(Clazz clazz, KotlinMultiFileFacadeKindMetadata kotlinMultiFileFacadeKindMetadata)
        {
            kotlinMultiFileFacadeKindMetadata.referencedPartClasses =
                kotlinMultiFileFacadeKindMetadata.partClassNames
                    .stream()
                    .map(partName ->
                         findClass(clazz, partName))
                    .collect(Collectors.toList());
        }

        @Override
        public void visitKotlinMultiFilePartMetadata(Clazz clazz, KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata)
        {
            kotlinMultiFilePartKindMetadata.referencedFacadeClass = findClass(clazz,
                                                                              kotlinMultiFilePartKindMetadata.facadeName);

            visitKotlinDeclarationContainerMetadata(clazz, kotlinMultiFilePartKindMetadata);

            // Initialize references from functions to their utility methods
            // for default parameter values.
            kotlinDefaultMethodInitializer.isInterface = false;
            kotlinMultiFilePartKindMetadata.functionsAccept(clazz, kotlinDefaultMethodInitializer);

            if (kotlinMultiFilePartKindMetadata.referencedFacadeClass != null)
            {
                // Initialize missing references from properties in multi-file parts
                // that have their backing field on the multi-file facade class.
                kotlinMultiFilePartKindMetadata.accept(clazz,
                                                       new AllPropertyVisitor(
                                                       new KotlinInterClassPropertyReferenceInitializer(kotlinMultiFilePartKindMetadata.referencedFacadeClass)));
            }
        }

        // Implementations for KotlinPropertyVisitor.

        @Override
        public void visitAnyProperty(Clazz                              clazz,
                                     KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                     KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            if (kotlinPropertyMetadata.backingFieldSignature != null)
            {
                kotlinPropertyMetadata.referencedBackingField =
                    strictMemberFinder.findField(clazz,
                                                 kotlinPropertyMetadata.backingFieldSignature.memberName,
                                                 kotlinPropertyMetadata.backingFieldSignature.descriptor);

                kotlinPropertyMetadata.referencedBackingFieldClass = strictMemberFinder.correspondingClass();
            }

            if (kotlinPropertyMetadata.getterSignature != null)
            {
                kotlinPropertyMetadata.referencedGetterMethod =
                    strictMemberFinder.findMethod(clazz,
                                                  kotlinPropertyMetadata.getterSignature.method,
                                                  kotlinPropertyMetadata.getterSignature.descriptor.toString());
            }

            if (kotlinPropertyMetadata.setterSignature != null)
            {
                kotlinPropertyMetadata.referencedSetterMethod =
                    strictMemberFinder.findMethod(clazz,
                                                  kotlinPropertyMetadata.setterSignature.method,
                                                  kotlinPropertyMetadata.setterSignature.descriptor.toString());
            }

            if (kotlinPropertyMetadata.syntheticMethodForAnnotations != null)
            {
                kotlinPropertyMetadata.referencedSyntheticMethodForAnnotations =
                    strictMemberFinder.findMethod(clazz,
                                                  kotlinPropertyMetadata.syntheticMethodForAnnotations.method,
                                                  kotlinPropertyMetadata.syntheticMethodForAnnotations.descriptor.toString());

                kotlinPropertyMetadata.referencedSyntheticMethodClass = strictMemberFinder.correspondingClass();
            }

            if (kotlinPropertyMetadata.syntheticMethodForDelegate != null)
            {
                kotlinPropertyMetadata.referencedSyntheticMethodForDelegateMethod =
                        strictMemberFinder.findMethod(clazz,
                                                      kotlinPropertyMetadata.syntheticMethodForDelegate.method,
                                                      kotlinPropertyMetadata.syntheticMethodForDelegate.descriptor.toString());

                kotlinPropertyMetadata.referencedSyntheticMethodForDelegateClass = strictMemberFinder.correspondingClass();
            }

            kotlinPropertyMetadata.typeParametersAccept(      clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.receiverTypeAccept(        clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.contextReceiverTypesAccept(clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.typeAccept(                clazz, kotlinDeclarationContainerMetadata, this);
            kotlinPropertyMetadata.setterParametersAccept(    clazz, kotlinDeclarationContainerMetadata, this);
        }


        // Implementations for KotlinFunctionVisitor.

        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata)
        {
            kotlinFunctionMetadata.referencedMethodClass = clazz;

            if (kotlinFunctionMetadata.jvmSignature != null)
            {
                String method = !FUNCTION_NAME_ANONYMOUS.equals(kotlinFunctionMetadata.jvmSignature.method) ?
                        kotlinFunctionMetadata.jvmSignature.method :
                        // T16483: In some cases, the jvmSignature erroneously contains the name <anonymous> instead of invoke
                        METHOD_NAME_LAMBDA_INVOKE;

                kotlinFunctionMetadata.referencedMethod =
                    strictMemberFinder.findMethod(kotlinFunctionMetadata.referencedMethodClass,
                                                  method,
                                                  kotlinFunctionMetadata.jvmSignature.descriptor.toString());
            }

            if (kotlinFunctionMetadata.lambdaClassOriginName != null)
            {
                kotlinFunctionMetadata.referencedLambdaClassOrigin =
                    findClass(clazz, kotlinFunctionMetadata.lambdaClassOriginName, false);

                if (kotlinFunctionMetadata.referencedLambdaClassOrigin == null)
                {
                    kotlinFunctionMetadata.lambdaClassOriginName = null;
                }
            }

            kotlinFunctionMetadata.contractsAccept(           clazz, kotlinMetadata, new AllTypeVisitor(this));
            kotlinFunctionMetadata.typeParametersAccept(      clazz, kotlinMetadata, this);
            kotlinFunctionMetadata.receiverTypeAccept(        clazz, kotlinMetadata, this);
            kotlinFunctionMetadata.contextReceiverTypesAccept(clazz, kotlinMetadata, this);
            kotlinFunctionMetadata.valueParametersAccept(     clazz, kotlinMetadata, this);
            kotlinFunctionMetadata.returnTypeAccept(          clazz, kotlinMetadata, this);
        }


        // Implementations for KotlinConstructorVisitor.

        @Override
        public void visitConstructor(Clazz                     clazz,
                                     KotlinClassKindMetadata   kotlinClassKindMetadata,
                                     KotlinConstructorMetadata kotlinConstructorMetadata)
        {
            // Annotation constructors don't have a corresponding constructor method.
            if (kotlinConstructorMetadata.jvmSignature != null)
            {
                kotlinConstructorMetadata.referencedMethod =
                    strictMemberFinder.findMethod(clazz,
                                                  kotlinConstructorMetadata.jvmSignature.method,
                                                  kotlinConstructorMetadata.jvmSignature.descriptor.toString());
            }

            kotlinConstructorMetadata.valueParametersAccept(clazz, kotlinClassKindMetadata, this);
        }


        // Implementations for KotlinTypeVisitor.

        @Override
        public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata)
        {
            String className = kotlinTypeMetadata.className;

            if (className != null)
            {
                kotlinTypeMetadata.referencedClass = findKotlinClass(clazz, className);
            }
            else if (kotlinTypeMetadata.aliasName != null)
            {
                // This type is an alias, that refers to an actual type (or another alias).
                // We search for the alias definition to create a reference for this use.

                String aliasName = kotlinTypeMetadata.aliasName;

                int innerClassMarkerIndex = aliasName.lastIndexOf('.');
                String classNameFilter, simpleName;

                if (innerClassMarkerIndex != -1)
                {
                    // Declared inside a class - we know exactly which one.
                    classNameFilter = aliasName.substring(0, innerClassMarkerIndex);
                    simpleName      = aliasName.substring(innerClassMarkerIndex + 1);
                }
                else
                {
                    // Declared in a file facade - we know which package only.
                    classNameFilter = ClassUtil.internalPackagePrefix(aliasName) + "*";
                    simpleName      = ClassUtil.internalSimpleClassName(kotlinTypeMetadata.aliasName);
                }

                ClassVisitor typeAliasInitializer =
                    new ReferencedKotlinMetadataVisitor(
                    new KotlinTypeAliasReferenceInitializer(kotlinTypeMetadata, simpleName));

                // Use a refreshed copy of the class pool, in case any classes have been renamed.
                ClassPool programClassPool = getProgramClassPoolCopy();
                if (innerClassMarkerIndex != -1)
                {
                    programClassPool.classAccept(classNameFilter, typeAliasInitializer);
                    libraryClassPool.classAccept(classNameFilter, typeAliasInitializer);
                }
                else
                {
                    programClassPool.classesAccept(classNameFilter, typeAliasInitializer);
                    libraryClassPool.classesAccept(classNameFilter, typeAliasInitializer);
                }
            }

            kotlinTypeMetadata.annotationsAccept(  clazz, this);
            kotlinTypeMetadata.typeArgumentsAccept(clazz, this);
            kotlinTypeMetadata.outerClassAccept(   clazz, this);
            kotlinTypeMetadata.upperBoundsAccept(  clazz, this);
            kotlinTypeMetadata.abbreviationAccept( clazz, this);
        }


        // Implementations for KotlinTypeAliasVisitor.

        @Override
        public void visitTypeAlias(Clazz                              clazz,
                                   KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                   KotlinTypeAliasMetadata            kotlinTypeAliasMetadata)
        {
            kotlinTypeAliasMetadata.referencedDeclarationContainer = kotlinDeclarationContainerMetadata;

            kotlinTypeAliasMetadata.annotationsAccept(   clazz, this);
            kotlinTypeAliasMetadata.underlyingTypeAccept(clazz, kotlinDeclarationContainerMetadata, this);
            kotlinTypeAliasMetadata.expandedTypeAccept(  clazz, kotlinDeclarationContainerMetadata, this);
            kotlinTypeAliasMetadata.typeParametersAccept(clazz, kotlinDeclarationContainerMetadata, this);
        }


        // Implementations for KotlinValueParameterVisitor.

        @Override
        public void visitAnyValueParameter(Clazz                        clazz,
                                           KotlinValueParameterMetadata kotlinValueParameterMetadata) {}

        @Override
        public void visitFunctionValParameter(Clazz                        clazz,
                                              KotlinMetadata               kotlinMetadata,
                                              KotlinFunctionMetadata       kotlinFunctionMetadata,
                                              KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinMetadata, kotlinFunctionMetadata, this);
        }

        @Override
        public void visitConstructorValParameter(Clazz                        clazz,
                                                 KotlinClassKindMetadata      kotlinClassKindMetadata,
                                                 KotlinConstructorMetadata    kotlinConstructorMetadata,
                                                 KotlinValueParameterMetadata kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinClassKindMetadata, kotlinConstructorMetadata, this);
        }

        @Override
        public void visitPropertyValParameter(Clazz                              clazz,
                                              KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                              KotlinPropertyMetadata             kotlinPropertyMetadata,
                                              KotlinValueParameterMetadata       kotlinValueParameterMetadata)
        {
            kotlinValueParameterMetadata.typeAccept(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata, this);
        }


        // Implementations for KotlinTypeParameterVisitor.

        @Override
        public void visitAnyTypeParameter(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
        {
            kotlinTypeParameterMetadata.annotationsAccept(clazz, this);
            kotlinTypeParameterMetadata.upperBoundsAccept(clazz, this);
        }

        // Implementations for KotlinMetadataAnnotationVisitor

        @Override
        public void visitAnyAnnotation(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation)
        {
            annotation.referencedAnnotationClass = findClass(clazz, annotation.className);
            if (annotation.referencedAnnotationClass != null)
            {
                annotation.argumentsAccept(clazz, annotatable, this);
            }
        }

        // Implementations for KotlinMetadataAnnotationArgumentVisitor

        @Override
        public void visitAnyArgument(Clazz                    clazz,
                                     KotlinAnnotatable        annotatable,
                                     KotlinAnnotation         annotation,
                                     KotlinAnnotationArgument argument,
                                     Value                    value)
        {
            argument.referencedAnnotationMethodClass =
                    annotation.referencedAnnotationClass;
            argument.referencedAnnotationMethod =
                    strictMemberFinder.findMethod(
                            annotation.referencedAnnotationClass,
                            argument.name,
                            null);
        }

        @Override
        public void visitClassArgument(Clazz                               clazz,
                                       KotlinAnnotatable                   annotatable,
                                       KotlinAnnotation                    annotation,
                                       KotlinAnnotationArgument            argument,
                                       KotlinAnnotationArgument.ClassValue value)
        {
            this.visitAnyArgument(clazz, annotatable, annotation, argument, value);

            value.referencedClass = findKotlinClass(clazz, value.className);
        }

        @Override
        public void visitEnumArgument(Clazz                    clazz,
                                      KotlinAnnotatable        annotatable,
                                      KotlinAnnotation         annotation,
                                      KotlinAnnotationArgument argument,
                                      EnumValue                value)
        {
            this.visitAnyArgument(clazz, annotatable, annotation, argument, value);

            value.referencedClass = findClass(clazz, value.className);
        }
    }


    // Small utility methods.

    /**
     * Returns the single class referenced by the given descriptor, or
     * <code>null</code> if there isn't any useful reference.
     */
    private Clazz findReferencedClass(Clazz  referencingClass,
                                      String descriptor)
    {
        DescriptorClassEnumeration enumeration =
            new DescriptorClassEnumeration(descriptor);

        enumeration.nextFluff();

        if (enumeration.hasMoreClassNames())
        {
            return findClass(referencingClass, enumeration.nextClassName());
        }

        return null;
    }


    /**
     * Returns an array of classes referenced by the given descriptor, or
     * <code>null</code> if there aren't any useful references.
     */
    private Clazz[] findReferencedClasses(Clazz  referencingClass,
                                          String descriptor)
    {
        DescriptorClassEnumeration enumeration =
            new DescriptorClassEnumeration(descriptor);

        int classCount = enumeration.classCount();
        if (classCount > 0)
        {
            Clazz[] referencedClasses = new Clazz[classCount];

            boolean foundReferencedClasses = false;

            for (int index = 0; index < classCount; index++)
            {
                String fluff = enumeration.nextFluff();
                String name  = enumeration.nextClassName();

                Clazz referencedClass = findClass(referencingClass, name);

                if (referencedClass != null)
                {
                    referencedClasses[index] = referencedClass;
                    foundReferencedClasses = true;
                }
            }

            if (foundReferencedClasses)
            {
                return referencedClasses;
            }
        }

        return null;
    }


    /**
     * Returns the class with the given name, either for the dummy Kotlin class pool,
     * program class pool or from the library class pool, or <code>null</code> if it can't be found.
     */
    private Clazz findKotlinClass(Clazz referencingClass, String name)
    {
        // If this is a dummy Kotlin type, assign a dummy referenced class.
        Clazz clazz = KotlinConstants.dummyClassPool.getClass(name);

        if (clazz == null)
        {
            clazz = findClass(referencingClass, name);
        }

        return clazz;
    }


    /**
     * Returns the class with the given name, either for the program class pool
     * or from the library class pool, or <code>null</code> if it can't be found.
     */
    private Clazz findClass(Clazz referencingClass, String name)
    {
        return findClass(referencingClass, name, true);
    }


    /**
     * @param report Report if the class is not found. Set to false if the class
     *               doesn't necessarily exist.
     */
    private Clazz findClass(Clazz referencingClass, String name, boolean report)
    {
        // Is it an array type?
        if (ClassUtil.isInternalArrayType(name))
        {
            // Ignore any primitive array types.
            if (!ClassUtil.isInternalClassType(name))
            {
                return null;
            }

            // Strip the array part.
            name = ClassUtil.internalClassNameFromClassType(name);
        }

        // First look for the class in the program class pool.
        Clazz clazz = programClassPool.getClass(name);

        // Otherwise look for the class in the library class pool.
        if (clazz == null)
        {
            clazz = libraryClassPool.getClass(name);

            if (report &&
                clazz == null &&
                missingClassWarningPrinter != null)
            {
                // We didn't find the superclass or interface. Print a warning.
                reportMissingClass(referencingClass.getName(), name);
            }
        }
        else if (dependencyWarningPrinter != null)
        {
            // The superclass or interface was found in the program class pool.
            // Print a warning.
            String referencingClassName = referencingClass.getName();

            dependencyWarningPrinter.print(referencingClassName,
                                           name,
                                           "Warning: library class " +
                                           ClassUtil.externalClassName(referencingClassName) +
                                           " depends on program class " +
                                           ClassUtil.externalClassName(name));
        }

        return clazz;
    }

    private void reportMissingClass(String referencingClassName, String name)
    {
        missingClassWarningPrinter.print(referencingClassName,
                                         name,
                                         "Warning: " +
                                         ClassUtil.externalClassName(referencingClassName) +
                                         ": can't find referenced class " +
                                         ClassUtil.externalClassName(name));
    }


    // Helper classes for KotlinReferenceInitializer.

    public static class KotlinTypeAliasReferenceInitializer
    implements          KotlinMetadataVisitor,
                        KotlinTypeAliasVisitor
    {
        private final KotlinTypeMetadata kotlinTypeMetadata;
        private final String             simpleName;

        KotlinTypeAliasReferenceInitializer(KotlinTypeMetadata kotlinTypeMetadata, String simpleName)
        {
            this.simpleName         = simpleName;
            this.kotlinTypeMetadata = kotlinTypeMetadata;
        }

        // Implementations for KotlinMetadataVisitor.

        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                            KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
        {
            kotlinDeclarationContainerMetadata.typeAliasesAccept(clazz, this);
        }

        @Override
        public void visitKotlinClassMetadata(Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata)
        {
            // Only if the alias was declared inside this class.
            if (kotlinTypeMetadata.aliasName.equals(clazz.getName() + "." + simpleName))
            {
                kotlinClassKindMetadata.typeAliasesAccept(clazz, this);
            }
        }

        // Implementations for KotlinTypeAliasVisitor.

        @Override
        public void visitTypeAlias(Clazz                              clazz,
                                   KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                   KotlinTypeAliasMetadata            kotlinTypeAliasMetadata)
        {
            if (this.simpleName.equals(kotlinTypeAliasMetadata.name))
            {
                this.kotlinTypeMetadata.referencedTypeAlias = kotlinTypeAliasMetadata;
            }
        }
    }

    /**
     * This KotlinFunctionVisitor initializes references to the default implementations of
     * interface functions.
     */
    private class KotlinDefaultImplsInitializer
    implements KotlinFunctionVisitor
    {
        private Clazz defaultImplsClass;

        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata) {}

        @Override
        public void visitFunction(Clazz                              clazz,
                                  KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                  KotlinFunctionMetadata             kotlinFunctionMetadata)
        {
            if (defaultImplsClass != null &&
                kotlinFunctionMetadata.jvmSignature != null &&
                !kotlinFunctionMetadata.flags.modality.isAbstract)
            {
                kotlinFunctionMetadata.referencedDefaultImplementationMethod =
                    strictMemberFinder.findMethod(
                        defaultImplsClass,
                        kotlinFunctionMetadata.jvmSignature.method,
                        getDescriptor(kotlinDeclarationContainerMetadata, kotlinFunctionMetadata)
                    );

                if (kotlinFunctionMetadata.referencedDefaultImplementationMethod != null)
                {
                    kotlinFunctionMetadata.referencedDefaultImplementationMethodClass = defaultImplsClass;
                }
            }
        }

        // Small helper methods.

        /**
         * Returns the descriptor for the default implementation method.
         */
        private String getDescriptor(KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                     KotlinFunctionMetadata             kotlinFunctionMetadata)
        {
            // Default implementation methods are static and have the interface
            // instance as its first parameter.
            return kotlinFunctionMetadata.jvmSignature.descriptor.toString()
                .replace("(", "(L" + kotlinDeclarationContainerMetadata.ownerClassName + ";");
        }
    }

    /**
     * This KotlinFunctionVisitor initializes references to sibling methods that
     * handle default parameter values.
      */
    private class KotlinDefaultMethodInitializer
    implements KotlinFunctionVisitor
    {
        boolean isInterface = false;
        boolean hasDefaults = false;

        // Implementations for KotlinFunctionVisitor.

        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata)
        {
            // During initialization this can be null because the
            // Asserter has not been run yet.
            if (kotlinFunctionMetadata.referencedMethod == null)
            {
                return;
            }

            // Use the jvm name here because the jvm name might not match
            // the metadata name.
            String methodName =
                kotlinFunctionMetadata.referencedMethod.getName(kotlinFunctionMetadata.referencedMethodClass);

            if (methodName.endsWith(DEFAULT_METHOD_SUFFIX))
            {
                return;
            }

            // Check if there are parameters with default values.
            hasDefaults = false;
            kotlinFunctionMetadata.valueParametersAccept(clazz,
                                                         kotlinMetadata,
                                                         (_clazz, vp) -> hasDefaults |= vp.flags.hasDefaultValue);

            if (hasDefaults)
            {
                String defaultMethodName = methodName + DEFAULT_METHOD_SUFFIX;
                String descriptor        = getDescriptor(kotlinFunctionMetadata);

                kotlinFunctionMetadata.referencedDefaultMethod =
                    strictMemberFinder.findMethod(kotlinFunctionMetadata.referencedMethodClass,
                                                  defaultMethodName,
                                                  descriptor);

                kotlinFunctionMetadata.referencedDefaultMethodClass = strictMemberFinder.correspondingClass();

                if (kotlinFunctionMetadata.referencedDefaultMethod == null && isInterface)
                {
                    Clazz defaultImplsClass = findClass(clazz,
                                                        clazz.getName() + DEFAULT_IMPLEMENTATIONS_SUFFIX,
                                                        false);

                    if (defaultImplsClass != null)
                    {
                        kotlinFunctionMetadata.referencedDefaultMethod =
                            strictMemberFinder.findMethod(defaultImplsClass, defaultMethodName, descriptor);

                        kotlinFunctionMetadata.referencedDefaultMethodClass = strictMemberFinder.correspondingClass();
                    }
                }
            }
        }

        // Small helper methods.

        /**
         * Return the descriptor for the utility method for default parameter values of
         * the supplied function.
         *
         * The descriptor matches the original method but with 2 or more extra parameters:
         *
         *  - the first parameter is an instance reference if the original method was not static.
         *  - the last 2+ parameters
         *     - int masks for every 32 parameters that encode which parameter values were passed
         *     - an object for future use? seems to be always null for now?
         */
        private String getDescriptor(KotlinFunctionMetadata kotlinFunctionMetadata)
        {
            String originalDescriptor =
                kotlinFunctionMetadata.referencedMethod.getDescriptor(kotlinFunctionMetadata.referencedMethodClass);

            // Each int param encodes up to 32 parameters as being present or not.
            int requiredIntParams = 1 + (ClassUtil.internalMethodParameterCount(originalDescriptor) / 32);

            String descriptor = originalDescriptor.replace(
                ")",
                String.join("", Collections.nCopies(requiredIntParams, "I")) + "Ljava/lang/Object;)"
            );

            // Instance methods will have been made static and have the class
            // instance as its first parameter.
            if ((kotlinFunctionMetadata.referencedMethod.getAccessFlags() & AccessConstants.STATIC) == 0)
            {
                descriptor = descriptor.replace("(", "(L" + kotlinFunctionMetadata.referencedMethodClass.getName() + ";");
            }

            return descriptor;
        }
    }

    /**
     * This KotlinPropertyVisitor tries to initialize missing property references
     * by looking into the supplied class.
     */
    private class KotlinInterClassPropertyReferenceInitializer
    implements KotlinPropertyVisitor
    {
        private final Clazz clazz;

        KotlinInterClassPropertyReferenceInitializer(Clazz clazz)
        {
            this.clazz = clazz;
        }

        // Implementations for KotlinPropertyVisitor.

        @Override
        public void visitAnyProperty(Clazz                              clazz,
                                     KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                     KotlinPropertyMetadata             kotlinPropertyMetadata)
        {
            if (kotlinPropertyMetadata.backingFieldSignature  != null &&
                kotlinPropertyMetadata.referencedBackingField == null)
            {
                kotlinPropertyMetadata.referencedBackingField =
                    strictMemberFinder.findField(this.clazz,
                                           kotlinPropertyMetadata.backingFieldSignature.memberName,
                                           kotlinPropertyMetadata.backingFieldSignature.descriptor);

                kotlinPropertyMetadata.referencedBackingFieldClass = strictMemberFinder.correspondingClass();
            }

            /* Default implementations of methods are stored in the $DefaultImpls class not in the
             * interface class.
             *
             * We fix any synthetic methods for annotations that were not already set (because
             * they weren't found the in interface class) - these will be found in the $DefaultImpls class.
             *
             * Not necessary for getters/setters as they are stored on the interface class itself.
             */
            if (kotlinPropertyMetadata.syntheticMethodForAnnotations           != null &&
                kotlinPropertyMetadata.referencedSyntheticMethodForAnnotations == null)
            {
                kotlinPropertyMetadata.referencedSyntheticMethodForAnnotations =
                    strictMemberFinder.findMethod(this.clazz,
                                                  kotlinPropertyMetadata.syntheticMethodForAnnotations.method,
                                                  kotlinPropertyMetadata.syntheticMethodForAnnotations.descriptor.toString());

                kotlinPropertyMetadata.referencedSyntheticMethodClass = strictMemberFinder.correspondingClass();
            }
        }
    }

    /**
     * This KotlinFunctionVisitor tries to initialize missing function references.
     */
    private static class KotlinInterClassSyntheticFunctionInitializer
    implements           KotlinFunctionVisitor,
                         ClassVisitor,
                         AttributeVisitor
    {
        private KotlinFunctionMetadata currentFunction;

        @Override
        public void visitAnyFunction(Clazz                  clazz,
                                     KotlinMetadata         kotlinMetadata,
                                     KotlinFunctionMetadata kotlinFunctionMetadata) {}

        @Override
        public void visitSyntheticFunction(Clazz                            clazz,
                                           KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata,
                                           KotlinFunctionMetadata           kotlinFunctionMetadata)
        {
            if (kotlinFunctionMetadata.referencedMethod == null)
            {
                this.currentFunction = kotlinFunctionMetadata;
                clazz.accept(this);
            }
        }


        @Override
        public void visitAnyClass(Clazz clazz) { }


        @Override
        public void visitProgramClass(ProgramClass programClass)
        {
            programClass.attributeAccept(Attribute.ENCLOSING_METHOD, this);
        }


        @Override
        public void visitLibraryClass(LibraryClass libraryClass)
        {
            // TODO: Not supported
        }


        @Override
        public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
        {
            Method enclosingMethod      = enclosingMethodAttribute.referencedMethod;
            Clazz  enclosingMethodClass = enclosingMethodAttribute.referencedClass;

            if (enclosingMethod              != null &&
                currentFunction.jvmSignature != null &&
                enclosingMethod.getName(enclosingMethodClass).equals(currentFunction.jvmSignature.method))
            {
                currentFunction.referencedMethod      = enclosingMethod;
                currentFunction.referencedMethodClass = enclosingMethodClass;
            }
        }
    }

    /**
     * This AttributeVisitor finds the parent of the visited companion class
     * and passes it to a KotlinInterClassPropertyReferenceInitializer.
     */
    private class KotlinCompanionParentPropertyInitializer
    implements    ClassVisitor,
                  AttributeVisitor,
                  InnerClassesInfoVisitor,
                  ConstantVisitor
    {
        // Implements for ClassVisitor


        @Override
        public void visitAnyClass(Clazz clazz) { }


        @Override
        public void visitProgramClass(ProgramClass programClass)
        {
            programClass.attributeAccept(Attribute.INNER_CLASSES, this);
        }


        @Override
        public void visitLibraryClass(LibraryClass libraryClass)
        {
            // Since `LibraryClass` doesn't have attributes, use a heuristic shortcut
            int dollarIndex = libraryClass.getName().lastIndexOf('$');
            if (dollarIndex != -1)
            {
                Clazz outerClass = findClass(libraryClass, libraryClass.getName().substring(0, dollarIndex));
                initProperty(libraryClass, outerClass);
            }
        }


        // Implementations for AttributeVisitor.

        @Override
        public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

        @Override
        public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
        {
            innerClassesAttribute.innerClassEntriesAccept(clazz, this);
        }

        // Implementations for InnerClassesInfoVisitor.

        @Override
        public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo)
        {
            innerClassesInfo.outerClassConstantAccept(clazz, this);
        }

        // Implementations for ConstantVisitor.

        @Override
        public void visitAnyConstant(Clazz clazz, Constant constant) {}

        @Override
        public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
        {
            initProperty(clazz, classConstant.referencedClass);
        }

        private void initProperty(Clazz thisClazz, Clazz parentClazz)
        {
            if (parentClazz != null)
            {
                 thisClazz.kotlinMetadataAccept(
                         new AllPropertyVisitor(
                         new KotlinInterClassPropertyReferenceInitializer(parentClazz)));
            }
        }
    }


    /**
     * Perform some sanity checks on the Signature and whether it follows
     * the JVM specification.
     */
    private boolean isValidClassSignature(Clazz clazz, String signature)
    {
        try
        {
            // Loop through the signature to if it can be parsed.
            new DescriptorClassEnumeration(signature).classCount();

            // Then check whether the listed types are as expected.
            InternalTypeEnumeration internalTypeEnumeration = new InternalTypeEnumeration(signature);

            if (!internalTypeEnumeration.hasMoreTypes())
            {
                return false;
            }

            String superName     = clazz.getSuperName();
            String signSuperName = ClassUtil.internalClassNameFromClassType(internalTypeEnumeration.nextType());
            if (superName != null &&
                !signSuperName.startsWith(superName))
            {
                return false;
            }

            for (int i = 0; i < clazz.getInterfaceCount(); i++)
            {
                if (!internalTypeEnumeration.hasMoreTypes())
                {
                    return false;
                }

                String intfName     = clazz.getInterfaceName(i);
                String signIntfName = ClassUtil.internalClassNameFromClassType(internalTypeEnumeration.nextType());
                if (!signIntfName.startsWith(intfName))
                {
                    return false;
                }
            }

            return !internalTypeEnumeration.hasMoreTypes();
        }
        catch (Exception corruptedSignature)
        {
            return false;
        }
    }
}
