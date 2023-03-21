/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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
 */
public class ProcessingFlags
{
    static final int mask1  = 0b1;                                // 1
    static final int mask2  = 0b10;                               // 2
    static final int mask3  = 0b100;                              // 4
    static final int mask4  = 0b1000;                             // 8
    static final int mask5  = 0b10000;                            // 16
    static final int mask6  = 0b100000;                           // 32
    static final int mask7  = 0b1000000;                          // 64
    static final int mask8  = 0b10000000;                         // 128
    static final int mask9  = 0b100000000;                        // 256
    static final int mask10 = 0b1000000000;                       // 512
    static final int mask11 = 0b10000000000;                      // 1024
    static final int mask12 = 0b100000000000;                     // 2048
    static final int mask13 = 0b1000000000000;                    // 4096
    static final int mask14 = 0b10000000000000;                   // 8192
    static final int mask15 = 0b100000000000000;                  // 16384
    static final int mask16 = 0b1000000000000000;                 // 32768
    static final int mask17 = 0b10000000000000000;                // 65536
    static final int mask18 = 0b100000000000000000;               // 131072
    static final int mask19 = 0b1000000000000000000;              // 262144
    static final int mask20 = 0b10000000000000000000;             // 524288
    static final int mask21 = 0b100000000000000000000;            // 1048576
    static final int mask22 = 0b1000000000000000000000;           // 2097152
    static final int mask23 = 0b10000000000000000000000;          // 4194304
    static final int mask24 = 0b100000000000000000000000;         // 8388608
    static final int mask25 = 0b1000000000000000000000000;        // 16777216
    static final int mask26 = 0b10000000000000000000000000;       // 33554432
    static final int mask27 = 0b100000000000000000000000000;      // 67108864
    static final int mask28 = 0b1000000000000000000000000000;     // 134217728
    static final int mask29 = 0b10000000000000000000000000000;    // 268435456
    static final int mask30 = 0b100000000000000000000000000000;   // 536870912
    static final int mask31 = 0b1000000000000000000000000000000;  // 1073741824
    static final int mask32 = 0b10000000000000000000000000000000; // -2147483648


    // External configuration flags.
    public static final int DONT_SHRINK                          = mask21; // Marks whether an entity should not be shrunk.
    public static final int DONT_OPTIMIZE                        = mask22; // Marks whether an entity should not be optimized.
    public static final int DONT_OBFUSCATE                       = mask23; // Marks whether an entity should not be obfuscated.

    // Combined flags
    public static final int DONT_SHRINK_OR_OBFUSCATE             = DONT_SHRINK    | DONT_OBFUSCATE;
    public static final int DONT_SHRINK_OR_OPTIMIZE              = DONT_SHRINK    | DONT_OPTIMIZE;
    public static final int DONT_OPTIMIZE_OR_OBFUSCATE           = DONT_OBFUSCATE | DONT_OPTIMIZE;
    public static final int DONT_SHRINK_OR_OPTIMIZE_OR_OBFUSCATE = DONT_SHRINK    | DONT_OPTIMIZE | DONT_OBFUSCATE;


    // Internal processing flags.
    public static final int IS_CLASS_AVAILABLE                   = mask1;  // Marks whether a class member can be used for generalization or specialization.
    public static final int REMOVED_FIELDS                       = mask3;  // Marks whether a class has (at least one) fields removed.
    public static final int REMOVED_PUBLIC_FIELDS                = mask4;  // Marks whether a class has (at least one) public fields removed.
    public static final int REMOVED_CONSTRUCTORS                 = mask5;  // Marks whether a class has (at least one) constructors removed.
    public static final int REMOVED_PUBLIC_CONSTRUCTORS          = mask6;  // Marks whether a class has (at least one) public constructors removed.
    public static final int REMOVED_METHODS                      = mask7;  // Marks whether a class has (at least one) methods removed.
    public static final int REMOVED_PUBLIC_METHODS               = mask8;  // Marks whether a class has (at least one) public methods removed.
    public static final int INJECTED                             = mask10; // Marks whether an entity was injected.
    public static final int DONT_PROCESS_KOTLIN_MODULE           = mask16; // Marks whether to processing a Kotlin module file.
    public static final int MODIFIED                             = mask15; // Marks whether an entity has been modified.

    // A mask for processing flags that can be copied as well when e.g. inlining a method / merging a class.
    // TODO: needs to be extended, e.g. with OBFUSCATE_CODE.
    public static final int COPYABLE_PROCESSING_FLAGS            = mask12;
}
