package proguard.dexfile.ir.ts;

import proguard.dexfile.ir.IrMethod;
import proguard.dexfile.ir.expr.Constant;
import proguard.dexfile.ir.expr.Exprs;
import proguard.dexfile.ir.expr.Local;
import proguard.dexfile.ir.expr.Value;
import proguard.dexfile.ir.stmt.AssignStmt;
import proguard.dexfile.ir.stmt.LabelStmt;
import proguard.dexfile.ir.stmt.Stmt;
import proguard.dexfile.ir.stmt.UnopStmt;

import java.util.*;

/**
 * 1. Remove constant AssignStmt.
 *
 * <pre>
 * a = &quot;123&quot;;
 * return a;
 * </pre>
 * <p>
 * to
 *
 * <pre>
 * return &quot;123&quot;;
 * </pre>
 * <p>
 * 2. Remove Phi if all value are equal
 *
 * <pre>
 * a = &quot;123&quot;;
 * // ...
 * b = &quot;123&quot;;
 * // ...
 * c = PHI(a, b);
 * return c;
 * </pre>
 * <p>
 * to
 *
 * <pre>
 * // ...
 * return &quot;123&quot;;
 * </pre>
 */
public class RemoveConstantFromSSA extends StatedTransformer {

    public static final Comparator<Local> LOCAL_COMPARATOR = Comparator.comparingInt(local -> local._ls_index);

    @Override
    public boolean transformReportChanged(IrMethod method) {
        boolean changed = false;
        List<AssignStmt> assignStmtList = new ArrayList<>();
        Set<Local> usedByLock = getLocalsUsedByLock(method);

        Map<Local, Object> cstMap = new LinkedHashMap<>();
        for (Stmt p = method.stmts.getFirst(); p != null; p = p.getNext()) {
            if (p.st == Stmt.ST.ASSIGN) {
                AssignStmt as = (AssignStmt) p;
                if (as.getOp1().vt == Value.VT.LOCAL && !usedByLock.contains(as.getOp1())) {
                    if (as.getOp2().vt == Value.VT.CONSTANT) {
                        assignStmtList.add(as);
                        cstMap.put((Local) as.getOp1(), ((Constant) as.getOp2()).value);
                    } else if (as.getOp2().vt == Value.VT.LOCAL) {
                        cstMap.put((Local) as.getOp1(), as.getOp2());
                    }
                }
            }
        }
        if (assignStmtList.size() == 0) {
            return false;
        }
        RemoveLocalFromSSA.fixReplace(cstMap);
        final Map<Local, Value> toReplace = new LinkedHashMap<>();
        Set<Value> usedInPhi = new LinkedHashSet<>();
        List<LabelStmt> phiLabels = method.phiLabels;
        if (phiLabels != null) {
            boolean loopAgain = true;
            while (loopAgain) {
                loopAgain = false;
                usedInPhi.clear();
                for (Iterator<LabelStmt> it = phiLabels.iterator(); it.hasNext(); ) {
                    LabelStmt labelStmt = it.next();
                    if (labelStmt.phis != null) {
                        for (Iterator<AssignStmt> it2 = labelStmt.phis.iterator(); it2.hasNext(); ) {
                            AssignStmt phi = it2.next();
                            Value[] vs = phi.getOp2().getOps();
                            Object sameCst = null;
                            boolean allEqual = true;
                            for (Value p : vs) {
                                Object cst = cstMap.get(p);
                                if (cst == null) {
                                    allEqual = false;
                                    break;
                                }
                                if (sameCst == null) {
                                    sameCst = cst;
                                } else if (!sameCst.equals(cst)) {
                                    allEqual = false;
                                    break;
                                }
                            }
                            if (allEqual) { // all are same constant
                                cstMap.put((Local) phi.getOp1(), sameCst);
                                if (sameCst instanceof Local) {
                                    phi.setOp2((Value) sameCst);
                                } else {
                                    phi.setOp2(Exprs.nConstant(sameCst));
                                    assignStmtList.add(phi);
                                }
                                it2.remove();
                                method.stmts.insertAfter(labelStmt, phi);
                                changed = true;
                                loopAgain = true; // loop again
                            } else {
                                usedInPhi.addAll(Arrays.asList(phi.getOp2().getOps()));
                            }
                        }
                        if (labelStmt.phis.size() == 0) {
                            it.remove();
                        }
                    }
                }
            }
        }

        for (Iterator<AssignStmt> it = assignStmtList.iterator(); it.hasNext(); ) {
            AssignStmt as = it.next();
            if (!usedInPhi.contains(as.getOp1())) {
                it.remove();
                method.stmts.remove(as);
                method.locals.remove(as.getOp1());
                changed = true;
            }
            toReplace.put((Local) as.getOp1(), as.getOp2());

        }

        Cfg.travelMod(method.stmts, new Cfg.TravelCallBack() {
            @Override
            public Value onAssign(Local v, AssignStmt as) {
                return v;
            }

            @Override
            public Value onUse(Local v) {
                Value n = toReplace.get(v);
                return n == null ? v : n.clone();
            }
        }, false);
        return changed;
    }

    /**
     * Don't inline any constants for locals used by a lock.
     * If converted back to dex, this may cause a VerifyError as new
     * throwing instructions could be added without being guarded by a catch-all.
     * https://issuetracker.google.com/issues/160192231
     *
     * @param method The method to check for lock instructions.
     * @return a set of Locals used by lock instructions.
     */
    private static Set<Local> getLocalsUsedByLock(IrMethod method) {
        Set<Local> usedByLock = new LinkedHashSet<>();

        for (Stmt p = method.stmts.getFirst(); p != null; p = p.getNext()) {
            if (p.st == Stmt.ST.LOCK) {
                UnopStmt lockStmt = (UnopStmt) p;
                if (lockStmt.getOp().vt == Value.VT.LOCAL) {
                    usedByLock.add((Local) lockStmt.getOp());
                }
            }
        }

        return usedByLock;
    }
}
