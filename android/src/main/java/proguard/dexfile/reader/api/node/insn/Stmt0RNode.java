package proguard.dexfile.reader.api.node.insn;

import proguard.dexfile.reader.api.reader.Op;
import proguard.dexfile.reader.api.visitors.DexCodeVisitor;

public class Stmt0RNode extends DexStmtNode {
    public Stmt0RNode(Op op) {
        super(op);
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitStmt0R(op);
    }
}
