package com.googlecode.dex2jar.ir.test;


import com.googlecode.dex2jar.ir.expr.*;
import com.googlecode.dex2jar.ir.stmt.Stmt;
import com.googlecode.dex2jar.ir.ts.NpeTransformer;
import org.junit.*;

import static com.googlecode.dex2jar.ir.expr.Exprs.*;
import static com.googlecode.dex2jar.ir.stmt.Stmts.nAssign;
import static com.googlecode.dex2jar.ir.stmt.Stmts.nReturn;

/**
 * Test the NpeTransformer.
 *
 * @author James Hamilton
 */
public class NpeTransformerTest extends BaseTransformerTest<NpeTransformer> {

    @Test
    public void test() {
        Local a = addLocal("a");
        // null[1] = a
        // return a
        addStmt(nAssign(nArray(nConstant(Constant.Null), nInt(1), "I"), a));
        addStmt(nReturn(a));
        transform();
        // throws NullPointerException
        // return a
        Assert.assertEquals(Stmt.ST.THROW, this.stmts.getFirst().st);
    }
}
