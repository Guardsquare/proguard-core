package proguard.classfile.util;

import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.io.ProgramClassWriter;
import proguard.classfile.visitor.ProgramClassFilter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import static proguard.classfile.util.ClassUtil.internalClassName;

/**
* A {@link ClassLoader} that can load classes from a ProGuardCORE
* classpool.
*
* @author James Hamilton
*/
public class ClassPoolClassLoader extends ClassLoader {
    private final ClassPool classPool;

    public ClassPoolClassLoader(ClassPool classPool)
    {
        this.classPool = classPool;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException
    {
        Clazz clazz = classPool.getClass(internalClassName(name));
        if (clazz == null)
        {
            throw new ClassNotFoundException("Class " + name + " not found in class pool");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        clazz.accept(
                new ProgramClassFilter(
                new ProgramClassWriter(
                new DataOutputStream(byteArrayOutputStream))));

        byte[] bytes = byteArrayOutputStream.toByteArray();
        return defineClass(name, bytes, 0, bytes.length);
    }
}
