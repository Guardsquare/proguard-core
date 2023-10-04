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
package proguard.util;

import java.util.HashSet;
import java.util.List;

/**
 * This {@link StringParser} can create a {@link CollectionMatcher} instance for regular expressions.
 * The regular expressions are either presented as a list, or they are interpreted as comma-separated lists.
 * These lists should contain strings that need to be literally matched meaning that wildcards, negations,... are
 * not interpreted by the {@link StringParser}.
 */
public class CollectionParser implements StringParser
{

    /**
     * Creates a new ListParser that parses individual elements in the
     * comma-separated list with the given StringParser.
     */
    public CollectionParser() {}


    // Implementations for StringParser.

    public StringMatcher parse(String regularExpression)
    {
        // Split the regular expression to a list and parse it.
        return parse(ListUtil.commaSeparatedList(regularExpression));
    }


    /**
     * Creates a StringMatcher for the given regular expression, which can
     * be a list of optionally negated simple entries.
     * <p/>
     * An empty list results in a StringMatcher that matches any string.
     */
    public StringMatcher parse(List<String> regularExpressions)
    {
        return new CollectionMatcher(new HashSet<>(regularExpressions));
    }
}
