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
import proguard.io.*;
import proguard.util.ExtensionMatcher;
import proguard.util.OrMatcher;

import java.io.*;
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
     * @param fileName    the name of the file.
     * @param isLibrary   specifies whether classes should be represented as
     *                    ProgramClass instances (for processing) or
     *                    LibraryClass instances (more compact).
     * @return a new class pool with the read classes.
     */
    public static ClassPool read(String  fileName,
                                 boolean isLibrary)
    throws IOException
    {
        return read(fileName, "**", isLibrary, (dataEntryReader, classPool) -> dataEntryReader);
    }


    /**
     * Reads the classes from the specified jar file and returns them as a class
     * pool.
     *
     * @param fileName    the name of the file.
     * @param isLibrary   specifies whether classes should be represented as
     *                    ProgramClass instances (for processing) or
     *                    LibraryClass instances (more compact).
     * @param extraDataEntryReader Optionally provide a function that wraps the reader in another reader.
     * @return a new class pool with the read classes.
     */
    public static ClassPool read(String  fileName,
                                 String  classNameFilter,
                                 boolean isLibrary,
                                 BiFunction<DataEntryReader, ClassPoolFiller, DataEntryReader> extraDataEntryReader)
    throws IOException
    {
        ClassPool classPool = new ClassPool();
        ClassPoolFiller classPoolFiller = new ClassPoolFiller(classPool);

        // Parse all classes from the input jar and
        // collect them in the class pool.
        DataEntrySource source =
            new FileSource(
            new File(fileName));

        DataEntryReader classReader =
            new NameFilteredDataEntryReader("**.class",
            new ClassReader(isLibrary, false, false, false, null,
            new ClassNameFilter(classNameFilter,
            classPoolFiller)));

        classReader = extraDataEntryReader.apply(classReader, classPoolFiller);

        // Extract files from an archive if necessary.
        classReader =
                new FilteredDataEntryReader(
                new DataEntryNameFilter(new ExtensionMatcher("aar")),
                    new JarReader(
                    new NameFilteredDataEntryReader("classes.jar",
                    new JarReader(classReader))),
                new FilteredDataEntryReader(
                new DataEntryNameFilter(new OrMatcher(
                                        new ExtensionMatcher("jar"),
                                        new ExtensionMatcher("zip"),
                                        new ExtensionMatcher("apk"))),
                    new JarReader(classReader),
                classReader));

        source.pumpDataEntries(classReader);

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
