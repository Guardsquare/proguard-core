<p align="center">
  <br />
  <br />
  <a href="https://www.guardsquare.com/proguard">
    <img
      src="https://www.guardsquare.com/hubfs/Logos/ProGuard_core_logo.png"
      alt="ProGuardCORE" width="400">
  </a>
</p>

<h4 align="center">A library to parse, modify and analyze Java class files.</h4>

<!-- Badges -->
<p align="center">
  <!-- CI -->
  <a href="https://github.com/Guardsquare/proguard-core/actions?query=workflow%3A%22Continuous+Integration%22">
    <img src="https://github.com/Guardsquare/proguard-core/workflows/Continuous%20Integration/badge.svg">
  </a>
  
  <!-- Github version -->
  <!--
  <a href="releases">
    <img src="https://img.shields.io/github/v/release/guardsquare/proguard-core">
  </a>
  -->

  <!-- Maven -->
  <a href="https://search.maven.org/search?q=g:com.guardsquare">
    <img src="https://img.shields.io/maven-central/v/com.guardsquare/proguard-core">
  </a>

  <!-- License -->
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/guardsquare/proguard-core">
  </a>

  <!-- Twitter -->
  <a href="https://twitter.com/Guardsquare">
    <img src="https://img.shields.io/twitter/follow/guardsquare?style=social">
  </a>

</p>

<br />
<p align="center">
  <a href="#-quick-start"><b>Quick Start</b></a> •
  <a href="#-features"><b>Features</b></a> •
  <a href="#-projects"><b>Projects</b></a> •
  <a href="#-contributing"><b>Contributing</b></a> •
  <a href="#-license"><b>License</b></a>
</p>
<br />

ProGuardCORE is a free library to read, analyze, modify, and write Java class
files. It is the core of the well-known shrinker, optimizer, and obfuscator
[ProGuard](https://www.guardsquare.com/proguard), the [ProGuard
Assembler and Disassembler](https://github.com/guardsquare/proguard-assembler),
and the [Kotlin Metadata
Printer](https://github.com/Guardsquare/kotlin-metadata-printer).

Typical applications:

- Read and write class files, including any Kotlin metadata.
- Search for instruction patterns.
- Create byte code instrumentation tools.
- Analyze code with abstract evaluation.
- Build static code analysis tools.
- Optimize and obfuscate, like ProGuard itself.
- ... and so much more!

ProGuard core comes with a short [manual](https://guardsquare.github.io/proguard-core/) and pretty nice
[API documentation](https://guardsquare.github.io/proguard-core/api/).

## ❓ Getting Help
If you have **usage or general questions** please ask them in the <a href="https://community.guardsquare.com/?utm_source=github&utm_medium=site-link&utm_campaign=github-community">**Guardsquare Community**.</a>  
Please use <a href="https://github.com/guardsquare/proguard-core/issues">**the issue tracker**</a> to report actual **bugs 🐛, crashes**, etc.
<br />
<br />
## 🚀 Quick Start

**ProGuardCORE** requires Java 1.8 or higher. You can download it as a pre-built
artifact from:

- [Maven Central](https://search.maven.org/search?q=g:com.guardsquare).

Just add it to your project dependencies, e.g. Gradle:

```gradle
dependencies {
    compile 'com.guardsquare:proguard-core:9.0.0'
}
```
or Maven:

```xml
<dependency>
    <groupId>com.guardsquare</groupId>
    <artifactId>proguard-core</artifactId>
    <version>9.0.0</version>
</dependency>
```

### Building Locally

You can of course also clone and build this repository manually. Using Gradle,
just execute the assemble task in the project root:

```shell
gradle clean assemble
```

## ✨ Features

The repository contains some sample code in the [examples](examples) directory.
Together with the complete set of manual pages they extensively cover the usage
of the library and its features in more detail:

<h3 align="center">
👉👉👉<a href="https://guardsquare.github.io/proguard-core/">&nbsp;&nbsp;&nbsp; Manual Pages &nbsp;&nbsp;&nbsp;</a>👈 👈 👈
</h3>

Below we'll highlight just a few of them in order to better showcase the
capabilities.

- [Creating classes programmatically](#creating-classes-programmatically-manual)
- [Replacing instruction sequences](#replacing-instruction-sequences-manual)
- [Kotlin metadata](#kotlin-metadata-manual)
- [Code analysis with abstract evaluation](#abstract-evaluation-manual)

### Creating classes programmatically ([manual](https://guardsquare.github.io/proguard-core/creating.html))

Using the fluent API, it becomes very easy to programmatically create
classes from scratch. The data structures directly correspond to the bytecode
specifications and ProGuardCORE can even preverify the code for you.

You can create classes, with fields, methods and sequences of
instructions. The following concise code creates the iconic `HelloWorld` class:

```java
ProgramClass programClass =
    new ClassBuilder(
        VersionConstants.CLASS_VERSION_1_8,
        AccessConstants.PUBLIC,
        "HelloWorld",
        ClassConstants.NAME_JAVA_LANG_OBJECT)
        .addMethod(
            AccessConstants.PUBLIC | AccessConstants.STATIC,
            "main",
            "([Ljava/lang/String;)V",
            50,
            code -> code
                .getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                .ldc("Hello, world!")
                .invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                .return_())
        .getProgramClass();
```

### Replacing instruction sequences ([manual](https://guardsquare.github.io/proguard-core/patternmatching.html))

ProGuardCORE has an excellent instruction pattern matching engine, which
can also replace matched bytecode instruction sequences.

For example, the snippet below defines an instruction sequence, including a
wildcard, and replaces it with an equivalent instruction sequence, in all code
attributes of all methods of all classes in a class pool.

```java
Instruction[][] replacements =
{
    ____.putstatic(X)
        .getstatic(X).__(),
    ____.dup()
        .putstatic(X).__()
};
...
programClassPool.classesAccept(
    new AllMethodVisitor(
    new AllAttributeVisitor(
    new PeepholeEditor(branchTargetFinder, codeAttributeEditor,
    new InstructionSequenceReplacer(constants, replacements, branchTargetFinder,
                                    codeAttributeEditor)))));
```

### Kotlin Metadata ([manual](https://guardsquare.github.io/proguard-core/kotlin.html))

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

### Abstract Evaluation ([manual](https://guardsquare.github.io/proguard-core/analyzing.html))

ProGuardCORE provides a number of ways to analyze code. One of the most
powerful techniques is abstract evaluation (closely related to symbolic
execution). It **executes the code**, but instead of computing with concrete values
it can compute with **abstract values**.

Consider the following `getAnswer` method:

```java
private static int getAnswer(int a, int b)
{
    return 2 * a + b;
}
```

The table represents the bytecode instructions at their
offsets, and the resulting evolving stacks and local variables.  Each value
slot has a numerical value and its origin (an instruction offset), presented as
`origin:value`.

| Instruction      | Stack             | v0    | v1    |
|------------------|-------------------|-------|-------|
| [0] iconst\_2    | [0:2]             | P0:i0 | P1:i1 |
| [1] iload\_0 v0  | [0:2][1:i0]       | P0:i0 | P1:i1 |
| [2] imul         | [2:(2*i0)]        | P0:i0 | P1:i1 |
| [3] iload\_1 v1  | [2:(2*i0)] [3:i1] | P0:i0 | P1:i1 |
| [4] iadd         | [4:((2*i0)+i1)]   | P0:i0 | P1:i1 |
| [5] ireturn      |                   | P0:i0 | P1:i1 |

At every point ProGuardCORE provides access to the symbolic expressions, or
if possible and requested, the concrete results.

## ⚙️ Projects

Some of the projects using ProGuardCORE:

- [ProGuard](https://github.com/Guardsquare/proguard)
- [Kotlin Metadata Printer](https://github.com/Guardsquare/kotlin-metadata-printer)
- [ProGuard Assembler/Disassembler](https://github.com/Guardsquare/proguard-assembler)
- [Log4Shell Detector](https://github.com/Guardsquare/log4shell-detector)
- [klox compiler](https://github.com/mrjameshamilton/klox)

If you've created your own, make sure to reach out through `github _ at _
guardsquare.com` and we'll add it to the list!

## 🤝 Contributing

Contributions, issues and feature requests are welcome.
Feel free to check the [issues](issues) page and the [contributing
guide](blob/master/CONTRIBUTING.md) if you would like to contribute.

## 📝 License

Copyright (c) 2002-2022 [Guardsquare NV](https://www.guardsquare.com/).
ProGuardCORE is released under the [Apache 2 license](LICENSE).
