package proguard.examples;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.editor.*;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.io.*;
import proguard.util.ExtensionMatcher;;

import java.io.*;

/**
 * This sample application illustrates how to read bytecode and print it out in
 * text format with the ProGuard API.
 *
 * Usage:
 *     java proguard.examples.PrintClasses input.jar
 */
public class PrintClasses
{
    public static void main(String[] args)
    {
        String inputDirectoryName = args[0];

        try
        {
            // Parse all classes from the input jar,
            // and stream them to the class printer.
            DataEntrySource source =
                new DirectorySource(
                new File(inputDirectoryName));

            source.pumpDataEntries(
                new FilteredDataEntryReader(new DataEntryNameFilter(new ExtensionMatcher(".jar")),
                new JarReader(
                new ClassFilter(
                new ClassReader(false, false, false, false, null,
                new ClassPrinter())))));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
