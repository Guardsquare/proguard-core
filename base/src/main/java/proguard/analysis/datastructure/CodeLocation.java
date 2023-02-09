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

package proguard.analysis.datastructure;

import static proguard.classfile.util.ClassUtil.externalClassName;

import java.util.Objects;
import proguard.classfile.Clazz;
import proguard.classfile.Member;
import proguard.classfile.Signature;

/**
 * Represents a unique location in the bytecode.
 * It comprises the {@link Clazz} and {@link Member}
 * where it is contained, the offset therein and the
 * corresponding line number in the source file
 * (may be -1 if it is unknown).
 * Consider the following pseudo-bytecode example
 * which contains code location comments:
 *
 * <pre>
 *     {@code
 *     public class Test
 *     {
 *         // class "Test", member "field", line 3, offset 0
 *         public String field;
 *
 *         public String toString()
 *         {
 *             // class "Test", member "toString", line 6, offset 0
 *             aload_0
 *             // class "Test", member "toString", line 6, offset 1
 *             getfield #1 &lt;Test.field : Ljava/lang/String;&gt;
 *             // class "Test", member "toString", line 6, offset 4
 *             areturn
 *         }
 *     }
 *     }
 * </pre>
 *
 * <ul>
 *     <li>
 *         <b>Inside methods:</b> Like each location, instructions
 *         inside methods have a line number. But as there may be
 *         several expressions on the same line, to correctly identify
 *         each instruction we also need their bytecode offset.
 *         E.g. the {@code getfield} instruction inside {@code toString()}
 *         has the offset 1.
 *     </li>
 *     <li>
 *         <b>Fields:</b> In this case {@code Test#field}.
 *         A field location has a line number (3 in this example)
 *         but no offset, as this concept is only applicable to
 *         methods.
 *     </li>
 * </ul>
 *
 * @author Samuel Hopstock
 */
public class CodeLocation
extends      Location
{

    public final Clazz     clazz;
    public final Member    member;
    public final int       offset;
    public final Signature signature;

    public CodeLocation(Clazz clazz, Member member, int offset, int line)
    {
        super(line);
        this.clazz     = clazz;
        this.member    = member;
        this.offset    = offset;
        this.signature = Signature.of(clazz, member);
    }

    public String getExternalClassName()
    {
        return externalClassName(clazz.getName());
    }

    public String getMemberName()
    {
        return member == null ? null : member.getName(clazz);
    }

    @Override
    public String getName()
    {
        return signature.getFqn();
    }

    @Override
    public String toString()
    {
        return signature + "+" + String.format("%04d", offset) + " (line " + line + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof CodeLocation))
        {
            return false;
        }
        CodeLocation codeLocation = (CodeLocation) o;
        return Objects.equals(signature, codeLocation.signature)
               && offset == codeLocation.offset;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(signature, offset);
    }

    @Override
    public int compareTo(Location o)
    {
        if (!o.getClass().equals(getClass()))
        {
            return -1;
        }
        CodeLocation other = (CodeLocation) o;
        // Locations are ordered first by member name (lexicographically
        // increasing), then increasing line number and lastly increasing offset
        if (signature.equals(other.signature))
        {
            if (line == other.line)
            {
                return offset - other.offset;
            }
            return line - other.line;
        }
        return signature.compareTo(other.signature);
    }
}
