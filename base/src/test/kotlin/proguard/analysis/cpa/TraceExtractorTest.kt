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
import proguard.analysis.cpa.defaults.SetAbstractState
import proguard.analysis.cpa.jvm.domain.taint.JvmInvokeTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmReturnTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintMemoryLocationBamCpaRun
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.state.DifferentialMapAbstractStateFactory
import proguard.analysis.cpa.state.HashMapAbstractStateFactory
import proguard.analysis.cpa.state.LimitedHashMapAbstractStateFactory
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import java.util.Optional

class TraceExtractorTest : StringSpec({

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
    val taintSourceStatic = JvmTaintSource(
        MethodSignature("A", "source", "()V"),
        false,
        false,
        setOf(),
        setOf("A.s:Ljava/lang/String;")
    )
    val taintSourceReturnDouble = JvmTaintSource(
        MethodSignature("A", "source", "()D"),
        false,
        true,
        setOf(),
        setOf()
    )
    val taintSinkArgument = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(Ljava/lang/String;)V"),
        false,
        setOf(1),
        setOf()
    )
    val taintSinkArgumentLong = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(J)V"),
        false,
        setOf(1),
        setOf()
    )
    val taintSinkArgumentDouble = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(D)V"),
        false,
        setOf(1),
        setOf()
    )
    val taintSinkArgumentMultiple = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(Ljava/lang/String;Ljava/lang/String;)V"),
        false,
        setOf(2),
        setOf()
    )
    val taintSinkStatic = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "()V"),
        false,
        setOf(),
        setOf("A.s:Ljava/lang/String;")
    )
    val taintSinkReturn = JvmReturnTaintSink(MethodSignature("A", "sink", "(Ljava/lang/String;)Ljava/lang/String;"))

    val jvmTaintMemoryLocationBamCpaRunBuilder = JvmTaintMemoryLocationBamCpaRun.Builder().setReduceHeap(false)

    listOf(
        HashMapAbstractStateFactory.getInstance(),
        DifferentialMapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> { false },
        LimitedHashMapAbstractStateFactory { _, _, _ -> Optional.empty() }
    ).forEach { staticFieldMapAbstractStateFactory ->

        val testNameSuffix = " for static fields ${staticFieldMapAbstractStateFactory.javaClass.simpleName}"
        jvmTaintMemoryLocationBamCpaRunBuilder.setStaticFieldMapAbstractStateFactory(staticFieldMapAbstractStateFactory)

        "Simple interprocedural flows are reconstructed$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:3",
                    "JvmStackLocation(0)@LA;callee()Ljava/lang/String;:3"
                )
            )
        }

        "Interprocedural traces in callees are reconstructed$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;callee1()V:3",
                    "JvmStackLocation(0)@LA;callee2()Ljava/lang/String;:3"
                )
            )
        }

        "Multiple interprocedural traces are reconstructed$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1, taintSourceReturn2))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
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

            traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;callee(Ljava/lang/String;)V:1",
                    "JvmLocalVariableLocation(0)@LA;callee(Ljava/lang/String;)V:0",
                    "JvmStackLocation(0)@LA;main()V:3"
                ),
                listOf(
                    "JvmStackLocation(0)@LA;callee(Ljava/lang/String;)V:1",
                    "JvmLocalVariableLocation(0)@LA;callee(Ljava/lang/String;)V:0",
                    "JvmStackLocation(0)@LA;main()V:9"
                )
            )
        }

        "Interprocedural traces through static fields are reconstructed$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceStatic))
                .setTaintSinks(setOf(taintSinkStatic))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;main()V:3",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;callee()V:3"
                )
            )
        }

        "Explicit static updates are supported$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:6",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;main()V:3",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;callee()V:6",
                    "JvmStackLocation(0)@LA;callee()V:3"
                )
            )
        }

        "Intermediate library calls don't disrupt the trace$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:11",
                    "JvmLocalVariableLocation(1)@LA;main()V:10",
                    "JvmLocalVariableLocation(1)@LA;main()V:7",
                    "JvmLocalVariableLocation(1)@LA;main()V:4",
                    "JvmStackLocation(0)@LA;main()V:3"
                )
            )
        }

        "Intermediate program calls don't disrupt the trace$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:8",
                    "JvmLocalVariableLocation(1)@LA;main()V:7",
                    "JvmLocalVariableLocation(1)@LA;main()V:4",
                    "JvmStackLocation(0)@LA;main()V:3"
                )
            )
        }

        "Intermediate local variable updates don't disrupt the trace$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:7",
                    "JvmLocalVariableLocation(1)@LA;main()V:6",
                    "JvmLocalVariableLocation(1)@LA;main()V:5",
                    "JvmLocalVariableLocation(1)@LA;main()V:4",
                    "JvmStackLocation(0)@LA;main()V:3"
                )
            )
        }

        "Category 2 sink arguments are supported$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgumentLong))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:9",
                    "JvmStackLocation(0)@LA;main()V:8",
                    "JvmStackLocation(0)@LA;main()V:5",
                    "JvmLocalVariableLocation(1)@LA;main()V:4",
                    "JvmStackLocation(0)@LA;main()V:3"
                )
            )
        }

        "Sink argument position is calculated correctly$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgumentMultiple))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:6",
                    "JvmLocalVariableLocation(1)@LA;main()V:5",
                    "JvmLocalVariableLocation(1)@LA;main()V:4",
                    "JvmStackLocation(0)@LA;main()V:3"
                )
            )
        }

        "The ternary operator is supported$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
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

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main(Z)V:13",
                    "JvmStackLocation(0)@LA;getSource2()Ljava/lang/String;:3"
                ),
                listOf(
                    "JvmStackLocation(0)@LA;main(Z)V:13",
                    "JvmStackLocation(0)@LA;main(Z)V:7",
                    "JvmStackLocation(0)@LA;getSource1()Ljava/lang/String;:3"
                )
            )
        }

        // with the current version of JvmMemoryLocationTransferRelation exception paths are not supported yet
        "Exception paths are supported$testNameSuffix".config(enabled = false) {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:14",
                    "JvmLocalVariableLocation(1)@LA;main()V:13",
                    "JvmLocalVariableLocation(1)@LA;main()V:12",
                    "JvmLocalVariableLocation(1)@LA;main()V:4",
                    "JvmStackLocation(0)@LA;main()V:3"
                )
            )
        }

        "The trace does not go beyond the trace origin$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:10",
                    "JvmStackLocation(0)@LA;callee(Ljava/lang/String;)Ljava/lang/String;:3"
                )
            )
        }

        "The trace does not go through discarded abstract states$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1, taintSourceReturn2))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
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

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main(Z)V:17",
                    "JvmLocalVariableLocation(3)@LA;main(Z)V:16",
                    "JvmLocalVariableLocation(3)@LA;main(Z)V:9",
                    "JvmLocalVariableLocation(3)@LA;main(Z)V:8",
                    "JvmStackLocation(0)@LA;main(Z)V:7"
                ),
                listOf(
                    "JvmStackLocation(0)@LA;main(Z)V:17",
                    "JvmStackLocation(0)@LA;main(Z)V:13",
                    "JvmLocalVariableLocation(2)@LA;main(Z)V:12",
                    "JvmLocalVariableLocation(2)@LA;main(Z)V:9",
                    "JvmLocalVariableLocation(2)@LA;main(Z)V:8",
                    "JvmLocalVariableLocation(2)@LA;main(Z)V:7",
                    "JvmLocalVariableLocation(2)@LA;main(Z)V:4",
                    "JvmStackLocation(0)@LA;main(Z)V:3"
                )
            )
        }

        "Return state less or equal than the post call site is supported$testNameSuffix" {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1, taintSourceReturn2))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
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

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main(Z)V:19",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;main(Z)V:16",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;main(Z)V:10",
                    "JvmStackLocation(0)@LA;main(Z)V:7"
                ),
                listOf(
                    "JvmStackLocation(0)@LA;main(Z)V:19",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;main(Z)V:16",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;callee()V:6",
                    "JvmStackLocation(0)@LA;callee()V:3"
                )
            )
        }

        "Correct trace for recursion$testNameSuffix".config(enabled = false) {
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1, taintSourceReturn2))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
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

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main(Z)V:11",
                    "JvmStackLocation(0)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:23",
                    "JvmLocalVariableLocation(2)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:22",
                    "JvmLocalVariableLocation(2)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:11",
                    "JvmLocalVariableLocation(2)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:0",
                    "JvmStackLocation(1)@LA;main(Z)V:8, JvmStackLocation(0)@LA;main(Z)V:5"
                )
            )
        }

        "Regression test: loading a different static field does not break the trace$testNameSuffix" {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                ClassPoolBuilder.fromSource(
                    JavaSource(
                        "A.java",
                        """
                    class A {

                        public static String s;
                        public static String s1;
                    
                        public void main() {
                            callee();
                            s1 = "42";
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
                    ),
                    javacArguments = listOf("-source", "1.8", "-target", "1.8")
                ).programClassPool
            )
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkArgument))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            /*
        Bytecode of main:
            [0] invokestatic #2 = Methodref(A.callee()V)
            [3] ldc #3 = String("42")
            [5] putstatic #4 = Fieldref(A.s1 Ljava/lang/String;)
            [8] getstatic #7 = Fieldref(A.s Ljava/lang/String;)
            [11] invokestatic #8 = Methodref(A.sink(Ljava/lang/String;)V)
            [14] return
         */

            traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;main()V:11",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;main()V:8",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;main()V:5",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;main()V:3",
                    "JvmStaticFieldLocation(A.s:Ljava/lang/String;)@LA;callee()V:6",
                    "JvmStackLocation(0)@LA;callee()V:3"
                )
            )
        }

        "Simple interprocedural flows with return sink are reconstructed$testNameSuffix" {
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
                    
                        public static String sink(String s)
                        {
                            return s;
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
            /*
            Bytecode of main:
                [0] invokestatic #2 = Methodref(A.callee()Ljava/lang/String;)
                [3] invokestatic #3 = Methodref(A.sink(Ljava/lang/String;)Ljava/lang/String;)
                [6] pop
                [7] return

            Bytecode of callee:
                [0] invokestatic #4 = Methodref(A.source1()Ljava/lang/String;)
                [3] areturn

            Bytecode of sink:
                [0] aload_0 v0
                [1] areturn
             */
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturn1))
                .setTaintSinks(setOf(taintSinkReturn))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;sink(Ljava/lang/String;)Ljava/lang/String;:1",
                    "JvmLocalVariableLocation(0)@LA;sink(Ljava/lang/String;)Ljava/lang/String;:0",
                    "JvmStackLocation(0)@LA;main()V:3",
                    "JvmStackLocation(0)@LA;callee()Ljava/lang/String;:3"
                )
            )
        }

        "Simple interprocedural flows with doubles and return sink are reconstructed$testNameSuffix" {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                        class A {
                        
                            public void main() {
                                foo(source());
                            }
                        
                            public static double source()
                            {
                                return 0.0;
                            }
                            
                            public static void foo(double d)
                            {
                                sink(d);
                            }
                        
                            public static void sink(double d)
                            {
                                return;
                            }
                        }
                    """.trimIndent()
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8")
            )
            val interproceduralCfa = CfaUtil.createInterproceduralCfa(programClassPool)
            /*
            Bytecode of main:
                 [0] invokestatic #7 = Methodref(A.source()D)
                 [3] invokestatic #13 = Methodref(A.foo(D)V)
                 [6] return
            Bytecode of source:
                 [0] dconst_0
                 [1] dreturn
            Bytecode of foo:
                [0] dload_0 v0
                [1] invokestatic #17 = Methodref(A.sink(D)V)
                [4] return
            Bytecode of sink:
                [0] return
             */
            val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
            val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                .setCfa(interproceduralCfa)
                .setMainSignature(mainSignature)
                .setTaintSources(setOf(taintSourceReturnDouble))
                .setTaintSinks(setOf(taintSinkArgumentDouble))
                .build()
            val traces = taintMemoryLocationCpaRun.extractLinearTraces()
            interproceduralCfa.clear()

            traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                listOf(
                    "JvmStackLocation(0)@LA;foo(D)V:1",
                    "JvmLocalVariableLocation(0)@LA;foo(D)V:0",
                    "JvmStackLocation(0)@LA;main()V:3"
                )
            )
        }
    }
})
