/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.resources.kotlinmodule.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;

/**
 * @author James Hamilton
 */
public class KotlinMetadataToModuleVisitor
implements KotlinMetadataVisitor
{
    private final KotlinModuleVisitor kotlinModuleVisitor;

    public KotlinMetadataToModuleVisitor(KotlinModuleVisitor kotlinModuleVisitor)
    {
        this.kotlinModuleVisitor = kotlinModuleVisitor;
    }

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    @Override
    public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
    {
        kotlinDeclarationContainerMetadata.moduleAccept(kotlinModuleVisitor);
    }
}
