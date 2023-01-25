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

import proguard.classfile.*;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;


/**
 * This {@link ConstantVisitor} delegates its visits to another given {@link ConstantVisitor},
 * but only when the visited constant has the proper processing flags.
 */
public class ConstantProcessingFlagFilter
implements   ConstantVisitor
{
    private final int             requiredSetProcessingFlags;
    private final int             requiredUnsetProcessingFlags;
    private final ConstantVisitor constantVisitor;


    /**
     * Creates a new ConstantProcessingFlagFilter.
     *
     * @param requiredSetProcessingFlags   the constant processing flags that should be set.
     * @param requiredUnsetProcessingFlags the constant processing flags that should be unset.
     * @param constantVisitor              the <code>ConstantVisitor</code> to which visits will be delegated.
     */
    public ConstantProcessingFlagFilter(int             requiredSetProcessingFlags,
                                        int             requiredUnsetProcessingFlags,
                                        ConstantVisitor constantVisitor)
    {
        this.requiredSetProcessingFlags   = requiredSetProcessingFlags;
        this.requiredUnsetProcessingFlags = requiredUnsetProcessingFlags;
        this.constantVisitor              = constantVisitor;
    }


    // Implementations for ConstantVisitor.

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant)
    {
        if (accepted(constant.getProcessingFlags()))
        {
            constant.accept(clazz, constantVisitor);
        }
    }


    // Small utility methods.

    private boolean accepted(int processingFlags)
    {
        return (requiredSetProcessingFlags   & ~processingFlags) == 0 &&
               (requiredUnsetProcessingFlags &  processingFlags) == 0;
    }
}
