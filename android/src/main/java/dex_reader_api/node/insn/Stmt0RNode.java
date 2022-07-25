package dex_reader_api.node.insn;

import dex_reader_api.reader.Op;
import dex_reader_api.visitors.DexCodeVisitor;

public class Stmt0RNode extends DexStmtNode {
    public Stmt0RNode(Op op) {
        super(op);
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitStmt0R(op);
    }
}
