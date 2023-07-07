/*
 * Copyright (c) 2002-2023 Guardsquare NV
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

import proguard.analysis.Metrics;
import proguard.analysis.Metrics.MetricType;
import proguard.classfile.AccessConstants;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramField;
import proguard.classfile.ProgramMethod;
import proguard.classfile.TypeConstants;
import proguard.classfile.VersionConstants;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.BootstrapMethodInfo;
import proguard.classfile.attribute.BootstrapMethodsAttribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ConstantValueAttribute;
import proguard.classfile.attribute.EnclosingMethodAttribute;
import proguard.classfile.attribute.ExceptionsAttribute;
import proguard.classfile.attribute.InnerClassesAttribute;
import proguard.classfile.attribute.InnerClassesInfo;
import proguard.classfile.attribute.SignatureAttribute;
import proguard.classfile.attribute.SourceFileAttribute;
import proguard.classfile.attribute.annotation.Annotation;
import proguard.classfile.attribute.annotation.AnnotationDefaultAttribute;
import proguard.classfile.attribute.annotation.AnnotationElementValue;
import proguard.classfile.attribute.annotation.AnnotationsAttribute;
import proguard.classfile.attribute.annotation.ArrayElementValue;
import proguard.classfile.attribute.annotation.ClassElementValue;
import proguard.classfile.attribute.annotation.ConstantElementValue;
import proguard.classfile.attribute.annotation.ElementValue;
import proguard.classfile.attribute.annotation.EnumConstantElementValue;
import proguard.classfile.attribute.annotation.ParameterAnnotationsAttribute;
import proguard.classfile.attribute.annotation.RuntimeInvisibleAnnotationsAttribute;
import proguard.classfile.attribute.annotation.RuntimeInvisibleParameterAnnotationsAttribute;
import proguard.classfile.attribute.annotation.RuntimeVisibleAnnotationsAttribute;
import proguard.classfile.attribute.annotation.RuntimeVisibleParameterAnnotationsAttribute;
import proguard.classfile.constant.MethodHandleConstant;
import proguard.classfile.editor.AttributesEditor;
import proguard.classfile.editor.BootstrapMethodsAttributeEditor;
import proguard.classfile.editor.ClassBuilder;
import proguard.classfile.editor.CompactCodeAttributeComposer;
import proguard.classfile.editor.ConstantPoolEditor;
import proguard.classfile.editor.ExceptionsAttributeEditor;
import proguard.classfile.editor.InnerClassesAttributeEditor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassVisitor;
import proguard.dexfile.ir.IrMethod;
import proguard.dexfile.ir.ts.AggTransformer;
import proguard.dexfile.ir.ts.CleanLabel;
import proguard.dexfile.ir.ts.ExceptionHandlerTrim;
import proguard.dexfile.ir.ts.Ir2JRegAssignTransformer;
import proguard.dexfile.ir.ts.MultiArrayTransformer;
import proguard.dexfile.ir.ts.NewTransformer;
import proguard.dexfile.ir.ts.RemoveConstantFromSSA;
import proguard.dexfile.ir.ts.RemoveLocalFromSSA;
import proguard.dexfile.ir.ts.TypeTransformer;
import proguard.dexfile.ir.ts.UnSSATransformer;
import proguard.dexfile.ir.ts.VoidInvokeTransformer;
import proguard.dexfile.ir.ts.ZeroTransformer;
import proguard.dexfile.reader.DexConstants;
import proguard.dexfile.reader.DexType;
import proguard.dexfile.reader.Field;
import proguard.dexfile.reader.MethodHandle;
import proguard.dexfile.reader.Proto;
import proguard.dexfile.reader.Visibility;
import proguard.dexfile.reader.node.DexAnnotationNode;
import proguard.dexfile.reader.node.DexClassNode;
import proguard.dexfile.reader.node.DexFieldNode;
import proguard.dexfile.reader.node.DexFileNode;
import proguard.dexfile.reader.node.DexMethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Derived from dex-translator/src/main/java/com/googlecode/d2j/dex/Dex2Asm.java
// We're preserving the original code as much as possible, to allow comparing
// and patching it.

/**
 * This utility class converts Dex2jar dex files to ProGuard class files.
 */
public class Dex2Pro {

    /**
     * If set to >0, this config option has the effect that overly complex
     * methods with more than this amount of phi labels (usual/unobfuscated
     * methods have 10-20) are not converted to Java bytecode,
     * as the conversion process would not terminate in a reasonable
     * amount of time. This results in a {@link proguard.classfile.Method}
     * with an empty {@link CodeAttribute}.
     */
    private static final int MAX_PHI_LABELS = Integer.parseInt(System.getProperty("proguard.dexconversion.maxphilabels", "0"));
    private static final int MAX_CODE_LENGTH = Integer.parseInt(System.getProperty("proguard.dexconversion.maxcodelength", "10000"));

    /**
     * If the conversion to dex fails for any reason, an empty code will be returned, but the execution will not stop
     **/
    private static final boolean SKIP_UNPARSEABLE_METHODS = System.getProperty("proguard.dexconversion.skip_unparseable_methods") != null;

//    private static final Logger  logger                   = LogManager.getLogger(Dex2Pro.class);

    /**
     * Similar to {@link #MAX_PHI_LABELS}, if set to >0, methods with more statements
     * than this threshold are not converted to bytecode.
     */
    private static final int     MAX_STATEMENTS           = Integer.parseInt(System.getProperty("proguard.dexconversion.maxstatements", "0"));

    private boolean usePrimitiveArrayConstants = false;


    private static class Clz {
        private int access;
        private Clz enclosingClass;
        private proguard.dexfile.reader.Method enclosingMethod;
        private String innerName;
        private Set<Clz> inners = null;
        private final String name;

        private Clz(String name) {
            super();
            this.name = name;
        }

        void addInner(Clz clz) {
            if (inners == null) {
                inners = new LinkedHashSet<>();
            }
            inners.add(clz);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Clz))
                return false;
            Clz clz = (Clz) o;
            return Objects.equals(name, clz.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "" + name;
        }
    }

    private static final int ACC_INTERFACE_ABSTRACT = (AccessConstants.INTERFACE | AccessConstants.ABSTRACT);

    private static final int NO_CODE_MASK = DexConstants.ACC_ABSTRACT | DexConstants.ACC_NATIVE
            | DexConstants.ACC_ANNOTATION;

    /* Some of these optimizations are unused as they have been disabled to fix various issues, e.g. #3543.
     *  We leave them here so it is easy to get an overview of which optimizations initially Dex2Pro used.
     */
    private static final CleanLabel T_cleanLabel = new CleanLabel();
    private static final Ir2JRegAssignTransformer T_ir2jRegAssign = new Ir2JRegAssignTransformer();
    private static final NewTransformer T_new = new NewTransformer();
    private static final RemoveConstantFromSSA T_removeConst = new RemoveConstantFromSSA();
    private static final RemoveLocalFromSSA T_removeLocal = new RemoveLocalFromSSA();
    private static final ExceptionHandlerTrim T_trimEx = new ExceptionHandlerTrim();
    private static final TypeTransformer T_type = new TypeTransformer();
    private static final AggTransformer T_agg = new AggTransformer();
    private static final UnSSATransformer T_unssa = new UnSSATransformer();
    private static final ZeroTransformer T_zero = new ZeroTransformer();
    private static final VoidInvokeTransformer T_voidInvoke = new VoidInvokeTransformer();
    private static final MultiArrayTransformer T_multiArray = new MultiArrayTransformer();


    static private int clearClassAccess(boolean isInner, int access) {
        if ((access & AccessConstants.INTERFACE) == 0) { // issue 55
            access |= AccessConstants.SUPER;// 解决生成的class文件使用dx重新转换时使用的指令与原始指令不同的问题
        }
        // access in class has no acc_static or acc_private
        access &= ~(AccessConstants.STATIC | AccessConstants.PRIVATE);
        if (isInner && (access & AccessConstants.PROTECTED) != 0) {// protected inner class are public
            access &= ~AccessConstants.PROTECTED;
            access |= AccessConstants.PUBLIC;
        }
        access &= ~DexConstants.ACC_DECLARED_SYNCHRONIZED; // clean ACC_DECLARED_SYNCHRONIZED
        access &= ~DexConstants.ACC_SYNTHETIC; // clean ACC_SYNTHETIC
        return access;
    }

    static private int clearInnerAccess(int access) {
        access &= (~AccessConstants.SUPER);// inner class attr has no acc_super
        if (0 != (access & AccessConstants.PRIVATE)) {// clear public/protected if it is private
            access &= ~(AccessConstants.PUBLIC | AccessConstants.PROTECTED);
        } else if (0 != (access & AccessConstants.PROTECTED)) {// clear public if it is protected
            access &= ~(AccessConstants.PUBLIC);
        }
        access &= ~DexConstants.ACC_SYNTHETIC; // clean ACC_SYNTHETIC
        return access;
    }

    private static String toInternalName(DexType type) {
        return toInternalName(type.desc);
    }

    private static String toInternalName(String desc) {
        return ClassUtil.internalClassNameFromClassType(desc);
    }


    /**
     * Adds the given Dex annotations to the given ProGuard class.
     */
    private static void convertAnnotations(List<DexAnnotationNode> anns, ProgramClass programClass, ConstantPoolEditor constantPoolEditor) {
        convertAnnotations(anns,
                constantPoolEditor,
                new AttributesEditor(programClass, false));
    }

    /**
     * Adds the given Dex annotations to the given ProGuard field.
     */
    private static void convertAnnotations(List<DexAnnotationNode> anns, ProgramClass programClass, ConstantPoolEditor constantPoolEditor, ProgramField programField) {
        convertAnnotations(anns,
                constantPoolEditor,
                new AttributesEditor(programClass, programField, false));
    }

    /**
     * Adds the given Dex annotations to the given ProGuard method.
     */
    private static void convertAnnotations(List<DexAnnotationNode> anns, ProgramClass programClass, ConstantPoolEditor constantPoolEditor, ProgramMethod programMethod) {
        convertAnnotations(anns,
                constantPoolEditor,
                new AttributesEditor(programClass, programMethod, false));
    }

    /**
     * Adds the given Dex annotations as annotations attributes, with the given
     * editor.
     */
    private static void convertAnnotations(List<DexAnnotationNode> anns, ConstantPoolEditor constantPoolEditor, AttributesEditor attributesEditor) {
        convertAnnotations(Visibility.BUILD, anns, constantPoolEditor, attributesEditor);
        convertAnnotations(Visibility.RUNTIME, anns, constantPoolEditor, attributesEditor);
    }

    /**
     * Adds the Dex annotations with the given visibility as a corresponding
     * runtime visible/invisible annotations attributes, with the given editor.
     */
    private static void convertAnnotations(Visibility visibility, List<DexAnnotationNode> anns, ConstantPoolEditor constantPoolEditor, AttributesEditor attributesEditor) {
        // Only consider annotations with the right visibility.
        anns = anns.stream().filter(ann -> ann.visibility == visibility).collect(Collectors.toList());

        if (!anns.isEmpty()) {
            // Create the annotations.
            Annotation[] annotations = new Annotation[anns.size()];
            for (int index = 0; index < anns.size(); index++) {
                DexAnnotationNode dexAnnotationNode = anns.get(index);
                annotations[index] = convertAnnotation(dexAnnotationNode, constantPoolEditor);
            }

            // Create the annotations attribute.
            AnnotationsAttribute annotationsAttribute = visibility == Visibility.BUILD ?
                    new RuntimeInvisibleAnnotationsAttribute(constantPoolEditor.addUtf8Constant(Attribute.RUNTIME_INVISIBLE_ANNOTATIONS),
                            annotations.length,
                            annotations) :
                    new RuntimeVisibleAnnotationsAttribute(constantPoolEditor.addUtf8Constant(Attribute.RUNTIME_VISIBLE_ANNOTATIONS),
                            annotations.length,
                            annotations);

            // Add the attribute.
            attributesEditor.addAttribute(annotationsAttribute);
        }
    }

    /**
     * Adds the given Dex parameter annotations to the given ProGuard method.
     */
    private static void convertParameterAnnotations(List<DexAnnotationNode>[] anns, ProgramClass programClass, ConstantPoolEditor constantPoolEditor, ProgramMethod programMethod) {
        AttributesEditor attributesEditor =
                new AttributesEditor(programClass, programMethod, false);

        convertParameterAnnotations(Visibility.BUILD, anns, constantPoolEditor, attributesEditor);
        convertParameterAnnotations(Visibility.RUNTIME, anns, constantPoolEditor, attributesEditor);
    }

    /**
     * Adds the Dex parameter annotations with the given visibility as a
     * corresponding runtime visible/invisible parameter annotations attribute,
     * with the given editor.
     */
    private static void convertParameterAnnotations(Visibility visibility, List<DexAnnotationNode>[] anns, ConstantPoolEditor constantPoolEditor, AttributesEditor attributesEditor) {
        // Only consider annotations with the right visibility.
        List<DexAnnotationNode>[] filteredAnns = new List[anns.length];
        for (int index = 0; index < anns.length; index++) {
            List<DexAnnotationNode> parameterAnns = anns[index];
            filteredAnns[index] = parameterAnns != null ?
                    parameterAnns.stream().filter(ann -> ann.visibility == visibility).collect(Collectors.toList()) :
                    Collections.emptyList();
        }

        // Only add a parameter annotation if there are any parameter
        // annotations with this visibility.
        if (hasAnnotations(filteredAnns)) {
            // Create the annotations.
            int[] annotationsCount = new int[filteredAnns.length];
            Annotation[][] annotations = new Annotation[filteredAnns.length][];
            for (int parameterIndex = 0; parameterIndex < filteredAnns.length; parameterIndex++) {
                List<DexAnnotationNode> dexAnnotationNodes = filteredAnns[parameterIndex];
                annotationsCount[parameterIndex] = dexAnnotationNodes.size();
                annotations[parameterIndex] = new Annotation[dexAnnotationNodes.size()];
                for (int index = 0; index < dexAnnotationNodes.size(); index++) {
                    annotations[parameterIndex][index] = convertAnnotation(dexAnnotationNodes.get(index), constantPoolEditor);
                }
            }

            // Create the annotations attribute.
            ParameterAnnotationsAttribute annotationsAttribute = visibility == Visibility.BUILD ?
                    new RuntimeInvisibleParameterAnnotationsAttribute(constantPoolEditor.addUtf8Constant(Attribute.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS),
                            annotations.length,
                            annotationsCount,
                            annotations) :
                    new RuntimeVisibleParameterAnnotationsAttribute(constantPoolEditor.addUtf8Constant(Attribute.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS),
                            annotations.length,
                            annotationsCount,
                            annotations);

            // Add the attribute.
            attributesEditor.addAttribute(annotationsAttribute);
        }
    }

    /**
     * Returns whether any array element has any annotations.
     */
    private static boolean hasAnnotations(List<DexAnnotationNode>[] anns) {
        for (List<DexAnnotationNode> ann : anns) {
            if (!ann.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a ProGuard annotation that corresponds to the given Dex annotation.
     */
    private static Annotation convertAnnotation(DexAnnotationNode dexAnnotationNode, ConstantPoolEditor constantPoolEditor) {
        ElementValue[] elementValues = convertElementValues(dexAnnotationNode.items, constantPoolEditor);

        return new Annotation(constantPoolEditor.addUtf8Constant(dexAnnotationNode.type),
                elementValues.length,
                elementValues);
    }

    /**
     * Returns ProGuard annotation element values that correspond to the given
     * Dex annotation items.
     */
    private static ElementValue[] convertElementValues(List<DexAnnotationNode.Item> items, ConstantPoolEditor cpe) {
        ElementValue[] elementValues = new ElementValue[items.size()];
        for (int index = 0; index < items.size(); index++) {
            DexAnnotationNode.Item item = items.get(index);
            elementValues[index] = convertElementValue(item.name,
                    item.value,
                    cpe);
        }

        return elementValues;
    }

    /**
     * Returns a ProGuard annotation element value that corresponds to the given
     * Dex constant.
     */
    private static ElementValue convertElementValue(String elementName, Object o, ConstantPoolEditor cpe) {
        return convertElementValue(cpe.addUtf8Constant(elementName), o, cpe);
    }

    /**
     * Returns a ProGuard annotation element value that corresponds to the given
     * Dex constant.
     */
    private static ElementValue convertElementValue(int elementNameIndex, Object o, ConstantPoolEditor cpe) {
        if (o instanceof Boolean) {
            return new ConstantElementValue(TypeConstants.BOOLEAN,
                    elementNameIndex,
                    cpe.addIntegerConstant((Boolean) o ? 1 : 0));
        } else if (o instanceof Byte) {
            return new ConstantElementValue(TypeConstants.BYTE,
                    elementNameIndex,
                    cpe.addIntegerConstant(((Byte) o).intValue()));
        } else if (o instanceof Short) {
            return new ConstantElementValue(TypeConstants.SHORT,
                    elementNameIndex,
                    cpe.addIntegerConstant(((Short) o).intValue()));
        } else if (o instanceof Character) {
            return new ConstantElementValue(TypeConstants.CHAR,
                    elementNameIndex,
                    cpe.addIntegerConstant((Character) o));
        } else if (o instanceof Integer) {
            return new ConstantElementValue(TypeConstants.INT,
                    elementNameIndex,
                    cpe.addIntegerConstant((Integer) o));
        } else if (o instanceof Long) {
            return new ConstantElementValue(TypeConstants.LONG,
                    elementNameIndex,
                    cpe.addLongConstant((Long) o));
        } else if (o instanceof Float) {
            return new ConstantElementValue(TypeConstants.FLOAT,
                    elementNameIndex,
                    cpe.addFloatConstant((Float) o));
        } else if (o instanceof Double) {
            return new ConstantElementValue(TypeConstants.DOUBLE,
                    elementNameIndex,
                    cpe.addDoubleConstant((Double) o));
        } else if (o instanceof String) {
            return new ConstantElementValue(ElementValue.TAG_STRING_CONSTANT,
                    elementNameIndex,
                    cpe.addUtf8Constant((String) o));
        } else if (o instanceof Object[]) {
            Object[] array = (Object[]) o;
            ElementValue[] values = new ElementValue[array.length];
            for (int index = 0; index < array.length; index++) {
                values[index] = convertElementValue(0, array[index], cpe);
            }
            return new ArrayElementValue(elementNameIndex, values.length, values);

        } else if (o instanceof DexAnnotationNode) {
            DexAnnotationNode ann = (DexAnnotationNode) o;
            return new AnnotationElementValue(elementNameIndex,
                    convertAnnotation(ann, cpe));
        } else if (o instanceof Field) {
            Field f = (Field) o;
            return new EnumConstantElementValue(elementNameIndex,
                    cpe.addUtf8Constant(f.getType()),
                    cpe.addUtf8Constant(f.getName()));
        } else if (o instanceof DexType) {
            return new ClassElementValue(elementNameIndex,
                    cpe.addUtf8Constant(((DexType) o).desc));
        } else if (o instanceof proguard.dexfile.reader.Method) {
            System.err.println("WARN: ignored method annotation value");
        } else if (o == null) {
            System.err.println("WARN: ignored null annotation value");
        }

        throw new UnsupportedOperationException("Unsupported element value " + (o == null ? "null class" : o.getClass().getName()) + " [" + o + "]");
    }

    private static Map<String, Clz> collectClzInfo(DexFileNode fileNode) {
        Map<String, Clz> classes = new LinkedHashMap<>();
        for (DexClassNode classNode : fileNode.clzs) {
            Clz clz = get(classes, classNode.className);
            clz.access = (clz.access & ~ACC_INTERFACE_ABSTRACT) | classNode.access;
            if (classNode.anns != null) {
                for (DexAnnotationNode ann : classNode.anns) {
                    if (ann.visibility == Visibility.SYSTEM) {
                        switch (ann.type) {
                            case DexConstants.ANNOTATION_ENCLOSING_CLASS_TYPE: {
                                DexType type = (DexType) findAnnotationAttribute(ann, "value");
                                Clz enclosingClass = get(classes, Objects.requireNonNull(type).desc);
                                clz.enclosingClass = enclosingClass;

                                // apply patch from ChaeHoon Lim,
                                // obfuscated code may declare itself as enclosing class
                                // which cause dex2jar to endless loop
                                //if(!clz.name.equals(clz.enclosingClass.name)) {
                                //    enclosingClass.addInner(clz);
                                //}
                                enclosingClass.addInner(clz);

                            }
                            break;
                            case DexConstants.ANNOTATION_ENCLOSING_METHOD_TYPE: {
                                proguard.dexfile.reader.Method m = (proguard.dexfile.reader.Method) findAnnotationAttribute(ann, "value");
                                Clz enclosingClass = get(classes, Objects.requireNonNull(m).getOwner());
                                clz.enclosingClass = enclosingClass;
                                clz.enclosingMethod = m;
                                enclosingClass.addInner(clz);
                            }
                            break;
                            case DexConstants.ANNOTATION_INNER_CLASS_TYPE: {
                                for (DexAnnotationNode.Item it : ann.items) {
                                    if ("accessFlags".equals(it.name)) {
                                        clz.access |= (Integer) it.value & ~ACC_INTERFACE_ABSTRACT;
                                    } else if ("name".equals(it.name)) {
                                        clz.innerName = (String) it.value;
                                    }
                                }
                            }
                            break;
                            case DexConstants.ANNOTATION_MEMBER_CLASSES_TYPE: {
                                Object[] ts = (Object[]) findAnnotationAttribute(ann, "value");
                                for (Object v : ts) {
                                    DexType type = (DexType) v;
                                    Clz inner = get(classes, type.desc);
                                    clz.addInner(inner);
                                    inner.enclosingClass = clz;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        return classes;
    }

    private static boolean isJavaIdentifier(String str) {
        if (str.length() < 1) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(str.charAt(0))) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public Dex2Pro usePrimitiveArrayConstants(boolean usePrimitiveArrayConstants) {
        this.usePrimitiveArrayConstants = usePrimitiveArrayConstants;
        return this;
    }


    /**
     * Converts the given Dex to classes and applies the given class visitor to them.
     */
    public void convertDex(DexFileNode fileNode, ClassVisitor classVisitor) {
        if (fileNode.clzs != null) {
            Map<String, Clz> classes = collectClzInfo(fileNode);
            for (DexClassNode classNode : fileNode.clzs) {
                convertClass(fileNode, classNode, classVisitor, classes);
            }
        }
    }

    /**
     * Converts the given Dex class and applies the given class visitor to it.
     */
    private void convertClass(DexFileNode dfn, DexClassNode classNode, ClassVisitor classVisitor, Map<String, Clz> classes) {
        convertClass(dfn.dexVersion, classNode, classVisitor, classes);
    }

    /**
     * Converts the given Dex class and applies the given class visitor to it.
     */
    private void convertClass(int dexVersion, DexClassNode classNode, ClassVisitor classVisitor, Map<String, Clz> classes) {

        // the default value of static-final field are omitted by dex, fix it
        DexFix.fixStaticFinalFieldValue(classNode);

        Clz clzInfo = classes.get(classNode.className);
        int access = classNode.access;
        boolean isInnerClass = false;
        if (clzInfo != null) {
//            isInnerClass = clzInfo.enclosingClass != null || clzInfo.enclosingMethod != null;
            if (clzInfo.enclosingClass != null || clzInfo.enclosingMethod != null) {
                if (classNode.anns != null && classNode.anns.stream().noneMatch(x ->
                        x.type.equals("Ldalvik/annotation/EnclosingMethod;"))) {
                    isInnerClass = true;
                } else if (clzInfo.enclosingClass != null) {
                    clzInfo.enclosingClass.inners.remove(clzInfo);
                }
            }
        }
        access = clearClassAccess(isInnerClass, access);

        int version = dexVersion >= DexConstants.DEX_037 ? VersionConstants.CLASS_VERSION_1_8 : VersionConstants.CLASS_VERSION_1_6;

        // Create the class.
        ClassBuilder classBuilder =
                new ClassBuilder(version,
                        access,
                        toInternalName(classNode.className),
                        classNode.superClass == null ? null :
                                toInternalName(classNode.superClass));

        ProgramClass programClass = classBuilder.getProgramClass();
        ConstantPoolEditor constantPoolEditor = classBuilder.getConstantPoolEditor();

        // Add the interfaces.
        String[] interfaceInterNames = null;
        if (classNode.interfaceNames != null) {
            for (int i = 0; i < classNode.interfaceNames.length; i++) {
                classBuilder.addInterface(toInternalName(classNode.interfaceNames[i]));
            }
        }

        // Add the signature attribute.
        if (classNode.anns != null) {
            for (DexAnnotationNode ann : classNode.anns) {
                if (ann.visibility == Visibility.SYSTEM) {
                    if (DexConstants.ANNOTATION_SIGNATURE_TYPE.equals(ann.type)) {
                        Object[] strs = (Object[]) findAnnotationAttribute(ann, "value");
                        if (strs != null) {
                            StringBuilder sb = new StringBuilder();
                            for (Object str : strs) {
                                sb.append(str);
                            }
                            String signature = sb.toString();

                            // Add the attribute to the class.
                            new AttributesEditor(programClass, false)
                                    .addAttribute(new SignatureAttribute(constantPoolEditor.addUtf8Constant(Attribute.SIGNATURE),
                                            constantPoolEditor.addUtf8Constant(signature)));
                        }
                    }
                }
            }
        }

        // Add the enclosing method attribute and inner classes attribute.
        if (clzInfo != null) {
            AttributesEditor attributesEditor =
                    new AttributesEditor(programClass, false);

            List<InnerClassNode> innerClassNodes = new ArrayList<>(5);
            if (isInnerClass) {
                // Add the enclosing method attribute, for enclosed classes
                // and for anonymous classes.
                proguard.dexfile.reader.Method enclosingMethod = clzInfo.enclosingMethod;
                if (enclosingMethod != null ||
                        clzInfo.innerName == null) {
                    EnclosingMethodAttribute enclosingMethodAttribute =
                            new EnclosingMethodAttribute(constantPoolEditor.addUtf8Constant(Attribute.ENCLOSING_METHOD),
                                    constantPoolEditor.addClassConstant(toInternalName(clzInfo.enclosingClass.name), null),
                                    enclosingMethod == null ? 0 :
                                            constantPoolEditor.addNameAndTypeConstant(enclosingMethod.getName(),
                                                    enclosingMethod.getDesc()));
                    attributesEditor.addAttribute(enclosingMethodAttribute);
                }

                // Collect all transitive outer classes.
                searchEnclosing(clzInfo, innerClassNodes);
            }

            // Collect the immediate inner classes.
            searchInnerClass(clzInfo, innerClassNodes, classNode.className);

            // Add the inner classes attribute, if any.
            if (!innerClassNodes.isEmpty()) {
                InnerClassesAttribute innerClassesAttribute =
                        new InnerClassesAttribute(constantPoolEditor.addUtf8Constant(Attribute.INNER_CLASSES),
                                0,
                                new InnerClassesInfo[0]);

                InnerClassesAttributeEditor innerClassesAttributeEditor =
                        new InnerClassesAttributeEditor(innerClassesAttribute);

                // Sort and add the inner class nodes.
                Collections.sort(innerClassNodes, INNER_CLASS_NODE_COMPARATOR);
                for (InnerClassNode icn : innerClassNodes) {
                    if (icn.innerName != null && !isJavaIdentifier(icn.innerName)) {
                        Metrics.increaseCount(MetricType.DEX2PRO_INVALID_INNER_CLASS);
                        icn.innerName = null;
                        icn.outerName = null;
                    }

                    // Add the inner classes info.
                    int innerClassIndex = constantPoolEditor.addClassConstant(icn.name, null);
                    int outerClassIndex = icn.outerName == null ? 0 : constantPoolEditor.addClassConstant(icn.outerName, null);
                    int innerNameIndex = icn.innerName == null ? 0 : constantPoolEditor.addUtf8Constant(icn.innerName);
                    int innerClassAccessFlags = icn.access;
                    InnerClassesInfo innerClassesInfo =
                            new InnerClassesInfo(innerClassIndex,
                                    outerClassIndex,
                                    innerNameIndex,
                                    innerClassAccessFlags);

                    innerClassesAttributeEditor.addInnerClassesInfo(innerClassesInfo);
                }

                attributesEditor.addAttribute(innerClassesAttribute);
            }
        }

        // Add the source file attribute.
        if (classNode.source != null) {
            new AttributesEditor(programClass, false)
                    .addAttribute(new SourceFileAttribute(constantPoolEditor.addUtf8Constant(Attribute.SOURCE_FILE),
                            constantPoolEditor.addUtf8Constant(classNode.source)));
        }

        // Add all annotations.
        if (classNode.anns != null) {
            convertAnnotations(classNode.anns, programClass, constantPoolEditor);
        }

        // Add all fields.
        if (classNode.fields != null) {
            for (DexFieldNode fieldNode : classNode.fields) {
                convertField(classNode, fieldNode, classBuilder);
            }
        }

        // Add all methods.
        if (classNode.methods != null) {
            for (DexMethodNode methodNode : classNode.methods) {
                convertMethod(classNode, methodNode, classBuilder);
            }
        }

        // Let the class visitor visit the created class.
        classVisitor.visitProgramClass(programClass);
    }

    private void convertCode(IrMethod irMethod, CompactCodeAttributeComposer composer) {
        optimize(irMethod);
        ir2j(irMethod, composer);
    }

    private boolean shouldSkipMethod(IrMethod method) {
        if (MAX_PHI_LABELS > 0 && method.phiLabels != null && method.phiLabels.size() > MAX_PHI_LABELS) {
            return true;
        }
        return MAX_STATEMENTS > 0 && method.stmts.getSize() > MAX_STATEMENTS;
    }

    private void convertField(DexClassNode classNode, DexFieldNode fieldNode, ClassBuilder classBuilder) {

        // Create the field.
        ProgramClass programClass = classBuilder.getProgramClass();
        ProgramField programField =
                classBuilder.addAndReturnField(fieldNode.access & ~DexConstants.ACC_DECLARED_SYNCHRONIZED,
                        fieldNode.field.getName(),
                        fieldNode.field.getType());

        ConstantPoolEditor constantPoolEditor = classBuilder.getConstantPoolEditor();

        // Add the signature attribute.
        if (fieldNode.anns != null) {
            for (DexAnnotationNode ann : fieldNode.anns) {
                if (ann.visibility == Visibility.SYSTEM) {
                    if (DexConstants.ANNOTATION_SIGNATURE_TYPE.equals(ann.type)) {
                        Object[] strs = (Object[]) findAnnotationAttribute(ann, "value");
                        if (strs != null) {
                            // Create a signature attribute.
                            StringBuilder sb = new StringBuilder();
                            for (Object str : strs) {
                                sb.append(str);
                            }
                            String signature = sb.toString();

                            // Add the attribute to the method.
                            new AttributesEditor(programClass, programField, false)
                                    .addAttribute(new SignatureAttribute(constantPoolEditor.addUtf8Constant(Attribute.SIGNATURE),
                                            constantPoolEditor.addUtf8Constant(signature)));
                        }
                    }
                }
            }
        }

        // Add the constant value attribute.
        Object cst = fieldNode.cst;
        if (cst != null) {
            int constantValueIndex =
                    convertConstantValue(constantPoolEditor, cst);
            int attributeNameIndex =
                    constantPoolEditor.addUtf8Constant(Attribute.CONSTANT_VALUE);
            ConstantValueAttribute attribute =
                    new ConstantValueAttribute(attributeNameIndex,
                            constantValueIndex);
            AttributesEditor editor =
                    new AttributesEditor(programClass, programField, false);
            editor.addAttribute(attribute);
        }

        // Add annotation attributes.
        if (fieldNode.anns != null) {
            convertAnnotations(fieldNode.anns, programClass, constantPoolEditor, programField);
        }
    }

    /**
     * Finds or adds the bootstrap methods attribute and adds the specified
     * bootstrap method.
     */
    static int convertBootstrapMethod(ProgramClass programClass, ConstantPoolEditor constantPoolEditor, MethodHandle mh, Object[] args) {
        // Find or add the bootstrap method attribute.
        AttributesEditor attributesEditor =
                new AttributesEditor(programClass, false);
        BootstrapMethodsAttribute bootstrapMethodsAttribute =
                (BootstrapMethodsAttribute) attributesEditor.findAttribute(Attribute.BOOTSTRAP_METHODS);
        if (bootstrapMethodsAttribute == null) {
            bootstrapMethodsAttribute =
                    new BootstrapMethodsAttribute(constantPoolEditor.addUtf8Constant(Attribute.BOOTSTRAP_METHODS),
                            0,
                            new BootstrapMethodInfo[0]);
            attributesEditor.addAttribute(bootstrapMethodsAttribute);
        }

        // Add the bootstrap method and return its index.
        int methodHandleIndex =
                convertMethodHandle(constantPoolEditor, mh);
        int[] methodArguments =
                convertConstantValues(constantPoolEditor, args);

        BootstrapMethodInfo bootstrapMethodInfo =
                new BootstrapMethodInfo(methodHandleIndex,
                        methodArguments.length,
                        methodArguments);
        return new BootstrapMethodsAttributeEditor(bootstrapMethodsAttribute)
                .addBootstrapMethodInfo(bootstrapMethodInfo);
    }

    private static int[] convertConstantValues(ConstantPoolEditor constantPoolEditor, Object[] v) {
        int[] inddices = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            inddices[i] = convertConstantValue(constantPoolEditor, v[i]);
        }
        return inddices;
    }

    private static int convertConstantValue(ConstantPoolEditor constantPoolEditor, Object ele) {
        if (ele instanceof Boolean) {
            return constantPoolEditor.addIntegerConstant((Boolean) ele ? 1 : 0);
        } else if (ele instanceof Character) {
            return constantPoolEditor.addIntegerConstant((Character) ele);
        } else if (ele instanceof Byte ||
                ele instanceof Short ||
                ele instanceof Integer) {
            return constantPoolEditor.addIntegerConstant(((Number) ele).intValue());
        } else if (ele instanceof Long) {
            return constantPoolEditor.addLongConstant((Long) ele);
        } else if (ele instanceof Float) {
            return constantPoolEditor.addFloatConstant((Float) ele);
        } else if (ele instanceof Double) {
            return constantPoolEditor.addDoubleConstant((Double) ele);
        } else if (ele instanceof String) {
            return constantPoolEditor.addStringConstant((String) ele);
        } else if (ele instanceof DexType) {
            return constantPoolEditor.addMethodTypeConstant(((DexType) ele).desc, null);
        } else if (ele instanceof Proto) {
            return constantPoolEditor.addMethodTypeConstant(((Proto) ele).getDesc(), null);
        } else if (ele instanceof MethodHandle) {
            return convertMethodHandle(constantPoolEditor, (MethodHandle) ele);
        }

        throw new UnsupportedOperationException("Unsupported constant " + ele.getClass().getName() + " [" + ele + "]");
    }

    private static int convertMethodHandle(ConstantPoolEditor constantPoolEditor, MethodHandle mh) {
        switch (mh.getType()) {
            case MethodHandle.INSTANCE_GET:
                return constantPoolEditor.addMethodHandleConstant(MethodHandleConstant.REF_GET_FIELD,
                        constantPoolEditor.addFieldrefConstant(toInternalName(mh.getField().getOwner()), mh.getField().getName(), mh.getField().getType(), null, null));
            case MethodHandle.INSTANCE_PUT:
                return constantPoolEditor.addMethodHandleConstant(MethodHandleConstant.REF_PUT_FIELD,
                        constantPoolEditor.addFieldrefConstant(toInternalName(mh.getField().getOwner()), mh.getField().getName(), mh.getField().getType(), null, null));
            case MethodHandle.STATIC_GET:
                return constantPoolEditor.addMethodHandleConstant(MethodHandleConstant.REF_GET_STATIC,
                        constantPoolEditor.addFieldrefConstant(toInternalName(mh.getField().getOwner()), mh.getField().getName(), mh.getField().getType(), null, null));
            case MethodHandle.STATIC_PUT:
                return constantPoolEditor.addMethodHandleConstant(MethodHandleConstant.REF_PUT_STATIC,
                        constantPoolEditor.addFieldrefConstant(toInternalName(mh.getField().getOwner()), mh.getField().getName(), mh.getField().getType(), null, null));
            case MethodHandle.INVOKE_INSTANCE:
                return constantPoolEditor.addMethodHandleConstant(MethodHandleConstant.REF_INVOKE_VIRTUAL,
                        constantPoolEditor.addMethodrefConstant(toInternalName(mh.getMethod().getOwner()), mh.getMethod().getName(), mh.getMethod().getDesc(), null, null));
            case MethodHandle.INVOKE_STATIC:
                return constantPoolEditor.addMethodHandleConstant(MethodHandleConstant.REF_INVOKE_STATIC,
                        constantPoolEditor.addMethodrefConstant(toInternalName(mh.getMethod().getOwner()), mh.getMethod().getName(), mh.getMethod().getDesc(), null, null));
            case MethodHandle.INVOKE_CONSTRUCTOR:
            case MethodHandle.INVOKE_DIRECT:
                return constantPoolEditor.addMethodHandleConstant(MethodHandleConstant.REF_INVOKE_SPECIAL,
                        constantPoolEditor.addMethodrefConstant(toInternalName(mh.getMethod().getOwner()), mh.getMethod().getName(), mh.getMethod().getDesc(), null, null));
            case MethodHandle.INVOKE_INTERFACE:
                return constantPoolEditor.addMethodHandleConstant(MethodHandleConstant.REF_INVOKE_INTERFACE,
                        constantPoolEditor.addInterfaceMethodrefConstant(toInternalName(mh.getMethod().getOwner()), mh.getMethod().getName(), mh.getMethod().getDesc(), null, null));
            default:
                throw new UnsupportedOperationException("Unsupported method handle type " + mh.getType());
        }
    }

    private void convertMethod(DexClassNode classNode, DexMethodNode methodNode, ClassBuilder classBuilder) {

        // Create the method.
        int flags = methodNode.access & ~(DexConstants.ACC_DECLARED_SYNCHRONIZED |
                DexConstants.ACC_CONSTRUCTOR);
        String name = methodNode.method.getName();
        String desc = methodNode.method.getDesc();

        ProgramClass programClass = classBuilder.getProgramClass();
        ProgramMethod programMethod;

        try {
            if (methodNode.codeNode == null) {
                programMethod = classBuilder.addAndReturnMethod(flags, name, desc);
            } else {
                IrMethod irMethod = dex2ir(methodNode);
                if (shouldSkipMethod(irMethod)) {
                    programMethod = classBuilder.addAndReturnMethod(flags, name, desc);
                } else {
                    programMethod = classBuilder.addAndReturnMethod(flags, name, desc,
                                    MAX_CODE_LENGTH,
                                    code -> convertCode(irMethod, code));
                }
            }
        } catch (Exception e) {
            if (SKIP_UNPARSEABLE_METHODS) {
                // This can e.g., happen for invalid bytecode. In that case,
                // we still want to continue to load the remaining methods.

                // Create an empty method to continue.
                programMethod = classBuilder.addAndReturnMethod(flags, name, desc);

                Metrics.increaseCount(MetricType.DEX2PRO_UNPARSEABLE_METHOD_SKIPPED);
            } else {
                throw e;
            }
        }

        ConstantPoolEditor constantPoolEditor = classBuilder.getConstantPoolEditor();

        // Add exception and signature attributes.
        if (methodNode.anns != null) {
            for (DexAnnotationNode ann : methodNode.anns) {
                if (ann.visibility == Visibility.SYSTEM) {
                    switch (ann.type) {
                        case DexConstants.ANNOTATION_THROWS_TYPE: {
                            Object[] strs = (Object[]) findAnnotationAttribute(ann, "value");
                            if (strs != null) {
                                // Create an exceptions attribute.
                                ExceptionsAttribute exceptionsAttribute =
                                        new ExceptionsAttribute(constantPoolEditor.addUtf8Constant(Attribute.EXCEPTIONS),
                                                0,
                                                new int[strs.length]);
                                ExceptionsAttributeEditor exceptionsAttributeEditor =
                                        new ExceptionsAttributeEditor(exceptionsAttribute);
                                for (Object str : strs) {
                                    DexType type = (DexType) str;
                                    exceptionsAttributeEditor.addException(constantPoolEditor.addClassConstant(toInternalName(type), null));
                                }
                                // Add the attribute to the method.
                                new AttributesEditor(programClass, programMethod, false)
                                        .addAttribute(exceptionsAttribute);
                            }
                        }
                        break;
                        case DexConstants.ANNOTATION_SIGNATURE_TYPE: {
                            Object[] strs = (Object[]) findAnnotationAttribute(ann, "value");
                            if (strs != null) {
                                // Create a signature attribute.
                                StringBuilder sb = new StringBuilder();
                                for (Object str : strs) {
                                    sb.append(str);
                                }
                                String signature = sb.toString();

                                // Add the attribute to the method.
                                new AttributesEditor(programClass, programMethod, false)
                                        .addAttribute(new SignatureAttribute(constantPoolEditor.addUtf8Constant(Attribute.SIGNATURE),
                                                constantPoolEditor.addUtf8Constant(signature)));
                            }
                        }
                        break;
                    }
                }
            }
        }

        // Add the annotation default attribute.
        if (0 != (classNode.access & DexConstants.ACC_ANNOTATION)) { // its inside an annotation
            Object defaultValue = null;
            if (classNode.anns != null) {
                for (DexAnnotationNode ann : classNode.anns) {
                    if (ann.visibility == Visibility.SYSTEM && ann.type.equals(DexConstants.ANNOTATION_DEFAULT_TYPE)) {
                        DexAnnotationNode node = (DexAnnotationNode) findAnnotationAttribute(ann, "value");
                        if (node != null) {
                            defaultValue = findAnnotationAttribute(node, methodNode.method.getName());
                        }
                        break;
                    }
                }
            }
            if (defaultValue != null) {
                // Create an annotation default attribute.
                AnnotationDefaultAttribute annotationDefaultAttribute =
                        new AnnotationDefaultAttribute(constantPoolEditor.addUtf8Constant(Attribute.ANNOTATION_DEFAULT),
                                convertElementValue(0, defaultValue, constantPoolEditor));

                // Add the attribute to the method.
                new AttributesEditor(programClass, programMethod, false)
                        .addAttribute(annotationDefaultAttribute);
            }
        }

        // Add annotation attributes.
        if (methodNode.anns != null) {
            convertAnnotations(methodNode.anns, programClass, constantPoolEditor, programMethod);
        }

        // Add parameter annotation attributes.
        if (methodNode.parameterAnns != null) {
            convertParameterAnnotations(methodNode.parameterAnns, programClass, constantPoolEditor, programMethod);
        }
    }

    private IrMethod dex2ir(DexMethodNode methodNode) {
        return new Dex2IRConverter()
                .convert(0 != (methodNode.access & DexConstants.ACC_STATIC), methodNode.method, methodNode.codeNode);
    }

    private static Object findAnnotationAttribute(DexAnnotationNode ann, String name) {
        for (DexAnnotationNode.Item item : ann.items) {
            if (item.name.equals(name)) {
                return item.value;
            }
        }
        return null;
    }

    private static Clz get(Map<String, Clz> classes, String name) {
        Clz clz = classes.get(name);
        if (clz == null) {
            clz = new Clz(name);
            classes.put(name, clz);
        }
        return clz;
    }

    private void ir2j(IrMethod irMethod, CompactCodeAttributeComposer composer) {
        new IR2ProConverter()
                .optimizeSynchronized(false)
                .usePrimitiveArrayConstants(this.usePrimitiveArrayConstants)
                .ir(irMethod)
                .code(composer)
                .convert();
    }

    private void optimize(IrMethod irMethod) {
        // Derived from the extension in Dex2Jar.java
        T_cleanLabel.transform(irMethod);
        T_removeLocal.transform(irMethod);
        T_removeConst.transform(irMethod);
        T_zero.transform(irMethod);
        T_new.transform(irMethod);
        T_agg.transform(irMethod);
        T_multiArray.transform(irMethod);
        T_voidInvoke.transform(irMethod);
        T_type.transform(irMethod);
        T_unssa.transform(irMethod);
        T_ir2jRegAssign.transform(irMethod);
        T_trimEx.transform(irMethod);
    }

    /**
     * Adds the transitive outer classes of the given class to the given list.
     */
    private static void searchEnclosing(Clz clz, List<InnerClassNode> innerClassNodes) {
        Set<Clz> visitedClz = new LinkedHashSet<>();
        for (Clz p = clz; p != null; p = p.enclosingClass) {
            if (!visitedClz.add(p)) { // prevent endless loop
                break;
            }
            Clz enclosingClass = p.enclosingClass;
            if (enclosingClass == null) {
                break;
            }
            if (enclosingClass.equals(clz)) {
                // enclosing itself, that is impossible
                break;
            }
            // Get the outer class name. unless the inner class is enclosed
            // in a method or anonymous.
            String outerName = p.enclosingMethod != null ||
                    p.innerName == null ? null :
                    toInternalName(enclosingClass.name);

            innerClassNodes.add(new InnerClassNode(toInternalName(p.name),
                    outerName,
                    p.innerName,
                    clearInnerAccess(p.access)));
        }
    }

    /**
     * Adds the immediate inner classes of the given class to the given list.
     */
    private static void searchInnerClass(Clz clz, List<InnerClassNode> innerClassNodes, String className) {
        if (clz.inners != null) {
            for (Clz inner : clz.inners) {
                // Get the outer class name. unless the inner class is enclosed
                // in a method or anonymous.
                String outerName = inner.enclosingMethod != null ||
                        inner.innerName == null ? null :
                        toInternalName(className);

                innerClassNodes.add(new InnerClassNode(toInternalName(inner.name),
                        outerName,
                        inner.innerName,
                        clearInnerAccess(inner.access)));
            }
        }
    }


    // Copied from org/objectweb/asm/tree/InnerClassNode.java
    private static class InnerClassNode {
        private String name;
        private String outerName;
        private String innerName;
        private int access;

        public InnerClassNode(final String name,
                              final String outerName,
                              final String innerName,
                              final int access) {
            this.name = name;
            this.outerName = outerName;
            this.innerName = innerName;
            this.access = access;
        }
    }

    private static final Comparator<InnerClassNode> INNER_CLASS_NODE_COMPARATOR = Comparator.comparing(o -> o.name);
}
