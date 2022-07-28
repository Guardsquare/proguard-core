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
package proguard.io;

import proguard.util.StringMatcher;

/**
 * This {@link DataEntryFilter} filters data entries based on whether their names match
 * a given {@link StringMatcher}.
 *
 * @author Eric Lafortune
 */
public class DataEntryNameFilter
implements   DataEntryFilter
{
    private final StringMatcher stringMatcher;


    /**
     * Creates a new DataEntryNameFilter.
     * @param stringMatcher the string matcher that will be applied to the names
     *                      of the filtered data entries.
     */
    public DataEntryNameFilter(StringMatcher stringMatcher)
    {
        this.stringMatcher = stringMatcher;
    }


    // Implementations for DataEntryFilter.

    @Override
    public boolean accepts(DataEntry dataEntry)
    {
        return dataEntry != null && stringMatcher.matches(dataEntry.getName());
    }
}
