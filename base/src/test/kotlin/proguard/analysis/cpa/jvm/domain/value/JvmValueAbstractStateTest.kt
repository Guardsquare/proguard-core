package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.defaults.HashMapAbstractState
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState
import proguard.analysis.cpa.jvm.state.heap.tree.JvmShallowHeapAbstractState
import proguard.classfile.MethodSignature
import proguard.classfile.MethodSignature.UNKNOWN
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.value.BasicValueFactory

class JvmValueAbstractStateTest : FreeSpec({
    "Given different instance but equivalent states" - {
        val programLocation = JvmCfaNode(MethodSignature.UNKNOWN, 0, null)
        val valueFactory = BasicValueFactory()
        val a = JvmValueAbstractState(
            valueFactory,
            ExecutingInvocationUnit.Builder().build(valueFactory),
            programLocation,
            JvmFrameAbstractState(),
            JvmShallowHeapAbstractState(HashMapAbstractState(), JvmCfaNode::class.java, ValueAbstractState.UNKNOWN),
            HashMapAbstractState()
        )
        val b = JvmValueAbstractState(
            valueFactory,
            ExecutingInvocationUnit.Builder().build(valueFactory),
            programLocation,
            JvmFrameAbstractState(),
            JvmShallowHeapAbstractState(HashMapAbstractState(), JvmCfaNode::class.java, ValueAbstractState.UNKNOWN),
            HashMapAbstractState()
        )

        "They should be equal" {
            a shouldBe b
        }

        "They should have the same hashCode" {
            a.hashCode() shouldBe b.hashCode()
        }

        "When putting one in a hash set" - {
            val set = mutableSetOf<JvmValueAbstractState>()
            set.add(a)

            "Then it should contain the other" {
                set.contains(b) shouldBe true
            }
        }

        "When modifying one it should still be contained in the set after modifying" {
            val set = mutableSetOf<JvmValueAbstractState>()
            set.add(a)
            a.staticFields.put("MyField", ValueAbstractState.UNKNOWN)
            set.contains(a) shouldBe true
        }
    }
})
