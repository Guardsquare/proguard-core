/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

package proguard.io;

import proguard.classfile.ClassPool;
import proguard.classfile.visitor.ClassVisitor;
import proguard.util.StringMatcher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * An dex writer that uses the d8 tool from the Android SDK.
 *
 * @author Thomas Neidhart
 */
public class D8BasedDexDataEntryWriter
extends      DexDataEntryWriter
{
    private final D8ClassConverter.D8DexFile dexFile;


    /**
     * Creates a new D8BasedDexDataEntryWriter.
     *
     * @param classPool                the class pool from which classes are
     *                                 collected.
     * @param classNameFilter          an optional filter for classes to be
     *                                 written.
     * @param dexFileName              the dex file name.
     * @param forceDex                 specifies whether the dex files should
     *                                 always be written, even if they don't
     *                                 contain any code.
     * @param minSdkVersion            the minimum supported API level.
     * @param debuggable               whether the dex file shall be debuggable
     *                                 or not.
     * @param extraDexDataEntryVisitor an optional extra visitor for all dex
     *                                 data entries that written. The visitor
     *                                 can use the data entry names, but
     *                                 must not read their contents.
     * @param dexDataEntryWriter       the writer to which the converted dex
     *                                 file is written.
     * @param otherDataEntryWriter     the writer to which other data entries
     *                                 are written.
     */
    public D8BasedDexDataEntryWriter(ClassPool       classPool,
                                     StringMatcher   classNameFilter,
                                     ClassPath       libraryJars,
                                     String          dexFileName,
                                     boolean         forceDex,
                                     int             minSdkVersion,
                                     boolean         debuggable,
                                     DataEntryReader extraDexDataEntryVisitor,
                                     DataEntryWriter dexDataEntryWriter,
                                     DataEntryWriter otherDataEntryWriter)
    {
        super(classPool,
              classNameFilter,
              dexFileName,
              forceDex,
              extraDexDataEntryVisitor,
              dexDataEntryWriter,
              otherDataEntryWriter);

        dexFile = new D8ClassConverter.D8DexFile(libraryJars, minSdkVersion, debuggable);
    }


    // Implementations for DexDataEntryWriter.

    @Override
    protected ClassVisitor createClassConverter()
    {
        return new D8ClassConverter(dexFile);
    }


    @Override
    protected void writeDex(OutputStream outputStream)
    throws IOException
    {
        try
        {
            dexFile.writeTo(outputStream);
        }
        finally
        {
            outputStream.close();
        }
    }


    @Override
    public void println(PrintWriter pw, String prefix)
    {
        pw.println(prefix + "D8BasedDexDataEntryWriter");
        dexDataEntryWriter.println(pw, prefix + "  ");
        otherDataEntryWriter.println(pw, prefix + "  ");
    }
}
