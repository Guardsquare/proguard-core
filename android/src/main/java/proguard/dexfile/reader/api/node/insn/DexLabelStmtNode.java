package proguard.dexfile.reader.api.node.insn;

import proguard.dexfile.reader.api.DexLabel;
import proguard.dexfile.reader.api.visitors.DexCodeVisitor;

public class DexLabelStmtNode extends DexStmtNode {
    public DexLabel label;

    public DexLabelStmtNode(DexLabel label) {
        super(null);
        this.label = label;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitLabel(label);
    }
}
