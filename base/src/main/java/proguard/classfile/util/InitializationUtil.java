package proguard.classfile.util;

import proguard.classfile.*;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.io.*;
import proguard.preverify.CodePreverifier;

import java.io.*;

/**
 * This utility class provides a method to initialize the cached
 * cross-references classes. They are necessary to traverse the class
 * hierarchy efficiently, for example when preverifying code or
 * performing more general partial evaluation.
 */
public class InitializationUtil
{
    /**
     * Initializes the cached cross-references of the classes in the given
     * class pools.
     * <p>
     * Note: no warnings are given when classes are missing: if you require
     *       warnings about missing classes use
     *       {@link InitializationUtil#initialize(ClassPool, ClassPool, WarningPrinter)} instead.
     * @param programClassPool the program class pool, typically with processed
     *                         classes.
     * @param libraryClassPool the library class pool, typically with run-time
     *                         classes.
     */
    public static void initialize(ClassPool programClassPool,
                                  ClassPool libraryClassPool)
    {
        WarningPrinter nullWarningPrinter = new WarningPrinter(new PrintWriter(new OutputStream()
        {
            @Override
            public void write(int i) { }
        }));
        initialize(programClassPool, libraryClassPool, nullWarningPrinter);
    }

    /**
     * Initializes the cached cross-references of the classes in the given
     * class pools.
     * @param programClassPool the program class pool, typically with processed
     *                         classes.
     * @param libraryClassPool the library class pool, typically with run-time
     *                         classes.
     * @param warningPrinter   the {@link WarningPrinter} to use for printing warnings
     *                         about missing classes.
     */
    public static void initialize(ClassPool      programClassPool,
                                  ClassPool      libraryClassPool,
                                  WarningPrinter warningPrinter)
    {
        // Initialize the class hierarchies.
        libraryClassPool.classesAccept(
            new ClassSuperHierarchyInitializer(programClassPool,
                                               libraryClassPool,
                                               null,
                                               null));

        programClassPool.classesAccept(
            new ClassSuperHierarchyInitializer(programClassPool,
                                               libraryClassPool,
                                               warningPrinter,
                                               warningPrinter));

        // Initialize the other references from the program classes.
        programClassPool.classesAccept(
            new ClassReferenceInitializer(programClassPool,
                                          libraryClassPool,
                                          warningPrinter,
                                          warningPrinter,
                                          warningPrinter,
                                          null));
    }
}
