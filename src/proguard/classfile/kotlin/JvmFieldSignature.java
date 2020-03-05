/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.classfile.kotlin;

import java.util.Objects;

/**
 *
 * @author james
 */
public class JvmFieldSignature
{
    private final String name;
    private final String desc;

    public JvmFieldSignature(String name, String desc)
    {
        this.name = name;
        this.desc = desc;
    }

    public String getName()
    {
        return name;
    }

    public String getDesc()
    {
        return desc;
    }

    public String asString()
    {
        return name + desc;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        JvmFieldSignature that = (JvmFieldSignature)o;
        return name.equals(that.name) &&
               desc.equals(that.desc);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, desc);
    }

    @Override
    public String toString()
    {
        return name + desc;
    }

    // Helper methods to convert from/to Kotlin metadata classes.

    public static JvmFieldSignature fromKotlinJvmFieldSignature(kotlinx.metadata.jvm.JvmFieldSignature jvmFieldSignature)
    {
        if (jvmFieldSignature == null)
        {
            return null;
        }

        return new JvmFieldSignature(jvmFieldSignature.getName(), jvmFieldSignature.getDesc());
    }

    public static kotlinx.metadata.jvm.JvmFieldSignature toKotlinJvmFieldSignature(JvmFieldSignature jvmFieldSignature)
    {
        if (jvmFieldSignature == null)
        {
            return null;
        }

        return new kotlinx.metadata.jvm.JvmFieldSignature(jvmFieldSignature.name, jvmFieldSignature.desc);
    }
}
