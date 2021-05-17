## Version 7.1

| Version| Issue    | Module   | Explanation
|--------|----------|----------|----------------------------------
| 7.1.0  |          | CORE     | The `PartialEvaluator` now throws `ExcessiveComplexityException` if an instruction is visited more than `stopAnalysisAfterNEvaluations` times.
| 7.1.0  |          | CORE     | The `PartialEvaluator` now throws `IncompleteClassHierarchyException` instead of `IllegalArgumentException` when an incomplete hierarchy is encountered.
| 7.1.0  |          | CORE     | Fixed potential `StackOverflowException` when comparing multi-typed reference values.
| 7.1.0  | DGD-3036 | CORE     | Improved code analysis by taking into account potentially throwing `ldc` instructions.
| 7.1.0  | DGD-3317 | CORE     | Fixed handling of Kotlin nested class names which contain `$`.
| 7.1.0  | PDG-127  | CORE     | Marked `Module`, `ModuleMainClass` and `ModulePackages` attributes as required.
| 7.1.0  |          | CORE     | Added support for partial evaluation with particular reference values.
| 7.1.0  | DGD-3231 | CORE     | Added missing method reference in injected static initializer instructions.
| 7.1.0  |          | CORE     | Added support for multiple possible types during partial evaluation.
| 7.1.0  | PGC-0016 | CORE     | Fixed potential `ClassCastException` in `ConstructorMethodFilter`.
| 7.1.0  | PGC-0015 | CORE     | Added support Java 16.
| 7.1.0  | PGD-0064 | CORE     | Added support for Java 14 and 15.
| 7.1.0  | PGD-0064 | CORE     | Added support for sealed classes (permitted subclasses attributes).
| 7.1.0  | PGD-0064 | CORE     | Added support for record attributes.
| 7.1.0  |          | CORE     | Fixed potential NullPointerException for module classes in ClassPrinter.
| 7.1.0  | PGD-5    | CORE     | Improved efficiency of building classes, methods and constant pools.
| 7.1.0  | DGD-2390 | CORE     | Fixed storage and alignment of uncompressed zip entries.
| 7.1.0  | DGD-2338 | CORE     | Fixed processing of constant boolean arrays.
| 7.1.0  |          | CORE     | Fixed adding branch instructions with labels in CompactCodeAttributeComposer.
| 7.1.0  |          | CORE     | Added missing dimensions argument to CompactCodeAttributeComposer.multianewarray.

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
