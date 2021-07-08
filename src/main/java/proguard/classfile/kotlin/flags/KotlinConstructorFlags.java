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
package proguard.classfile.kotlin.flags;

/**
 * Flags for Kotlin constructors.
 *
 * Valid common flags:
 *   - hasAnnotations
 *   - isInternal
 *   - isPrivate
 *   - isProtected
 *   - isPublic
 *   - isPrivateToThis
 *   - isLocal
 */
public class KotlinConstructorFlags implements KotlinFlags
{

    public final KotlinCommonFlags     common;
    public final KotlinVisibilityFlags visibility;

    /**
     * Signifies that the corresponding constructor is the primary constructor.
     * @deprecated Use {@link #isSecondary} instead.
     */
    @Deprecated
    public boolean isPrimary;

    /**
     * Signifies that the corresponding constructor is secondary,
     * i.e. declared not in the class header, but in the class body.
     */
    public boolean isSecondary;


    /**
     * Signifies that the corresponding constructor has non-stable parameter names,
     * i.e. cannot be called with named arguments.
     */
    public boolean hasNonStableParameterNames;

    public KotlinConstructorFlags(KotlinCommonFlags common, KotlinVisibilityFlags visibility)
    {
        this.common     = common;
        this.visibility = visibility;
    }
}
