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

import java.io.IOException;

/**
 * This interface provides a source for data entries. The implementation
 * determines the type of the data entries. Typical examples are files or
 * directories. The source can pump its data entries into a data entry reader
 * (a "push" model for the entries), which can then optionally read their
 * contents (a "pull" model for the contents).
 *
 * @author Eric Lafortune
 */
public interface DataEntrySource
{
    /**
     * Applies the given DataEntryReader to all data entries that the
     * implementation can provide.
     */
    public void pumpDataEntries(DataEntryReader dataEntryReader)
    throws IOException;
}
