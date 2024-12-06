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
package proguard.classfile.util;

import java.util.HashMap;
import java.util.Map;
import proguard.classfile.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This {@link ClassVisitor} shares strings in the class files that it visits.
 *
 * @author Eric Lafortune
 */
public class StringSharer
    implements ClassVisitor,
        MemberVisitor,
        ConstantVisitor,
        AttributeVisitor,
        KotlinMetadataVisitor {
  // We share strings using a string pool to ensure that all duplicates are removed.
  private final Map<String, String> stringPool;

  public StringSharer() {
    stringPool = new HashMap<>();
  }

  public StringSharer(int initialStringPoolCapacity) {
    stringPool = new HashMap<>(initialStringPoolCapacity);
  }

  // Implementations for ClassVisitor.

  @Override
  public void visitAnyClass(Clazz clazz) {
    throw new UnsupportedOperationException(
        this.getClass().getName() + " does not support " + clazz.getClass().getName());
  }

  @Override
  public void visitProgramClass(ProgramClass programClass) {
    // Replace name strings in the constant pool by shared strings.
    programClass.constantPoolEntriesAccept(this);

    // Replace strings in Kotlin metadata.
    programClass.kotlinMetadataAccept(this);
  }

  @Override
  public void visitLibraryClass(LibraryClass libraryClass) {
    // Replace the super class name string with copies from the string pool.
    libraryClass.superClassName = getFromStringPool(libraryClass.superClassName);

    // Replace the interface name strings with copies from the string pool.
    if (libraryClass.interfaceNames != null) {
      String[] interfaceNames = libraryClass.interfaceNames;

      for (int index = 0; index < interfaceNames.length; index++) {
        interfaceNames[index] = getFromStringPool(interfaceNames[index]);
      }
    }

    // Share member names and descriptors.
    libraryClass.fieldsAccept(this);
    libraryClass.methodsAccept(this);

    // Replace strings in Kotlin metadata.
    libraryClass.kotlinMetadataAccept(this);
  }

  // Implementations for MemberVisitor.

  @Override
  public void visitLibraryMember(LibraryClass libraryClass, LibraryMember libraryMember) {
    // Replace the name and descriptor with copies from the string pool.
    libraryMember.name = getFromStringPool(libraryMember.name);
    libraryMember.descriptor = getFromStringPool(libraryMember.descriptor);
  }

  // Implementations for ConstantVisitor.

  @Override
  public void visitAnyConstant(Clazz clazz, Constant constant) {}

  @Override
  public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
    // Replace the string with a copy from the string pool.
    utf8Constant.setString(getFromStringPool(utf8Constant.getString()));
  }

  // Implementations for KotlinMetadataVisitor.

  @Override
  public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

  @Override
  public void visitKotlinClassMetadata(
      Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata) {
    kotlinClassKindMetadata.className = getFromStringPool(kotlinClassKindMetadata.className);
    kotlinClassKindMetadata.companionObjectName =
        getFromStringPool(kotlinClassKindMetadata.companionObjectName);
    kotlinClassKindMetadata.anonymousObjectOriginName =
        getFromStringPool(kotlinClassKindMetadata.anonymousObjectOriginName);
  }

  /**
   * Adds the given string to the string pool if it isn't already present, and returns a copy of the
   * string from the string pool.
   */
  private String getFromStringPool(String newString) {
    String existingString = stringPool.putIfAbsent(newString, newString);
    return existingString != null ? existingString : newString;
  }
}
