# The following tests a very large method that is bigger than
# allowed in the JVM specification (code must be < 64k bytes).
# The dalvik specification does not have this restriction so
# DexGuard should be able to accept a large method and output
# a large method.
#
# REQUIRES: DALVIKVM
#
# RUN: awk '/insertcodehere/{ if (count++ == 1) { for(i = 1; i <= 15000; i++) print "invoke-virtual {v1, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V" } } {print}' %s > %t/temp.smali
# RUN: %smali a %t/temp.smali -o %t/classes.dex
# RUN: %dexguard -injars %t/classes.dex \
# RUN:           -outjars %t/%basename_s.apk \
# RUN:           -ignorewarnings \
# RUN:           -verbose \
# RUN:           -dalvik \
# RUN:           -android \
# RUN:           -dontobfuscate \
# RUN:           -dontoptimize \
# RUN:           -dontshrink \
# RUN:           -keep class %basename_s | FileCheck %s -check-prefix DG

# RUN: %baksmali d %t/%basename_s.apk -o %t/smali
# RUN: FileCheck %s -input-file %t/smali/%basename_s.smali -check-prefix PROCESSED
# RUN: %dalvikvm -cp %t/%basename_s.apk %basename_s | FileCheck %s -check-prefix OUTPUT
#
# OUTPUT-COUNT-15000: The answer is 42
#
# DG-NOT: Method code too large!

.class public final LTestLargeMethod;
.super Ljava/lang/Object;

.method public static final main([Ljava/lang/String;)V
   .registers 3
   # PROCESSED-DAG: const-string [[v2:(v|p)[0-9]+]], "The answer is 42"
   const-string v2, "The answer is 42"
   # PROCESSED-DAG: sget-object [[v1:(v|p)[0-9]+]], Ljava/lang/System;->out:Ljava/io/PrintStream;   
   sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;
   # PROCESSED-COUNT-15000: invoke-virtual {[[v1]], [[v2]]}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
   # insertcodehere
   return-void
.end method

