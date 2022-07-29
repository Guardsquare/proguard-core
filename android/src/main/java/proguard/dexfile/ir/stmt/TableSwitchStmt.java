/*
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
package proguard.dexfile.ir.stmt;

import proguard.dexfile.ir.LabelAndLocalMapper;
import proguard.dexfile.ir.expr.Value;

/**
 * Represent a TABLE_SWITCH statement
 *
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 * @version $Rev: 9fd8005bbaa4 $
 * @see ST#TABLE_SWITCH
 */
public class TableSwitchStmt extends BaseSwitchStmt {

    public int lowIndex;

    public TableSwitchStmt() {
        super(ST.TABLE_SWITCH, null);
    }

    public TableSwitchStmt(Value key, int lowIndex, LabelStmt[] targets, LabelStmt defaultTarget) {
        super(ST.TABLE_SWITCH, key);
        this.lowIndex = lowIndex;
        this.targets = targets;
        this.defaultTarget = defaultTarget;
    }

    @Override
    public Stmt clone(LabelAndLocalMapper mapper) {
        LabelStmt[] nTargets = new LabelStmt[targets.length];
        for (int i = 0; i < nTargets.length; i++) {
            nTargets[i] = mapper.map(targets[i]);
        }
        return new TableSwitchStmt(op.clone(mapper), lowIndex, nTargets, mapper.map(defaultTarget));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("switch(").append(op).append(") {");

        for (int i = 0; i < targets.length; i++) {
            sb.append("\n case ").append(lowIndex + i).append(": GOTO ").append(targets[i].getDisplayName())
                    .append(";");
        }
        sb.append("\n default : GOTO ").append(defaultTarget.getDisplayName()).append(";");
        sb.append("\n}");
        return sb.toString();
    }

}
