package proguard.classfile.kotlin.flags

import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

class KotlinClassFlagsTest : FreeSpec({

    val (classFlagsTestPool, _) = ClassPoolBuilder.fromSource(
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
        testClassFlags(classFlagsTestPool.getClass("UsualClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    include(
        testClassFlags(classFlagsTestPool.getClass("ValueClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    include(
        testClassFlags(classFlagsTestPool.getClass("InlineClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    include(
        testClassFlags(classFlagsTestPool.getClass("UsualClass\$InnerClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    include(
        testClassFlags(classFlagsTestPool.getClass("UsualClass\$CompanionClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    include(
        testClassFlags(classFlagsTestPool.getClass("DataClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    include(
        testClassFlags(classFlagsTestPool.getClass("InterfaceClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    include(
        testClassFlags(classFlagsTestPool.getClass("EnumClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    // TODO EnumEntry

    include(
        testClassFlags(classFlagsTestPool.getClass("AnnotationClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    include(
        testClassFlags(classFlagsTestPool.getClass("ObjectClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    include(
        testClassFlags(classFlagsTestPool.getClass("funInterfaceClass")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe false
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    // TODO isExpect
    // TODO isExternal

    // JVM specific flag tests
    val (allJvmClassFlagTestPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
                interface InterfaceClass1 {
                    fun foo() {
                        print("bar")
                    }
                }
            """.trimIndent()
        ),
        kotlincArguments = listOf("-Xjvm-default=all")
    )

    include(
        testClassFlags(allJvmClassFlagTestPool.getClass("InterfaceClass1")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe true
            it.isCompiledInCompatibilityMode shouldBe false
        }
    )

    val (allCompatibilityJvmClassFlagTestPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
                interface InterfaceClass2 {
                    fun foo() {
                        print("bar")
                    }
                }
            """.trimIndent()
        ),
        kotlincArguments = listOf("-Xjvm-default=all-compatibility")
    )

    include(
        testClassFlags(allCompatibilityJvmClassFlagTestPool.getClass("InterfaceClass2")) {
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
            // JVM specific flags
            it.hasMethodBodiesInInterface shouldBe true
            it.isCompiledInCompatibilityMode shouldBe true
        }
    )
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
