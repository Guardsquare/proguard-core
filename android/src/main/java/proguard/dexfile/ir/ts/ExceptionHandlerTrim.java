/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2012 Panxiaobo
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

import proguard.dexfile.ir.IrMethod;
import proguard.dexfile.ir.LabelAndLocalMapper;
import proguard.dexfile.ir.Trap;
import proguard.dexfile.ir.Util;
import proguard.dexfile.ir.stmt.LabelStmt;
import proguard.dexfile.ir.stmt.Stmt;
import proguard.dexfile.ir.stmt.Stmts;

import java.util.ArrayList;
import java.util.List;

/**
 * Trim Exception handler.
 * <p>
 * before:
 *
 * <pre>
 * L1
 * STMTs
 * throwableSTMTs
 * STMTs
 * throwableSTMTs
 * L2
 * ...
 * L3
 * ...
 *
 * L1 - L2 : all > L3
 *
 * </pre>
 * <p>
 * after:
 *
 * <pre>
 * L1
 * STMTs
 * L4
 * throwableSTMTs
 * L5
 * STMTs
 * L6
 * throwableSTMTs
 * L2
 * ...
 * L3
 * ...
 *
 * L4 - L5 : all > L3
 * L6 - L2 : all > L3
 * </pre>
 *
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 */
public class ExceptionHandlerTrim implements Transformer {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void transform(IrMethod irMethod) {
        List<Trap> trips = irMethod.traps;
        irMethod.traps = new ArrayList();
        LabelAndLocalMapper map = new LabelAndLocalMapper() {
            @Override
            public LabelStmt map(LabelStmt label) {
                return label;
            }
        };
        int counter = 0;
        for (Trap trap : trips) {
            if (counter % 100 == 0) {
                if (Util.irTransformMemoryLimit > 0 && Util.isMemoryLimitReached(Util.irTransformMemoryLimit)) {
                    throw new RuntimeException("Memory Limit Reached");
                }
            }
            counter++;
            Trap ntrap = trap.clone(map);
            int status = 0;
            for (Stmt p = trap.start.getNext(); p != trap.end; p = p.getNext()) {
                if (!Cfg.notThrow(p)) {
                    if (status == 0) {
                        Stmt pre = p.getPre();
                        if (pre == null || pre.st != Stmt.ST.LABEL) {
                            pre = Stmts.nLabel();
                            irMethod.stmts.insertBefore(p, pre);
                        }
                        ntrap.start = (LabelStmt) pre;
                        status = 1;
                    }  // continue;

                } else if (status == 1) {
                    Stmt pre = p.getPre();
                    if (pre == null || pre.st != Stmt.ST.LABEL) {
                        pre = Stmts.nLabel();
                        irMethod.stmts.insertBefore(p, pre);
                    }

                    ntrap.end = (LabelStmt) pre;
                    irMethod.traps.add(ntrap);
                    status = 0;
                    ntrap = trap.clone(map);
                }
            }
            if (status == 1) {
                ntrap.end = trap.end;
                irMethod.traps.add(ntrap);
                status = 0;
            }
        }
    }
}
