package proguard.dexfile.reader.api.node.insn;

import proguard.dexfile.reader.api.DexLabel;
import proguard.dexfile.reader.api.reader.Op;

public abstract class BaseSwitchStmtNode extends DexStmtNode {

    public final int a;
    public final DexLabel[] labels;

    protected BaseSwitchStmtNode(Op op, int a, DexLabel[] labels) {
        super(op);
        this.a = a;
        this.labels = labels;
    }
}
