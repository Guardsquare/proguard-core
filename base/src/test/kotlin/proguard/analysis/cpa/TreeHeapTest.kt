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

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.defaults.SetAbstractState
import proguard.analysis.cpa.jvm.domain.reference.Reference
import proguard.analysis.cpa.jvm.domain.taint.JvmInvokeTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintMemoryLocationBamCpaRun
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource
import proguard.analysis.cpa.jvm.state.heap.HeapModel
import proguard.analysis.cpa.jvm.state.heap.tree.HeapNode
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.state.DifferentialMapAbstractStateFactory
import proguard.analysis.cpa.state.HashMapAbstractStateFactory
import proguard.analysis.cpa.state.LimitedHashMapAbstractStateFactory
import proguard.analysis.cpa.state.MapAbstractStateFactory
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import java.util.Optional

@Ignored
class TreeHeapTest : StringSpec({

    fun booleanToPreposition(b: Boolean): String {
        return if (b) "with" else "without"
    }

    val taintSourceReturn1 = JvmTaintSource(
        MethodSignature("A", "source1", "()Ljava/lang/String;"),
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

    val jvmTaintMemoryLocationBamCpaRunBuilder = JvmTaintMemoryLocationBamCpaRun.Builder()
        .setTaintSources(setOf(taintSourceReturn1))
        .setTaintSinks(setOf(taintSinkArgument))
        .setHeapModel(HeapModel.TREE)

    listOf(
        HashMapAbstractStateFactory.getInstance(),
        DifferentialMapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> { false },
        LimitedHashMapAbstractStateFactory { _, _, _ -> Optional.empty() }
    ).forEach { staticFieldMapAbstractStateFactory ->
        listOf<Pair<MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<Reference>>>, MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<JvmTaintSource>>>>>(
            Pair(HashMapAbstractStateFactory.getInstance(), HashMapAbstractStateFactory.getInstance()),
            Pair(DifferentialMapAbstractStateFactory { false }, DifferentialMapAbstractStateFactory { false }),
            Pair(LimitedHashMapAbstractStateFactory { _, _, _ -> Optional.empty() }, LimitedHashMapAbstractStateFactory { _, _, _ -> Optional.empty() })
        ).forEach { (principalHeapMapAbstractStateFactory, followerHeapMapAbstractStateFactory) ->
            listOf<Pair<MapAbstractStateFactory<String, SetAbstractState<Reference>>, MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>>>>(
                Pair(HashMapAbstractStateFactory.getInstance(), HashMapAbstractStateFactory.getInstance()),
                Pair(DifferentialMapAbstractStateFactory { false }, DifferentialMapAbstractStateFactory { false }),
                Pair(LimitedHashMapAbstractStateFactory { _, _, _ -> Optional.empty() }, LimitedHashMapAbstractStateFactory { _, _, _ -> Optional.empty() })
            ).forEach { (principalHeapNodeMapAbstractStateFactory, followerHeapNodeMapAbstractStateFactory) ->
                listOf(false, true).forEach { reduceHeap ->

                    val testNameSuffix =
                        """
                            for the
                             static field map factory ${staticFieldMapAbstractStateFactory.javaClass.simpleName}
                             principal heap map factory ${principalHeapMapAbstractStateFactory.javaClass.simpleName},
                             follower heap map factory ${followerHeapMapAbstractStateFactory.javaClass.simpleName},
                             principal object map factory ${principalHeapNodeMapAbstractStateFactory.javaClass.simpleName},
                             follower object map factory ${followerHeapNodeMapAbstractStateFactory.javaClass.simpleName},
                             and ${booleanToPreposition(reduceHeap)} heap reduction
                        """.trimIndent()

                    jvmTaintMemoryLocationBamCpaRunBuilder
                        .setReduceHeap(reduceHeap)
                        .setStaticFieldMapAbstractStateFactory(staticFieldMapAbstractStateFactory)
                        .setPrincipalHeapMapAbstractStateFactory(principalHeapMapAbstractStateFactory)
                        .setFollowerHeapMapAbstractStateFactory(followerHeapMapAbstractStateFactory)
                        .setPrincipalHeapNodeMapAbstractStateFactory(principalHeapNodeMapAbstractStateFactory)
                        .setFollowerHeapNodeMapAbstractStateFactory(followerHeapNodeMapAbstractStateFactory)

                    "Method arguments are unaliased by default$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        traces.size shouldBe 0
                    }

                    "Explicit argument aliasing is supported$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()

                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()
                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;callee(LA\$B;LA\$B;)V:11",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA;main()V:3)], A\$B#s)@LA;callee(LA\$B;LA\$B;)V:8",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA;main()V:3)], A\$B#s)@LA;callee(LA\$B;LA\$B;)V:7",
                                "JvmStackLocation(0)@LA;callee(LA\$B;LA\$B;)V:4"
                            )
                        )
                    }

                    "Flow through an array element is detected$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;main()V:15",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:14",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:13",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:10",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:9",
                                "JvmStackLocation(0)@LA;main()V:8"
                            )
                        )
                    }

                    "All array elements are assumed to be aliased$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        // TODO adjust this test after the heap model refinement
                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;main()V:15",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:14",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:13",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:10",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:9",
                                "JvmStackLocation(0)@LA;main()V:8"
                            )
                        )
                    }

                    "All fields with the same name are aliased in complex objects$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;main()V:14",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], A#s)@LA;main()V:11",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], A#s)@LA;main()V:10",
                                "JvmStackLocation(0)@LA;main()V:7"
                            )
                        )
                    }

                    "Flows through nonstatic fields are supported$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;main()V:11",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], A#s)@LA;main()V:8",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], A#s)@LA;main()V:7",
                                "JvmStackLocation(0)@LA;main()V:4"
                            )
                        )
                    }

                    "Analysis of loops converges$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;main(Z)V:18",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0)], A#s)@LA;main(Z)V:15",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0)], A#s)@LA;main(Z)V:14",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0)], A#s)@LA;main(Z)V:1",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0)], A#s)@LA;main(Z)V:0",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0)], A#s)@LA;main(Z)V:11",
                                "JvmStackLocation(0)@LA;main(Z)V:8"
                            )
                        )
                    }

                    "Unaliased overwriting is supported$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        traces.size shouldBe 0
                    }

                    "Aliased overwriting results in a weak update preserving the taint$testNameSuffix" {
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
                            a.s = null;      // weak update preserves the taint
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;main(Z)V:30",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0), Reference(JvmStackLocation(0)@LA;main(Z)V:9)], A#s)@LA;main(Z)V:27",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0), Reference(JvmStackLocation(0)@LA;main(Z)V:9)], A#s)@LA;main(Z)V:26",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0), Reference(JvmStackLocation(0)@LA;main(Z)V:9)], A#s)@LA;main(Z)V:23",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0), Reference(JvmStackLocation(0)@LA;main(Z)V:9)], A#s)@LA;main(Z)V:22",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main(Z)V:0), Reference(JvmStackLocation(0)@LA;main(Z)V:9)], A#s)@LA;main(Z)V:21",
                                "JvmStackLocation(0)@LA;main(Z)V:18"
                            )
                        )
                    }

                    "Array overwriting results in a weak update preserving the taint$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;main()V:22",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:21",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:20",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:17",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:16",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:15",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:14",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:13",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:10",
                                "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)], [])@LA;main()V:9",
                                "JvmStackLocation(0)@LA;main()V:8"
                            )
                        )
                    }

                    "Multiple paths are reconstructed$testNameSuffix" {
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        /*
        Bytecode of main:
            [0] iload_1 v1
            [1] ifeq +9 (target=10)
            [4] invokestatic #2 = Methodref(A.callee1()V)
            [7] goto +6 (target=13)
            [10] invokestatic #3 = Methodref(A.callee2()V)
            [13] invokestatic #4 = Methodref(A.callee3()V)
            [16] return
        Bytecode of callee1:
            [0] getstatic #5 = Fieldref(A.b LA$B;)
            [3] invokestatic #6 = Methodref(A.source1()Ljava/lang/String;)
            [6] putfield #7 = Fieldref(A$B.s Ljava/lang/String;)
            [9] return
         */

                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;callee3()V:6",
                                "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;callee3()V:3",
                                "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;callee3()V:0",
                                "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;main(Z)V:13",
                                "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;callee2()V:9",
                                "JvmStackLocation(0)@LA;callee2()V:6"
                            ),
                            listOf(
                                "JvmStackLocation(0)@LA;callee3()V:6",
                                "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;callee3()V:3",
                                "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;callee3()V:0",
                                "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;main(Z)V:13",
                                "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;main(Z)V:7",
                                "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;callee1()V:9",
                                "JvmStackLocation(0)@LA;callee1()V:6"
                            )
                        )
                    }

                    "Regression test: trace not interrupted when same field of different reference is assigned$testNameSuffix" {
                        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                            ClassPoolBuilder.fromSource(
                                JavaSource(
                                    "A.java",
                                    """
                class A {

                    public void main() {
                        B b = new B();
                        B b1 = new B();
                        b.s = source1();
                        b1.s = "42";
                        sink(b.s);
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
                                ),
                                javacArguments = listOf("-source", "1.8", "-target", "1.8")
                            ).programClassPool
                        )
                        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
                        val taintMemoryLocationCpaRun = jvmTaintMemoryLocationBamCpaRunBuilder
                            .setCfa(interproceduralCfa)
                            .setMainSignature(mainSignature)
                            .build()
                        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                        interproceduralCfa.clear()

                        /*
        Bytecode of main:
            [0] new #2 = Class(A$B)
            [3] dup
            [4] aload_0 v0
            [5] invokespecial #3 = Methodref(A$B.<init>(LA;)V)
            [8] astore_1 v1
            [9] new #2 = Class(A$B)
            [12] dup
            [13] aload_0 v0
            [14] invokespecial #3 = Methodref(A$B.<init>(LA;)V)
            [17] astore_2 v2
            [18] aload_1 v1
            [19] invokestatic #4 = Methodref(A.source1()Ljava/lang/String;)
            [22] putfield #5 = Fieldref(A$B.s Ljava/lang/String;)
            [25] aload_2 v2
            [26] ldc #6 = String("42")
            [28] putfield #5 = Fieldref(A$B.s Ljava/lang/String;)
            [31] aload_1 v1
            [32] getfield #5 = Fieldref(A$B.s Ljava/lang/String;)
            [35] invokestatic #7 = Methodref(A.sink(Ljava/lang/String;)V)
            [38] return
         */

                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;main()V:35",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA;main()V:3)], A\$B#s)@LA;main()V:32",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA;main()V:3)], A\$B#s)@LA;main()V:31",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA;main()V:3)], A\$B#s)@LA;main()V:28",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA;main()V:3)], A\$B#s)@LA;main()V:26",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA;main()V:3)], A\$B#s)@LA;main()V:25",
                                "JvmStackLocation(0)@LA;main()V:22"
                            )
                        )
                    }
                }
            }
        }
    }
})
