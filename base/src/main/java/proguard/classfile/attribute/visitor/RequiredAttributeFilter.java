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

/**
 * This {@link AttributeVisitor} delegates its visits to one of two other
 * {@link AttributeVisitor} instances, depending on whether the visited attribute
 * is strictly required or not.
 * <p/>
 * Stack map attributes and stack map table attributes are treated as optional.
 *
 * @author Eric Lafortune
 */
public class RequiredAttributeFilter
implements   AttributeVisitor
{
    private final AttributeVisitor requiredAttributeVisitor;
    private final AttributeVisitor optionalAttributeVisitor;


    /**
     * Creates a new RequiredAttributeFilter for visiting required attributes.
     * @param requiredAttributeVisitor   the visitor that will visit required
     *                                   attributes.
     */
    public RequiredAttributeFilter(AttributeVisitor requiredAttributeVisitor)
    {
        this(requiredAttributeVisitor, null);
    }


    /**
     * Creates a new RequiredAttributeFilter for visiting required and
     * optional attributes.
     * @param requiredAttributeVisitor the visitor that will visit required
     *                                 attributes.
     * @param optionalAttributeVisitor the visitor that will visit optional
     *                                 attributes.
     */
    public RequiredAttributeFilter(AttributeVisitor requiredAttributeVisitor,
                                   AttributeVisitor optionalAttributeVisitor)
    {
        this.requiredAttributeVisitor = requiredAttributeVisitor;
        this.optionalAttributeVisitor = optionalAttributeVisitor;
    }


    // Implementations for AttributeVisitor.

    public void visitUnknownAttribute(Clazz clazz, UnknownAttribute unknownAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitUnknownAttribute(clazz, unknownAttribute);
        }
    }


    public void visitBootstrapMethodsAttribute(Clazz clazz, BootstrapMethodsAttribute bootstrapMethodsAttribute)
    {
        if (requiredAttributeVisitor != null)
        {
            requiredAttributeVisitor.visitBootstrapMethodsAttribute(clazz, bootstrapMethodsAttribute);
        }
    }


    public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSourceFileAttribute(clazz, sourceFileAttribute);
        }
    }


    public void visitSourceDirAttribute(Clazz clazz, SourceDirAttribute sourceDirAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSourceDirAttribute(clazz, sourceDirAttribute);
        }
    }


    public void visitSourceDebugExtensionAttribute(Clazz                         clazz,
                                                   SourceDebugExtensionAttribute sourceDebugExtensionAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSourceDebugExtensionAttribute(clazz, sourceDebugExtensionAttribute);
        }
    }


    public void visitRecordAttribute(Clazz clazz, RecordAttribute recordAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRecordAttribute(clazz, recordAttribute);
        }
    }


    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitInnerClassesAttribute(clazz, innerClassesAttribute);
        }
    }


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitEnclosingMethodAttribute(clazz, enclosingMethodAttribute);
        }
    }


    public void visitNestHostAttribute(Clazz clazz, NestHostAttribute nestHostAttribute)
    {
        if (requiredAttributeVisitor != null)
        {
            requiredAttributeVisitor.visitNestHostAttribute(clazz, nestHostAttribute);
        }
    }


    public void visitNestMembersAttribute(Clazz clazz, NestMembersAttribute nestMembersAttribute)
    {
        if (requiredAttributeVisitor != null)
        {
            requiredAttributeVisitor.visitNestMembersAttribute(clazz, nestMembersAttribute);
        }
    }


    public void visitPermittedSubclassesAttribute(Clazz clazz, PermittedSubclassesAttribute permittedSubclassesAttribute)
    {
        if (requiredAttributeVisitor != null)
        {
            requiredAttributeVisitor.visitPermittedSubclassesAttribute(clazz, permittedSubclassesAttribute);
        }
    }


    public void visitModuleAttribute(Clazz clazz, ModuleAttribute moduleAttribute)
    {
        if (requiredAttributeVisitor != null)
        {
            requiredAttributeVisitor.visitModuleAttribute(clazz, moduleAttribute);
        }
    }


    public void visitModuleMainClassAttribute(Clazz clazz, ModuleMainClassAttribute moduleMainClassAttribute)
    {
        if (requiredAttributeVisitor != null)
        {
            requiredAttributeVisitor.visitModuleMainClassAttribute(clazz, moduleMainClassAttribute);
        }
    }


    public void visitModulePackagesAttribute(Clazz clazz, ModulePackagesAttribute modulePackagesAttribute)
    {
        if (requiredAttributeVisitor != null)
        {
            requiredAttributeVisitor.visitModulePackagesAttribute(clazz, modulePackagesAttribute);
        }
    }


    public void visitDeprecatedAttribute(Clazz clazz, DeprecatedAttribute deprecatedAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitDeprecatedAttribute(clazz, deprecatedAttribute);
        }
    }


    public void visitDeprecatedAttribute(Clazz clazz, Field field, DeprecatedAttribute deprecatedAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitDeprecatedAttribute(clazz, field, deprecatedAttribute);
        }
    }


    public void visitDeprecatedAttribute(Clazz clazz, Method method, DeprecatedAttribute deprecatedAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitDeprecatedAttribute(clazz, method, deprecatedAttribute);
        }
    }


    public void visitSyntheticAttribute(Clazz clazz, SyntheticAttribute syntheticAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSyntheticAttribute(clazz, syntheticAttribute);
        }
    }


    public void visitSyntheticAttribute(Clazz clazz, Field field, SyntheticAttribute syntheticAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSyntheticAttribute(clazz, field, syntheticAttribute);
        }
    }


    public void visitSyntheticAttribute(Clazz clazz, Method method, SyntheticAttribute syntheticAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSyntheticAttribute(clazz, method, syntheticAttribute);
        }
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSignatureAttribute(clazz, signatureAttribute);
        }
    }


    public void visitSignatureAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, SignatureAttribute signatureAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSignatureAttribute(clazz, recordComponentInfo, signatureAttribute);
        }
    }


    public void visitSignatureAttribute(Clazz clazz, Field field, SignatureAttribute signatureAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSignatureAttribute(clazz, field, signatureAttribute);
        }
    }


    public void visitSignatureAttribute(Clazz clazz, Method method, SignatureAttribute signatureAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitSignatureAttribute(clazz, method, signatureAttribute);
        }
    }


    public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        if (requiredAttributeVisitor != null)
        {
            requiredAttributeVisitor.visitConstantValueAttribute(clazz, field, constantValueAttribute);
        }
    }


    public void visitMethodParametersAttribute(Clazz clazz, Method method, MethodParametersAttribute exceptionsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitMethodParametersAttribute(clazz, method, exceptionsAttribute);
        }
    }


    public void visitExceptionsAttribute(Clazz clazz, Method method, ExceptionsAttribute exceptionsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitExceptionsAttribute(clazz, method, exceptionsAttribute);
        }
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        if (requiredAttributeVisitor != null)
        {
            requiredAttributeVisitor.visitCodeAttribute(clazz, method, codeAttribute);
        }
    }


    public void visitStackMapAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapAttribute stackMapAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitStackMapAttribute(clazz, method, codeAttribute, stackMapAttribute);
        }
    }


    public void visitStackMapTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapTableAttribute stackMapTableAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitStackMapTableAttribute(clazz, method, codeAttribute, stackMapTableAttribute);
        }
    }


    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitLineNumberTableAttribute(clazz, method, codeAttribute, lineNumberTableAttribute);
        }
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitLocalVariableTableAttribute(clazz, method, codeAttribute, localVariableTableAttribute);
        }
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitLocalVariableTypeTableAttribute(clazz, method, codeAttribute, localVariableTypeTableAttribute);
        }
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleAnnotationsAttribute(clazz, runtimeVisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleAnnotationsAttribute(clazz, recordComponentInfo, runtimeVisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, Field field, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleAnnotationsAttribute(clazz, field, runtimeVisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleAnnotationsAttribute(clazz, method, runtimeVisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleAnnotationsAttribute(clazz, runtimeInvisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleAnnotationsAttribute(clazz, recordComponentInfo, runtimeInvisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, Field field, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleAnnotationsAttribute(clazz, field, runtimeInvisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleAnnotationsAttribute(clazz, method, runtimeInvisibleAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleParameterAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleParameterAnnotationsAttribute(clazz, method, runtimeVisibleParameterAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleParameterAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleParameterAnnotationsAttribute(clazz, method, runtimeInvisibleParameterAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleTypeAnnotationsAttribute(clazz, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleTypeAnnotationsAttribute(clazz, recordComponentInfo, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Field field, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleTypeAnnotationsAttribute(clazz, field, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleTypeAnnotationsAttribute(clazz, method, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeVisibleTypeAnnotationsAttribute(clazz, method, codeAttribute, runtimeVisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, RecordComponentInfo recordComponentInfo, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, recordComponentInfo, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Field field, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, field, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, method, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitRuntimeInvisibleTypeAnnotationsAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitRuntimeInvisibleTypeAnnotationsAttribute(clazz, method, codeAttribute, runtimeInvisibleTypeAnnotationsAttribute);
        }
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        if (optionalAttributeVisitor != null)
        {
            optionalAttributeVisitor.visitAnnotationDefaultAttribute(clazz, method, annotationDefaultAttribute);
        }
    }
}
