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
package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.visitor.filter.KotlinFunctionFilter;

import static proguard.classfile.TypeConstants.INNER_CLASS_SEPARATOR;

/**
 * This {@link KotlinMetadataVisitor} travels to the function of the provided
 * anonymous object origin and delegates to the given {@link KotlinFunctionVisitor}.
 * 
 * e.g. kotlin/properties/Delegates$observable$1 -> visits the observable.
 *
 * @author James Hamilton
 */
public class KotlinClassToInlineOriginFunctionVisitor
implements   KotlinMetadataVisitor
{
    private final String anonymousObjectOriginName;
    private final KotlinFunctionVisitor kotlinFunctionVisitor;

    public KotlinClassToInlineOriginFunctionVisitor(String                anonymousObjectOriginName,
                                                    KotlinFunctionVisitor kotlinFunctionVisitor)
    {
        this.anonymousObjectOriginName = anonymousObjectOriginName;
        this.kotlinFunctionVisitor     = kotlinFunctionVisitor;
    }

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) { }


    @Override
    public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
    {
        if (this.anonymousObjectOriginName == null)
        {
            return;
        }

        int index    = this.anonymousObjectOriginName.indexOf(INNER_CLASS_SEPARATOR) + 1;
        int endIndex = this.anonymousObjectOriginName.indexOf(INNER_CLASS_SEPARATOR, index);
        if (endIndex == -1)
        {
            endIndex = this.anonymousObjectOriginName.length();
        }

        String funName = this.anonymousObjectOriginName.substring(index, endIndex);

        kotlinDeclarationContainerMetadata.functionsAccept(clazz,
            new KotlinFunctionFilter(fun -> fun.name.equals(funName),
                this.kotlinFunctionVisitor));
    }
}
