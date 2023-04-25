/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package proguard.io;

import proguard.classfile.constant.PrimitiveArrayConstant;
import proguard.classfile.util.PrimitiveArrayConstantReplacer;
import proguard.classfile.visitor.ClassPrinter;
import proguard.classfile.visitor.ClassVisitor;
import proguard.dexfile.converter.Dex2Pro;
import proguard.dexfile.reader.DexException;
import proguard.dexfile.reader.DexFileReader;
import proguard.dexfile.reader.node.DexFileNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static proguard.dexfile.reader.DexFileReader.KEEP_CLINIT;
import static proguard.dexfile.reader.DexFileReader.SKIP_CODE;
import static proguard.dexfile.reader.DexFileReader.SKIP_DEBUG;

/**
 * This data entry reader reads dex files, converts their classes, and passes them to
 * a given class visitor.
 *
 * @author Eric Lafortune
 */
public class DexClassReader implements DataEntryReader {
    private final boolean      readCode;
    private final ClassVisitor classVisitor;
    private final boolean      usePrimitiveArrayConstants;

    /**
     * Creates a new DexClassReader.
     * <p>
     * Does not generate {@link PrimitiveArrayConstant}s by default, which is a custom
     * ProGuardCORE extension.
     *
     * @param readCode     specifies whether to read the actual code or just
     *                     skip it.
     * @param classVisitor the class visitor to which decoded classes will be
     *                     passed.
     */
    public DexClassReader(boolean readCode,
                          ClassVisitor classVisitor)
    {
        this(readCode, false, classVisitor);
    }

    /**
     * Creates a new DexClassReader.
     * <p>
     * If {@link PrimitiveArrayConstant}s are generated then they should be converted back to standard
     * Java arrays before converting to Java class files using {@link PrimitiveArrayConstantReplacer}.
     *
     * @param readCode     specifies whether to read the actual code or just
     *                     skip it.
     * @param usePrimitiveArrayConstants specifies whether {@link PrimitiveArrayConstant} can
     *                                   be generated when applicable.
     * @param classVisitor the class visitor to which decoded classes will be
     *                     passed.
     */
    public DexClassReader(boolean      readCode,
                          boolean      usePrimitiveArrayConstants,
                          ClassVisitor classVisitor)
    {
        this.readCode                   = readCode;
        this.usePrimitiveArrayConstants = usePrimitiveArrayConstants;
        this.classVisitor               = classVisitor;
    }


    // Implementation for classVisitor.

    @Override
    public void read(DataEntry dataEntry) throws IOException {
        // Get the input.
        try (InputStream inputStream = dataEntry.getInputStream())
        {
            // Fill out a Dex2jar file node.
            DexFileNode fileNode = new DexFileNode();
            int readerConfig = readCode ? 0 : (SKIP_CODE | KEEP_CLINIT | SKIP_DEBUG);
            new DexFileReader(inputStream).accept(fileNode, readerConfig);

            // Convert it to classes, with the help of Dex2Pro.
            new Dex2Pro()
                    .usePrimitiveArrayConstants(usePrimitiveArrayConstants)
                    .convertDex(fileNode, classVisitor);
        }
        catch (DexException e)
        {
            throw new IOException("Dex file conversion failed: " + e.getMessage(), e);
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
