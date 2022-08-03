# Tests that Dex2Jar's NewTransformer correctly transforms
# object creation and instantation instructions.
#
# T5005: NewTransformer sometimes has issues.
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

.class public LTestNewTransformer;
.super Ljava/lang/Object;

.method foo()LTestNewTransformer;
    .registers 2
    # BYTEC: new #{{[0-9]+}} = Class(TestNewTransformer)
    new-instance v1, LTestNewTransformer;
    # BYTEC: dup
    # BYTEC: invokespecial #{{[0-9]+}} = Methodref(TestNewTransformer.<init>()V)
    invoke-direct {v1}, LTestNewTransformer;-><init>()V
    # BYTEC: areturn
    return-object v1
.end method
