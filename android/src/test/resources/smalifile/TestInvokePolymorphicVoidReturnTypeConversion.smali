# T3710: Tests that invoke-polymorphic instructions with void return types can be
#        converted correctly to Java bytecode.
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

.class public LTestInvokePolymorphicVoidReturnTypeConversion;
.super Ljava/lang/Object;

.method foo(Ljava/lang/invoke/MethodHandle;)V
    .registers 2
    const/16 v0, 0xa
    const/16 v1, 0x14

    # BYTEC: invokevirtual #{{[0-9]+}} = Methodref(java/lang/invoke/MethodHandle.invoke(II)V
    invoke-polymorphic {p1, v0, v1}, Ljava/lang/invoke/MethodHandle;->invoke([Ljava/lang/Object;)Ljava/lang/Object;, (II)V

    return-void
.end method
