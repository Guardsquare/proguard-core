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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet
import proguard.analysis.cpa.defaults.SetAbstractState
import proguard.analysis.cpa.jvm.cfa.edges.JvmCallCfaEdge
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintBamCpaRun
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource
import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.jvm.witness.JvmLocalVariableLocation
import proguard.analysis.cpa.jvm.witness.JvmStackLocation
import proguard.analysis.cpa.state.HashMapAbstractStateFactory
import proguard.classfile.MethodSignature
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder

class JvmTaintPropagationThroughConstructorsTest : FunSpec({
    val jvmTaintBamCpaRunBuilder = JvmTaintBamCpaRun.Builder()
        .setStaticFieldMapAbstractStateFactory(HashMapAbstractStateFactory.getInstance())

    val taintSource = JvmTaintSource(
        MethodSignature(
            "Test",
            "source",
            "()Ljava/lang/String;",
        ),
        false,
        true,
        setOf(),
        setOf(),
    )

    /**
     *       [0] invokestatic #20 = Methodref(Test.source()Ljava/lang/String;)
     *       [3] astore_1 v1
     *       [4] new #22 = Class(java/lang/String)
     *       [7] dup
     *       [8] aload_1 v1
     *       [9] getstatic #28 = Fieldref(java/nio/charset/StandardCharsets.UTF_8 Ljava/nio/charset/Charset;)
     *       [12] invokevirtual #32 = Methodref(java/lang/String.getBytes(Ljava/nio/charset/Charset;)[B)
     *       [15] iconst_1
     *       [16] iconst_2
     *       [17] invokespecial #35 = Methodref(java/lang/String.<init>([BII)V)
     *       [20] astore_2 v2
     *       [21] aload_2 v2
     *       [22] invokestatic #39 = Methodref(Test.sink(Ljava/lang/String;)V)
     *       [25] return
     */
    test("Taint propagation through constructor via tainting top of the stack") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "Test.jbc",
                """
                version 1.8;
                class Test extends java.lang.Object [
                    SourceFile "Test.java";
                ] {
                
                    void <init>() {
                        line 2
                            aload_0
                            invokespecial java.lang.Object#void <init>()
                            return
                    }
                
                    public void test() {
                        line 5
                            invokestatic #java.lang.String source()
                            astore_1
                        line 6
                            new java.lang.String
                            dup
                            aload_1
                            getstatic java.nio.charset.StandardCharsets#java.nio.charset.Charset UTF_8
                            invokevirtual java.lang.String#byte[] getBytes(java.nio.charset.Charset)
                            iconst_1
                            iconst_2
                            invokespecial java.lang.String#void <init>(byte[],int,int)
                            astore_2
                        line 7
                            aload_2
                            invokestatic #void sink(java.lang.String)
                        line 8
                            return
                    }
                
                    public static void sink(java.lang.String) {
                        line 10
                            return
                    }
                
                    public static java.lang.String source() {
                        line 13
                            ldc "tainted"
                            areturn
                    }
                
                }
                """.trimIndent(),
            ),
        )
        val interproceduralCfa = CfaUtil.createInterproceduralCfa(
            programClassPool,
        )

        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("test") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 22)
        val callToInit = interproceduralCfa.getFunctionNode(mainSignature, 17).leavingEdges
            .filterIsInstance<JvmCallCfaEdge>().first().call
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(mainSignature)
            .setTaintSources(setOf(taintSource))
            .setMaxCallStackDepth(10)
            .setExtraTaintPropagationLocations(mapOf(callToInit to setOf(JvmStackLocation(0))))
            .build()
        val abstractStates =
            (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(
                location,
            )
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(
            taintSource,
        )
    }

    /**
     *       [0] invokestatic #20 = Methodref(Test.source()Ljava/lang/String;)
     *       [3] astore_1 v1
     *       [4] new #22 = Class(java/lang/String)
     *       [7] astore_2 v2
     *       [8] aload_2 v2
     *       [9] aload_1 v1
     *       [10] getstatic #28 = Fieldref(java/nio/charset/StandardCharsets.UTF_8 Ljava/nio/charset/Charset;)
     *       [13] invokevirtual #32 = Methodref(java/lang/String.getBytes(Ljava/nio/charset/Charset;)[B)
     *       [16] iconst_1
     *       [17] iconst_2
     *       [18] invokespecial #35 = Methodref(java/lang/String.<init>([BII)V)
     *       [21] aload_2 v2
     *       [22] invokestatic #39 = Methodref(Test.sink(Ljava/lang/String;)V)
     *       [25] return
     */
    test("Taint propagation through constructors via tainting local variables") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "Test.jbc",
                """
                version 1.8;
                class Test extends java.lang.Object [
                    SourceFile "Test.java";
                ] {
                
                    void <init>() {
                        line 2
                            aload_0
                            invokespecial java.lang.Object#void <init>()
                            return
                    }
                
                    public void test() {
                        line 5
                            invokestatic #java.lang.String source()
                            astore_1
                        line 6
                            new java.lang.String
                            astore_2
                            aload_2
                            aload_1
                            getstatic java.nio.charset.StandardCharsets#java.nio.charset.Charset UTF_8
                            invokevirtual java.lang.String#byte[] getBytes(java.nio.charset.Charset)
                            iconst_1
                            iconst_2
                            invokespecial java.lang.String#void <init>(byte[],int,int)
                        line 7
                            aload_2
                            invokestatic #void sink(java.lang.String)
                        line 8
                            return
                    }
                
                    public static void sink(java.lang.String) {
                        line 10
                            return
                    }
                
                    public static java.lang.String source() {
                        line 13
                            ldc "tainted"
                            areturn
                    }
                
                }
                """.trimIndent(),
            ),
        )
        val interproceduralCfa = CfaUtil.createInterproceduralCfa(
            programClassPool,
        )

        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("test") }.findFirst().get().signature
        val callToInit = interproceduralCfa.getFunctionNode(mainSignature, 18).leavingEdges
            .filterIsInstance<JvmCallCfaEdge>().first().call
        val location = interproceduralCfa.getFunctionNode(mainSignature, 22)
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(mainSignature)
            .setTaintSources(setOf(taintSource))
            .setMaxCallStackDepth(10)
            .setExtraTaintPropagationLocations(mapOf(callToInit to setOf(JvmLocalVariableLocation(2))))
            .build()
        val abstractStates =
            (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(
                location,
            )
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(
            taintSource,
        )
    }
})
