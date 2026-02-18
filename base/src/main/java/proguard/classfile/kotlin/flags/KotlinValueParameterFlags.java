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
package proguard.classfile.kotlin.flags;

/**
 * Flags for Kotlin value parameters.
 *
 * <p>No valid visibility or modality flags.
 */
public class KotlinValueParameterFlags implements KotlinFlags {

  /**
   * Signifies that the corresponding value parameter declares a default value. Note that the
   * default value itself can be a complex expression and is not available via metadata. Also note
   * that in case of an override of a parameter with default value, the parameter in the derived
   * method does _not_ declare the default value ([DECLARES_DEFAULT_VALUE] == false), but the
   * parameter is still optional at the call site because the default value from the base method is
   * used.
   */
  public boolean hasDefaultValue;

  /** Signifies that the corresponding value parameter is `crossinline`. */
  public boolean isCrossInline;

  /** Signifies that the corresponding value parameter is `noinline`. */
  public boolean isNoInline;

  // JVM Specific flags

  /**
   * Indicates that the corresponding value parameter has at least one annotation in the JVM
   * bytecode.
   *
   * <p>Before annotations in metadata are enabled by default in the Kotlin compiler
   * (https://youtrack.jetbrains.com/issue/KT-75736), annotations are only generated in the JVM
   * bytecode. The compiler writes and reads this flag to metadata as an optimization, to avoid
   * parsing class file one additional time when it's not needed.
   *
   * <p>Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are
   * written to the class files.
   */
  public boolean hasAnnotationsInBytecode;

  @Deprecated public boolean hasAnnotations;
}
