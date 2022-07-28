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

package proguard.analysis.cpa.jvm.util;

import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.ClassUtil;

/**
 * Utility class to retrieve information about a {@link Call}.
 *
 * @author Carlo Alberto Pozzoli
 */
public class CallUtil
{

    // hidden constructor
    private CallUtil()
    {
    }

    /**
     * Calculates the argument size of the called method.
     */
    public static int calculateArgumentSize(Call call)
    {
        return ClassUtil.internalMethodParameterSize(call.getTarget().descriptor.toString(),
                                                     call.invocationOpcode == Instruction.OP_INVOKESTATIC || call.invocationOpcode == Instruction.OP_INVOKEDYNAMIC);
    }
}
