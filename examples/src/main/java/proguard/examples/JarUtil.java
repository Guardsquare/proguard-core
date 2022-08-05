package proguard.examples;

import proguard.classfile.ClassPool;
import proguard.classfile.visitor.ClassNameFilter;
import proguard.classfile.visitor.ClassPoolFiller;
import proguard.io.*;
import proguard.util.ExtensionMatcher;
import proguard.util.OrMatcher;

import java.io.File;
import java.io.IOException;

/**
 * This utility class provides methods to read and write the classes in jars.
 */
public class JarUtil
{
    /**
     * Reads the classes from the specified jar file and returns them as a class
     * pool.
     *
     * @param jarFileName the name of the jar file or jmod file.
     * @param isLibrary   specifies whether classes should be represented as
     *                    ProgramClass instances (for processing) or
     *                    LibraryClass instances (more compact).
     * @return a new class pool with the read classes.
     */
    public static ClassPool readJar(String  jarFileName,
                                    boolean isLibrary)
    throws IOException
    {
        return readJar(jarFileName, "**", isLibrary);
    }


    /**
     * Reads the classes from the specified jar file and returns them as a class
     * pool.
     *
     * @param jarFileName the name of the jar file or jmod file.
     * @param isLibrary   specifies whether classes should be represented as
     *                    ProgramClass instances (for processing) or
     *                    LibraryClass instances (more compact).
     * @return a new class pool with the read classes.
     */
    public static ClassPool readJar(String  jarFileName,
                                    String  classNameFilter,
                                    boolean isLibrary)
    throws IOException
    {
        ClassPool classPool = new ClassPool();

        // Parse all classes from the input jar and
        // collect them in the class pool.
        DataEntrySource source =
            new FileSource(
            new File(jarFileName));

        ClassPoolFiller classPoolFiller = new ClassPoolFiller(classPool);
        DataEntryReader classReader =
            new NameFilteredDataEntryReader("**.class",
            new ClassReader(isLibrary, false, false, false, null,
            new ClassNameFilter(classNameFilter,
                                classPoolFiller)));

        // Convert dex files to a JAR first.
        classReader = new NameFilteredDataEntryReader("classes*.dex",
                      new DexClassReader(!isLibrary, classPoolFiller),
                      classReader);

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
        JarWriter jarWriter =
            new JarWriter(
            new ZipWriter(
            new FixedFileWriter(
            new File(outputJarFileName))));

        programClassPool.classesAccept(
            new DataEntryClassWriter(jarWriter));

        jarWriter.close();
    }
}
