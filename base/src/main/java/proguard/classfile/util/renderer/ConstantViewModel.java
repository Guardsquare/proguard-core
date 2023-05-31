package proguard.classfile.util.renderer;

import java.io.PrintWriter;
import java.io.StringWriter;

import proguard.classfile.Clazz;
import proguard.classfile.constant.Constant;
import proguard.classfile.visitor.ClassPrinter;

/**
 * This utility class renders {@link Constant} objects into a more human-readable format.
 * It can be used as a Java type renderer in Intellij-based IDE to support debugging.
 *
 * @author Kymeng Tang
 */
public class ConstantViewModel extends ProcessableViewModel
{
    private MemberViewModel.Pair<Clazz, Constant> model;

    /**
     * A constructor to keep track of the {@link Constant} object to be rendered and its associated
     * {@link Clazz}
     *
     * @param clazz         The {@link Clazz} that the {@link Constant} entry belongs to.
     * @param constant      The {@link Constant} object to be rendered.
     */
    public ConstantViewModel(Clazz clazz, Constant constant)
    {
        model = new MemberViewModel.Pair<>(clazz, constant);
    }

    /**
     * A utility method that renders a {@link Constant} entry into a preview string,
     * e.g., String(18) -> "Foo".
     *
     * @return A string previewing the constant pool entry.
     */
    public String renderPreview()
    {
        StringWriter stringWriter = new StringWriter();
        model.value.accept(model.key, new ClassPrinter(new PrintWriter(stringWriter)));
        stringWriter.flush();
        return stringWriter.toString().trim();
    }
}
