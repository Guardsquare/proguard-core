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
 * Flags for Kotlin properties.
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
public class KotlinPropertyFlags implements KotlinFlags
{

    public final KotlinCommonFlags     common;
    public final KotlinVisibilityFlags visibility;
    public final KotlinModalityFlags   modality;

    /**
     * A member kind flag, signifying that the corresponding property is explicitly declared in the containing class.
     */
    public boolean isDeclared;

    /**
     * A member kind flag, signifying that the corresponding property exists in the containing class because a property with a suitable
     * signature exists in a supertype. This flag is not written by the Kotlin compiler and its effects are unspecified.
     */
    public boolean isFakeOverride;

    /**
     * A member kind flag, signifying that the corresponding property exists in the containing class because it has been produced
     * by interface delegation (delegation "by"). Kotlinc never writes this flag.
     */
    public boolean isDelegation;

    /**
     * A member kind flag, signifying that the corresponding property exists in the containing class because it has been synthesized
     * by the compiler and has no declaration in the source code.
     */
    public boolean isSynthesized;

    /**
     * Signifies that the corresponding property is `var`.
     */
    public boolean isVar;

    /**
     * Signifies that the corresponding property has a getter.
     */
    public boolean hasGetter;

    /**
     * Signifies that the corresponding property has a setter.
     */
    public boolean hasSetter;

    /**
     * Signifies that the corresponding property is `const`.
     */
    public boolean isConst;

    /**
     * Signifies that the corresponding property is `lateinit`.
     */
    public boolean isLateinit;

    /**
     * Signifies that the corresponding property has a constant value. On JVM, this flag allows an optimization similarly to
     * [F.HAS_ANNOTATIONS]: constant values of properties are written to the bytecode directly, and this flag can be used to avoid
     * reading the value from the bytecode in case there isn't one.
     */
    public boolean hasConstant;

    /**
     * Signifies that the corresponding property is `external`.
     */
    public boolean isExternal;

    /**
     * Signifies that the corresponding property is a delegated property.
     */
    public boolean isDelegated;

    /**
     * Signifies that the corresponding property is `expect`.
     */
    public boolean isExpect;

    //JVM Specific flags

    /**
     * Signifies that its backing field is declared as a static field in an interface,
     * usually happens when @JvmField annotation is used e.g.
     *
     * interface A {
     *     companion object {
     *          @JvmField
     *          val s:String = "string"
     *     }
     * }
     *
     */
    public boolean isMovedFromInterfaceCompanion;


    public KotlinPropertyFlags(KotlinCommonFlags     common,
                               KotlinVisibilityFlags visibility,
                               KotlinModalityFlags   modality)
    {
        this.common     = common;
        this.visibility = visibility;
        this.modality   = modality;
    }
}
