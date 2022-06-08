package proguard.classfile.visitor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.Clazz;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.InnerClassesAttribute;
import proguard.classfile.attribute.InnerClassesInfo;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.attribute.visitor.InnerClassesInfoVisitor;
import proguard.classfile.editor.InnerClassesAttributeEditor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class InnerClassRemover implements AttributeVisitor, InnerClassesInfoVisitor
{
    private final Clazz classToBeRemoved;
    private final Set<InnerClassesInfo> innerClassesEntriesToBeRemoved = new HashSet<>();
    private static final Logger logger = LogManager.getLogger(InnerClassRemover.class);

    public InnerClassRemover(Clazz clazz)
    {
        this.classToBeRemoved = clazz;
    }

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {
    }

    @Override
    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute) {
        innerClassesAttribute.innerClassEntriesAccept(clazz, this);
        InnerClassesAttributeEditor editor = new InnerClassesAttributeEditor(innerClassesAttribute);
        logger.trace("{} inner class entries are removed from class {}",
                innerClassesEntriesToBeRemoved.size(), clazz);
        for (InnerClassesInfo entry : innerClassesEntriesToBeRemoved)
        {
            editor.removeInnerClassesInfo(entry);
        }
    }

    @Override
    public void visitInnerClassesInfo(Clazz clazz, InnerClassesInfo innerClassesInfo) {
        String innerClassName = clazz.getClassName(innerClassesInfo.u2innerClassIndex);
        if (Objects.equals(innerClassName, this.classToBeRemoved.getName()))
        {
            logger.trace("Removing inner classes entry of class {} enqueued to be removed from class {}",
                    innerClassName, clazz);
            innerClassesEntriesToBeRemoved.add(innerClassesInfo);
        }
    }
}
