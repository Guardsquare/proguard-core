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

package proguard.analysis.cpa

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import proguard.analysis.cpa.jvm.cfa.JvmCfa
import proguard.analysis.cpa.jvm.cfa.edges.JvmAssumeCaseCfaEdge
import proguard.analysis.cpa.jvm.cfa.edges.JvmAssumeCfaEdge
import proguard.analysis.cpa.jvm.cfa.edges.JvmAssumeDefaultCfaEdge
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.cfa.nodes.JvmUnknownCfaNode
import proguard.analysis.cpa.jvm.cfa.visitors.JvmIntraproceduralCfaFillerAllInstructionVisitor
import proguard.classfile.MethodSignature
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.instruction.Instruction
import proguard.testutils.cpa.NamedClass
import proguard.testutils.cpa.NamedMember

class JvmIntraproceduralCfaFillerVisitorTest : FreeSpec({

    fun checkNode(node: JvmCfaNode, offset: Int, isExitNode: Boolean, enteringEdgesSize: Int, leavingEdgesSize: Int, code: ByteArray) {
        node.offset shouldBe offset
        node.isExitNode shouldBe isExitNode
        node.enteringEdges.size shouldBe enteringEdgesSize
        node.leavingEdges.size shouldBe leavingEdgesSize
        for (edge in node.leavingEdges) {
            (edge as JvmInstructionCfaEdge).instruction.opcode shouldBe code[offset]
        }
    }

    "Branch test" - {

        val code = byteArrayOf(
            Instruction.OP_IFICMPNE, 0.toByte(), 5.toByte(), // if_icmpne (+5)
            Instruction.OP_BIPUSH, 42.toByte(), //              bipush 42
            Instruction.OP_BIPUSH, 43.toByte(), //              bipush 43
            Instruction.OP_IRETURN //                           ireturn
        )

        val clazz = NamedClass("com/test/CfaTest")
        val method = NamedMember("testCfa", "()V")

        val cfa = JvmCfa()
        val signature = MethodSignature(
            clazz.name,
            method.memberName,
            method.descriptor
        )

        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val attributeVisitor = JvmIntraproceduralCfaFillerAllInstructionVisitor(cfa)
        attributeVisitor.visitCodeAttribute(clazz, method, codeAttribute)

        /*
                0
           [t]↙   ↘[f]
             5  ←  3
             ↓
             7
             ↓
            -1
        */
        "Correct graph" {
            cfa.getFunctionNodes(signature).size shouldBe 5 // number of instructions + 1 for the exit node
            cfa.getFunctionCatchNodes(signature).size shouldBe 0 // no exception table

            val n0 = cfa.getFunctionNode(signature, 0)
            checkNode(n0, 0, false, 0, 2, code)

            val notTakenEdges = n0.leavingEdges.filter { e -> !(e as JvmAssumeCfaEdge).isSatisfied }
            notTakenEdges.size shouldBe 1
            val notTakenEdge = notTakenEdges[0]
            val takenEdges = n0.leavingEdges.filter { e -> (e as JvmAssumeCfaEdge).isSatisfied }
            takenEdges.size shouldBe 1
            val takenEdge = takenEdges[0]

            val n3 = notTakenEdge.target
            checkNode(n3, 3, false, 1, 1, code)
            val e0 = n3.leavingEdges[0]

            val n5 = takenEdge.target
            e0.target shouldBeSameInstanceAs n5
            checkNode(n5, 5, false, 2, 1, code)
            val e1 = n5.leavingEdges[0]

            val n7 = e1.target
            checkNode(n7, 7, false, 1, 1, code)
            val e2 = n7.leavingEdges[0]

            val exitNode = e2.target
            checkNode(exitNode, -1, true, 1, 0, code)
        }

        cfa.clear()
    }

    "Goto test" - {

        val code = byteArrayOf(
            Instruction.OP_GOTO, 0.toByte(), 5.toByte(), // goto (+5)
            Instruction.OP_BIPUSH, 42.toByte(), //          bipush 42
            Instruction.OP_IRETURN //                       ireturn
        )

        val clazz = NamedClass("com/test/CfaTest")
        val method = NamedMember("testCfa", "()V")

        val cfa = JvmCfa()
        val signature = MethodSignature(
            clazz.name,
            method.memberName,
            method.descriptor
        )

        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val attributeVisitor = JvmIntraproceduralCfaFillerAllInstructionVisitor(cfa)
        attributeVisitor.visitCodeAttribute(clazz, method, codeAttribute)

        /*
                0
              ↙
             5  ←  3
             ↓
            -1
        */
        "Correct graph" {
            cfa.getFunctionNodes(signature).size shouldBe 4 // number of instructions + 1 for the exit node
            cfa.getFunctionCatchNodes(signature).size shouldBe 0 // no exception table

            val n0 = cfa.getFunctionNode(signature, 0)
            checkNode(n0, 0, false, 0, 1, code)
            val e0 = n0.leavingEdges[0]

            val n3 = cfa.getFunctionNode(signature, 3)
            checkNode(n3, 3, false, 0, 1, code)
            val e1 = n3.leavingEdges[0]

            val n5 = e0.target
            e1.target shouldBeSameInstanceAs n5
            checkNode(n5, 5, false, 2, 1, code)
            val e2 = n5.leavingEdges[0]

            val exitNode = e2.target
            checkNode(exitNode, -1, true, 1, 0, code)
        }

        cfa.clear()
    }

    "Return test" - {

        val code = byteArrayOf(
            Instruction.OP_BIPUSH, 42.toByte(), // bipush 42
            Instruction.OP_IRETURN, //             ireturn
            Instruction.OP_BIPUSH, 43.toByte(), // bipush 43
            Instruction.OP_IRETURN //              ireturn
        )

        val clazz = NamedClass("com/test/CfaTest")
        val method = NamedMember("testCfa", "()V")

        val cfa = JvmCfa()
        val signature = MethodSignature(
            clazz.name,
            method.memberName,
            method.descriptor
        )

        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val attributeVisitor = JvmIntraproceduralCfaFillerAllInstructionVisitor(cfa)
        attributeVisitor.visitCodeAttribute(clazz, method, codeAttribute)

        /*
             0     3
             ↓     ↓
             2     5
              ↘   ↙
               -1
        */
        "Correct graph" {
            cfa.getFunctionNodes(signature).size shouldBe 5 // number of instructions + 1 for the exit node
            cfa.getFunctionCatchNodes(signature).size shouldBe 0 // no exception table

            val n0 = cfa.getFunctionNode(signature, 0)
            checkNode(n0, 0, false, 0, 1, code)
            val e0 = n0.leavingEdges[0]

            val n2 = e0.target
            checkNode(n2, 2, false, 1, 1, code)
            val e1 = n2.leavingEdges[0]

            val n3 = cfa.getFunctionNode(signature, 3)
            checkNode(n3, 3, false, 0, 1, code)
            val e2 = n3.leavingEdges[0]

            val n5 = e2.target
            checkNode(n5, 5, false, 1, 1, code)
            val e3 = n5.leavingEdges[0]

            val exitNode = e1.target
            e3.target shouldBeSameInstanceAs exitNode
            checkNode(exitNode, -1, true, 2, 0, code)
        }

        cfa.clear()
    }

    "Ret test" - {

        val code = byteArrayOf(
            Instruction.OP_RET, 1.toByte(), //     ret 1
            Instruction.OP_BIPUSH, 42.toByte(), // bipush 42
            Instruction.OP_IRETURN //              ireturn
        )

        val clazz = NamedClass("com/test/CfaTest")
        val method = NamedMember("testCfa", "()V")

        val cfa = JvmCfa()
        val signature = MethodSignature(
            clazz.name,
            method.memberName,
            method.descriptor
        )

        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val attributeVisitor = JvmIntraproceduralCfaFillerAllInstructionVisitor(cfa)
        attributeVisitor.visitCodeAttribute(clazz, method, codeAttribute)

        /*
             0     2
             ↓     ↓
            unk    4
                   ↓
                  -1
        */
        "Correct graph" {
            cfa.getFunctionNodes(signature).size shouldBe 4 // number of instructions + 1 for the exit node
            cfa.getFunctionCatchNodes(signature).size shouldBe 0 // no exception table

            val n0 = cfa.getFunctionNode(signature, 0)
            checkNode(n0, 0, false, 0, 1, code)

            val n2 = cfa.getFunctionNode(signature, 2)
            checkNode(n2, 2, false, 0, 1, code)

            val n4 = cfa.getFunctionNode(signature, 4)
            checkNode(n4, 4, false, 1, 1, code)
            val e2 = n4.leavingEdges[0]

            val exitNode = e2.target
            checkNode(exitNode, -1, true, 1, 0, code)

            val unk = JvmUnknownCfaNode.INSTANCE
            checkNode(unk, -1, false, 1, 0, code)
        }

        cfa.clear()
    }

    "Switch test" - {

        val code = byteArrayOf(
            Instruction.OP_LOOKUPSWITCH, 0.toByte(), 0.toByte(), 0.toByte(), // lookupswitch (3 bits padding):
            0.toByte(), 0.toByte(), 0.toByte(), 41.toByte(), //                     default (+41)
            0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), //                      3 cases
            0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), //                      case 1
            0.toByte(), 0.toByte(), 0.toByte(), 36.toByte(), //                     (+36)
            0.toByte(), 0.toByte(), 0.toByte(), 42.toByte(), //                     case 42
            0.toByte(), 0.toByte(), 0.toByte(), 36.toByte(), //                     (+36)
            0.toByte(), 0.toByte(), 0.toByte(), 100.toByte(), //                    case 100
            0.toByte(), 0.toByte(), 0.toByte(), 39.toByte(), //                     (+39)
            Instruction.OP_GOTO, 0.toByte(), 7.toByte(), //                     goto (+7)
            Instruction.OP_BIPUSH, 42.toByte(), //                              bipush 42
            Instruction.OP_BIPUSH, 43.toByte(), //                              bipush 43
            Instruction.OP_IRETURN //                                           ireturn
        )

        val clazz = NamedClass("com/test/CfaTest")
        val method = NamedMember("testCfa", "()V")

        val cfa = JvmCfa()
        val signature = MethodSignature(
            clazz.name,
            method.memberName,
            method.descriptor
        )

        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val attributeVisitor = JvmIntraproceduralCfaFillerAllInstructionVisitor(cfa)
        attributeVisitor.visitCodeAttribute(clazz, method, codeAttribute)

        /*
                     0
           ↙[1]↙[42] ↓[100]   ↘[def]
         36         39    →    41
             ↘              ↙
                    43
                     ↓
                    -1
        */
        "Correct graph" {
            cfa.getFunctionNodes(signature).size shouldBe 6 // number of instructions + 1 for the exit node
            cfa.getFunctionCatchNodes(signature).size shouldBe 0 // no exception table

            val n0 = cfa.getFunctionNode(signature, 0)
            checkNode(n0, 0, false, 0, 4, code)

            val defaultEdges = n0.leavingEdges.filterIsInstance<JvmAssumeDefaultCfaEdge>()
            defaultEdges.size shouldBe 1
            val defaultEdge = defaultEdges[0]
            val caseEdges = n0.leavingEdges.filterIsInstance<JvmAssumeCaseCfaEdge>()
            caseEdges.size shouldBe 3
            val caseEdges1 = caseEdges.filter { e -> e.assumedCase == 1 }
            caseEdges1.size shouldBe 1
            val caseEdge1 = caseEdges1[0]
            val caseEdges42 = caseEdges.filter { e -> e.assumedCase == 42 }
            caseEdges42.size shouldBe 1
            val caseEdge42 = caseEdges42[0]
            val caseEdges100 = caseEdges.filter { e -> e.assumedCase == 100 }
            caseEdges100.size shouldBe 1
            val caseEdge100 = caseEdges100[0]

            val n36 = caseEdge1.target
            caseEdge42.target shouldBeSameInstanceAs n36
            checkNode(n36, 36, false, 2, 1, code)
            val e1 = n36.leavingEdges[0]

            val n39 = caseEdge100.target
            checkNode(n39, 39, false, 1, 1, code)
            val e2 = n39.leavingEdges[0]

            val n41 = defaultEdge.target
            e2.target shouldBeSameInstanceAs n41
            checkNode(n41, 41, false, 2, 1, code)
            val e3 = n41.leavingEdges[0]

            val n43 = e3.target
            e1.target shouldBeSameInstanceAs n43
            checkNode(n43, 43, false, 2, 1, code)
            val e4 = n43.leavingEdges[0]

            val exitNode = e4.target
            checkNode(exitNode, -1, true, 1, 0, code)
        }

        cfa.clear()
    }
})
