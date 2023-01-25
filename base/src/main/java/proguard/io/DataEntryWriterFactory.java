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
import proguard.resources.file.ResourceFilePool;
import proguard.util.ConstantStringFunction;
import proguard.util.ExtensionMatcher;
import proguard.util.FileNameParser;
import proguard.util.ListFunctionParser;
import proguard.util.ListParser;
import proguard.util.SingleFunctionParser;
import proguard.util.StringFunction;
import proguard.util.StringMatcher;
import proguard.util.WildcardManager;

import java.io.File;
import java.security.KeyStore;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static proguard.classfile.ClassConstants.CLASS_FILE_EXTENSION;

/**
 * This class can create DataEntryWriter instances based on class paths. The
 * writers will wrap the output in the proper apks, jars, aars, wars, ears,
 * and zips.
 *\
 * @author Eric Lafortune
 */
public class DataEntryWriterFactory
{

    private static final boolean ENABLE_ZIP64_SUPPORT = System.getProperty("enable.zip64.support") != null;

    private static final String CLASS_FILE_PATTERN = "**.class";
    private static final String CLASS_FILE_PREFIX  = "classes/";

    private static final byte[] JMOD_HEADER = new byte[] { 'J', 'M', 1, 0 };

    private static final int PAGE_ALIGNMENT = 4096;

    private /*static*/ final String[][] JMOD_PREFIXES = new String[][]
    {
        { CLASS_FILE_PATTERN, CLASS_FILE_PREFIX }
    };

    private /*static*/ final String[][] WAR_PREFIXES = new String[][]
    {
        { CLASS_FILE_PATTERN, CLASS_FILE_PREFIX }
    };


    private final ClassPool                  programClassPool;
    private final ResourceFilePool           resourceFilePool;
    private final int                        modificationTime;
    private final StringMatcher              uncompressedFilter;
    private final int                        uncompressedAlignment;
    private final boolean                    pageAlignNativeLibs;
    private final boolean                    mergeAarJars;
    private final KeyStore.PrivateKeyEntry[] privateKeyEntries;

    private final Map<File,DataEntryWriter>                  jarWriterCache = new HashMap<>();

    private final Function<DataEntryWriter, DataEntryWriter> alternativeClassDataEntryWriterProvider;


    /**
     * Creates a new DataEntryWriterFactory.
     *
     * @param programClassPool      the program class pool to process.
     * @param resourceFilePool      the resource file pool to process.
     * @param modificationTime      the modification date and time of
     *                              the zip entries, in DOS
     *                              format.
     * @param uncompressedFilter    an optional filter for files that
     *                              should not be compressed.
     * @param uncompressedAlignment the desired alignment for the data
     *                              of uncompressed entries.
     * @param pageAlignNativeLibs   specifies whether to align native
     *                              libraries at page boundaries.
     * @param mergeAarJars          specifies whether to merge all jars
     *                              in an Android library aar into a
     *                              single jar.
     * @param privateKeyEntries     optional private keys to sign jars.
     */
    public DataEntryWriterFactory(ClassPool                  programClassPool,
                                  ResourceFilePool           resourceFilePool,
                                  int                        modificationTime,
                                  StringMatcher              uncompressedFilter,
                                  int                        uncompressedAlignment,
                                  boolean                    pageAlignNativeLibs,
                                  boolean                    mergeAarJars,
                                  KeyStore.PrivateKeyEntry[] privateKeyEntries)
    {
        this(
                programClassPool,
                resourceFilePool,
                modificationTime,
                uncompressedFilter,
                uncompressedAlignment,
                pageAlignNativeLibs,
                mergeAarJars,
                privateKeyEntries,
                null
        );
    }

    /**
     * Creates a new DataEntryWriterFactory.
     *
     * @param programClassPool          the program class pool to process.
     * @param resourceFilePool          the resource file pool to process.
     * @param modificationTime          the modification date and time of
     *                                  the zip entries, in DOS
     *                                  format.
     * @param uncompressedFilter        an optional filter for files that
     *                                  should not be compressed.
     * @param uncompressedAlignment     the desired alignment for the data
     *                                  of uncompressed entries.
     * @param pageAlignNativeLibs       specifies whether to align native
     *                                  libraries at page boundaries.
     * @param mergeAarJars           specifies whether to merge all jars
     *                                  in an Android app bundle into a
     *                                  single jar.
     * @param privateKeyEntries         optional private keys to sign jars.
     * @param alternativeClassDataEntryWriterProvider optional, to provide an alternative class writer,
     *                                                instead of the default {@link ClassDataEntryWriter}.
     */
    public DataEntryWriterFactory(ClassPool                                  programClassPool,
                                  ResourceFilePool                           resourceFilePool,
                                  int                                        modificationTime,
                                  StringMatcher                              uncompressedFilter,
                                  int                                        uncompressedAlignment,
                                  boolean                                    pageAlignNativeLibs,
                                  boolean                                    mergeAarJars,
                                  KeyStore.PrivateKeyEntry[]                 privateKeyEntries,
                                  Function<DataEntryWriter, DataEntryWriter> alternativeClassDataEntryWriterProvider)
    {
        this.programClassPool                        = programClassPool;
        this.resourceFilePool                        = resourceFilePool;
        this.modificationTime                        = modificationTime;
        this.uncompressedFilter                      = uncompressedFilter;
        this.uncompressedAlignment                   = uncompressedAlignment;
        this.pageAlignNativeLibs                     = pageAlignNativeLibs;
        this.mergeAarJars                            = mergeAarJars;
        this.privateKeyEntries                       = privateKeyEntries;
        this.alternativeClassDataEntryWriterProvider = alternativeClassDataEntryWriterProvider;
    }

    /**
     * Creates a new DataEntryWriterFactory.
     *
     * @param programClassPool          the program class pool to process.
     * @param resourceFilePool          the resource file pool to process.
     * @param uncompressedFilter        an optional filter for files that
     *                                  should not be compressed.
     * @param uncompressedAlignment     the desired alignment for the data
     *                                  of uncompressed entries.
     * @param pageAlignNativeLibs       specifies whether to align native
     *                                  libraries at page boundaries.
     * @param mergeAarJars           specifies whether to merge all jars
     *                                  in an Android app bundle into a
     *                                  single jar.
     * @param privateKeyEntries         optional private keys to sign jars.
     * @param alternativeClassDataEntryWriterProvider optional, to provide an alternative class writer,
     *                                                instead of the default {@link ClassDataEntryWriter}.
     */
    public DataEntryWriterFactory(ClassPool                                  programClassPool,
                                  ResourceFilePool                           resourceFilePool,
                                  StringMatcher                              uncompressedFilter,
                                  int                                        uncompressedAlignment,
                                  boolean                                    pageAlignNativeLibs,
                                  boolean                                    mergeAarJars,
                                  KeyStore.PrivateKeyEntry[]                 privateKeyEntries,
                                  Function<DataEntryWriter, DataEntryWriter> alternativeClassDataEntryWriterProvider)
    {
        this.programClassPool                        = programClassPool;
        this.resourceFilePool                        = resourceFilePool;
        // Convert the current time into DOS date and time.
        Date currentDate = new Date();
        this.modificationTime             =
                (currentDate.getYear()  - 80) << 25 |
                (currentDate.getMonth() + 1 ) << 21 |
                 currentDate.getDate()        << 16 |
                 currentDate.getHours()       << 11 |
                 currentDate.getMinutes()     << 5  |
                 currentDate.getSeconds()     >> 1;
        this.uncompressedFilter                      = uncompressedFilter;
        this.uncompressedAlignment                   = uncompressedAlignment;
        this.pageAlignNativeLibs                     = pageAlignNativeLibs;
        this.mergeAarJars                            = mergeAarJars;
        this.privateKeyEntries                       = privateKeyEntries;
        this.alternativeClassDataEntryWriterProvider = alternativeClassDataEntryWriterProvider;
    }




    /**
     * Creates a DataEntryWriter that can write to the given class path entries.
     *
     * @param classPath            the output class path.
     * @param fromIndex            the start index in the class path.
     * @param toIndex              the end index in the class path.
     * @param extraDataEntryWriter a writer to which extra injected files can be written.
     * @return a DataEntryWriter for writing to the given class path entries.
     */
    public DataEntryWriter createDataEntryWriter(ClassPath       classPath,
                                                 int             fromIndex,
                                                 int             toIndex,
                                                 DataEntryWriter extraDataEntryWriter)
    {
        DataEntryWriter writer = null;

        // Create a chain of writers, one for each class path entry.
        for (int index = toIndex - 1; index >= fromIndex; index--)
        {
            ClassPathEntry entry = classPath.get(index);

            // We're allowing the same output file to be specified multiple
            // times in the class path. We only add a control manifest for
            // the input of the first occurrence.
            boolean addCheckingJarWriter =
                !outputFileOccurs(entry,
                                  classPath,
                                  0,
                                  index);

            // We're allowing the same output file to be specified multiple
            // times in the class path. We only close cached jar writers
            // for this entry if its file doesn't occur again later on.
            boolean closeCachedJarWriter =
                !outputFileOccurs(entry,
                                  classPath,
                                  index + 1,
                                  classPath.size());

            writer = createClassPathEntryWriter(entry,
                                                writer,
                                                extraDataEntryWriter,
                                                addCheckingJarWriter,
                                                closeCachedJarWriter);
        }

        return writer;
    }


    private boolean outputFileOccurs(ClassPathEntry entry,
                                     ClassPath      classPath,
                                     int            startIndex,
                                     int            endIndex)
    {
        File file = entry.getFile();

        for (int index = startIndex; index < endIndex; index++)
        {
            ClassPathEntry classPathEntry = classPath.get(index);
            if (classPathEntry.isOutput() &&
                classPathEntry.getFile().equals(file))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Creates a DataEntryWriter that can write to the given class path entry,
     * or delegate to another DataEntryWriter if its filters don't match.
     */
    private DataEntryWriter createClassPathEntryWriter(ClassPathEntry  classPathEntry,
                                                       DataEntryWriter alternativeWriter,
                                                       DataEntryWriter extraDataEntryWriter,
                                                       boolean         addCheckingJarWriter,
                                                       boolean         closeCachedJarWriter)
    {
        File file = classPathEntry.getFile();

        boolean isDex  = classPathEntry.isDex();
        boolean isApk  = classPathEntry.isApk();
        boolean isAab  = classPathEntry.isAab();
        boolean isJar  = classPathEntry.isJar();
        boolean isAar  = classPathEntry.isAar();
        boolean isWar  = classPathEntry.isWar();
        boolean isEar  = classPathEntry.isEar();
        boolean isJmod = classPathEntry.isJmod();
        boolean isZip  = classPathEntry.isZip();

        boolean isFile = isDex || isApk || isAab || isJar || isAar || isWar || isEar || isJmod || isZip;

        List<String> filter     = DataEntryReaderFactory.getFilterExcludingVersionedClasses(classPathEntry);
        List<String> apkFilter  = classPathEntry.getApkFilter();
        List<String> aabFilter  = classPathEntry.getAabFilter();
        List<String> jarFilter  = classPathEntry.getJarFilter();
        List<String> aarFilter  = classPathEntry.getAarFilter();
        List<String> warFilter  = classPathEntry.getWarFilter();
        List<String> earFilter  = classPathEntry.getEarFilter();
        List<String> jmodFilter = classPathEntry.getJmodFilter();
        List<String> zipFilter  = classPathEntry.getZipFilter();

        // Create the writer for the main file or directory.
        DataEntryWriter writer = isFile ?
            new FixedFileWriter(file) :
            new DirectoryWriter(file);

        if (isDex)
        {
            // A dex file can't contain resource files.
            writer =
                new FilteredDataEntryWriter(
                new DataEntryNameFilter(
                new ExtensionMatcher(".dex")),
                    writer);
        }
        else
        {
            // If the output is an archive, we'll flatten (unpack the contents of)
            // higher level input archives, e.g. when writing into a jar file, we
            // flatten zip files.
            boolean flattenApks  = false;
            boolean flattenAabs  = flattenApks  || isApk;
            boolean flattenJars  = flattenAabs  || isAab;
            boolean flattenAars  = flattenJars  || isJar;
            boolean flattenWars  = flattenAars  || isAar;
            boolean flattenEars  = flattenWars  || isWar;
            boolean flattenJmods = flattenEars  || isEar;
            boolean flattenZips  = flattenJmods || isJmod;

            // Set up the filtered jar writers.
            writer = wrapInJarWriter(file, writer, extraDataEntryWriter, closeCachedJarWriter, flattenZips,  isZip,  false, ".zip",  zipFilter,  null,        false, null);
            writer = wrapInJarWriter(file, writer, extraDataEntryWriter, closeCachedJarWriter, flattenJmods, isJmod, false, ".jmod", jmodFilter, JMOD_HEADER, false, JMOD_PREFIXES);
            writer = wrapInJarWriter(file, writer, extraDataEntryWriter, closeCachedJarWriter, flattenEars,  isEar,  false, ".ear",  earFilter,  null,        false, null);
            writer = wrapInJarWriter(file, writer, extraDataEntryWriter, closeCachedJarWriter, flattenWars,  isWar,  false, ".war",  warFilter,  null,        false, WAR_PREFIXES);
            writer = wrapInJarWriter(file, writer, extraDataEntryWriter, closeCachedJarWriter, flattenAars,  isAar,  false, ".aar",  aarFilter,  null,        false, null);

            if (isAar)
            {
                // If we're writing an AAR, all input jars need to
                // be merged into a final classes.jar file or need to be put in the lib folder.
                if (mergeAarJars)
                {

                    writer =
                        new FilteredDataEntryWriter(new DataEntryNameFilter(new ExtensionMatcher(".jar")),
                                                    new RenamedDataEntryWriter(
                                                        new ConstantStringFunction("classes.jar"), writer),
                                                    writer);
                }
                else
                {
                    writer =
                        new FilteredDataEntryWriter(new DataEntryNameFilter(new ExtensionMatcher(".jar")),
                                                    new RenamedDataEntryWriter(string -> {
                                                        String fileName = string.substring(string.lastIndexOf('/') + 1);
                                                        if (fileName.equals("classes.jar"))
                                                        {
                                                            return fileName;
                                                        }
                                                        else
                                                        {
                                                            return "libs/" + fileName;
                                                        }
                                                    }, writer),
                                                    writer);
                }
            }

            writer = wrapInJarWriter(file, writer, extraDataEntryWriter, closeCachedJarWriter, flattenJars,  isJar,  false, ".jar",  jarFilter,  null,        false, null);

            // Either we create an aab or apk; they can not be nested.
            writer = isAab ?
                wrapInJarWriter(file, writer, extraDataEntryWriter, closeCachedJarWriter, flattenAabs, isAab, true,  ".aab", aabFilter, null, false,               null) :
                wrapInJarWriter(file, writer, extraDataEntryWriter, closeCachedJarWriter, flattenApks, isApk, false, ".apk", apkFilter, null, pageAlignNativeLibs, null);

            // Create a writer for plain class files. Don't close the enclosed
            // writer through it, but let it be closed later on.
            DataEntryWriter classWriter =
                new ClassDataEntryWriter(programClassPool,
                new NonClosingDataEntryWriter(writer));

            // Add a renaming filter, if specified.
            if (filter != null)
            {
                WildcardManager wildcardManager = new WildcardManager();

                StringFunction fileNameFunction =
                    new ListFunctionParser(
                    new SingleFunctionParser(
                    new FileNameParser(wildcardManager), wildcardManager)).parse(filter);

                // Slight asymmetry: we filter plain class files beforehand,
                // but we filter and rename dex files and resource files after
                // creating and renaming them in the feature structure.
                // We therefore don't filter class files that go into dex
                // files.
                classWriter = new RenamedDataEntryWriter(fileNameFunction, classWriter);
                writer      = new RenamedDataEntryWriter(fileNameFunction, writer);
            }

            writer =
                // Filter on class files.
                new NameFilteredDataEntryWriter(
                new ExtensionMatcher(CLASS_FILE_EXTENSION),
                        alternativeClassDataEntryWriterProvider != null ?
                            alternativeClassDataEntryWriterProvider.apply(writer) :
                            classWriter,
                    writer);
        }

        // Let the writer cascade, if specified.
        return alternativeWriter != null ?
            new CascadingDataEntryWriter(writer, alternativeWriter) :
            writer;
    }


    /**
     * Wraps the given DataEntryWriter in a ZipWriter, filtering and signing
     * if necessary.
     */
    private DataEntryWriter wrapInJarWriter(File            file,
                                            DataEntryWriter writer,
                                            DataEntryWriter extraDataEntryWriter,
                                            boolean         closeCachedJarWriter,
                                            boolean         flatten,
                                            boolean         isJar,
                                            boolean         isAab,
                                            String          jarFilterExtension,
                                            List<String>    jarFilter,
                                            byte[]          jarHeader,
                                            boolean         pageAlignNativeLibs,
                                            String[][]      prefixes)
    {
        StringMatcher pageAlignmentFilter = pageAlignNativeLibs ?
            new FileNameParser().parse("lib/*/*.so") :
            null;

        // Flatten jars or zip them up.
        DataEntryWriter zipWriter;
        if (flatten)
        {
            // Unpack the jar.
            zipWriter = new ParentDataEntryWriter(writer);
        }

        // Do we have a cached writer?
        else if (!isJar || (zipWriter = jarWriterCache.get(file)) == null)
        {
            // Sign the jar.
            zipWriter =
                wrapInSignedJarWriter(writer,
                                      extraDataEntryWriter,
                                      isAab,
                                      jarHeader,
                                      pageAlignmentFilter);

            // Add a prefix to specified files inside the jar.
            if (prefixes != null)
            {
                DataEntryWriter prefixlessJarWriter = zipWriter;

                for (int index = prefixes.length - 1; index >= 0; index--)
                {
                    String prefixFileNameFilter = prefixes[index][0];
                    String prefix               = prefixes[index][1];

                    zipWriter =
                        new FilteredDataEntryWriter(
                            new DataEntryNameFilter(
                                new ListParser(new FileNameParser()).parse(prefixFileNameFilter)),
                            new PrefixAddingDataEntryWriter(prefix,
                                                            prefixlessJarWriter),
                            zipWriter);
                }
            }

            // Is it an outermost archive?
            if (isJar)
            {
                // Cache the jar writer so it can be reused.
                jarWriterCache.put(file, zipWriter);
            }
        }

        // Only close an outermost archive if specified.
        // It may be used later on.
        if (isJar && !closeCachedJarWriter)
        {
            zipWriter = new NonClosingDataEntryWriter(zipWriter);
        }

        // Either zip up the jar or delegate to the original writer.
        return
            // Is the data entry part of the specified type of jar?
            new FilteredDataEntryWriter(
            new DataEntryParentFilter(
            new DataEntryNameFilter(
            new ExtensionMatcher(jarFilterExtension))),

                // The parent of the data entry is a jar.
                // Write the data entry to the jar.
                // Apply the jar filter, if specified, to the parent.
                jarFilter != null ?
                    new FilteredDataEntryWriter(
                    new DataEntryParentFilter(
                    new DataEntryNameFilter(
                    new ListParser(new FileNameParser()).parse(jarFilter))),
                    zipWriter) :
                    zipWriter,

                // The parent of the data entry is not a jar.
                // Write the entry to a jar anyway if the output is a jar.
                // Otherwise just delegate to the original writer.
                isJar ?
                    zipWriter :
                    writer);
    }


    /**
     * Wraps the given DataEntryWriter in a ZipWriter, signing if necessary.
     */
    private DataEntryWriter wrapInSignedJarWriter(DataEntryWriter writer,
                                                  DataEntryWriter extraDataEntryWriter,
                                                  boolean         isAab,
                                                  byte[]          jarHeader,
                                                  StringMatcher   pageAlignmentFilter)
    {
        // Pack the zip.
        DataEntryWriter zipWriter =
            new ZipWriter(uncompressedFilter,
                          uncompressedAlignment,
                          ENABLE_ZIP64_SUPPORT,
                          pageAlignmentFilter,
                          PAGE_ALIGNMENT,
                          modificationTime,
                          jarHeader,
                          writer);

        // Do we need to sign the jar?
        if (privateKeyEntries != null)
        {
            // Sign the jar (signature scheme v1).
            zipWriter =
                new SignedJarWriter(privateKeyEntries[0],
                                    new String[] { JarWriter.DEFAULT_DIGEST_ALGORITHM },
                                    "ProGuard",
                                    null,
                                    zipWriter);
        }

        return zipWriter;
    }
}
