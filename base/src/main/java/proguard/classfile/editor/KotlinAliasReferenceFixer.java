/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2026 Guardsquare NV
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
package proguard.classfile.editor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinTypeAliasMetadata;
import proguard.classfile.kotlin.KotlinTypeMetadata;
import proguard.classfile.kotlin.visitor.AllTypeVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassVisitor;

/**
 * This {@link ClassVisitor} fixes the {@code aliasName} of Kotlin types that reference a type alias
 * whose declaration container has been renamed.
 *
 * <p>{@link ClassReferenceFixer} re-derives a type's {@code className} from its referenced class,
 * but it leaves {@code aliasName} alone, because {@code aliasName} is a plain string naming the
 * alias's <i>declaration site</i> rather than a reference that {@link ClassReferenceFixer}
 * resolves. Run this fixer over the program class pool <b>after</b> {@link ClassReferenceFixer} (so
 * the declaration containers already carry their new names) to keep those strings consistent with
 * the renamed declarations.
 *
 * <p>The recorded format matches what {@link proguard.classfile.util.ClassReferenceInitializer}
 * parses back: an alias declared in a class is {@code <declaring class>.<simple name>}; one
 * declared in a file facade (or multi-file part) is {@code <facade package>/<simple name>}. Both
 * pieces are read from the post-rename declaration container, so the rewrite holds under any
 * renaming, including a move to another package. An unresolved reference (no referenced type alias)
 * is left untouched, mirroring how {@link ClassReferenceFixer} skips unresolved references.
 *
 * @see ClassReferenceFixer
 */
public class KotlinAliasReferenceFixer implements ClassVisitor, KotlinTypeVisitor {

  // Implementations for ClassVisitor.

  @Override
  public void visitAnyClass(Clazz clazz) {
    clazz.kotlinMetadataAccept(new AllTypeVisitor(this));
  }

  // Implementations for KotlinTypeVisitor.

  @Override
  public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata) {
    if (kotlinTypeMetadata.aliasName == null) {
      return;
    }

    KotlinTypeAliasMetadata referencedTypeAlias = kotlinTypeMetadata.referencedTypeAlias;
    if (referencedTypeAlias == null || referencedTypeAlias.referencedDeclarationContainer == null) {
      return;
    }

    KotlinDeclarationContainerMetadata container =
        referencedTypeAlias.referencedDeclarationContainer;

    kotlinTypeMetadata.aliasName =
        container instanceof KotlinClassKindMetadata
            ? ((KotlinClassKindMetadata) container).className + "." + referencedTypeAlias.name
            : ClassUtil.internalPackagePrefix(container.ownerClassName) + referencedTypeAlias.name;
  }
}
