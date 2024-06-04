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

import static proguard.dexfile.reader.DexFileReader.KEEP_CLINIT;
import static proguard.dexfile.reader.DexFileReader.SKIP_CODE;
import static proguard.dexfile.reader.DexFileReader.SKIP_DEBUG;
import static proguard.dexfile.reader.DexReaderFactory.createSingleReader;

import java.io.IOException;
import java.io.InputStream;
import proguard.classfile.constant.PrimitiveArrayConstant;
import proguard.classfile.util.PrimitiveArrayConstantReplacer;
import proguard.classfile.visitor.ClassVisitor;
import proguard.dexfile.converter.Dex2Pro;
import proguard.dexfile.reader.DexException;
import proguard.dexfile.reader.node.DexFileNode;

/**
 * This data entry reader reads dex files, converts their classes, and passes them to a given class
 * visitor. It is essential to call shutdown() after pumping to this {@link DataEntryReader}, as
 * there might be active workers left that need an orderly shutdown.
 */
public class ParallelDexClassReader implements DataEntryReader {
  private final boolean readCode;
  private final ClassVisitor classVisitor;
  public final Dex2Pro dex2pro;

  /**
   * Creates a new DexClassReader.
   *
   * <p>If {@link PrimitiveArrayConstant}s are generated then they should be converted back to
   * standard Java arrays before converting to Java class files using {@link
   * PrimitiveArrayConstantReplacer}.
   *
   * @param readCode specifies whether to read the actual code or just skip it.
   * @param usePrimitiveArrayConstants specifies whether {@link PrimitiveArrayConstant} can be
   *     generated when applicable.
   * @param classVisitor the class visitor to which decoded classes will be passed.
   */
  public ParallelDexClassReader(
      boolean readCode,
      boolean usePrimitiveArrayConstants,
      ClassVisitor classVisitor,
      int maximumThreads) {
    this.readCode = readCode;
    this.classVisitor = classVisitor;
    this.dex2pro = new Dex2Pro(maximumThreads);
    dex2pro.usePrimitiveArrayConstants(usePrimitiveArrayConstants);
  }

  // Implementation for classVisitor.

  @Override
  public void read(DataEntry dataEntry) throws IOException {
    // Get the input.
    try (InputStream inputStream = dataEntry.getInputStream()) {
      // Fill out a Dex2jar file node.
      DexFileNode fileNode = new DexFileNode();
      int readerConfig = readCode ? 0 : (SKIP_CODE | KEEP_CLINIT | SKIP_DEBUG);
      createSingleReader(inputStream).accept(fileNode, readerConfig);

      // Convert it to classes, with the help of Dex2Pro.
      dex2pro.convertDex(fileNode, classVisitor);
    } catch (DexException e) {
      throw new IOException("Dex file conversion failed: " + e.getMessage(), e);
    }
  }

  /** Shuts down and waits for up to timeoutSeconds for any outstanding workers to finish. */
  public void shutdown(int timeoutSeconds) {
    dex2pro.shutdown(timeoutSeconds);
  }
}
