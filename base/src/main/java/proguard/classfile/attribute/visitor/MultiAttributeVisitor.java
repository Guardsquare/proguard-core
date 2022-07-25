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
package proguard.classfile.attribute.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.module.*;
import proguard.classfile.attribute.preverification.*;
import proguard.util.ArrayUtil;

/**
 * This {@link AttributeVisitor} delegates all visits to each {@link AttributeVisitor}
 * in a given list.
 *
 * @author Eric Lafortune
 */
public class MultiAttributeVisitor implements AttributeVisitor
{
    private AttributeVisitor[] attributeVisitors;
    private int                attributeVisitorCount;


    public MultiAttributeVisitor()
    {
        this.attributeVisitors = new AttributeVisitor[16];
    }


    public MultiAttributeVisitor(AttributeVisitor... attributeVisitors)
    {
        this.attributeVisitors     = attributeVisitors;
        this.attributeVisitorCount = attributeVisitors.length;
    }


    public void addAttributeVisitor(AttributeVisitor attributeVisitor)
    {
        attributeVisitors =
            ArrayUtil.add(attributeVisitors,
                          attributeVisitorCount++,
                          attributeVisitor);
    }


    // Implementations for AttributeVisitor.

    public void visitUnknownAttribute(Clazz clazz, UnknownAttribute unknownAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitUnknownAttribute(clazz, unknownAttribute);
        }
    }

    public void visitBootstrapMethodsAttribute(Clazz clazz, BootstrapMethodsAttribute bootstrapMethodsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitBootstrapMethodsAttribute(clazz, bootstrapMethodsAttribute);
        }
    }


    public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSourceFileAttribute(clazz, sourceFileAttribute);
        }
    }


    public void visitSourceDirAttribute(Clazz clazz, SourceDirAttribute sourceDirAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSourceDirAttribute(clazz, sourceDirAttribute);
        }
    }


    public void visitSourceDebugExtensionAttribute(Clazz clazz, SourceDebugExtensionAttribute sourceDebugExtensionAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSourceDebugExtensionAttribute(clazz, sourceDebugExtensionAttribute);
        }
    }


    public void visitRecordAttribute(Clazz clazz, RecordAttribute recordAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRecordAttribute(clazz, recordAttribute);
        }
    }


    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitInnerClassesAttribute(clazz, innerClassesAttribute);
        }
    }


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitEnclosingMethodAttribute(clazz, enclosingMethodAttribute);
        }
    }


    public void visitNestHostAttribute(Clazz clazz, NestHostAttribute nestHostAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitNestHostAttribute(clazz, nestHostAttribute);
        }
    }


    public void visitNestMembersAttribute(Clazz clazz, NestMembersAttribute nestMembersAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitNestMembersAttribute(clazz, nestMembersAttribute);
        }
    }


    public void visitPermittedSubclassesAttribute(Clazz clazz, PermittedSubclassesAttribute permittedSubclassesAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitPermittedSubclassesAttribute(clazz, permittedSubclassesAttribute);
        }
    }


    public void visitModuleAttribute(Clazz clazz, ModuleAttribute moduleAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitModuleAttribute(clazz, moduleAttribute);
        }
    }


    public void visitModuleMainClassAttribute(Clazz clazz, ModuleMainClassAttribute moduleMainClassAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitModuleMainClassAttribute(clazz, moduleMainClassAttribute);
        }
    }


    public void visitModulePackagesAttribute(Clazz clazz, ModulePackagesAttribute modulePackagesAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitModulePackagesAttribute(clazz, modulePackagesAttribute);
        }
    }


    public void visitDeprecatedAttribute(Clazz clazz, DeprecatedAttribute deprecatedAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitDeprecatedAttribute(clazz, deprecatedAttribute);
        }
    }


    public void visitDeprecatedAttribute(Clazz clazz, Field field, DeprecatedAttribute deprecatedAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitDeprecatedAttribute(clazz, field, deprecatedAttribute);
        }
    }


    public void visitDeprecatedAttribute(Clazz clazz, Method method, DeprecatedAttribute deprecatedAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitDeprecatedAttribute(clazz, method, deprecatedAttribute);
        }
    }


    public void visitSyntheticAttribute(Clazz clazz, SyntheticAttribute syntheticAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSyntheticAttribute(clazz, syntheticAttribute);
        }
    }


    public void visitSyntheticAttribute(Clazz clazz, Field field, SyntheticAttribute syntheticAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSyntheticAttribute(clazz, field, syntheticAttribute);
        }
    }


    public void visitSyntheticAttribute(Clazz clazz, Method method, SyntheticAttribute syntheticAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSyntheticAttribute(clazz, method, syntheticAttribute);
        }
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute syntheticAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSignatureAttribute(clazz, syntheticAttribute);
        }
    }


    public void visitSignatureAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, SignatureAttribute syntheticAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSignatureAttribute(clazz, recordComponentInfo, syntheticAttribute);
        }
    }


    public void visitSignatureAttribute(Clazz clazz, Field field, SignatureAttribute syntheticAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSignatureAttribute(clazz, field, syntheticAttribute);
        }
    }


    public void visitSignatureAttribute(Clazz clazz, Method method, SignatureAttribute syntheticAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitSignatureAttribute(clazz, method, syntheticAttribute);
        }
    }


    public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitConstantValueAttribute(clazz, field, constantValueAttribute);
        }
    }


    public void visitMethodParametersAttribute(Clazz clazz, Method method, MethodParametersAttribute methodParametersAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitMethodParametersAttribute(clazz, method, methodParametersAttribute);
        }
    }


    public void visitExceptionsAttribute(Clazz clazz, Method method, ExceptionsAttribute exceptionsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitExceptionsAttribute(clazz, method, exceptionsAttribute);
        }
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitCodeAttribute(clazz, method, codeAttribute);
        }
    }


    public void visitStackMapAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapAttribute stackMapAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitStackMapAttribute(clazz, method, codeAttribute, stackMapAttribute);
        }
    }


    public void visitStackMapTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapTableAttribute stackMapTableAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitStackMapTableAttribute(clazz, method, codeAttribute, stackMapTableAttribute);
        }
    }


    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitLineNumberTableAttribute(clazz, method, codeAttribute, lineNumberTableAttribute);
        }
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitLocalVariableTableAttribute(clazz, method, codeAttribute, localVariableTableAttribute);
        }
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitLocalVariableTypeTableAttribute(clazz, method, codeAttribute, localVariableTypeTableAttribute);
        }
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleAnnotationsAttribute(clazz, runtimeVisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleAnnotationsAttribute(clazz, recordComponentInfo, runtimeVisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, Field field, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleAnnotationsAttribute(clazz, field, runtimeVisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleAnnotationsAttribute(clazz, method, runtimeVisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleAnnotationsAttribute(clazz, runtimeInvisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleAnnotationsAttribute(clazz, recordComponentInfo, runtimeInvisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, Field field, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleAnnotationsAttribute(clazz, field, runtimeInvisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleAnnotationsAttribute(clazz, method, runtimeInvisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleParameterAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleParameterAnnotationsAttribute(clazz, method, runtimeVisibleParameterAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleParameterAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleParameterAnnotationsAttribute(clazz, method, runtimeInvisibleParameterAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleTypeAnnotationsAttribute(clazz, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleTypeAnnotationsAttribute(clazz, recordComponentInfo, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Field field, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleTypeAnnotationsAttribute(clazz, field, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleTypeAnnotationsAttribute(clazz, method, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeVisibleTypeAnnotationsAttribute(clazz, method, codeAttribute, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, recordComponentInfo, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Field field, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, field, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, method, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        for (int index = 0; index < attributeVisitors.length; index++)
        {
            attributeVisitors[index].visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, method, codeAttribute, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        for (int index = 0; index < attributeVisitorCount; index++)
        {
            attributeVisitors[index].visitAnnotationDefaultAttribute(clazz, method, annotationDefaultAttribute);
        }
    }
}
