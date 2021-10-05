/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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
package proguard.util;

/**
 * Constants used by ProGuard for marking entities (classes, class members,
 * resource files, ...) during processing.
 *
 * @author Johan Leys
 */
public class ProcessingFlags
{
    // External configuration flags.
    public static final int DONT_SHRINK         = 0x00100000; // Marks whether an entity should not be shrunk.
    public static final int DONT_OPTIMIZE       = 0x00200000; // Marks whether an entity should not be optimized.
    public static final int DONT_OBFUSCATE      = 0x00400000; // Marks whether an entity should not be obfuscated.

    // Internal processing flags.
    public static final int IS_CLASS_AVAILABLE            = 0x00000001; // Marks whether a class member can be used for generalization or specialization.
    public static final int REMOVED_FIELDS                = 0x00000004; // Marks whether a class has (at least one) fields removed.
    public static final int REMOVED_PUBLIC_FIELDS         = 0x00000008; // Marks whether a class has (at least one) public fields removed.
    public static final int REMOVED_CONSTRUCTORS          = 0x00000010; // Marks whether a class has (at least one) constructors removed.
    public static final int REMOVED_PUBLIC_CONSTRUCTORS   = 0x00000020; // Marks whether a class has (at least one) public constructors removed.
    public static final int REMOVED_METHODS               = 0x00000040; // Marks whether a class has (at least one) methods removed.
    public static final int REMOVED_PUBLIC_METHODS        = 0x00000080; // Marks whether a class has (at least one) public methods removed.
    public static final int INJECTED                      = 0x00000200; // Marks whether an entity was injected by DexGuard.
    public static final int DONT_PROCESS_KOTLIN_MODULE    = 0x00002000; // Marks whether to processing a Kotlin module file.
    public static final int MODIFIED                      = 0x00004000; // Marks whether an entity has been modified.

    // A mask for processing flags that can be copied as well when e.g. inlining a method / merging a class.
    // TODO: needs to be extended, e.g. with OBFUSCATE_CODE.
    public static final int COPYABLE_PROCESSING_FLAGS = 0x00000800;
}
