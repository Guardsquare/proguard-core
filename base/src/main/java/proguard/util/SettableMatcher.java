/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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
package proguard.util;

/**
 * This {@link StringMatcher} delegates to a another {@link StringMatcher} that can be set after
 * this {@link StringMatcher} has been constructed.
 *
 * @author Eric Lafortune
 */
public class SettableMatcher extends StringMatcher {
  private StringMatcher matcher;

  public void setMatcher(StringMatcher matcher) {
    this.matcher = matcher;
  }

  // Implementations for StringMatcher.

  @Override
  public String prefix() {
    return matcher.prefix();
  }

  @Override
  protected boolean matches(String string, int beginOffset, int endOffset) {
    return matcher.matches(string, beginOffset, endOffset);
  }
}
