# T4942: Tests that const-class instructions with are correctly
#        converted to the equivalent in Java bytecode that loads the correct class type.

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
    const-class v1, [I
    const-class v2, [J
    const-class v3, [Z
    const-class v4, [F
    const-class v5, [D
    const-class v6, [B
    const-class v7, [S
    const-class v8, [C

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v3}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v4}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v5}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v6}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v7}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v8}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

   return-void
.end method

.method public static final testPrimitiveClasses()V
   .registers 9
    const-class v1, I
    const-class v2, J
    const-class v3, Z
    const-class v4, F
    const-class v5, D
    const-class v6, B
    const-class v7, S
    const-class v8, C

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v3}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v4}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v5}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v6}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v7}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v8}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

   return-void
.end method

# Usual classes should not be matched as primitive types
.method public static final testUsualClasses()V
   .registers 3

    const-class v1, LTestConstClassConversion;
    # A class named I should not be converted to an Integer type class
    const-class v2, LI;

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    invoke-virtual {v0, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

   return-void
.end method

