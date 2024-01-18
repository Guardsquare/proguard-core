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
 * This {@link StringMatcher} tests whether strings start with a specified variable string and then
 * match another optional given {@link StringMatcher}.
 *
 * @see VariableStringMatcher
 * @author Eric Lafortune
 */
public class MatchedStringMatcher extends StringMatcher {
  private final VariableStringMatcher variableStringMatcher;
  private final StringMatcher nextMatcher;

  /**
   * Creates a new MatchedStringMatcher
   *
   * @param variableStringMatcher the variable string matcher that can provide the string to match.
   * @param nextMatcher an optional string matcher to match the remainder of the string.
   */
  public MatchedStringMatcher(
      VariableStringMatcher variableStringMatcher, StringMatcher nextMatcher) {
    this.variableStringMatcher = variableStringMatcher;
    this.nextMatcher = nextMatcher;
  }

  // Implementation for StringMatcher.

  @Override
  public String prefix() {
    String prefix = variableStringMatcher.getMatchingString();
    // Append the next matcher's prefix if applicable
    if (nextMatcher != null) {
      String nextPrefix = nextMatcher.prefix();
      if (nextPrefix == null) {
        return null;
      }
      prefix += nextMatcher.prefix();
    }
    return prefix;
  }

  @Override
  protected boolean matches(String string, int beginOffset, int endOffset) {
    String matchingString = variableStringMatcher.getMatchingString();
    if (matchingString == null) {
      return false;
    }

    int stringLength = endOffset - beginOffset;
    int matchingStringLength = matchingString.length();
    return stringLength >= matchingStringLength
        && string.startsWith(matchingString, beginOffset)
        && ((nextMatcher == null && stringLength == matchingStringLength)
            || (nextMatcher != null
                && nextMatcher.matches(string, beginOffset + matchingStringLength, endOffset)));
  }
}
