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
package proguard.dexfile.ir.expr;

import proguard.dexfile.ir.LabelAndLocalMapper;

/**
 * Represent a LENGTH,NEG expression
 *
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 * @version $Rev$
 * @see Value.VT#LENGTH
 * @see Value.VT#NEG
 * @see Value.VT#NOT
 */
public class UnopExpr extends Value.E1Expr {
  public String type;

  @Override
  protected void releaseMemory() {
    type = null;
    super.releaseMemory();
  }

  /**
   * @param vt
   * @param value
   * @param type
   */
  public UnopExpr(VT vt, Value value, String type) {
    super(vt, value);
    this.type = type;
  }

  @Override
  public Value clone() {
    return new UnopExpr(vt, op.trim().clone(), type);
  }

  @Override
  public Value clone(LabelAndLocalMapper mapper) {
    return new UnopExpr(vt, op.clone(mapper), type);
  }

  @Override
  public String toString0() {
    switch (vt) {
      case LENGTH:
        return op + ".length";
      case NEG:
        return "(-" + op + ")";
      case NOT:
        return "(!" + op + ")";
      default:
    }
    return super.toString();
  }
}
