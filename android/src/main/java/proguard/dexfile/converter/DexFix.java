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
package proguard.dexfile.converter;

import proguard.dexfile.reader.DexConstants;
import proguard.dexfile.reader.Field;
import proguard.dexfile.reader.node.DexClassNode;
import proguard.dexfile.reader.node.DexFieldNode;
import proguard.dexfile.reader.node.DexFileNode;
import proguard.dexfile.reader.node.DexMethodNode;
import proguard.dexfile.reader.Op;
import proguard.dexfile.reader.visitors.DexCodeVisitor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1. Dex omit the value of static-final filed if it is the default value.
 * <p>
 * 2. static-final field init by zero, but assigned in clinit
 * <p>
 * this method is try to fix the problems.
 */
public class DexFix {
    private static final int ACC_STATIC_FINAL = DexConstants.ACC_STATIC | DexConstants.ACC_FINAL;

    public static void fixStaticFinalFieldValue(final DexFileNode dex) {
        if (dex.clzs != null) {
            for (DexClassNode classNode : dex.clzs) {
                fixStaticFinalFieldValue(classNode);
            }
        }
    }

    /**
     * init value to default if the field is static and final, and the field is not init in clinit method
     * <p>
     * erase the default value if the field is init in clinit method
     *
     * @param classNode
     */
    public static void fixStaticFinalFieldValue(final DexClassNode classNode) {
        if (classNode.fields == null) {
            return;
        }
        final Map<String, DexFieldNode> fs = new LinkedHashMap<>();
        final Map<String, DexFieldNode> shouldNotBeAssigned = new LinkedHashMap<>();
        for (DexFieldNode fn : classNode.fields) {
            if ((fn.access & ACC_STATIC_FINAL) == ACC_STATIC_FINAL) {
                if (fn.cst == null) {
                    char t = fn.field.getType().charAt(0);
                    if (t == 'L' || t == '[') {
                        // ignore Object
                        continue;
                    }
                    fs.put(fn.field.getName() + ":" + fn.field.getType(), fn);
                } else if (isPrimitiveZero(fn.field.getType(), fn.cst)) {
                    shouldNotBeAssigned.put(fn.field.getName() + ":" + fn.field.getType(), fn);
                }
            }
        }
        if (fs.isEmpty() && shouldNotBeAssigned.isEmpty()) {
            return;
        }
        DexMethodNode node = null;
        if (classNode.methods != null) {
            for (DexMethodNode mn : classNode.methods) {
                if (mn.method.getName().equals("<clinit>")) {
                    node = mn;
                    break;
                }
            }
        }
        if (node != null) {
            if (node.codeNode != null) {
                node.codeNode.accept(new DexCodeVisitor() {
                    @Override
                    public void visitFieldStmt(Op op, int a, int b, Field field) {
                        switch (op) {
                            case SPUT:
                            case SPUT_BOOLEAN:
                            case SPUT_BYTE:
                            case SPUT_CHAR:
                            case SPUT_OBJECT:
                            case SPUT_SHORT:
                            case SPUT_WIDE:
                                if (field.getOwner().equals(classNode.className)) {
                                    String key = field.getName() + ":" + field.getType();
                                    fs.remove(key);
                                    DexFieldNode dn = shouldNotBeAssigned.get(key);
                                    if (dn != null) {
                                        dn.cst = null;
                                    }
                                }
                                break;
                            default:
                                // ignored
                                break;
                        }
                    }
                });
            } else {
                // has init but no code
                return;
            }
        }

        for (DexFieldNode fn : fs.values()) {
            fn.cst = getDefaultValueOfType(fn.field.getType().charAt(0));
        }

    }

    private static Object getDefaultValueOfType(char t) {
        switch (t) {
            case 'B':
                return (byte) 0;
            case 'Z':
                return Boolean.FALSE;
            case 'S':
                return (short) 0;
            case 'C':
                return (char) 0;
            case 'I':
                return 0;
            case 'F':
                return (float) 0.0;
            case 'J':
                return (long) 0;
            case 'D':
                return 0.0;
            case '[':
            case 'L':
            default:
                return null;
            // impossible
        }
    }

    static boolean isPrimitiveZero(String desc, Object value) {
        if (value != null && desc != null && desc.length() > 0) {
            switch (desc.charAt(0)) {
                // case 'V':// VOID_TYPE
                case 'Z':// BOOLEAN_TYPE
                    return !((Boolean) value);
                case 'C':// CHAR_TYPE
                    return (Character) value == (char) 0;
                case 'B':// BYTE_TYPE
                    return (Byte) value == 0;
                case 'S':// SHORT_TYPE
                    return (Short) value == 0;
                case 'I':// INT_TYPE
                    return (Integer) value == 0;
                case 'F':// FLOAT_TYPE
                    return (Float) value == 0f;
                case 'J':// LONG_TYPE
                    return (Long) value == 0L;
                case 'D':// DOUBLE_TYPE
                    return (Double) value == 0.0;
            }
        }
        return false;
    }
}
