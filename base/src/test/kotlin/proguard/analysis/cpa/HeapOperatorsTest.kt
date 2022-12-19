package proguard.analysis.cpa

import io.kotest.assertions.fail
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.defaults.LatticeAbstractState
import proguard.analysis.cpa.defaults.MapAbstractState
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet
import proguard.analysis.cpa.defaults.SetAbstractState
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.domain.reference.CompositeHeapJvmAbstractState
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceAbstractState
import proguard.analysis.cpa.jvm.domain.reference.Reference
import proguard.analysis.cpa.jvm.domain.taint.JvmInvokeTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintMemoryLocationBamCpaRun
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource
import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.state.heap.HeapModel
import proguard.analysis.cpa.jvm.state.heap.tree.HeapNode
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapAbstractState
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapPrincipalAbstractState
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.jvm.witness.JvmStackLocation
import proguard.analysis.cpa.state.DifferentialMapAbstractStateFactory
import proguard.analysis.cpa.state.HashMapAbstractStateFactory
import proguard.analysis.cpa.state.LimitedHashMapAbstractStateFactory
import proguard.analysis.cpa.state.MapAbstractStateFactory
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import java.util.Optional
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Ignored
class HeapOperatorsTest : FreeSpec({

    fun <StateT : LatticeAbstractState<StateT>?> JvmTreeHeapAbstractState<StateT>.assertKeysCount(expected: Int) {
        val f = JvmTreeHeapAbstractState::class.memberProperties.find { it.name == "referenceToNode" }

        f?.let {
            it.isAccessible = true
            val referenceToNode = it.get(this) as MapAbstractState<Reference, HeapNode<StateT>>
            referenceToNode.size shouldBe expected
            return
        }

        fail("JvmTreeHeapAbstractState map is null")
    }

    fun <StateT : LatticeAbstractState<StateT>?> JvmTreeHeapAbstractState<StateT>.assertContainsKeys(expected: Set<Reference>) {
        val f = JvmTreeHeapAbstractState::class.memberProperties.find { it.name == "referenceToNode" }

        f?.let {
            it.isAccessible = true
            val referenceToNode = it.get(this) as MapAbstractState<Reference, HeapNode<StateT>>
            referenceToNode.keys.shouldContainAll(expected)
            return
        }

        fail("JvmTreeHeapAbstractState map is null")
    }

    fun <StateT : LatticeAbstractState<StateT>?> JvmTreeHeapAbstractState<StateT>.assertNotContainsKeys(expected: Set<Reference>) {
        val f = JvmTreeHeapAbstractState::class.memberProperties.find { it.name == "referenceToNode" }

        f?.let {
            it.isAccessible = true
            val referenceToNode = it.get(this) as MapAbstractState<Reference, HeapNode<StateT>>
            referenceToNode.keys.intersect(expected) shouldBe setOf()
            return
        }

        fail("JvmTreeHeapAbstractState map is null")
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

    val mainSignature = MethodSignature("A", "main", "()V")
    val calleeSignature = MethodSignature("A", "callee", "(LA\$B;)V")
    val initCSignature = MethodSignature("A\$C", "<init>", "(LA;)V")
    val initDSignature = MethodSignature("A\$D", "<init>", "(LA;)V")

    "Reduce and expand operators work as expected" - {
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
                    val testNameSuffix =
                        """
                            for the
                             static field map factory ${staticFieldMapAbstractStateFactory.javaClass.simpleName}
                             principal heap map factory ${principalHeapMapAbstractStateFactory.javaClass.simpleName},
                             follower heap map factory ${followerHeapMapAbstractStateFactory.javaClass.simpleName},
                             principal object map factory ${principalHeapNodeMapAbstractStateFactory.javaClass.simpleName},
                             and follower object map factory ${followerHeapNodeMapAbstractStateFactory.javaClass.simpleName}
                        """.trimIndent()

                    val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
                        ClassPoolBuilder.fromSource(
                            JavaSource(
                                "A.java",
                                """
                class A {
                
                    public static D d; 

                    public void main() {
                        B b = new B();
                        C c = new C();
                        d = new D();
                        callee(c.b);
                    }
                
                    public static void callee(B b){                   
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
                    
                    public class C {
                        public String s;
                        public B b = new B();                    
                        
                        public C() {                   
                            b.s = source1();
                        }
                    }
                    
                    public class D {
                        public String s;
                        public B b;
                        public C c;
                        
                        public D() {
                            b = new B();
                            c = new C();
                            s = source1();
                            b.s = source1();
                        }
                    }
                }
                                """.trimIndent()
                            ),
                            javacArguments = listOf("-source", "1.8", "-target", "1.8")
                        ).programClassPool
                    )

                    val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun.Builder()
                        .setCfa(interproceduralCfa)
                        .setMainSignature(mainSignature)
                        .setTaintSources(setOf(taintSourceReturn1))
                        .setTaintSinks(setOf(taintSinkArgument))
                        .setHeapModel(HeapModel.TREE)
                        .setReduceHeap(true)
                        .setStaticFieldMapAbstractStateFactory(staticFieldMapAbstractStateFactory)
                        .setPrincipalHeapMapAbstractStateFactory(principalHeapMapAbstractStateFactory)
                        .setFollowerHeapMapAbstractStateFactory(followerHeapMapAbstractStateFactory)
                        .setPrincipalHeapNodeMapAbstractStateFactory(principalHeapNodeMapAbstractStateFactory)
                        .setFollowerHeapNodeMapAbstractStateFactory(followerHeapNodeMapAbstractStateFactory)
                        .build()

                    /*
            Bytecode of main:
                [0] new #2 = Class(A$B)
                [3] dup
                [4] aload_0 v0
                [5] invokespecial #3 = Methodref(A$B.<init>(LA;)V)
                [8] astore_1 v1
                [9] new #4 = Class(A$C)
                [12] dup
                [13] aload_0 v0
                [14] invokespecial #5 = Methodref(A$C.<init>(LA;)V)
                [17] astore_2 v2
                [18] new #6 = Class(A$D)
                [21] dup
                [22] aload_0 v0
                [23] invokespecial #7 = Methodref(A$D.<init>(LA;)V)
                [26] putstatic #8 = Fieldref(A.d LA$D;)
                [29] aload_2 v2
                [30] getfield #9 = Fieldref(A$C.b LA$B;)
                [33] invokestatic #10 = Methodref(A.callee(LA$B;)V)
                [36] return

            Bytecode of callee:
                [0] aload_0 v0
                [1] getfield #11 = Fieldref(A$B.s Ljava/lang/String;)
                [4] invokestatic #12 = Methodref(A.sink(Ljava/lang/String;)V)
                [7] return

            Bytecode of D<init>:
                [0] aload_0 v0
                [1] aload_1 v1
                [2] putfield #1 = Fieldref(A$D.this$0 LA;)
                [5] aload_0 v0
                [6] invokespecial #2 = Methodref(java/lang/Object.<init>()V)
                [9] aload_0 v0
                [10] new #3 = Class(A$B)
                [13] dup
                [14] aload_1 v1
                [15] invokespecial #4 = Methodref(A$B.<init>(LA;)V)
                [18] putfield #5 = Fieldref(A$D.b LA$B;)
                [21] aload_0 v0
                [22] new #6 = Class(A$C)
                [25] dup
                [26] aload_1 v1
                [27] invokespecial #7 = Methodref(A$C.<init>(LA;)V)
                [30] putfield #8 = Fieldref(A$D.c LA$C;)
                [33] aload_0 v0
                [34] invokestatic #9 = Methodref(A.source1()Ljava/lang/String;)
                [37] putfield #10 = Fieldref(A$D.s Ljava/lang/String;)
                [40] aload_0 v0
                [41] getfield #5 = Fieldref(A$D.b LA$B;)
                [44] invokestatic #9 = Methodref(A.source1()Ljava/lang/String;)
                [47] putfield #11 = Fieldref(A$B.s Ljava/lang/String;)
                [50] return

             Bytecode of C<init>
                 [0] aload_0 v0
                 [1] aload_1 v1
                 [2] putfield #1 = Fieldref(A$C.this$0 LA;)
                 [5] aload_0 v0
                 [6] invokespecial #2 = Methodref(java/lang/Object.<init>()V)
                 [9] aload_0 v0
                 [10] new #3 = Class(A$B)
                 [13] dup
                 [14] aload_0 v0
                 [15] getfield #1 = Fieldref(A$C.this$0 LA;)
                 [18] invokespecial #4 = Methodref(A$B.<init>(LA;)V)
                 [21] putfield #5 = Fieldref(A$C.b LA$B;)
                 [24] aload_0 v0
                 [25] getfield #5 = Fieldref(A$C.b LA$B;)
                 [28] invokestatic #6 = Methodref(A.source1()Ljava/lang/String;)
                 [31] putfield #7 = Fieldref(A$B.s Ljava/lang/String;)
                 [34] return
             */

                    val traces = taintMemoryLocationCpaRun.extractLinearTraces()

                    val cache = taintMemoryLocationCpaRun.inputCpaRun.cpa.cache

                    val mainCacheEntries = cache.get(mainSignature)
                    val calleeCacheEntries = cache.get(calleeSignature)

                    "Correct cache size$testNameSuffix" {
                        mainCacheEntries.size shouldBe 1
                        calleeCacheEntries.size shouldBe 1
                    }

                    val mainCacheEntry = mainCacheEntries.first()
                    val calleeCacheEntry = calleeCacheEntries.first()

                    val callerState = (mainCacheEntry.reachedSet as ProgramLocationDependentReachedSet<JvmCfaNode, *, *, *>)
                        .getReached(interproceduralCfa.getFunctionNode(mainSignature, 33)).first() as CompositeHeapJvmAbstractState
                    val returnState = (mainCacheEntry.reachedSet as ProgramLocationDependentReachedSet<JvmCfaNode, *, *, *>)
                        .getReached(interproceduralCfa.getFunctionNode(mainSignature, 36)).first() as CompositeHeapJvmAbstractState
                    val reducedEntryState = (calleeCacheEntry.reachedSet as ProgramLocationDependentReachedSet<JvmCfaNode, *, *, *>)
                        .getReached(interproceduralCfa.getFunctionNode(calleeSignature, 0)).first() as CompositeHeapJvmAbstractState

                    val bMainRef = Reference(interproceduralCfa.getFunctionNode(mainSignature, 3), JvmStackLocation(0))
                    val cMainRef = Reference(interproceduralCfa.getFunctionNode(mainSignature, 12), JvmStackLocation(0))
                    val dMainRef = Reference(interproceduralCfa.getFunctionNode(mainSignature, 21), JvmStackLocation(0))
                    val bCRef = Reference(interproceduralCfa.getFunctionNode(initCSignature, 13), JvmStackLocation(0))
                    val bDRef = Reference(interproceduralCfa.getFunctionNode(initDSignature, 13), JvmStackLocation(0))
                    val cDRef = Reference(interproceduralCfa.getFunctionNode(initDSignature, 25), JvmStackLocation(0))

                    /*
            This test is acknowledgedly incomplete. A more complete test can check the entire tree and not just the keys.
             */
                    "Correct reduction for$testNameSuffix" - {

                        val callerPrincipalHeap = (callerState.getStateByName("Reference") as JvmReferenceAbstractState).heap as JvmTreeHeapPrincipalAbstractState
                        val reducedPrincipalHeap = (reducedEntryState.getStateByName("Reference") as JvmReferenceAbstractState).heap as JvmTreeHeapPrincipalAbstractState

                        "Correct number of states$testNameSuffix" {
                            callerPrincipalHeap.assertKeysCount(6)
                            reducedPrincipalHeap.assertKeysCount(4)
                        }

                        "Parameters kept$testNameSuffix" {
                            callerPrincipalHeap.assertContainsKeys(setOf(bCRef))
                            reducedPrincipalHeap.assertContainsKeys(setOf(bCRef))
                        }

                        "Static variables kept$testNameSuffix" {
                            callerPrincipalHeap.assertContainsKeys(setOf(dMainRef, bDRef, cDRef, bCRef))
                            reducedPrincipalHeap.assertContainsKeys(setOf(dMainRef, bDRef, cDRef, bCRef))
                        }

                        "References discarded$testNameSuffix" {
                            callerPrincipalHeap.assertContainsKeys(setOf(bMainRef, cMainRef))
                            reducedPrincipalHeap.assertNotContainsKeys(setOf(bMainRef, cMainRef))
                        }
                    }

                    /*
            This test is valid as long as callee just calls the sink without modifying the heap.
             */
                    "Correct expansion$testNameSuffix" {
                        (callerState.getStateByName("Reference") as JvmAbstractState<*>).heap shouldBe (returnState.getStateByName("Reference") as JvmAbstractState<*>).heap
                    }

                    interproceduralCfa.clear()

                    "Correct trace$testNameSuffix" {
                        traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                            listOf(
                                "JvmStackLocation(0)@LA;callee(LA\$B;)V:4",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA;callee(LA\$B;)V:1",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA;callee(LA\$B;)V:0",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA;main()V:33",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA;main()V:30",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA;main()V:29",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA;main()V:26",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$D;<init>(LA;)V:50",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$D;<init>(LA;)V:47",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$D;<init>(LA;)V:44",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$D;<init>(LA;)V:41",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$D;<init>(LA;)V:40",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$D;<init>(LA;)V:37",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$D;<init>(LA;)V:34",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$D;<init>(LA;)V:33",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$D;<init>(LA;)V:30",
                                "JvmHeapLocation([Reference(JvmStackLocation(0)@LA\$C;<init>(LA;)V:13)], A\$B#s)@LA\$C;<init>(LA;)V:34",
                                "JvmStackLocation(0)@LA\$C;<init>(LA;)V:31"
                            )
                        )
                    }
                }
            }
        }
    }
})
