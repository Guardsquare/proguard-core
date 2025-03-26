package proguard.classfile.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.attribute.RecordComponentInfo
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.RequiresJavaVersion
import proguard.testutils.currentJavaVersion

@RequiresJavaVersion(15)
class ClassReferenceInitializerJavaRecordTest : FreeSpec({
    "Given a Java record class with a signature attribute" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Record.java",
                """
                public record Record<K>(Key<K> key) {}
                """.trimIndent(),
            ),
            JavaSource(
                "Key.java",
                """
                public interface Key<T> {}
                """.trimIndent(),
            ),
            initialize = false,
            javacArguments = if (currentJavaVersion == 15) {
                listOf("--enable-preview", "--release", "15")
            } else {
                emptyList()
            },
        )

        val recordClazz = programClassPool.getClass("Record")

        "Then the record class should not be null" {
            recordClazz shouldNotBe null
        }

        "Then initializing should visit the signature attributes correctly" {
            val initializer = spyk(ClassReferenceInitializer(programClassPool, libraryClassPool))

            programClassPool.classesAccept(initializer)

            verify(exactly = 1) {
                // The class attribute should be visited.
                initializer.visitSignatureAttribute(
                    recordClazz,
                    withArg {
                        it.getSignature(recordClazz) shouldBe "<K:Ljava/lang/Object;>Ljava/lang/Record;"
                    },
                )

                // The record component attribute should be visited.
                initializer.visitSignatureAttribute(
                    recordClazz,
                    ofType<RecordComponentInfo>(),
                    withArg {
                        it.getSignature(recordClazz) shouldBe "LKey<TK;>;"
                    },
                )
            }

            verify(exactly = 0) {
                // The record component attribute should not be visited as generic attribute.
                // This happens if the ClassReferenceInitializer does not override
                // the RecordComponentInfo specific method.
                initializer.visitSignatureAttribute(
                    recordClazz,
                    withArg {
                        it.getSignature(recordClazz) shouldBe "LKey<TK;>;"
                    },
                )
            }
        }
    }
})
