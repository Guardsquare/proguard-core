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
package proguard.util.kotlin.asserter.constraint;

import static proguard.classfile.kotlin.KotlinConstants.DEFAULT_IMPLEMENTATIONS_SUFFIX;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_FILE_FACADE;
import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_MULTI_FILE_CLASS_PART;
import static proguard.classfile.kotlin.KotlinConstants.REFLECTION;
import static proguard.classfile.kotlin.KotlinConstants.WHEN_MAPPINGS_SUFFIX;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinSyntheticClassKindMetadata;
import proguard.classfile.kotlin.reflect.CallableReferenceInfo;
import proguard.classfile.kotlin.reflect.FunctionReferenceInfo;
import proguard.classfile.kotlin.reflect.LocalVariableReferenceInfo;
import proguard.classfile.kotlin.reflect.PropertyReferenceInfo;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.util.kotlin.asserter.AssertUtil;

public class SyntheticClassIntegrity
extends      AbstractKotlinMetadataConstraint
implements   KotlinMetadataVisitor

{

    @Override
    public void visitKotlinSyntheticClassMetadata(Clazz                            clazz,
                                                  KotlinSyntheticClassKindMetadata kotlinSyntheticClassKindMetadata)
    {
        AssertUtil util = new AssertUtil("Synthetic class", reporter, programClassPool, libraryClassPool);

        switch (kotlinSyntheticClassKindMetadata.flavor)
        {
            case DEFAULT_IMPLS:
                if (!clazz.getName().endsWith(DEFAULT_IMPLEMENTATIONS_SUFFIX))
                {
                    reporter.report("Default implementations class name does not end with " + DEFAULT_IMPLEMENTATIONS_SUFFIX);
                }
                break;
            case WHEN_MAPPINGS:
                if (!clazz.getName().endsWith(WHEN_MAPPINGS_SUFFIX))
                {
                    reporter.report("When mappings class name does not end with " + WHEN_MAPPINGS_SUFFIX);
                }
                break;
            case LAMBDA:
                try {
                    Integer.parseInt(clazz.getName().substring(clazz.getName().lastIndexOf("$") + 1));
                }
                catch (NumberFormatException e)
                {
                    reporter.report( "Lambda inner classname is not an integer.");
                }

                if (kotlinSyntheticClassKindMetadata.functions.isEmpty())
                {
                    reporter.report("Lambda class has no functions");
                }
                else if (kotlinSyntheticClassKindMetadata.functions.size() > 1)
                {
                    reporter.report("Lambda class has multiple functions");
                }
                break;
            case REGULAR:
        }

        if (clazz.extendsOrImplements(REFLECTION.CALLABLE_REFERENCE_CLASS_NAME))
        {
            util.setParentElement("Synthetic callable reference class");
            util.reportIfNullReference("callable reference info", kotlinSyntheticClassKindMetadata.callableReferenceInfo);

            kotlinSyntheticClassKindMetadata.callableReferenceInfoAccept(new CallableReferenceInfoVisitor()
            {
                @Override
                public void visitAnyCallableReferenceInfo(CallableReferenceInfo callableReferenceInfo)
                {
                    util.setParentElement("Synthetic callable reference (" + callableReferenceInfo.getClass().getSimpleName() + ")");
                    util.reportIfNull("name", callableReferenceInfo.getName());
                    util.reportIfNull("signature", callableReferenceInfo.getSignature());
                }

                @Override
                public void visitFunctionReferenceInfo(FunctionReferenceInfo functionReferenceInfo)
                {
                    visitAnyCallableReferenceInfo(functionReferenceInfo);
                    checkOwner(functionReferenceInfo);
                }

                @Override
                public void visitPropertyReferenceInfo(PropertyReferenceInfo propertyReferenceInfo)
                {
                    visitAnyCallableReferenceInfo(propertyReferenceInfo);
                    checkOwner(propertyReferenceInfo);
                }

                @Override
                public void visitLocalVariableReferenceInfo(LocalVariableReferenceInfo localVariableReferenceInfo)
                {
                    visitAnyCallableReferenceInfo(localVariableReferenceInfo);
                    checkOwner(localVariableReferenceInfo);
                }

                private void checkOwner(CallableReferenceInfo callableReferenceInfo)
                {
                    // We don't check this for JavaReferenceInfo.

                    util.reportIfNull("owner", callableReferenceInfo.getOwner());
                    if (callableReferenceInfo.getOwner() != null)
                    {
                        // We need the module to update the getOwner() method for file facades and multi-file class parts.
                        if (callableReferenceInfo.getOwner().k == METADATA_KIND_FILE_FACADE ||
                            callableReferenceInfo.getOwner().k == METADATA_KIND_MULTI_FILE_CLASS_PART)
                        {
                            util.reportIfNull("referenced module", callableReferenceInfo.getOwner().referencedModule);
                        }
                    }
                }
            });
        }
    }
}
