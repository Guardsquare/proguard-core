/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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
package proguard.classfile.attribute;

/**
 * This line number table entry contains additional information about its source. This information
 * can not be represented in class files, but it can be used internally to represent lines in
 * inlined or merged code.
 *
 * @author Eric Lafortune
 * @deprecated Use {@link StructuredLineNumberInfo} instead to properly track the source of lines
 *     that don't originate from the given method.
 */
@Deprecated
public class ExtendedLineNumberInfo extends LineNumberInfo {
  public String source;

  /** Creates an uninitialized ExtendedLineNumberInfo. */
  public ExtendedLineNumberInfo() {}

  /** Creates an initialized ExtendedLineNumberInfo. */
  public ExtendedLineNumberInfo(int u2startPC, int u2lineNumber, String source) {
    super(u2startPC, u2lineNumber);

    this.source = source;
  }

  // Implementations for LineNumberInfo.

  public String getSource() {
    return source;
  }

  public String getSourceMethod() {
    if (source == null) return null;
    if (!source.contains(":")) return source;
    return source.split(":")[0];
  }

  public int getSourceLineStart() {
    if (source == null || !source.contains(":")) return LINE_RANGE_NO_SOURCE;
    return Integer.parseInt(source.split(":")[1]);
  }

  public int getSourceLineEnd() {
    if (source == null || !source.contains(":")) return LINE_RANGE_NO_SOURCE;
    return Integer.parseInt(source.split(":")[2]);
  }

  public int getBlockId() {
    return System.identityHashCode(source);
  }

  public LineNumberInfoBlock getBlock() {
    return new Block(source);
  }

  private static class Block implements LineNumberInfoBlock {
    private final String source;

    private Block(String source) {
      this.source = source;
    }

    public LineNumberInfo line(int u2StartPc, int u2LineNumber) {
      return new ExtendedLineNumberInfo(u2StartPc, u2LineNumber, source);
    }
  }
}
