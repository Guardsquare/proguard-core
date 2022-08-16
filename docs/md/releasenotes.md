## Version 9.0.4

## Version 9.0.3

### Java support

- Update maximum supported Java class version to 63.65535 (Java 19 ea). (`PGD-247`)

### Improved

- Add utility to produce dot-graphs for control flow automatons (`CfaUtil.toDot(cfa)`, example `VisualizeCfa`).
- Add support for heap slicing on call sites and heap reconstruction on return sites in dataflow analysis heap tree model.
- Add support for differential map representation to be used in the data flow analysis.
 
#### API Improvements

- Split `ProgramLocationDependentTransferRelation` into forward and backward variants.
- Add `ClassPoolClassLoader` utility for loading ProGuardCORE classes.
- Add builders for CPA runs.


## Version 9.0.2

### Improved
- `JvmMemoryLocationTransferRelation` does not rely anymore on a pre-computed abstract reachability graph.

### Improvements to Kotlin metadata initialization

Several improvements to Kotlin metadata initialization now allow building the Kotlin metadata model for library classes (`LibraryClass`):

- `KotlinMetadataInitializer` provides a new public method `initialize(Clazz, int, int[], String[], String[], int, String, String)` to initialize
  the Kotlin metadata model for a given class.
- `ClassReader` can now build the Kotlin metadata model by setting `includeKotlinMetadata` to `true`.
- `LibraryClassReader` can now read the Kotlin metadata annotation and provide the components to a consumer.
- An `UnsupportedKotlinMetadata` type is now assigned by the `KotlinMetadataInitializer` if the Kotlin metadata model
  could not be initialized correctly. 

## Version 9.0.1

### Improved

- `ExecutingInvocationUnit` now loads values from static final fields.
- Initialize Kotlin lambda method references when the JVM method name is `<anonymous>`.
- Add the possibility of limiting the number of `CodeAttributes` contributing into CFA.
- Add the possibility of limiting the number of `CodeAttributes` considered by the `CallResolver`.

### Bug fixes

- Fix wrong handling of array types in `ExecutingIvocationUnit` and `ParticularReferenceValue`.
- `ParticularReferenceValue` sanity checks now take inheritance into consideration, improving call analysis.
- Prevent missing semicolon leading to an infinite loop in `ClassUtil#internalMethodParameterCount`.
- Make category 2 CPA taint sources affect only the most significant byte abstract state.
- Fix inconsistent usage of type names in the context of the `PartialEvaluator` that could result in
  trying to create an internal type string from a string that was already an internal type.
- Fix initialization of Kotlin callable references when using `-Xno-optimized-callable-references` compiler option.
- Fix `createCache()` delegation in `ArgBamCpaRun`
  
### Upgrade considerations
####TYPE NAME CONVENTION

PGC has different representation for type string variables: 

- External class name: `com.guardsquare.SomeClass`
- Internal class name: `com/guardsquare/SomeClass`
- Internal type (or just `type`): `Lcom/guardsquare/SomeClass;` (for arrays e.g. `[I`, `[Ljava/lang/Object;`)
- Internal class type: `com/guardsquare/SomeClass` (for arrays this is their internal type e.g. `[I`, `[Ljava/lang/Object;`)

See `proguard.classfile.util.ClassUtil` for useful methods to convert between the different representations.

Since internal class name and type were used ambiguously, from version 9.0.1 the internal type is used 
consistently whenever we have a variable named `type`.

Since this was not the case, this update might cause some `type` variables switching from the internal class name
notation to the internal type notation, potentially breaking some logic if types are used by an external
application using proguard-core.

## Version 9.0 (April 2022)

### Configurable program analysis (CPA)

_CPA is a formalism for data flow analysis allowing seamless composition of various analyses
and model checking techniques. Thus, it adds a framework for systematic development and extension
of static analyses in a uniform structured way._

Taint analysis is the first ProGuardCORE CPA. Its goal is to detect data flow between source and sink
method calls, which is useful for detecting bugs and security flaws.

The [Taint Analysis manual page](taintcpa.md) provides more information.

- Add configurable program analysis (CPA) for interprocedural data flow analysis development.
- Add taint analysis.

### Bug fixes

- Prevent linking a final method with a shadowing method in a subclass.
- Force `Call#getArgumentCount()` to be correct even if the actual argument values
  could not be calculated or have been cleared.
- Reset `ExecutingInvocationUnit` parameters array even when an exception happens.

## Version 8.0.7

### Java support

- Update maximum supported Java class version to 62.65535 (Java 18 ea).

### Improved

- Add support for Kotlin property synthetic delegate methods.
- Add ability to pass `KotlinMetadataVersion` to `KotlinMetadataWriter` / `KotlinModuleWriter`.

## Version 8.0.6

### Improved

- Add support for writing out zip64 archives.
- Improve speed for `ClassPool.contains` method.

## Version 8.0.5

### Improved

- Upgrade log4j2 dependency to v2.17.1 in response to CVE-2021-44832.
- Add support for reading and writing Kotlin 1.6 metadata.

### Bug fixes

- Fix `CallResolver` erroneously creating call edges to unimplemented interface methods.
- Make the `DominatorCalculator` skip methods with an empty `CodeAttribute`.
- Prevent updating Kotlin function names with mangled JVM method names in `ClassReferenceFixer`.
- Initialize Kotlin default implementation classes of annotation classes correctly in `ClassReferenceInitializer`.
- Correctly initialize Java Record component attributes in `ClassReferenceInitializer`.

### API changes

- `KotlinInterfaceToDefaultImplsClassVisitor` replaced by `KotlinClassToDefaultImplsClassVisitor`.
- Deprecate Kotlin class metadata flag `IS_INLINE` and replaced with `IS_VALUE`.
- Convert to/from Kotlin unsigned integers in Kotlin annotation unsigned type arguments.
- Initialize array dimension in Kotlin annotation `ClassValue` type arguments.
- Add support for Kotlin inline class underlying type to Kotlin metadata model.
- Add support to `MemberDescriptorReferencedClassVisitor` for visiting referenced Kotlin inline class parameters.

## Version 8.0.4

### Improved

- Upgrade log4j2 dependency to v2.17 in response to CVE-2021-45105.

### API Improvements

- Add `KotlinMetadataVersionFilter` to filter classes based on the version of the attached metadata.

## Version 8.0.3

### Improved

- Upgrade log4j2 dependency in response to CVE-2021-45046.

## Version 8.0.2

### Improved

- Upgrade log4j2 dependency in response to CVE-2021-44228.

### API Improvements

- Add call resolving and graph traversal features to enable interprocedural control flow analyses.

### Bug fixes

- Fix potential `StringIndexOutOfBoundsException` while trimming attribute value spaces in `SignedJarWriter`.
- Fix `referencedClass` of Values generated by the `ExecutingInvocationUnit`.
- Fix potential `StackOverflowError` when using an `AttributeVisitor` to visit runtime invisible type annotations.
- Fix potential `StringIndexOutOfBoundsException` in `KotlinCallableReferenceInitializer`.
- Fix potential `NullPointerException` in `KotlinInterClassPropertyReferenceInitializer`.
- Fix wrong offset for complementary branch instruction when widening branch instructions in `InstructionWriter`.
- Fix potential `ClassFormatError` due to adding multiple annotation attributes when processing Kotlin code.
- Fix potential `NullPointerException` due to missing classes in `ClassReferenceInitializer`.
- Prevent making package-private final methods that are shadowed protected.

## Version 8.0.1

### API Improvements

- Add `LibraryClassBuilder` and `LibraryClassEditor` classes to create and edit a `LibraryClass`.
- Add additional constructors to `LibraryClass`. 

### Bug fixes

- Fix potential `NullPointerException` when initializing Kotlin callable references.
- Prevent requiring `--enable-preview` on a JVM for Java 16 class files (write class file version `60.0` instead of `60.65535`).
- Fix potential `NullPointerException` when visiting referenced methods of Kotlin functions.

## Version 8.0.0

### Java support

- Update maximum supported Java class version to 61.0 (Java 17).

### Kotlin support

- Add support for processing Kotlin 1.5 metadata.
- Update `kotlinx` metadata dependency to version 0.2.

### API Improvements

- Add `WarningLogger` class to allow using a custom Log4j2 logger.
- Add Kotlin metadata model classes and visitors for Kotlin annotations.
- Add Kotlin metadata model enum for `KmVariance`.
- Add Kotlin metadata model enum for `KmVersionRequirement(Kind|Level)`.
- Add Kotlin metadata model enum for `KmEffect(Type|InvocationKind)`.
- Add Kotlin metadata flag `IS_FUN` for functional interfaces.
- Add Kotlin metadata flag `HAS_NON_STABLE_PARAMETER_NAMES` for Kotlin callables.
- Add error handler callback to `KotlinMetadataInitializer`.
- Add error handler callback to `KotlinMetadataWriter`.
- Add error handler callback to `KotlinModuleReader`.
- Add error handler callback to `KotlinModuleWriter`. Add Kotlin metadata flag `IS_SECONDARY` for constructors.
- Implement `ClassVisitor` in `KotlinMetadataInitializer` to allow easier initialization of Kotlin metadata.
- Implement `ClassVisitor` in `KotlinMetadataWriter` to allow easier writing of Kotlin metadata.

### API changes

- `KotlinTypeParameterVistor#visitClassParameter(Clazz, KotlinMetadata, KotlinTypeParameterMetadata)` now has the correct signature: `KotlinTypeParameterVistor#visitClassParameter(Clazz, KotlinClassKindMetadata, KotlinTypeParameterMetadata)`. 
- Rename `AllKotlinPropertiesVisitor` to `AllPropertyVisitor`.
- Rename `AllConstructorsVisitor` to `AllConstructorVisitor`.
- Rename `AllFunctionsVisitor` to `AllFunctionVisitor`.
- Remove `KotlinValueParameterVisitor.onNewFunctionStart()' method.
- Deprecate Kotlin metadata flag `IS_PRIMARY` for constructors.

## Version 7.1.1

### API improvements

- Add `KotlinTypeParameterFilter` to allow filtering easily when visiting type parameters.
- Add `KotlinValueParameterFilter` to allow filtering easily when visiting value parameters.

### Bug fixes

 - Fix `AllTypeParameterVisitor` so that it visits type parameters defined in any kind of declaration container.
 - Fix `AllTypeParameterVisitor` so that it visits type parameters of type aliases.
 - Fix potential `NullPointerException` when initializing a Kotlin default implementation class that does not contain an initialized `jvmSignature`.
 - Add missing `equals` method to `ParticularReferenceValue`.
 - Fix incorrect handling of `InterruptedException` in `ParallelAllClassVisitor`.
 - Fix potential `ZipOutput` alignment issue when writing large uncompressed zip entries.
 - Fix potential `ZipOutput` synchronization issue when writing uncompressed zip entries.
 - Fix potential `NullPointerException` when comparing strings with `FixedStringMatcher`.
 - Fix potential `NullPointerException` when comparing strings with `MatchedStringMatcher`.
 - Fix initialization of Kotlin callable references when using Kotlin >= 1.4.

## Version 7.1 (June 2021)

### Java support

ProGuardCORE 7.1 now supports Java versions 14, 15 and 16:

 - Add support for reading & writing Java 14, 15 and 16 class files.
 - Add support for Java 14 sealed classes (permitted subclasses attributes).
 - Add support for record attributes (previewed in Java 15/16, targeted for Java 17).

### Improved code analysis

- The partial evaluator can now be used to reconstruct the specific values of `String`, `StringBuilder` and `StringBuffer` type objects. 
  See [analyzing code manual page](analyzing.md#particularreference) for more information.
- The partial evaluator will now throw an `IncompleteClassHierarchyException` instead of
  `IllegalArgumentException` when an incomplete hierarchy is encountered.
- The partial evaluator will now throw an `ExcessiveComplexityException` if an instruction is visited more than `stopAnalysisAfterNEvaluations` times.
- Potentially throwing `ldc` instructions are now taken into account during partial evaluation,
  improving the accuracy of code analysis.
- Add support for multiple possible types during partial evaluation.

### Performance improvements

 - Improve efficiency of building classes, methods and constant pools.

### API improvements

- Add `ClassRenamer` to allow renaming classes and members easily.

### Bug fixes

 - Add missing method reference in injected static initializer instructions.
 - Add missing dimensions argument to `CompactCodeAttributeComposer.multianewarray`.
 - Fix potential `StackOverflowException` when comparing multi-typed reference values.
 - Fix handling of Kotlin nested class names which contain `$`.
 - Mark `Module`, `ModuleMainClass` and `ModulePackages` attributes as required.
 - Fix potential `ClassCastException` in `ConstructorMethodFilter`.
 - Fix potential `NullPointerException` for module classes in ClassPrinter.
 - Fix storage and alignment of uncompressed zip entries.
 - Fix processing of constant boolean arrays.
 - Fix adding branch instructions with labels in `CompactCodeAttributeComposer`.
 - Fix handling of array dereferencing in `MultiTypedReferenceValue`.
 - Fix `AllKotlinAnnotationVisitor` so that it visits type alias annotations defined in any kind of declaration container.
 - Move initialization of Kotlin declaration container's `ownerClassName` field from `ClassReferenceInitializer` to `KotlinMetadataInitializer`.

## Version 7.0 (Jan 2020)

| Version| Issue    | Module   | Explanation
|--------|----------|----------|----------------------------------
| 7.0.1  | DGD-2382 | CORE     | Fixed processing of Kotlin 1.4 metadata annotations.
| 7.0.1  | DGD-2390 | CORE     | Fixed storage and alignment of uncompressed zip entries.
| 7.0.1  | DGD-2338 | CORE     | Fixed processing of constant boolean arrays.
| 7.0.1  |          | CORE     | Fixed adding branch instructions with labels in CompactCodeAttributeComposer.
| 7.0.0  |          | CORE     | Initial release of Kotlin support.
| 7.0.0  | PGD-32   | CORE     | Added support for Java 14 class files.
| 7.0.0  | DGD-1780 | CORE     | Removed dependency on internal sun.security API.
| 7.0.0  | DGD-1800 | CORE     | Fixed obfuscation of functional interfaces with abstract Object methods.
| 7.0.0  |          | CORE     | Initial release, extracted from ProGuard.
