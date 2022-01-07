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

package testutils

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.filter.SpecFilter
import io.kotest.core.filter.SpecFilterResult
import io.kotest.core.filter.SpecFilterResult.Exclude
import io.kotest.core.filter.SpecFilterResult.Include
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Suppress("UNUSED")
object TestConfig : AbstractProjectConfig() {
    override fun extensions(): List<Extension> {
        return listOf(RequiresJavaVersionAnnotationFilter())
    }
}

class RequiresJavaVersionAnnotationFilter : SpecFilter {
    override fun filter(kclass: KClass<*>): SpecFilterResult = if (with(kclass.findAnnotation<RequiresJavaVersion>()) {
        (this == null || (currentJavaVersion >= this.from && currentJavaVersion <= this.to))
    }
    ) Include else Exclude("Required Java version is not in range.")
}

@Target(AnnotationTarget.CLASS)
annotation class RequiresJavaVersion(val from: Int, val to: Int = Int.MAX_VALUE)
