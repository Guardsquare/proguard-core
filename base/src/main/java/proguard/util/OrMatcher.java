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
 * This {@link StringMatcher} tests whether strings matches at least one of the given {@link
 * StringMatcher} instances.
 *
 * @author Eric Lafortune
 */
public class OrMatcher extends StringMatcher {
  private final StringMatcher[] matchers;

  /** Creates a new OrMatcher with the given string matchers. */
  public OrMatcher(StringMatcher... matchers) {
    this.matchers = matchers;
  }

  // Implementations for StringMatcher.

  @Override
  public String prefix() {
    if (this.matchers.length == 0 || this.matchers[0] == null) return null;
    if (this.matchers.length == 1) return this.matchers[0].prefix();
    // TODO(T6101) - if there is more than 1 matcher, find a more specific prefix than the empty
    // string
    return "";
  }

  @Override
  protected boolean matches(String string, int beginOffset, int endOffset) {
    for (StringMatcher matcher : matchers) {
      if (matcher.matches(string, beginOffset, endOffset)) {
        return true;
      }
    }

    return false;
  }
}
