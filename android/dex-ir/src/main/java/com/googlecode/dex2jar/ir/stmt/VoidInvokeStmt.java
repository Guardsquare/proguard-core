/*
 * dex2jar - Tools to work with android .dex and java .class files
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
package com.googlecode.dex2jar.ir.stmt;

import com.googlecode.dex2jar.ir.LabelAndLocalMapper;
import com.googlecode.dex2jar.ir.expr.Value;
import com.googlecode.dex2jar.ir.stmt.Stmt.E1Stmt;

/**
 * Represent a void-expr: the expr result is ignored.
 * possible op type: AbstractInvokeExpr, FieldExpr, or others
 * 
 * @see ST#VOID_INVOKE
 * 
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 * @version $Rev: 8da5a5faa6bd $
 */
public class VoidInvokeStmt extends E1Stmt {

    public VoidInvokeStmt(Value op) {
        super(ST.VOID_INVOKE, op);
    }

    @Override
    public Stmt clone(LabelAndLocalMapper mapper) {
        return new VoidInvokeStmt(op.clone(mapper));
    }

    @Override
    public String toString() {
        return "void " + op;
    }

}
