/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.resources.kotlinmodule;

import proguard.classfile.kotlin.*;
import proguard.resources.kotlinmodule.visitor.KotlinModulePackageVisitor;

import java.util.*;

/**
 * @author James Hamilton
 */
public class KotlinModulePackage
{
    public String                    fqName;
    public final List<String>        fileFacadeNames;
    public final Map<String, String> multiFileClassParts;

    public final List<KotlinFileFacadeKindMetadata>           referencedFileFacades;
    public final Map<String, KotlinMultiFilePartKindMetadata> referencedMultiFileParts;

    public KotlinModulePackage(String fqName, List<String> fileFacadeNames, Map<String, String> multiFileClassParts)
    {
        this.fqName                   = fqName;
        this.fileFacadeNames          = fileFacadeNames;
        this.multiFileClassParts      = multiFileClassParts;
        this.referencedFileFacades    = new ArrayList<>(Collections.nCopies(fileFacadeNames.size(), null));
        this.referencedMultiFileParts = new HashMap<>();
    }

    public void accept(KotlinModule kotlinModule, KotlinModulePackageVisitor kotlinModulePartVisitor)
    {
        kotlinModulePartVisitor.visitKotlinModulePackage(kotlinModule, this);
    }
}
