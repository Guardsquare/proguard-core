/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2026 Guardsquare NV
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Data entry created by {@link JrtDataEntrySource}. */
public class JrtDataEntry implements DataEntry {
  private final Path jrtPath;
  private final String name;
  private final long size;

  private InputStream inputStream = null;

  JrtDataEntry(Path jrtPath, String name, long size) {
    this.jrtPath = jrtPath;
    this.name = name;
    this.size = size;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getOriginalName() {
    return getName();
  }

  @Override
  public long getSize() {
    return size;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (inputStream == null) {
      inputStream = new BufferedInputStream(Files.newInputStream(jrtPath));
    }
    return inputStream;
  }

  @Override
  public void closeInputStream() throws IOException {
    if (inputStream != null) {
      inputStream.close();
      inputStream = null;
    }
  }

  @Override
  public DataEntry getParent() {
    return null;
  }

  @Override
  public String toString() {
    return "jrt:/" + jrtPath;
  }
}
