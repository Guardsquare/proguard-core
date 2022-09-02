/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
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

package proguard.io.util;

import proguard.classfile.ClassPool;
import proguard.classfile.visitor.ClassNameFilter;
import proguard.classfile.visitor.ClassPoolFiller;
import proguard.classfile.visitor.ClassVisitor;
import proguard.io.ClassPath;
import proguard.io.ClassPathEntry;
import proguard.io.ClassReader;
import proguard.io.DataEntry;
import proguard.io.DataEntryClassWriter;
import proguard.io.DataEntryReader;
import proguard.io.DataEntryReaderFactory;
import proguard.io.DataEntrySource;
import proguard.io.DataEntryWriter;
import proguard.io.DirectorySource;
import proguard.io.FixedFileWriter;
import proguard.io.JarWriter;
import proguard.io.NameFilteredDataEntryReader;
import proguard.io.ZipWriter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.function.BiFunction;

/**
 * This utility class provides methods to read the classes in jar, zips, aars
 * into a {@link ClassPool} and write a {@link ClassPool} to a jar.
 *
 * @author James Hamilton
 */
public class IOUtil
{
    private IOUtil() { }

    /**
     * Reads the classes from the specified jar file and returns them as a class
     * pool.
     *
     * @param fileName    the file name.
     * @param isLibrary   specifies whether classes should be represented as
     *                    ProgramClass instances (for processing) or
     *                    LibraryClass instances (more compact).
     * @return a new class pool with the read classes.
     */
    public static ClassPool read(String   fileName,
                                 boolean isLibrary)
    throws IOException
    {
        return read(new File(fileName), isLibrary);
    }

    /**
     * Reads the classes from the specified jar file and returns them as a class
     * pool.
     *
     * @param file        the file.
     * @param isLibrary   specifies whether classes should be represented as
     *                    ProgramClass instances (for processing) or
     *                    LibraryClass instances (more compact).
     * @return a new class pool with the read classes.
     */
    public static ClassPool read(File    file,
                                 boolean isLibrary)
    throws IOException
    {
        return read(file, isLibrary, (dataEntryReader, classPool) -> dataEntryReader);
    }


    /**
     * Reads the classes from the specified jar file and returns them as a class
     * pool.
     *
     * @param file        the name of the file.
     * @param isLibrary   specifies whether classes should be represented as
     *                    ProgramClass instances (for processing) or
     *                    LibraryClass instances (more compact).
     * @param extraDataEntryReader Optionally provide a function that wraps the reader in another reader.
     * @return a new class pool with the read classes.
     */
    public static ClassPool read(File    file,
                                 boolean isLibrary,
                                 BiFunction<DataEntryReader, ClassVisitor, DataEntryReader> extraDataEntryReader)
    throws IOException
    {
        return read(new ClassPath(new ClassPathEntry(file, false)), "**",false, isLibrary, false, false, false, extraDataEntryReader);
    }

    public static ClassPool read(ClassPath      classPath,
                                 String         classNameFilter,
                                 boolean        android,
                                 boolean        isLibrary,
                                 boolean        skipNonPublicLibraryClasses,
                                 boolean        skipNonPublicLibraryClassMembers,
                                 boolean        ignoreStackMapAttributes,
                                 BiFunction<DataEntryReader, ClassVisitor, DataEntryReader> extraDataEntryReader)
    throws IOException
    {
        ClassPool classPool = new ClassPool();
        ClassVisitor classPoolFiller = new ClassPoolFiller(classPool);

        if (classNameFilter != null)
            classPoolFiller = new ClassNameFilter(classNameFilter, classPoolFiller);

        DataEntryReader classReader =
                new NameFilteredDataEntryReader("**.class",
                new ClassReader(
                        isLibrary,
                        skipNonPublicLibraryClasses,
                        skipNonPublicLibraryClassMembers,
                        ignoreStackMapAttributes,
                        null,
                        classPoolFiller));

        classReader = extraDataEntryReader.apply(classReader, classPoolFiller);

        for (int index = 0; index < classPath.size(); index++)
        {
            ClassPathEntry entry = classPath.get(index);
            if (!entry.isOutput())
            {
                try
                {
                    // Create a reader that can unwrap jars, wars, ears, jmods and zips.
                    DataEntryReader reader =
                            new DataEntryReaderFactory(android)
                                    .createDataEntryReader(entry, classReader);

                    // Create the data entry source.
                    DataEntrySource source = new DirectorySource(entry.getFile());

                    // Pump the data entries into the reader.
                    source.pumpDataEntries(reader);
                }
                catch (IOException ex)
                {
                    throw new IOException("Can't read [" + entry + "] (" + ex.getMessage() + ")", ex);
                }
            }
        }

        return classPool;
    }


    /**
     * Writes the classes from the given class pool to a specified jar.
     * @param programClassPool  the classes to write.
     * @param outputJarFileName the name of the output jar file.
     */
    public static void writeJar(ClassPool programClassPool,
                                String    outputJarFileName)
    throws IOException
    {
       writeJar(programClassPool, outputJarFileName, null);
    }

    /**
     * Writes the classes from the given class pool to a specified jar.
     * @param programClassPool  the classes to write.
     * @param outputJarFileName the name of the output jar file.
     */
    public static void writeJar(ClassPool programClassPool,
                                String    outputJarFileName,
                                String    mainClassName)
    throws IOException
    {
        class MyJarWriter extends JarWriter implements Closeable {
            public MyJarWriter(DataEntryWriter zipEntryWriter)
            {
                super(zipEntryWriter);
            }

            @Override
            protected OutputStream createManifestOutputStream(DataEntry manifestEntry) throws IOException
            {
                OutputStream outputStream = super.createManifestOutputStream(manifestEntry);
                if (mainClassName != null) {
                    PrintWriter writer = new PrintWriter(outputStream);
                    writer.println("Main-Class: " + mainClassName);
                    writer.flush();
                }
                return outputStream;
            }

            @Override
            public void close() throws IOException
            {
                super.close();
            }
        }

        try (MyJarWriter jarWriter = new MyJarWriter(
                                     new ZipWriter(
                                     new FixedFileWriter(
                                     new File(outputJarFileName))))) {
            programClassPool.classesAccept(new DataEntryClassWriter(jarWriter));
        }
    }
}
