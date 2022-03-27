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
import proguard.analysis.cpa.domain.taint.TaintAbstractState
import proguard.analysis.cpa.domain.taint.TaintSource
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintBamCpaRun
import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.classfile.MethodSignature
import testutils.ClassPoolBuilder
import testutils.JavaSource

class JvmTaintCpaTest : FreeSpec({

    val taintSourceReturn1 = TaintSource(
        "LA;source1()Ljava/lang/String;",
        false,
        true,
        setOf(),
        setOf()
    )
    val taintSourceReturn2 = TaintSource(
        "LA;source2()Ljava/lang/String;",
        false,
        true,
        setOf(),
        setOf()
    )
    val taintSourceStatic = TaintSource(
        "LA;source()V",
        false,
        false,
        setOf(),
        setOf("A.s")
    )

    "Simple flow is detected" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 5)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceReturn1), mainSignature, 0)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).peek() shouldBe setOf(taintSourceReturn1)
    }

    "Taint can be overwritten" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 8)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceReturn1), mainSignature, 0)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).peek() shouldBe setOf()
    }

    "Taints combine upon merge" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 16)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceReturn1, taintSourceReturn2), mainSignature, 0)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).peek() shouldBe setOf(taintSourceReturn1, taintSourceReturn2)
    }

    "Taint propagates along loops" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 9)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceReturn1, taintSourceReturn2), mainSignature, 0)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).peek() shouldBe setOf(taintSourceReturn1, taintSourceReturn2)
    }

    "Taint propagates through static fields" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 6)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceStatic), mainSignature, 0)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).getStaticOrDefault("A.s", TaintAbstractState.bottom) shouldBe setOf(taintSourceStatic)
    }

    "Taint flows through the return value of a non-tainting function" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 5)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceReturn1), mainSignature, -1)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).peek() shouldBe setOf(taintSourceReturn1)
    }

    "Taint flows through static field tainted in a function call" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 3)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceReturn1), mainSignature, -1)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).getStaticOrDefault("A.s", TaintAbstractState.bottom) shouldBe setOf(taintSourceReturn1)
    }

    "Recursive function analysis converges" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 6)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceReturn1), mainSignature, -1)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).peek() shouldBe setOf(taintSourceReturn1)
    }

    "Merging works interprocedurally" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 6)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceReturn1, taintSourceReturn2), mainSignature, -1)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).peek() shouldBe setOf(taintSourceReturn1, taintSourceReturn2)
    }

    "Tail recursion analysis converges" - {
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
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(mainSignature, 7)
        val taintCpaRun = JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(interproceduralCfa, setOf(taintSourceStatic), mainSignature, -1)
        val abstractStates = (taintCpaRun.execute() as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<TaintAbstractState>, MethodSignature>).getReached(location)
        interproceduralCfa.clear()

        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<TaintAbstractState>).getStaticOrDefault("A.s", TaintAbstractState.bottom) shouldBe setOf(taintSourceStatic)
    }
})
