/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */
package proguard.classfile.kotlin;

import proguard.classfile.*;
import proguard.classfile.kotlin.reflect.*;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;
import proguard.classfile.kotlin.visitor.*;

import java.util.List;

import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_SYNTHETIC_CLASS;

public class KotlinSyntheticClassKindMetadata
extends KotlinMetadata
{
    public List<KotlinFunctionMetadata> functions;

    // For CallableReferences, the synthetic class will implement CallableReference.
    public CallableReferenceInfo callableReferenceInfo;

    public final Flavor flavor;

    public enum Flavor
    {
        REGULAR,
        LAMBDA,
        DEFAULT_IMPLS,
        WHEN_MAPPINGS
    }

    public KotlinSyntheticClassKindMetadata(int[]  mv,
                                            int[]  bv,
                                            int    xi,
                                            String xs,
                                            String pn,
                                            Flavor flavor)
    {
        super(METADATA_KIND_SYNTHETIC_CLASS, mv, bv, xi, xs, pn);
        this.flavor = flavor;
    }


    @Override
    public void accept(Clazz clazz, KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        kotlinMetadataVisitor.visitKotlinSyntheticClassMetadata(clazz, this);
    }


    public void functionsAccept(Clazz clazz, KotlinFunctionVisitor kotlinFunctionVisitor)
    {
        for (KotlinFunctionMetadata function : functions)
        {
            function.accept(clazz, this, kotlinFunctionVisitor);
        }
    }

    
    public void callableReferenceInfoAccept(CallableReferenceInfoVisitor callableReferenceInfoVisitor)
    {
        if (this.callableReferenceInfo != null)
        {
            this.callableReferenceInfo.accept(callableReferenceInfoVisitor);
        }
    }


    // Implementations for Object.
    @Override
    public String toString()
    {
        String functionName = functions.size() > 0 ? functions.get(0).name : "//";
        return "Kotlin synthetic class(" + functionName + ")";
    }
}
