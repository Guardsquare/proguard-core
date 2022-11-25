package proguard.analysis.cpa

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintAbstractState
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintBamCpaRun
import proguard.analysis.cpa.jvm.state.heap.HeapModel
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.util.StateNames
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class JvmTaintBamCpaRunTest : FreeSpec({

    "The reduce operator produces an abstract state of correct type" {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A
                    {

                        public void main()
                        {
                            callee();
                        }

                        public static void callee()
                        {
                        }
                    }
                    """.trimIndent()
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8")
            ).programClassPool
        )
        val calleeSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("callee") }.findFirst().get().signature
        val location = interproceduralCfa.getFunctionNode(calleeSignature, 0)
        val mainSignature = interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst().get().signature
        val taintCpaRun = JvmTaintBamCpaRun
            .Builder()
            .setCfa(interproceduralCfa)
            .setMainSignature(mainSignature)
            .setTaintSources(setOf())
            .setHeapModel(HeapModel.TAINT_TREE)
            .build()
        taintCpaRun.execute()
        interproceduralCfa.clear()

        (taintCpaRun.cpa.cache.get(calleeSignature).first().reachedSet as ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmTaintAbstractState, MethodSignature>)
            .getReached(location)
            .first()
            .getStateByName(StateNames.Jvm)
            .shouldBeInstanceOf<JvmTaintAbstractState>()
    }
})
