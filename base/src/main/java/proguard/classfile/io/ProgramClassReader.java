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
package proguard.classfile.io;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.target.*;
import proguard.classfile.attribute.annotation.target.visitor.*;
import proguard.classfile.attribute.annotation.visitor.*;
import proguard.classfile.attribute.module.*;
import proguard.classfile.attribute.module.visitor.*;
import proguard.classfile.attribute.preverification.*;
import proguard.classfile.attribute.preverification.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.io.RuntimeDataInput;

import java.io.DataInput;

/**
 * This {@link ClassVisitor} fills out the {@link ProgramClass} instances that it visits with data
 * from the given {@link DataInput} object.
 *
 * @author Eric Lafortune
 */
public class ProgramClassReader
implements   ClassVisitor,
             MemberVisitor,
             ConstantVisitor,
             AttributeVisitor,
             BootstrapMethodInfoVisitor,
             RecordComponentInfoVisitor,
             InnerClassesInfoVisitor,
             ExceptionInfoVisitor,
             StackMapFrameVisitor,
             VerificationTypeVisitor,
             LineNumberInfoVisitor,
             ParameterInfoVisitor,
             LocalVariableInfoVisitor,
             LocalVariableTypeInfoVisitor,
             RequiresInfoVisitor,
             ExportsInfoVisitor,
             OpensInfoVisitor,
             ProvidesInfoVisitor,
             AnnotationVisitor,
             TypeAnnotationVisitor,
             TargetInfoVisitor,
             TypePathInfoVisitor,
             LocalVariableTargetElementVisitor,
             ElementValueVisitor
{
    private final RuntimeDataInput dataInput;
    private final boolean          ignoreStackMapAttributes;


    /**
     * Creates a new ProgramClassReader for reading from the given DataInput.
     */
    public ProgramClassReader(DataInput dataInput)
    {
        this(dataInput, false);
    }


    /**
     * Creates a new ProgramClassReader for reading from the given DataInput,
     * optionally treating stack map attributes as unknown attributes.
     */
    public ProgramClassReader(DataInput dataInput,
                              boolean   ignoreStackMapAttributes)
    {
        this.dataInput                = new RuntimeDataInput(dataInput);
        this.ignoreStackMapAttributes = ignoreStackMapAttributes;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        // Read and check the magic number.
        int u4magic = dataInput.readInt();

        ClassUtil.checkMagicNumber(u4magic);

        // Read and check the version numbers.
        int u2minorVersion = dataInput.readUnsignedShort();
        int u2majorVersion = dataInput.readUnsignedShort();

        programClass.u4version = ClassUtil.internalClassVersion(u2majorVersion,
                                                                u2minorVersion);

        ClassUtil.checkVersionNumbers(programClass.u4version);

        // Read the constant pool. Note that the first entry is not used.
        programClass.u2constantPoolCount = dataInput.readUnsignedShort();

        programClass.constantPool = new Constant[programClass.u2constantPoolCount];
        for (int index = 1; index < programClass.u2constantPoolCount; index++)
        {
            Constant constant = createConstant();
            constant.accept(programClass, this);
            programClass.constantPool[index] = constant;

            // Long constants and double constants take up two entries in the
            // constant pool.
            int tag = constant.getTag();
            if (tag == Constant.LONG ||
                tag == Constant.DOUBLE)
            {
                programClass.constantPool[++index] = null;
            }
        }

        // Read the general class information.
        programClass.u2accessFlags = dataInput.readUnsignedShort();
        programClass.u2thisClass   = dataInput.readUnsignedShort();
        programClass.u2superClass  = dataInput.readUnsignedShort();

        // Read the interfaces.
        programClass.u2interfacesCount = dataInput.readUnsignedShort();
        programClass.u2interfaces      = readUnsignedShorts(programClass.u2interfacesCount);

        // Read the fields.
        programClass.u2fieldsCount = dataInput.readUnsignedShort();

        programClass.fields = new ProgramField[programClass.u2fieldsCount];
        for (int index = 0; index < programClass.u2fieldsCount; index++)
        {
            ProgramField programField = new ProgramField();
            this.visitProgramField(programClass, programField);
            programClass.fields[index] = programField;
        }

        // Read the methods.
        programClass.u2methodsCount = dataInput.readUnsignedShort();

        programClass.methods = new ProgramMethod[programClass.u2methodsCount];
        for (int index = 0; index < programClass.u2methodsCount; index++)
        {
            ProgramMethod programMethod = new ProgramMethod();
            this.visitProgramMethod(programClass, programMethod);
            programClass.methods[index] = programMethod;
        }

        // Read the class attributes.
        programClass.u2attributesCount = dataInput.readUnsignedShort();

        programClass.attributes = new Attribute[programClass.u2attributesCount];
        for (int index = 0; index < programClass.u2attributesCount; index++)
        {
            Attribute attribute = createAttribute(programClass);
            attribute.accept(programClass, this);
            programClass.attributes[index] = attribute;
        }
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        // Read the general field information.
        programField.u2accessFlags     = dataInput.readUnsignedShort();
        programField.u2nameIndex       = dataInput.readUnsignedShort();
        programField.u2descriptorIndex = dataInput.readUnsignedShort();

        // Read the field attributes.
        programField.u2attributesCount = dataInput.readUnsignedShort();

        programField.attributes = new Attribute[programField.u2attributesCount];
        for (int index = 0; index < programField.u2attributesCount; index++)
        {
            Attribute attribute = createAttribute(programClass);
            attribute.accept(programClass, programField, this);
            programField.attributes[index] = attribute;
        }
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        // Read the general method information.
        programMethod.u2accessFlags     = dataInput.readUnsignedShort();
        programMethod.u2nameIndex       = dataInput.readUnsignedShort();
        programMethod.u2descriptorIndex = dataInput.readUnsignedShort();

        // Read the method attributes.
        programMethod.u2attributesCount = dataInput.readUnsignedShort();

        programMethod.attributes = new Attribute[programMethod.u2attributesCount];
        for (int index = 0; index < programMethod.u2attributesCount; index++)
        {
            Attribute attribute = createAttribute(programClass);
            attribute.accept(programClass, programMethod, this);
            programMethod.attributes[index] = attribute;
        }
    }


    public void visitLibraryMember(LibraryClass libraryClass, LibraryMember libraryMember)
    {
    }


    // Implementations for ConstantVisitor.

    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
    {
        integerConstant.u4value = dataInput.readInt();
    }


    public void visitLongConstant(Clazz clazz, LongConstant longConstant)
    {
        longConstant.u8value = dataInput.readLong();
    }


    public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant)
    {
        floatConstant.f4value = dataInput.readFloat();
    }


    public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant)
    {
        doubleConstant.f8value = dataInput.readDouble();
    }


    public void visitPrimitiveArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant)
    {
        char u2primitiveType = dataInput.readChar();
        int  u4length        = dataInput.readInt();

        switch (u2primitiveType)
        {
            case TypeConstants.BOOLEAN:
            {
                boolean[] values = new boolean[u4length];

                for (int index = 0; index < u4length; index++)
                {
                    values[index] = dataInput.readBoolean();
                }

                primitiveArrayConstant.values = values;
                break;
            }
            case TypeConstants.BYTE:
            {
                byte[] values = new byte[u4length];
                dataInput.readFully(values);

                primitiveArrayConstant.values = values;
                break;
            }
            case TypeConstants.CHAR:
            {
                char[] values = new char[u4length];

                for (int index = 0; index < u4length; index++)
                {
                    values[index] = dataInput.readChar();
                }

                primitiveArrayConstant.values = values;
                break;
            }
            case TypeConstants.SHORT:
            {
                short[] values = new short[u4length];

                for (int index = 0; index < u4length; index++)
                {
                    values[index] = dataInput.readShort();
                }

                primitiveArrayConstant.values = values;
                break;
            }
            case TypeConstants.INT:
            {
                int[] values = new int[u4length];

                for (int index = 0; index < u4length; index++)
                {
                    values[index] = dataInput.readInt();
                }

                primitiveArrayConstant.values = values;
                break;
            }
            case TypeConstants.FLOAT:
            {
                float[] values = new float[u4length];

                for (int index = 0; index < u4length; index++)
                {
                    values[index] = dataInput.readFloat();
                }

                primitiveArrayConstant.values = values;
                break;
            }
            case TypeConstants.LONG:
            {
                long[] values = new long[u4length];

                for (int index = 0; index < u4length; index++)
                {
                    values[index] = dataInput.readLong();
                }

                primitiveArrayConstant.values = values;
                break;
            }
            case TypeConstants.DOUBLE:
            {
                double[] values = new double[u4length];

                for (int index = 0; index < u4length; index++)
                {
                    values[index] = dataInput.readDouble();
                }

                primitiveArrayConstant.values = values;
                break;
            }
        }
    }


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        stringConstant.u2stringIndex = dataInput.readUnsignedShort();
    }


    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        int u2length = dataInput.readUnsignedShort();

        // Read the UTF-8 bytes.
        byte[] bytes = new byte[u2length];
        dataInput.readFully(bytes);
        utf8Constant.setBytes(bytes);
    }


    public void visitDynamicConstant(Clazz clazz, DynamicConstant dynamicConstant)
    {
        dynamicConstant.u2bootstrapMethodAttributeIndex = dataInput.readUnsignedShort();
        dynamicConstant.u2nameAndTypeIndex              = dataInput.readUnsignedShort();
    }


    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        invokeDynamicConstant.u2bootstrapMethodAttributeIndex = dataInput.readUnsignedShort();
        invokeDynamicConstant.u2nameAndTypeIndex              = dataInput.readUnsignedShort();
    }


    public void visitMethodHandleConstant(Clazz clazz, MethodHandleConstant methodHandleConstant)
    {
        methodHandleConstant.u1referenceKind  = dataInput.readUnsignedByte();
        methodHandleConstant.u2referenceIndex = dataInput.readUnsignedShort();
    }


    public void visitModuleConstant(Clazz clazz, ModuleConstant moduleConstant)
    {
        moduleConstant.u2nameIndex = dataInput.readUnsignedShort();
    }


    public void visitPackageConstant(Clazz clazz, PackageConstant packageConstant)
    {
        packageConstant.u2nameIndex = dataInput.readUnsignedShort();
    }


    public void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
    {
        refConstant.u2classIndex       = dataInput.readUnsignedShort();
        refConstant.u2nameAndTypeIndex = dataInput.readUnsignedShort();
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        classConstant.u2nameIndex = dataInput.readUnsignedShort();
    }


    public void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant methodTypeConstant)
    {
        methodTypeConstant.u2descriptorIndex = dataInput.readUnsignedShort();
    }


    public void visitNameAndTypeConstant(Clazz clazz, NameAndTypeConstant nameAndTypeConstant)
    {
        nameAndTypeConstant.u2nameIndex       = dataInput.readUnsignedShort();
        nameAndTypeConstant.u2descriptorIndex = dataInput.readUnsignedShort();
    }


    // Implementations for AttributeVisitor.

    public void visitUnknownAttribute(Clazz clazz, UnknownAttribute unknownAttribute)
    {
        // Read the unknown information.
        byte[] info = new byte[unknownAttribute.u4attributeLength];
        dataInput.readFully(info);

        unknownAttribute.info = info;
    }


    public void visitBootstrapMethodsAttribute(Clazz clazz, BootstrapMethodsAttribute bootstrapMethodsAttribute)
    {
        // Read the bootstrap methods.
        bootstrapMethodsAttribute.u2bootstrapMethodsCount = dataInput.readUnsignedShort();

        bootstrapMethodsAttribute.bootstrapMethods = new BootstrapMethodInfo[bootstrapMethodsAttribute.u2bootstrapMethodsCount];
        for (int index = 0; index < bootstrapMethodsAttribute.u2bootstrapMethodsCount; index++)
        {
            BootstrapMethodInfo bootstrapMethodInfo = new BootstrapMethodInfo();
            visitBootstrapMethodInfo(clazz, bootstrapMethodInfo);
            bootstrapMethodsAttribute.bootstrapMethods[index] = bootstrapMethodInfo;
        }
    }


    public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute)
    {
        sourceFileAttribute.u2sourceFileIndex = dataInput.readUnsignedShort();
    }


    public void visitSourceDirAttribute(Clazz clazz, SourceDirAttribute sourceDirAttribute)
    {
        sourceDirAttribute.u2sourceDirIndex = dataInput.readUnsignedShort();
    }


    public void visitSourceDebugExtensionAttribute(Clazz clazz, SourceDebugExtensionAttribute sourceDebugExtensionAttribute)
    {
        // Read the source debug extension information.
        byte[] info = new byte[sourceDebugExtensionAttribute.u4attributeLength];
        dataInput.readFully(info);
        sourceDebugExtensionAttribute.info = info;
    }


    public void visitRecordAttribute(Clazz clazz, RecordAttribute recordAttribute)
    {
        // Read the components.
        recordAttribute.u2componentsCount = dataInput.readUnsignedShort();

        recordAttribute.components = new RecordComponentInfo[recordAttribute.u2componentsCount];
        for (int index = 0; index < recordAttribute.u2componentsCount; index++)
        {
            RecordComponentInfo recordComponentInfo = new RecordComponentInfo();
            visitRecordComponentInfo(clazz, recordComponentInfo);
            recordAttribute.components[index] = recordComponentInfo;
        }
    }


    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        // Read the inner classes.
        innerClassesAttribute.u2classesCount = dataInput.readUnsignedShort();

        innerClassesAttribute.classes = new InnerClassesInfo[innerClassesAttribute.u2classesCount];
        for (int index = 0; index < innerClassesAttribute.u2classesCount; index++)
        {
            InnerClassesInfo innerClassesInfo = new InnerClassesInfo();
            visitInnerClassesInfo(clazz, innerClassesInfo);
            innerClassesAttribute.classes[index] = innerClassesInfo;
        }
    }


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        enclosingMethodAttribute.u2classIndex       = dataInput.readUnsignedShort();
        enclosingMethodAttribute.u2nameAndTypeIndex = dataInput.readUnsignedShort();
    }


    public void visitNestHostAttribute(Clazz clazz, NestHostAttribute nestHostAttribute)
    {
        nestHostAttribute.u2hostClassIndex = dataInput.readUnsignedShort();
    }


    public void visitNestMembersAttribute(Clazz clazz, NestMembersAttribute nestMembersAttribute)
    {
        // Read the nest host classes.
        nestMembersAttribute.u2classesCount = dataInput.readUnsignedShort();
        nestMembersAttribute.u2classes      = readUnsignedShorts(nestMembersAttribute.u2classesCount);
    }


    public void visitPermittedSubclassesAttribute(Clazz clazz, PermittedSubclassesAttribute permittedSubclassesAttribute)
    {
        // Read the nest host classes.
        permittedSubclassesAttribute.u2classesCount = dataInput.readUnsignedShort();
        permittedSubclassesAttribute.u2classes      = readUnsignedShorts(permittedSubclassesAttribute.u2classesCount);
    }


    public void visitModuleAttribute(Clazz clazz, ModuleAttribute moduleAttribute)
    {
        moduleAttribute.u2moduleNameIndex    = dataInput.readUnsignedShort();
        moduleAttribute.u2moduleFlags        = dataInput.readUnsignedShort();
        moduleAttribute.u2moduleVersionIndex = dataInput.readUnsignedShort();

        // Read the requires.
        moduleAttribute.u2requiresCount      = dataInput.readUnsignedShort();

        moduleAttribute.requires             = new RequiresInfo[moduleAttribute.u2requiresCount];
        for (int index = 0; index < moduleAttribute.u2requiresCount; index++)
        {
            RequiresInfo requiresInfo = new RequiresInfo();
            visitRequiresInfo(clazz, requiresInfo);
            moduleAttribute.requires[index] = requiresInfo;
        }

        // Read the exports.
        moduleAttribute.u2exportsCount       = dataInput.readUnsignedShort();

        moduleAttribute.exports              = new ExportsInfo[moduleAttribute.u2exportsCount];
        for (int index = 0; index < moduleAttribute.u2exportsCount; index++)
        {
            ExportsInfo exportsInfo = new ExportsInfo();
            visitExportsInfo(clazz, exportsInfo);
            moduleAttribute.exports[index] = exportsInfo;
        }

        // Read the opens.
        moduleAttribute.u2opensCount         = dataInput.readUnsignedShort();

        moduleAttribute.opens                = new OpensInfo[moduleAttribute.u2opensCount];
        for (int index = 0; index < moduleAttribute.u2opensCount; index++)
        {
            OpensInfo opensInfo = new OpensInfo();
            visitOpensInfo(clazz, opensInfo);
            moduleAttribute.opens[index] = opensInfo;
        }

        // Read the uses.
        moduleAttribute.u2usesCount = dataInput.readUnsignedShort();
        moduleAttribute.u2uses      = readUnsignedShorts(moduleAttribute.u2usesCount);

        // Read the provides.
        moduleAttribute.u2providesCount      = dataInput.readUnsignedShort();

        moduleAttribute.provides             = new ProvidesInfo[moduleAttribute.u2providesCount];
        for (int index = 0; index < moduleAttribute.u2providesCount; index++)
        {
            ProvidesInfo providesInfo = new ProvidesInfo();
            visitProvidesInfo(clazz, providesInfo);
            moduleAttribute.provides[index] = providesInfo;
        }
    }


    public void visitModuleMainClassAttribute(Clazz clazz, ModuleMainClassAttribute moduleMainClassAttribute)
    {
        moduleMainClassAttribute.u2mainClass = dataInput.readUnsignedShort();
    }


    public void visitModulePackagesAttribute(Clazz clazz, ModulePackagesAttribute modulePackagesAttribute)
    {
        // Read the packages.
        modulePackagesAttribute.u2packagesCount = dataInput.readUnsignedShort();
        modulePackagesAttribute.u2packages      = readUnsignedShorts(modulePackagesAttribute.u2packagesCount);
    }


    public void visitDeprecatedAttribute(Clazz clazz, DeprecatedAttribute deprecatedAttribute)
    {
        // This attribute does not contain any additional information.
    }


    public void visitSyntheticAttribute(Clazz clazz, SyntheticAttribute syntheticAttribute)
    {
        // This attribute does not contain any additional information.
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        signatureAttribute.u2signatureIndex = dataInput.readUnsignedShort();
    }


    public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        constantValueAttribute.u2constantValueIndex = dataInput.readUnsignedShort();
    }


    public void visitMethodParametersAttribute(Clazz clazz, Method method, MethodParametersAttribute methodParametersAttribute)
    {
        // Read the parameter information.
        methodParametersAttribute.u1parametersCount = dataInput.readUnsignedByte();

        methodParametersAttribute.parameters = new ParameterInfo[methodParametersAttribute.u1parametersCount];
        for (int index = 0; index < methodParametersAttribute.u1parametersCount; index++)
        {
            ParameterInfo parameterInfo = new ParameterInfo();
            visitParameterInfo(clazz, method, index, parameterInfo);
            methodParametersAttribute.parameters[index] = parameterInfo;
        }
    }


    public void visitExceptionsAttribute(Clazz clazz, Method method, ExceptionsAttribute exceptionsAttribute)
    {
        // Read the exceptions.
        exceptionsAttribute.u2exceptionIndexTableLength = dataInput.readUnsignedShort();
        exceptionsAttribute.u2exceptionIndexTable       = readUnsignedShorts(exceptionsAttribute.u2exceptionIndexTableLength);
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Read the stack size and local variable frame size.
        codeAttribute.u2maxStack   = dataInput.readUnsignedShort();
        codeAttribute.u2maxLocals  = dataInput.readUnsignedShort();

        // Read the byte code.
        codeAttribute.u4codeLength = dataInput.readInt();

        byte[] code = new byte[codeAttribute.u4codeLength];
        dataInput.readFully(code);
        codeAttribute.code = code;

        // Read the exceptions.
        codeAttribute.u2exceptionTableLength = dataInput.readUnsignedShort();

        codeAttribute.exceptionTable = new ExceptionInfo[codeAttribute.u2exceptionTableLength];
        for (int index = 0; index < codeAttribute.u2exceptionTableLength; index++)
        {
            ExceptionInfo exceptionInfo = new ExceptionInfo();
            visitExceptionInfo(clazz, method, codeAttribute, exceptionInfo);
            codeAttribute.exceptionTable[index] = exceptionInfo;
        }

        // Read the code attributes.
        codeAttribute.u2attributesCount = dataInput.readUnsignedShort();

        codeAttribute.attributes = new Attribute[codeAttribute.u2attributesCount];
        for (int index = 0; index < codeAttribute.u2attributesCount; index++)
        {
            Attribute attribute = createAttribute(clazz);
            attribute.accept(clazz, method, codeAttribute, this);
            codeAttribute.attributes[index] = attribute;
        }
    }


    public void visitStackMapAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapAttribute stackMapAttribute)
    {
        // Read the stack map frames (only full frames, without tag).
        stackMapAttribute.u2stackMapFramesCount = dataInput.readUnsignedShort();

        stackMapAttribute.stackMapFrames = new FullFrame[stackMapAttribute.u2stackMapFramesCount];
        for (int index = 0; index < stackMapAttribute.u2stackMapFramesCount; index++)
        {
            FullFrame stackMapFrame = new FullFrame();
            visitFullFrame(clazz, method, codeAttribute, index, stackMapFrame);
            stackMapAttribute.stackMapFrames[index] = stackMapFrame;
        }
    }


    public void visitStackMapTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapTableAttribute stackMapTableAttribute)
    {
        // Read the stack map frames.
        stackMapTableAttribute.u2stackMapFramesCount = dataInput.readUnsignedShort();

        stackMapTableAttribute.stackMapFrames = new StackMapFrame[stackMapTableAttribute.u2stackMapFramesCount];
        for (int index = 0; index < stackMapTableAttribute.u2stackMapFramesCount; index++)
        {
            StackMapFrame stackMapFrame = createStackMapFrame();
            stackMapFrame.accept(clazz, method, codeAttribute, 0, this);
            stackMapTableAttribute.stackMapFrames[index] = stackMapFrame;
        }
    }


    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        // Read the line numbers.
        lineNumberTableAttribute.u2lineNumberTableLength = dataInput.readUnsignedShort();

        lineNumberTableAttribute.lineNumberTable = new LineNumberInfo[lineNumberTableAttribute.u2lineNumberTableLength];
        for (int index = 0; index < lineNumberTableAttribute.u2lineNumberTableLength; index++)
        {
            LineNumberInfo lineNumberInfo = new LineNumberInfo();
            visitLineNumberInfo(clazz, method, codeAttribute, lineNumberInfo);
            lineNumberTableAttribute.lineNumberTable[index] = lineNumberInfo;
        }
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Read the local variables.
        localVariableTableAttribute.u2localVariableTableLength = dataInput.readUnsignedShort();

        localVariableTableAttribute.localVariableTable = new LocalVariableInfo[localVariableTableAttribute.u2localVariableTableLength];
        for (int index = 0; index < localVariableTableAttribute.u2localVariableTableLength; index++)
        {
            LocalVariableInfo localVariableInfo = new LocalVariableInfo();
            visitLocalVariableInfo(clazz, method, codeAttribute, localVariableInfo);
            localVariableTableAttribute.localVariableTable[index] = localVariableInfo;
        }
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Read the local variable types.
        localVariableTypeTableAttribute.u2localVariableTypeTableLength = dataInput.readUnsignedShort();

        localVariableTypeTableAttribute.localVariableTypeTable = new LocalVariableTypeInfo[localVariableTypeTableAttribute.u2localVariableTypeTableLength];
        for (int index = 0; index < localVariableTypeTableAttribute.u2localVariableTypeTableLength; index++)
        {
            LocalVariableTypeInfo localVariableTypeInfo = new LocalVariableTypeInfo();
            visitLocalVariableTypeInfo(clazz, method, codeAttribute, localVariableTypeInfo);
            localVariableTypeTableAttribute.localVariableTypeTable[index] = localVariableTypeInfo;
        }
    }


    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        // Read the annotations.
        annotationsAttribute.u2annotationsCount = dataInput.readUnsignedShort();

        annotationsAttribute.annotations = new Annotation[annotationsAttribute.u2annotationsCount];
        for (int index = 0; index < annotationsAttribute.u2annotationsCount; index++)
        {
            Annotation annotation = new Annotation();
            visitAnnotation(clazz, annotation);
            annotationsAttribute.annotations[index] = annotation;
        }
    }


    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        // Read the parameter annotations.
        parameterAnnotationsAttribute.u1parametersCount           = dataInput.readUnsignedByte();
        parameterAnnotationsAttribute.u2parameterAnnotationsCount = new int[parameterAnnotationsAttribute.u1parametersCount];
        parameterAnnotationsAttribute.parameterAnnotations        = new Annotation[parameterAnnotationsAttribute.u1parametersCount][];

        for (int parameterIndex = 0; parameterIndex < parameterAnnotationsAttribute.u1parametersCount; parameterIndex++)
        {
            // Read the parameter annotations of the given parameter.
            int u2annotationsCount = dataInput.readUnsignedShort();

            Annotation[] annotations = new Annotation[u2annotationsCount];

            for (int index = 0; index < u2annotationsCount; index++)
            {
                Annotation annotation = new Annotation();
                visitAnnotation(clazz, annotation);
                annotations[index] = annotation;
            }

            parameterAnnotationsAttribute.u2parameterAnnotationsCount[parameterIndex] = u2annotationsCount;
            parameterAnnotationsAttribute.parameterAnnotations[parameterIndex]        = annotations;
        }
    }


    public void visitAnyTypeAnnotationsAttribute(Clazz clazz, TypeAnnotationsAttribute typeAnnotationsAttribute)
    {
        // Read the type annotations.
        typeAnnotationsAttribute.u2annotationsCount = dataInput.readUnsignedShort();

        typeAnnotationsAttribute.annotations = new TypeAnnotation[typeAnnotationsAttribute.u2annotationsCount];
        for (int index = 0; index < typeAnnotationsAttribute.u2annotationsCount; index++)
        {
            TypeAnnotation typeAnnotation = new TypeAnnotation();
            visitTypeAnnotation(clazz, typeAnnotation);
            typeAnnotationsAttribute.annotations[index] = typeAnnotation;
        }
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        // Read the default element value.
        ElementValue elementValue = createElementValue();
        elementValue.accept(clazz, null, this);
        annotationDefaultAttribute.defaultValue = elementValue;
    }


    // Implementations for BootstrapMethodInfoVisitor.

    public void visitBootstrapMethodInfo(Clazz clazz, BootstrapMethodInfo bootstrapMethodInfo)
    {
        bootstrapMethodInfo.u2methodHandleIndex = dataInput.readUnsignedShort();

        // Read the bootstrap method arguments.
        bootstrapMethodInfo.u2methodArgumentCount = dataInput.readUnsignedShort();
        bootstrapMethodInfo.u2methodArguments     = readUnsignedShorts(bootstrapMethodInfo.u2methodArgumentCount);
    }


    // Implementations for RecordComponentInfoVisitor.

    public void visitRecordComponentInfo(Clazz clazz, RecordComponentInfo recordComponentInfo)
    {
        recordComponentInfo.u2nameIndex       = dataInput.readUnsignedShort();
        recordComponentInfo.u2descriptorIndex = dataInput.readUnsignedShort();

        // Read the component attributes.
        recordComponentInfo.u2attributesCount = dataInput.readUnsignedShort();

        recordComponentInfo.attributes = new Attribute[recordComponentInfo.u2attributesCount];
        for (int index = 0; index < recordComponentInfo.u2attributesCount; index++)
        {
            Attribute attribute = createAttribute(clazz);
            attribute.accept(clazz, this);
            recordComponentInfo.attributes[index] = attribute;
        }
    }


    // Implementations for InnerClassesInfoVisitor.

    public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo)
    {
        innerClassesInfo.u2innerClassIndex       = dataInput.readUnsignedShort();
        innerClassesInfo.u2outerClassIndex       = dataInput.readUnsignedShort();
        innerClassesInfo.u2innerNameIndex        = dataInput.readUnsignedShort();
        innerClassesInfo.u2innerClassAccessFlags = dataInput.readUnsignedShort();
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo)
    {
        exceptionInfo.u2startPC   = dataInput.readUnsignedShort();
        exceptionInfo.u2endPC     = dataInput.readUnsignedShort();
        exceptionInfo.u2handlerPC = dataInput.readUnsignedShort();
        exceptionInfo.u2catchType = dataInput.readUnsignedShort();
    }


    // Implementations for StackMapFrameVisitor.

    public void visitSameZeroFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SameZeroFrame sameZeroFrame)
    {
        if (sameZeroFrame.getTag() == StackMapFrame.SAME_ZERO_FRAME_EXTENDED)
        {
            sameZeroFrame.u2offsetDelta = dataInput.readUnsignedShort();
        }
    }


    public void visitSameOneFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SameOneFrame sameOneFrame)
    {
        if (sameOneFrame.getTag() == StackMapFrame.SAME_ONE_FRAME_EXTENDED)
        {
            sameOneFrame.u2offsetDelta = dataInput.readUnsignedShort();
        }

        // Read the verification type of the stack entry.
        VerificationType verificationType = createVerificationType();
        verificationType.accept(clazz, method, codeAttribute, offset, this);
        sameOneFrame.stackItem = verificationType;
    }


    public void visitLessZeroFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, LessZeroFrame lessZeroFrame)
    {
        lessZeroFrame.u2offsetDelta = dataInput.readUnsignedShort();
    }


    public void visitMoreZeroFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, MoreZeroFrame moreZeroFrame)
    {
        moreZeroFrame.u2offsetDelta = dataInput.readUnsignedShort();

        // Read the verification types of the additional local variables.
        moreZeroFrame.additionalVariables = new VerificationType[moreZeroFrame.additionalVariablesCount];
        for (int index = 0; index < moreZeroFrame.additionalVariablesCount; index++)
        {
            VerificationType verificationType = createVerificationType();
            verificationType.accept(clazz, method, codeAttribute, offset, this);
            moreZeroFrame.additionalVariables[index] = verificationType;
        }
    }


    public void visitFullFrame(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, FullFrame fullFrame)
    {
        fullFrame.u2offsetDelta = dataInput.readUnsignedShort();

        // Read the verification types of the local variables.
        fullFrame.variablesCount = dataInput.readUnsignedShort();
        fullFrame.variables = new VerificationType[fullFrame.variablesCount];
        for (int index = 0; index < fullFrame.variablesCount; index++)
        {
            VerificationType verificationType = createVerificationType();
            verificationType.variablesAccept(clazz, method, codeAttribute, offset, index, this);
            fullFrame.variables[index] = verificationType;
        }

        // Read the verification types of the stack entries.
        fullFrame.stackCount = dataInput.readUnsignedShort();
        fullFrame.stack = new VerificationType[fullFrame.stackCount];
        for (int index = 0; index < fullFrame.stackCount; index++)
        {
            VerificationType verificationType = createVerificationType();
            verificationType.stackAccept(clazz, method, codeAttribute, offset, index, this);
            fullFrame.stack[index] = verificationType;
        }
    }


    // Implementations for VerificationTypeVisitor.

    public void visitAnyVerificationType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VerificationType verificationType)
    {
        // Most verification types don't contain any additional information.
    }


    public void visitObjectType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ObjectType objectType)
    {
        objectType.u2classIndex = dataInput.readUnsignedShort();
    }


    public void visitUninitializedType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, UninitializedType uninitializedType)
    {
        uninitializedType.u2newInstructionOffset = dataInput.readUnsignedShort();
    }


    // Implementations for LineNumberInfoVisitor.

    public void visitLineNumberInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberInfo lineNumberInfo)
    {
        lineNumberInfo.u2startPC    = dataInput.readUnsignedShort();
        lineNumberInfo.u2lineNumber = dataInput.readUnsignedShort();
    }


    // Implementations for ParameterInfoVisitor.

    public void visitParameterInfo(Clazz clazz, Method method, int parameterIndex, ParameterInfo parameterInfo)
    {
        parameterInfo.u2nameIndex   = dataInput.readUnsignedShort();
        parameterInfo.u2accessFlags = dataInput.readUnsignedShort();
    }


    // Implementations for LocalVariableInfoVisitor.

    public void visitLocalVariableInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableInfo localVariableInfo)
    {
        localVariableInfo.u2startPC         = dataInput.readUnsignedShort();
        localVariableInfo.u2length          = dataInput.readUnsignedShort();
        localVariableInfo.u2nameIndex       = dataInput.readUnsignedShort();
        localVariableInfo.u2descriptorIndex = dataInput.readUnsignedShort();
        localVariableInfo.u2index           = dataInput.readUnsignedShort();
    }


    // Implementations for LocalVariableTypeInfoVisitor.

    public void visitLocalVariableTypeInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeInfo localVariableTypeInfo)
    {
        localVariableTypeInfo.u2startPC        = dataInput.readUnsignedShort();
        localVariableTypeInfo.u2length         = dataInput.readUnsignedShort();
        localVariableTypeInfo.u2nameIndex      = dataInput.readUnsignedShort();
        localVariableTypeInfo.u2signatureIndex = dataInput.readUnsignedShort();
        localVariableTypeInfo.u2index          = dataInput.readUnsignedShort();
    }


    // Implementations for RequiresInfoVisitor.

    public void visitRequiresInfo(Clazz clazz, RequiresInfo requiresInfo)
    {
        requiresInfo.u2requiresIndex        = dataInput.readUnsignedShort();
        requiresInfo.u2requiresFlags        = dataInput.readUnsignedShort();
        requiresInfo.u2requiresVersionIndex = dataInput.readUnsignedShort();
    }


    // Implementations for ExportsInfoVisitor.

    public void visitExportsInfo(Clazz clazz, ExportsInfo exportsInfo)
    {
        exportsInfo.u2exportsIndex   = dataInput.readUnsignedShort();
        exportsInfo.u2exportsFlags   = dataInput.readUnsignedShort();

        // Read the targets.
        exportsInfo.u2exportsToCount = dataInput.readUnsignedShort();
        exportsInfo.u2exportsToIndex = readUnsignedShorts(exportsInfo.u2exportsToCount);
    }


    // Implementations for OpensInfoVisitor.

    public void visitOpensInfo(Clazz clazz, OpensInfo opensInfo)
    {
        opensInfo.u2opensIndex   = dataInput.readUnsignedShort();
        opensInfo.u2opensFlags   = dataInput.readUnsignedShort();

        // Read the targets.
        opensInfo.u2opensToCount = dataInput.readUnsignedShort();
        opensInfo.u2opensToIndex = readUnsignedShorts(opensInfo.u2opensToCount);
    }


    // Implementations for ProvidesInfoVisitor.

    public void visitProvidesInfo(Clazz clazz, ProvidesInfo providesInfo)
    {
        providesInfo.u2providesIndex     = dataInput.readUnsignedShort();

        // Read the provides withs.
        providesInfo.u2providesWithCount = dataInput.readUnsignedShort();
        providesInfo.u2providesWithIndex = readUnsignedShorts(providesInfo.u2providesWithCount);
    }


    // Implementations for AnnotationVisitor.

    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        // Read the annotation type.
        annotation.u2typeIndex = dataInput.readUnsignedShort();

        // Read the element value pairs.
        annotation.u2elementValuesCount = dataInput.readUnsignedShort();

        annotation.elementValues = new ElementValue[annotation.u2elementValuesCount];
        for (int index = 0; index < annotation.u2elementValuesCount; index++)
        {
            int u2elementNameIndex = dataInput.readUnsignedShort();
            ElementValue elementValue = createElementValue();
            elementValue.u2elementNameIndex = u2elementNameIndex;
            elementValue.accept(clazz, annotation, this);
            annotation.elementValues[index] = elementValue;
        }
    }


    // Implementations for TypeAnnotationVisitor.

    public void visitTypeAnnotation(Clazz clazz, TypeAnnotation typeAnnotation)
    {
        // Read the target info.
        TargetInfo targetInfo = createTargetInfo();
        targetInfo.accept(clazz, typeAnnotation, this);
        typeAnnotation.targetInfo = targetInfo;

        // Read the type path.
        int u1pathLength = dataInput.readUnsignedByte();

        typeAnnotation.typePath = new TypePathInfo[u1pathLength];
        for (int index = 0; index < u1pathLength; index++)
        {
            TypePathInfo typePathInfo = new TypePathInfo();
            visitTypePathInfo(clazz, typeAnnotation, typePathInfo);
            typeAnnotation.typePath[index] = typePathInfo;
        }

        // Read the actual annotation.
        visitAnnotation(clazz, typeAnnotation);
    }


    // Implementations for TargetInfoVisitor.

    public void visitTypeParameterTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypeParameterTargetInfo typeParameterTargetInfo)
    {
        typeParameterTargetInfo.u1typeParameterIndex = dataInput.readUnsignedByte();
    }


    public void visitSuperTypeTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, SuperTypeTargetInfo superTypeTargetInfo)
    {
        superTypeTargetInfo.u2superTypeIndex = dataInput.readUnsignedShort();
    }


    public void visitTypeParameterBoundTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypeParameterBoundTargetInfo typeParameterBoundTargetInfo)
    {
        typeParameterBoundTargetInfo.u1typeParameterIndex = dataInput.readUnsignedByte();
        typeParameterBoundTargetInfo.u1boundIndex         = dataInput.readUnsignedByte();
    }


    public void visitEmptyTargetInfo(Clazz clazz, Member member, TypeAnnotation typeAnnotation, EmptyTargetInfo emptyTargetInfo)
    {
    }


    public void visitFormalParameterTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, FormalParameterTargetInfo formalParameterTargetInfo)
    {
        formalParameterTargetInfo.u1formalParameterIndex = dataInput.readUnsignedByte();
    }


    public void visitThrowsTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, ThrowsTargetInfo throwsTargetInfo)
    {
        throwsTargetInfo.u2throwsTypeIndex = dataInput.readUnsignedShort();
    }


    public void visitLocalVariableTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo)
    {
        // Read the local variable target elements.
        localVariableTargetInfo.u2tableLength = dataInput.readUnsignedShort();

        localVariableTargetInfo.table = new LocalVariableTargetElement[localVariableTargetInfo.u2tableLength];
        for (int index = 0; index < localVariableTargetInfo.u2tableLength; index++)
        {
            LocalVariableTargetElement element = new LocalVariableTargetElement();
            visitLocalVariableTargetElement(clazz, method, codeAttribute, typeAnnotation, localVariableTargetInfo, element);
            localVariableTargetInfo.table[index] = element;
        }
    }


    public void visitCatchTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, CatchTargetInfo catchTargetInfo)
    {
        catchTargetInfo.u2exceptionTableIndex = dataInput.readUnsignedShort();
    }


    public void visitOffsetTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, OffsetTargetInfo offsetTargetInfo)
    {
        offsetTargetInfo.u2offset = dataInput.readUnsignedShort();
    }


    public void visitTypeArgumentTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, TypeArgumentTargetInfo typeArgumentTargetInfo)
    {
        typeArgumentTargetInfo.u2offset            = dataInput.readUnsignedShort();
        typeArgumentTargetInfo.u1typeArgumentIndex = dataInput.readUnsignedByte();
    }


    // Implementations for TypePathInfoVisitor.

    public void visitTypePathInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypePathInfo typePathInfo)
    {
        typePathInfo.u1typePathKind      = dataInput.readUnsignedByte();
        typePathInfo.u1typeArgumentIndex = dataInput.readUnsignedByte();
    }


    // Implementations for LocalVariableTargetElementVisitor.

    public void visitLocalVariableTargetElement(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo, LocalVariableTargetElement localVariableTargetElement)
    {
        localVariableTargetElement.u2startPC = dataInput.readShort();
        localVariableTargetElement.u2length  = dataInput.readShort();
        localVariableTargetElement.u2index   = dataInput.readShort();
    }


    // Implementations for ElementValueVisitor.

    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
        constantElementValue.u2constantValueIndex = dataInput.readUnsignedShort();
    }


    public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        enumConstantElementValue.u2typeNameIndex     = dataInput.readUnsignedShort();
        enumConstantElementValue.u2constantNameIndex = dataInput.readUnsignedShort();
    }


    public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        classElementValue.u2classInfoIndex = dataInput.readUnsignedShort();
    }


    public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        // Read the annotation.
        Annotation annotationValue = new Annotation();
        visitAnnotation(clazz, annotationValue);
        annotationElementValue.annotationValue = annotationValue;
    }


    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        // Read the element values.
        arrayElementValue.u2elementValuesCount = dataInput.readUnsignedShort();

        arrayElementValue.elementValues = new ElementValue[arrayElementValue.u2elementValuesCount];
        for (int index = 0; index < arrayElementValue.u2elementValuesCount; index++)
        {
            ElementValue elementValue = createElementValue();
            elementValue.accept(clazz, annotation, this);
            arrayElementValue.elementValues[index] = elementValue;
        }
    }


    // Small utility methods.

    private Constant createConstant()
    {
        int u1tag = dataInput.readUnsignedByte();

        switch (u1tag)
        {
            case Constant.INTEGER:             return new IntegerConstant();
            case Constant.FLOAT:               return new FloatConstant();
            case Constant.LONG:                return new LongConstant();
            case Constant.DOUBLE:              return new DoubleConstant();
            case Constant.PRIMITIVE_ARRAY:     return new PrimitiveArrayConstant();
            case Constant.STRING:              return new StringConstant();
            case Constant.UTF8:                return new Utf8Constant();
            case Constant.DYNAMIC:             return new DynamicConstant();
            case Constant.INVOKE_DYNAMIC:      return new InvokeDynamicConstant();
            case Constant.METHOD_HANDLE:       return new MethodHandleConstant();
            case Constant.FIELDREF:            return new FieldrefConstant();
            case Constant.METHODREF:           return new MethodrefConstant();
            case Constant.INTERFACE_METHODREF: return new InterfaceMethodrefConstant();
            case Constant.CLASS:               return new ClassConstant();
            case Constant.METHOD_TYPE:         return new MethodTypeConstant();
            case Constant.NAME_AND_TYPE:       return new NameAndTypeConstant();
            case Constant.MODULE:              return new ModuleConstant();
            case Constant.PACKAGE:             return new PackageConstant();

            default: throw new RuntimeException("Unknown constant type ["+u1tag+"] in constant pool");
        }
    }


    private Attribute createAttribute(Clazz clazz)
    {
        int u2attributeNameIndex = dataInput.readUnsignedShort();
        int u4attributeLength    = dataInput.readInt();
        String attributeName     = clazz.getString(u2attributeNameIndex);

        Attribute attribute =
            attributeName.equals(Attribute.BOOTSTRAP_METHODS)                            ? new BootstrapMethodsAttribute()                               :
            attributeName.equals(Attribute.SOURCE_FILE)                                  ? new SourceFileAttribute()                                     :
            attributeName.equals(Attribute.SOURCE_DIR)                                   ? new SourceDirAttribute()                                      :
            attributeName.equals(Attribute.SOURCE_DEBUG_EXTENSION)                       ? new SourceDebugExtensionAttribute(0, u4attributeLength, null) :
            attributeName.equals(Attribute.RECORD)                                       ? new RecordAttribute()                                         :
            attributeName.equals(Attribute.INNER_CLASSES)                                ? new InnerClassesAttribute()                                   :
            attributeName.equals(Attribute.ENCLOSING_METHOD)                             ? new EnclosingMethodAttribute()                                :
            attributeName.equals(Attribute.NEST_HOST)                                    ? new NestHostAttribute()                                       :
            attributeName.equals(Attribute.NEST_MEMBERS)                                 ? new NestMembersAttribute()                                    :
            attributeName.equals(Attribute.PERMITTED_SUBCLASSES)                         ? new PermittedSubclassesAttribute()                            :
            attributeName.equals(Attribute.DEPRECATED)                                   ? new DeprecatedAttribute()                                     :
            attributeName.equals(Attribute.SYNTHETIC)                                    ? new SyntheticAttribute()                                      :
            attributeName.equals(Attribute.SIGNATURE)                                    ? new SignatureAttribute()                                      :
            attributeName.equals(Attribute.CONSTANT_VALUE)                               ? new ConstantValueAttribute()                                  :
            attributeName.equals(Attribute.METHOD_PARAMETERS)                            ? new MethodParametersAttribute()                               :
            attributeName.equals(Attribute.EXCEPTIONS)                                   ? new ExceptionsAttribute()                                     :
            attributeName.equals(Attribute.CODE)                                         ? new CodeAttribute()                                           :
            attributeName.equals(Attribute.STACK_MAP)       && !ignoreStackMapAttributes ? new StackMapAttribute()                                       :
            attributeName.equals(Attribute.STACK_MAP_TABLE) && !ignoreStackMapAttributes ? new StackMapTableAttribute()                                  :
            attributeName.equals(Attribute.LINE_NUMBER_TABLE)                            ? new LineNumberTableAttribute()                                :
            attributeName.equals(Attribute.LOCAL_VARIABLE_TABLE)                         ? new LocalVariableTableAttribute()                             :
            attributeName.equals(Attribute.LOCAL_VARIABLE_TYPE_TABLE)                    ? new LocalVariableTypeTableAttribute()                         :
            attributeName.equals(Attribute.RUNTIME_VISIBLE_ANNOTATIONS)                  ? new RuntimeVisibleAnnotationsAttribute()                      :
            attributeName.equals(Attribute.RUNTIME_INVISIBLE_ANNOTATIONS)                ? new RuntimeInvisibleAnnotationsAttribute()                    :
            attributeName.equals(Attribute.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS)        ? new RuntimeVisibleParameterAnnotationsAttribute()             :
            attributeName.equals(Attribute.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS)      ? new RuntimeInvisibleParameterAnnotationsAttribute()           :
            attributeName.equals(Attribute.RUNTIME_VISIBLE_TYPE_ANNOTATIONS)             ? new RuntimeVisibleTypeAnnotationsAttribute()                  :
            attributeName.equals(Attribute.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)           ? new RuntimeInvisibleTypeAnnotationsAttribute()                :
            attributeName.equals(Attribute.ANNOTATION_DEFAULT)                           ? new AnnotationDefaultAttribute()                              :
            attributeName.equals(Attribute.MODULE)                                       ? new ModuleAttribute()                                         :
            attributeName.equals(Attribute.MODULE_MAIN_CLASS)                            ? new ModuleMainClassAttribute()                                :
            attributeName.equals(Attribute.MODULE_PACKAGES)                              ? new ModulePackagesAttribute()                                 :
                                                                                           new UnknownAttribute(u2attributeNameIndex, u4attributeLength);
        attribute.u2attributeNameIndex = u2attributeNameIndex;

        return attribute;
    }


    private StackMapFrame createStackMapFrame()
    {
        int u1tag = dataInput.readUnsignedByte();

        return
            u1tag < StackMapFrame.SAME_ONE_FRAME           ? new SameZeroFrame(u1tag) :
            u1tag < StackMapFrame.SAME_ONE_FRAME_EXTENDED  ? new SameOneFrame(u1tag)  :
            u1tag < StackMapFrame.LESS_ZERO_FRAME          ? new SameOneFrame(u1tag)  :
            u1tag < StackMapFrame.SAME_ZERO_FRAME_EXTENDED ? new LessZeroFrame(u1tag) :
            u1tag < StackMapFrame.MORE_ZERO_FRAME          ? new SameZeroFrame(u1tag) :
            u1tag < StackMapFrame.FULL_FRAME               ? new MoreZeroFrame(u1tag) :
                                                             new FullFrame();
    }


    private VerificationType createVerificationType()
    {
        int u1tag = dataInput.readUnsignedByte();

        switch (u1tag)
        {
            case VerificationType.INTEGER_TYPE:            return new IntegerType();
            case VerificationType.FLOAT_TYPE:              return new FloatType();
            case VerificationType.LONG_TYPE:               return new LongType();
            case VerificationType.DOUBLE_TYPE:             return new DoubleType();
            case VerificationType.TOP_TYPE:                return new TopType();
            case VerificationType.OBJECT_TYPE:             return new ObjectType();
            case VerificationType.NULL_TYPE:               return new NullType();
            case VerificationType.UNINITIALIZED_TYPE:      return new UninitializedType();
            case VerificationType.UNINITIALIZED_THIS_TYPE: return new UninitializedThisType();

            default: throw new RuntimeException("Unknown verification type ["+u1tag+"] in stack map frame");
        }
    }


    private TargetInfo createTargetInfo()
    {
        byte u1targetType = dataInput.readByte();

        switch (u1targetType)
        {
            case TargetInfo.TARGET_TYPE_PARAMETER_GENERIC_CLASS:
            case TargetInfo.TARGET_TYPE_PARAMETER_GENERIC_METHOD:            return new TypeParameterTargetInfo(u1targetType);
            case TargetInfo.TARGET_TYPE_EXTENDS:                             return new SuperTypeTargetInfo(u1targetType);
            case TargetInfo.TARGET_TYPE_BOUND_GENERIC_CLASS:
            case TargetInfo.TARGET_TYPE_BOUND_GENERIC_METHOD:                return new TypeParameterBoundTargetInfo(u1targetType);
            case TargetInfo.TARGET_TYPE_FIELD:
            case TargetInfo.TARGET_TYPE_RETURN:
            case TargetInfo.TARGET_TYPE_RECEIVER:                            return new EmptyTargetInfo(u1targetType);
            case TargetInfo.TARGET_TYPE_PARAMETER:                           return new FormalParameterTargetInfo(u1targetType);
            case TargetInfo.TARGET_TYPE_THROWS:                              return new ThrowsTargetInfo(u1targetType);
            case TargetInfo.TARGET_TYPE_LOCAL_VARIABLE:
            case TargetInfo.TARGET_TYPE_RESOURCE_VARIABLE:                   return new LocalVariableTargetInfo(u1targetType);
            case TargetInfo.TARGET_TYPE_CATCH:                               return new CatchTargetInfo(u1targetType);
            case TargetInfo.TARGET_TYPE_INSTANCE_OF:
            case TargetInfo.TARGET_TYPE_NEW:
            case TargetInfo.TARGET_TYPE_METHOD_REFERENCE_NEW:
            case TargetInfo.TARGET_TYPE_METHOD_REFERENCE:                    return new OffsetTargetInfo(u1targetType);
            case TargetInfo.TARGET_TYPE_CAST:
            case TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHODNew:
            case TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHOD:
            case TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHODReferenceNew:
            case TargetInfo.TARGET_TYPE_ARGUMENT_GENERIC_METHODReference:    return new TypeArgumentTargetInfo(u1targetType);

            default: throw new RuntimeException("Unknown annotation target type ["+u1targetType+"]");
        }
    }


    private ElementValue createElementValue()
    {
        int u1tag = dataInput.readUnsignedByte();

        switch (u1tag)
        {
            case TypeConstants.BOOLEAN:
            case TypeConstants.BYTE:
            case TypeConstants.CHAR:
            case TypeConstants.SHORT:
            case TypeConstants.INT:
            case TypeConstants.FLOAT:
            case TypeConstants.LONG:
            case TypeConstants.DOUBLE:
            case ElementValue.TAG_STRING_CONSTANT: return new ConstantElementValue((char)u1tag);
            case ElementValue.TAG_ENUM_CONSTANT:   return new EnumConstantElementValue();
            case ElementValue.TAG_CLASS:           return new ClassElementValue();
            case ElementValue.TAG_ANNOTATION:      return new AnnotationElementValue();
            case ElementValue.TAG_ARRAY:           return new ArrayElementValue();

            default: throw new IllegalArgumentException("Unknown element value tag ["+u1tag+"]");
        }
    }


    /**
     * Reads a list of unsigned shorts of the given size.
     */
    private int[] readUnsignedShorts(int size)
    {
        int[] values = new int[size];

        for (int index = 0; index < size; index++)
        {
            values[index] = dataInput.readUnsignedShort();
        }

        return values;
    }
}
