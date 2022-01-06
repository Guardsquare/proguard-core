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
 * Flags for Kotlin classes.
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
public class KotlinClassFlags implements KotlinFlags
{

    public final KotlinVisibilityFlags visibility;
    public final KotlinModalityFlags   modality;
    public final KotlinCommonFlags     common;

    /**
     * A class kind flag, signifying that the corresponding class is a usual `class`.
     */
    public boolean isUsualClass;

    /**
     * A class kind flag, signifying that the corresponding class is an `interface`.
     */
    public boolean isInterface;

    /**
     * A class kind flag, signifying that the corresponding class is an `enum class`.
     */
    public boolean isEnumClass;

    /**
     * A class kind flag, signifying that the corresponding class is an enum entry.
     */
    public boolean isEnumEntry;

    /**
     * A class kind flag, signifying that the corresponding class is an `annotation class`.
     */
    public boolean isAnnotationClass;

    /**
     * A class kind flag, signifying that the corresponding class is a non-companion `object`.
     */
    public boolean isObject;

    /**
     * A class kind flag, signifying that the corresponding class is a `companion object`.
     */
    public boolean isCompanionObject;

    /**
     * Signifies that the corresponding class is `inner`.
     */
    public boolean isInner;

    /**
     * Signifies that the corresponding class is `data`.
     */
    public boolean isData;

    /**
     * Signifies that the corresponding class is `external`.
     */
    public boolean isExternal;

    /**
     * Signifies that the corresponding class is `expect`.
     */
    public boolean isExpect;

    /**
     * Signifies that the corresponding class is `inline`.
     */
    @Deprecated
    public boolean isInline;


    /**
     * Signifies that the corresponding class is `value`.
     */
    public boolean isValue;

    /**
     * Signifies that the corresponding class is a functional interface,
     * i.e. marked with the keyword `fun`.
     */
    public boolean isFun;


    public KotlinClassFlags(KotlinCommonFlags     common,
                            KotlinVisibilityFlags visibility,
                            KotlinModalityFlags   modality)
    {
        this.common     = common;
        this.visibility = visibility;
        this.modality   = modality;
    }
}
