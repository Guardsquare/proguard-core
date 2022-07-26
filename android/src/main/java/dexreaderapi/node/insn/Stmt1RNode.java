package dexreaderapi.node.insn;

import dexreaderapi.reader.Op;
import dexreaderapi.visitors.DexCodeVisitor;

public class Stmt1RNode extends DexStmtNode {

    public final int a;

    public Stmt1RNode(Op op, int a) {
        super(op);
        this.a = a;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitStmt1R(op, a);
    }
}
