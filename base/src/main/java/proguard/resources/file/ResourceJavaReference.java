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
package proguard.resources.file;

import proguard.classfile.Clazz;

import java.util.Objects;

/**
 * Represents a reference to a Java class from a resource file.
 *
 * @author Lars Vandenbergh
 */
public class ResourceJavaReference
{
    public String externalClassName;
    public Clazz  referencedClass;


    public ResourceJavaReference(String externalClassName)
    {
        this.externalClassName = externalClassName;
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceJavaReference that = (ResourceJavaReference)o;
        return externalClassName.equals(that.externalClassName);
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(externalClassName);
    }
}
