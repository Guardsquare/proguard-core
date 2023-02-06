package proguard.examples;

import proguard.classfile.ClassPool;
import proguard.classfile.VersionConstants;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.util.InitializationUtil;
import proguard.classfile.util.WarningPrinter;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.classfile.visitor.ClassVersionFilter;
import proguard.preverify.CodePreverifier;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * This sample application illustrates how to preverify classes with the
 * ProGuard API.
 *
 * Usage:
 *     java proguard.examples.Preverify input.jar output.jar
 */
public class Preverify
{
    public static void main(String[] args)
    {
        String inputJarFileName  = args[0];
        String outputJarFileName = args[1];

        try
        {
            // Read the program classes and library classes.
            // The latter are necessary to reconstruct the class hierarchy,
            // which is necessary to properly preverify the code.
            // We're only reading the base jmod here. General code may need
            // additional jmod files.
            String runtimeFileName = System.getProperty("java.home") + "/jmods/java.base.jmod";

            ClassPool libraryClassPool = JarUtil.readJar(runtimeFileName, true);
            ClassPool programClassPool = JarUtil.readJar(inputJarFileName, false);

            // We may get some warnings about missing dependencies.
            // They're a pain, but for proper results, we really need to have
            // all dependencies.
            PrintWriter printWriter       = new PrintWriter(System.err);
            WarningPrinter warningPrinter = new WarningPrinter(printWriter);

            // Initialize all cross-references.
            InitializationUtil.initialize(programClassPool, libraryClassPool, warningPrinter);

            // Flush the warnings.
            printWriter.flush();

            // Preverify the program classes.
            preverify(programClassPool);

            // Write the preverified classes to the jar file.
            JarUtil.writeJar(programClassPool, outputJarFileName);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Preverifies the code of the classes in the given class pool,
     * adding StackMapTable attributes to code that requires it.
     * @param programClassPool the classes to be preverified.
     */
    private static void preverify(ClassPool programClassPool)
    {
        programClassPool.classesAccept(
            new ClassVersionFilter(VersionConstants.CLASS_VERSION_1_6,
            new AllMethodVisitor(
            new AllAttributeVisitor(
            new CodePreverifier(false)))));
    }
}
