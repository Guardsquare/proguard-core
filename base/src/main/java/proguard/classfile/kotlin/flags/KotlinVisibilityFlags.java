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


public class KotlinVisibilityFlags implements KotlinFlags
{
    // Valid for: class, constructor, function, synthetic function, property (including getter + setter), typeAlias

    /**
     * For top-level declarations : signifies visibility everywhere in the same module
     * For class/interface members: signifies visibility everywhere in the same module
     *                              to users who can has access to the declaring class
     */
    public boolean isInternal;

    /**
     * For top-level declarations: visible only inside the file containing the declaration
     * For class/interface members: visible only within the class
     */
    public boolean isPrivate;

    /**
     * For class/interface members: private + visible in subclasses
     */
    public boolean isProtected;

    /**
     * For top-level declarations: visible everywhere
     * For class/interface members: visible to everywhere to users who can access the
     *                              declaring class
     */
    public boolean isPublic;

    /**
     * For class/interface members: visible only on the same instance of the declaring class
     */
    public boolean isPrivateToThis;

    /**
     * Signifies that the declaration is declared inside a code block, not visible from outside
     */
    public boolean isLocal;
}
