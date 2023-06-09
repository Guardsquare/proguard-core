package proguard.examples;

import proguard.classfile.AccessConstants;
import proguard.classfile.ClassConstants;
import proguard.classfile.ProgramClass;
import proguard.classfile.VersionConstants;
import proguard.classfile.editor.ClassBuilder;
import proguard.classfile.io.ProgramClassWriter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This sample application illustrates how to create a class with the ProGuardCORE API.
 *
 * Usage:
 *     java proguard.examples.CreateHelloWorldClass
 */
public class CreateHelloWorldClass
{
    private static final String CLASS_NAME = "HelloWorld";
    private static final String MESSAGE    = "Hello, world!";


    public static void main(String[] args) throws IOException
    {
        // Create the class.
        ProgramClass programClass = createMessagePrintingClass(CLASS_NAME, MESSAGE);

        // Write out the class.
        String classFileName = CLASS_NAME + ClassConstants.CLASS_FILE_EXTENSION;

        try (DataOutputStream dataOutputStream = new DataOutputStream(Files.newOutputStream(Paths.get(classFileName))))
        {
            programClass.accept(new ProgramClassWriter(dataOutputStream));
        }
    }


    /**
     * Creates a HelloWorld class.
     */
    public static ProgramClass createMessagePrintingClass(String name, String message)
    {
        return
            // Start building the class.
            new ClassBuilder(
                VersionConstants.CLASS_VERSION_1_8,
                AccessConstants.PUBLIC,
                name,
                ClassConstants.NAME_JAVA_LANG_OBJECT)

                // Add the main method.
                .addMethod(
                    AccessConstants.PUBLIC |
                    AccessConstants.STATIC,
                    "main",
                    "([Ljava/lang/String;)V",
                    50,

                    // Compose the equivalent of this java code:
                    //     System.out.println("Hello, world!");
                    code -> code
                        .getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                        .ldc(message)
                        .invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                        .return_())

                // We don't need to preverify simple code that doesn't have
                // special control flow. It works fine without a stack map
                // table attribute.

                // Retrieve the final class.
                .getProgramClass();
    }
}
