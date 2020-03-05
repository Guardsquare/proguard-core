/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */
package proguard.resources.kotlinmodule.visitor;

import proguard.resources.file.ResourceFile;
import proguard.resources.file.visitor.*;
import proguard.resources.kotlinmodule.*;

import java.io.PrintWriter;
import java.util.Map;

import static proguard.classfile.kotlin.visitor.KotlinMetadataPrinter.DEFAULT_MISSING_REF_INDICATOR;


/**
 * Print Kotlin modules.
 */
public class KotlinModulePrinter
extends      SimplifiedResourceFileVisitor
implements   ResourceFileVisitor,
             KotlinModuleVisitor,
             KotlinModulePackageVisitor
{

    private static final String INDENTATION = "  ";

    private final PrintWriter pw;

    private int indentation;

    private String missingRefIndicator;

    public KotlinModulePrinter()
    {
        this(new PrintWriter(System.out, true), DEFAULT_MISSING_REF_INDICATOR);
    }

    public KotlinModulePrinter(PrintWriter pw, String missingRefIndicator)
    {
        this.pw                  = pw;
        this.missingRefIndicator = missingRefIndicator;
    }


    // Implementations for ResourceFileVisitor.

    @Override
    public void visitAnyResourceFile(ResourceFile resourceFile) {}

    @Override
    public void visitKotlinModule(KotlinModule kotlinModule)
    {
        println("_____________________________________________________________________");
        println("[MODL] " + kotlinModule.name + " from file(" + kotlinModule.fileName + ")");

        indent();
        kotlinModule.modulePackagesAccept(this);
        outdent();
    }

    public void visitKotlinModulePackage(KotlinModule kotlinModule, KotlinModulePackage kotlinModulePart)
    {
        println("[MPKG] \"" + kotlinModulePart.fqName + "\"");

        indent();
        for (int i = 0; i < kotlinModulePart.fileFacadeNames.size(); i++)
        {
            String fileFacadeName = kotlinModulePart.fileFacadeNames.get(i);
            println("[FFAC] " + hasRefIndicator(kotlinModulePart.referencedFileFacades.get(i)) + fileFacadeName);
        }

        kotlinModulePart.multiFileClassParts
            .values()
            .stream()
            .distinct()
            .forEachOrdered(
                (multiFileFacadeName) ->
                {
                    println("[MFAC] " + multiFileFacadeName);

                    indent();
                    kotlinModulePart.multiFileClassParts
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().equals(multiFileFacadeName))
                        .map(Map.Entry::getKey)
                        .forEachOrdered(
                            multiFilePartName ->
                                println(
                                    "[MPRT] " +
                                    hasRefIndicator(kotlinModulePart.referencedMultiFileParts.get(multiFilePartName)) +
                                    multiFilePartName));
                    outdent();
                });

        outdent();
    }

    // Small utility methods.

    private void indent()
    {
        indentation++;
    }

    private void outdent()
    {
        indentation--;
    }

    private void println(String string)
    {
        print(string);
        println();
    }

    private void print(String string)
    {
        for (int index = 0; index < indentation; index++)
        {
            pw.print(INDENTATION);
        }

        pw.print(string);
    }

    private void println()
    {
        pw.println();
    }

    private String hasRefIndicator(Object arg)
    {
        return arg == null ? this.missingRefIndicator + " " : "";
    }
}
