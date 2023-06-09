package proguard.examples.instrument;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.classfile.visitor.MultiClassVisitor;
import proguard.io.ClassFilter;
import proguard.io.ClassReader;
import proguard.io.DataEntryClassWriter;
import proguard.io.DataEntrySource;
import proguard.io.FileSource;
import proguard.io.FixedFileWriter;
import proguard.io.JarReader;
import proguard.io.JarWriter;
import proguard.io.ZipWriter;

import java.io.File;
import java.io.IOException;

/**
 * This sample application illustrates how to modify bytecode with the ProGuardCORE API.
 * It adds logging code at the start of all methods of all classes that it processes.
 * <p>
 * Usage:
 *     java proguard.examples.AddMethodInvocationLogging input.jar output.jar
 */
public class AddMethodInvocationLogging
{
    public static void main(String[] args) throws IOException
    {
        String inputJarFileName  = args[0];
        String outputJarFileName = args[1];

        // We'll write the output to a jar file.
        JarWriter jarWriter =
            new JarWriter(
            new ZipWriter(
            new FixedFileWriter(
            new File(outputJarFileName))));

        // Parse and push all classes from the input jar.
        DataEntrySource source =
            new FileSource(new File(inputJarFileName));

        source.pumpDataEntries(
            new JarReader(
            new ClassFilter(
            new ClassReader(false, false, false, false, null,
            new MultiClassVisitor(
                // Modify the class.
                new AllMethodVisitor(
                new AllAttributeVisitor(
                new MyLoggingAdder())),

                // For simple changes that don't change the control flow,
                // we don't need to preverify the processed code from
                // scratch. The updated stack map tables remain valid.

                // Write the class file.
                new DataEntryClassWriter(jarWriter)
            )))));

        jarWriter.close();
    }


    /**
     * This AttributeVisitor inserts logging instructions at the start of every
     * code attribute that it visits.
     */
    private static class MyLoggingAdder
    implements           AttributeVisitor
    {
        private final CodeAttributeEditor codeAttributeEditor = new CodeAttributeEditor(true, true);


        // Implementations for AttributeVisitor.

        public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


        public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
        {
            // Create instructions to insert.
            String logMessage =
                ClassUtil.externalClassName(clazz.getName()) +
                ": entering method '" +
                ClassUtil.externalFullMethodDescription(clazz.getName(),
                                                        clazz.getAccessFlags(),
                                                        method.getName(clazz),
                                                        method.getDescriptor(clazz)) +
                "'";

            Instruction[] loggingInstructions =
                new InstructionSequenceBuilder((ProgramClass)clazz)
                    .getstatic("java/lang/System", "err", "Ljava/io/PrintStream;")
                    .ldc(logMessage)
                    .invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                    .instructions();

            // Insert the instructions at the start of the method.
            codeAttributeEditor.reset(codeAttribute.u4codeLength);
            codeAttributeEditor.insertBeforeInstruction(0, loggingInstructions);
            codeAttributeEditor.visitCodeAttribute(clazz, method, codeAttribute);
        }
    }
}
