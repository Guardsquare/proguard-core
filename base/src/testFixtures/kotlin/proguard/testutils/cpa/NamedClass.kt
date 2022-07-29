/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

package proguard.testutils.cpa

import proguard.classfile.ProgramClass
class NamedClass(val memberName: String) : ProgramClass() {
    private var superNameStr: String? = null
    constructor(memberName: String, vararg methods: NamedMember) : this(memberName) {
        this.methods = methods
    }
    override fun getName(): String {
        return memberName
    }
    fun setSuperName(superNameStr: String) {
        this.superNameStr = superNameStr
    }
    override fun getSuperName(): String {
        return superNameStr ?: ""
    }
}
