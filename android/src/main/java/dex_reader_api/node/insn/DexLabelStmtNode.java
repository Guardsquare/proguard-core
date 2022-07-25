package dex_reader_api.node.insn;

import dex_reader_api.DexLabel;
import dex_reader_api.visitors.DexCodeVisitor;

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
