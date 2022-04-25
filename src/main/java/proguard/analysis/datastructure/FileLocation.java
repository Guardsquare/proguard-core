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

import java.util.Objects;

/**
 * Represents a unique location in a file, e.g.
 * JAR/APK resources. They comprise the name
 * of the containing file and the line therein.
 *
 * @author Dennis Titze
 */
public class FileLocation
extends      Location
{

    public final String filename;
    public       String obfuscatedFilename;

    public FileLocation(String filename, int line)
    {
        super(line);
        this.filename = filename;
    }

    public FileLocation(String filename, int line, String obfuscatedFilename)
    {
        this(filename, line);
        this.obfuscatedFilename = obfuscatedFilename;
    }

    @Override
    public String getName()
    {
        return filename;
    }

    @Override
    public String toString()
    {
        return filename + " (line " + line + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof FileLocation))
        {
            return false;
        }
        FileLocation other = (FileLocation) o;
        return line == other.line && Objects.equals(filename, other.filename);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(filename, line);
    }

    @Override
    public int compareTo(Location o)
    {
        if (!o.getClass().equals(getClass()))
        {
            return -1;
        }
        FileLocation other = (FileLocation) o;
        if (filename.equals(other.filename))
        {
            return line - other.line;
        }
        return filename.compareTo(other.filename);
    }

    public String getOriginalFilename()
    {
        return obfuscatedFilename != null ? obfuscatedFilename : filename;
    }
}
