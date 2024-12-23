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
import proguard.analysis.cpa.defaults.SetAbstractState
import proguard.analysis.cpa.jvm.domain.taint.JvmInvokeTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintTransformer
import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.util.TaintAnalyzer
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class JvmTaintCpaTest : FreeSpec({

    val taintSourceReturn1 = JvmTaintSource(
        MethodSignature("A", "source1", "()Ljava/lang/String;"),
        false,
        true,
        setOf(),
        setOf(),
    )
    val taintSourceReturn2 = JvmTaintSource(
        MethodSignature("A", "source2", "()Ljava/lang/String;"),
        false,
        true,
        setOf(),
        setOf(),
    )
    val taintSourceReturnDouble = JvmTaintSource(
        MethodSignature("A", "source", "()D"),
        false,
        true,
        setOf(),
        setOf(),
    )
    val taintSourceStatic = JvmTaintSource(
        MethodSignature("A", "source", "()V"),
        false,
        false,
        setOf(),
        setOf("A.s:Ljava/lang/String;"),
    )

    val sink = JvmInvokeTaintSink(
        MethodSignature(
            "A",
            "sink",
            "(Ljava/lang/String;)V",
        ),
        false,
        setOf(1),
        setOf(),
    )

    "Simple flow is detected" {
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
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(sink))
            .setMaxCallStackDepth(0)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpointToTriggeredSinks.size shouldBe 1
        result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
        result.endpoints.size shouldBe 1
        result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
            taintSourceReturn1,
        )
    }

    "Taint can be overwritten" {
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
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(sink))
            .setMaxCallStackDepth(0)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpoints.size shouldBe 0
    }

    "Taints combine upon merge" {
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
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1, taintSourceReturn2), setOf(sink))
            .setMaxCallStackDepth(0)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpointToTriggeredSinks.size shouldBe 1
        result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
        result.endpoints.size shouldBe 1
        result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
            taintSourceReturn1,
            taintSourceReturn2,
        )
    }

    "Taint propagates along loops" {
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
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1, taintSourceReturn2), setOf(sink))
            .setMaxCallStackDepth(0)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpointToTriggeredSinks.size shouldBe 1
        result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
        result.endpoints.size shouldBe 1
        result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
            taintSourceReturn1,
            taintSourceReturn2,
        )
    }

    "Taint propagates through static fields" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                    public static String s;
                    public static String a;
                
                    public void main() {
                        source();
                        a = s;
                        sink(s);
                    }
                
                    public static void sink(String s)
                    {
                    }
                
                    public static void source()
                    {
                    }
                }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceStatic), setOf(sink))
            .setMaxCallStackDepth(0)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpointToTriggeredSinks.size shouldBe 1
        result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
        result.endpoints.size shouldBe 1
        result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
            taintSourceStatic,
        )
    }

    "Taint flows through the return value of a non-tainting function" {
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
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(sink))
            .setMaxCallStackDepth(-1)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpointToTriggeredSinks.size shouldBe 1
        result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
        result.endpoints.size shouldBe 1
        result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
            taintSourceReturn1,
        )
    }

    "Taint flows through static field tainted in a function call" {
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
                            sink(s);
                        }
                    
                        public static void sink(String s)
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
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(sink))
            .setMaxCallStackDepth(-1)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpointToTriggeredSinks.size shouldBe 1
        result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
        result.endpoints.size shouldBe 1
        result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
            taintSourceReturn1,
        )
    }

    "Recursive function analysis converges" {
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
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(sink))
            .setMaxCallStackDepth(-1)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpointToTriggeredSinks.size shouldBe 1
        result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
        result.endpoints.size shouldBe 1
        result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
            taintSourceReturn1,
        )
    }

    "Merging works interprocedurally" {
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
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1, taintSourceReturn2), setOf(sink))
            .setMaxCallStackDepth(-1)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpointToTriggeredSinks.size shouldBe 1
        result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
        result.endpoints.size shouldBe 1
        result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
            taintSourceReturn1,
            taintSourceReturn2,
        )
    }

    "Tail recursion analysis converges" {
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
                            sink(s); // in offset: 7
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
                    
                        public static void sink(String s)
                        {
                            return;
                        }
                    
                        public static void source()
                        {
                            return;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceStatic), setOf(sink))
            .setMaxCallStackDepth(-1)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        interproceduralCfa.clear()

        result.endpointToTriggeredSinks.size shouldBe 1
        result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
        result.endpoints.size shouldBe 1
        result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
            taintSourceStatic,
        )
    }

    "Category 2 taint sources taint only top of the stack" {
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
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturnDouble), setOf())
            .setMaxCallStackDepth(-1)
            .build()
        val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

        val location = interproceduralCfa.getFunctionNode(mainSignature, 3)

        val abstractStates = result.mainMethodReachedSet.getReached(location)
        interproceduralCfa.clear()
        abstractStates.size shouldBe 1
        (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek() shouldBe setOf(taintSourceReturnDouble)
        (abstractStates.first() as JvmAbstractState<SetAbstractState<JvmTaintSource>>).peek(1) shouldBe setOf()
    }

    "Sanitizing transformer breaks the dataflow" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfa(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A
                    {

                        public void main()
                        {
                            sink(source1().toString());
                        }

                        public static void sink(String s)
                        {
                        }

                        public static String source1()
                        {
                            return "ciao";
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )

        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature

        val taintAnalyzerBuilder = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(sink))
            .setMaxCallStackDepth(-1)

        "Normal run has dataflow" {
            val taintAnalyzer = taintAnalyzerBuilder.build()
            val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

            result.endpointToTriggeredSinks.size shouldBe 1
            result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
            result.endpoints.size shouldBe 1
            result.endpoints.first().extractFirstValue(SetAbstractState.bottom) shouldBe setOf(
                taintSourceReturn1,
            )
        }

        "Run with sanitizing transformer doesn't have a dataflow" {
            val sanitizingTransformer = object : JvmTaintTransformer {
                override fun transformReturn(returnValue: SetAbstractState<JvmTaintSource>?): SetAbstractState<JvmTaintSource> {
                    return SetAbstractState()
                }
            }

            val taintAnalyzer = taintAnalyzerBuilder
                .setTaintTransformers(mapOf(MethodSignature("java/lang/String", "toString", "()Ljava/lang/String;") to sanitizingTransformer))
                .build()
            val result = taintAnalyzer.analyze(mainSignature).taintAnalysisResult

            result.endpoints.size shouldBe 0
        }

        interproceduralCfa.clear()
    }
})
