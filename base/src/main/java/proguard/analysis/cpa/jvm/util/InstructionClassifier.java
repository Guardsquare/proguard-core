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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import proguard.classfile.instruction.Instruction;

/**
 * Util for operations involving {@link Instruction}.
 *
 * @author Carlo Alberto Pozzoli
 */
public class InstructionClassifier
{

    private static final Set<Byte> returnInstructions = Stream.of(
        Instruction.OP_RETURN,
        Instruction.OP_IRETURN,
        Instruction.OP_LRETURN,
        Instruction.OP_FRETURN,
        Instruction.OP_DRETURN,
        Instruction.OP_ARETURN
    ).collect(Collectors.toSet());

    private static final Set<Byte> invokeInstructions = Stream.of(
        Instruction.OP_INVOKEDYNAMIC,
        Instruction.OP_INVOKESTATIC,
        Instruction.OP_INVOKESPECIAL,
        Instruction.OP_INVOKEVIRTUAL,
        Instruction.OP_INVOKEINTERFACE
    ).collect(Collectors.toSet());

    private static final Set<Byte> longShiftInstructions = Stream.of(
        Instruction.OP_LSHL,
        Instruction.OP_LSHR,
        Instruction.OP_LUSHR
    ).collect(Collectors.toSet());

    public static boolean isReturn(byte opcode)
    {
        return returnInstructions.contains(opcode);
    }

    /**
     * Checks if the opcode is nonvoid return.
     */
    public static boolean isTypedReturn(byte opcode)
    {
        return returnInstructions.contains(opcode) && opcode != Instruction.OP_RETURN;
    }

    public static boolean isInvoke(byte opcode)
    {
        return invokeInstructions.contains(opcode);
    }

    public static boolean isLongShift(byte opcode)
    {
        return longShiftInstructions.contains(opcode);
    }
}
