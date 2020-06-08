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
