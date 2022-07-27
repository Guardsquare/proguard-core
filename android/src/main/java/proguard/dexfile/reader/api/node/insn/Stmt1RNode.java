package proguard.dexfile.reader.api.node.insn;

import proguard.dexfile.reader.api.reader.Op;
import proguard.dexfile.reader.api.visitors.DexCodeVisitor;

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
