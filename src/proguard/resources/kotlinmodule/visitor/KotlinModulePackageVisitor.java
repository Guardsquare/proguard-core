/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.resources.kotlinmodule.visitor;

import proguard.resources.kotlinmodule.*;

/**
 * @author James Hamilton
 */
public interface KotlinModulePackageVisitor
{
    void visitKotlinModulePackage(KotlinModule kotlinModule, KotlinModulePackage kotlinModulePart);
}
