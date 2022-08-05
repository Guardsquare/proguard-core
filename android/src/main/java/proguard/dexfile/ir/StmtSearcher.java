package proguard.dexfile.ir;

import proguard.dexfile.ir.expr.Value;
import proguard.dexfile.ir.stmt.Stmt;
import proguard.dexfile.ir.stmt.StmtList;

public class StmtSearcher {
    public void travel(StmtList stmts) {
        for (Stmt stmt : stmts) {
            travel(stmt);
        }
    }

    public void travel(Stmt stmt) {
        switch (stmt.et) {
            case E0:
                break;
            case E1:
                travel(stmt.getOp());
                break;
            case E2:
                travel(stmt.getOp1());
                travel(stmt.getOp2());
                break;
            case En:
                Value[] ops = stmt.getOps();
                for (Value op : ops) {
                    travel(op);
                }
                break;
        }
    }

    public void travel(Value op) {
        switch (op.et) {
            case E0:
                break;
            case E1:
                travel(op.getOp());
                break;
            case E2:
                travel(op.getOp1());
                travel(op.getOp2());
                break;
            case En:
                Value[] ops = op.getOps();
                for (Value op1 : ops) {
                    travel(op1);
                }
                break;
        }
    }
}
