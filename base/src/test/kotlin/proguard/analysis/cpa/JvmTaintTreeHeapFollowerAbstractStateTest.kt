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
import io.kotest.core.spec.style.FreeSpec
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
class JvmTaintTreeHeapFollowerAbstractStateTest : FreeSpec({

    fun booleanToPreposition(b: Boolean): String {
        return if (b) "with" else "without"
    }

    fun depthToInterprocedurality(i: Int): String {
        return if (i == -1) "interprocedural" else "intraprocedural"
    }

    val taintSourceReturn1 = JvmTaintSource(
        MethodSignature("A", "source1", "()LA\$B;"),
        false,
        true,
        setOf(),
        setOf()
    )

    val taintSourceArg1 = JvmTaintSource(
        MethodSignature("A", "source1", "(LA\$B;)V"),
        false,
        false,
        setOf(1),
        setOf()
    )

    val taintSourceArg2 = JvmTaintSource(
        MethodSignature("A", "source2", "(LA\$B;)V"),
        false,
        false,
        setOf(1),
        setOf()
    )

    val taintSourceArg3 = JvmTaintSource(
        MethodSignature("A", "source1", "(LA\$B;Ljava/lang/Object;)V"),
        false,
        false,
        setOf(1),
        setOf()
    )

    val taintSourceArg4 = JvmTaintSource(
        MethodSignature("A", "source1", "(Ljava/lang/Object;LA\$B;)V"),
        false,
        false,
        setOf(2),
        setOf()
    )

    val taintSourceArg5 = JvmTaintSource(
        MethodSignature("A", "source1", "(LA\$B;J)V"),
        false,
        false,
        setOf(1),
        setOf()
    )

    val taintSourceArg6 = JvmTaintSource(
        MethodSignature("A", "source1", "([I)V"),
        false,
        false,
        setOf(1),
        setOf()
    )

    val taintSourceInstance1 = JvmTaintSource(
        MethodSignature("A", "source1", "()V"),
        true,
        false,
        setOf(),
        setOf()
    )

    val taintSourceStatic1 = JvmTaintSource(
        MethodSignature("A", "source1", "()V"),
        false,
        false,
        setOf(),
        setOf("A.b")
    )

    val taintSinkArgument1 = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(Ljava/lang/String;)V"),
        false,
        setOf(1),
        setOf()
    )

    val taintSinkArgument2 = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(LA\$B;)V"),
        false,
        setOf(1),
        setOf()
    )

    val taintSinkArgument3 = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(I)V"),
        false,
        setOf(1),
        setOf()
    )

    val jvmTaintMemoryLocationBamCpaRunBuilder = JvmTaintMemoryLocationBamCpaRun.Builder()
        .setTaintSinks(setOf(taintSinkArgument1))
        .setHeapModel(HeapModel.TAINT_TREE)

    "Object tainting behaves as expected" - {

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
                        listOf(-1, 0).forEach { stackDepth ->

                            val testNameSuffix =
                                """
                            for the
                             static field map factory ${staticFieldMapAbstractStateFactory.javaClass.simpleName}
                             principal heap map factory ${principalHeapMapAbstractStateFactory.javaClass.simpleName},
                             follower heap map factory ${followerHeapMapAbstractStateFactory.javaClass.simpleName},
                             principal object map factory ${principalHeapNodeMapAbstractStateFactory.javaClass.simpleName},
                             follower object map factory ${followerHeapNodeMapAbstractStateFactory.javaClass.simpleName},
                             ${booleanToPreposition(reduceHeap)} heap reduction,
                             and ${depthToInterprocedurality(stackDepth)} analysis
                                """.trimIndent()

                            jvmTaintMemoryLocationBamCpaRunBuilder
                                .setReduceHeap(reduceHeap)
                                .setStaticFieldMapAbstractStateFactory(staticFieldMapAbstractStateFactory)
                                .setPrincipalHeapMapAbstractStateFactory(principalHeapMapAbstractStateFactory)
                                .setFollowerHeapMapAbstractStateFactory(followerHeapMapAbstractStateFactory)
                                .setPrincipalHeapNodeMapAbstractStateFactory(principalHeapNodeMapAbstractStateFactory)
                                .setFollowerHeapNodeMapAbstractStateFactory(followerHeapNodeMapAbstractStateFactory)
                                .setMaxCallStackDepth(stackDepth)

                            "Arguments can be tainted$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b){
                                                    source1(b);
                                                    sink(b);
                                                }
                                            
                                                public static void sink(B b)
                                                {
                                                }
                                            
                                                public static void source1(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg1))
                                    .setTaintSinks(setOf(taintSinkArgument2))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;)V:5",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:4"
                                    )
                                )
                            }

                            "Argument fields are tainted$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b){
                                                    source1(b);
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;)V:8",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:5",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:4"
                                    )
                                )
                            }

                            "Null return fields are not tainted$testNameSuffix" {
                                // the test makes sense only interprocedurally
                                if (stackDepth > 0 || stackDepth == -1) {
                                    val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                        ClassPoolBuilder.fromSource(
                                            JavaSource(
                                                "A.java",
                                                """
                                            class A {
                        
                                                public void main(){
                                                    sink(source1().s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static B source1()
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
                                        .setTaintSources(setOf(taintSourceReturn1))
                                        .setTaintSinks(setOf(taintSinkArgument1))
                                        .build()
                                    val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                    interproceduralCfa.clear()

                                    traces.size shouldBe 0
                                }
                            }

                            "Return fields are tainted$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(){
                                                    sink(source1().s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static B source1()
                                                {
                                                    return new B();
                                                }
                                            
                                                public static class B {
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
                                    .setTaintSources(setOf(taintSourceReturn1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main()V:6",
                                        "JvmStackLocation(0)@LA;main()V:3"
                                    )
                                )
                            }

                            "Argument field taint can be overwritten$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b){
                                                    source1(b);
                                                    b.s = null;
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.size shouldBe 0
                            }

                            "Return field taint can be overwritten$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(){
                                                    B b = source1();
                                                    b.s = null;
                                                    sink(source1().s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public B source1()
                                                {
                                                    return new B();
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
                                    .setTaintSources(setOf(taintSourceArg1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.size shouldBe 0
                            }

                            "Calling instance can be tainted$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                                            
                                                private String s;
                        
                                                public void main(){
                                                    source1();
                                                    sink(s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public void source1()
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
                                    .setTaintSources(setOf(taintSourceInstance1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()
                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main()V:8",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)]@LA;main()V:5",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(0)@LA;main()V:0)]@LA;main()V:4"
                                    )
                                )
                            }

                            "Static field fields can be tainted$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                                            
                                                public static B b;
                        
                                                public void main(){
                                                    source1();
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public void source1()
                                                {
                                                }
                                                
                                                public static class B {
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
                                    .setTaintSources(setOf(taintSourceStatic1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()
                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main()V:10",
                                        "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;main()V:7",
                                        "JvmHeapLocation([Reference(JvmStaticFieldLocation(A.b)@unknown], A\$B#s)@LA;main()V:4"
                                    ),
                                    listOf(
                                        "JvmStackLocation(0)@LA;main()V:10",
                                        "JvmStackLocation(0)@LA;main()V:7",
                                        "JvmStaticFieldLocation(A.b)@LA;main()V:4"
                                    )
                                )
                            }

                            "Array elements can be tainted$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(int[] a){
                                                    source1(a);
                                                    sink(a[0]);
                                                }
                                            
                                                public static void sink(int a)
                                                {
                                                }
                                            
                                                public static void source1(int[] a)
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
                                    .setTaintSources(setOf(taintSourceArg6))
                                    .setTaintSinks(setOf(taintSinkArgument3))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()
                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main([I)V:7",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main([I)V:0)]@LA;main([I)V:6",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main([I)V:0)]@LA;main([I)V:5",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main([I)V:0)]@LA;main([I)V:4"
                                    )
                                )
                            }

                            "Object taints are accumulated$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b){
                                                    source1(b);
                                                    source2(b);
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b)
                                                {
                                                }
                                                
                                                public static void source2(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg1, taintSourceArg2))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;)V:12",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:9",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:8",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:5",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:4"
                                    )
                                )
                            }

                            "First argument position is calculated correctly$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b){
                                                    source1(b, null);
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b, Object o)
                                                {
                                                }
                                                
                                                public static void source2(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg3))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;)V:9",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:6",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:5"
                                    )
                                )
                            }

                            "Last argument position is calculated correctly$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b){
                                                    source1(null, b);
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(Object o, B b)
                                                {
                                                }
                                                
                                                public static void source2(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg4))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;)V:9",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:6",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:5"
                                    )
                                )
                            }

                            "Argument position is calculated correctly for category 2 irrelevant argument$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b){
                                                    source1(b, 0);
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b, long l)
                                                {
                                                }
                                                
                                                public static void source2(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg5))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;)V:9",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:6",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:5"
                                    )
                                )
                            }

                            "Object taint propagates to discovered fields$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b){
                                                    b.s = null;
                                                    source1(b);
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;)V:13",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:10",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;)V:0)]@LA;main(LA\$B;)V:9"
                                    )
                                )
                            }

                            "Object taint propagates to discovered fields upon right join$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b, boolean cond){
                                                    if (cond)
                                                    {
                                                        source1(b);
                                                    }
                                                    else
                                                    {
                                                        b.s = null;
                                                    }
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;Z)V:20",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:17",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:16",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:8"
                                    )
                                )
                            }

                            "Object taint propagates to discovered fields upon left join$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b, boolean cond){
                                                    if (cond)
                                                    {
                                                        b.s = null;
                                                    }
                                                    else
                                                    {
                                                        source1(b);
                                                    }
                                                    sink(b.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b)
                                                {
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
                                    .setTaintSources(setOf(taintSourceArg1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;Z)V:20",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:17",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:16"
                                    )
                                )
                            }

                            "Object taint transitively propagates to discovered fields upon right join$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b, boolean cond){
                                                    if (cond)
                                                    {
                                                        source1(b);
                                                    }
                                                    else
                                                    {
                                                        b.c.s = "";
                                                    }
                                                    sink(b.c.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b)
                                                {
                                                }
                                            
                                                public class B {
                                                    public C c;
                                                }
                                                
                                                public class C {
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
                                    .setTaintSources(setOf(taintSourceArg1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;Z)V:27",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)], A\$C#s)@LA;main(LA\$B;Z)V:24",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)], A\$C#s)@LA;main(LA\$B;Z)V:21",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:20",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:8"
                                    ),
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;Z)V:27",
                                        "JvmStackLocation(0)@LA;main(LA\$B;Z)V:24",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:21",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:20",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:8"
                                    )
                                )
                            }

                            "Object taint transitively propagates to discovered fields upon left join$testNameSuffix" {
                                val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                                    ClassPoolBuilder.fromSource(
                                        JavaSource(
                                            "A.java",
                                            """
                                            class A {
                        
                                                public void main(B b, boolean cond){
                                                    if (cond)
                                                    {
                                                        b.c.s = "";
                                                    }
                                                    else
                                                    {
                                                        source1(b);
                                                    }
                                                    sink(b.c.s);
                                                }
                                            
                                                public static void sink(String s)
                                                {
                                                }
                                            
                                                public static void source1(B b)
                                                {
                                                }
                                            
                                                public class B {
                                                    public C c;
                                                }
                                                
                                                public class C {
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
                                    .setTaintSources(setOf(taintSourceArg1))
                                    .setTaintSinks(setOf(taintSinkArgument1))
                                    .build()
                                val traces = taintMemoryLocationCpaRun.extractLinearTraces()
                                interproceduralCfa.clear()

                                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;Z)V:27",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)], A\$C#s)@LA;main(LA\$B;Z)V:24",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)], A\$C#s)@LA;main(LA\$B;Z)V:21",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:20"
                                    ),
                                    listOf(
                                        "JvmStackLocation(0)@LA;main(LA\$B;Z)V:27",
                                        "JvmStackLocation(0)@LA;main(LA\$B;Z)V:24",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:21",
                                        "JvmHeapLocation([Reference(JvmLocalVariableLocation(1)@LA;main(LA\$B;Z)V:0)]@LA;main(LA\$B;Z)V:20"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
})
