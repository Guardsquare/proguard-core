/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.kotlin;

/** Small container for KotlinMetadataVersion metadata (mv). */
public class KotlinMetadataVersion {
  public final int major;
  public final int minor;
  public final int patch;

  public static final KotlinMetadataVersion UNKNOWN_VERSION =
      new KotlinMetadataVersion(new int[] {-1, -1, 0});

  public KotlinMetadataVersion(int[] version) {
    this(version[0], version[1], version.length == 2 ? 0 : version[2]);
  }

  public KotlinMetadataVersion(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  /**
   * @return true iff we support writing this version of Kotlin Metadata.
   */
  public boolean canBeWritten() {
    // The Kotlin metadata library v0.4.1 supports writing versions 1.4 and later.
    // This version may change for future versions of the library!
    return this != UNKNOWN_VERSION && (this.major == 1 && this.minor >= 4 || this.major == 2);
  }

  public String toString() {
    return this.major + "." + this.minor + "." + this.patch;
  }

  public int[] toArray() {
    return new int[] {major, minor, patch};
  }
}
