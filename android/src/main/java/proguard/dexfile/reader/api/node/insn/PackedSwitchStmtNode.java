package proguard.dexfile.reader.api.node.insn;

import proguard.dexfile.reader.api.DexLabel;
import proguard.dexfile.reader.api.reader.Op;
import proguard.dexfile.reader.api.visitors.DexCodeVisitor;

public class PackedSwitchStmtNode extends BaseSwitchStmtNode {

    public final int first_case;

    public PackedSwitchStmtNode(Op op, int a, int first_case, DexLabel[] labels) {
        super(op, a, labels);
        this.first_case = first_case;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitPackedSwitchStmt(op, a, first_case, labels);
    }
}
