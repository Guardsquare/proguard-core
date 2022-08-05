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
import proguard.dexfile.ir.Util;
import proguard.dexfile.reader.Method;
import proguard.dexfile.reader.Proto;

public class InvokePolymorphicExpr extends InvokeExpr {
    public Proto proto;

    @Override
    protected void releaseMemory() {
        proto = null;
        super.releaseMemory();
    }

    @Override
    public Proto getProto() {
        return proto;
    }

    public InvokePolymorphicExpr(Value.VT type, Value[] args, Proto proto, Method method) {
        super(type, args, method);
        this.proto = proto;
    }

    @Override
    public InvokePolymorphicExpr clone() {
        return new InvokePolymorphicExpr(vt, cloneOps(), proto, method);
    }

    @Override
    public InvokePolymorphicExpr clone(LabelAndLocalMapper mapper) {
        return new InvokePolymorphicExpr(vt, cloneOps(mapper), proto, method);
    }

    @Override
    public String toString0() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        sb.append(ops[i++]).append('.').append(this.method.getName());
        String[] argTypes = getProto().getParameterTypes();
        sb.append('(');
        int j = 0;
        boolean first = true;
        for (; i < ops.length; i++) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append("(").append(Util.toShortClassName(argTypes[j++])).append(")").append(ops[i]);
        }
        sb.append(')');

        return sb.toString();
    }
}
