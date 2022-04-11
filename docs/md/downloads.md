The **ProGuardCORE** library is distributed under the terms of the Apache
License Version 2.0. Please consult the [license page](license.md) for more
details.

ProGuardCORE is written in Java, so it requires a Java Runtime Environment
(JRE 1.8 or higher).

You can download the library in various forms:

- [Pre-built artifacts](https://search.maven.org/search?q=g:com.guardsquare) at Maven Central

        :::groovy
        dependencies {
            compile project(':com.guardsquare:proguard-core:9.0.0')
        }

    or

        <dependency>
            <groupId>com.guardsquare</groupId>
            <artifactId>proguard-core</artifactId>
            <version>9.0.0</version>
        </dependency>

- A [Git repository of the source code](https://github.com/Guardsquare/proguard-core) at Github

        git clone https://github.com/Guardsquare/proguard-core.git

You can find major releases, minor releases with important bug fixes, and
beta releases with the latest new features and any less urgent bug fixes.

If you're still working with an older version of the library, check out the
[release notes](releasenotes.md), to see if you're missing something essential.
