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
package proguard.evaluation.value;

import static java.util.stream.Collectors.joining;
import static proguard.classfile.util.ClassUtil.externalClassName;
import static proguard.classfile.util.ClassUtil.isInternalArrayType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.AccessConstants;
import proguard.classfile.ClassConstants;
import proguard.classfile.Clazz;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassCollector;
import proguard.classfile.visitor.ClassVisitor;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.exception.ArrayStoreTypeException;
import proguard.evaluation.exception.IncompleteClassHierarchyException;
import proguard.evaluation.exception.ValueTypeException;
import proguard.evaluation.value.object.AnalyzedObjectFactory;

/**
 * This {@link ReferenceValue} represents a partially evaluated reference value. It has a type and a
 * flag that indicates whether the value could be <code>null</code>. If the type is <code>null
 * </code>, the value is <code>null</code>.
 *
 * @author Eric Lafortune
 */
public class TypedReferenceValue extends ReferenceValue {

  /** Indicates whether there were occurrences of {@link IncompleteClassHierarchyException}. */
  public static boolean INCOMPLETE_CLASS_HIERARCHY = false;

  /** If true, do not throw an {@link IncompleteClassHierarchyException} when one would occur. */
  public static boolean ALLOW_INCOMPLETE_CLASS_HIERARCHY =
      System.getProperty("allow.incomplete.class.hierarchy") != null;

  /**
   * If true, print warnings for occurrences of {@link IncompleteClassHierarchyException} when
   * ALLOW_INCOMPLETE_CLASS_HIERARCHY is enabled.
   */
  public static boolean WARN_INCOMPLETE_CLASS_HIERARCHY = false;

  private static final boolean DEBUG = false;

  private static final Logger logger = LogManager.getLogger(TypedReferenceValue.class);

  protected final String type;
  protected final Clazz referencedClass;
  protected final boolean mayBeExtension;
  protected final boolean mayBeNull;

  /** Creates a new TypedReferenceValue. */
  public TypedReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull) {
    this.type = type;
    this.referencedClass = referencedClass;
    this.mayBeExtension = mayBeExtension;
    this.mayBeNull = mayBeNull;
  }

  // Implementations for ReferenceValue.

  public String getType() {
    return type;
  }

  public Clazz getReferencedClass() {
    return referencedClass;
  }

  // Implementations of unary methods of ReferenceValue.

  public boolean mayBeExtension() {
    return mayBeExtension;
  }

  public int isNull() {
    return type == null ? ALWAYS : mayBeNull ? MAYBE : NEVER;
  }

  public int instanceOf(String otherType, Clazz otherReferencedClass) {
    String thisType = this.type;

    // If this type is null, it is never an instance of any class.
    if (thisType == null) {
      return NEVER;
    }

    // Start taking into account the type dimensions.
    int thisDimensionCount = ClassUtil.internalArrayTypeDimensionCount(thisType);
    int otherDimensionCount = ClassUtil.internalArrayTypeDimensionCount(otherType);
    int commonDimensionCount = Math.min(thisDimensionCount, otherDimensionCount);

    // Strip any common array prefixes.
    thisType = thisType.substring(commonDimensionCount);
    otherType = otherType.substring(commonDimensionCount);

    // If either stripped type is a primitive type, we can tell right away.
    if (commonDimensionCount > 0
        && (ClassUtil.isInternalPrimitiveType(thisType.charAt(0))
            || ClassUtil.isInternalPrimitiveType(otherType.charAt(0)))) {
      return !thisType.equals(otherType) ? NEVER : mayBeNull ? MAYBE : ALWAYS;
    }

    // If this type is an array type, and the other type is not
    // java.lang.Object, java.lang.Cloneable, or java.io.Serializable,
    // this type can never be an instance.
    if (thisDimensionCount > otherDimensionCount
        && !ClassUtil.isInternalArrayInterfaceName(
            ClassUtil.internalClassNameFromClassType(otherType))) {
      return NEVER;
    }

    // If the other type is an array type, and this type is not
    // java.lang.Object, java.lang.Cloneable, or java.io.Serializable,
    // this type can never be an instance.
    if (thisDimensionCount < otherDimensionCount
        && !ClassUtil.isInternalArrayInterfaceName(
            ClassUtil.internalClassNameFromClassType(thisType))) {
      return NEVER;
    }

    // If this type is equal to the other type, or if the other type is
    // java.lang.Object, or if this type is an array type, then this type
    // is always an instance (unless it may be null).
    if (thisType.equals(otherType)
        || ClassConstants.TYPE_JAVA_LANG_OBJECT.equals(otherType)
        || thisDimensionCount > otherDimensionCount) {
      return mayBeNull ? MAYBE : ALWAYS;
    }

    // If the other type is an array type, it might be ok.
    if (thisDimensionCount < otherDimensionCount) {
      return MAYBE;
    }

    // If the value extends the type, we're sure (unless it may be null).
    // Otherwise, if the value type is final, it can never be an instance.
    // Also, if the types are not interfaces and not in the same hierarchy,
    // the value can never be an instance.
    return referencedClass == null || otherReferencedClass == null
        ? MAYBE
        : referencedClass.extendsOrImplements(otherReferencedClass)
            ? mayBeNull ? MAYBE : ALWAYS
            : (referencedClass.getAccessFlags() & AccessConstants.FINAL) != 0
                ? NEVER
                : (referencedClass.getAccessFlags() & AccessConstants.INTERFACE) == 0
                        && (otherReferencedClass.getAccessFlags() & AccessConstants.INTERFACE) == 0
                        && !otherReferencedClass.extendsOrImplements(referencedClass)
                    ? NEVER
                    : MAYBE;
  }

  public ReferenceValue cast(
      String type, Clazz referencedClass, ValueFactory valueFactory, boolean alwaysCast) {

    // If `type` is null the TypedReferenceValue represents a value with unknown type and null value
    if (this.type == null) {
      if (alwaysCast) {
        // The value is still null but the type is known (if the value factory allows it)
        return valueFactory.createReferenceValue(
            referencedClass, true, true, AnalyzedObjectFactory.createNullOfType(type));
      } else {
        return this;
      }
    }

    // Just return this value if it's the same type or if the current type is more specific than the
    // checked one
    return (this.type.equals(type) || (!alwaysCast && instanceOf(type, referencedClass) == ALWAYS))
        ? this
        : valueFactory.createReferenceValue(type, referencedClass, true, mayBeNull);
  }

  public ReferenceValue generalizeMayBeNull(boolean mayBeNull) {
    return this.mayBeNull == mayBeNull
        ? this
        : new TypedReferenceValue(type, referencedClass, mayBeExtension, true);
  }

  public ReferenceValue referenceArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    return type == null
        ? TypedReferenceValueFactory.REFERENCE_VALUE_NULL
        : !ClassUtil.isInternalArrayType(type)
            ? TypedReferenceValueFactory.REFERENCE_VALUE_JAVA_LANG_OBJECT_MAYBE_NULL
            : valueFactory
                .createValue(type.substring(1), referencedClass, true, true)
                .referenceValue();
  }

  // Implementations of binary methods of ReferenceValue.

  public ReferenceValue generalize(ReferenceValue other) {
    return other.generalize(this);
  }

  public int equal(ReferenceValue other) {
    return other.equal(this);
  }

  //    // Implementations of binary ReferenceValue methods with
  //    // UnknownReferenceValue arguments.
  //
  //    public ReferenceValue generalize(UnknownReferenceValue other)
  //    {
  //        return other;
  //    }
  //
  //
  //    public int equal(UnknownReferenceValue other)
  //    {
  //        return MAYBE;
  //    }

  // Implementations of binary ReferenceValue methods with TypedReferenceValue
  // arguments.

  public ReferenceValue generalize(TypedReferenceValue other) {
    // If both types are identical, the generalization is the same too.
    if (this.equals(other)) {
      return this;
    }

    String thisType = this.type;
    String otherType = other.type;

    // If both types are null, the generalization is null too.
    if (thisType == null && otherType == null) {
      return TypedReferenceValueFactory.REFERENCE_VALUE_NULL;
    }

    // If this type is null, the generalization is the other type, maybe null.
    if (thisType == null) {
      return other.generalizeMayBeNull(true);
    }

    // If the other type is null, the generalization is this type, maybe null.
    if (otherType == null) {
      return this.generalizeMayBeNull(true);
    }

    boolean mayBeExtension = this.mayBeExtension || other.mayBeExtension;
    boolean mayBeNull = this.mayBeNull || other.mayBeNull;

    // If the two types are equal, the generalization remains the same,
    // maybe an extension, maybe null.
    if (thisType.equals(otherType)) {
      return typedReferenceValue(this, mayBeExtension, mayBeNull);
    }

    // Start taking into account the type dimensions.
    int thisDimensionCount = ClassUtil.internalArrayTypeDimensionCount(thisType);
    int otherDimensionCount = ClassUtil.internalArrayTypeDimensionCount(otherType);
    int commonDimensionCount = Math.min(thisDimensionCount, otherDimensionCount);

    if (thisDimensionCount == otherDimensionCount) {
      // See if we can take into account the referenced classes.
      Clazz thisReferencedClass = this.referencedClass;
      Clazz otherReferencedClass = other.referencedClass;

      if (thisReferencedClass != null && otherReferencedClass != null) {
        // Is one class simply an extension of the other one?
        if (thisReferencedClass.extendsOrImplements(otherReferencedClass)) {
          return typedReferenceValue(other, true, mayBeNull);
        }

        if (otherReferencedClass.extendsOrImplements(thisReferencedClass)) {
          return typedReferenceValue(this, true, mayBeNull);
        }

        try {
          // Do the classes have a non-trivial common superclass?
          Clazz commonClass = findCommonClass(thisReferencedClass, otherReferencedClass, false);

          if (commonClass.getName().equals(ClassConstants.NAME_JAVA_LANG_OBJECT)) {
            // Otherwise, do the classes have a common interface?
            Clazz commonInterface =
                findCommonClass(thisReferencedClass, otherReferencedClass, true);
            if (commonInterface != null) {
              commonClass = commonInterface;
            }
          }

          return new TypedReferenceValue(
              commonDimensionCount == 0
                  ? ClassUtil.internalTypeFromClassName(commonClass.getName())
                  : ClassUtil.internalArrayTypeFromClassName(
                      commonClass.getName(), commonDimensionCount),
              commonClass,
              true,
              mayBeNull);
        } catch (IncompleteClassHierarchyException e) {
          // The class hierarchy seems to be incomplete.
          if (ALLOW_INCOMPLETE_CLASS_HIERARCHY) {
            if (WARN_INCOMPLETE_CLASS_HIERARCHY) {
              logger.warn("Warning: Incomplete class hierarchy: {}", e.getMessage());
            }
            // We'll return an unknown reference value.
            return BasicValueFactory.REFERENCE_VALUE;
          }

          throw e;
        }
      }
    } else if (thisDimensionCount > otherDimensionCount) {
      // See if the other type is an interface type of arrays.
      if (ClassUtil.isInternalArrayInterfaceName(
          ClassUtil.internalClassNameFromClassType(otherType))) {
        return typedReferenceValue(other, true, mayBeNull);
      }
    } else // if (thisDimensionCount < otherDimensionCount)
    {
      // See if this type is an interface type of arrays.
      if (ClassUtil.isInternalArrayInterfaceName(
          ClassUtil.internalClassNameFromClassType(thisType))) {
        return typedReferenceValue(this, true, mayBeNull);
      }
    }

    // Reduce the common dimension count if either type is an array of
    // primitives type of this dimension.
    if (commonDimensionCount > 0
        && (ClassUtil.isInternalPrimitiveType(otherType.charAt(commonDimensionCount))
            || ClassUtil.isInternalPrimitiveType(thisType.charAt(commonDimensionCount)))) {
      commonDimensionCount--;
    }

    // Fall back on a basic Object or array of Objects type.
    return commonDimensionCount != 0
        ? new TypedReferenceValue(
            ClassUtil.internalArrayTypeFromClassName(
                ClassConstants.NAME_JAVA_LANG_OBJECT, commonDimensionCount),
            null,
            true,
            mayBeNull)
        : mayBeNull
            ? TypedReferenceValueFactory.REFERENCE_VALUE_JAVA_LANG_OBJECT_MAYBE_NULL
            : TypedReferenceValueFactory.REFERENCE_VALUE_JAVA_LANG_OBJECT_NOT_NULL;
  }

  /** Returns whether incomplete class hierarchies are allowed. */
  public static boolean allowsIncompleteClassHierarchy() {
    return ALLOW_INCOMPLETE_CLASS_HIERARCHY;
  }

  /**
   * Returns the most specific common superclass or interface of the given classes.
   *
   * @param class1 the first class.
   * @param class2 the second class.
   * @param interfaces specifies whether to look for a superclass or for an interface.
   * @return the common class.
   * @throws IncompleteClassHierarchyException if a common class cannot be found.
   */
  private Clazz findCommonClass(Clazz class1, Clazz class2, boolean interfaces) {
    // Collect the superclasses or the interfaces of this class.
    Set<Clazz> superClasses1 = new HashSet<>();
    class1.hierarchyAccept(
        !interfaces, !interfaces, interfaces, false, new ClassCollector(superClasses1));

    int superClasses1Count = superClasses1.size();
    if (superClasses1Count == 0) {
      if (interfaces) {
        return null;
      } else if (class1.getSuperName() != null) {
        throw new IncompleteClassHierarchyException(
            "Can't find any super classes of ["
                + class1.getName()
                + "] (not even immediate super class ["
                + class1.getSuperName()
                + "])");
      }
    }

    // Collect the superclasses or the interfaces of the other class.
    Set<Clazz> superClasses2 = new HashSet<>();
    class2.hierarchyAccept(
        !interfaces, !interfaces, interfaces, false, new ClassCollector(superClasses2));

    int superClasses2Count = superClasses2.size();
    if (superClasses2Count == 0) {
      if (interfaces) {
        return null;
      } else if (class2.getSuperName() != null) {
        throw new IncompleteClassHierarchyException(
            "Can't find any super classes of ["
                + class2.getName()
                + "] (not even immediate super class ["
                + class2.getSuperName()
                + "])");
      }
    }

    if (DEBUG) {
      System.out.println(
          "ReferenceValue.generalize this ["
              + class1.getName()
              + "] with other ["
              + class2.getName()
              + "] (interfaces = "
              + interfaces
              + ")");
      System.out.println("  This super classes:  " + superClasses1);
      System.out.println("  Other super classes: " + superClasses2);
    }

    // Find the common superclasses.
    Set<Clazz> commonClasses = new HashSet<>(superClasses1);
    commonClasses.retainAll(superClasses2);

    if (DEBUG) {
      System.out.println("  Common super classes: " + commonClasses);
    }

    if (interfaces && commonClasses.isEmpty()) {
      return null;
    }

    // Find a class that is a subclass of all common superclasses,
    // or that at least has the maximum number of common superclasses.
    Clazz commonClass = null;

    int maximumSuperClassCount = -1;

    // Go over all common superclasses to find it. In case of
    // multiple subclasses, keep the lowest one alphabetically,
    // in order to ensure that the choice is deterministic.
    for (Clazz commonSuperClass : commonClasses) {
      int superClassCount = superClassCount(commonSuperClass, commonClasses);
      if (maximumSuperClassCount < superClassCount
          || (maximumSuperClassCount == superClassCount
              && commonClass != null
              && commonClass.getName().compareTo(commonSuperClass.getName()) > 0)) {
        commonClass = commonSuperClass;
        maximumSuperClassCount = superClassCount;
      }
    }

    if (commonClass == null) {
      // No common superclass could be found. This usually happens
      // when classes are missing in the classpool due to incomplete
      // configurations.

      // In case one of the two classes is java/lang/Object itself,
      // or is a final class that extends java/lang/Object, we can
      // safely assume that java/lang/Object must be the common
      // superclass of both.

      for (Clazz clazz : Arrays.asList(class1, class2)) {
        if (ClassConstants.NAME_JAVA_LANG_OBJECT.equals(clazz.getName())) {
          commonClass = clazz;
          break;
        } else if ((clazz.getAccessFlags() & AccessConstants.FINAL) != 0
            && ClassConstants.NAME_JAVA_LANG_OBJECT.equals(clazz.getSuperName())) {
          commonClass = clazz.getSuperClass();
          break;
        }
      }
    }

    if (commonClass == null) {
      Set<String> unknownClasses1 = new HashSet<>();
      class1.hierarchyAccept(
          !interfaces,
          !interfaces,
          interfaces,
          false,
          new UnknownSuperClassNameCollector(unknownClasses1));

      Set<String> unknownClasses2 = new HashSet<>();
      class2.hierarchyAccept(
          !interfaces,
          !interfaces,
          interfaces,
          false,
          new UnknownSuperClassNameCollector(unknownClasses2));

      throw new IncompleteClassHierarchyException(
          "Can't find common super class of ["
              + externalClassName(class1.getName())
              + "] (with "
              + superClasses1Count
              + " known super classes: "
              + superClasses1.stream()
                  .map(clazz -> externalClassName(clazz.getName()))
                  .collect(joining(", "))
              + (!unknownClasses1.isEmpty()
                  ? " and "
                      + unknownClasses1.size()
                      + " unknown classes: "
                      + unknownClasses1.stream()
                          .map(ClassUtil::externalClassName)
                          .collect(joining(", "))
                  : "")
              + ") and ["
              + externalClassName(class2.getName())
              + "] (with "
              + superClasses2Count
              + " known super classes: "
              + superClasses2.stream()
                  .map(clazz -> externalClassName(clazz.getName()))
                  .collect(joining(", "))
              + (!unknownClasses2.isEmpty()
                  ? " and "
                      + unknownClasses2.size()
                      + " unknown classes: "
                      + unknownClasses2.stream()
                          .map(ClassUtil::externalClassName)
                          .collect(joining(", "))
                  : "")
              + ")");
    }

    if (DEBUG) {
      System.out.println("  Best common class: [" + commonClass.getName() + "]");
    }

    return commonClass;
  }

  private static class UnknownSuperClassNameCollector implements ClassVisitor {
    private final Collection<String> unknownSuperClassNames;

    UnknownSuperClassNameCollector(Collection<String> unknownSuperClassNames) {
      this.unknownSuperClassNames = unknownSuperClassNames;
    }

    @Override
    public void visitAnyClass(Clazz clazz) {
      if (clazz.getSuperName() != null && clazz.getSuperClass() == null) {
        unknownSuperClassNames.add(clazz.getSuperName());
      }
    }
  }

  /**
   * Returns the given reference value that may or may not be null, ensuring that it is a
   * TypedReferenceValue, not a subclass.
   */
  private static ReferenceValue typedReferenceValue(
      TypedReferenceValue referenceValue, boolean mayBeExtension, boolean mayBeNull) {
    return referenceValue.getClass() == TypedReferenceValue.class
            && referenceValue.mayBeExtension == mayBeExtension
        ? referenceValue.generalizeMayBeNull(mayBeNull)
        : new TypedReferenceValue(
            referenceValue.type, referenceValue.referencedClass, mayBeExtension, mayBeNull);
  }

  /** Returns if the number of superclasses of the given class in the given set of classes. */
  private int superClassCount(Clazz subClass, Set classes) {
    int count = 0;

    Iterator iterator = classes.iterator();

    while (iterator.hasNext()) {
      Clazz clazz = (Clazz) iterator.next();
      if (subClass.extendsOrImplements(clazz)) {
        count++;
      }
    }

    return count;
  }

  @Override
  public DoubleValue doubleArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    if (PartialEvaluator.ENABLE_NEW_EXCEPTIONS && !isInternalArrayType(type)) {
      throw new ValueTypeException("array reference", this);
    }
    return super.doubleArrayLoad(indexValue, valueFactory);
  }

  @Override
  public IntegerValue integerArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    if (PartialEvaluator.ENABLE_NEW_EXCEPTIONS && !isInternalArrayType(type)) {
      throw new ValueTypeException("array reference", this);
    }
    return super.integerArrayLoad(indexValue, valueFactory);
  }

  @Override
  public LongValue longArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    if (PartialEvaluator.ENABLE_NEW_EXCEPTIONS && !isInternalArrayType(type)) {
      throw new ValueTypeException("array reference", this);
    }
    return super.longArrayLoad(indexValue, valueFactory);
  }

  @Override
  public FloatValue floatArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    if (PartialEvaluator.ENABLE_NEW_EXCEPTIONS && !isInternalArrayType(type)) {
      throw new ValueTypeException("array reference", this);
    }
    return super.floatArrayLoad(indexValue, valueFactory);
  }

  @Override
  public void arrayStore(IntegerValue indexValue, Value value) {
    if (PartialEvaluator.ENABLE_NEW_EXCEPTIONS) {
      if (!isInternalArrayType(type)) {
        throw new ValueTypeException("array reference", this);
      }
      // When the array is over a primitive type, there is no class hierarchy,
      // hence, the type of the array and the value you want to store in it should be the same
      if (ClassUtil.isInternalPrimitiveType(ClassUtil.internalTypeFromArrayType(this.type))
          && !Objects.equals(
              value.internalType(), ClassUtil.internalTypeFromArrayType(this.type))) {
        throw new ArrayStoreTypeException(this, value);
      }
    }
  }

  public int equal(TypedReferenceValue other) {
    return this.type == null
        ? other.type == null ? ALWAYS : other.mayBeNull ? MAYBE : NEVER
        : other.type == null
            ? this.mayBeNull ? MAYBE : NEVER
            : this.mayBeExtension || other.mayBeExtension || this.type.equals(other.type)
                ? MAYBE
                : NEVER;
  }

  //    // Implementations of binary ReferenceValue methods with
  //    // IdentifiedReferenceValue arguments.
  //
  //    public ReferenceValue generalize(IdentifiedReferenceValue other)
  //    {
  //        return generalize((TypedReferenceValue)other);
  //    }
  //
  //
  //    public int equal(IdentifiedReferenceValue other)
  //    {
  //        return equal((TypedReferenceValue)other);
  //    }
  //
  //
  //    // Implementations of binary ReferenceValue methods with
  //    // ArrayReferenceValue arguments.
  //
  //    public ReferenceValue generalize(ArrayReferenceValue other)
  //    {
  //        return generalize((TypedReferenceValue)other);
  //    }
  //
  //
  //    public int equal(ArrayReferenceValue other)
  //    {
  //        return equal((TypedReferenceValue)other);
  //    }
  //
  //
  //    // Implementations of binary ReferenceValue methods with
  //    // IdentifiedArrayReferenceValue arguments.
  //
  //    public ReferenceValue generalize(IdentifiedArrayReferenceValue other)
  //    {
  //        return generalize((ArrayReferenceValue)other);
  //    }
  //
  //
  //    public int equal(IdentifiedArrayReferenceValue other)
  //    {
  //        return equal((ArrayReferenceValue)other);
  //    }
  //
  //
  //    // Implementations of binary ReferenceValue methods with
  //    // DetailedArrayReferenceValue arguments.
  //
  //    public ReferenceValue generalize(DetailedArrayReferenceValue other)
  //    {
  //        return generalize((IdentifiedArrayReferenceValue)other);
  //    }
  //
  //
  //    public int equal(DetailedArrayReferenceValue other)
  //    {
  //        return equal((IdentifiedArrayReferenceValue)other);
  //    }
  //
  //
  //    // Implementations of binary ReferenceValue methods with
  //    // TracedReferenceValue arguments.
  //
  //    public ReferenceValue generalize(TracedReferenceValue other)
  //    {
  //        return other.generalize(this);
  //    }
  //
  //
  //    public int equal(TracedReferenceValue other)
  //    {
  //        return other.equal(this);
  //    }

  // Implementations for Value.

  public boolean isParticular() {
    return type == null;
  }

  public final String internalType() {
    return type == null ? ClassConstants.TYPE_JAVA_LANG_OBJECT : type;
  }

  // Implementations for Object.

  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (!super.equals(object)) {
      return false;
    }

    TypedReferenceValue other = (TypedReferenceValue) object;
    return this.type == null
        ? other.type == null
        : (this.mayBeExtension == other.mayBeExtension
            && this.mayBeNull == other.mayBeNull
            && this.type.equals(other.type));
  }

  public int hashCode() {
    return this.getClass().hashCode()
        ^ (type == null ? 0 : type.hashCode() ^ (mayBeExtension ? 0 : 1) ^ (mayBeNull ? 0 : 2));
  }

  public String toString() {
    return type == null
        ? "n"
        : type
            + (referencedClass == null ? "?" : "")
            + (mayBeExtension ? "" : "=")
            + (mayBeNull ? "" : "!");
  }
}
