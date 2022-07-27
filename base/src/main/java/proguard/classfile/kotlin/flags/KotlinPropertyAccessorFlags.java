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
 * Flags for Kotlin property accessors (getters/setters for properties).
 *
 * Valid common flags:
 *   - hasAnnotations
 *   - isInternal
 *   - isPrivate
 *   - isProtected
 *   - isPublic
 *   - isPrivateToThis
 *   - isLocal
 *   - isFinal
 *   - isOpen
 *   - isAbstract
 *   - isSealed
 */
public class KotlinPropertyAccessorFlags implements KotlinFlags
{
    public KotlinCommonFlags     common;
    public KotlinVisibilityFlags visibility;
    public KotlinModalityFlags   modality;

    /**
     * Signifies that the corresponding property is not default, i.e. it has a body and/or annotations in the source code.
     */
    public boolean isDefault;

    /**
     * Signifies that the corresponding property is `external`.
     */
    public boolean isExternal;

    /**
     * Signifies that the corresponding property is `inline`.
     */
    public boolean isInline;

    public KotlinPropertyAccessorFlags(KotlinCommonFlags common, KotlinVisibilityFlags visibility, KotlinModalityFlags modality)
    {
        this.common     = common;
        this.visibility = visibility;
        this.modality   = modality;
    }
}
