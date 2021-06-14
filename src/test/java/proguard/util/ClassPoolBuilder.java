package proguard.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import proguard.classfile.ClassPool;
import proguard.classfile.util.ClassInitializer;
import proguard.classfile.visitor.ClassPoolFiller;
import proguard.io.ClassReader;
import proguard.io.DataEntry;
import proguard.io.DataEntrySource;

/**
 * Toolkit to produce a {@link ClassPool} from different input files.
 *
 * @author Samuel Hopstock
 */
public class ClassPoolBuilder
{

    /**
     * Compile all Java files in a directory into a {@link ClassPool}.
     *
     * @param dir The directory to traverse recursively looking for Java files
     * @return The class pool containing all compiled classes
     * @throws IOException       If there were errors accessing some files needed for compilation
     * @throws CompilerException If javac reported any errors
     */
    public static ClassPool fromDirectory(Path dir) throws IOException, CompilerException
    {
        return fromDirectory(Collections.emptyList(), dir);
    }

    /**
     * Compile all Java files in a directory into a {@link ClassPool}.
     *
     * @param options Options that are passed on to javac, e.g. ["-source", "11"]
     * @param dir     The directory to traverse recursively looking for Java files
     * @return The class pool containing all compiled classes
     * @throws IOException       If there were errors accessing some files needed for compilation
     * @throws CompilerException If javac reported any errors
     */
    public static ClassPool fromDirectory(Iterable<String> options, Path dir) throws IOException, CompilerException
    {
        File[] files = Files.walk(dir)
                            .filter(p -> p.toString().endsWith(".java"))
                            .map(Path::toFile)
                            .toArray(File[]::new);
        return fromSourceFiles(options, files);
    }

    /**
     * Compile multiple Java files into a {@link ClassPool}.
     *
     * @param sourceFiles The files to compile
     * @return The class pool containing all compiled classes
     * @throws IOException       If there were errors accessing some files needed for compilation
     * @throws CompilerException If javac reported any errors
     */
    public static ClassPool fromSourceFiles(File... sourceFiles) throws IOException, CompilerException
    {
        return fromSourceFiles(Collections.emptyList(), sourceFiles);
    }

    /**
     * Compile multiple Java files into a {@link ClassPool}.
     *
     * @param options     Options that are passed on to javac, e.g. ["-source", "11"]
     * @param sourceFiles The files to compile
     * @return The class pool containing all compiled classes
     * @throws IOException       If there were errors accessing some files needed for compilation
     * @throws CompilerException If javac reported any errors
     */
    public static ClassPool fromSourceFiles(Iterable<String> options, File... sourceFiles) throws IOException, CompilerException
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> objects = fileManager.getJavaFileObjects(sourceFiles);
        List<JavaFileObject> compilationUnits = new ArrayList<>();
        objects.forEach(compilationUnits::add);

        return compile(options, compilationUnits);
    }

    /**
     * Compile Java source code strings into a {@link ClassPool}.
     *
     * @param codeStrings The strings containing the source code to compile
     * @return The class pool containing all compiled classes
     * @throws IOException       If there were errors accessing some files needed for compilation
     * @throws CompilerException If javac reported any errors
     */
    public static ClassPool fromStrings(String... codeStrings) throws IOException, CompilerException
    {
        return fromStrings(Collections.emptyList(), codeStrings);
    }

    /**
     * Compile Java source code strings into a {@link ClassPool}.
     *
     * @param options     Options that are passed on to javac, e.g. ["-source", "11"]
     * @param codeStrings The strings containing the source code to compile
     * @return The class pool containing all compiled classes
     * @throws IOException       If there were errors accessing some files needed for compilation
     * @throws CompilerException If javac reported any errors
     */
    public static ClassPool fromStrings(Iterable<String> options, String... codeStrings) throws IOException, CompilerException
    {
        List<JavaFileObject> compilationUnits = new ArrayList<>();
        for (String code : codeStrings)
        {
            Pattern classNamePattern = Pattern.compile("^\\s*(public\\s+)?(class|interface)\\s+(?<name>[a-zA-Z_$][a-zA-Z\\d_$]*)");
            Matcher matcher = classNamePattern.matcher(code);
            if (matcher.find())
            {
                compilationUnits.add(new StringJavaFileObject(matcher.group("name"), code));
            }
            else
            {
                throw new CompilerException("String needs to contain an interface, or a public or package-private class");
            }
        }
        return compile(options, compilationUnits);
    }

    private static ClassPool compile(Iterable<String> optionsIterable, List<JavaFileObject> compilationUnits) throws CompilerException, IOException
    {
        List<String> options = new ArrayList<>();
        boolean sourceIsSet = false;
        boolean targetIsSet = false;
        for (String option : optionsIterable)
        {
            options.add(option);
            sourceIsSet |= option.equals("-source");
            targetIsSet |= option.equals("-target");
        }
        if (!sourceIsSet && !targetIsSet)
        {
            options.add("-source");
            options.add("1.8");
            options.add("-target");
            options.add("1.8");
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        SimpleJavaFileManager fileManager = new SimpleJavaFileManager(compiler.getStandardFileManager(diagnosticCollector, null, null));

        JavaCompiler.CompilationTask compilationTask = compiler.getTask(null, fileManager, diagnosticCollector, options, null, compilationUnits);

        if (!compilationTask.call())
        {
            StringBuilder messageBuilder = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics())
            {
                String file = diagnostic.getSource() instanceof StringJavaFileObject ? "" : diagnostic.getSource().getName() + " ";
                messageBuilder.append(String.format("%sline %d column %d -> %s\n", file, diagnostic.getLineNumber(), diagnostic.getColumnNumber(), diagnostic.getMessage(null)));
            }
            throw new CompilerException(messageBuilder.toString().trim());
        }

        ClassPool classPool = new ClassPool();
        DataEntrySource source = dataEntryReader ->
        {
            for (DataEntry entry : fileManager.getGeneratedOutputFiles())
            {
                dataEntryReader.read(entry);
            }
        };
        source.pumpDataEntries(
            new ClassReader(false, false, false, false, null, new ClassPoolFiller(classPool)));

        classPool.classesAccept(new ClassInitializer(classPool, new ClassPool()));
        return classPool;
    }

    /**
     * Thrown when a file could not be compiled by the Java compiler.
     */
    public static class CompilerException
        extends Exception
    {

        public CompilerException(String message)
        {
            super(message);
        }
    }

    /**
     * Wraps a Java source code string inside a virtual file object that the Java compiler API can process.
     *
     * @author James Hamilton
     */
    private static class StringJavaFileObject
        extends SimpleJavaFileObject
    {

        private final String code;

        public StringJavaFileObject(String name, String code)
        {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors)
        {
            return code;
        }
    }

    /**
     * Virtual file object to be returned by the Java compiler API, containing the compiled class.
     *
     * @author James Hamilton
     */
    private static class ClassJavaFileObject
        extends SimpleJavaFileObject
        implements DataEntry
    {

        private final ByteArrayOutputStream outputStream;
        private final String                className;

        protected ClassJavaFileObject(String className, Kind kind)
        {
            super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
            this.className = className;
            outputStream = new ByteArrayOutputStream();
        }

        @Override
        public OutputStream openOutputStream()
        {
            return outputStream;
        }

        public byte[] getBytes()
        {
            return outputStream.toByteArray();
        }

        public String getClassName()
        {
            return className;
        }

        @Override
        public String getOriginalName()
        {
            return this.getClassName();
        }

        @Override
        public long getSize()
        {
            return this.getBytes().length;
        }

        @Override
        public boolean isDirectory()
        {
            return false;
        }

        @Override
        public InputStream getInputStream()
        {
            return new ByteArrayInputStream(this.getBytes());
        }

        @Override
        public void closeInputStream()
        {

        }

        @Override
        public DataEntry getParent()
        {
            return null;
        }
    }

    /**
     * File manager for the Java compiler API that holds all needed files in memory instead of on disk.
     *
     * @author James Hamilton
     */
    public static class SimpleJavaFileManager
        extends ForwardingJavaFileManager<JavaFileManager>
    {

        private final List<ClassJavaFileObject> outputFiles;

        protected SimpleJavaFileManager(JavaFileManager fileManager)
        {
            super(fileManager);
            outputFiles = new ArrayList<>();
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException
        {
            ClassJavaFileObject file = new ClassJavaFileObject(className, kind);
            outputFiles.add(file);
            return file;
        }

        public List<ClassJavaFileObject> getGeneratedOutputFiles()
        {
            return outputFiles;
        }
    }
}
