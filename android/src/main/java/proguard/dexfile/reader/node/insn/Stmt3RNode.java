package proguard.dexfile.reader.node.insn;

import proguard.dexfile.reader.Op;
import proguard.dexfile.reader.visitors.DexCodeVisitor;

public class Stmt3RNode extends DexStmtNode {
    public final int a;
    public final int b;
    public final int c;

    public Stmt3RNode(Op op, int a, int b, int c) {
        super(op);
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitStmt3R(op, a, b, c);
    }
}
