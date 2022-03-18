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
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.analysis.cpa.domain.taint.TaintAbstractState
import proguard.analysis.cpa.domain.taint.TaintSource
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintMemoryLocationBamCpaRun
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSink
import proguard.analysis.cpa.jvm.state.heap.HeapModel
import proguard.analysis.cpa.jvm.util.CfaUtil
import testutils.ClassPoolBuilder
import testutils.JavaSource

class TreeHeapTest : FreeSpec({

    val taintSourceReturn1 = TaintSource(
        "LA;source1()Ljava/lang/String;",
        false,
        true,
        setOf(),
        setOf()
    )

    val taintSinkArgument = JvmTaintSink(
        "LA;sink(Ljava/lang/String;)V",
        false,
        setOf(1),
        setOf()
    )

    "Method arguments are unaliased by default" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main(B b1, B b2){
                            b1.s = source1();
                            sink(b2.s);
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    
                        public class B {
                            public String s;
                        }
                    }
                    """.trimIndent()
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.size shouldBe 0
    }

    "Explicit argument aliasing is supported" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            B b = new B();
                            callee(b, b);
                        }
                    
                        public static void callee(B b1, B b2){
                            b1.s = source1();
                            sink(b2.s);
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    
                        public class B {
                            public String s;
                        }
                    }
                    """.trimIndent()
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        // TODO replace this check with an exact trace comparison
        traces.size shouldNotBe 0
    }

    "Flow through an array element is detected" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        private String[] s;
                    
                        public void main() {
                            s[0] = source1();
                            sink(s[0]);
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:15, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:14, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:13, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:10, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:9, JvmStackLocation(0)@LA;main()V:8]"
        )
    }

    "All array elements are assumed to be aliased" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        private String[] s;
                    
                        public void main() {
                            s[1] = source1();
                            sink(s[0]);
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        // TODO adjust this test after the heap model refinement
        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:15, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:14, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:13, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:10, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:9, JvmStackLocation(0)@LA;main()V:8]"
        )
    }

    "All fields with the same name are aliased in complex objects" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        private String s;
                        private A a;
                    
                        public void main() {
                            a.s = source1();
                            sink(s);
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:14, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], A#s)@LA;main()V:11, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], A#s)@LA;main()V:10, JvmStackLocation(0)@LA;main()V:7]"
        )
    }

    "Flows through nonstatic fields are supported" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        private String s;
                    
                        public void main() {
                            s = source1();
                            sink(s);
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:11, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], A#s)@LA;main()V:8, JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], A#s)@LA;main()V:7, JvmStackLocation(0)@LA;main()V:4]"
        )
    }

    "Analysis of loops converges" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        private String s;
                    
                        public void main(boolean b) {
                            while(b)
                            {
                                s = source1();
                            }
                            sink(s);
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        // TODO replace this check with an exact trace comparison
        traces.size shouldNotBe 0
    }

    "Unaliased overwriting is supported" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        private String s;
                    
                        public void main() {
                            s = sourceReturn1();
                            s = null;
                            sinkArgument1(s);
                        }
                    
                        public static void sinkArgument1(String s)
                        {
                        }
                    
                        public static String sourceReturn1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent()
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.size shouldBe 0
    }

    "Aliased overwriting results in a weak update preserving the taint" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public String s;
                    
                        public void main(boolean b) {
                            A a = this;
                            if (b)
                            {
                                a = new A();
                            }
                            a.s = source1(); // weak update taints both aliases
                            a.s = null;            // weak update preserves the taint
                            sink(a.s);
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        // TODO replace this check with an exact trace comparison
        traces.size shouldNotBe 0
    }

    "Array overwriting results in a weak update preserving the taint" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        private String[] s;
                    
                        public void main() {
                            s[0] = source1();
                            s[0] = null;
                            sink(s[0]);
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        // TODO replace this check with an exact trace comparison
        traces.size shouldNotBe 0
    }

    "Multiple paths are reconstructed" - {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public static B b;
                    
                        public void main(boolean condition) {
                            if (condition)
                            {
                                callee1();
                            }
                            else
                            {
                                callee2();
                            }
                            callee3();
                        }
                    
                        public static void callee1()
                        {
                            b.s = source1();
                        }
                    
                        public static void callee2()
                        {
                            b.s = source1();
                        }
                    
                        public static void callee3()
                        {
                            sink(b.s);
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    
                        private class B {
                            public String s;
                        }
                    }
                    """.trimIndent()
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1),
            mainSignature,
            -1,
            HeapModel.TREE,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        // TODO replace this check with an exact trace comparison
        traces.size shouldBeGreaterThanOrEqual 2
    }
})
