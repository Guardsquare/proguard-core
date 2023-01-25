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

import proguard.classfile.ClassConstants;
import proguard.classfile.ClassPool;
import proguard.classfile.visitor.ClassFeatureNameCollector;
import proguard.dexfile.AndroidConstants;
import proguard.util.ClassPoolFeatureNameFunction;
import proguard.util.EmptyStringMatcher;
import proguard.util.FixedStringMatcher;
import proguard.util.StringMatcher;
import proguard.util.SuffixRemovingStringFunction;
import proguard.util.TransformedStringMatcher;

import java.util.HashSet;
import java.util.Set;

/**
 * This class can create DataEntryWriter instances for writing dex files.
 *
 * @author Eric Lafortune
 * @author Thomas Neidhart
 */
public class DexDataEntryWriterFactory
{
    private final ClassPool           programClassPool;
    private final int                 multiDexCount;
    private final boolean             appBundle;
    private final ClassPath           libraryJars;
    private final int                 minSdkVersion;
    private final boolean             debuggable;
    private final DataEntryReader     extraDexDataEntryVisitor;

    /**
     * Creates a new DexDataEntryWriterFactory.
     *
     * @param programClassPool         the program class pool to process.
     * @param libraryJars              {@link ClassPathEntry} list of library jars
     * @param appBundle                specifies whether the dex files should
     *                                 be named following the app bundle
     *                                 directory structure.
     * @param multiDexCount            specifies the number of dex files in
     *                                 the multidex partitioning.
     * @param minSdkVersion            the minimum supported API level.
     * @param debuggable               whether the dex file shall be debuggable
     *                                 or not.
     * @param extraDexDataEntryVisitor an optional extra visitor for all dex
     *                                 data entries that are written. The
     *                                 visitor can use the data entry names,
     *                                 but must not read their contents.
     */
    public DexDataEntryWriterFactory(ClassPool       programClassPool,
                                     ClassPath       libraryJars,
                                     boolean         appBundle,
                                     int             multiDexCount,
                                     int             minSdkVersion,
                                     boolean         debuggable,
                                     DataEntryReader extraDexDataEntryVisitor)
    {
        this.programClassPool         = programClassPool;
        this.libraryJars              = libraryJars;
        this.appBundle                = appBundle;
        this.multiDexCount            = multiDexCount;
        this.minSdkVersion            = minSdkVersion;
        this.debuggable               = debuggable;
        this.extraDexDataEntryVisitor = extraDexDataEntryVisitor;
    }


    /**
     * Wraps the given data entry writer in dex data entry writers for
     * "classes.dex", etc, supporting feature dex files, multidex, and
     * split dex files.
     * @param dexWriter the data entry writer to which dex files can be
     *                  written.
     */
    public DataEntryWriter wrapInDexWriter(DataEntryWriter dexWriter)
    {
        return wrapInDexWriter(dexWriter, dexWriter);
    }


    /**
     * Wraps the given data entry writers in dex data entry writers for
     * "classes.dex", etc, supporting feature dex files, multidex, and
     * split dex files.
     * @param dexWriter   the data entry writer to which dex files can be
     *                    written.
     * @param otherWriter the data entry writer to which all other files
     *                    can be written.
     */
    private DataEntryWriter wrapInDexWriter(DataEntryWriter dexWriter,
                                            DataEntryWriter otherWriter)
    {
        // Collect all unique feature names.
        Set<String> featureNames = new HashSet<>();
        programClassPool.classesAccept(new ClassFeatureNameCollector(featureNames));

        if (featureNames.isEmpty())
        {
            // Wrap in a writer for the only classes.dex file.
            otherWriter = wrapInDexWriter(appBundle ?
                            AndroidConstants.AAB_BASE + AndroidConstants.AAB_DEX_INFIX :
                            "",
                    appBundle ?
                            AndroidConstants.AAB_BASE + AndroidConstants.AAB_ROOT_INFIX :
                            "",
                    dexWriter,
                    otherWriter);
        }
        else
        {
            // Start with wrapping in a writer for the basic classes.dex
            // file.
            otherWriter = wrapInDexWriter(AndroidConstants.AAB_BASE + AndroidConstants.AAB_DEX_INFIX,
                    AndroidConstants.AAB_BASE + AndroidConstants.AAB_ROOT_INFIX,
                    dexWriter,
                    otherWriter);

            // Don't close the writer for dex files from enclosing writers.
            dexWriter = new NonClosingDataEntryWriter(dexWriter);

            // Wrap with writers for any features.
            for(String featureName : featureNames)
            {
                otherWriter = wrapInFeatureDexWriter(featureName,
                        dexWriter,
                        otherWriter);

            }

        }

        return otherWriter;
    }

    /**
     * Wraps the given data entry writers in dex data entry writers for
     * "feature/dex/classes.dex", etc, for the given feature, supporting
     * multidex and split dex files.
     * @param featureName the feature name that can be used to filter classes
     *                    and name dex files.
     * @param dexWriter   the data entry writer to which dex files can be
     *                    written.
     * @param otherWriter the data entry writer to which all other files
     *                    can be written.
     */
    private DataEntryWriter wrapInFeatureDexWriter(String          featureName,
                                                   DataEntryWriter dexWriter,
                                                   DataEntryWriter otherWriter)
    {
        return
                new NameFilteredDataEntryWriter(
                        new TransformedStringMatcher(new SuffixRemovingStringFunction(ClassConstants.CLASS_FILE_EXTENSION),
                        new TransformedStringMatcher(new ClassPoolFeatureNameFunction(programClassPool, AndroidConstants.AAB_BASE),
                        new FixedStringMatcher(featureName, new EmptyStringMatcher()))),
                            wrapInDexWriter(featureName + AndroidConstants.AAB_DEX_INFIX,
                                            featureName + AndroidConstants.AAB_ROOT_INFIX,
                                            dexWriter,
                                            otherWriter),
                            otherWriter);
    }

    /**
     * Wraps the given data entry writers in dex data entry writers for
     * "classes.dex", etc, supporting multidex and split dex files.
     * @param dexFilePrefix      the path prefix for dex files.
     * @param dexWriter          the data entry writer to which dex files can be
     *                           written.
     * @param otherWriter        the data entry writer to which all other files
     *                           can be written.
     */
    private DataEntryWriter wrapInDexWriter(String          dexFilePrefix,
                                            String          extraDexFilePrefix,
                                            DataEntryWriter dexWriter,
                                            DataEntryWriter otherWriter)
    {
        // Start with wrapping in a writer for the basic classes.dex
        // file.
        otherWriter = wrapInSimpleDexWriter(dexFilePrefix,
                                            dexWriter,
                                            otherWriter);

//        // Don't close the writer for dex files from enclosing writers.
        dexWriter = new NonClosingDataEntryWriter(otherWriter);

        // Wrap with writers for any multidex files.
        // The Android runtime and util classes will load them eagerly.
        for (int index = multiDexCount; index > 0; index--)
        {
            otherWriter = wrapInMultiDexWriter(dexFilePrefix,
                                               dexWriter,
                                               otherWriter,
                                               index);
        }

        return otherWriter;
    }


    /**
     * Wraps the given data entry writer in a dex data entry writer for
     * "classes.dex".
     */
    private DataEntryWriter wrapInSimpleDexWriter(String          dexFilePrefix,
                                                  DataEntryWriter dexWriter,
                                                  DataEntryWriter otherWriter)
    {
        // Add a writer for the simple file.
        String dexFileName = dexFilePrefix +
                             AndroidConstants.CLASSES_DEX;

        // Add a writer for the base dex file.
        // TODO: Don't force empty dex files for features in app bundles, but put hasCode="false" in their manifest files.
        return createDataEntryWriter(null,
                                      dexFileName,
                                     true,
                                     dexWriter,
                                     otherWriter);

    }


    /**
     * Wraps the given data entry writer in a dex data entry writer for
     * "classes[index].dex".
     */
    private DataEntryWriter wrapInMultiDexWriter(String          dexFilePrefix,
                                                 DataEntryWriter dexWriter,
                                                 DataEntryWriter otherWriter,
                                                 int             index)
    {
        // Add a writer for the multidex file.
        String dexBaseName = AndroidConstants.CLASSES_PREFIX + (index + 1) +
                             AndroidConstants.DEX_FILE_EXTENSION;
        String dexFileName = dexFilePrefix + dexBaseName;

        // Is the entry to be partitioned into this dex file?
        // Note that the filter currently works on the base name.
        return new FilteredDataEntryWriter(
            new DataEntryClassInfoFilter(programClassPool, dexBaseName),

                // Then use the matching dex writer.
                createDataEntryWriter(null,
                                      dexFileName,
                                      false,
                                      dexWriter,
                                      otherWriter),

                // Otherwise, the standard writer.
                otherWriter);
    }

    /**
     * Wraps the given data entry writer in a dex data entry writer with the
     * given parameters.
     */
    private DataEntryWriter createDataEntryWriter(StringMatcher   classNameFilter,
                                                  String          dexFileName,
                                                  boolean         forceDex,
                                                  DataEntryWriter dexWriter,
                                                  DataEntryWriter otherWriter)
    {
        return
                // Convert with d8.
                // This converter does not support a class name order.
                new D8BasedDexDataEntryWriter(programClassPool,
                                              classNameFilter,
                                              libraryJars,
                                              dexFileName,
                                              forceDex,
                                              minSdkVersion,
                                              debuggable,
                                              extraDexDataEntryVisitor,
                                              dexWriter,
                                              otherWriter);
    }
}
