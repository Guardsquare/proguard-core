package dex_reader_api.node.insn;

import dex_reader_api.reader.Op;
import dex_reader_api.visitors.DexCodeVisitor;

public class FilledNewArrayStmtNode extends DexStmtNode {

    public final int[] args;
    public final String type;

    public FilledNewArrayStmtNode(Op op, int[] args, String type) {
        super(op);
        this.args = args;
        this.type = type;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitFilledNewArrayStmt(op, args, type);
    }
}
