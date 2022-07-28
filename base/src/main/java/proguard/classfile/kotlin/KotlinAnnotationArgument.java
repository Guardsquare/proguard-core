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

package proguard.classfile.kotlin;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor;
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;
import proguard.util.Processable;
import proguard.util.SimpleProcessable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static proguard.classfile.util.ClassUtil.*;

/**
 * Represents an argument of a {@link KotlinAnnotation} e.g.
 *
 * <code>
 * @Annotation(arg1 = "String", arg2 = 1)
 * </code>
 *
 * An argument consists of a name and a value:
 *
 * - The name which references a Java annotation method.
 * - The value is an instance of sub-type of {@link Value}.
 *
 * @author James Hamilton
 */
public class KotlinAnnotationArgument
extends      SimpleProcessable
implements   Processable
{
    public String name;

    // The name of the argument corresponds to a Java annotation method.
    public Clazz  referencedAnnotationMethodClass;
    public Method referencedAnnotationMethod;

    private final Value value;


    public KotlinAnnotationArgument(String name, Value value)
    {
        this.name  = name;
        this.value = value;
    }


    public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation, KotlinAnnotationArgumentVisitor visitor)
    {
        // Delegate to the value, to call the correct argument type visitor
        this.value.accept(clazz, annotatable, annotation, this, visitor);
    }


    public void referencedMethodAccept(MemberVisitor methodVisitor)
    {
        if (this.referencedAnnotationMethod != null)
        {
            this.referencedAnnotationMethod.accept(this.referencedAnnotationMethodClass, methodVisitor);
        }
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KotlinAnnotationArgument that = (KotlinAnnotationArgument) o;
        return name.equals(that.name) && value.equals(that.value);
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(name, value);
    }


    @Override
    public String toString()
    {
        return this.name + " = " + this.value;
    }


    // Argument value types

    public interface Value
    {
        void accept(
                Clazz clazz,
                KotlinAnnotatable annotatable,
                KotlinAnnotation annotation,
                KotlinAnnotationArgument argument,
                KotlinAnnotationArgumentVisitor visitor
        );
    }


    public static class LiteralValue<T> implements Value
    {
        public final T value;


        public LiteralValue(T value)
        {
            this.value = value;
        }


        @Override
        public int hashCode()
        {
            return this.value.hashCode();
        }


        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LiteralValue<?> that = (LiteralValue<?>) o;
            return value.equals(that.value);
        }


        @Override
        public String toString()
        {
            return value.toString();
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitAnyLiteralArgument(clazz, annotatable, annotation, argument, this);
        }
    }


    public static final class ByteValue extends LiteralValue<Byte>
    {
        public ByteValue(byte value)
        {
            super(value);
        }

        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitByteArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }
    }


    public static final class CharValue extends LiteralValue<Character>
    {
        public CharValue(char value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitCharArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }
    }


    public static final class ShortValue extends LiteralValue<Short>
    {
        public ShortValue(short value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitShortArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }
    }


    public static final class IntValue extends LiteralValue<Integer>
    {
        public IntValue(int value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitIntArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }
    }


    public static final class LongValue extends LiteralValue<Long>
    {
        public LongValue(long value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitLongArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }
    }


    public static final class FloatValue extends LiteralValue<Float>
    {
        public FloatValue(float value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitFloatArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }
    }


    public static final class DoubleValue extends LiteralValue<Double>
    {
        public DoubleValue(double value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitDoubleArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }
    }


    public static final class BooleanValue extends LiteralValue<Boolean>
    {
        public BooleanValue(boolean value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitBooleanArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }
    }


    public static final class UByteValue extends LiteralValue<Byte>
    {
        public UByteValue(byte value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitUByteArgument(clazz, annotatable, annotation, argument, this);
        }
    }


    public static final class UShortValue extends LiteralValue<Short>
    {
        public UShortValue(short value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitUShortArgument(clazz, annotatable, annotation, argument, this);
        }
    }


    public static final class UIntValue extends LiteralValue<Integer>
    {
        public UIntValue(int value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitUIntArgument(clazz, annotatable, annotation, argument, this);
        }
    }


    public static final class ULongValue extends LiteralValue<Long>
    {
        public ULongValue(long value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitULongArgument(clazz, annotatable, annotation, argument, this);
        }
    }


    public static final class StringValue extends LiteralValue<String>
    {
        public StringValue(String value)
        {
            super(value);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitStringArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }
    }


    public static final class ClassValue implements Value
    {
        public String className;
        public Clazz  referencedClass;
        // TODO(T5406): arrayDimensionsCount not yet in use
        public int    arrayDimensionsCount;

        public ClassValue(String className)
        {
            this(className, 0);
        }

        public ClassValue(String className, int arrayDimensionsCount)
        {
            this.className            = className;
            this.arrayDimensionsCount = arrayDimensionsCount;
        }


        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassValue that = (ClassValue) o;
            return arrayDimensionsCount == that.arrayDimensionsCount && className.equals(that.className);
        }


        @Override
        public int hashCode()
        {
            return Objects.hash(className, arrayDimensionsCount);
        }


        @Override
        public String toString()
        {
            return String.join("", nCopies(this.arrayDimensionsCount, "[")) + externalClassName(this.className);
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitClassArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }


        public void referencedClassAccept(ClassVisitor visitor)
        {
            if (referencedClass != null)
            {
                this.referencedClass.accept(visitor);
            }
        }
    }


    public static final class EnumValue implements Value
    {
        public String className;
        public Clazz  referencedClass;
        public String enumEntryName;


        public EnumValue(String className, String enumEntryName)
        {
            this.className = className;
            this.enumEntryName = enumEntryName;
        }


        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EnumValue enumValue = (EnumValue) o;
            return className.equals(enumValue.className) && enumEntryName.equals(enumValue.enumEntryName);
        }


        @Override
        public int hashCode()
        {
            return Objects.hash(className, enumEntryName);
        }


        @Override
        public String toString()
        {
            return externalClassName(this.className) + "." + this.enumEntryName;
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitEnumArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }


        public void referencedClassAccept(ClassVisitor visitor)
        {
            if (this.referencedClass != null)
            {
                this.referencedClass.accept(visitor);
            }
        }
    }


    public static final class AnnotationValue implements Value
    {
        public KotlinAnnotation kotlinMetadataAnnotation;


        public AnnotationValue(KotlinAnnotation kotlinMetadataAnnotation)
        {
            this.kotlinMetadataAnnotation = kotlinMetadataAnnotation;
        }


        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnnotationValue that = (AnnotationValue) o;
            return kotlinMetadataAnnotation.equals(that.kotlinMetadataAnnotation);
        }


        @Override
        public int hashCode()
        {
            return Objects.hash(kotlinMetadataAnnotation);
        }


        @Override
        public String toString()
        {
            return this.kotlinMetadataAnnotation.toString();
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitAnnotationArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }


        public void annotationAccept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotationVisitor visitor)
        {
            this.kotlinMetadataAnnotation.accept(clazz, annotatable, visitor);
        }
    }


    public static final class ArrayValue implements Value
    {
        public List<? extends Value> elements;

        public ArrayValue(List<? extends Value> elements)
        {
            this.elements = elements;
        }


        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArrayValue that = (ArrayValue) o;
            return elements.equals(that.elements);
        }


        @Override
        public int hashCode()
        {
            return Objects.hash(elements);
        }


        @Override
        public String toString()
        {
            return elements.stream()
                        .map(Objects::toString)
                        .collect(Collectors.joining(", ", "[", "]"));
        }


        @Override
        public void accept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinMetadataAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
            visitor.visitArrayArgument(clazz, annotatable, kotlinMetadataAnnotation, argument, this);
        }


        public void elementsAccept(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation kotlinAnnotation, KotlinAnnotationArgument argument, KotlinAnnotationArgumentVisitor visitor)
        {
           this.elements.forEach(element -> element.accept(clazz, annotatable, kotlinAnnotation, argument, visitor));
        }
    }
}
