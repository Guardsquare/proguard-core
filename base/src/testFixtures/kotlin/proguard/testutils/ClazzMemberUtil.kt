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

import io.kotest.matchers.EqualityMatcherResult
import io.kotest.matchers.Matcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import proguard.classfile.Clazz
import proguard.classfile.Member
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.instruction.visitor.InstructionVisitor
import proguard.classfile.util.ClassUtil.externalFullMethodDescription

data class ClazzMemberPair(val clazz: Clazz, val member: Member) {
    override fun toString(): String =
        externalFullMethodDescription(clazz.name, member.accessFlags, member.getName(clazz), member.getDescriptor(clazz))
}

// Allows creation of a `ClassMemberPair` with the following:
// val pair = Clazz and Member
infix fun Clazz.and(member: Member) = ClazzMemberPair(this, member)

fun ClazzMemberPair.instructionsAccept(visitor: InstructionVisitor) =
    member.accept(clazz, AllAttributeVisitor(AllInstructionVisitor(visitor)))

fun Clazz.findMethod(name: String) = findMethod(name, null)

private fun matchInstructions(builder: InstructionBuilder.() -> InstructionBuilder) = Matcher<ClazzMemberPair> { value ->
    EqualityMatcherResult(
        passed = with(value) {
            val (constants, instructions) = builder(InstructionBuilder())
            val matcher = InstructionMatcher(constants, instructions)

            class EarlyReturn : RuntimeException()

            try {
                instructionsAccept(object : InstructionVisitor {
                    override fun visitAnyInstruction(clazz: Clazz, method: Method, codeAttribute: CodeAttribute, offset: Int, instruction: Instruction) {
                        if (matcher.isMatching) throw EarlyReturn()
                        instruction.accept(clazz, method, codeAttribute, offset, matcher)
                    }
                })
            } catch (_: EarlyReturn) { }

            matcher.isMatching
        },
        actual = object {
            override fun toString(): String = with(value) {
                val instructions = MethodInstructionCollector.getMethodInstructions(clazz, member as Method)
                var offset = 0
                instructions.joinToString("\n") {
                    val thisOffset = offset
                    offset += it.length(offset)
                    it.toString(clazz, thisOffset)
                }
            }
        },
        expected = object {
            override fun toString(): String {
                val (constants, instructions) = builder(InstructionBuilder())
                val clazz = ProgramClass().apply { constantPool = constants }
                var offset = 0
                return instructions.joinToString("\n") {
                    val thisOffset = offset
                    offset += it.length(offset)
                    it.toString(clazz, thisOffset)
                }
            }
        },
        failureMessageFn = { "Instructions should match" },
        negatedFailureMessageFn = { "Instructions should not match" },
    )
}

@Deprecated("Use shouldMatch or shouldNotMatch instead")
fun ClazzMemberPair.match(builder: InstructionBuilder.() -> InstructionBuilder): Boolean = matchInstructions(builder).test(this).passed()

fun ClazzMemberPair.shouldMatch(builder: InstructionBuilder.() -> InstructionBuilder) {
    this should matchInstructions(builder)
}

fun ClazzMemberPair.shouldNotMatch(builder: InstructionBuilder.() -> InstructionBuilder) {
    this shouldNot matchInstructions(builder)
}
