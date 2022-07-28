/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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
package proguard.classfile.io;

import proguard.classfile.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.visitor.ElementValueVisitor;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.*;
import proguard.classfile.util.kotlin.KotlinMetadataInitializer.MetadataType;
import proguard.classfile.visitor.*;
import proguard.io.RuntimeDataInput;

import java.io.*;
import java.util.*;

import static proguard.classfile.attribute.Attribute.RUNTIME_VISIBLE_ANNOTATIONS;
import static proguard.classfile.kotlin.KotlinConstants.TYPE_KOTLIN_METADATA;
import static proguard.classfile.util.kotlin.KotlinMetadataInitializer.isValidKotlinMetadataAnnotationField;
import static proguard.classfile.util.kotlin.KotlinMetadataInitializer.metadataTypeOf;

/**
 * This {@link ClassVisitor} fills out the {@link LibraryClass} instances that it visits with data
 * from the given {@link DataInput} object.
 *
 * @author Eric Lafortune
 */
public class LibraryClassReader
implements   ClassVisitor,
             MemberVisitor,
             ConstantVisitor
{
    private static final LibraryField[]  EMPTY_LIBRARY_FIELDS  = new LibraryField[0];
    private static final LibraryMethod[] EMPTY_LIBRARY_METHODS = new LibraryMethod[0];


    private final RuntimeDataInput dataInput;
    private final boolean          skipNonPublicClasses;
    private final boolean          skipNonPublicClassMembers;

    // A callback which can be used to build the Kotlin metadata model.
    private final KotlinMetadataElementValueConsumer kmElementValueConsumer;

    // A global array that acts as a parameter for the visitor methods.
    private Constant[]      constantPool;


    /**
     * Creates a new ProgramClassReader for reading from the given DataInput.
     */
    public LibraryClassReader(DataInput dataInput,
                              boolean   skipNonPublicClasses,
                              boolean   skipNonPublicClassMembers)
    {
        this(dataInput, skipNonPublicClasses, skipNonPublicClassMembers, null);
    }



    /**
     * Creates a new ProgramClassReader for reading from the given DataInput.
     */
    public LibraryClassReader(DataInput                          dataInput,
                              boolean                            skipNonPublicClasses,
                              boolean                            skipNonPublicClassMembers,
                              KotlinMetadataElementValueConsumer kmElementValueConsumer)
    {
        this.dataInput                 = new RuntimeDataInput(dataInput);
        this.skipNonPublicClasses      = skipNonPublicClasses;
        this.skipNonPublicClassMembers = skipNonPublicClassMembers;
        this.kmElementValueConsumer    = kmElementValueConsumer;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz) { }


    @Override
    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Read and check the magic number.
        int u4magic = dataInput.readInt();

        ClassUtil.checkMagicNumber(u4magic);

        // Read and check the version numbers.
        int u2minorVersion = dataInput.readUnsignedShort();
        int u2majorVersion = dataInput.readUnsignedShort();

        int u4version = ClassUtil.internalClassVersion(u2majorVersion,
                                                       u2minorVersion);

        ClassUtil.checkVersionNumbers(u4version);

        // Read the constant pool. Note that the first entry is not used.
        int u2constantPoolCount = dataInput.readUnsignedShort();

        // Create the constant pool array.
        constantPool = new Constant[u2constantPoolCount];

        for (int index = 1; index < u2constantPoolCount; index++)
        {
            Constant constant = createConstant();
            constant.accept(libraryClass, this);

            int tag = constant.getTag();
            if (tag == Constant.CLASS ||
                tag == Constant.UTF8 ||
                tag == Constant.INTEGER)
            {
                constantPool[index] = constant;
            }

            // Long constants and double constants take up two entries in the
            // constant pool.
            if (tag == Constant.LONG ||
                tag == Constant.DOUBLE)
            {
                index++;
            }
        }

        // Read the general class information.
        libraryClass.u2accessFlags = dataInput.readUnsignedShort();

        // We may stop parsing this library class if it's not public anyway.
        // E.g. only about 60% of all rt.jar classes need to be parsed.
        if (skipNonPublicClasses &&
            AccessUtil.accessLevel(libraryClass.getAccessFlags()) < AccessUtil.PUBLIC)
        {
            return;
        }

        // Read the class and super class indices.
        int u2thisClass  = dataInput.readUnsignedShort();
        int u2superClass = dataInput.readUnsignedShort();

        // Store their actual names.
        libraryClass.thisClassName  = getClassName(u2thisClass);
        libraryClass.superClassName = (u2superClass == 0) ? null :
                                      getClassName(u2superClass);

        // Read the interfaces
        int u2interfacesCount = dataInput.readUnsignedShort();

        libraryClass.interfaceNames = new String[u2interfacesCount];
        for (int index = 0; index < u2interfacesCount; index++)
        {
            // Store the actual interface name.
            int u2interface = dataInput.readUnsignedShort();
            libraryClass.interfaceNames[index] = getClassName(u2interface);
        }

        // Read the fields.
        int u2fieldsCount = dataInput.readUnsignedShort();

        // Create the fields array.
        LibraryField[] reusableFields = new LibraryField[u2fieldsCount];

        int visibleFieldsCount = 0;
        for (int index = 0; index < u2fieldsCount; index++)
        {
            LibraryField field = new LibraryField();
            this.visitLibraryMember(libraryClass, field);

            // Only store fields that are visible, except if
            // we're building the Kotlin metadata model, we may
            // need private fields such as backing fields,
            // to initialize the model references fully.
            if (kmElementValueConsumer != null ||
                (AccessUtil.accessLevel(field.getAccessFlags()) >=
                (skipNonPublicClassMembers ? AccessUtil.PROTECTED :
                                             AccessUtil.PACKAGE_VISIBLE)))
            {
                reusableFields[visibleFieldsCount++] = field;
            }
        }

        // Copy the visible fields (if any) into a fields array of the right size.
        if (visibleFieldsCount == 0)
        {
            libraryClass.fields = EMPTY_LIBRARY_FIELDS;
        }
        else
        {
            libraryClass.fields = new LibraryField[visibleFieldsCount];
            System.arraycopy(reusableFields, 0, libraryClass.fields, 0, visibleFieldsCount);
        }

        // Read the methods.
        int u2methodsCount = dataInput.readUnsignedShort();

        // Create the methods array.
        LibraryMethod[] reusableMethods = new LibraryMethod[u2methodsCount];

        int visibleMethodsCount = 0;
        for (int index = 0; index < u2methodsCount; index++)
        {
            LibraryMethod method = new LibraryMethod();
            this.visitLibraryMember(libraryClass, method);

            // Only store methods that are visible, except if
            // we're building the Kotlin metadata model, we may need
            // private members such as private constructors,
            // to initialize the model references fully.
            if (kmElementValueConsumer != null ||
                (AccessUtil.accessLevel(method.getAccessFlags()) >=
                (skipNonPublicClassMembers ? AccessUtil.PROTECTED :
                                             AccessUtil.PACKAGE_VISIBLE)))
            {
                reusableMethods[visibleMethodsCount++] = method;
            }
        }

        // Copy the visible methods (if any) into a methods array of the right size.
        if (visibleMethodsCount == 0)
        {
            libraryClass.methods = EMPTY_LIBRARY_METHODS;
        }
        else
        {
            libraryClass.methods = new LibraryMethod[visibleMethodsCount];
            System.arraycopy(reusableMethods, 0, libraryClass.methods, 0, visibleMethodsCount);
        }

        skipClassAttributes(libraryClass);
    }


    // Implementations for MemberVisitor.

    public void visitProgramMember(ProgramClass libraryClass, ProgramMember libraryMember)
    {
    }


    public void visitLibraryMember(LibraryClass libraryClass, LibraryMember libraryMember)
    {
        // Read the general field information.
        libraryMember.u2accessFlags = dataInput.readUnsignedShort();
        libraryMember.name          = getString(dataInput.readUnsignedShort());
        libraryMember.descriptor    = getString(dataInput.readUnsignedShort());

        skipMemberAttributes();
    }


    // Implementations for ConstantVisitor.

    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
    {
        integerConstant.u4value = dataInput.readInt();
    }


    public void visitLongConstant(Clazz clazz, LongConstant longConstant)
    {
        dataInput.skipBytes(8);
    }


    public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant)
    {
        dataInput.skipBytes(4);
    }


    public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant)
    {
        dataInput.skipBytes(8);
    }


    public void visitPrimitiveArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant)
    {
        char u2primitiveType = dataInput.readChar();
        int  u4length        = dataInput.readInt();

        dataInput.skipBytes(primitiveSize(u2primitiveType) * u4length);
    }


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        dataInput.skipBytes(2);
    }


    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        int u2length = dataInput.readUnsignedShort();

        // Read the UTF-8 bytes.
        byte[] bytes = new byte[u2length];
        dataInput.readFully(bytes);
        utf8Constant.setBytes(bytes);
    }


    public void visitDynamicConstant(Clazz clazz, DynamicConstant dynamicConstant)
    {
        dataInput.skipBytes(4);
    }


    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        dataInput.skipBytes(4);
    }


    public void visitMethodHandleConstant(Clazz clazz, MethodHandleConstant methodHandleConstant)
    {
        dataInput.skipBytes(3);
    }


    public void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
    {
        dataInput.skipBytes(4);
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        classConstant.u2nameIndex = dataInput.readUnsignedShort();
    }


    public void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant methodTypeConstant)
    {
        dataInput.skipBytes(2);
    }


    public void visitNameAndTypeConstant(Clazz clazz, NameAndTypeConstant nameAndTypeConstant)
    {
        dataInput.skipBytes(4);
    }


    public void visitModuleConstant(Clazz clazz, ModuleConstant moduleConstant)
    {
        dataInput.skipBytes(2);
    }


    public void visitPackageConstant(Clazz clazz, PackageConstant packageConstant)
    {
        dataInput.skipBytes(2);
    }

    // Small utility methods.

    /**
     * Returns the class name of the ClassConstant at the specified index in the
     * reusable constant pool.
     */
    private String getClassName(int constantIndex)
    {
        ClassConstant classEntry = (ClassConstant)constantPool[constantIndex];

        return getString(classEntry.u2nameIndex);
    }


    /**
     * Returns the string of the Utf8Constant at the specified index in the
     * reusable constant pool.
     */
    private String getString(int constantIndex)
    {
        return ((Utf8Constant)constantPool[constantIndex]).getString();
    }

    /**
     * Returns the {@link IntegerConstant} at the specified index in the
     * reusable constant pool.
     */
    private int getInteger(int constantIndex)
    {
        return ((IntegerConstant)constantPool[constantIndex]).getValue();
    }


    private Constant createConstant()
    {
        int u1tag = dataInput.readUnsignedByte();

        switch (u1tag)
        {
            case Constant.INTEGER:             return new IntegerConstant();
            case Constant.FLOAT:               return new FloatConstant();
            case Constant.LONG:                return new LongConstant();
            case Constant.DOUBLE:              return new DoubleConstant();
            case Constant.STRING:              return new StringConstant();
            case Constant.UTF8:                return new Utf8Constant();
            case Constant.DYNAMIC:             return new DynamicConstant();
            case Constant.INVOKE_DYNAMIC:      return new InvokeDynamicConstant();
            case Constant.METHOD_HANDLE:       return new MethodHandleConstant();
            case Constant.FIELDREF:            return new FieldrefConstant();
            case Constant.METHODREF:           return new MethodrefConstant();
            case Constant.INTERFACE_METHODREF: return new InterfaceMethodrefConstant();
            case Constant.CLASS:               return new ClassConstant();
            case Constant.METHOD_TYPE:         return new MethodTypeConstant();
            case Constant.NAME_AND_TYPE:       return new NameAndTypeConstant();
            case Constant.MODULE:              return new ModuleConstant();
            case Constant.PACKAGE:             return new PackageConstant();

            default: throw new RuntimeException("Unknown constant type ["+u1tag+"] in constant pool");
        }
    }


    private void skipClassAttributes(LibraryClass libraryClass)
    {
        int u2attributesCount = dataInput.readUnsignedShort();

        for (int index = 0; index < u2attributesCount; index++)
        {
            int    u2attributeNameIndex = dataInput.readUnsignedShort();
            String attributeName        = getString(u2attributeNameIndex);
            if (kmElementValueConsumer != null && RUNTIME_VISIBLE_ANNOTATIONS.equals(attributeName))
            {
                skipAttributeOrReadKotlinMetadataAnnotation(libraryClass);
            }
            else
            {
                skipAttribute();
            }
        }
    }

    private void skipMemberAttributes()
    {
        int u2attributesCount = dataInput.readUnsignedShort();

        for (int index = 0; index < u2attributesCount; index++)
        {
            // u2attributeNameIndex
            dataInput.skipBytes(2);

            skipAttribute();
        }
    }

    private void skipAttribute()
    {
        int u4attributeLength = dataInput.readInt();
        dataInput.skipBytes(u4attributeLength);
    }

    private void skipAttributeOrReadKotlinMetadataAnnotation(LibraryClass libraryClass)
    {
        // u4attributeLength
        dataInput.skipBytes(4);

        int u2annotationsCount = dataInput.readUnsignedShort();
        for (int index = 0; index < u2annotationsCount; index++)
        {
            skipAnnotationOrReadKotlinMetadataAnnotation(libraryClass);
        }
    }


    /**
     * Returns the size in bytes of the given primitive type.
     */
    private int primitiveSize(char primitiveType)
    {
        switch (primitiveType)
        {
            case TypeConstants.BOOLEAN:
            case TypeConstants.BYTE:    return 1;
            case TypeConstants.CHAR:
            case TypeConstants.SHORT:   return 2;
            case TypeConstants.INT:
            case TypeConstants.FLOAT:   return 4;
            case TypeConstants.LONG:
            case TypeConstants.DOUBLE:  return 8;
        }

        return 0;
    }

    // Helpers for reading the {@link kotlin.Metadata} annotation.
    
    private void skipAnnotationOrReadKotlinMetadataAnnotation(Clazz clazz)
    {
        Annotation annotation  = new Annotation();
        annotation.u2typeIndex = dataInput.readUnsignedShort();
        String annotationType  = getString(annotation.u2typeIndex);

        if (!TYPE_KOTLIN_METADATA.equals(annotationType))
        {
            skipAnnotationRemainingBytes(false, clazz, annotation);
            return;
        }

        annotation.u2elementValuesCount = dataInput.readUnsignedShort();

        KotlinMetadataElementValues kmValues = new KotlinMetadataElementValues();
        for (int index = 0; index < annotation.u2elementValuesCount; index++)
        {
            int u2elementNameIndex          = dataInput.readUnsignedShort();
            ElementValue elementValue       = createElementValue();
            elementValue.u2elementNameIndex = u2elementNameIndex;
            String elementName              = getString(u2elementNameIndex);

            elementValue.accept(
                clazz,
                annotation,
                isValidKotlinMetadataAnnotationField(elementName) ?
                    new KotlinMetadataAnnotationElementValueReader(
                        metadataTypeOf(elementName),
                        kmValues
                    ) :
                    new SkipAnnotationElementVisitor()
            );
        }

        kmElementValueConsumer.accept(
            kmValues.k,
            kmValues.mv == null ? null : kmValues.mv.stream().mapToInt(i -> i).toArray(),
            kmValues.d1 == null ? null : kmValues.d1.toArray(new String[0]),
            kmValues.d2 == null ? null : kmValues.d2.toArray(new String[0]),
            kmValues.xi,
            kmValues.xs,
            kmValues.pn
        );
    }

    private static final class KotlinMetadataElementValues
    {
        public int           k = -1;
        public List<Integer> mv;
        public List<String>  d1;
        public List<String>  d2;
        public int           xi = 0;
        public String        xs;
        public String        pn;
    }

    private ElementValue createElementValue()
    {
        int u1tag = dataInput.readUnsignedByte();

        switch (u1tag)
        {
            case TypeConstants.BOOLEAN:
            case TypeConstants.BYTE:
            case TypeConstants.CHAR:
            case TypeConstants.SHORT:
            case TypeConstants.INT:
            case TypeConstants.FLOAT:
            case TypeConstants.LONG:
            case TypeConstants.DOUBLE:
            case ElementValue.TAG_STRING_CONSTANT: return new ConstantElementValue((char)u1tag);
            case ElementValue.TAG_ENUM_CONSTANT:   return new EnumConstantElementValue();
            case ElementValue.TAG_CLASS:           return new ClassElementValue();
            case ElementValue.TAG_ANNOTATION:      return new AnnotationElementValue();
            case ElementValue.TAG_ARRAY:           return new ArrayElementValue();

            default: throw new IllegalArgumentException("Unknown element value tag ["+u1tag+"]");
        }
    }


    private class KotlinMetadataAnnotationElementValueReader implements ElementValueVisitor
    {

        private final MetadataType                elementName;
        private final KotlinMetadataElementValues kotlinMetadataFields;


        public KotlinMetadataAnnotationElementValueReader(MetadataType                elementName,
                                                          KotlinMetadataElementValues kotlinMetadataFields)
        {
            this.elementName          = elementName;
            this.kotlinMetadataFields = kotlinMetadataFields;
        }


        @Override
        public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
        {
            int u2constantValueIndex = dataInput.readUnsignedShort();

            switch (elementName)
            {
                case mv: if (kotlinMetadataFields.mv == null) kotlinMetadataFields.mv = new ArrayList<>(); break;
                case d1: if (kotlinMetadataFields.d1 == null) kotlinMetadataFields.d1 = new ArrayList<>(); break;
                case d2: if (kotlinMetadataFields.d2 == null) kotlinMetadataFields.d2 = new ArrayList<>(); break;
            }

            switch (elementName)
            {
                case  k: kotlinMetadataFields.k = getInteger(u2constantValueIndex); break;
                case mv: kotlinMetadataFields.mv.add(getInteger(u2constantValueIndex)); break;
                case d1: kotlinMetadataFields.d1.add(getString(u2constantValueIndex)); break;
                case d2: kotlinMetadataFields.d2.add(getString(u2constantValueIndex)); break;
                case xi: kotlinMetadataFields.xi = getInteger(u2constantValueIndex); break;
                case xs: kotlinMetadataFields.xs = getString(u2constantValueIndex); break;
                case pn: kotlinMetadataFields.pn = getString(u2constantValueIndex); break;
            }
        }

        @Override
        public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
        {
            int u2elementValuesCount = dataInput.readUnsignedShort();

            for (int index = 0; index < u2elementValuesCount; index++)
            {
                ElementValue elementValue = createElementValue();
                elementValue.accept(clazz, annotation, this);
            }
        }
    }

    private void skipAnnotationRemainingBytes(boolean readTypeIndex, Clazz clazz, Annotation annotation)
    {
        if (readTypeIndex)
        {
            // u2typeIndex
            dataInput.skipBytes(2);
        }
        annotation.u2elementValuesCount = dataInput.readUnsignedShort();
        for (int index = 0; index < annotation.u2elementValuesCount; index++)
        {
            // u2elementNameIndex
            dataInput.skipBytes(2);
            createElementValue().accept(clazz, annotation, new SkipAnnotationElementVisitor());
        }
    }

    private class SkipAnnotationElementVisitor implements ElementValueVisitor
    {

        @Override
        public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
        {
            // u2constantValueIndex
            dataInput.skipBytes(2);
        }


        @Override
        public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
        {
            // u2typeNameIndex
            dataInput.skipBytes(2);
            // u2constantNameIndex
            dataInput.skipBytes(2);
        }


        @Override
        public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
        {
            // u2classInfoIndex
            dataInput.skipBytes(2);
        }


        @Override
        public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
        {
            skipAnnotationRemainingBytes(/* readTypeIndex = */ true, clazz, new Annotation());
        }


        @Override
        public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
        {
            int u2elementValuesCount = dataInput.readUnsignedShort();

            for (int index = 0; index < u2elementValuesCount; index++)
            {
                createElementValue().accept(clazz, annotation, this);
            }
        }
    }

    public interface KotlinMetadataElementValueConsumer
    {
        void accept(int k, int[] mv, String[] d1, String[] d2, int xi, String xs, String pn);
    }
}
