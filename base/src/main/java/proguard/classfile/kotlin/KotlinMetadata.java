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
package proguard.classfile.kotlin;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.util.*;

/**
 * This abstract class represents metadata that is attached to a Kotlin
 * class, parsed from its @Metadata tag.
 *
 * Documentation on the different fields is copied from:
 *     https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/jvm/runtime/kotlin/Metadata.kt
 *
 * @author Tim Van Den Broecke
 */
public abstract class KotlinMetadata
extends               SimpleProcessable
{
    /**
     * A kind of the metadata this annotation encodes. Kotlin compiler recognizes the following kinds (see KotlinClassHeader.Kind):
     *
     * 1 Class
     * 2 File
     * 3 Synthetic class
     * 4 Multi-file class facade
     * 5 Multi-file class part
     *
     * The class file with a kind not listed here is treated as a non-Kotlin file.
     */
    public int      k;

    /**
     * The version of the metadata provided in the arguments of this annotation.
     */
    public int[]    mv;

//    /**
//     * Metadata in a custom format. The format may be different (or even absent) for different kinds.
//     */
//    public String[] d1;

//    /**
//     * An addition to [d1]: array of strings which occur in metadata, written in plain text so that strings already present
//     * in the constant pool are reused. These strings may be then indexed in the metadata by an integer index in this array.
//     */
//    public String[] d2;

    /**
     * An extra int. Bits of this number represent the following flags:
     *
     * * 0 - this is a multi-file class facade or part, compiled with `-Xmultifile-parts-inherit`.
     * * 1 - this class file is compiled by a pre-release version of Kotlin and is not visible to release versions.
     * * 2 - this class file is a compiled Kotlin script source file (.kts).
     * * 3 - the metadata of this class file is not supposed to be read by the compiler, whose major.minor version is less than
     *   the major.minor version of this metadata ([mv]).
     */
    public int      xi;

    /**
     * An extra string. For a multi-file part class, internal name of the facade class.
     */
    public String   xs;

    /**
     * Fully qualified name of the package this class is located in, from Kotlin's point of view, or empty string if this name
     * does not differ from the JVM's package FQ name. These names can be different in case the [JvmPackageName] annotation is used.
     * Note that this information is also stored in the corresponding module's `.kotlin_module` file.
     */
    public String   pn;

    
    protected KotlinMetadata(int k, int[] mv, int xi, String xs, String pn)
    {
        this.k  = k;
        this.mv = mv;
        this.xi = xi;
        this.xs = xs;
        this.pn = pn;
    }


    public abstract void accept(Clazz clazz, KotlinMetadataVisitor kotlinMetadataVisitor);

}
