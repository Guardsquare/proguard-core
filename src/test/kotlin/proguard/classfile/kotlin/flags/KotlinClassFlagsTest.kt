package proguard.classfile.kotlin.flags

import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import testutils.ReWritingMetadataVisitor

class KotlinClassFlagsTest : FreeSpec({

    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            class UsualClass {
                inner class InnerClass
                companion object CompanionClass { }
            }
            @JvmInline
            value class ValueClass(val param:String)
            inline class InlineClass(val param:String)
            data class DataClass(val param:String)
            interface InterfaceClass
            enum class EnumClass(val param:String) { EnumEntry("foo") }
            annotation class AnnotationClass
            object ObjectClass
            fun interface funInterfaceClass { fun invoke() }
            """.trimIndent()
        )
    )

    include(
        testClassFlags(programClassPool.getClass("UsualClass")) {
            it.isUsualClass shouldBe true
            it.isInterface shouldBe false
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe false
            it.isCompanionObject shouldBe false
            it.isInner shouldBe false
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe false
            @Suppress("DEPRECATION")
            it.isInline shouldBe false
            it.isFun shouldBe false
        }
    )

    include(
        testClassFlags(programClassPool.getClass("ValueClass")) {
            it.isUsualClass shouldBe true
            it.isInterface shouldBe false
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe false
            it.isCompanionObject shouldBe false
            it.isInner shouldBe false
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe true
            @Suppress("DEPRECATION")
            it.isInline shouldBe true
            it.isFun shouldBe false
        }
    )

    include(
        testClassFlags(programClassPool.getClass("InlineClass")) {
            it.isUsualClass shouldBe true
            it.isInterface shouldBe false
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe false
            it.isCompanionObject shouldBe false
            it.isInner shouldBe false
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe true
            @Suppress("DEPRECATION")
            it.isInline shouldBe true
            it.isFun shouldBe false
        }
    )

    include(
        testClassFlags(programClassPool.getClass("UsualClass\$InnerClass")) {
            it.isUsualClass shouldBe true
            it.isInterface shouldBe false
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe false
            it.isCompanionObject shouldBe false
            it.isInner shouldBe true
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe false
            @Suppress("DEPRECATION")
            it.isInline shouldBe false
            it.isFun shouldBe false
        }
    )

    include(
        testClassFlags(programClassPool.getClass("UsualClass\$CompanionClass")) {
            it.isUsualClass shouldBe false
            it.isInterface shouldBe false
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe false
            it.isCompanionObject shouldBe true
            it.isInner shouldBe false
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe false
            @Suppress("DEPRECATION")
            it.isInline shouldBe false
            it.isFun shouldBe false
        }
    )

    include(
        testClassFlags(programClassPool.getClass("DataClass")) {
            it.isUsualClass shouldBe true
            it.isInterface shouldBe false
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe false
            it.isCompanionObject shouldBe false
            it.isInner shouldBe false
            it.isData shouldBe true
            it.isExternal shouldBe false
            it.isValue shouldBe false
            @Suppress("DEPRECATION")
            it.isInline shouldBe false
            it.isFun shouldBe false
        }
    )

    include(
        testClassFlags(programClassPool.getClass("InterfaceClass")) {
            it.isUsualClass shouldBe false
            it.isInterface shouldBe true
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe false
            it.isCompanionObject shouldBe false
            it.isInner shouldBe false
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe false
            @Suppress("DEPRECATION")
            it.isInline shouldBe false
            it.isFun shouldBe false
        }
    )

    include(
        testClassFlags(programClassPool.getClass("EnumClass")) {
            it.isUsualClass shouldBe false
            it.isInterface shouldBe false
            it.isEnumClass shouldBe true
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe false
            it.isCompanionObject shouldBe false
            it.isInner shouldBe false
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe false
            @Suppress("DEPRECATION")
            it.isInline shouldBe false
            it.isFun shouldBe false
        }
    )

    // TODO EnumEntry

    include(
        testClassFlags(programClassPool.getClass("AnnotationClass")) {
            it.isUsualClass shouldBe false
            it.isInterface shouldBe false
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe true
            it.isObject shouldBe false
            it.isCompanionObject shouldBe false
            it.isInner shouldBe false
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe false
            @Suppress("DEPRECATION")
            it.isInline shouldBe false
            it.isFun shouldBe false
        }
    )

    include(
        testClassFlags(programClassPool.getClass("ObjectClass")) {
            it.isUsualClass shouldBe false
            it.isInterface shouldBe false
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe true
            it.isCompanionObject shouldBe false
            it.isInner shouldBe false
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe false
            @Suppress("DEPRECATION")
            it.isInline shouldBe false
            it.isFun shouldBe false
        }
    )

    include(
        testClassFlags(programClassPool.getClass("funInterfaceClass")) {
            it.isUsualClass shouldBe false
            it.isInterface shouldBe true
            it.isEnumClass shouldBe false
            it.isEnumEntry shouldBe false
            it.isAnnotationClass shouldBe false
            it.isObject shouldBe false
            it.isCompanionObject shouldBe false
            it.isInner shouldBe false
            it.isData shouldBe false
            it.isExternal shouldBe false
            it.isValue shouldBe false
            @Suppress("DEPRECATION")
            it.isInline shouldBe false
            it.isFun shouldBe true
        }
    )

    // TODO isExpect
    // TODO isExternal
})

internal fun testClassFlags(clazz: Clazz, flags: (KotlinClassFlags) -> Unit) = funSpec {

    test("${clazz.name} flags should be initialized correctly") {
        val kotlinClassVisitor = spyk<KotlinMetadataVisitor>()
        clazz.accept(ReferencedKotlinMetadataVisitor(kotlinClassVisitor))

        verify {
            kotlinClassVisitor.visitKotlinClassMetadata(
                clazz,
                withArg { flags.invoke(it.flags) }
            )
        }
    }

    test("${clazz.name} flags should be written and re-initialized correctly") {
        val kotlinClassVisitor = spyk<KotlinMetadataVisitor>()
        clazz.accept(ReWritingMetadataVisitor(kotlinClassVisitor))

        verify {
            kotlinClassVisitor.visitKotlinClassMetadata(
                clazz,
                withArg { flags.invoke(it.flags) }
            )
        }
    }
}
