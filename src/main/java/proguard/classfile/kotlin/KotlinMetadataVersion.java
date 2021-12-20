/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.kotlin;

/**
 * Small container for KotlinMetadataVersion metadata (mv).
 */
public class KotlinMetadataVersion
{
    public final int major;
    public final int minor;
    public final int patch;

    public KotlinMetadataVersion(int[] version)
    {
        this(version[0], version[1], version[2]);
    }

    public KotlinMetadataVersion(int major, int minor)
    {
        this(major, minor, -1);
    }

    public KotlinMetadataVersion(int major, int minor, int patch)
    {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public String toString()
    {
        return this.major + "." + this.minor + (this.patch != -1 ? "." + this.patch : "");
    }
}
