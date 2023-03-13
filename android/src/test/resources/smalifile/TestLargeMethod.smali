# The following tests a very large method that is bigger than
# allowed in the JVM specification (code must be < 64k bytes).
# The dalvik specification does not have this restriction so
# Proguard-core should be able to accept a large method and output
# a large method.

.class public final LTestLargeMethod;
.super Ljava/lang/Object;

.method public static final main([Ljava/lang/String;)V
   .registers 3
   const-string v2, "The answer is 42"
   sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;
   return-void
.end method

