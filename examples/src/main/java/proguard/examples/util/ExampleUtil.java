package proguard.examples.util;

import proguard.classfile.ClassPool;
import proguard.classfile.ProgramClass;
import proguard.classfile.io.ProgramClassReader;
import proguard.classfile.util.ClassPoolClassLoader;
import proguard.classfile.util.ClassUtil;

import java.io.DataInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static proguard.classfile.util.ClassUtil.internalClassName;

/**
 * Utility methods used by examples.
 */
public class ExampleUtil
{
    private ExampleUtil() { }

    /**
     * Create a {@link ClassPool} for the given Java {@link Class}es.
     */
    public static ClassPool createClassPool(Class<?>...classes)
    {
        ClassPool classPool = new ClassPool();
        addClass(classPool, classes);
        return classPool;
    }

    /**
     * Add the given Java {@link Class}es to a {@link ClassPool}.
     */
    public static void addClass(ClassPool classPool, Class<?>...classes)
    {
        for (var clazz : classes)
        {
            var is = ExampleUtil.class.getClassLoader().getResourceAsStream(internalClassName(clazz.getName()) + ".class");
            if (is == null)
            {
                throw new RuntimeException("Class " + clazz.getName() + " not found");
            }
            var classReader  = new ProgramClassReader(new DataInputStream(is));
            var programClass = new ProgramClass();
            programClass.accept(classReader);
            classPool.addClass(programClass);
        }
    }

    /**
     * Execute the "main" method of a ProGuardCORE {@link ProgramClass}.
     */
    public static Object executeMainMethod(ProgramClass programClass)
    {
        return executeMainMethod(new ClassPool(programClass), programClass.getName());
    }

    /**
     * Execute the "main" method of a ProGuardCORE class in the given {@link ClassPool}
     * with the specified name.
     */
    public static Object executeMainMethod(ClassPool classPool, String internalClassName)
    {
        return executeMainMethod(classPool, internalClassName, (String) null);
    }

    /**
     * Execute the "main" method of a ProGuardCORE class in the given {@link ClassPool}
     * with the specified name and the specified arguments.
     */
    public static Object executeMainMethod(ClassPool classPool, String internalClassName, String...arguments)
    {
        try
        {
            return executeMethod(classPool,
                    internalClassName,
                    "main",
                    new Class[] { String[].class },
                    arguments);
        }
        catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute the specified method of a ProGuardCORE class in the given {@link ClassPool}
     * with the specified name and the specified arguments.
     */
    public static Object executeMethod(ClassPool classPool, String internalClassName, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        ClassPoolClassLoader classPoolClassLoader = new ClassPoolClassLoader(classPool);
        Class<?> clazz = classPoolClassLoader.findClass(ClassUtil.externalClassName(internalClassName));
        Method mainMethod = clazz.getDeclaredMethod(methodName, parameterTypes);
        return mainMethod.invoke(null, (Object)arguments);
    }
}
