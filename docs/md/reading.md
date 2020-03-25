## Streaming classes from a jar file

You can read classes from class files and various types of (nested) jar files
or jmod files, with some convenient utility classes and visitors. For example,
you can read all classes from all jar files in a given directory and print
them out in a streaming fashion, while they are read, without collecting their
representations:

    :::java
    DataEntrySource source =
        new DirectorySource(
        new File(inputDirectoryName));

    source.pumpDataEntries(
        new FilteredDataEntryReader(new DataEntryNameFilter(new ExtensionMatcher(".jar"))
        new JarReader(
        new ClassFilter(
        new ClassReader(false, false, false, false, null,
        new ClassPrinter()))));

Note the constructor-based dependency injection, to create a chain of visitor
classes. We typically use a slightly unconventional indentation to make this
construct easy to read.

Complete example: PrintClasses.java

## Writing out streamed classes

You can read classes, optionally perform some small modifications, and write
them out right away, again in a streaming fashion.

    :::java
    JarWriter jarWriter =
        new JarWriter(
        new ZipWriter(
        new FixedFileWriter(
        new File(outputJarFileName))));

    DataEntrySource source =
        new FileSource(
        new File(inputJarFileName));

    source.pumpDataEntries(
        new JarReader(
        new ClassFilter(
        new ClassReader(false, false, false, false, null,
        new DataEntryClassWriter(jarWriter)))));

    jarWriter.close();

Complete example: ApplyPeepholeOptimizations.java

## Collecting classes

Alternatively, you may want to collect the classes in a so-called class pool
first, so you can perform more extensive analyses on them:

    :::java
    ClassPool classPool = new ClassPool();

    DataEntrySource source =
        new FileSource(
        new File(jarFileName));

    source.pumpDataEntries(
        new JarReader(false,
        new ClassFilter(
        new ClassReader(false, false, false, false, null,
        new ClassPoolFiller(classPool)))));

Complete example: Preverify.java

## Writing out a set of classes

If you've collected a set of classes in a class pool, you can write them out
with the same visitors as before.

    :::java
    JarWriter jarWriter =
        new JarWriter(
        new ZipWriter(
        new FixedFileWriter(
        new File(outputJarFileName))));

    classPool.classesAccept(
        new DataEntryClassWriter(jarWriter));

    jarWriter.close();

Complete example: Preverify.java
