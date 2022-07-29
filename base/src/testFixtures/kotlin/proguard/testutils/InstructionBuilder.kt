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

package proguard.testutils

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
