## Version 7.1.1

### API improvements

- Add `KotlinTypeParameterFilter` to allow filtering easily when visiting type parameters.

### Bug fixes

 - Fix `AllTypeParameterVisitor` so that it visits type parameters defined in any kind of declaration container.
 - Fix `AllTypeParameterVisitor` so that it visits type parameters of type aliases.

## Version 7.1 (June 2021)

### Java support

ProGuardCORE 7.1 now supports Java versions 14, 15 and 16:

 - Add support for reading & writing Java 14, 15 and 16 class files. (`PGC-0015`, `PGD-0064`)
 - Add support for Java 14 sealed classes (permitted subclasses attributes). (`PGD-0064`)
 - Add support for record attributes (previewed in Java 15/16, targeted for Java 17). (`PGD-0064`)

### Improved code analysis

- The partial evaluator can now be used to reconstruct the specific values of `String`, `StringBuilder` and `StringBuffer` type objects. 
  See [analyzing code manual page](analyzing.md#particularreference) for more information.
- The partial evaluator will now throw an `IncompleteClassHierarchyException` instead of
  `IllegalArgumentException` when an incomplete hierarchy is encountered.
- The partial evaluator will now throw an `ExcessiveComplexityException` if an instruction is visited more than `stopAnalysisAfterNEvaluations` times.
- Potentially throwing `ldc` instructions are now taken into account during partial evaluation,
  improving the accuracy of code analysis. (`DGD-3036`)
- Add support for multiple possible types during partial evaluation.

### Performance improvements

 - Improve efficiency of building classes, methods and constant pools (`PGD-5`).

### API improvements

- Add `ClassRenamer` to allow renaming classes and members easily. (`T5302`)

### Bug fixes

 - Add missing method reference in injected static initializer instructions. (`DGD-3231`)
 - Add missing dimensions argument to `CompactCodeAttributeComposer.multianewarray`.
 - Fix potential `StackOverflowException` when comparing multi-typed reference values.
 - Fix handling of Kotlin nested class names which contain `$`. (`DGD-3317`)
 - Mark `Module`, `ModuleMainClass` and `ModulePackages` attributes as required. (`PDG-127`)
 - Fix potential `ClassCastException` in `ConstructorMethodFilter`. (`PGC-0016`)
 - Fix potential `NullPointerException` for module classes in ClassPrinter.
 - Fix storage and alignment of uncompressed zip entries. (`DGD-2390`)
 - Fix processing of constant boolean arrays. (`DGD-2338`)
 - Fix adding branch instructions with labels in `CompactCodeAttributeComposer`.
 - Fix handling of array dereferencing in `MultiTypedReferenceValue`.
 - Fix `AllKotlinAnnotationVisitor` so that it visits type alias annotations defined in any kind of declaration container.
 - Move initialization of Kotlin declaration container's `ownerClassName` field from `ClassReferenceInitializer` to `KotlinMetadataInitializer`. (`T5348`)

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
