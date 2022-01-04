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
package proguard.classfile.kotlin;

import proguard.classfile.*;
import proguard.classfile.editor.*;
import proguard.util.ProcessingFlags;

import java.util.*;

public class KotlinConstants
{
    public static final int METADATA_KIND_CLASS                   = 1;
    public static final int METADATA_KIND_FILE_FACADE             = 2;
    public static final int METADATA_KIND_SYNTHETIC_CLASS         = 3;
    public static final int METADATA_KIND_MULTI_FILE_CLASS_FACADE = 4;
    public static final int METADATA_KIND_MULTI_FILE_CLASS_PART   = 5;

    public static final char INNER_CLASS_SEPARATOR = '.';

    public static final char FUNCTION_NAME_MANGLE_SEPARATOR = '-';

    public static final String NAME_KOTLIN_METADATA                   = "kotlin/Metadata";
    public static final String TYPE_KOTLIN_METADATA                   = "Lkotlin/Metadata;";
    public static final String NAME_KOTLIN_ANY                        = "kotlin/Any";
    public static final String NAME_KOTLIN_UNIT                       = "kotlin/Unit";
    public static final String NAME_KOTLIN_ENUM                       = "kotlin/Enum";
    public static final String NAME_KOTLIN_FUNCTION                   = "kotlin/Function"; // kotlin/Function and also kotlin/FunctionN
    public static final String NAME_KOTLIN_EXTENSION_FUNCTION         = "kotlin/ExtensionFunctionType";
    public static final String NAME_KOTLIN_PARAMETER_NAME             = "kotlin/ParameterName";
    public static final String NAME_KOTLIN_COROUTINES_DEBUG_METADATA  = "kotlin/coroutines/jvm/internal/DebugMetadata";
    public static final String TYPE_KOTLIN_JVM_JVMNAME                = "Lkotlin/jvm/JvmName;";
    public static final String TYPE_KOTLIN_DEFAULT_CONSTRUCTOR_MARKER = "Lkotlin/jvm/internal/DefaultConstructorMarker;";

    public static final String DEFAULT_METHOD_SUFFIX                 = "$default";
    public static final String DEFAULT_IMPLEMENTATIONS_SUFFIX        = "$DefaultImpls";
    public static final String WHEN_MAPPINGS_SUFFIX                  = "$WhenMappings";

    public static final String KOTLIN_OBJECT_INSTANCE_FIELD_NAME     = "INSTANCE";

    public static final String KOTLIN_INTRINSICS_CLASS               = "kotlin/jvm/internal/Intrinsics";

    public static final class REFLECTION
    {
        public static final String CLASS_NAME                           = "kotlin/jvm/internal/Reflection";
        public static final String CALLABLE_REFERENCE_CLASS_NAME        = "kotlin/jvm/internal/CallableReference";
        public static final String FUNCTION_REFERENCE_CLASS_NAME        = "kotlin/jvm/internal/FunctionReference";
        public static final String PROPERTY_REFERENCE_CLASS_NAME        = "kotlin/jvm/internal/PropertyReference";
        public static final String LOCALVAR_REFERENCE_CLASS_NAME        = "kotlin/jvm/internal/LocalVariableReference";

        public static final String PROPERTY_REFERENCE_GET_METHOD_NAME   = "get";
        public static final String PROPERTY_REFERENCE_GET_METHOD_TYPE   = "()Ljava/lang/Object;";

        public static final String GETNAME_METHOD_NAME                  = "getName";
        public static final String GETNAME_METHOD_DESC                  = "()Ljava/lang/String;";

        public static final String GETSIGNATURE_METHOD_NAME             = "getSignature";
        public static final String GETSIGNATURE_METHOD_DESC             = "()Ljava/lang/String;";

        public static final String GETOWNER_METHOD_NAME                 = "getOwner";
        public static final String GETOWNER_METHOD_DESC                 = "()Lkotlin/reflect/KDeclarationContainer;";

        public static final String GETORCREATEKOTLINCLASS_METHOD_NAME   = "getOrCreateKotlinClass";
        public static final String GETORCREATEKOTLINCLASS_METHOD_DESC   = "(Ljava/lang/Class;)Lkotlin/reflect/KClass;";

        public static final String GETORCREATEKOTLINPACKAGE_METHOD_NAME = "getOrCreateKotlinPackage";
        public static final String GETORCREATEKOTLINPACKAGE_METHOD_DESC = "(Ljava/lang/Class;Ljava/lang/String;)Lkotlin/reflect/KDeclarationContainer;";
    }

    public static final class MODULE
    {
        public static final String FILE_EXTENSION  = ".kotlin_module";
        public static final String FILE_EXPRESSION = "META-INF/*" + FILE_EXTENSION;
    }

    private static final String[] KOTLIN_MAPPED_TYPES = {
        // Primitives
        "kotlin/Byte",
        "kotlin/Short",
        "kotlin/Int",
        "kotlin/Long",
        "kotlin/Char",
        "kotlin/Float",
        "kotlin/Double",
        "kotlin/Boolean",

        // Non-primitives
        "kotlin/Unit",
        "kotlin/Nothing",
        "kotlin/Any",
        "kotlin/Cloneable",
        "kotlin/Comparable",
        "kotlin/Enum",
        "kotlin/Annotation",
        "kotlin/CharSequence",
        "kotlin/String",
        "kotlin/Number",
        "kotlin/Throwable",

        // Collection types
        "kotlin/collections/Iterator",
        "kotlin/collections/Iterable",
        "kotlin/collections/Collection",
        "kotlin/collections/Set",
        "kotlin/collections/List",
        "kotlin/collections/ListIterator",
        "kotlin/collections/Map",
        "kotlin/collections/Map$Entry",
        "kotlin/collections/MutableIterator",
        "kotlin/collections/MutableIterable",
        "kotlin/collections/MutableCollection",
        "kotlin/collections/MutableSet",
        "kotlin/collections/MutableList",
        "kotlin/collections/MutableListIterator",
        "kotlin/collections/MutableMap",
        "kotlin/collections/MutableMap$MutableEntry",

        // Arrays
        "kotlin/Array",
        "kotlin/ByteArray",
        "kotlin/ShortArray",
        "kotlin/IntArray",
        "kotlin/LongArray",
        "kotlin/CharArray",
        "kotlin/FloatArray",
        "kotlin/DoubleArray",
        "kotlin/BooleanArray",

        // Primitive companions
        "kotlin/Byte$Companion",
        "kotlin/Short$Companion",
        "kotlin/Int$Companion",
        "kotlin/Long$Companion",
        "kotlin/Char$Companion",
        "kotlin/Float$Companion",
        "kotlin/Double$Companion",
        "kotlin/Boolean$Companion",

        // Non-primitive companions
        "kotlin/String$Companion",

        // kotlin/Function0..kotlin/FunctionN
        // kotlin/reflect/KFunction0..kotlin/reflect/KFunctionN
        // Created dynamically when encountered
        // See Kotlin documentation for more information:
        //  https://github.com/JetBrains/kotlin/blob/master/spec-docs/function-types.md

        ""
    };

    private static final Map<String, String> javaToKotlinTypeMap = new HashMap<>();

    static
    {
       javaToKotlinTypeMap.put("java/lang/Byte",      "kotlin/Byte");
       javaToKotlinTypeMap.put("java/lang/Short",     "kotlin/Short");
       javaToKotlinTypeMap.put("java/lang/Integer",   "kotlin/Int");
       javaToKotlinTypeMap.put("java/lang/Long",      "kotlin/Long");
       javaToKotlinTypeMap.put("java/lang/Character", "kotlin/Char");
       javaToKotlinTypeMap.put("java/lang/Float",     "kotlin/Float");
       javaToKotlinTypeMap.put("java/lang/Double",    "kotlin/Double");
       javaToKotlinTypeMap.put("java/lang/Boolean",   "kotlin/Boolean");

       javaToKotlinTypeMap.put("java/lang/Object",       "kotlin/Any");
       javaToKotlinTypeMap.put("java/lang/Cloneable",    "kotlin/Cloneable");
       javaToKotlinTypeMap.put("java/lang/Comparable",   "kotlin/Comparable");
       javaToKotlinTypeMap.put("java/lang/Enum",         "kotlin/Enum");
       javaToKotlinTypeMap.put("java/lang/Annotation",   "kotlin/Annotation");
       javaToKotlinTypeMap.put("java/lang/CharSequence", "kotlin/CharSequence");
       javaToKotlinTypeMap.put("java/lang/String",       "kotlin/String");
       javaToKotlinTypeMap.put("java/lang/Number",       "kotlin/Number");
       javaToKotlinTypeMap.put("java/lang/Throwable",    "kotlin/Throwable");

        javaToKotlinTypeMap.put("java/util/Iterator",     "kotlin/collections/Iterator");
        javaToKotlinTypeMap.put("java/lang/Iterable",     "kotlin/collections/Iterable");
        javaToKotlinTypeMap.put("java/util/Collection",   "kotlin/collections/Collection");
        javaToKotlinTypeMap.put("java/util/Set",          "kotlin/collections/Set");
        javaToKotlinTypeMap.put("java/util/List",         "kotlin/collections/List");
        javaToKotlinTypeMap.put("java/util/ListIterator", "kotlin/collections/ListIterator");
        javaToKotlinTypeMap.put("java/util/Map",          "kotlin/collections/Map");
        javaToKotlinTypeMap.put("java/util/Map$Entry",    "kotlin/collections/Map$Entry");
    }


    public static String metadataKindToString(int kind)
    {
        switch (kind)
        {
            case METADATA_KIND_CLASS:                   return "class";
            case METADATA_KIND_FILE_FACADE:             return "file facade";
            case METADATA_KIND_SYNTHETIC_CLASS:         return "synthetic class";
            case METADATA_KIND_MULTI_FILE_CLASS_FACADE: return "multi-file class facade";
            case METADATA_KIND_MULTI_FILE_CLASS_PART:   return "multi-file class part";
            default:                                    return "unknown";
        }
    }


    public static final ClassPool dummyClassPool = new ClassPool()
    {
        {
            for (String dummyType : KOTLIN_MAPPED_TYPES)
            {
                addClass(createDummyClass(dummyType));
            }
        }

        @Override
        public Clazz getClass(String className)
        {
            Clazz clazz = super.getClass(className);
            if (clazz == null && (className.startsWith("kotlin/Function") || className.startsWith("kotlin/reflect/KFunction")))
            {
                clazz = createDummyClass(className);
                super.addClass(clazz);
            }

            return clazz;
        }
    };

    /**
     * Get the Kotlin equivalent of a Java type.
     *
     * @param javaType the class of the Java type.
     *
     * @return Kotlin type class or the original Java type class if it doesn't have a Kotlin equivalent
     */
    public static Clazz getKotlinType(Clazz javaType)
    {
        String javaTypeName = javaType.getName();
        if (javaToKotlinTypeMap.containsKey(javaTypeName))
        {
            return dummyClassPool.getClass(javaToKotlinTypeMap.get(javaTypeName));
        }
        else
        {
            return javaType;
        }
    }

    // Small helper methods.

    private static ProgramClass createDummyClass(String name)
    {
        ClassBuilder editor = new ClassBuilder(
            55,
            AccessConstants.PUBLIC,
            name,
            ClassConstants.NAME_JAVA_LANG_OBJECT,
           null,
           ProcessingFlags.DONT_OBFUSCATE | ProcessingFlags.DONT_OPTIMIZE | ProcessingFlags.DONT_SHRINK,
           null
        );

        return editor.getProgramClass();
    }
}
