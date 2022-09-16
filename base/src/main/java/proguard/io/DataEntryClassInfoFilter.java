/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;

import static proguard.classfile.ClassConstants.CLASS_FILE_EXTENSION;

/**
 * This DataEntryFilter filters data entries based on whether they correspond
 * to classes in a given class pool that have a given processing info.
 *
 * @author Eric Lafortune
 */
public class DataEntryClassInfoFilter
implements DataEntryFilter
{
    private final ClassPool classPool;
    private final Object    processingInfo;


    /**
     * Creates a new DataEntryClassInfoFilter.
     * @param classPool   the class pool in which the data entry is searched.
     * @param processingInfo the processing info that the found class should have.
     */
    public DataEntryClassInfoFilter(ClassPool classPool,
                                    Object    processingInfo)
    {
        this.classPool   = classPool;
        this.processingInfo = processingInfo;
    }


    // Implementations for DataEntryFilter.

    @Override
    public boolean accepts(DataEntry dataEntry)
    {
        // Is it a class entry?
        String name = dataEntry.getName();
        if (name.endsWith(CLASS_FILE_EXTENSION))
        {
            // Does it still have a corresponding class?
            String className = name.substring(0, name.length() - CLASS_FILE_EXTENSION.length());
            Clazz clazz = classPool.getClass(className);
            if (clazz != null)
            {
                // Does it have the specified processing info?
                return processingInfo.equals(clazz.getProcessingInfo());
            }
        }

        return false;
    }
}
