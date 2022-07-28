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
package proguard.io;

import proguard.classfile.*;
import proguard.classfile.io.*;
import proguard.classfile.util.*;
import proguard.classfile.util.kotlin.KotlinMetadataInitializer;
import proguard.classfile.visitor.*;

import java.io.*;

/**
 * This {@link DataEntryReader} applies a given {@link ClassVisitor} to the class
 * definitions that it reads.
 * <p/>
 * Class files are read as {@link ProgramClass} instances or {@link LibraryClass} instances,
 * depending on the <code>isLibrary</code> flag.
 * <p/>
 * In case of libraries, only public classes are considered, if the
 * <code>skipNonPublicLibraryClasses</code> flag is set.
 *
 * @author Eric Lafortune
 */
public class ClassReader implements DataEntryReader
{
    private static final String MODULE_INFO_CLASS  = "module-info.class";


    private final boolean        isLibrary;
    private final boolean        skipNonPublicLibraryClasses;
    private final boolean        skipNonPublicLibraryClassMembers;
    private final boolean        ignoreStackMapAttributes;
    private final WarningPrinter warningPrinter;
    private final ClassVisitor   classVisitor;

    // Optionally build the Kotlin metadata model while reading classes.
    private final KotlinMetadataInitializer kmInitializer;


    /**
     * Creates a new ClassReader for reading the specified
     * Clazz objects.
     */
    public ClassReader(boolean        isLibrary,
                       boolean        skipNonPublicLibraryClasses,
                       boolean        skipNonPublicLibraryClassMembers,
                       boolean        ignoreStackMapAttributes,
                       WarningPrinter warningPrinter,
                       ClassVisitor   classVisitor)
    {
        this(isLibrary,
             skipNonPublicLibraryClasses,
             skipNonPublicLibraryClassMembers,
             ignoreStackMapAttributes,
             false,
             warningPrinter,
             classVisitor);
    }


    /**
     * Creates a new ClassReader for reading the specified
     * Clazz objects.
     */
    public ClassReader(boolean        isLibrary,
                       boolean        skipNonPublicLibraryClasses,
                       boolean        skipNonPublicLibraryClassMembers,
                       boolean        ignoreStackMapAttributes,
                       boolean        includeKotlinMetadata,
                       WarningPrinter warningPrinter,
                       ClassVisitor   classVisitor)
    {
        this.isLibrary                        = isLibrary;
        this.skipNonPublicLibraryClasses      = skipNonPublicLibraryClasses;
        this.skipNonPublicLibraryClassMembers = skipNonPublicLibraryClassMembers;
        this.ignoreStackMapAttributes         = ignoreStackMapAttributes;
        this.warningPrinter                   = warningPrinter;
        this.classVisitor                     = classVisitor;
        this.kmInitializer = includeKotlinMetadata ?
                new KotlinMetadataInitializer(warningPrinter) : null;
    }


    // Implementations for DataEntryReader.

    @Override
    public void read(DataEntry dataEntry) throws IOException
    {
        try
        {
            // Get the input stream.
            InputStream inputStream = dataEntry.getInputStream();

            // Wrap it into a data input stream.
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            // Create a Clazz representation.
            Clazz clazz;
            if (isLibrary)
            {
                clazz = new LibraryClass();
                ClassVisitor libraryClassReader = new LibraryClassReader(
                    dataInputStream,
                    skipNonPublicLibraryClasses,
                    skipNonPublicLibraryClassMembers,
                    kmInitializer != null ? (k, mv, d1, d2, xi, xs, pn) -> kmInitializer.initialize(clazz, k, mv, d1, d2, xi, xs, pn) : null
                );

                clazz.accept(libraryClassReader);
            }
            else
            {
                clazz = new ProgramClass();
                ClassVisitor programClassReader = new ProgramClassReader(
                    dataInputStream,
                    ignoreStackMapAttributes
                );

                if (kmInitializer != null)
                {
                    programClassReader = new MultiClassVisitor(programClassReader, kmInitializer);
                }

                clazz.accept(programClassReader);
            }

            // Apply the visitor, if we have a real class.
            String className = clazz.getName();
            if (className != null)
            {
                String dataEntryName = dataEntry.getName();
                if (!dataEntryName.equals(MODULE_INFO_CLASS) &&
                    !dataEntryName.replace(File.pathSeparatorChar, TypeConstants.PACKAGE_SEPARATOR).equals(className + ClassConstants.CLASS_FILE_EXTENSION) &&
                    warningPrinter != null)
                {
                    warningPrinter.print(className,
                                         "Warning: class [" + dataEntry.getName() + "] unexpectedly contains class [" + ClassUtil.externalClassName(className) + "]");
                }

                clazz.accept(classVisitor);
            }

            dataEntry.closeInputStream();
        }
        catch (Exception ex)
        {
            throw (IOException)new IOException("Can't process class ["+dataEntry.getName()+"] ("+ex.getMessage()+")").initCause(ex);
        }
    }


    /**
     * This main method illustrates the use of this class.
     *
     * It writes out the structure of the specified classes (packaged
     * in jar, zip, or class files).
     */
    public static void main(String[] args)
    {
        for (String fileName : args)
        {
            try
            {
                DataEntryReader dataEntryReader =
                    new ClassFilter(
                    new ClassReader(false, false, false, false, null,
                    new ClassPrinter()));

                if (fileName.endsWith(".jar") ||
                    fileName.endsWith(".zip"))
                {
                    dataEntryReader = new JarReader(dataEntryReader);
                }

                new FileSource(new File(fileName)).pumpDataEntries(dataEntryReader);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
