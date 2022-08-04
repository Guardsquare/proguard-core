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
package proguard.dexfile.ir.expr;

import proguard.dexfile.ir.LabelAndLocalMapper;
import proguard.dexfile.reader.MethodHandle;
import proguard.dexfile.reader.Proto;

public class InvokeCustomExpr extends InvokeExpr {
    public String name;
    public Proto proto;
    public MethodHandle handle;
    public Object[] bsmArgs;

    @Override
    protected void releaseMemory() {
        name = null;
        proto = null;
        handle = null;
        bsmArgs = null;
        super.releaseMemory();
    }

    @Override
    public Proto getProto() {
        return proto;
    }

    public InvokeCustomExpr(Value.VT type, Value[] args, String methodName, Proto proto, MethodHandle handle, Object[] bsmArgs) {
        super(type, args, handle == null ? null : handle.getMethod());
        this.proto = proto;
        this.name = methodName;
        this.handle = handle;
        this.bsmArgs = bsmArgs;
    }

    @Override
    public InvokeCustomExpr clone() {
        return new InvokeCustomExpr(vt, cloneOps(), name, proto, handle, bsmArgs);
    }

    @Override
    public InvokeCustomExpr clone(LabelAndLocalMapper mapper) {
        return new InvokeCustomExpr(vt, cloneOps(mapper), name, proto, handle, bsmArgs);
    }

    @Override
    public String toString0() {
        StringBuilder sb = new StringBuilder();

        sb.append("InvokeCustomExpr(....)");
        return sb.toString();
    }
}
