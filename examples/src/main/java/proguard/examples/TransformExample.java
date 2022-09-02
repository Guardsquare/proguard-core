package proguard.examples;

import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.ProgramClass;
import proguard.classfile.VersionConstants;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.constant.Constant;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.editor.InstructionSequenceReplacer;
import proguard.classfile.editor.PeepholeEditor;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.BranchTargetFinder;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.classfile.visitor.ClassCounter;
import proguard.classfile.visitor.ClassPoolFiller;
import proguard.classfile.visitor.ClassVersionFilter;
import proguard.classfile.visitor.ClassVisitor;
import proguard.io.ClassFilter;
import proguard.io.ClassPath;
import proguard.io.ClassPathEntry;
import proguard.io.ClassReader;
import proguard.io.DataEntryCopier;
import proguard.io.DataEntryReader;
import proguard.io.DataEntryReaderFactory;
import proguard.io.DataEntrySource;
import proguard.io.DataEntryWriter;
import proguard.io.DataEntryWriterFactory;
import proguard.io.DexClassReader;
import proguard.io.DexDataEntryWriterFactory;
import proguard.io.DirectorySource;
import proguard.io.IdleRewriter;
import proguard.io.NameFilteredDataEntryReader;
import proguard.io.RenamedDataEntry;
import proguard.io.util.IOUtil;
import proguard.preverify.CodePreverifier;
import proguard.io.util.ClassPathUtil;
import proguard.util.StringMatcher;

import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;

public class TransformExample
{

    public static void main(String[] args)
    throws IOException
    {

        // input file
        ClassPathEntry inputEntry = new ClassPathEntry(new File(args[0]), false);
        // output file
        ClassPathEntry outputEntry = new ClassPathEntry(new File(args[1]), true);
        // library file
        ClassPathEntry libraryEntry = new ClassPathEntry(new File(args[2]), false);

        ClassPath programJars = new ClassPath(inputEntry, outputEntry);
        ClassPath libraryJars = new ClassPath(libraryEntry);

        // Reading
        boolean dalvik = ClassPathUtil.isDalvik(programJars);
        boolean android = ClassPathUtil.isAndroid(programJars);
        boolean skipNonPublicLibraryClasses      = false;
        boolean skipNonPublicLibraryClassMembers = true;
        boolean ignoreStackMapAttributes         = false;

        BiFunction<DataEntryReader, ClassVisitor, DataEntryReader> dexConverter = (dataEntryReader, classPoolFiller) -> new NameFilteredDataEntryReader(
                "classes*.dex",
                new DexClassReader(
                        true,
                        classPoolFiller
                ),
                dataEntryReader
        );

        ClassPool programClassPool = IOUtil.read(
                programJars,
                "**",
                android,
                false,
                skipNonPublicLibraryClasses,
                skipNonPublicLibraryClassMembers,
                ignoreStackMapAttributes,
                dexConverter
        );

        ClassPool libraryClassPool = IOUtil.read(
                libraryJars,
                "**",
                android,
                true,
                skipNonPublicLibraryClasses,
                skipNonPublicLibraryClassMembers,
                ignoreStackMapAttributes,
                dexConverter
        );

        // to print class count
        ClassCounter programClassCounter = new ClassCounter();
        programClassPool.classesAccept(programClassCounter);
        ClassCounter libraryClassCounter = new ClassCounter();
        libraryClassPool.classesAccept(libraryClassCounter);
        System.out.println("program classes: " + programClassCounter.getCount());
        System.out.println("library classes: " + libraryClassCounter.getCount());

        // TRANSFORM
        programClassPool.classesAccept(new MyTransformer());

        // Preverify
        programClassPool.classesAccept(
            new ClassVersionFilter(VersionConstants.CLASS_VERSION_1_6,
            new AllMethodVisitor(
            new AllAttributeVisitor(
            new CodePreverifier(false)))));

        // Writing

        int multiDexCount  = 1;
        int minSdkVersion  = 24;
        boolean debuggable = false;

        // Construct a filter for files that shouldn't be compressed.
        StringMatcher uncompressedFilter = ClassPathUtil.determineCompressionMethod(programJars);

        // Create a data entry writer factory for dex files.
        DexDataEntryWriterFactory dexDataEntryWriterFactory =
                dalvik ?
                        new DexDataEntryWriterFactory(
                                programClassPool,
                                libraryJars,
                                false,
                                multiDexCount,
                                minSdkVersion,
                                debuggable,
                                null) :
                        null;

        // Create a main data entry writer factory for all nested archives.
        DataEntryWriterFactory dataEntryWriterFactory =
                new DataEntryWriterFactory(
                        programClassPool,
                        null,
                        uncompressedFilter,
                        1,
                        false,
                        false,
                        null,
                        dexDataEntryWriterFactory == null ? null : dexDataEntryWriterFactory::wrapInDexWriter);


        DataEntryWriter writer = dataEntryWriterFactory
                .createDataEntryWriter(programJars,
                                       1,
                                       programJars.size(),
                                       null);

        // By default, just copy resource files into the above writers.
        DataEntryReader resourceCopier = new DataEntryCopier(writer);

        // Write classes.
        DataEntryReader classReader = new ClassFilter(new IdleRewriter(writer), resourceCopier);

        if (dalvik)
        {
            // Trigger writing classes loaded from dex files
            // without converting the code attributes again.
            DataEntryReader finalClassReader = classReader;
            classReader =
                    new NameFilteredDataEntryReader(
                            "classes*.dex",
                            dataEntry ->
                            {
                                ClassPool classPool = new ClassPool();
                                new DexClassReader(false, new ClassPoolFiller(classPool)).read(dataEntry);
                                for (Clazz programClass : classPool.classes())
                                {
                                    finalClassReader.read(new RenamedDataEntry(dataEntry, programClass.getName() + ".class"));
                                }
                            },
                            classReader
                    );
        }

        DataEntryReader reader = new DataEntryReaderFactory(android)
                .createDataEntryReader(inputEntry, classReader);

        // Create the data entry source.
        DataEntrySource source = new DirectorySource(inputEntry.getFile());

        // Pump the data entries into the reader.
        source.pumpDataEntries(reader);

        // Close all output entries.
        writer.close();


    }


    public static class MyTransformer implements ClassVisitor {
        @Override
        public void visitAnyClass(Clazz clazz) { }

        @Override
        public void visitProgramClass(ProgramClass programClass) {
            InstructionSequenceBuilder ____ =
                    new InstructionSequenceBuilder();

            Instruction[][] replacements =
                    {
                            ____.ldc("Hello, world!").__(),

                            ____.ldc("Hallo, wereld!").__()
                    };

            Constant[] constants = ____.constants();

            BranchTargetFinder branchTargetFinder  = new BranchTargetFinder();
            CodeAttributeEditor codeAttributeEditor = new CodeAttributeEditor();

            programClass.methodsAccept(
                    new AllAttributeVisitor(
                    new PeepholeEditor(branchTargetFinder, codeAttributeEditor,
                                      new InstructionSequenceReplacer(constants,
                                            replacements[0],
                                            constants,
                                            replacements[1],
                                            branchTargetFinder,
                                            codeAttributeEditor))));
        }
    }
}