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

import java.util.Collections;
import java.util.List;

/**
 * Representation of a line number table entry.
 *
 * @author Eric Lafortune
 */
public class LineNumberInfo {
  public static int SIMPLE_LINE_NUMBER_BLOCK_ID = -1;
  public static int LINE_RANGE_NO_SOURCE = -1;

  public int u2startPC;
  public int u2lineNumber;

  /** Creates an uninitialized LineNumberInfo. */
  public LineNumberInfo() {}

  /** Creates an initialized LineNumberInfo. */
  public LineNumberInfo(int u2startPC, int u2lineNumber) {
    this.u2startPC = u2startPC;
    this.u2lineNumber = u2lineNumber;
  }

  /**
   * Returns a description of the source of the line, if known, or null otherwise. Standard line
   * number entries don't contain information about their source; it is assumed to be the same
   * source file.
   */
  public String getSource() {
    return null;
  }

  public String getSourceMethod() {
    return null;
  }

  public int getSourceLineStart() {
    return LINE_RANGE_NO_SOURCE;
  }

  public int getSourceLineEnd() {
    return LINE_RANGE_NO_SOURCE;
  }

  public int getBlockId() {
    return SIMPLE_LINE_NUMBER_BLOCK_ID;
  }

  public List<LineOrigin> getOrigin() {
    return Collections.emptyList();
  }

  public LineNumberInfoBlock getBlock() {
    return new Block();
  }

  private static class Block implements LineNumberInfoBlock {
    public LineNumberInfo line(int u2StartPc, int u2LineNumber) {
      return new LineNumberInfo(u2StartPc, u2LineNumber);
    }
  }
}
