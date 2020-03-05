/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */

package proguard.classfile.kotlin.reflect;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;

import static proguard.classfile.util.kotlin.KotlinNameUtil.generateGetterName;

/**
 * Property reference info.
 *
 * @author James Hamilton
 */
public class PropertyReferenceInfo
implements   CallableReferenceInfo
{

    private final Clazz                              ownerClass;
    private final KotlinDeclarationContainerMetadata ownerMetadata;
    private final KotlinPropertyMetadata             propertyMetadata;

    public PropertyReferenceInfo(Clazz                              ownerClass,
                                 KotlinDeclarationContainerMetadata ownerMetadata,
                                 KotlinPropertyMetadata             propertyMetadata)
    {
        this.ownerClass       = ownerClass;
        this.propertyMetadata = propertyMetadata;
        this.ownerMetadata    = ownerMetadata;
    }

    @Override
    public String getName()
    {
        return propertyMetadata.name;
    }


    /**
     * For properties this is the signature of it's JVM getter method.
     *
     * If the property has no getter in the bytecode (e.g. private property in a class), it's still the signature of
     * the imaginary default getter that would be generated otherwise e.g. "myProperty" -> "getMyProperty".
     *
     * @return the signature.
     */
    @Override
    public String getSignature()
    {
        return propertyMetadata.getterSignature != null        ?
                   propertyMetadata.getterSignature.asString() :
                   generateGetterName(propertyMetadata.backingFieldSignature.getName()) +
                                      "()" + propertyMetadata.backingFieldSignature.getDesc();
    }


    @Override
    public KotlinDeclarationContainerMetadata getOwner()
    {
        return this.ownerMetadata;
    }

    @Override
    public void accept(CallableReferenceInfoVisitor callableReferenceInfoVisitor)
    {
        callableReferenceInfoVisitor.visitPropertyReferenceInfo(this);
    }

    @Override
    public void ownerAccept(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        this.ownerMetadata.accept(this.ownerClass, kotlinMetadataVisitor);
    }
}
