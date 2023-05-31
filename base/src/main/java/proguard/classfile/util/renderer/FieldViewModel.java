package proguard.classfile.util.renderer;

import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.constant.Constant;
import proguard.classfile.util.ClassUtil;

/**
 * This utility class renders {@link Field} objects into a more human-readable format.
 * It can be used as a Java type renderer in Intellij-based IDE to support debugging.
 *
 * @author Kymeng Tang
 */
public class FieldViewModel extends MemberViewModel
{
    /**
     * A constructor to keep track of the {@link Field} object to be rendered and its associated
     * {@link Clazz}
     *
     * @param clazz     The {@link Clazz} that the {@link Field} belongs to.
     * @param field     The {@link Field} object to be rendered.
     */
    private FieldViewModel(Clazz clazz, Field field)
    {
        super(clazz, field);
    }

    /**
     * A wrapper utility method that wraps a {@link Field} object in a {@link FieldViewModel}
     * @param clazz     The {@link Clazz} that the {@link Field} belongs to.
     * @param field     The {@link Field} object to be rendered.
     * @return          An abstract representation of the {@link Field} object.
     */
    public static FieldViewModel render(Clazz clazz, Field field)
    {
        FieldViewModel viewModel = new FieldViewModel(clazz, field);
        return viewModel;
    }

    /**
     * A utility method that renders a {@link Field} entry into a preview string,
     * e.g., "public static int bar"
     *
     * @return A string previewing the constant pool entry.
     */
    public String renderPreview()
    {
        return ClassUtil.externalFullFieldDescription(this.model.value.getAccessFlags(),
                                                      this.model.value.getName(this.model.key),
                                                      this.model.value.getDescriptor(this.model.key));
    }
}
