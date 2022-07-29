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

package proguard.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import proguard.classfile.ProgramClass
import proguard.classfile.constant.Utf8Constant
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder

class ProguardAssemblerTest : FreeSpec({
    "Given Java bytecode" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "A.jbc",
                """
        version 1.8;
        public class A extends java.lang.Object [
            SourceFile "A.java";
            ] {

            public void <init>() {
                 line 1
                 aload_0
                 invokespecial java.lang.Object#void <init>()
                 return
            }

            public static void main(java.lang.String[]) {
                line 3
                getstatic java.lang.System#java.io.PrintStream out
                ldc "hello"
                invokevirtual java.io.PrintStream#void println(java.lang.String)
                line 4
                return
            }

        }
        """
            )
        )
        "When the ClassPool object is created" - {
            programClassPool.shouldNotBeNull()
            "Then the count and name of the methods should match the bytecode" {
                val classA = programClassPool.getClass("A") as ProgramClass
                classA.methods.size shouldBe 2
                (classA.constantPool[classA.methods[0].u2nameIndex] as Utf8Constant).string shouldBe "<init>"
                (classA.constantPool[classA.methods[1].u2nameIndex] as Utf8Constant).string shouldBe "main"
            }
        }
    }
})
