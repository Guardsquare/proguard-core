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
package proguard.resources.file.visitor;

import proguard.resources.file.ResourceFile;
import proguard.resources.kotlinmodule.KotlinModule;
import proguard.resources.kotlinmodule.visitor.KotlinModuleVisitor;

/**
 * This interface specifies the methods for a visitor of ResourceFile instances.
 *
 * @author Johan Leys
 */
public interface ResourceFileVisitor
extends          // ...Visitor,
                 KotlinModuleVisitor
{
    /**
     * Visits any ResourceFile instance. The more specific default implementations of
     * this interface delegate to this method.
     *
     * Unlike most other visitor interfaces, this default implementation is
     * empty, because most implementations only care about one type of resource
     * file.
     */
    default void visitAnyResourceFile(ResourceFile resourceFile)
    {
    }

    default void visitKotlinModule(KotlinModule kotlinModule)
    {
        visitAnyResourceFile(kotlinModule);
    }

    default void visitResourceFile(ResourceFile resourceFile)
    {
        visitAnyResourceFile(resourceFile);
    }
}
