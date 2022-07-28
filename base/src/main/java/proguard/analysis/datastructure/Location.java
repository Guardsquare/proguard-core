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

package proguard.analysis.datastructure;

/**
 * Helper data structure that makes it possible to identify
 * specific places inside a program, either inside the bytecode
 * ({@link CodeLocation}) or some other file, e.g. JAR/APK
 * resources ({@link FileLocation}).
 *
 * @author Dennis Titze
 */
public abstract class Location
implements            Comparable<Location>
{

    public final int line;

    protected Location(int line)
    {
        this.line = line;
    }

    /**
     * Returns the name of the location, e.g., the signature of a CodeLocation, or the filename of a FileLocation
     */
    public abstract String getName();
}
