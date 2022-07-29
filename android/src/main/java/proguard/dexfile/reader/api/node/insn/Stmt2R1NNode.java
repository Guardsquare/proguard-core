package proguard.dexfile.reader.api.node.insn;

import proguard.dexfile.reader.api.reader.Op;
import proguard.dexfile.reader.api.visitors.DexCodeVisitor;

public class Stmt2R1NNode extends DexStmtNode {

    public final int distReg;
    public final int srcReg;
    public final int content;

    public Stmt2R1NNode(Op op, int distReg, int srcReg, int content) {
        super(op);
        this.distReg = distReg;
        this.srcReg = srcReg;
        this.content = content;
    }

    @Override
    public void accept(DexCodeVisitor cv) {
        cv.visitStmt2R1N(op, distReg, srcReg, content);
    }
}
