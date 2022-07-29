package proguard.dexfile.reader.node.insn;

import proguard.dexfile.reader.DexLabel;
import proguard.dexfile.reader.Op;
import proguard.dexfile.reader.visitors.DexCodeVisitor;

public class SparseSwitchStmtNode extends BaseSwitchStmtNode {

    public final int[] cases;

    public SparseSwitchStmtNode(Op op, int a, int[] cases, DexLabel[] labels) {
        super(op, a, labels);
        this.cases = cases;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitSparseSwitchStmt(op, a, cases, labels);
    }
}
