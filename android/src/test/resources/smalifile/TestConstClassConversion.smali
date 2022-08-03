# T4942: Tests that const-class instructions with are correctly
#        converted to the equivalent in Java bytecode that loads the correct class type.
#
# RUN: echo 'public class I {}' > %t/I.java
# RUN: %javac %t/I.java
# RUN: %smali a %s -o %t/classes.dex
#
# RUN: %dexguard -injars %t/classes.dex -injars %t/I.class \
# RUN:           -outjars %t/%basename_s.apk \
# RUN:           -libraryjars %android \
# RUN:           -ignorewarnings \
# RUN:           -verbose \
# RUN:           -forceprocessing \
# RUN:           -dalvik \
# RUN:           -android \
# RUN:           -dontobfuscate \
# RUN:           -dontoptimize \
# RUN:           -dontshrink \
# RUN:           -keep class %basename_s { *\; } \
# RUN:           -dump | FileCheck %s --check-prefix BYTEC

# RUN: %dalvikvm -cp %t/%basename_s.apk %basename_s | FileCheck %s -check-prefix OUTPUT

.class public final LTestConstClassConversion;
.super Ljava/lang/Object;

.method public static final main([Ljava/lang/String;)V
    .registers 1

    invoke-static {}, LTestConstClassConversion;->testPrimitiveArrayClasses()V
    invoke-static {}, LTestConstClassConversion;->testPrimitiveClasses()V
    invoke-static {}, LTestConstClassConversion;->testUsualClasses()V

   return-void
.end method

# Primitive array classes are usual class constants
.method public static final testPrimitiveArrayClasses()V
   .registers 9
    # BYTEC: Class([I)
    const-class v1, [I
    # BYTEC: Class([J)
    const-class v2, [J
    # BYTEC: Class([Z)
    const-class v3, [Z
    # BYTEC: Class([F)
    const-class v4, [F
    # BYTEC: Class([D)
    const-class v5, [D
    # BYTEC: Class([B)
    const-class v6, [B
    # BYTEC: Class([S)
    const-class v7, [S
    # BYTEC: Class([C)
    const-class v8, [C

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    # OUTPUT: class [I
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: class [J
    invoke-virtual {v0, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: class [Z
    invoke-virtual {v0, v3}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: class [F
    invoke-virtual {v0, v4}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: class [D
    invoke-virtual {v0, v5}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: class [B
    invoke-virtual {v0, v6}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: class [S
    invoke-virtual {v0, v7}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: class [C
    invoke-virtual {v0, v8}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

   return-void
.end method

.method public static final testPrimitiveClasses()V
   .registers 9
    # BYTEC: Fieldref(java/lang/Integer.TYPE Ljava/lang/Class;)
    const-class v1, I
    # BYTEC: Fieldref(java/lang/Long.TYPE Ljava/lang/Class;)
    const-class v2, J
    # BYTEC: Fieldref(java/lang/Boolean.TYPE Ljava/lang/Class;)
    const-class v3, Z
    # BYTEC: Fieldref(java/lang/Float.TYPE Ljava/lang/Class;)
    const-class v4, F
    # BYTEC: Fieldref(java/lang/Double.TYPE Ljava/lang/Class;)
    const-class v5, D
    # BYTEC: Fieldref(java/lang/Byte.TYPE Ljava/lang/Class;)
    const-class v6, B
    # BYTEC: Fieldref(java/lang/Short.TYPE Ljava/lang/Class;)
    const-class v7, S
    # BYTEC: Fieldref(java/lang/Character.TYPE Ljava/lang/Class;)
    const-class v8, C

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    # OUTPUT: int
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: long
    invoke-virtual {v0, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: boolean
    invoke-virtual {v0, v3}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: float
    invoke-virtual {v0, v4}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: double
    invoke-virtual {v0, v5}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: byte
    invoke-virtual {v0, v6}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: short
    invoke-virtual {v0, v7}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: char
    invoke-virtual {v0, v8}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

   return-void
.end method

# Usual classes should not be matched as primitive types
.method public static final testUsualClasses()V
   .registers 3

    # BYTEC: Class(TestConstClassConversion)
    const-class v1, LTestConstClassConversion;
    # A class named I should not be converted to an Integer type class
    # BYTEC: Class(I)
    const-class v2, LI;

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    # OUTPUT: TestConstClassConversion
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    # OUTPUT: I
    invoke-virtual {v0, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

   return-void
.end method

