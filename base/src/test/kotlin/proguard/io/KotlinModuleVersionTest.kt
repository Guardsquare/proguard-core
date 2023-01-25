/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 */

package proguard.io

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.metadata.internal.metadata.jvm.deserialization.JvmMetadataVersion
import proguard.resources.kotlinmodule.KotlinModule
import proguard.resources.kotlinmodule.io.KotlinModuleReader
import proguard.resources.kotlinmodule.io.KotlinModuleWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

class KotlinModuleVersionTest : FreeSpec({

    "When given a Kotlin Module file with a supported Kotlin metadata version" - {
        val base64encoded1dot7dot0ModuleFile = "AAAAAwAAAAEAAAAHAAAAAAAAAAAiACoA"
        val decodedBytes = Base64.getDecoder().decode(base64encoded1dot7dot0ModuleFile)
        // Read the module.
        val kotlinModule = KotlinModule("main.kotlin_module", decodedBytes.size.toLong())
        val kotlinModuleReader = KotlinModuleReader(ByteArrayInputStream(decodedBytes))
        kotlinModuleReader.visitKotlinModule(kotlinModule)
        kotlinModule.version.toArray() shouldBe arrayOf(1, 7, 0)

        "Then that version of the Kotlin metadata is written" {
            // Write the processed module.
            val outputStream = ByteArrayOutputStream()
            val kotlinModuleWriter = KotlinModuleWriter(outputStream)
            kotlinModuleWriter.visitKotlinModule(kotlinModule)

            // Read the processed module.
            val processedModuleBytes = outputStream.toByteArray()
            val processedModule = KotlinModule("main.kotlin_module", processedModuleBytes.size.toLong())
            val processedModuleReader = KotlinModuleReader(ByteArrayInputStream(processedModuleBytes))
            processedModuleReader.visitKotlinModule(processedModule)
            processedModule.version.toArray() shouldBe arrayOf(1, 7, 0)
        }
    }

    "When given a Kotlin Module file with an unsupported Kotlin metadata version" - {
        val base64encoded1dot1dot18ModuleFile = "AAAAAwAAAAEAAAABAAAAEiIAKgA="
        val decodedBytes = Base64.getDecoder().decode(base64encoded1dot1dot18ModuleFile)
        val kotlinModule = KotlinModule("kotlin_test.kotlin_module", decodedBytes.size.toLong())
        // Read the module.
        val kotlinModuleReader = KotlinModuleReader(ByteArrayInputStream(decodedBytes))
        kotlinModuleReader.visitKotlinModule(kotlinModule)
        kotlinModule.version.toArray() shouldBe arrayOf(1, 1, 18)

        "Then the COMPATIBLE version of the Kotlin metadata is written" {
            // Write the processed module.
            val outputStream = ByteArrayOutputStream()
            val kotlinModuleWriter = KotlinModuleWriter(outputStream)
            kotlinModuleWriter.visitKotlinModule(kotlinModule)

            // Read the processed module.
            val processedModuleBytes = outputStream.toByteArray()
            val processedModule = KotlinModule("main.kotlin_module", processedModuleBytes.size.toLong())
            val processedModuleReader = KotlinModuleReader(ByteArrayInputStream(processedModuleBytes))
            processedModuleReader.visitKotlinModule(processedModule)
            // The module should have the COMPATIBLE version of metadata.
            processedModule.version.toArray() shouldBe JvmMetadataVersion.INSTANCE.toArray()
        }
    }
})
