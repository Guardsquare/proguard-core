/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2013 Panxiaobo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package proguard.dexfile.ir.ts;

import java.util.*;
import proguard.dexfile.ir.IrMethod;
import proguard.dexfile.ir.expr.Local;
import proguard.dexfile.ir.expr.Value;
import proguard.dexfile.ir.stmt.AssignStmt;
import proguard.dexfile.ir.stmt.LabelStmt;
import proguard.dexfile.ir.stmt.Stmt;
import proguard.dexfile.ir.stmt.StmtList;

/**
 * This class attempts to remove SSA form and reduce the number of variables used by the program.
 * This is an optimiser which does not need to be used as it does not change the semantics of a
 * program but simply the number of variables it uses and therefore its performance. However not
 * using this will lead to much larger memory requirements of the other optimisers as it requires
 * them to process more variables and variable assignments.
 */
public class RemoveLocalFromSSA extends StatedTransformer {
  static <T extends Value> void replaceAssign(
      List<AssignStmt> assignStmtList, Map<Local, T> toReplace) {
    for (AssignStmt as : assignStmtList) {
      Value right = as.getOp2();
      T to = toReplace.get(right);
      if (to != null) {
        as.setOp2(to);
      }
    }
  }

  private boolean simpleAssign(
      List<LabelStmt> phiLabels,
      List<AssignStmt> assignStmtList,
      Map<Local, Local> toReplace,
      StmtList stmts) {
    Set<Value> usedInPhi = new LinkedHashSet<>();
    if (phiLabels != null) {
      for (LabelStmt labelStmt : phiLabels) {
        for (AssignStmt phi : labelStmt.phis) {
          usedInPhi.addAll(Arrays.asList(phi.getOp2().getOps()));
        }
      }
    }
    boolean changed = false;
    for (Iterator<AssignStmt> it = assignStmtList.iterator(); it.hasNext(); ) {
      AssignStmt as = it.next();
      if (!usedInPhi.contains(as.getOp1())) {
        it.remove();
        stmts.remove(as);
        toReplace.put((Local) as.getOp1(), (Local) as.getOp2());
        changed = true;
      }
    }

    return changed;
  }

  private void replacePhi(List<LabelStmt> phiLabels, Map<Local, Local> toReplace, Set<Value> set) {
    if (phiLabels != null) {
      for (LabelStmt labelStmt : phiLabels) {
        for (AssignStmt phi : labelStmt.phis) {
          Value[] ops = phi.getOp2().getOps();
          for (Value op : ops) {
            Value n = toReplace.get(op);
            if (n != null) {
              set.add(n);
            } else {
              set.add(op);
            }
          }
          set.remove(phi.getOp1());
          phi.getOp2().setOps(set.toArray(new Value[0]));
          set.clear();
        }
      }
    }
  }

  static class PhiObject {
    Set<PhiObject> parent = new LinkedHashSet<>();
    Set<PhiObject> children = new LinkedHashSet<>();
    Local local;
    boolean isInitByPhi = false;
  }

  public static PhiObject getOrCreate(Map<Local, PhiObject> map, Local local) {
    PhiObject po = map.get(local);
    if (po == null) {
      po = new PhiObject();
      po.local = local;
      map.put(local, po);
    }
    return po;
  }

  public static void linkPhiObject(PhiObject parent, PhiObject child) {
    parent.children.add(child);
    child.parent.add(parent);
  }

  private boolean simplePhi(
      List<LabelStmt> phiLabels, Map<Local, Local> toReplace, Set<Value> set) {
    if (phiLabels == null) {
      return false;
    }

    boolean changed = false;
    for (Iterator<LabelStmt> itLabel = phiLabels.iterator(); itLabel.hasNext(); ) {
      LabelStmt labelStmt = itLabel.next();
      for (Iterator<AssignStmt> it = labelStmt.phis.iterator(); it.hasNext(); ) {
        AssignStmt phi = it.next();

        // Add possible phi inputs to set e.g. [B,C] in A = φ(B,C).
        set.addAll(Arrays.asList(phi.getOp2().getOps()));

        // Situations where a phi operand is in the input operands and output operands can
        // be removed e.g. A = φ(A,B) -> A = φ(B).
        set.remove(phi.getOp1());
        if (set.size() == 1) {
          // If there is only one phi operand then we can replace all references to the output
          // variable with the input variable e.g. [ A = φ(B); print(A); ] -> [ print(B); ].
          it.remove();
          changed = true;
          toReplace.put((Local) phi.getOp1(), (Local) set.iterator().next());
        }
        set.clear();
      }
      if (labelStmt.phis.size() == 0) {
        labelStmt.phis = null;
        itLabel.remove();
      }
    }
    return changed;
  }

  /**
   * Create a graph of <a
   * href="https://en.wikipedia.org/wiki/Static_single-assignment_form">phis</a>. Propagate parent
   * nodes throughout the graph such that every node will know every single node above it e.g.
   * parents + grandparents + great-grandparents etc. The nodes at the top of this directed graph
   * (nodes with no parents, which I refer to as top nodes) will represent variable locations e.g.
   * input parameters for a method will originate in local variables v1, v2 etc. This will also mean
   * that a node that contains itself in its parents will be contained in a loop in the Phi graph.
   * Since Phis are simply variable reassignments each node is checked to see if it contains only 1
   * top node in its parents. The output variable of that phi operation can simply be replaced with
   * the top node.
   */
  private boolean removeLoopFromPhi(List<LabelStmt> phiLabels, Map<Local, Local> toReplace) {
    if (phiLabels == null) {
      return false;
    }

    boolean changed = false;
    Set<Local> toDeletePhiAssign = new LinkedHashSet<>();
    Map<Local, PhiObject> phis;
    // Make a directed phi graph with nodes for all the phi calls e.g. In the example below
    // C = φ(A,B) is represented by the following graph:
    //     A -- +
    //          | --> C
    //     B ---+
    phis = collectPhiObjects(phiLabels);
    Queue<PhiObject> q = new UniqueQueue<>();
    // Starting from the top nodes of the graph doesn't seem to provide any speed up.
    q.addAll(phis.values());

    while (!q.isEmpty()) {
      PhiObject po = q.poll();
      // This loop makes child phi nodes inherit all their parent's parent nodes.
      for (PhiObject child : po.children) {
        // Unclear why this check is needed as non phi-inited nodes should have no parents,
        // and manual testing on an individual app also confirmed this.
        if (child.isInitByPhi) {
          // If new nodes are added to the child then we need to propagate them.
          if (child.parent.addAll(po.parent)) {
            q.add(child);
          }
        }
      }
    }

    for (PhiObject po : phis.values()) {
      if (po.isInitByPhi) {
        Local local = null;
        // Itter over parents - if ONLY one of them has a concrete instantiation location (not a
        // phi), then we replace the produced phi variable with this location.
        for (PhiObject p : po.parent) {
          if (!p.isInitByPhi) {
            if (local == null) { // The first non-phi value.
              local = p.local;
            } else {
              // Another parent has a concrete location, so we can't replace it.
              local = null;
              break;
            }
          }
        }
        if (local != null) {
          // The actual replacements are handled in replacePhi.
          toReplace.put(po.local, local);
          toDeletePhiAssign.add(po.local);
          changed = true;
        }
      }
    }
    for (Iterator<LabelStmt> itLabel = phiLabels.iterator(); itLabel.hasNext(); ) {
      LabelStmt labelStmt = itLabel.next();
      labelStmt.phis.removeIf(phi -> toDeletePhiAssign.contains(phi.getOp1()));
      if (labelStmt.phis.size() == 0) {
        labelStmt.phis = null;
        itLabel.remove();
      }
    }
    return changed;
  }

  private Map<Local, PhiObject> collectPhiObjects(List<LabelStmt> phiLabels) {
    Map<Local, PhiObject> phis;
    phis = new LinkedHashMap<>();
    for (LabelStmt labelStmt : phiLabels) {
      for (AssignStmt as : labelStmt.phis) {
        Local local = (Local) as.getOp1();
        PhiObject child = getOrCreate(phis, local);
        child.isInitByPhi = true;
        for (Value op : as.getOp2().getOps()) {
          if (op == local) {
            continue;
          }
          PhiObject parent = getOrCreate(phis, (Local) op);
          linkPhiObject(parent, child);
        }
      }
    }
    return phis;
  }

  static <T> void fixReplace(Map<Local, T> toReplace) {
    List<Map.Entry<Local, T>> set = new ArrayList<>(toReplace.entrySet());
    set.sort(Comparator.comparingInt(localTEntry -> localTEntry.getKey()._ls_index));

    boolean changed = true;
    while (changed) {
      changed = false;
      for (Map.Entry<Local, T> e : set) {
        T b = e.getValue();
        if (b instanceof Local) {
          T n = toReplace.get(b);
          if (n != null && b != n) {
            changed = true;
            e.setValue(n);
          }
        }
      }
    }
  }

  @Override
  public boolean transformReportChanged(IrMethod method) {
    boolean irChanged = false;
    List<AssignStmt> assignStmtList = new ArrayList<>();
    List<LabelStmt> phiLabels = method.phiLabels;
    for (Stmt p = method.stmts.getFirst(); p != null; p = p.getNext()) {
      if (p.st == Stmt.ST.ASSIGN) {
        AssignStmt as = (AssignStmt) p;
        if (as.getOp1().vt == Value.VT.LOCAL && as.getOp2().vt == Value.VT.LOCAL) {
          assignStmtList.add(as);
        }
      }
    }
    final Map<Local, Local> toReplace = new LinkedHashMap<>();
    Set<Value> set = new LinkedHashSet<>();
    boolean changed = true;

    while (changed) {
      changed = false;

      // The removeLoopFromPhi call is where most of the work happens and the large majority of the
      // processing occurs here. Optimising this would cause significant improvements.
      // From initial testing it appears this function call can be removed, however it still causes
      // other optimisers to more memory and therefore crash in some applications.
      if (removeLoopFromPhi(phiLabels, toReplace)) {
        fixReplace(toReplace);
        replacePhi(phiLabels, toReplace, set);
      }

      // This loop is effectively doing graph squashing in O(n^2) time e.g.
      // (A -> B -> C -> D) = (A-> D).
      // This can be brought down to O(n) time (if performed on a DAG), however the initial
      // implementation made the O(n) implementation take longer. It was suspected that
      // the initial cost of instantiating objects for the algorithm combined with potentially
      // small graphs being traversed caused this slowdown.

      // Remove a = phi(b) - simple case where phi has only one operand.
      while (simplePhi(phiLabels, toReplace, set)) {
        fixReplace(toReplace);
        replacePhi(phiLabels, toReplace, set);
      }

      while (simpleAssign(phiLabels, assignStmtList, toReplace, method.stmts)) { // remove a=b
        fixReplace(toReplace);
        replaceAssign(assignStmtList, toReplace);
        changed = true;
        irChanged = true;
      }
      replacePhi(phiLabels, toReplace, set);
    }

    for (Local local : toReplace.keySet()) {
      method.locals.remove(local);
      irChanged = true;
    }
    if (toReplace.size() > 0) {
      Cfg.travelMod(
          method.stmts,
          new Cfg.TravelCallBack() {
            @Override
            public Value onAssign(Local v, AssignStmt as) {
              return v;
            }

            @Override
            public Value onUse(Local v) {
              Local n = toReplace.get(v);
              return n == null ? v : n;
            }
          },
          true);
    }
    return irChanged;
  }
}
