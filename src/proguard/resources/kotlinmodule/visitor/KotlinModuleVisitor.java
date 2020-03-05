/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.resources.kotlinmodule.visitor;

import proguard.resources.kotlinmodule.KotlinModule;


/**
 * A visitor for {@link KotlinModule}.
 *
 * @author James Hamilton
 */
public interface KotlinModuleVisitor
{
    void visitKotlinModule(KotlinModule kotlinModule);
}
