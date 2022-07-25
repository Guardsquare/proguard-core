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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.domain.taint.TaintAbstractState
import proguard.analysis.cpa.domain.taint.TaintSource
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintMemoryLocationBamCpaRun
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSink
import proguard.analysis.cpa.jvm.util.CfaUtil
import testutils.ClassPoolBuilder
import testutils.JavaSource

class TraceExtractorTest : StringSpec({

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

    val taintSinkArgument = JvmTaintSink(
        "LA;sink(Ljava/lang/String;)V",
        false,
        setOf(1),
        setOf()
    )
    val taintSinkArgumentLong = JvmTaintSink(
        "LA;sink(J)V",
        false,
        setOf(1),
        setOf()
    )
    val taintSinkArgumentMultiple = JvmTaintSink(
        "LA;sink(Ljava/lang/String;Ljava/lang/String;)V",
        false,
        setOf(2),
        setOf()
    )
    val taintSinkStatic = JvmTaintSink(
        "LA;sink()V",
        false,
        setOf(),
        setOf("A.s")
    )

    "Simple interprocedural flows are reconstructed" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            sink(callee());
                        }
                    
                        public static String callee()
                        {
                            return source1();
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:3, JvmStackLocation(0)@LA;callee()Ljava/lang/String;:3]"
        )
    }

    "Interprocedural traces in callees are reconstructed" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            callee1();
                        }
                    
                        public static void callee1()
                        {
                            sink(callee2());
                        }
                    
                        public static String callee2()
                        {
                            return source1();
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;callee1()V:3, JvmStackLocation(0)@LA;callee2()Ljava/lang/String;:3]"
        )
    }

    "Multiple interprocedural traces are reconstructed" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            callee(source1());
                            callee(source2());
                        }
                    
                        public static void callee(String s)
                        {
                            sink(s);
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1, taintSourceReturn2),
            mainSignature,
            -1,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        /*
        Bytecode of main:
            [0] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
            [3] invokestatic #3 = Methodref(A.callee(Ljava/lang/String;)V)
            [6] invokestatic #4 = Methodref(A.source2()Ljava/lang/String;)
            [9] invokestatic #3 = Methodref(A.callee(Ljava/lang/String;)V)
            [12] return

         Bytecode of callee:
             [0] aload_0 v0
             [1] invokestatic #5 = Methodref(A.sink(Ljava/lang/String;)V)
             [4] return
        */

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;callee(Ljava/lang/String;)V:1, JvmLocalVariableLocation(0)@LA;callee(Ljava/lang/String;)V:0, JvmStackLocation(0)@LA;main()V:3]",
            "[JvmStackLocation(0)@LA;callee(Ljava/lang/String;)V:1, JvmLocalVariableLocation(0)@LA;callee(Ljava/lang/String;)V:0, JvmStackLocation(0)@LA;main()V:9]"
        )
    }

    "Interprocedural traces through static fields are reconstructed" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public static String s;
                    
                        public void main() {
                            callee();
                            sink();
                        }
                    
                        public static void callee()
                        {
                            source();
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceStatic),
            mainSignature,
            -1,
            TaintAbstractState.bottom,
            setOf(taintSinkStatic)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStaticFieldLocation(A.s)@LA;main()V:3, JvmStaticFieldLocation(A.s)@LA;callee()V:3]"
        )
    }

    "Explicit static updates are supported" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public static String s;
                    
                        public void main() {
                            callee();
                            sink(s);
                        }
                    
                        public static void callee()
                        {
                            s = source1();
                        }
                    
                        public static void sink(String s)
                        {
                            return;
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:6, JvmStaticFieldLocation(A.s)@LA;main()V:3, JvmStaticFieldLocation(A.s)@LA;callee()V:6, JvmStackLocation(0)@LA;callee()V:3]"
        )
    }

    "Intermediate library calls don't disrupt the trace" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            System.out.println();
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:11, JvmLocalVariableLocation(1)@LA;main()V:10, JvmLocalVariableLocation(1)@LA;main()V:7, JvmLocalVariableLocation(1)@LA;main()V:4, JvmStackLocation(0)@LA;main()V:3]"
        )
    }

    "Intermediate program calls don't disrupt the trace" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            foo();
                            sink(s);
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                        
                        public static void foo()
                        {
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:8, JvmLocalVariableLocation(1)@LA;main()V:7, JvmLocalVariableLocation(1)@LA;main()V:4, JvmStackLocation(0)@LA;main()V:3]"
        )
    }

    "Intermediate local variable updates don't disrupt the trace" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s1 = source1(); // out offset: 3
                            String s2 = null;
                            sink(s1); // in offset: 7
                    
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:7, JvmLocalVariableLocation(1)@LA;main()V:6, JvmLocalVariableLocation(1)@LA;main()V:5, JvmLocalVariableLocation(1)@LA;main()V:4, JvmStackLocation(0)@LA;main()V:3]"
        )
    }

    "Category 2 sink arguments are supported" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            sink(s.length());
                        }
                    
                        public static void sink(long l)
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgumentLong)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:9, JvmStackLocation(0)@LA;main()V:8, JvmStackLocation(0)@LA;main()V:5, JvmLocalVariableLocation(1)@LA;main()V:4, JvmStackLocation(0)@LA;main()V:3]"
        )
    }

    "Sink argument position is calculated correctly" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            sink(null, s);
                        }
                    
                        public static void sink(String s1, String s2)
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgumentMultiple)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:6, JvmLocalVariableLocation(1)@LA;main()V:5, JvmLocalVariableLocation(1)@LA;main()V:4, JvmStackLocation(0)@LA;main()V:3]"
        )
    }

    "The ternary operator is supported" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main(boolean b) {
                            sink(b ? getSource1() : getSource2());
                        }
                    
                        public static String getSource1()
                        {
                            return source1();
                        }
                    
                        public static String getSource2()
                        {
                            return source1();
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        /*
        Bytecode of main:
            [0] iload_1 v1
            [1] ifeq +9 (target=10)
            [4] invokestatic #2 = Methodref(A.getSource1()Ljava/lang/String;)
            [7] goto +6 (target=13)
            [10] invokestatic #3 = Methodref(A.getSource2()Ljava/lang/String;)
            [13] invokestatic #4 = Methodref(A.sink(Ljava/lang/String;)V)
            [16] return
         */

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main(Z)V:13, JvmStackLocation(0)@LA;getSource2()Ljava/lang/String;:3]",
            "[JvmStackLocation(0)@LA;main(Z)V:13, JvmStackLocation(0)@LA;main(Z)V:7, JvmStackLocation(0)@LA;getSource1()Ljava/lang/String;:3]"
        )
    }

    // with the current version of JvmMemoryLocationTransferRelation exception paths are not supported yet
    "Exception paths are supported".config(enabled = false) {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            try
                            {
                                callee();
                                s = null;
                            }
                            catch (Exception e)
                            {
                    
                            }
                            sink(s);
                        }
                    
                        public static void callee()
                        {
                            throw new IllegalStateException();
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:14, JvmLocalVariableLocation(1)@LA;main()V:13, JvmLocalVariableLocation(1)@LA;main()V:12, JvmLocalVariableLocation(1)@LA;main()V:4, JvmStackLocation(0)@LA;main()V:3]"
        )
    }

    "The trace does not go beyond the trace origin" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            sink(callee(source1()));
                        }
                    
                        public static String callee(String s)
                        {
                            return source1();
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
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main()V:10, JvmStackLocation(0)@LA;callee(Ljava/lang/String;)Ljava/lang/String;:3]"
        )
    }

    "The trace does not go through discarded abstract states" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {
                    
                        public void main(boolean b) {
                            String s1 = source1();
                            String s2 = source1();
                            sink(b ? s1 : s2);
                        }
                        
                        public static String source1()
                        {
                            return null;
                        }
                        
                        public static String source2()
                        {
                            return null;
                        }
                        
                        public static void sink(String s)
                        {
                        }
                    }
                    """.trimIndent()
                )
            ).programClassPool
        )
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1, taintSourceReturn2),
            mainSignature,
            -1,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        /*
        Bytecode of main:
            [0] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
            [3] astore_2 v2
            [4] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
            [7] astore_3 v3
            [8] iload_1 v1
            [9] ifeq +7 (target=16)
            [12] aload_2 v2
            [13] goto +4 (target=17)
            [16] aload_3 v3
            [17] invokestatic #3 = Methodref(A.sink(Ljava/lang/String;)V)
            [20] return
        */

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main(Z)V:17, JvmLocalVariableLocation(3)@LA;main(Z)V:16, JvmLocalVariableLocation(3)@LA;main(Z)V:9, JvmLocalVariableLocation(3)@LA;main(Z)V:8, JvmStackLocation(0)@LA;main(Z)V:7]",
            "[JvmStackLocation(0)@LA;main(Z)V:17, JvmStackLocation(0)@LA;main(Z)V:13, JvmLocalVariableLocation(2)@LA;main(Z)V:12, JvmLocalVariableLocation(2)@LA;main(Z)V:9, JvmLocalVariableLocation(2)@LA;main(Z)V:8, JvmLocalVariableLocation(2)@LA;main(Z)V:7, JvmLocalVariableLocation(2)@LA;main(Z)V:4, JvmStackLocation(0)@LA;main(Z)V:3]"
        )
    }

    "Return state less or equal than the post call site is supported" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {
                    
                        public static String s;

                        public void main(boolean b) {
                            
                            if (b)
                            {
                                s = source1();
                            }
                            else 
                            {
                                callee();
                            }
                            
                            sink(s);
                        }
                    
                        public static void callee()
                        {
                            s = source2();
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1, taintSourceReturn2),
            mainSignature,
            -1,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        /*
        Bytecode of main:
            [0] iload_1 v1
            [1] ifeq +12 (target=13)
            [4] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
            [7] putstatic #3 = Fieldref(A.s Ljava/lang/String;)
            [10] goto +6 (target=16)
            [13] invokestatic #4 = Methodref(A.callee()V)
            [16] getstatic #3 = Fieldref(A.s Ljava/lang/String;)
            [19] invokestatic #5 = Methodref(A.sink(Ljava/lang/String;)V)
            [22] return

        Bytecode of callee:
            [0] invokestatic #6 = Methodref(A.source2()Ljava/lang/String;)
            [3] putstatic #3 = Fieldref(A.s Ljava/lang/String;)
            [6] return
        */

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main(Z)V:19, JvmStaticFieldLocation(A.s)@LA;main(Z)V:16, JvmStaticFieldLocation(A.s)@LA;main(Z)V:10, JvmStackLocation(0)@LA;main(Z)V:7]",
            "[JvmStackLocation(0)@LA;main(Z)V:19, JvmStaticFieldLocation(A.s)@LA;main(Z)V:16, JvmStaticFieldLocation(A.s)@LA;callee()V:6, JvmStackLocation(0)@LA;callee()V:3]"
        )
    }

    "Correct trace for recursion".config(enabled = false) {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main(boolean b) 
                        {
                            sink(callee(b, source1(), source2()));
                        }
                        
                        public String callee(boolean b, String s1, String s2) 
                        {             
                            return b ? callee(!b, s1, s2) : s1;
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
        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun(
            interproceduralCfa,
            setOf(taintSourceReturn1, taintSourceReturn2),
            mainSignature,
            -1,
            TaintAbstractState.bottom,
            setOf(taintSinkArgument)
        )
        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        interproceduralCfa.clear()

        /*
        Bytecode of main:
            [0] aload_0 v0
            [1] iload_1 v1
            [2] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
            [5] invokestatic #3 = Methodref(A.source2()Ljava/lang/String;)
            [8] invokevirtual #4 = Methodref(A.callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;)
            [11] invokestatic #5 = Methodref(A.sink(Ljava/lang/String;)V)
            [14] return

        Bytecode of callee:
            [0] iload_1 v1
            [1] ifeq +21 (target=22)
            [4] aload_0 v0
            [5] iload_1 v1
            [6] ifne +7 (target=13)
            [9] iconst_1
            [10] goto +4 (target=14)
            [13] iconst_0
            [14] aload_2 v2
            [15] aload_3 v3
            [16] invokevirtual #4 = Methodref(A.callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;)
            [19] goto +4 (target=23)
            [22] aload_2 v2
            [23] areturn
         */

        traces.map { it.toString() }.toSet() shouldBe setOf(
            "[JvmStackLocation(0)@LA;main(Z)V:11, JvmStackLocation(0)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:23, " +
                "JvmLocalVariableLocation(2)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:22, JvmLocalVariableLocation(2)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:11, " +
                "JvmLocalVariableLocation(2)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:0, JvmStackLocation(1)@LA;main(Z)V:8, JvmStackLocation(0)@LA;main(Z)V:5]"
        )
    }
})
