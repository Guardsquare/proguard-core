/*
 * Copyright (c) 2002-2020 Guardsquare NV
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
package proguard.dexfile.converter;

import proguard.classfile.constant.PrimitiveArrayConstant;
import proguard.classfile.util.PrimitiveArrayConstantReplacer;
import proguard.dexfile.ir.IrMethod;
import proguard.dexfile.ir.Trap;
import proguard.dexfile.ir.expr.*;
import proguard.dexfile.ir.expr.Value.E1Expr;
import proguard.dexfile.ir.expr.Value.E2Expr;
import proguard.dexfile.ir.expr.Value.EnExpr;
import proguard.dexfile.ir.expr.Value.VT;
import proguard.dexfile.ir.stmt.*;
import proguard.dexfile.ir.stmt.Stmt.E2Stmt;
import proguard.dexfile.ir.stmt.Stmt.ST;
import proguard.dexfile.reader.DexType;
import proguard.dexfile.reader.Method;
import proguard.dexfile.reader.Proto;
import proguard.classfile.ClassConstants;
import proguard.classfile.Clazz;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.editor.CompactCodeAttributeComposer;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.ClassUtil;

import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.Map;

// Derived from dex-translator/src/main/java/com/googlecode/d2j/converter/IR2JConverter.java
// We're preserving the original code as much as possible, to allow comparing
// and patching it.

/**
 * This utility class converts code in Dex2jar's intermediate representation
 * to ProGuard code.
 */
@SuppressWarnings("incomplete-switch")
public class IR2ProConverter {

    private static class StatementDepthException extends RuntimeException {
    }

    /**
     * If set to >0, this config option has the effect that methods
     * that contain overly complex statements with more than this
     * amount of nested expressions are not converted to Java bytecode,
     * as the conversion process would most likely produce a {@link StackOverflowError}.
     * This workaround results in a {@link proguard.classfile.Method}
     * with an empty {@link CodeAttribute} if it contains such a deeply
     * nested statement.
     */
    private static final int MAX_STATEMENT_DEPTH = Integer.parseInt(System.getProperty("proguard.dexconversion.maxstatementdepth", "0"));

    private boolean optimizeSynchronized = false;
    private boolean usePrimitiveArrayConstants = false;
    private int statementDepthCounter = 0;

    IrMethod ir;
    CompactCodeAttributeComposer code;

    public IR2ProConverter() {
        super();
    }

    /**
     * Specifies whether {@link PrimitiveArrayConstant}s can be generated,
     * when applicable.
     * <p>
     * If they are generated then they should be converted back to standard
     * Java arrays before converting to Java class files using {@link PrimitiveArrayConstantReplacer}.
     */
    public IR2ProConverter usePrimitiveArrayConstants(boolean usePrimitiveArrayConstants) {
        this.usePrimitiveArrayConstants = usePrimitiveArrayConstants;
        return this;
    }

    public IR2ProConverter optimizeSynchronized(boolean optimizeSynchronized) {
        this.optimizeSynchronized = optimizeSynchronized;
        return this;
    }

    public IR2ProConverter ir(IrMethod ir) {
        this.ir = ir;
        return this;
    }

    public IR2ProConverter code(CompactCodeAttributeComposer code) {
        this.code = code;
        return this;
    }

    public void convert() {
        mapLabelStmt(ir, code);
        try {
            reBuildInstructions(ir, code);
        } catch (StatementDepthException e) {
            // Conversion aborted, exit
            code.reset();
            code.beginCodeFragment(10); // start dummy fragment such that the subsequent endCodeFragment call succeeds
            return;
        }
        reBuildTryCatchBlocks(ir, code);
    }

    private void mapLabelStmt(IrMethod ir, CompactCodeAttributeComposer code) {
        for (Stmt p : ir.stmts) {
            if (p.st == ST.LABEL) {
                LabelStmt labelStmt = (LabelStmt) p;
                labelStmt.tag = code.createLabel();
            }
        }
    }

    /**
     * an empty try-catch block will cause other crash, we check this by finding non-label stmts between
     * {@link Trap#start} and {@link Trap#end}. if find we add the try-catch or we drop the try-catch.
     *
     * @param ir
     * @param code
     */
    private void reBuildTryCatchBlocks(IrMethod ir, CompactCodeAttributeComposer code) {
        for (Trap trap : ir.traps) {
            boolean needAdd = false;
            for (Stmt p = trap.start.getNext(); p != null && p != trap.end; p = p.getNext()) {
                if (p.st != ST.LABEL) {
                    needAdd = true;
                    break;
                }
            }
            if (needAdd) {
                for (int i = 0; i < trap.handlers.length; i++) {
                    String type = trap.types[i];
                    code.catch_((CompactCodeAttributeComposer.Label) trap.start.tag,
                            (CompactCodeAttributeComposer.Label) trap.end.tag,
                            (CompactCodeAttributeComposer.Label) trap.handlers[i].tag,
                            type == null ? null : toInternal(type),
                            null);
                }
            }
        }
    }

    static String toInternal(String n) {
        return ClassUtil.internalClassTypeFromType(n);
    }


    private CompactCodeAttributeComposer reBuildInstructions(IrMethod ir, CompactCodeAttributeComposer code) {
        //asm = new LdcOptimizeAdapter(asm);
        int maxLocalIndex = 0;
        for (Local local : ir.locals) {
            maxLocalIndex = Math.max(maxLocalIndex, local._ls_index);
        }
        Map<String, Integer> lockMap = new LinkedHashMap<>();
        int[] mutableMaxLocalIndex = new int[]{maxLocalIndex};
        for (Stmt st : ir.stmts) {
            statementDepthCounter = 0;
            reBuildInstructions(ir, st, lockMap, mutableMaxLocalIndex, code);
        }

        return code;
    }


    private CompactCodeAttributeComposer reBuildInstructions(IrMethod ir, Stmt st, Map<String, Integer> lockMap, int[] mutableMaxLocalIndex, CompactCodeAttributeComposer code) {
        switch (st.st) {
            case LABEL: {
                LabelStmt labelStmt = (LabelStmt) st;
                if (labelStmt.lineNumber >= 0) {
                    code.line(labelStmt.lineNumber);
                }
                return code.label((CompactCodeAttributeComposer.Label) labelStmt.tag);
            }
            case ASSIGN: {
                E2Stmt e2 = (E2Stmt) st;
                Value v1 = e2.op1;
                Value v2 = e2.op2;
                switch (v1.vt) {
                    case LOCAL:

                        Local local = ((Local) v1);
                        int i = local._ls_index;

                        boolean skipOrg = false;
                        if (v2.vt == VT.LOCAL && (i == ((Local) v2)._ls_index)) {// check for a=a
                            skipOrg = true;
                        } else if (v1.valueType.charAt(0) == 'I') {// check for IINC
                            if (v2.vt == VT.ADD) {
                                if (isLocalWithIndex(v2.getOp1(), i) && v2.getOp2().vt == VT.CONSTANT) { // a=a+1;
                                    int increment = (Integer) ((Constant) v2.getOp2()).value;
                                    if (increment >= Short.MIN_VALUE && increment <= Short.MAX_VALUE) {
                                        code.iinc(i, increment);
                                        skipOrg = true;
                                    }
                                } else if (isLocalWithIndex(v2.getOp2(), i) && v2.getOp1().vt == VT.CONSTANT) { // a=1+a;
                                    int increment = (Integer) ((Constant) v2.getOp1()).value;
                                    if (increment >= Short.MIN_VALUE && increment <= Short.MAX_VALUE) {
                                        code.iinc(i, increment);
                                        skipOrg = true;
                                    }
                                }
                            } else if (v2.vt == VT.SUB) {
                                if (isLocalWithIndex(v2.getOp1(), i) && v2.getOp2().vt == VT.CONSTANT) { // a=a-1;
                                    int increment = -(Integer) ((Constant) v2.getOp2()).value;
                                    if (increment >= Short.MIN_VALUE && increment <= Short.MAX_VALUE) {
                                        code.iinc(i, increment);
                                        skipOrg = true;
                                    }
                                }
                            }
                        }
                        if (!skipOrg) {
                            accept(v2, code);
                            if (i >= 0) {
                                return xstore(i, v1, code);
                            } else if (!v1.valueType.equals("V")) { // skip void type locals
                                switch (v1.valueType.charAt(0)) {
                                    case 'J':
                                    case 'D':
                                        return code.pop2();
                                    default:
                                        return code.pop();
                                }
                            }
                        }
                        return code;
                    case STATIC_FIELD: {
                        StaticFieldExpr fe = (StaticFieldExpr) v1;
                        accept(v2, code);
                        insertI2x(v2.valueType, fe.type, code);
                        return code.putstatic(toInternal(fe.owner), fe.name, fe.type);
                    }
                    case FIELD: {
                        FieldExpr fe = (FieldExpr) v1;
                        accept(fe.op, code);
                        accept(v2, code);
                        insertI2x(v2.valueType, fe.type, code);
                        return code.putfield(toInternal(fe.owner), fe.name, fe.type);
                    }
                    case ARRAY:
                        ArrayExpr ae = (ArrayExpr) v1;
                        accept(ae.op1, code);
                        accept(ae.op2, code);
                        accept(v2, code);
                        String tp1 = ae.op1.valueType;
                        String tp2 = ae.valueType;
                        if (tp1.charAt(0) == '[') {
                            String arrayElementType = tp1.substring(1);
                            insertI2x(v2.valueType, arrayElementType, code);
                            return xastore(arrayElementType, code);
                        } else {
                            return xastore(tp2, code);
                        }
                    default:
                        throw new RuntimeException(v1.vt.toString());
                }
            }

            case IDENTITY: {
                E2Stmt e2 = (E2Stmt) st;
                if (e2.op2.vt == VT.EXCEPTION_REF) {
                    int index = ((Local) e2.op1)._ls_index;
                    if (index >= 0) {
                        return code.astore(index);
                    } else {
                        return code.pop();
                    }
                }
                return code;
            }

            case FILL_ARRAY_DATA: {
                E2Stmt e2 = (E2Stmt) st;
                if (e2.getOp2().vt == VT.CONSTANT) {
                    Object arrayData = ((Constant) e2.getOp2()).value;
                    int arraySize = Array.getLength(arrayData);

                    String arrayValueType = e2.getOp1().valueType;
                    String elementType = arrayValueType.charAt(0) == '[' ?
                            arrayValueType.substring(1) :
                            "I";

                    // convert small arrays to unrolled loop
                    if (!usePrimitiveArrayConstants || arraySize < 0x200) {
                        accept(e2.getOp1(), code);
                        for (int i = 0; i < arraySize; i++) {
                            code.dup()
                                    .ldc(i);
                            pushConstant(Array.get(arrayData, i), code);
                            xastore(elementType, code);
                        }
                        return code.pop();
                    } else {
                        // call to arrayCopy(arrayData, 0, leftOperand, 0, arraySize)

                        if (arrayData instanceof short[] && elementType.equals("C")) {
                            // fill-array-data is short[] but the actual array should be char[]
                            // TODO(#T6572): other incorrect type conversions?
                            char[] charArray = new char[arraySize];
                            for (int i = 0; i < arraySize; i++) {
                                short digit = (Short) Array.get(arrayData, i);
                                charArray[i] = (char) digit;
                            }
                            code.ldc(charArray);
                        } else {
                            code.ldc(arrayData); // puts a new array on the stack
                        }
                        code.iconst_0();
                        accept(e2.getOp1(), code);
                        code.iconst_0();
                        code.pushInt(arraySize);
                        code.invokestatic("java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
                        return code;
                    }
                } else {
                    // TODO(T5466): potentially refactor to create ProGuardCORE primitive arrays
                    FilledArrayExpr filledArrayExpr = (FilledArrayExpr) e2.getOp2();
                    int arraySize = filledArrayExpr.ops.length;
                    String arrayValueType = e2.getOp1().valueType;
                    String elementType = arrayValueType.charAt(0) == '[' ?
                            arrayValueType.substring(1) :
                            "I";
                    accept(e2.getOp1(), code);
                    for (int i = 0; i < arraySize; i++) {
                        code.dup()
                                .ldc(i);
                        accept(filledArrayExpr.ops[i], code);
                        xastore(elementType, code);
                    }
                    return code.pop();
                }
            }

            case GOTO:
                return code.goto_((CompactCodeAttributeComposer.Label) ((GotoStmt) st).target.tag);
            case IF:
                return reBuildJumpInstructions((IfStmt) st, code);
            case LOCK: {
                Value v = ((UnopStmt) st).op;
                accept(v, code);
                if (optimizeSynchronized) {
                    switch (v.vt) {
                        case LOCAL:
                            // FIXME do we have to disable local due to OptSyncTest ?
                            // break;
                        case CONSTANT: {
                            String key;
                            if (v.vt == VT.LOCAL) {
                                key = "L" + ((Local) v)._ls_index;
                            } else {
                                key = "C" + ((Constant) v).value;
                            }
                            Integer integer = lockMap.get(key);
                            int nIndex = integer != null ? integer : ++mutableMaxLocalIndex[0];
                            code.dup();
                            xstore(nIndex, v, code);
                            lockMap.put(key, nIndex);
                            break;
                        }
                        default:
                            throw new RuntimeException(v.vt.toString());
                    }
                }
                return code.monitorenter();
            }
            case UNLOCK: {
                Value v = ((UnopStmt) st).op;
                if (optimizeSynchronized) {
                    switch (v.vt) {
                        case LOCAL:
                        case CONSTANT: {
                            String key = v.vt == VT.LOCAL ?
                                    "L" + ((Local) v)._ls_index :
                                    "C" + ((Constant) v).value;
                            Integer integer = lockMap.get(key);
                            return integer != null ?
                                    xload(integer, v, code) :
                                    accept(v, code);
                        }
                        // TODO other
                        default:
                            return accept(v, code);
                    }
                } else {
                    accept(v, code);
                }
                return code.monitorexit();
            }
            case NOP:
                return code;
            case RETURN: {
                Value v = ((UnopStmt) st).op;
                accept(v, code);
                insertI2x(v.valueType, ir.ret, code);
                return xreturn(v, code);
            }
            case RETURN_VOID:
                return code.return_();
            case LOOKUP_SWITCH: {
                LookupSwitchStmt lss = (LookupSwitchStmt) st;
                accept(lss.op, code);
                CompactCodeAttributeComposer.Label[] targets =
                        new CompactCodeAttributeComposer.Label[lss.targets.length];
                for (int i = 0; i < targets.length; i++) {
                    targets[i] = (CompactCodeAttributeComposer.Label) lss.targets[i].tag;
                }
                return code.lookupswitch((CompactCodeAttributeComposer.Label) lss.defaultTarget.tag,
                        lss.lookupValues,
                        targets);
            }
            case TABLE_SWITCH: {
                TableSwitchStmt tss = (TableSwitchStmt) st;
                accept(tss.op, code);
                CompactCodeAttributeComposer.Label[] targets =
                        new CompactCodeAttributeComposer.Label[tss.targets.length];
                for (int i = 0; i < targets.length; i++) {
                    targets[i] = (CompactCodeAttributeComposer.Label) tss.targets[i].tag;
                }
                return code.tableswitch((CompactCodeAttributeComposer.Label) tss.defaultTarget.tag,
                        tss.lowIndex,
                        tss.lowIndex + targets.length - 1,
                        targets);
            }
            case THROW:
                accept(((UnopStmt) st).op, code);
                return code.athrow();
            case VOID_INVOKE:
                Value op = st.getOp();
                accept(op, code);

                String ret = op.valueType;
                if (op.vt == VT.INVOKE_NEW) {
                    return code.pop();
                } else if (!"V".equals(ret)) {
                    switch (ret.charAt(0)) {
                        case 'J':
                        case 'D':
                            return code.pop2();
                        default:
                            return code.pop();
                    }
                }
                return code;
            default:
                throw new RuntimeException(st.st.toString());
        }
    }

    private static boolean isLocalWithIndex(Value v, int i) {
        return v.vt == VT.LOCAL && ((Local) v)._ls_index == i;
    }

    /**
     * insert I2x instruction
     *
     * @param tos
     * @param expect
     * @param code
     */
    private CompactCodeAttributeComposer insertI2x(String tos, String expect, CompactCodeAttributeComposer code) {
        switch (expect.charAt(0)) {
            case 'B':
                switch (tos.charAt(0)) {
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.i2b();
                }
            case 'S':
                switch (tos.charAt(0)) {
                    case 'C':
                    case 'I':
                        return code.i2s();
                }
            case 'C':
                switch (tos.charAt(0)) {
                    case 'I':
                        return code.i2c();
                }
        }

        return code;
    }

    static boolean isZeroOrNull(Value v1) {
        if (v1.vt == VT.CONSTANT) {
            Object v = ((Constant) v1).value;
            return Integer.valueOf(0).equals(v) || Constant.Null.equals(v);
        }
        return false;
    }

    private CompactCodeAttributeComposer reBuildJumpInstructions(IfStmt st, CompactCodeAttributeComposer code) {
        CompactCodeAttributeComposer.Label target =
                (CompactCodeAttributeComposer.Label) st.target.tag;
        Value v = st.op;
        Value v1 = v.getOp1();
        Value v2 = v.getOp2();

        String type = v1.valueType;

        boolean isZeroOrNullV2 = isZeroOrNull(v2);

        switch (type.charAt(0)) {
            case '[':
            case 'L':
                // IF_ACMPx
                // IF[non]null
                if (isZeroOrNull(v1) || isZeroOrNullV2) { // IF[non]null
                    if (isZeroOrNullV2) {// v2 is null
                        accept(v1, code);
                    } else {
                        accept(v2, code);
                    }
                    return v.vt == VT.EQ ?
                            code.ifnull(target) :
                            code.ifnonnull(target);
                } else {
                    accept(v1, code);
                    accept(v2, code);
                    return v.vt == VT.EQ ?
                            code.ifacmpeq(target) :
                            code.ifacmpne(target);
                }
            default:
                // IFx
                // IF_ICMPx
                if (isZeroOrNull(v1) || isZeroOrNullV2) { // IFx
                    if (isZeroOrNullV2) {// v2 is zero
                        accept(v1, code);
                    } else {
                        accept(v2, code);
                    }
                    switch (v.vt) {
                        case NE:
                            return code.ifne(target);
                        case EQ:
                            return code.ifeq(target);
                        case GE:
                            return isZeroOrNullV2 ? code.ifge(target) : code.ifle(target);
                        case GT:
                            return isZeroOrNullV2 ? code.ifgt(target) : code.iflt(target);
                        case LE:
                            return isZeroOrNullV2 ? code.ifle(target) : code.ifge(target);
                        case LT:
                            return isZeroOrNullV2 ? code.iflt(target) : code.ifgt(target);
                        default:
                            throw new RuntimeException(v.vt.toString());
                    }
                } else { // IF_ICMPx
                    accept(v1, code);
                    accept(v2, code);
                    switch (v.vt) {
                        case NE:
                            return code.ificmpne(target);
                        case EQ:
                            return code.ificmpeq(target);
                        case GE:
                            return code.ificmpge(target);
                        case GT:
                            return code.ificmpgt(target);
                        case LE:
                            return code.ificmple(target);
                        case LT:
                            return code.ificmplt(target);
                        default:
                            throw new RuntimeException(v1.vt.toString());
                    }
                }
        }
    }


    private CompactCodeAttributeComposer accept(Value value, CompactCodeAttributeComposer code) {
        if (MAX_STATEMENT_DEPTH > 0 && statementDepthCounter > MAX_STATEMENT_DEPTH) {
            throw new StatementDepthException();
        }
        statementDepthCounter++;

        switch (value.et) {
            case E0:
                switch (value.vt) {
                    case LOCAL:
                        return xload(((Local) value)._ls_index, value, code);
                    case CONSTANT:
                        Constant cst = (Constant) value;
                        if (cst.value.equals(Constant.Null)) {
                            return code.aconst_null();
                        } else if (cst.value instanceof DexType) {
                            String descriptor = ((DexType) cst.value).desc;
                            if (ClassUtil.isInternalPrimitiveType(descriptor)) {
                                return code.getstatic(
                                        ClassUtil.internalNumericClassNameFromPrimitiveType(descriptor.charAt(0)),
                                        ClassConstants.FIELD_NAME_TYPE,
                                        ClassConstants.FIELD_TYPE_TYPE
                                );
                            } else {
                                return code.ldc(toInternal(descriptor), (Clazz) null);
                            }
                        } else {
                            return pushConstant(cst.value, code);
                        }
                    case NEW:
                        return code.new_(toInternal(((NewExpr) value).type));
                    case STATIC_FIELD:
                        StaticFieldExpr sfe = (StaticFieldExpr) value;
                        return code.getstatic(toInternal(sfe.owner), sfe.name, sfe.type);
                    default:
                        throw new RuntimeException(value.vt.toString());
                }
            case E1:
                return reBuildE1Expression((E1Expr) value, code);
            case E2:
                return reBuildE2Expression((E2Expr) value, code);
            case En:
                return reBuildEnExpression((EnExpr) value, code);
            default:
                throw new RuntimeException(value.et.toString());
        }
    }


    // Based on LdcOptimizeAdapter.visitLdcInsn
    private CompactCodeAttributeComposer pushConstant(Object cst, CompactCodeAttributeComposer code) {
        if (cst == null) {
            return code.aconst_null();
        } else if (cst instanceof Boolean) {
            boolean value = (Boolean) cst;
            return code.iconst(value ? 1 : 0);
        } else if (cst instanceof Byte) {
            int value = (Byte) cst;
            return code.pushInt(value);
        } else if (cst instanceof Short) {
            int value = (Short) cst;
            return code.pushInt(value);
        } else if (cst instanceof Character) {
            int value = (Character) cst;
            return code.pushInt(value);
        } else if (cst instanceof Integer) {
            int value = (Integer) cst;
            if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE) {
                return code.pushInt(value);
            } else {
                return code.ldc(value);
            }
        } else if (cst instanceof Long) {
            long value = (Long) cst;
            if (value == 0L || value == 1L) {
                return code.lconst((int) value);
            } else {
                return code.ldc2_w(value);
            }
        } else if (cst instanceof Float) {
            float value = (Float) cst;
            if (value == 0.0F ||
                    value == 1.0F ||
                    value == 2.0F) {
                return code.fconst((int) value);
            } else {
                return code.ldc((float) value);
            }
        } else if (cst instanceof Double) {
            double value = (Double) cst;
            if (value == 0.0D ||
                    value == 1.0D) {
                return code.dconst((int) value);
            } else {
                return code.ldc2_w((double) value);
            }
        } else if (cst instanceof String) {
            return code.ldc((String) cst);
        }

        throw new UnsupportedOperationException("Unsupported constant " + cst.getClass().getName() + " [" + cst + "]");
    }

    private CompactCodeAttributeComposer reBuildEnExpression(EnExpr value, CompactCodeAttributeComposer code) {
        if (value.vt == VT.FILLED_ARRAY) {
            FilledArrayExpr fae = (FilledArrayExpr) value;
            if (isConstantPrimitiveArray(fae)) {
                // Load the primitive array as a constant from the constant
                // pool (ProGuard extension).
                code.ldc(primitiveArray(fae));
            } else {
                // Create a new array and fill out its elements.
                reBuildE1Expression(Exprs.nNewArray(fae.type, Exprs.nInt(fae.ops.length)), code);
                String tp1 = fae.valueType;
                String elementType = tp1.charAt(0) == '[' ?
                        tp1.substring(1) :
                        null;

                for (int i = 0; i < fae.ops.length; i++) {
                    if (fae.ops[i] == null)
                        continue;
                    code.dup()
                            .ldc(i);
                    accept(fae.ops[i], code);
                    String tp2 = fae.ops[i].valueType;
                    if (elementType != null) {
                        insertI2x(tp2, elementType, code);
                    }
                    xastore(elementType != null ? elementType : "I", code);
                }
            }
            return code;
        }

        switch (value.vt) {
            case NEW_MULTI_ARRAY:
                for (Value vb : value.ops) {
                    accept(vb, code);
                }
                NewMutiArrayExpr nmae = (NewMutiArrayExpr) value;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < nmae.dimension; i++) {
                    sb.append('[');
                }
                sb.append(nmae.baseType);
                return code.multianewarray(sb.toString(), null, value.ops.length);
            case INVOKE_NEW:
                code.new_(toInternal(((InvokeNewExpr) value).getClassName()))
                        .dup();
                // pass through
            case INVOKE_INTERFACE:
            case INVOKE_SPECIAL:
            case INVOKE_STATIC:
            case INVOKE_VIRTUAL: {
                InvokeExpr ie = (InvokeExpr) value;
                int i = 0;
                if (value.vt != VT.INVOKE_STATIC && value.vt != VT.INVOKE_NEW) {
                    i = 1;
                    accept(value.ops[0], code);
                }
                for (int j = 0; i < value.ops.length; i++, j++) {
                    Value vb = value.ops[i];
                    accept(vb, code);
                    insertI2x(vb.valueType, ie.getArgs()[j], code);
                }

                Proto p = ie.getProto();
                if (ie.vt == VT.INVOKE_NEW) {
                    p = new Proto(p.getParameterTypes(), "V");
                }
                String className = toInternal(ie.getOwner());
                String methodName = ie.getName();
                String methodDescriptor = p.getDesc();

                switch (value.vt) {
                    case INVOKE_VIRTUAL:
                        return code.invokevirtual(className, methodName, methodDescriptor);
                    case INVOKE_INTERFACE:
                        return code.invokeinterface(className, methodName, methodDescriptor);
                    case INVOKE_NEW:
                    case INVOKE_SPECIAL:
                        return code.invokespecial(className, methodName, methodDescriptor);
                    case INVOKE_STATIC:
                        return code.invokestatic(className, methodName, methodDescriptor);
                    default:
                        throw new RuntimeException(value.vt.toString());
                }
            }
            case INVOKE_CUSTOM: {
                InvokeCustomExpr ice = (InvokeCustomExpr) value;
                String[] argTypes = ice.getProto().getParameterTypes();
                Value[] vbs = ice.getOps();
                if (argTypes.length == vbs.length) {
                    for (int i = 0; i < vbs.length; i++) {
                        Value vb = vbs[i];
                        accept(vb, code);
                        insertI2x(vb.valueType, argTypes[i], code);
                    }
                } else if (argTypes.length + 1 == vbs.length) {
                    accept(vbs[0], code);
                    for (int i = 1; i < vbs.length; i++) {
                        Value vb = vbs[i];
                        accept(vb, code);
                        insertI2x(vb.valueType, argTypes[i - 1], code);
                    }
                } else {
                    throw new RuntimeException();
                }

                // Create the bootstrap methods attribute with the bootstrap method.
                int bootStrapMethodIndex =
                        Dex2Pro.convertBootstrapMethod(code.getTargetClass(),
                                code.getConstantPoolEditor(),
                                ice.handle,
                                ice.bsmArgs);

                // Invoke it with the given name and descriptor.
                return code.invokedynamic(bootStrapMethodIndex,
                        ice.name,
                        ice.proto.getDesc(),
                        null);
            }
            case INVOKE_POLYMORPHIC: {
                InvokePolymorphicExpr ipe = (InvokePolymorphicExpr) value;
                Method m = ipe.method;
                String[] argTypes = ipe.getProto().getParameterTypes();
                Value[] vbs = ipe.getOps();
                accept(vbs[0], code);
                for (int i = 1; i < vbs.length; i++) {
                    Value vb = vbs[i];
                    accept(vb, code);
                    insertI2x(vb.valueType, argTypes[i - 1], code);
                }
                return code.invokevirtual(toInternal(m.getOwner()), m.getName(), ipe.getProto().getDesc());
            }
            default:
                throw new RuntimeException(value.vt.toString());
        }
    }


    /**
     * Returns whether the given array is a primitive array with only
     * constants.
     */
    private boolean isConstantPrimitiveArray(FilledArrayExpr fae) {

        if (!this.usePrimitiveArrayConstants) return false;

        // Is it a primitive array type, like "[I".
        if (fae.valueType.length() != 2)
            return false;

        // Are its elements constants?
        for (int i = 0; i < fae.ops.length; i++) {
            Value op = fae.ops[i];
            if (op == null || op.vt != VT.CONSTANT) {
                return false;
            }
        }

        return true;
    }


    /**
     * Converts the given FilledArrayExpr into a primitive array.
     */
    private static Object primitiveArray(FilledArrayExpr fae) {
        char valueType = fae.valueType.charAt(1);
        Value[] values = fae.ops;
        int length = values.length;

        // Some values don't have exactly the expected type, e.g. Short values for a char array.
        switch (valueType) {
            case 'Z': {
                boolean[] booleans = new boolean[length];
                for (int i = 0; i < length; i++) {
                    booleans[i] = ((Number) ((Constant) values[i]).value).byteValue() != 0;
                }
                return booleans;
            }
            case 'B': {
                byte[] bytes = new byte[length];
                for (int i = 0; i < length; i++) {
                    bytes[i] = ((Number) ((Constant) values[i]).value).byteValue();
                }
                return bytes;
            }
            case 'S': {
                short[] shorts = new short[length];
                for (int i = 0; i < length; i++) {
                    shorts[i] = ((Number) ((Constant) values[i]).value).shortValue();
                }
                return shorts;
            }
            case 'C': {
                char[] chars = new char[length];
                for (int i = 0; i < length; i++) {
                    chars[i] = (char) ((Number) ((Constant) values[i]).value).shortValue();
                }
                return chars;
            }
            case 'I': {
                int[] ints = new int[length];
                for (int i = 0; i < length; i++) {
                    ints[i] = ((Number) ((Constant) values[i]).value).intValue();
                }
                return ints;
            }
            case 'F': {
                float[] floats = new float[length];
                for (int i = 0; i < length; i++) {
                    floats[i] = ((Number) ((Constant) values[i]).value).floatValue();
                }
                return floats;
            }
            case 'J': {
                long[] longs = new long[length];
                for (int i = 0; i < length; i++) {
                    longs[i] = ((Number) ((Constant) values[i]).value).longValue();
                }
                return longs;
            }
            case 'D': {
                double[] doubles = new double[length];
                for (int i = 0; i < length; i++) {
                    doubles[i] = ((Number) ((Constant) values[i]).value).doubleValue();
                }
                return doubles;
            }
            default:
                throw new IllegalArgumentException("Unsupported type " + valueType);
        }
    }


    /**
     * Creates a primitive array of the given type (for example "[I")
     * and length.
     */
    private static Object primitiveArray(String valueType, int length) {
        switch (valueType.charAt(1)) {
            case 'Z':
                return new boolean[length];
            case 'B':
                return new byte[length];
            case 'S':
                return new short[length];
            case 'C':
                return new char[length];
            case 'I':
                return new int[length];
            case 'F':
                return new float[length];
            case 'J':
                return new long[length];
            case 'D':
                return new double[length];
            default:
                throw new IllegalArgumentException(valueType);
        }
    }


    private CompactCodeAttributeComposer box(String provideType, String expectedType, CompactCodeAttributeComposer code) {
        if (provideType.equals(expectedType)) {
            return code;
        }
        if (expectedType.equals("V")) {
            switch (provideType.charAt(0)) {
                case 'J':
                case 'D':
                    return code.pop2();
                default:
                    return code.pop();
            }
        }

        char p = provideType.charAt(0);
        char e = expectedType.charAt(0);

        if (expectedType.equals("Ljava/lang/Object;") && (p == '[' || p == 'L')) {
            return code;
        }
        if (provideType.equals("Ljava/lang/Object;") && (e == '[' || e == 'L')) {
            return code.checkcast(toInternal(expectedType));
        }

        switch (provideType + expectedType) {
            case "ZLjava/lang/Object;":
            case "ZLjava/lang/Boolean;":
                return code.invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
            case "BLjava/lang/Object;":
            case "BLjava/lang/Byte;":
                return code.invokestatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
            case "SLjava/lang/Object;":
            case "SLjava/lang/Short;":
                return code.invokestatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
            case "CLjava/lang/Object;":
            case "CLjava/lang/Character;":
                return code.invokestatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
            case "ILjava/lang/Object;":
            case "ILjava/lang/Integer;":
                return code.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            case "FLjava/lang/Object;":
            case "FLjava/lang/Float;":
                return code.invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
            case "JLjava/lang/Object;":
            case "JLjava/lang/Long;":
                return code.invokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
            case "DLjava/lang/Object;":
            case "DLjava/lang/Double;":
                return code.invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");

            case "Ljava/lang/Object;Z":
                code.checkcast("java/lang/Boolean");
                // pass through
            case "Ljava/lang/Boolean;Z":
                return code.invokevirtual("java/lang/Boolean", "booleanValue", "()Z");

            case "Ljava/lang/Object;B":
                code.checkcast("java/lang/Byte");
                // pass through
            case "Ljava/lang/Byte;B":
                return code.invokevirtual("java/lang/Byte", "byteValue", "()B");

            case "Ljava/lang/Object;S":
                code.checkcast("java/lang/Short");
                // pass through
            case "Ljava/lang/Short;S":
                return code.invokevirtual("java/lang/Short", "shortValue", "()S");

            case "Ljava/lang/Object;C":
                code.checkcast("java/lang/Character");
                // pass through
            case "Ljava/lang/Character;C":
                return code.invokevirtual("java/lang/Character", "charValue", "()C");

            case "Ljava/lang/Object;I":
                code.checkcast("java/lang/Integer");
                // pass through
            case "Ljava/lang/Integer;I":
                return code.invokevirtual("java/lang/Integer", "intValue", "()I");

            case "Ljava/lang/Object;F":
                code.checkcast("java/lang/Float");
                // pass through
            case "Ljava/lang/Float;F":
                return code.invokevirtual("java/lang/Float", "floatValue", "()F");

            case "Ljava/lang/Object;J":
                code.checkcast("java/lang/Long");
                // pass through
            case "Ljava/lang/Long;J":
                return code.invokevirtual("java/lang/Long", "longValue", "()J");

            case "Ljava/lang/Object;D":
                code.checkcast("java/lang/Double");
                // pass through
            case "Ljava/lang/Double;D":
                return code.invokevirtual("java/lang/Double", "doubleValue", "()D");

            default:
                throw new RuntimeException("i have trouble to auto convert from " + provideType + " to " + expectedType + " currently");
        }
    }

    private CompactCodeAttributeComposer reBuildE1Expression(E1Expr e1, CompactCodeAttributeComposer code) {
        accept(e1.getOp(), code);
        switch (e1.vt) {
            case STATIC_FIELD: {
                FieldExpr fe = (FieldExpr) e1;
                return code.getstatic(toInternal(fe.owner), fe.name, fe.type);
            }
            case FIELD: {
                FieldExpr fe = (FieldExpr) e1;
                return code.getfield(toInternal(fe.owner), fe.name, fe.type);
            }
            case NEW_ARRAY: {
                TypeExpr te = (TypeExpr) e1;
                switch (te.type.charAt(0)) {
                    case '[':
                    case 'L':
                        return code.anewarray(toInternal(te.type), null);
                    case 'Z':
                        return code.newarray(Instruction.ARRAY_T_BOOLEAN);
                    case 'B':
                        return code.newarray(Instruction.ARRAY_T_BYTE);
                    case 'S':
                        return code.newarray(Instruction.ARRAY_T_SHORT);
                    case 'C':
                        return code.newarray(Instruction.ARRAY_T_CHAR);
                    case 'I':
                        return code.newarray(Instruction.ARRAY_T_INT);
                    case 'F':
                        return code.newarray(Instruction.ARRAY_T_FLOAT);
                    case 'J':
                        return code.newarray(Instruction.ARRAY_T_LONG);
                    case 'D':
                        return code.newarray(Instruction.ARRAY_T_DOUBLE);
                    default:
                        throw new RuntimeException(te.type);
                }
            }
            case CHECK_CAST:
                return code.checkcast(toInternal(((TypeExpr) e1).type));
            case INSTANCE_OF:
                return code.instanceof_(toInternal(((TypeExpr) e1).type), null);
            case CAST:
                return cast2(e1.op.valueType, ((CastExpr) e1).to, code);
            case LENGTH:
                return code.arraylength();
            case NEG:
                switch (((UnopExpr) e1).type.charAt(0)) {
                    case 'I':
                        return code.ineg();
                    case 'F':
                        return code.fneg();
                    case 'J':
                        return code.lneg();
                    case 'D':
                        return code.dneg();
                    default:
                        throw new RuntimeException(((UnopExpr) e1).toString0());
                }
            case NOT:
                switch (((UnopExpr) e1).type.charAt(0)) {
                    case 'I':
                        return code
                                .iconst_m1()
                                .ixor();
                    case 'J':
                        return code
                                .ldc2_w(-1L)
                                .lxor();
                    default:
                        throw new RuntimeException(((UnopExpr) e1).toString0());
                }
            default:
                throw new RuntimeException(e1.vt.toString());
        }
    }

    private CompactCodeAttributeComposer reBuildE2Expression(E2Expr e2, CompactCodeAttributeComposer code) {
        String type = e2.op2.valueType;
        accept(e2.op1, code);
        if ((e2.vt == VT.ADD || e2.vt == VT.SUB) && e2.op2.vt == VT.CONSTANT) {
            // [x + (-1)] to [x - 1]
            // [x - (-1)] to [x + 1]
            Constant constant = (Constant) e2.op2;
            String t = constant.valueType;
            switch (t.charAt(0)) {
                case 'Z': // These types would probably not be autoboxed as
                case 'B': // Integer instances, but it's not possible to express
                case 'S': // such shorter constants in Dalvik/Java bytecode, so
                case 'C': // they will never occur.

                case 'I': {
                    int s = (Integer) constant.value;
                    if (s < 0) {
                        code.ldc(-s);
                        return e2.vt == VT.ADD ?
                                code.isub() :
                                code.iadd();
                    }
                    break;
                }
                case 'F': {
                    float s = (Float) constant.value;
                    if (s < 0) {
                        code.ldc(-s);
                        return e2.vt == VT.ADD ?
                                code.fsub() :
                                code.fadd();
                    }
                    break;
                }
                case 'J': {
                    long s = (Long) constant.value;
                    if (s < 0) {
                        code.ldc2_w(-s);
                        return e2.vt == VT.ADD ?
                                code.lsub() :
                                code.ladd();
                    }
                    break;
                }
                case 'D': {
                    double s = (Double) constant.value;
                    if (s < 0) {
                        code.ldc2_w(-s);
                        return e2.vt == VT.ADD ?
                                code.dsub() :
                                code.dadd();
                    }
                    break;
                }
            }
        }

        accept(e2.op2, code);

        String tp1 = e2.op1.valueType;
        switch (e2.vt) {
            case ARRAY:
                String tp2 = e2.valueType;
                type = tp1.charAt(0) == '[' ?
                        tp1.substring(1) :
                        tp2;

                switch (type.charAt(0)) {
                    case 'L':
                    case '[':
                        return code.aaload();
                    case 'Z':
                    case 'B':
                        return code.baload();
                    case 'S':
                        return code.saload();
                    case 'C':
                        return code.caload();
                    case 'I':
                        return code.iaload();
                    case 'F':
                        return code.faload();
                    case 'J':
                        return code.laload();
                    case 'D':
                        return code.daload();
                    default:
                        throw new RuntimeException(type);
                }
            case ADD:
                switch (type.charAt(0)) {
                    case 'Z':
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.iadd();
                    case 'F':
                        return code.fadd();
                    case 'J':
                        return code.ladd();
                    case 'D':
                        return code.dadd();
                    default:
                        throw new RuntimeException(type);
                }
            case SUB:
                switch (type.charAt(0)) {
                    case 'Z':
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.isub();
                    case 'F':
                        return code.fsub();
                    case 'J':
                        return code.lsub();
                    case 'D':
                        return code.dsub();
                    default:
                        throw new RuntimeException(type);
                }
            case IDIV:
                return code.idiv();
            case LDIV:
                return code.ldiv();
            case FDIV:
                return code.fdiv();
            case DDIV:
                return code.ddiv();
            case MUL:
                switch (type.charAt(0)) {
                    case 'Z':
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.imul();
                    case 'F':
                        return code.fmul();
                    case 'J':
                        return code.lmul();
                    case 'D':
                        return code.dmul();
                    default:
                        throw new RuntimeException(type);
                }
            case REM:
                switch (type.charAt(0)) {
                    case 'Z':
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.irem();
                    case 'F':
                        return code.frem();
                    case 'J':
                        return code.lrem();
                    case 'D':
                        return code.drem();
                    default:
                        throw new RuntimeException(type);
                }
            case AND:
                switch (type.charAt(0)) {
                    case 'Z':
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.iand();
                    case 'J':
                        return code.land();
                    default:
                        throw new RuntimeException(type);
                }
            case OR:
                switch (type.charAt(0)) {
                    case 'Z':
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.ior();
                    case 'J':
                        return code.lor();
                    default:
                        throw new RuntimeException(type);
                }
            case XOR:
                switch (type.charAt(0)) {
                    case 'Z':
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.ixor();
                    case 'J':
                        return code.lxor();
                    default:
                        throw new RuntimeException(type);
                }
            case SHL:
                switch (tp1.charAt(0)) {
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.ishl();
                    case 'J':
                        return code.lshl();
                    default:
                        throw new RuntimeException(tp1);
                }
            case SHR:
                switch (tp1.charAt(0)) {
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.ishr();
                    case 'J':
                        return code.lshr();
                    default:
                        throw new RuntimeException(tp1);
                }
            case USHR:
                switch (tp1.charAt(0)) {
                    case 'B':
                    case 'S':
                    case 'C':
                    case 'I':
                        return code.iushr();
                    case 'J':
                        return code.lushr();
                    default:
                        throw new RuntimeException(tp1);
                }
            case LCMP:
                return code.lcmp();
            case FCMPG:
                return code.fcmpg();
            case DCMPG:
                return code.dcmpg();
            case FCMPL:
                return code.fcmpl();
            case DCMPL:
                return code.dcmpl();
            default:
                throw new RuntimeException(e2.vt.toString());
        }
    }

    private CompactCodeAttributeComposer cast2(String t1, String t2, CompactCodeAttributeComposer code) {
        if (t1.equals(t2)) {
            return code;
        }
        switch (t1.charAt(0)) {
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                switch (t2.charAt(0)) {
                    case 'F':
                        return code.i2f();
                    case 'J':
                        return code.i2l();
                    case 'D':
                        return code.i2d();
                    case 'C':
                        return code.i2c();
                    case 'B':
                        return code.i2b();
                    case 'S':
                        return code.i2s();
                    default:
                        throw new RuntimeException(t2);
                }
            case 'J':
                switch (t2.charAt(0)) {
                    case 'I':
                        return code.l2i();
                    case 'F':
                        return code.l2f();
                    case 'D':
                        return code.l2d();
                    default:
                        throw new RuntimeException(t2);
                }
            case 'D':
                switch (t2.charAt(0)) {
                    case 'I':
                        return code.d2i();
                    case 'F':
                        return code.d2f();
                    case 'J':
                        return code.d2l();
                    default:
                        throw new RuntimeException(t2);
                }
            case 'F':
                switch (t2.charAt(0)) {
                    case 'I':
                        return code.f2i();
                    case 'J':
                        return code.f2l();
                    case 'D':
                        return code.f2d();
                    default:
                        throw new RuntimeException(t2);
                }
            default:
                throw new RuntimeException(t1);
        }
    }

    private CompactCodeAttributeComposer xstore(int var, Value v, CompactCodeAttributeComposer code) {
        return xstore(var, v.valueType, code);
    }

    private CompactCodeAttributeComposer xstore(int var, String v, CompactCodeAttributeComposer code) {
        switch (v.charAt(0)) {
            case 'L':
            case '[':
                return code.astore(var);
            case 'Z':
            case 'B':
            case 'S':
            case 'C':
            case 'I':
                return code.istore(var);
            case 'F':
                return code.fstore(var);
            case 'J':
                return code.lstore(var);
            case 'D':
                return code.dstore(var);
            default:
                // FIXME handle undetected types
                throw new RuntimeException(v);
        }
    }

    private CompactCodeAttributeComposer xload(int var, Value v, CompactCodeAttributeComposer code) {
        return xload(var, v.valueType, code);
    }

    private CompactCodeAttributeComposer xload(int var, String v, CompactCodeAttributeComposer code) {
        switch (v.charAt(0)) {
            case 'L':
            case '[':
                return code.aload(var);
            case 'Z':
            case 'B':
            case 'S':
            case 'C':
            case 'I':
                return code.iload(var);
            case 'F':
                return code.fload(var);
            case 'J':
                return code.lload(var);
            case 'D':
                return code.dload(var);
            default:
                throw new RuntimeException(v);
        }
    }


    private CompactCodeAttributeComposer xastore(Value v, CompactCodeAttributeComposer code) {
        return xastore(v.valueType, code);
    }

    private CompactCodeAttributeComposer xastore(String v, CompactCodeAttributeComposer code) {
        switch (v.charAt(0)) {
            case 'L':
            case '[':
                return code.aastore();
            case 'Z':
            case 'B':
                return code.bastore();
            case 'S':
                return code.sastore();
            case 'C':
                return code.castore();
            case 'I':
                return code.iastore();
            case 'F':
                return code.fastore();
            case 'J':
                return code.lastore();
            case 'D':
                return code.dastore();
            default:
                throw new RuntimeException(v);
        }
    }


    private CompactCodeAttributeComposer xreturn(Value v, CompactCodeAttributeComposer code) {
        return xreturn(v.valueType, code);
    }

    private CompactCodeAttributeComposer xreturn(String v, CompactCodeAttributeComposer code) {
        switch (v.charAt(0)) {
            case 'L':
            case '[':
                return code.areturn();
            case 'Z':
            case 'B':
            case 'S':
            case 'C':
            case 'I':
                return code.ireturn();
            case 'F':
                return code.freturn();
            case 'J':
                return code.lreturn();
            case 'D':
                return code.dreturn();
            default:
                throw new RuntimeException(v);
        }
    }
}
