/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 */

package testutils

import proguard.classfile.constant.Constant
import proguard.classfile.editor.InstructionSequenceBuilder
import proguard.classfile.instruction.Instruction

typealias InstructionBuilder = InstructionSequenceBuilder

// allows the following:
//     val (constants, instructions) = builder
// instead of
//     val constants = builder.constants()
//     val instructions = builder.instructions()
operator fun InstructionBuilder.component1(): Array<Constant> = this.constants()
operator fun InstructionBuilder.component2(): Array<Instruction> = this.instructions()
