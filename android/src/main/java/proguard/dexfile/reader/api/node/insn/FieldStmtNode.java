package proguard.dexfile.reader.api.node.insn;

import proguard.dexfile.reader.api.Field;
import proguard.dexfile.reader.api.reader.Op;
import proguard.dexfile.reader.api.visitors.DexCodeVisitor;

public class FieldStmtNode extends DexStmtNode {

    public final int a;
    public final int b;
    public final Field field;

    public FieldStmtNode(Op op, int a, int b, Field field) {
        super(op);
        this.a = a;
        this.b = b;
        this.field = field;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitFieldStmt(op, a, b, field);
    }
}
