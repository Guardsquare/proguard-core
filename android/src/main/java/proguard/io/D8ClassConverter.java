/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

import com.android.tools.r8.ByteDataView;
import com.android.tools.r8.CompilationFailedException;
import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.DexIndexedConsumer;
import com.android.tools.r8.DiagnosticsHandler;
import com.android.tools.r8.origin.Origin;
import proguard.classfile.Clazz;
import proguard.classfile.ProgramClass;
import proguard.classfile.io.ProgramClassWriter;
import proguard.classfile.visitor.ClassVisitor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Set;

/**
 * This ClassVisitor converts and collects the classes that it visits, and uses D8
 * to convert them to the dex format.
<p>
 *
 * @author Thomas Neidhart
 */
public class D8ClassConverter implements ClassVisitor
{
    private final D8DexFile dexFile;

    public D8ClassConverter(D8DexFile dexFile)
    {
        this.dexFile = dexFile;
    }

    // Implementations for ClassVisitor.
    @Override
    public void visitAnyClass(Clazz clazz) {}


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        try
        {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            programClass.accept(
                new ProgramClassWriter(
                        new DataOutputStream(byteArrayOutputStream)
                )
            );
            byte[] classData = byteArrayOutputStream.toByteArray();
            synchronized (dexFile)
            {
                dexFile.addProgramClassData(classData);
            }
        }
        catch (Exception e)
        {
            System.err.println("Unexpected error while converting:");
            System.err.println("  Class       = ["+programClass.getName()+"]");

            e.printStackTrace();
            System.err.println("Not converting this class");
        }
    }


    // Helper classes.

    public static class D8DexFile
    {
        private final InMemoryDexConsumer consumer;
        private final D8Command.Builder  androidAppBuilder;
        private final int minSdkVersion;
        private final boolean debuggable;

        public D8DexFile(ClassPath libraryJars, int minSdkVersion, boolean debuggable)
        {
            this.minSdkVersion = minSdkVersion;
            this.debuggable = debuggable;

            consumer          = new InMemoryDexConsumer();
            androidAppBuilder = D8Command.builder();


            if (libraryJars != null)
            {
                // Add all configured library jars for D8.
                for (int index = 0; index < libraryJars.size(); index++)
                {
                    ClassPathEntry classPathEntry = libraryJars.get(index);

                    // Only add existing library jars and supported file types to D8,
                    // otherwise we might get runtime errors.
                    if (Files.exists(classPathEntry.getFile().toPath()) &&
                        (classPathEntry.isDex()) || isArchive(classPathEntry))
                    {
                        androidAppBuilder.addLibraryFiles(classPathEntry.getFile().toPath());
                    }
                }
            }
        }

        public static boolean isArchive(ClassPathEntry path) {

            return path.isApk()
                || path.isJar()
                || path.isAar()
                || path.isZip();
        }

        public void addProgramClassData(byte[] data)
        {
            androidAppBuilder.addClassProgramData(data, Origin.unknown());
        }


        public void writeTo(OutputStream outputStream)
            throws IOException {

            try {
                D8.run(
                    androidAppBuilder
                        .setMinApiLevel(minSdkVersion)
                        .setDisableDesugaring(true)
                        .setMode(debuggable ? CompilationMode.DEBUG : CompilationMode.RELEASE)
                        .setProgramConsumer(consumer)
                        .setIntermediate(false)
                        .build()
                );
                outputStream.write(consumer.data);
                outputStream.flush();
            } catch (CompilationFailedException e) {
                throw new IOException(e);
            }
        }
    }


    /**
     * A simple implementation of a DexIndexedConsumer that just keeps the dex file content in memory.
     */
    private static class InMemoryDexConsumer implements DexIndexedConsumer
    {
        private byte[] data;

        @Override
        public void accept(int fileIndex, ByteDataView data, Set<String> descriptors, DiagnosticsHandler handler)
        {
            this.data = data.copyByteData();
        }


        @Override
        public void finished(DiagnosticsHandler handler)
        {
            // Do nothing.
        }
    }
}
