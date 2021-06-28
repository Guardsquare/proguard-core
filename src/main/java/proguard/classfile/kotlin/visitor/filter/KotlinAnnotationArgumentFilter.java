/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package proguard.classfile.kotlin.visitor.filter;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinAnnotatable;
import proguard.classfile.kotlin.KotlinAnnotation;
import proguard.classfile.kotlin.KotlinAnnotationArgument;
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor;

import java.util.function.Predicate;

import static proguard.classfile.kotlin.KotlinAnnotationArgument.*;

/**
 * Delegates to another {@link KotlinAnnotationArgumentVisitor} based on the
 * result of the given {@link Predicate<KotlinAnnotationArgument>}.
 *
 * @author James Hamilton
 */
public class KotlinAnnotationArgumentFilter implements KotlinAnnotationArgumentVisitor
{
    private final Predicate<KotlinAnnotationArgument> predicate;
    private final KotlinAnnotationArgumentVisitor     acceptedKotlinAnnotationVisitor;
    private final KotlinAnnotationArgumentVisitor     rejectedKotlinAnnotationVisitor;

    public KotlinAnnotationArgumentFilter(Predicate<KotlinAnnotationArgument> predicate,
                                          KotlinAnnotationArgumentVisitor     acceptedKotlinAnnotationVisitor,
                                          KotlinAnnotationArgumentVisitor     rejectedKotlinAnnotationVisitor)
    {
        this.predicate = predicate;
        this.acceptedKotlinAnnotationVisitor = acceptedKotlinAnnotationVisitor;
        this.rejectedKotlinAnnotationVisitor = rejectedKotlinAnnotationVisitor;
    }

    public KotlinAnnotationArgumentFilter(Predicate<KotlinAnnotationArgument> predicate,
                                          KotlinAnnotationArgumentVisitor     acceptedKotlinAnnotationVisitor)
    {
        this(predicate, acceptedKotlinAnnotationVisitor, null);
    }


    @Override
    public void visitAnyArgument(Clazz                    clazz,
                                 KotlinAnnotatable        annotatable,
                                 KotlinAnnotation         annotation,
                                 KotlinAnnotationArgument argument,
                                 Value                    value)
    {
        KotlinAnnotationArgumentVisitor delegate = this.getDelegate(argument);

        if (delegate != null)
        {
            argument.accept(clazz, annotatable, annotation, delegate);
        }
    }


    private KotlinAnnotationArgumentVisitor getDelegate(KotlinAnnotationArgument argument)
    {
        return this.predicate.test(argument)
                ? this.acceptedKotlinAnnotationVisitor
                : this.rejectedKotlinAnnotationVisitor;
    }
}
