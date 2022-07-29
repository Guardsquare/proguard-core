/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 */

package proguard.dexfile.reader;

import proguard.dexfile.reader.api.visitors.DexFileVisitor;

import java.util.List;

public interface BaseDexFileReader {

    int getDexVersion();

    void accept(DexFileVisitor dv);

    List<String> getClassNames();

    void accept(DexFileVisitor dv, int config);

    void accept(DexFileVisitor dv, int classIdx, int config);
}
