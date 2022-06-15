/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 */

package proguard.io

import io.kotest.core.spec.style.FreeSpec
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.LibraryClass
import proguard.classfile.io.LibraryClassReader
import proguard.classfile.io.LibraryClassReader.KotlinMetadataElementValueConsumer
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.Base64

class LibraryClassReaderTest : FreeSpec({

    "Given a Kotlin library class" - {
        val dataInput = DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(TEST_CLASS)))
        val initializer = spyk<KotlinMetadataElementValueConsumer>()
        "When using the LibraryClassReader" - {
            val libraryClassReader = LibraryClassReader(dataInput, false, false, initializer)

            "Then the Kotlin metadata should be read" {
                libraryClassReader.visitLibraryClass(LibraryClass())

                verify {
                    initializer.accept(
                        /* k = */ 1,
                        /* mv = */ intArrayOf(1, 7, 1),
                        /* d1 = */ ofType<Array<String>>(),
                        /* d2 = */ ofType<Array<String>>(),
                        /* xi = */ 48,
                        /* xs = */ null,
                        /* pn = */ null
                    )
                }
            }
        }
    }

    "Given a Kotlin library class with an invalid field" - {
        val dataInput = DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(TEST_CLASS_INVALID_FIELD)))
        val initializer = spyk<KotlinMetadataElementValueConsumer>()
        "When using the LibraryClassReader" - {
            val libraryClassReader = LibraryClassReader(dataInput, false, false, initializer)

            "Then the Kotlin metadata should be read as much as possible" {
                libraryClassReader.visitLibraryClass(LibraryClass())

                verify {
                    // K is -1 because it was not initialized, since the field did not exist.
                    // The LibraryClassReader should not crash in this case, the `KotlinMetadataInitializer`
                    // can be provided these values which will log a warning about invalid metadata.

                    initializer.accept(
                        /* k = */ -1,
                        /* mv = */ intArrayOf(1, 7, 1),
                        /* d1 = */ ofType<Array<String>>(),
                        /* d2 = */ ofType<Array<String>>(),
                        /* xi = */ 48,
                        /* xs = */ null,
                        /* pn = */ null
                    )
                }
            }
        }
    }
})

/* Compiled with Kotlin 1.7
class Test {
    fun foo() { }
}
*/
private val TEST_CLASS = """
yv66vgAAADQAHQEABFRlc3QHAAEBABBqYXZhL2xhbmcvT2JqZWN0BwADAQAGPGluaXQ+AQADKClW
DAAFAAYKAAQABwEABHRoaXMBAAZMVGVzdDsBAANmb28BABFMa290bGluL01ldGFkYXRhOwEAAm12
AwAAAAEDAAAABwEAAWsBAAJ4aQMAAAAwAQACZDEBACvAgBAKAhgCCgIQwIAKAggCCgIQAhjAgDIC
MAFCBcKiBgIQAkoGEAMaAjAEAQACZDIBAAABAAdUZXN0Lmt0AQAEQ29kZQEAD0xpbmVOdW1iZXJU
YWJsZQEAEkxvY2FsVmFyaWFibGVUYWJsZQEAClNvdXJjZUZpbGUBABlSdW50aW1lVmlzaWJsZUFu
bm90YXRpb25zADEAAgAEAAAAAAACAAEABQAGAAEAGAAAAC8AAQABAAAABSq3AAixAAAAAgAZAAAA
BgABAAAAAQAaAAAADAABAAAABQAJAAoAAAARAAsABgABABgAAAArAAAAAQAAAAGxAAAAAgAZAAAA
BgABAAAABAAaAAAADAABAAAAAQAJAAoAAAACABsAAAACABcAHAAAADoAAQAMAAUADVsAA0kADkkA
D0kADgAQSQAOABFJABIAE1sAAXMAFAAVWwAFcwAKcwAWcwAGcwALcwAW
""".trimIndent().replace("\n", "")

// The `k` field is renamed to `invalid`.
private val TEST_CLASS_INVALID_FIELD = """
yv66vgAAADQAEwMAAAABAwAAAAIDAAAABwMAAAAwBwALBwAPAQAAAQASwIAGCsCACgIQAhoGEMCA
GhABAQARTGtvdGxpbi9NZXRhZGF0YTsBABlSdW50aW1lVmlzaWJsZUFubm90YXRpb25zAQAEVGVz
dAEAAmQxAQACZDIBAAdpbnZhbGlkAQAQamF2YS9sYW5nL09iamVjdAEABG1haW4BAAJtdgEAAnhp
ADEABQAGAAAAAAAAAAEACgAAADEAAQAJAAUAEVsAA0kAAUkAA0kAAQAOSQACABJJAAQADFsAAXMA
CAANWwACcwAQcwAH
""".trimIndent().replace("\n", "")
