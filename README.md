<p align="center">
  <a href="https://www.guardsquare.com/en/products/proguard">
    <img
      src="https://mma.prnewswire.com/media/750323/ProGuard_Logo.jpg?p=publish"
      alt="ProGuard Core" width="400">
  </a>
</p>

<h4 align="center">A library to parse, modify and analyze Java class files.</h4>

<!-- Badges -->
<p align="center">
  <!-- CI -->
  <a href="https://github.com/Guardsquare/proguard-core/actions?query=workflow%3A%22Continuous+Integration%22">
    <img src="https://github.com/Guardsquare/proguard-core/workflows/Continuous%20Deployment/badge.svg?branch=github-workflow">
  </a>

  <!-- Github version -->
  <a href="./releases">
    <img src="https://img.shields.io/github/v/release/guardsquare/proguard-core">
  </a>

  <!-- Maven -->
  <a href="https://search.maven.org/search?q=g:net.sf.proguard">
    <img src="https://img.shields.io/maven-central/v/net.sf.proguard/proguard-parent">
  </a>

  <!-- License -->
  <a href="./LICENSE">
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

ProGuard Core is a free library to read, analyze, modify, and write Java class
files. It is the core of the well-known shrinker, optimizer, and obfuscator
[ProGuard](https://www.guardsquare.com/proguard) and of the [ProGuard
Assembler and Disassembler](https://github.com/guardsquare/proguard-assembler).

Typical applications:

- Read an write class files, including any Kotlin metadata.
- Search for instruction patterns.
- Create byte code instrumentation tools.
- Analyze code with abstract evaluation.
- Build static code analysis tools.
- Optimize and obfuscate, like ProGuard itself.
- ... and so much more!

You can find the complete documentation in [here](docs/md).

## 🚀 Quick Start

**ProGuard Core** requires Java 1.8 or higher and can be obtained as a pre-built
artefact from either

- [JCenter](https://bintray.com/guardsquare/proguard) or
- [Maven Central](https://search.maven.org/search?q=g:net.sf.proguard).

Just add it to your project dependencies, e.g. Gradle:

```gradle
dependencies {
    compile project(':net.sf.proguard:core:7.0.0')
}
```
or Maven:

```xml
<dependency>
    <groupId>net.sf.proguard</groupId>
    <artifactId>proguard-core</artifactId>
    <version>7.0.0</version>
</dependency>
```

### Building Locally
You can of course also clone and build this repository manually. Using Gradle,
just execute the assemble task in the project root:

```shell
gradle clean assemble
```

## ✨ Features

The repository contains some sample code in the [examples]() directory.
Together with the complete set of manual pages they extensively cover the usage
of the library and its features in detail:

<h3 align="center">
👉👉👉<a href="">&nbsp;&nbsp;&nbsp; Manual Pages &nbsp;&nbsp;&nbsp;</a>👈 👈 👈 
</h3>

Below we'll highlight just a few them in order to better showcase the
capabilities.

- [Creating classes programmatically](#creating-classes-programmatically-manual)
- [Replacing instruction sequences](#replacing-instruction-sequences-manual)
- [Kotlin metadata](#kotlin-metadata-manual)
- [Code analysis with abstract evaluation](#abstract-evaluation-manual)

### Creating classes programmatically [(manual)](blaat)

Using the fluent style API, it becomes very easy to programmatically create
classes from scratch. The data structures directly correspond to the bytecode
specifications and ProGuard Core can even preverify the code for you.

You can create classes, with fields and methods, with sequences of
instructions. The following concise code creates the iconic `HelloWorld` class:

```java
ProgramClass programClass =
    new ClassBuilder(
        VersionConstants.CLASS_VERSION_1_8,
        AccessConstants.PUBLIC,
        "HelloWorld",
        ClassConstants.NAME_JAVA_LANG_OBJECT)
        .addMethod(
            AccessConstants.PUBLIC |
            AccessConstants.STATIC,
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

### Replacing instruction sequences ([manual](blaat))

ProGuard Core has an excellent instruction pattern matching engine able to
replace specific bytecode instruction sequences.

For example, the snippet below defines an instruction sequence, including a
wildcard, and then applies the replacements to all code attributes of all
methods of all classes in a class pool.

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
	new AllMethodVisitor(new AllAttributeVisitor(
	new PeepholeEditor(branchTargetFinder, codeAttributeEditor,
	new InstructionSequenceReplacer(constants, replacements, branchTargetFinder,
                                    codeAttributeEditor)))));
```

### Kotlin Metadata [(manual)](blaat)

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

### Abstract Evaluation [(manual)](blaat)

ProGuard Core provides a number of ways to analyze code, one of the most
powerful techniques is abstract evaluation (closely related to symbolic
execution). It **executes the code**, but instead of computing with concrete values
it can compute **with abstract values**.

Consider the `getAnswer` method:

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
| [0] iconst_2     | [0:2]             | P0:i0 | P1:i1 |
| [1] iload_0 v0   | [0:2][1:i0]       | P0:i0 | P1:i1 |
| [2] imul         | [2:(2*i0)]        | P0:i0 | P1:i1 |
| [3] iload_1 v1   | [2:(2*i0)] [3:i1] | P0:i0 | P1:i1 |
| [4] iadd         | [4:((2*i0)+i1)]   | P0:i0 | P1:i1 |
| [5] ireturn      |                   | P0:i0 | P1:i1 |

At every point ProGuard Core provides access to the symbolic expressions, or
if possible and requested, the concrete results.

## 🚧 Projects

Some of the project using ProGuard Core:

- [ProGuard]()
- [Kotlin Metadata Printer]()
- [ProGuard Assembler/Disassembler]()

If you created your own, make sure to reach out through `github _ at _
guardsquare.com` and we'll add it to the list!

## 🤝 Contributing

Contributions, issues and feature requests are welcome.  
Feel free to check [issues](./issues) page if you want to contribute and check
the [contributing guide](./blob/master/CONTRIBUTING.md).

## 📝 License

Copyright (c) 2002-2020 [Guardsquare NV](https://www.guardsquare.com/)  
ProGuard Core is [Apache 2](./blob/master/LICENSE) licensed.
