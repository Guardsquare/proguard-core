# Kotlin metadata

The library makes it easy to read, write and modify the Kotlin metadata that is
attached to Java classes. The following example prints all the names of Kotlin
functions in the metadata attached to the Java class `Foo`:

```java
programClassPool.classesAccept(
   new ClassNameFilter("Foo",
   new ReferencedKotlinMetadataVisitor(
   new AllFunctionsVisitor(
       (clazz, container, function) -> System.out.println(function.name)))));
```

## Initializing the Kotlin metadata model

There are two ways to initialize the Kotlin metadata model:

* While reading classes with `ClassReader` by setting `includeKotlinMetadata` to `true`.
* After class reading using `KotlinMetadataInitializer`.

### `KotlinMetadataInitializer`

The `KotlinMetadataInitializer` can be used as a `ClassVisitor` which will read the Kotlin metadata
from the `kotlin.Metadata` annotation on program classes (`ProgramClass`) and 
automatically initialize the metadata model on visited classes:

```java
BiConsumer<Clazz, String> errorHandler = (clazz, message) -> System.err.println(message);
programClassPool.classesAccept(new KotlinMetadataInitializer(errorHandler));
```

Alternatively, the `initialize(Clazz, int, int[], String[], String[], int, String, String)` can be called
directly with a `Clazz` and the components making up the `kotlin.Metadata` annotation:

```java
BiConsumer<Clazz, String> errorHandler
        = (clazz, message) -> System.err.println(message);
KotlinMetadataInitializer initializer
        = new KotlinMetadataInitializer(errorHandler);
programClassPool.classesAccept(
    clazz -> {
        // get the values for k, mv, d1, d2, xi, xs, pn to provide to the initializer
        initializer.initialize(clazz, k, mv, d1, d2, xi, xs, pn);
    }
);
```

This is useful for initializing the model on library classes (`LibraryClass`), since the library class model doesn't contain 
all the necessary information to automatically extract these parameters.

### Initializing references

Once the model itself is initialized by `KotlinMetadataInitializer`, like the `Clazz` model, references
must be initialized. This is done by the `ClassReferenceInitializer`:

```java
// First build the model
BiConsumer<Clazz, String> errorHandler 
        = (clazz, message) -> System.err.println(message);
programClassPool.classesAccept(new KotlinMetadataInitializer(errorHandler));
// Then initialize the references
programClassPool.classesAccept(
    new ClassReferenceInitializer(programClassPool, libraryClassPool)
);
```
