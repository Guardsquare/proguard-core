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
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet
import proguard.analysis.cpa.defaults.SetAbstractState
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintBamCpaRun
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource
import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.state.DifferentialMapAbstractStateFactory
import proguard.analysis.cpa.state.HashMapAbstractStateFactory
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class JvmTaintCpaTest : FreeSpec({

    val taintSourceReturn1 = JvmTaintSource(
        MethodSignature("A", "source1", "()Ljava/lang/String;"),
        false,
        true,
        setOf(),
        setOf()
    )
    val taintSourceReturn2 = JvmTaintSource(
        MethodSignature("A", "source2", "()Ljava/lang/String;"),
        false,
        true,
        setOf(),
        setOf()
    )
    val taintSourceReturnDouble = JvmTaintSource(
        MethodSignature("A", "source", "()D"),
        false,
        true,
        setOf(),
        setOf()
    )
    val taintSourceStatic = JvmTaintSource(
        MethodSignature("A", "source", "()V"),
        false,
        false,
        setOf(),
        setOf("A.s")
    )

    listOf(
        HashMapAbstractStateFactory.getInstance(),
        DifferentialMapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> { false }
    ).forEach { staticFieldMapAbstractStateFactory ->

        val testNameSuffix = " for static fields ${staticFieldMapAbstractStateFactory.javaClass.simpleName}"
        val jvmTaintBamCpaRunBuilder = JvmTaintBamCpaRun.Builder().setStaticFieldMapAbstractStateFactory(staticFieldMapAbstractStateFactory)

        "Simple flow is detected$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {

                        public void main()
                        {
                            String s;
                            s = source1(); // out offset: 3
                            sink(s); // in offset: 5
                        }

                        public static void sink(String s)
                        {
                        }

                        public static String source1()
                        {
                            return null;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 5)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setMaxCallStackDepth(0)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(taintSourceReturn1)
        }

        "Taint can be overwritten$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {

                        public void main()
                        {
                            String s;
                            s = source1();
                            s = "";
                            sink(s); // in offset 8
                        }

                        public static void sink(String s)
                        {
                        }

                        public static String source1()
                        {
                            return null;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 8)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setMaxCallStackDepth(0)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf()
        }

        "Taints combine upon merge$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {
                    
                        public void main(boolean b)
                        {
                            String s;
                            if (b)
                            {
                                s = source1();
                            }
                            else
                            {
                                s = source2();
                            }
                            sink(s); // in offset: 16
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    
                        public static String source2()
                        {
                            return null;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 16)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1, taintSourceReturn2))
                .setMaxCallStackDepth(0)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(taintSourceReturn1, taintSourceReturn2)
        }

        "Taint propagates along loops$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {
                    
                        public void main(boolean b)
                        {
                            String s;
                            s = source1();
                            while (b)
                            {
                                sink(s); // in offset: 9
                                s = source2();
                            }
                    
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    
                        public static String source2()
                        {
                            return null;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 9)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1, taintSourceReturn2))
                .setMaxCallStackDepth(0)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(taintSourceReturn1, taintSourceReturn2)
        }

        "Taint propagates through static fields$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A {

                    public static String s;
                
                    public void main() {
                        source();
                        sink(); // in offset: 6
                    }
                
                    public static void sink()
                    {
                    }
                
                    public static void source()
                    {
                    }
                }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 6)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceStatic))
                .setMaxCallStackDepth(0)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).getStaticOrDefault("A.s", SetAbstractState.bottom as SetAbstractState<JvmTaintSource>) shouldBe setOf(taintSourceStatic)
        }

        "Taint flows through the return value of a non-tainting function$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {
                    
                        public void main()
                        {
                            String s;
                            s = absolutelyNotASource();
                            sink(s); // in offset 5
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String absolutelyNotASource()
                        {
                            return source1();
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 5)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setMaxCallStackDepth(-1)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(taintSourceReturn1)
        }

        "Taint flows through static field tainted in a function call$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {
                        static String s;
                    
                        public void main()
                        {
                            absolutelyNotASource();
                            sink(); // in offset 3
                        }
                    
                        public static void sink()
                        {
                        }
                    
                        public static void absolutelyNotASource()
                        {
                            s = source1();
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 3)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setMaxCallStackDepth(-1)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).getStaticOrDefault("A.s:Ljava/lang/String;", SetAbstractState.bottom as SetAbstractState<JvmTaintSource>) shouldBe setOf(taintSourceReturn1)
        }

        "Recursive function analysis converges$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {
                    
                        public void main()
                        {
                            String s;
                            s = absolutelyNotASource(4);
                            sink(s); // in offset 6
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String absolutelyNotASource(int a)
                        {
                            if  (a > 0)
                            {
                                return absolutelyNotASource(a - 1);
                            }
                            else
                            {
                                return source1();
                            }
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 6)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setMaxCallStackDepth(-1)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(taintSourceReturn1)
        }

        "Merging works interprocedurally$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {
                    
                        public void main(boolean b)
                        {
                            String s;
                            s = absolutelyNotASource(b);
                            sink(s); // in offset 6
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String absolutelyNotASource(boolean b)
                        {
                            if  (b)
                            {
                                return source1();
                            }
                            else
                            {
                                return source2();
                            }
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    
                        public static String source2()
                        {
                            return null;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 6)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1, taintSourceReturn2))
                .setMaxCallStackDepth(-1)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(taintSourceReturn1, taintSourceReturn2)
        }

        "Tail recursion analysis converges$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {
                    
                        public static String  s;
                        public static boolean b;
                    
                        public void main(boolean b)
                        {
                            A.b = b;
                            callee();
                            sink(); // in offset: 7
                        }
                    
                        public static void callee()
                        {
                            if (b)
                            {
                                source();
                                b = !b;
                                callee();
                            }
                        }
                    
                        public static void sink()
                        {
                            return;
                        }
                    
                        public static void source()
                        {
                            return;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 7)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceStatic))
                .setMaxCallStackDepth(-1)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).getStaticOrDefault("A.s", SetAbstractState.bottom as SetAbstractState<JvmTaintSource>) shouldBe setOf(taintSourceStatic)
        }

        "Category 2 taint sources taint only top of the stack$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A
                    {
                    
                        public void main()
                        {
                            source(); // out offset: 3
                        }
                    
                        public static double source()
                        {
                            return 0.0;
                        }
                    }
                        """.trimIndent()
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val location = interproceduralCfa.getFunctionNode(mainSignature, 3)
            val taintCpaRun = jvmTaintBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturnDouble))
                .setMaxCallStackDepth(-1)
                .build()
            val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<SetAbstractState<JvmTaintSource>>, MethodSignature>).getReached(location)
            interproceduralCfa.clear()

            abstractStates.size shouldBe 1
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(taintSourceReturnDouble)
            (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek(1) shouldBe setOf()
        }
    }
})
