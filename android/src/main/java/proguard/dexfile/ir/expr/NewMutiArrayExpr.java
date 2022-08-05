/*
 * Copyright (c) 2009-2012 Panxiaobo
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

/**
 * Represent a NEW_MUTI_ARRAY expression.
 *
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 * @version $Rev: 9fd8005bbaa4 $
 * @see Value.VT#NEW_MULTI_ARRAY
 */
public class NewMutiArrayExpr extends Value.EnExpr {

    /**
     * the basic type, ZBSCIFDJL, no [
     */
    public String baseType;
    /**
     * the dimension of the array,
     * <p/>
     * for baseType: I, dimension 4, the result type is int[][][][];
     * <p/>
     * NOTICE, not all dimension are init in ops, so ops.length <= dimension
     */
    public int dimension;

    public NewMutiArrayExpr(String base, int dimension, Value[] sizes) {
        super(VT.NEW_MULTI_ARRAY, sizes);
        this.baseType = base;
        this.dimension = dimension;
    }

    @Override
    protected void releaseMemory() {
        baseType = null;
        super.releaseMemory();
    }

    @Override
    public Value clone() {
        return new NewMutiArrayExpr(baseType, dimension, cloneOps());
    }

    @Override
    public Value clone(LabelAndLocalMapper mapper) {
        return new NewMutiArrayExpr(baseType, dimension, cloneOps(mapper));
    }

    @Override
    public String toString0() {
        StringBuilder sb = new StringBuilder();
        sb.append("new ").append(Util.toShortClassName(baseType));
        for (Value op : ops) {
            sb.append('[').append(op).append(']');
        }
        for (int i = ops.length; i < dimension; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }

}
