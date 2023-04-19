package proguard.analysis.cpa

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class CfaUtilTest : FreeSpec({
    "Static false shouldAnalyzeNextCodeAttribute skips all attributes" {
        val cfa = CfaUtil.createIntraproceduralCfaFromClassPool(
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
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8")
            ).programClassPool
        ) { false }

        cfa.isEmpty() shouldBe true
    }
})
