package proguard.dexfile.reader.node.insn;

import proguard.dexfile.reader.DexLabel;
import proguard.dexfile.reader.Op;

public abstract class BaseSwitchStmtNode extends DexStmtNode {

    public final int a;
    public final DexLabel[] labels;

    protected BaseSwitchStmtNode(Op op, int a, DexLabel[] labels) {
        super(op);
        this.a = a;
        this.labels = labels;
    }
}
