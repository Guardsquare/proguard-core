/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2017 GuardSquare NV
 */
package dex_reader.reader;

import dex_reader_api.node.DexFileNode;
import dex_translator.converter.Dex2Pro;
import proguard.classfile.visitor.ClassPrinter;
import proguard.classfile.visitor.ClassVisitor;
import proguard.io.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * This data entry reader reads dex files, converts their classes, and passes them to
 * a given class visitor.
 *
 * @author Eric Lafortune
 */
public class DexClassReader implements DataEntryReader {
    private final boolean readCode;
    private final ClassVisitor classVisitor;

    /**
     * Creates a new DexClassReader.
     *
     * @param readCode     specifies whether to read the actual code or just
     *                     skip it.
     * @param classVisitor the class visitor to which decoded classes will be
     *                     passed.
     */
    public DexClassReader(boolean readCode,
                          ClassVisitor classVisitor) {
        this.readCode = readCode;
        this.classVisitor = classVisitor;
    }


    // Implementation for classVisitor.

    @Override
    public void read(DataEntry dataEntry) throws IOException {
        // Get the input.
        InputStream inputStream = dataEntry.getInputStream();
        try {
            // Fill out a Dex2jar file node.
            DexFileNode fileNode = new DexFileNode();
            int readerConfig = readCode ? 0 : (DexFileReader.SKIP_CODE |
                    DexFileReader.KEEP_CLINIT |
                    DexFileReader.SKIP_DEBUG);
            new DexFileReader(inputStream).accept(fileNode, readerConfig);

            // Convert it to classes, with the help of Dex2jar.
            new Dex2Pro().convertDex(fileNode, classVisitor);
        } finally {
            inputStream.close();
        }
    }

    // Small utility methods.

    /**
     * This main method illustrates and tests the class. It reads an input
     * dex file (or jar file with a dex file) and prints out its classes.
     */
    public static void main(String[] args) throws Exception {
        String fileName = args.length > 0 ?
                args[0] :
                "classes.dex";

        DataEntryReader reader =
                new DexClassReader(true,
                        new ClassPrinter());

        if (!fileName.endsWith(".dex")) {
            reader =
                    new JarReader(
                            new NameFilteredDataEntryReader("**.dex",
                                    reader));
        }

        new FileSource(new File(fileName)).pumpDataEntries(reader);
    }
}
