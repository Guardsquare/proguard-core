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
package proguard.classfile.attribute.module;

import proguard.classfile.Clazz;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.module.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.visitor.ConstantVisitor;

/**
 * This {@link Attribute} represents a module attribute.
 *
 * @author Joachim Vandersmissen
 */
public class ModuleAttribute extends Attribute
{
    public int            u2moduleNameIndex;
    public int            u2moduleFlags;
    public int            u2moduleVersionIndex;
    public int            u2requiresCount;
    public RequiresInfo[] requires;
    public int            u2exportsCount;
    public ExportsInfo[]  exports;
    public int            u2opensCount;
    public OpensInfo[]    opens;
    public int            u2usesCount;
    public int[]          u2uses;
    public int            u2providesCount;
    public ProvidesInfo[] provides;


    /**
     * Creates an uninitialized ModuleAttribute.
     */
    public ModuleAttribute()
    {
    }


    /**
     * Creates an initialized ModuleAttribute.
     */
    public ModuleAttribute(int            u2attributeNameIndex,
                           int            u2moduleNameIndex,
                           int            u2moduleFlags,
                           int            u2moduleVersionIndex,
                           int            u2requiresCount,
                           RequiresInfo[] requires,
                           int            u2exportsCount,
                           ExportsInfo[]  exports,
                           int            u2opensCount,
                           OpensInfo[]    opens,
                           int            u2usesCount,
                           int[]          u2uses,
                           int            u2ProvidesCount,
                           ProvidesInfo[] provides)
    {
        super(u2attributeNameIndex);

        this.u2moduleNameIndex    = u2moduleNameIndex;
        this.u2moduleFlags        = u2moduleFlags;
        this.u2moduleVersionIndex = u2moduleVersionIndex;
        this.u2requiresCount      = u2requiresCount;
        this.requires             = requires;
        this.u2exportsCount       = u2exportsCount;
        this.exports              = exports;
        this.u2opensCount         = u2opensCount;
        this.opens                = opens;
        this.u2usesCount          = u2usesCount;
        this.u2uses               = u2uses;
        this.u2providesCount      = u2ProvidesCount;
        this.provides             = provides;
    }


    // Implementations for Attribute.

    public void accept(Clazz clazz, AttributeVisitor attributeVisitor)
    {
        attributeVisitor.visitModuleAttribute(clazz, this);
    }


    /**
     * Applies the given constant pool visitor to the Utf8 constant of the name,
     * if any.
     */
    public void nameAccept(Clazz clazz, ConstantVisitor constantVisitor)
    {
        if (u2moduleNameIndex != 0)
        {
            clazz.constantPoolEntryAccept(u2moduleNameIndex, constantVisitor);
        }
    }


    /**
     * Applies the given constant pool visitor to the Utf8 constant of the
     * version, if any.
     */
    public void versionAccept(Clazz clazz, ConstantVisitor constantVisitor)
    {
        if (u2moduleVersionIndex != 0)
        {
            clazz.constantPoolEntryAccept(u2moduleVersionIndex, constantVisitor);
        }
    }


    /**
     * Applies the given visitor to all requires.
     */
    public void requiresAccept(Clazz clazz, RequiresInfoVisitor requiresInfoVisitor)
    {
        for (int index = 0; index < u2requiresCount; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of RequiresInfo.
            requiresInfoVisitor.visitRequiresInfo(clazz, requires[index]);
        }
    }


    /**
     * Applies the given visitor to all exports.
     */
    public void exportsAccept(Clazz clazz, ExportsInfoVisitor exportsInfoVisitor)
    {
        for (int index = 0; index < u2exportsCount; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of ExportsInfo.
            exportsInfoVisitor.visitExportsInfo(clazz, exports[index]);
        }
    }


    /**
     * Applies the given visitor to all exports.
     */
    public void opensAccept(Clazz clazz, OpensInfoVisitor opensInfoVisitor)
    {
        for (int index = 0; index < u2opensCount; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of OpensInfo.
            opensInfoVisitor.visitOpensInfo(clazz, opens[index]);
        }
    }


    /**
     * Applies the given constant pool visitor to all uses.
     */
    public void usesAccept(Clazz clazz, ConstantVisitor constantVisitor)
    {
        for (int index = 0; index < u2usesCount; index++)
        {
            clazz.constantPoolEntryAccept(u2uses[index], constantVisitor);
        }
    }


    /**
     * Applies the given visitor to all provides.
     */
    public void providesAccept(Clazz clazz, ProvidesInfoVisitor providesInfoVisitor)
    {
        for (int index = 0; index < u2providesCount; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of ProvidesInfo.
            providesInfoVisitor.visitProvidesInfo(clazz, provides[index]);
        }
    }
}
