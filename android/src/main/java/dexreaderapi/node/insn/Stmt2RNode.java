package dexreaderapi.node.insn;

import dexreaderapi.reader.Op;
import dexreaderapi.visitors.DexCodeVisitor;

public class Stmt2RNode extends DexStmtNode {
    public final int a;
    public final int b;

    public Stmt2RNode(Op op, int a, int b) {
        super(op);
        this.a = a;
        this.b = b;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitStmt2R(op, a, b);
    }
}
