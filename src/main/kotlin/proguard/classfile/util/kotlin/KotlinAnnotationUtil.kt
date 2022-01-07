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
package proguard.classfile.util.kotlin

import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument
import kotlinx.metadata.KmAnnotationArgument.AnnotationValue
import kotlinx.metadata.KmAnnotationArgument.ArrayValue
import kotlinx.metadata.KmAnnotationArgument.BooleanValue
import kotlinx.metadata.KmAnnotationArgument.ByteValue
import kotlinx.metadata.KmAnnotationArgument.CharValue
import kotlinx.metadata.KmAnnotationArgument.DoubleValue
import kotlinx.metadata.KmAnnotationArgument.EnumValue
import kotlinx.metadata.KmAnnotationArgument.FloatValue
import kotlinx.metadata.KmAnnotationArgument.IntValue
import kotlinx.metadata.KmAnnotationArgument.KClassValue
import kotlinx.metadata.KmAnnotationArgument.LongValue
import kotlinx.metadata.KmAnnotationArgument.ShortValue
import kotlinx.metadata.KmAnnotationArgument.StringValue
import kotlinx.metadata.KmAnnotationArgument.UByteValue
import kotlinx.metadata.KmAnnotationArgument.UIntValue
import kotlinx.metadata.KmAnnotationArgument.ULongValue
import kotlinx.metadata.KmAnnotationArgument.UShortValue
import proguard.classfile.Clazz
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import java.util.function.BiConsumer
import java.util.function.Consumer
import proguard.classfile.kotlin.KotlinAnnotatable as ProGuardKotlinAnnotatable
import proguard.classfile.kotlin.KotlinAnnotation as ProGuardKotlinAnnotation
import proguard.classfile.kotlin.KotlinAnnotationArgument as ProGuardAnnotationArgument
import proguard.classfile.kotlin.KotlinAnnotationArgument.AnnotationValue as ProGuardAnnotationValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.ArrayValue as ProGuardArrayValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.BooleanValue as ProGuardBooleanValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.ByteValue as ProGuardByteValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.CharValue as ProGuardCharValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.ClassValue as ProGuardClassValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.DoubleValue as ProGuardDoubleValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.EnumValue as ProGuardEnumValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.FloatValue as ProGuardFloatValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.IntValue as ProGuardIntValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.LongValue as ProGuardLongValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.ShortValue as ProGuardShortValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.StringValue as ProGuardStringValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.UByteValue as ProGuardUByteValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.UIntValue as ProGuardUIntValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.ULongValue as ProGuardULongValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.UShortValue as ProGuardUShortValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.Value as ProGuardKotlinAnnotationArgumentValue

// Helper methods to convert between kotlinx metadata annotations and ProGuardCORE model Kotlin annotations

@ExperimentalUnsignedTypes
fun convertAnnotation(kmAnnotation: KmAnnotation): ProGuardKotlinAnnotation = kmAnnotation.toProGuardKotlinAnnotation()

@ExperimentalUnsignedTypes
private fun KmAnnotation.toProGuardKotlinAnnotation(): ProGuardKotlinAnnotation =
    ProGuardKotlinAnnotation(
        className,
        arguments.map {
            (key, value) ->
            ProGuardAnnotationArgument(key, value.toProGuardKotlinAnnotationArgumentValue())
        }
    )

@ExperimentalUnsignedTypes
private fun KmAnnotationArgument.toProGuardKotlinAnnotationArgumentValue(): ProGuardKotlinAnnotationArgumentValue =
    when (this) {
        is ByteValue -> ProGuardByteValue(value)
        is CharValue -> ProGuardCharValue(value)
        is ShortValue -> ProGuardShortValue(value)
        is IntValue -> ProGuardIntValue(value)
        is LongValue -> ProGuardLongValue(value)
        is FloatValue -> ProGuardFloatValue(value)
        is DoubleValue -> ProGuardDoubleValue(value)
        is BooleanValue -> ProGuardBooleanValue(value)
        is UByteValue -> ProGuardUByteValue(value.toByte())
        is UShortValue -> ProGuardUShortValue(value.toShort())
        is UIntValue -> ProGuardUIntValue(value.toInt())
        is ULongValue -> ProGuardULongValue(value.toLong())
        is StringValue -> ProGuardStringValue(value)
        is KClassValue -> ProGuardClassValue(className, arrayDimensionCount)
        is EnumValue -> ProGuardEnumValue(enumClassName, enumEntryName)
        is AnnotationValue -> ProGuardAnnotationValue(annotation.toProGuardKotlinAnnotation())
        is ArrayValue -> ProGuardArrayValue(elements.map { it.toProGuardKotlinAnnotationArgumentValue() })
    }

@ExperimentalUnsignedTypes
class AnnotationConstructor(private val consumer: Consumer<KmAnnotation>) : KotlinAnnotationVisitor {
    override fun visitAnyAnnotation(clazz: Clazz, annotatable: ProGuardKotlinAnnotatable, annotation: ProGuardKotlinAnnotation) =
        with(mutableMapOf<String, KmAnnotationArgument>()) {
            // collect the arguments in a Map
            annotation.argumentsAccept(
                clazz,
                annotatable,
                AnnotationArgumentConstructor { key, value -> this[key] = value }
            )
            // Create the KmAnnotation with the arguments
            consumer.accept(KmAnnotation(annotation.className, this))
        }
}

@ExperimentalUnsignedTypes
private class AnnotationArgumentConstructor(private val consumer: BiConsumer<String, KmAnnotationArgument>) : KotlinAnnotationArgumentVisitor {

    override fun visitAnyArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardKotlinAnnotationArgumentValue
    ) { }

    override fun visitUByteArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardUByteValue
    ) = consumer.accept(argument.name, UByteValue(value.value.toUByte()))

    override fun visitUIntArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardUIntValue
    ) = consumer.accept(argument.name, UIntValue(value.value.toUInt()))

    override fun visitULongArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardULongValue
    ) = consumer.accept(argument.name, ULongValue(value.value.toULong()))

    override fun visitUShortArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardUShortValue
    ) = consumer.accept(argument.name, UShortValue(value.value.toUShort()))

    override fun visitByteArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardByteValue
    ) = consumer.accept(argument.name, ByteValue(value.value))

    override fun visitCharArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardCharValue
    ) = consumer.accept(argument.name, CharValue(value.value))

    override fun visitShortArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardShortValue
    ) = consumer.accept(argument.name, ShortValue(value.value))

    override fun visitIntArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardIntValue
    ) = consumer.accept(argument.name, IntValue(value.value))

    override fun visitLongArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardLongValue
    ) = consumer.accept(argument.name, LongValue(value.value))

    override fun visitFloatArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardFloatValue
    ) = consumer.accept(argument.name, FloatValue(value.value))

    override fun visitDoubleArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardDoubleValue
    ) = consumer.accept(argument.name, DoubleValue(value.value))

    override fun visitBooleanArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardBooleanValue
    ) = consumer.accept(argument.name, BooleanValue(value.value))

    override fun visitStringArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardStringValue
    ) = consumer.accept(argument.name, StringValue(value.value))

    override fun visitClassArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardClassValue
    ) = consumer.accept(argument.name, KClassValue(value.className, value.arrayDimensionsCount))

    override fun visitEnumArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardEnumValue
    ) = consumer.accept(argument.name, EnumValue(value.className, value.enumEntryName))

    override fun visitAnnotationArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardAnnotationValue
    ) = value.annotationAccept(
        clazz,
        annotatable,
        AnnotationConstructor { consumer.accept(argument.name, AnnotationValue(it)) }
    )

    override fun visitArrayArgument(
        clazz: Clazz,
        annotatable: ProGuardKotlinAnnotatable,
        annotation: ProGuardKotlinAnnotation,
        argument: ProGuardAnnotationArgument,
        value: ProGuardArrayValue
    ) = with(mutableListOf<KmAnnotationArgument>()) {
        // Collect the elements
        value.elementsAccept(
            clazz,
            annotatable,
            annotation,
            argument,
            AnnotationArgumentConstructor { _, element -> add(element) }
        )
        // Create the ArrayValue with the elements
        consumer.accept(argument.name, ArrayValue(this))
    }
}
