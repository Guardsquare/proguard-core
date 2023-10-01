package proguard.classfile.util.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import proguard.classfile.AccessConstants;
import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.LibraryClass;
import proguard.classfile.LibraryField;
import proguard.classfile.LibraryMethod;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramField;
import proguard.classfile.ProgramMethod;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;
import proguard.util.ProcessingFlags;

/**
 * This utility class renders {@link Clazz} objects into a more human-readable format.
 * It can be used as a Java type renderer in Intellij-based IDE to support debugging.
 *
 * @author Kymeng Tang
 */
public class ClassViewModel extends ProcessableViewModel
{
    private Clazz               model;
    private ClassViewModel      parent;
    private ClassViewModel[]    interfaces;
    private ConstantViewModel[] constantPool;

    private FieldViewModel[]    fields;
    private MethodViewModel[]   methods;

    /**
     * Renders {@link Clazz} object into an abstract representation that hides the low-level
     * Jvm class data structure. The processing flags rendering are limited to those
     * declared in {@link ProcessingFlags}.
     *
     * @param clazz     The {@link Clazz} object to be rendered.
     * @return          A high-level representation of {@link Clazz} (i.e., {@link ClassViewModel} object).
     */
    public static ClassViewModel render(Clazz clazz)
    {
        ClassViewModel viewModel = new ClassViewModel(clazz);

        viewModel.parent          = renderSuperClass(clazz);
        viewModel.interfaces      = renderInterfaces(clazz);
        viewModel.constantPool    = renderConstantPool(clazz);
        viewModel.fields          = renderFields(clazz);
        viewModel.methods         = renderMethods(clazz);
        viewModel.processingFlags = renderProcessingFlags(clazz.getProcessingFlags());
        viewModel.processingInfo  = clazz.getProcessingInfo();

        return viewModel;
    }

    /**
     * Renders {@link Clazz} object into an abstract representation that hides the low-level
     * Jvm class data structure; additional processing flags declared in a subclass of
     * {@link ProcessingFlags} will also be rendered.
     *
     * @param clazz                     The {@link Clazz} object to be rendered.
     * @param processingFlagsHolder     A subclass of {@link ProcessingFlags} that holds additional
     *                                  processing flags.
     * @return                          A high-level representation of {@link Clazz} (i.e., {@link ClassViewModel} object).
     */
    public static <T extends ProcessingFlags> ClassViewModel render(Clazz clazz, Class<T> processingFlagsHolder)
    {
        addExtraProcessingFlags(processingFlagsHolder);
        return render(clazz);
    }

    /**
     * Private constructor for the {@link ClassViewModel}. The rendering of the {@link Clazz} object
     * should be done with ClassViewModel.render(clazz:Clazz) or ClassViewModel.render(clazz:Clazz, processingFlagsHolder:Class).
     *
     * @param clazz     {@link Class} object to be rendered.
     */
    private ClassViewModel(Clazz clazz)
    {
        this.model = clazz;
    }

    /**
     * A utility method that renders a {@link Clazz} object into a string containing class name, type,
     * accessibility. (e.g., "public static class Foo").
     *
     * @param model     {@link Clazz} object to be rendered.
     * @return          A string previewing the class signature.
     */
    public static String renderPreview(Clazz model)
    {
        return ClassUtil.externalClassAccessFlags(model.getAccessFlags()) +
               ((model.getAccessFlags() & AccessConstants.INTERFACE) == 0 ? "class " : "") +
               model.getName();
    }

    /**
     * A utility method for recursively rendering {@link Clazz} objects in the hierarchy.
     * @param model     A {@link Clazz} object whose parent needs to be rendered.
     * @return          A high-level representation of the parent {@link Clazz}.
     */
    public static ClassViewModel renderSuperClass(Clazz model)
    {
        return model.getSuperClass() != null ?
            new ClassViewModel(model.getSuperClass()) : null;
    }

    /**
     * A utility method for recursively rendering interfaces of an implementing {@link Clazz}.
     * @param model     A {@link Clazz} object whose interfaces need to be rendered.
     * @return          An array of high-level representation of the implemented interfaces.
     */
    public static ClassViewModel[] renderInterfaces(Clazz model)
    {
        List<ClassViewModel> interfaces = new ArrayList<>();
        model.hierarchyAccept(false,
                              false,
                              true,
                              false,
                              new ClassVisitor()
                              {
                                  public void visitAnyClass(Clazz clazz)
                                  {
                                      interfaces.add(new ClassViewModel(clazz));
                                  }
                              });
        return interfaces.toArray(new ClassViewModel[interfaces.size()]);
    }

    /**
     * A utility method for rendering each constant pool entry into a higher-level representation.
     * see {@link ConstantViewModel}.
     *
     * @param model     A {@link Clazz} object whose constant pool needs to be rendered.
     * @return          An array of high-level representation of the constant pool entries.
     */
    public static ConstantViewModel[] renderConstantPool(Clazz model)
    {
        List<ConstantViewModel> constantList = new ArrayList<>();
        constantList.add(null);
        model.constantPoolEntriesAccept(new ConstantVisitor()
        {
            public void visitAnyConstant(Clazz clazz, Constant constant)
            {
                constantList.add(new ConstantViewModel(clazz, constant));
            }
        });
        return constantList.toArray(new ConstantViewModel[constantList.size()]);
    }

    /**
     * A utility method for rendering each {@link Field} into a higher-level representation.
     * see {@link FieldViewModel}.
     *
     * @param model     A {@link Clazz} object whose fields need to be rendered.
     * @return          An array of high-level representation of the class' fields.
     */
    public static FieldViewModel[] renderFields(Clazz model)
    {
        List<FieldViewModel> fields = new ArrayList<>();
        model.fieldsAccept(new MemberVisitor()
        {
            private void visitAnyField(Clazz clazz, Field field)
            {
                fields.add(FieldViewModel.render(clazz, field));
            }
            public void visitProgramField(ProgramClass programClass, ProgramField programField)
            {
                visitAnyField(programClass, programField);
            }
            public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
            {
                visitAnyField(libraryClass, libraryField);
            }
        });
        return fields.toArray(new FieldViewModel[fields.size()]);
    }

    /**
     * A utility method for rendering each {@link Method} into a higher-level representation.
     * see {@link MethodViewModel}.
     *
     * @param model     A {@link Clazz} object whose methods need to be rendered.
     * @return          An array of high-level representation of the class' methods.
     */
    public static MethodViewModel[] renderMethods(Clazz model)
    {
        List<MethodViewModel> methods = new ArrayList<>();
        model.methodsAccept(new MemberVisitor()
        {
            private void visitAnyMethod(Clazz clazz, Method method)
            {
                MethodViewModel methodViewModel = MethodViewModel.render(clazz, method);
                methods.add(methodViewModel);
            }
            public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
            {
                this.visitAnyMethod(programClass, programMethod);
            }
            public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
            {
                this.visitAnyMethod(libraryClass, libraryMethod);
            }
        });

        return methods.toArray(new MethodViewModel[methods.size()]);
    }
}
