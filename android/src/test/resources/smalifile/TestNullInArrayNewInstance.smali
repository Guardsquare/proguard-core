# T6883: Test that reproduces bug ClassCastException: java.lang.Integer cannot be cast to com.googlecode.d2j.DexType.
#
# RUN: %smali a -a 26 %s -o %t/classes.dex
# RUN: jar -cf %t/input.jar -C %t classes.dex

# RUN: %dexguard -injars %t/input.jar \
# RUN:           -libraryjars %android \
# RUN:           -ignorewarnings \
# RUN:           -dontwarn java.lang.invoke.MethodHandle \
# RUN:           -verbose \
# RUN:           -forceprocessing \
# RUN:           -dalvik \
# RUN:           -dontobfuscate \
# RUN:           -dontoptimize \
# RUN:           -dontshrink \
# RUN:           -dump | FileCheck %s --check-prefix BYTEC --check-prefix DEXGUARD
#
# DEXGUARD: Printing classes

.class public LTestArrayNewInstance;
.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 4

    const/4 v1, 0x0
    const/4 v2, 0x3
    invoke-static {v1, v2}, Ljava/lang/reflect/Array;->newInstance(Ljava/lang/Class;I)Ljava/lang/Object;
    move-result-object v0
    check-cast v0, [Ljava/lang/Object;
    return-void
.end method
