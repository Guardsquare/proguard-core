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
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintBamCpaRun
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource
import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.state.HashMapAbstractStateFactory
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class JvmArgumentsAsTaintSourcesCpaTest : FunSpec({

    val taintSourceTaintsArgs1 = JvmTaintSource(
        MethodSignature(
            "A",
            "sourceTaintsArgs",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
        ),
        false,
        false,
        setOf(1),
        setOf(),
    )
    val taintSourceTaintsArgs2 = JvmTaintSource(
        MethodSignature("A", "sourceTaintsArgs2", "(Ljava/lang/String;)Ljava/lang/String;"),
        false,
        false,
        setOf(1),
        setOf(),
    )
    val taintSourceTaintsTwoArgs1 = JvmTaintSource(
        MethodSignature(
            "A",
            "sourceTaintsTwoArgs",
            "(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;",
        ),
        false,
        false,
        setOf(1),
        setOf(),
    )
    val taintSourceTaintsTwoArgs2 = JvmTaintSource(
        MethodSignature(
            "A",
            "sourceTaintsTwoArgs",
            "(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;",
        ),
        false,
        false,
        setOf(2),
        setOf(),
    )

    val jvmTaintBamCpaRunBuilder = JvmTaintBamCpaRun.Builder()
        .setStaticFieldMapAbstractStateFactory(HashMapAbstractStateFactory.getInstance())

    test("Taints arguments static") {
        val programClassPool = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    class A
                    {

                        public void main() {
                            String s;
                            String s1 = "s1";
                            s = sourceTaintsArgs(s1, "s2");
                            sink(s); // in offset: 11
                        }

                        public static void sink(String s) { }

                        public static String sourceTaintsArgs(String s1, String s2) {
                            return s1;
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        ).programClassPool
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 11)
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(mainSignature)
            .setTaintSources(setOf(taintSourceTaintsArgs1))
            .setMaxCallStackDepth(10)
            .build()
        val abstractStates =
            (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(
                location,
            )
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(
            taintSourceTaintsArgs1,
        )
    }

    test("Taints arguments non static") {
        val programClassPool = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    class A {

                        public void main(boolean b) {
                            String s;
                            String s1 = "s1";
                            s = sourceTaintsTwoArgs(s1, "s2", b);
                            sink(s); // in offset: 14
                        }

                        public void sink(String s) { }

                        public String sourceTaintsTwoArgs(String s1, String s2, boolean b) {
                            if (b) {
                                return s2;
                            } else {
                                return s1;
                            }
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        ).programClassPool
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 14)
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(mainSignature)
            .setTaintSources(setOf(taintSourceTaintsTwoArgs2, taintSourceTaintsTwoArgs1))
            .setMaxCallStackDepth(10)
            .build()
        val abstractStates =
            (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(
                location,
            )
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(
            taintSourceTaintsTwoArgs2,
            taintSourceTaintsTwoArgs1,
        )
    }

    test("Source as an entrypoint taints arguments") {
        val programClassPool = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    class A {

                        public void passToSink(String s) {
                            sink(s); // in offset: 2
                        }

                        public void sink(String s) { }

                        public String sourceTaintsArgs(String s1, String s2) {
                            passToSink(s1);
                            return s1;
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        ).programClassPool
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            programClassPool,
        )
        val entrySignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("source") }.findFirst().get().signature
        val passToSinkSignature = interproceduralCfa.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("passToSink") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(passToSinkSignature, 2)
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(entrySignature)
            .setTaintSources(setOf(taintSourceTaintsArgs1))
            .setMaxCallStackDepth(10)
            .build()

        taintCpaRun.execute()
        val cache = taintCpaRun.cpa.cache
        val cacheEntries = cache.get(passToSinkSignature).first()
        interproceduralCfa.clear()

        val reached =
            (cacheEntries.reachedSet as ProgramLocationDependentReachedSet<JvmCfaNode, *, *, *>).getReached(
                location,
            )
        reached.size shouldBe 1
        (reached.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(
            taintSourceTaintsArgs1,
        )
    }

    test("Source as an entrypoint taints arguments static") {
        val programClassPool = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    class A {

                        public static void sink(String s) { }
            
                        private static String intermediate() {
                          return "intermediate value";
                        }

                        public static String sourceTaintsArgs(String s1, String s2) {
                            s2 = intermediate();
                            sink(s1); // in offset: 5
                            sink(s2); // in offset: 9
                            return "";
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        ).programClassPool
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            programClassPool,
        )
        val sourceSignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("source") }.findFirst().get().signature
        val locationTainted = interproceduralCfa.getFunctionNode(sourceSignature, 5)
        val locationNotTainted = interproceduralCfa.getFunctionNode(sourceSignature, 9)
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(sourceSignature)
            .setTaintSources(setOf(taintSourceTaintsArgs1))
            .setMaxCallStackDepth(10)
            .build()

        taintCpaRun.execute()
        val cache = taintCpaRun.cpa.cache
        val cacheEntries = cache.get(sourceSignature).first()
        interproceduralCfa.clear()

        val reachedTainted =
            (cacheEntries.reachedSet as ProgramLocationDependentReachedSet<JvmCfaNode, *, *, *>).getReached(
                locationTainted,
            )
        reachedTainted.size shouldBe 1
        (reachedTainted.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(
            taintSourceTaintsArgs1,
        )

        val reachedNotTainted =
            (cacheEntries.reachedSet as ProgramLocationDependentReachedSet<JvmCfaNode, *, *, *>).getReached(
                locationNotTainted,
            )
        reachedNotTainted.size shouldBe 1
        (reachedNotTainted.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf()
    }

    test("Tainted argument gets cleared by intermediate call") {
        val programClassPool = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    class A {

                        public static void sink(String s) { }
            
                        private static String intermediate() {
                          return "not tainted";
                        }

                        public static String sourceTaintsArgs(String s1, String s2) {
                            s1 = intermediate();
                            sink(s1); // in offset: 5
                            return "";
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        ).programClassPool
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            programClassPool,
        )
        val sourceSignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("source") }.findFirst().get().signature
        val locationTainted = interproceduralCfa.getFunctionNode(sourceSignature, 5)
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(sourceSignature)
            .setTaintSources(setOf(taintSourceTaintsArgs1))
            .setMaxCallStackDepth(10)
            .build()

        taintCpaRun.execute()
        val cache = taintCpaRun.cpa.cache
        val cacheEntries = cache.get(sourceSignature).first()
        interproceduralCfa.clear()

        val reachedTainted =
            (cacheEntries.reachedSet as ProgramLocationDependentReachedSet<JvmCfaNode, *, *, *>).getReached(
                locationTainted,
            )
        reachedTainted.size shouldBe 1
        (reachedTainted.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf()
    }

    test("Tainted argument does not get cleared by intermediate call") {
        val programClassPool = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    class A {

                        public static void sink(String s) { }
            
                        private static String intermediate(String s) {
                          return s;
                        }

                        public static String sourceTaintsArgs(String s1, String s2) {
                            intermediate(s1);
                            sink(s1); // in offset: 6
                            return "";
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        ).programClassPool
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            programClassPool,
        )
        val sourceSignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("source") }.findFirst().get().signature
        val locationTainted = interproceduralCfa.getFunctionNode(sourceSignature, 6)
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(sourceSignature)
            .setTaintSources(setOf(taintSourceTaintsArgs1))
            .setMaxCallStackDepth(10)
            .build()

        taintCpaRun.execute()
        val cache = taintCpaRun.cpa.cache
        val cacheEntries = cache.get(sourceSignature).first()
        interproceduralCfa.clear()

        val reachedTainted =
            (cacheEntries.reachedSet as ProgramLocationDependentReachedSet<JvmCfaNode, *, *, *>).getReached(
                locationTainted,
            )
        reachedTainted.size shouldBe 1
        (reachedTainted.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(taintSourceTaintsArgs1)
    }

    test("Sources from tainted arguments combine upon merge") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {
                    
                        public void main(boolean b) {
                            String s;
                            if (b) {
                                s = sourceTaintsArgs("a", "b");
                            }
                            else {
                                s = sourceTaintsArgs2("c");
                            }
                            sink(s); // in offset: 22
                        }
                    
                        public static void sink(String s) { }
                    
                        public static String sourceTaintsArgs(String s1, String s2) {
                            return s1;
                        }
                    
                         public static String sourceTaintsArgs2(String s) {
                            return s;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 22)
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(mainSignature)
            .setTaintSources(setOf(taintSourceTaintsArgs1, taintSourceTaintsArgs2))
            .setMaxCallStackDepth(10)
            .build()
        val abstractStates =
            (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(
                location,
            )
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(
            taintSourceTaintsArgs1,
            taintSourceTaintsArgs2,
        )
    }

    test("Taints in tainted arguments are overwritten") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {
                    
                        public void main(boolean b) {
                            String s;
                            s = sourceTaintsArgs("a", "b");
                            s = sourceTaintsArgs2("c");
                            sink(s); // in offset: 17
                        }
                    
                        public static void sink(String s) { }
                    
                        public String sourceTaintsArgs(String s1, String s2) {
                            return s1;
                        }
                    
                         public String sourceTaintsArgs2(String s) {
                            return s;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream()
            .filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 17)
        val taintCpaRun = jvmTaintBamCpaRunBuilder
            .setCfa(interproceduralCfa)
            .setMainSignature(mainSignature)
            .setTaintSources(setOf(taintSourceTaintsArgs1, taintSourceTaintsArgs2))
            .setMaxCallStackDepth(10)
            .build()
        val abstractStates =
            (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(
                location,
            )
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(
            taintSourceTaintsArgs2,
        )
    }
})