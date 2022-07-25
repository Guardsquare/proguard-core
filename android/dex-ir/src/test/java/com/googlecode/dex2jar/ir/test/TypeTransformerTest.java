/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2013 Panxiaobo
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
package com.googlecode.dex2jar.ir.test;

import com.googlecode.dex2jar.ir.TypeClass;
import com.googlecode.dex2jar.ir.expr.Local;
import com.googlecode.dex2jar.ir.expr.Value;
import com.googlecode.dex2jar.ir.stmt.AssignStmt;
import com.googlecode.dex2jar.ir.stmt.LabelStmt;
import com.googlecode.dex2jar.ir.stmt.UnopStmt;
import com.googlecode.dex2jar.ir.ts.TypeTransformer;
import org.junit.Assert;
import org.junit.Test;

import static com.googlecode.dex2jar.ir.expr.Exprs.*;
import static com.googlecode.dex2jar.ir.stmt.Stmts.*;

public class TypeTransformerTest extends BaseTransformerTest<TypeTransformer> {
    /**
     * base test
     */
    @Test
    public void test00Base() {
        initMethod(true, "Ljava/lang/Object;");
        Local b = addLocal("b");

        addStmt(nAssign(b, nString("123")));
        addStmt(nReturn(b));

        transform();
        Assert.assertEquals("", "L", b.valueType.substring(0, 1));
    }

    @Test
    public void test1Const() {
        initMethod(true, "F");
        Local b = addLocal("b");

        AssignStmt st1 = addStmt(nAssign(b, nInt(0)));
        UnopStmt st3 = addStmt(nReturn(b));
        transform();
        Assert.assertEquals("", b.valueType, "F");
    }

    @Test
    public void test2byte() {
        initMethod(true, "V");
        Local b = addLocal("b");

        addStmt(nAssign(b, nStaticField("La;", "z", "B")));
        addStmt(nVoidInvoke(nInvokeStatic(new Value[] { b }, "La;", "y", new String[] { "I" }, "V")));
        addStmt(nAssign(nStaticField("La;", "z", "B"), b));
        addStmt(nReturnVoid());
        transform();
        // FIXME fix type detect
        // Assert.assertEquals("", "I", b.valueType);
    }

    @Test
    public void test2char() {
        initMethod(true, "V");
        Local b = addLocal("b");

        addStmt(nAssign(b, nInt(255)));
        addStmt(nVoidInvoke(nInvokeStatic(new Value[] { b }, "La;", "y", new String[] { "I" }, "V")));
        addStmt(nAssign(nStaticField("La;", "z", "C"), b));
        addStmt(nReturnVoid());
        transform();
        // FIXME fix type detect
        // Assert.assertEquals("", "I", b.valueType);
    }

    // @Ignore("type b to Int is ok to this context")
    @Test
    public void test3() {
        initMethod(true, "V");
        Local b = addLocal("b");

        addStmt(nAssign(b, nInt(456)));
        LabelStmt L0 = newLabel();
        addStmt(nIf(nEq(b, nInt(0), TypeClass.ZIFL.name), L0));
        addStmt(L0);
        addStmt(nReturnVoid());
        transform();
        Assert.assertEquals("", "I", b.valueType);
    }

    @Test
    public void test3Z() {
        initMethod(true, "V");
        Local b = addLocal("b");

        addStmt(nAssign(b, nInt(1)));
        LabelStmt L0 = newLabel();
        addStmt(nIf(nEq(b, nInt(0), TypeClass.ZIFL.name), L0));
        addStmt(L0);
        addStmt(nReturnVoid());
        transform();
        // FIXME local should type to Z but I works as well
        // Assert.assertEquals("", "Z", b.valueType);
    }

    @Test
    public void test2arrayF() {
        initMethod(true, "V");
        Local b = addLocal("b");
        Local c = addLocal("c");

        addStmt(nAssign(b, nNewMutiArray("F", 1, new Value[]{nInt(2)})));
        addStmt(nFillArrayData(b, nConstant(new int[]{5, 6})));
        addStmt(nAssign(c, nArray(b, nInt(3), TypeClass.IF.name)));
        addStmt(nReturnVoid());
        transform();
        Assert.assertEquals("", b.valueType, "[F");
    }

    @Test
    public void testDefaultZI() {
        initMethod(true, "V");
        Local b = addLocal("b");
        Local c = addLocal("c");

        addStmt(nAssign(b, nInt(5)));
        addStmt(nAssign(c, nOr(b, nInt(6), TypeClass.ZI.name)));

        addStmt(nReturnVoid());
        transform();
        Assert.assertEquals("I", c.valueType);
    }


    @Test
    public void testGithubIssue28() {
        initMethod(true, "V");
        Local b = addLocal("b");

        addStmt(nAssign(b, nNewMutiArray("D", 2, new Value[]{nInt(2), nInt(3)})));
        addStmt(nAssign(nArray(nArray(b, nInt(5), TypeClass.OBJECT.name), nInt(1), TypeClass.JD.name), nLong(0)));
        addStmt(nReturnVoid());
        transform();
        Assert.assertEquals("", b.valueType, "[[D");
    }

    @Test
    public void testGithubIssue28x() {
        initMethod(true, "V");
        Local b = addLocal("b");

        addStmt(nAssign(b, nInt(0)));
        addStmt(nAssign(nArray(nArray(b, nInt(5), TypeClass.OBJECT.name), nInt(1), TypeClass.JD.name), nLong(0)));
        addStmt(nReturnVoid());
        transform();
        // this case is ok to fail as the NPE transformer cover this
        // Assert.assertEquals("", "[[D", b.valueType);
    }

    @Test
    public void testMergingMultiDimensionalArray() {
        initMethod(true, "V");
        Local a = addLocal("a");
        Local b = addLocal("b");
        Local c = addLocal("c");
        addStmt(nAssign(a, nNewArray("LA;", nInt(1))));
        addStmt(nAssign(b, nNewMutiArray("LB;", 2, new Value[]{nInt(2), nInt(3)})));
        addStmt(nAssign(c, nPhi(a, b)));
        addStmt(nReturnVoid());
        transform();
        // c = phi(a [L, b [[L)
        // what type is c?
        // the type transformer will merge types:
        // [[L + [L -> [[L
        Assert.assertEquals("","[[L", c.valueType);
    }

    @Test //Origin: T5347
    public void testArrayAccessMergeIZ() {
        initMethod(true, "V");
        Local b = addLocal("b");
        Local c = addLocal("c");

        addStmt(nAssign(b, nNewMutiArray("I", 1, new Value[]{nInt(2)})));
        addStmt(nFillArrayData(b, nConstant(new int[]{1, 0})));
        addStmt(nAssign(c, nArray(b, nInt(0), TypeClass.BOOLEAN.name)));
        addStmt(nReturnVoid());
        transform();
        Assert.assertEquals("", "[I", b.valueType);
        Assert.assertEquals("", "I", c.valueType);
    }
}
