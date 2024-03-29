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

import java.io.File;
import java.io.IOException;
import java.util.List;
import proguard.util.ListUtil;

/**
 * This class represents an entry from a class path: an apk, a jar, an aar, a war, a zip, an ear, or
 * a directory. It has a name, and a flag to indicate whether the entry is an input entry or an
 * output entry.
 *
 * <p>It also has an optional feature name that can serve as a tag to indicate the group to which
 * the entry belongs, e.g. a given Android dynamic feature.
 *
 * <p>It also has optional filters for the names of the contained resource/classes, apks, jars,
 * aars, wars, ears, and zips.
 *
 * @author Eric Lafortune
 */
public class ClassPathEntry {
  private File file;
  private boolean output;
  private String featureName;
  private List<String> filter;
  private List<String> apkFilter;
  private List<String> aabFilter;
  private List<String> jarFilter;
  private List<String> aarFilter;
  private List<String> warFilter;
  private List<String> earFilter;
  private List<String> jmodFilter;
  private List<String> zipFilter;

  private String cachedName;

  /** Creates a new ClassPathEntry with the given file and output flag. */
  public ClassPathEntry(File file, boolean isOutput) {
    this.file = file;
    this.output = isOutput;
  }

  /** Creates a new ClassPathEntry with the given file, output flag, and optional feature name. */
  public ClassPathEntry(File file, boolean isOutput, String featureName) {
    this.file = file;
    this.output = isOutput;
    this.featureName = featureName;
  }

  /** Returns the path name of the entry. */
  public String getName() {
    if (cachedName == null) {
      cachedName = getUncachedName();
    }

    return cachedName;
  }

  /** Returns the uncached path name of the entry. */
  private String getUncachedName() {
    try {
      return file.getCanonicalPath();
    } catch (IOException ex) {
      return file.getPath();
    }
  }

  /** Returns the file. */
  public File getFile() {
    return file;
  }

  /** Sets the file. */
  public void setFile(File file) {
    this.file = file;
    this.cachedName = null;
  }

  /** Returns whether this data entry is an output entry. */
  public boolean isOutput() {
    return output;
  }

  /** Specifies whether this data entry is an output entry. */
  public void setOutput(boolean output) {
    this.output = output;
  }

  /** Returns the feature name. */
  public String getFeatureName() {
    return featureName;
  }

  /** Sets the feature name. */
  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  /** Returns whether this data entry is a dex file. */
  public boolean isDex() {
    return hasExtension(".dex");
  }

  /** Returns whether this data entry is an apk file. */
  public boolean isApk() {
    return hasExtension(".apk") || hasExtension(".ap_");
  }

  /** Returns whether this data entry is an aab file. */
  public boolean isAab() {
    return hasExtension(".aab");
  }

  /** Returns whether this data entry is a jar file. */
  public boolean isJar() {
    return hasExtension(".jar");
  }

  /** Returns whether this data entry is an aar file. */
  public boolean isAar() {
    return hasExtension(".aar");
  }

  /** Returns whether this data entry is a war file. */
  public boolean isWar() {
    return hasExtension(".war");
  }

  /** Returns whether this data entry is a ear file. */
  public boolean isEar() {
    return hasExtension(".ear");
  }

  /** Returns whether this data entry is a jmod file. */
  public boolean isJmod() {
    return hasExtension(".jmod");
  }

  /** Returns whether this data entry is a zip file. */
  public boolean isZip() {
    return hasExtension(".zip");
  }

  /** Returns whether this data entry has the given extension. */
  private boolean hasExtension(String extension) {
    return endsWithIgnoreCase(file.getPath(), extension);
  }

  /** Returns whether the given string ends with the given suffix, ignoring its case. */
  private static boolean endsWithIgnoreCase(String string, String suffix) {
    int stringLength = string.length();
    int suffixLength = suffix.length();

    return string.regionMatches(true, stringLength - suffixLength, suffix, 0, suffixLength);
  }

  /** Returns whether this data entry has any kind of filter. */
  public boolean isFiltered() {
    return filter != null
        || apkFilter != null
        || aabFilter != null
        || jarFilter != null
        || aarFilter != null
        || warFilter != null
        || earFilter != null
        || jmodFilter != null
        || zipFilter != null;
  }

  /** Returns the name filter that is applied to bottom-level files in this entry. */
  public List<String> getFilter() {
    return filter;
  }

  /** Sets the name filter that is applied to bottom-level files in this entry. */
  public void setFilter(List<String> filter) {
    this.filter = filter == null || filter.size() == 0 ? null : filter;
  }

  /** Returns the name filter that is applied to apk files in this entry, if any. */
  public List<String> getApkFilter() {
    return apkFilter;
  }

  /** Sets the name filter that is applied to apk files in this entry, if any. */
  public void setApkFilter(List<String> filter) {
    this.apkFilter = filter == null || filter.size() == 0 ? null : filter;
  }

  /** Returns the name filter that is applied to aab files in this entry, if any. */
  public List<String> getAabFilter() {
    return aabFilter;
  }

  /** Sets the name filter that is applied to aab files in this entry, if any. */
  public void setAabFilter(List<String> filter) {
    this.aabFilter = filter == null || filter.size() == 0 ? null : filter;
  }

  /** Returns the name filter that is applied to jar files in this entry, if any. */
  public List<String> getJarFilter() {
    return jarFilter;
  }

  /** Sets the name filter that is applied to jar files in this entry, if any. */
  public void setJarFilter(List<String> filter) {
    this.jarFilter = filter == null || filter.size() == 0 ? null : filter;
  }

  /** Returns the name filter that is applied to aar files in this entry, if any. */
  public List<String> getAarFilter() {
    return aarFilter;
  }

  /** Sets the name filter that is applied to aar files in this entry, if any. */
  public void setAarFilter(List<String> filter) {
    this.aarFilter = filter == null || filter.size() == 0 ? null : filter;
  }

  /** Returns the name filter that is applied to war files in this entry, if any. */
  public List<String> getWarFilter() {
    return warFilter;
  }

  /** Sets the name filter that is applied to war files in this entry, if any. */
  public void setWarFilter(List<String> filter) {
    this.warFilter = filter == null || filter.size() == 0 ? null : filter;
  }

  /** Returns the name filter that is applied to ear files in this entry, if any. */
  public List<String> getEarFilter() {
    return earFilter;
  }

  /** Sets the name filter that is applied to ear files in this entry, if any. */
  public void setEarFilter(List<String> filter) {
    this.earFilter = filter == null || filter.size() == 0 ? null : filter;
  }

  /** Returns the name filter that is applied to jmod files in this entry, if any. */
  public List<String> getJmodFilter() {
    return jmodFilter;
  }

  /** Sets the name filter that is applied to jmod files in this entry, if any. */
  public void setJmodFilter(List<String> filter) {
    this.jmodFilter = filter == null || filter.size() == 0 ? null : filter;
  }

  /** Returns the name filter that is applied to zip files in this entry, if any. */
  public List<String> getZipFilter() {
    return zipFilter;
  }

  /** Sets the name filter that is applied to zip files in this entry, if any. */
  public void setZipFilter(List<String> filter) {
    this.zipFilter = filter == null || filter.size() == 0 ? null : filter;
  }

  // Implementations for Object.

  public String toString() {
    String string = getName();

    if (filter != null
        || apkFilter != null
        || aabFilter != null
        || jarFilter != null
        || aarFilter != null
        || warFilter != null
        || earFilter != null
        || jmodFilter != null
        || zipFilter != null) {
      string +=
          "("
              + (aarFilter != null ? ListUtil.commaSeparatedString(aarFilter, true) : "")
              + ";"
              + (aabFilter != null ? ListUtil.commaSeparatedString(aabFilter, true) : "")
              + ";"
              + (apkFilter != null ? ListUtil.commaSeparatedString(apkFilter, true) : "")
              + ";"
              + (zipFilter != null ? ListUtil.commaSeparatedString(zipFilter, true) : "")
              + ";"
              + (jmodFilter != null ? ListUtil.commaSeparatedString(jmodFilter, true) : "")
              + ";"
              + (earFilter != null ? ListUtil.commaSeparatedString(earFilter, true) : "")
              + ";"
              + (warFilter != null ? ListUtil.commaSeparatedString(warFilter, true) : "")
              + ";"
              + (jarFilter != null ? ListUtil.commaSeparatedString(jarFilter, true) : "")
              + ";"
              + (filter != null ? ListUtil.commaSeparatedString(filter, true) : "")
              + ")";
    }

    return string;
  }
}
