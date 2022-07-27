package proguard.examples;

import proguard.classfile.ClassPool;
import proguard.classfile.visitor.ClassNameFilter;
import proguard.classfile.visitor.ClassPoolFiller;
import proguard.classfile.visitor.ClassPrinter;
import proguard.io.*;
import proguard.util.ExtensionMatcher;
import proguard.util.OrMatcher;

import java.io.File;
import java.io.IOException;

/**
 * This sample application illustrates how to read bytecode and print it out in
 * text format with the ProGuard API.
 *
 * Usage:
 *     java proguard.examples.PrintAndroidClasses input.dex
 */
public class PrintAndroidClasses
{

    public static void main(String[] args) throws IOException
    {

        String inputDirectoryName = args[0];

        ClassPool programClassPool = readJar(inputDirectoryName, "**", false);
        programClassPool.classesAccept(new ClassPrinter());
    }


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
        classReader =
                new NameFilteredDataEntryReader("classes*.dex",
                        new DexClassReader(!isLibrary,
                                classPoolFiller),
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

}
