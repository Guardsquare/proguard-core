/*
 * Copyright (c) 2009-2017 Panxiaobo
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
package proguard.dexfile.reader.node.insn;

import proguard.dexfile.reader.Method;
import proguard.dexfile.reader.Proto;
import proguard.dexfile.reader.Op;
import proguard.dexfile.reader.visitors.DexCodeVisitor;

public class MethodStmtNode extends AbstractMethodStmtNode {
    public final Method method;

    public MethodStmtNode(Op op, int[] args, Method method) {
        super(op, args);
        this.method = method;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitMethodStmt(op, args, method);
    }

    @Override
    public Proto getProto() {
        return method.getProto();
    }
}
