## Version 7.1

| Version| Issue    | Module   | Explanation
|--------|----------|----------|----------------------------------
| 7.1.x  | DGD-3231 | CORE     | Add missing method reference in injected static initializer instructions.
| 7.1.x  | PGC-0016 | CORE     | Fixed potential `ClassCastException` in `ConstructorMethodFilter`.
| 7.1.x  | PGC-0015 | CORE     | Allowing Java 16.
| 7.1.x  | PGD-0064 | CORE     | Added support for Java 14 and 15.
| 7.1.x  | PGD-0064 | CORE     | Added support for sealed classes (permitted subclasses attributes).
| 7.1.x  | PGD-0064 | CORE     | Added support for record attributes.
| 7.1.x  |          | CORE     | Fixed potential NullPointerException for module classes in ClassPrinter.
| 7.1.x  | PGD-5    | CORE     | Improved efficiency of building classes, methods and constant pools.
| 7.1.x  | DGD-2390 | CORE     | Fixed storage and alignment of uncompressed zip entries.
| 7.1.x  | DGD-2338 | CORE     | Fixed processing of constant boolean arrays.
| 7.1.x  |          | CORE     | Fixed adding branch instructions with labels in CompactCodeAttributeComposer.
| 7.1.x  |          | CORE     | Added missing dimensions argument to CompactCodeAttributeComposer.multianewarray.

## Version 7.0 (Jan 2020)

| Version| Issue    | Module   | Explanation
|--------|----------|----------|----------------------------------
| 7.0.1  | DGD-2382 | CORE     | Fixed processing of Kotlin 1.4 metadata annotations.
| 7.0.1  | DGD-2390 | CORE     | Fixed storage and alignment of uncompressed zip entries.
| 7.0.1  | DGD-2338 | CORE     | Fixed processing of constant boolean arrays.
| 7.0.1  |          | CORE     | Fixed adding branch instructions with labels in CompactCodeAttributeComposer.
| 7.0.0  |          | CORE     | Initial release of Kotlin support.
| 7.0.0  | PGD-32   | CORE     | Allowing Java 14 class files.
| 7.0.0  | DGD-1780 | CORE     | Removed dependency on internal sun.security API.
| 7.0.0  | DGD-1800 | CORE     | Fixed obfuscation of functional interfaces with abstract Object methods.
| 7.0.0  |          | CORE     | Initial release, extracted from ProGuard.
