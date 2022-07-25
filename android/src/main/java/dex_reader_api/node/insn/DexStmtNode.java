package dex_reader_api.node.insn;

import dex_reader_api.reader.Op;
import dex_reader_api.visitors.DexCodeVisitor;


public abstract class DexStmtNode {
    public final Op op;

    public int __index;

    protected DexStmtNode(Op op) {
        this.op = op;
    }

    public abstract void accept(DexCodeVisitor cv);
}
