/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.resources.kotlinmodule.util;

import proguard.classfile.*;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.*;
import proguard.resources.file.ResourceFile;
import proguard.resources.file.visitor.*;
import proguard.resources.kotlinmodule.*;
import proguard.resources.kotlinmodule.visitor.KotlinModulePackageVisitor;


/**
 * Initialize the Kotlin module references.
 *
 * @author James Hamilton
 */
public class KotlinModuleReferenceInitializer
implements   ResourceFileVisitor,
             KotlinModulePackageVisitor
{
    private final ClassPool             programClassPool;
    private final ClassPool             libraryClassPool;
    private final KotlinReferenceFinder finder = new KotlinReferenceFinder();

    public KotlinModuleReferenceInitializer(ClassPool programClassPool, ClassPool libraryClassPool)
    {
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;
    }


    // Implementations for ResourceFileVisitor.

    @Override
    public void visitKotlinModule(KotlinModule kotlinModule)
    {
        kotlinModule.modulePackagesAccept(this);
    }


    // Implementations for KotlinModulePartVisitor.

    @Override
    public void visitKotlinModulePackage(KotlinModule kotlinModule, KotlinModulePackage modulePackage)
    {
        for (int i = 0; i < modulePackage.fileFacadeNames.size(); i++)
        {
            Clazz clazz = programClassPool.getClass(modulePackage.fileFacadeNames.get(i));
            if (clazz != null)
            {
                clazz.kotlinMetadataAccept(finder.reset());
                KotlinFileFacadeKindMetadata fileFacadeKindMetadata = finder.fileFacadeKindMetadata;
                if (fileFacadeKindMetadata != null)
                {
                    fileFacadeKindMetadata.referencedModule = kotlinModule;
                    modulePackage.referencedFileFacades.set(i, fileFacadeKindMetadata);
                }
            }
        }

        modulePackage.multiFileClassParts.forEach((multiFilePartName, multiFileFacadeName) -> {
            Clazz multiFilePartClass = programClassPool.getClass(multiFilePartName);
            if (multiFilePartClass != null)
            {
                multiFilePartClass.kotlinMetadataAccept(finder.reset());
                KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata = finder.kotlinMultiFilePartKindMetadata;
                if (kotlinMultiFilePartKindMetadata != null)
                {
                    kotlinMultiFilePartKindMetadata.referencedModule = kotlinModule;
                    modulePackage.referencedMultiFileParts.put(multiFilePartName, kotlinMultiFilePartKindMetadata);
                }
            }
        });
    }

    // Helper class.

    private static class KotlinReferenceFinder
    implements           KotlinMetadataVisitor
    {
        KotlinFileFacadeKindMetadata    fileFacadeKindMetadata;
        KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata;

        public KotlinReferenceFinder reset()
        {
            this.fileFacadeKindMetadata          = null;
            this.kotlinMultiFilePartKindMetadata = null;
            return this;
        }

        // Implementations for KotlinMetadataVisitor.

        @Override
        public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

        @Override
        public void visitKotlinFileFacadeMetadata(Clazz                        clazz,
                                                  KotlinFileFacadeKindMetadata kotlinFileFacadeKindMetadata)
        {
            this.fileFacadeKindMetadata = kotlinFileFacadeKindMetadata;
        }

        @Override
        public void visitKotlinMultiFilePartMetadata(Clazz                           clazz,
                                                     KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata)
        {
            this.kotlinMultiFilePartKindMetadata = kotlinMultiFilePartKindMetadata;
        }
    }
}
