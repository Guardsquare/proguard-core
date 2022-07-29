/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package proguard.testutils

import java.io.File
import javax.lang.model.SourceVersion

val currentJavaVersion: Int by lazy {
    var version = System.getProperty("java.version")

    // Strip early access suffix
    if (version.endsWith("-ea")) {
        version = version.substring(0, version.length - 3)
    }

    if (version.startsWith("1.")) {
        version = version.substring(2, 3)
    } else {
        val dot = version.indexOf(".")
        if (dot != -1) {
            version = version.substring(0, dot)
        }
    }

    return@lazy version.toInt()
}

fun isJava9OrLater(): Boolean =
    SourceVersion.latestSupported() > SourceVersion.RELEASE_8

fun getCurrentJavaHome(): File =
    if (isJava9OrLater()) File(System.getProperty("java.home"))
    else File(System.getProperty("java.home")).parentFile
