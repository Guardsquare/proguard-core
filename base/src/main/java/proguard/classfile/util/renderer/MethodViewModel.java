package proguard.classfile.util.renderer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import proguard.classfile.Clazz;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionsAttribute;
import proguard.classfile.attribute.SignatureAttribute;
import proguard.classfile.attribute.annotation.AnnotationsAttribute;
import proguard.classfile.attribute.annotation.RuntimeVisibleTypeAnnotationsAttribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.renderer.core.StringListWriter;
import proguard.classfile.visitor.ClassPrinter;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This utility class renders {@link Method} objects into a more human-readable format.
 * It can be used as a Java type renderer in Intellij-based IDE to support debugging.
 *
 * @author Kymeng Tang
 */
public class MethodViewModel extends MemberViewModel
{
    private Clazz[]             referencedClasses;
    private Map<String, Object> attributes;

    /**
     * A wrapper utility method that wraps a {@link Method} object in a {@link MethodViewModel}
     * @param clazz     The {@link Clazz} that the {@link Method} belongs to.
     * @param method    The {@link Method} object to be rendered.
     * @return          An abstract representation of the {@link Method} object.
     */
    public static MethodViewModel render(Clazz clazz, Method method)
    {
        MethodViewModel viewModel = new MethodViewModel(clazz, method);
        viewModel.referencedClasses = renderReferenceClasses(method);
        viewModel.attributes = renderAttribute(clazz, method);
        return viewModel;
    }

    /**
     * A constructor to keep track of the {@link Method} object to be rendered and its associated
     * {@link Clazz}
     *
     * @param clazz     The {@link Clazz} that the {@link Method} belongs to.
     * @param method    The {@link Method} object to be rendered.
     */
    private MethodViewModel(Clazz clazz, Method method)
    {
        super(clazz, method);
    }

    /**
     * A utility method that renders a {@link Method} entry into a preview string,
     * e.g., private int bar(Foo obj)
     *
     * @return A string previewing the method signature.
     */
    public String renderPreview()
    {
        return ClassUtil.externalFullMethodDescription(this.model.key.getName(),
                                                       this.model.value.getAccessFlags(),
                                                       this.model.value.getName(this.model.key),
                                                       this.model.value.getDescriptor(this.model.key));
    }

    /**
     * A utility method for recursively rendering classes that the method refers to.
     * @param method    A {@link Method} object whose reference classes need to be rendered.
     * @return          An array of high-level representation of the reference classes.
     */
    private static Clazz[] renderReferenceClasses(Method method)
    {
        List<Clazz> referencedClasses = new ArrayList();
        method.referencedClassesAccept(new ClassVisitor()
        {
            public void visitAnyClass(Clazz clazz)
            {
                referencedClasses.add(clazz);
            }
        });
        return referencedClasses.toArray(new Clazz[referencedClasses.size()]);
    }

    /**
     * A utility method for rendering the method's attributes (e.g., code attributes) into
     * a human-readable format, i.e., it prints out the byte code instead of showing just the byte array.
     *
     * @param clazz     The {@link Clazz} that the {@link Method} belongs to.
     * @param method    The {@link Method} object whose attributes need to be rendered.
     * @return          A map that categorises the rendered attributes.
     */
    private static Map<String, Object> renderAttribute(Clazz clazz, Method method)
    {
        Map<String, Object> attributeList = new TreeMap<>();
        method.accept(clazz, new MemberVisitor()
        {
            public void visitAnyMember(Clazz clazz, Member member) {}
            public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
            {
                programMethod.attributesAccept(programClass, new AttributeVisitor()
                {
                    List<String> exceptionAttributeList                  = new ArrayList<>();
                    List<String> signatureAttributeList                  = new ArrayList<>();
                    List<String> runtimeInvisibleAnnotationAttributeList = new ArrayList<>();


                    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

                    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
                    {
                        signatureAttributeList.add(renderAttributeAsString(clazz, method, signatureAttribute));
                        if (!attributeList.containsKey("signature"))
                            attributeList.put("signature", signatureAttributeList);
                    }

                    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
                    {
                        attributeList.put("code", renderAttributeAsList(clazz, method, codeAttribute));
                    }

                    public void visitExceptionsAttribute(Clazz clazz, Method method, ExceptionsAttribute exceptionsAttribute)
                    {
                        exceptionAttributeList.add(renderAttributeAsString(clazz, method, exceptionsAttribute));
                        if (!attributeList.containsKey("exceptions"))
                            attributeList.put("exceptions", exceptionAttributeList);
                    }

                    public void visitRuntimeVisibleTypeAnnotationsAttribute(Clazz clazz, RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute)
                    {
                        runtimeInvisibleAnnotationAttributeList.add(renderAttributeAsString(clazz, method, runtimeVisibleTypeAnnotationsAttribute));
                        if (!attributeList.containsKey("runtime invisible annotations"))
                            attributeList.put("runtime invisible annotations", runtimeInvisibleAnnotationAttributeList);
                    }

                    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
                    {
                        Object tmp = attributeList.get(annotationsAttribute.getClass().getName());
                        List attributeContainer = null;
                        if (tmp == null) {
                            attributeContainer = new ArrayList<>();
                        }
                        else {
                            attributeContainer = (List) tmp;
                        }

                        runtimeInvisibleAnnotationAttributeList.add(renderAttributeAsString(clazz, method, annotationsAttribute));
                        if (!attributeList.containsKey(annotationsAttribute.getClass().getName()))
                            attributeList.put(annotationsAttribute.getClass().getName(), attributeContainer);
                    }

                    private String renderAttributeAsString(Clazz clazz, Method method, Attribute attribute) {
                        StringWriter stringWriter = new StringWriter();
                        attribute.accept(clazz, method, new ClassPrinter(new PrintWriter(stringWriter)));
                        stringWriter.flush();
                        return stringWriter.toString();
                    }

                    private List<String> renderAttributeAsList(Clazz clazz, Method method, Attribute attribute) {
                        StringListWriter writer = new StringListWriter(new ArrayList<>());
                        attribute.accept(clazz, method, new ClassPrinter(new PrintWriter(writer)));
                        return writer.getOutput();
                    }
                });
            }
        });
        return attributeList;
    }
}
