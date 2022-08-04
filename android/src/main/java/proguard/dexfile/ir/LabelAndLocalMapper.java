package proguard.dexfile.ir;

import proguard.dexfile.ir.expr.Local;
import proguard.dexfile.ir.stmt.LabelStmt;
import proguard.dexfile.ir.stmt.Stmts;

import java.util.LinkedHashMap;
import java.util.Map;

public class LabelAndLocalMapper {
    Map<LabelStmt, LabelStmt> labels = new LinkedHashMap<>();
    Map<Local, Local> locals = new LinkedHashMap<>();

    public LabelStmt map(LabelStmt label) {
        LabelStmt nTarget = labels.get(label);
        if (nTarget == null) {
            nTarget = Stmts.nLabel();
            labels.put(label, nTarget);
        }
        return nTarget;
    }

    public Local map(Local local) {
        Local nTarget = locals.get(local);
        if (nTarget == null) {
            nTarget = (Local) local.clone();
            locals.put(local, nTarget);
        }
        return nTarget;
    }
}
