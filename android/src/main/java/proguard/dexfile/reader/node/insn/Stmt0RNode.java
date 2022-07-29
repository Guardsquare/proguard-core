package proguard.dexfile.reader.node.insn;

import proguard.dexfile.reader.Op;
import proguard.dexfile.reader.visitors.DexCodeVisitor;

public class Stmt0RNode extends DexStmtNode {
    public Stmt0RNode(Op op) {
        super(op);
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitStmt0R(op);
    }
}
