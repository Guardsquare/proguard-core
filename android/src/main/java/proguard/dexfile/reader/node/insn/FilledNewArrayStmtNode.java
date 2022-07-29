package proguard.dexfile.reader.node.insn;

import proguard.dexfile.reader.Op;
import proguard.dexfile.reader.visitors.DexCodeVisitor;

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
