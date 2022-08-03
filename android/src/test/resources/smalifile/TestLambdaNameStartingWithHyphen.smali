# R8 backported lambda classes can be prefixed with -$$.
# DexGuard should not modify these names if obfuscation is disabled.

# REQUIRES: ANDROID

# RUN: %smali a %s -o %t/classes.dex
# RUN: %dexguard -injars %t/classes.dex \
# RUN:           -outjars %t/%basename_s.apk \
# RUN:           -libraryjars %android \
# RUN:           -ignorewarnings \
# RUN:           -verbose \
# RUN:           -dalvik \
# RUN:           -android \
# RUN:           -dontobfuscate \
# RUN:           -dontoptimize \
# RUN:           -dontshrink \
# RUN:           -keep class %basename_s

# RUN: %dexdump %t/%basename_s.apk | FileCheck %s -check-prefix PROCESSED
#
# PROCESSED: -$$Lambda$RetrofitProvider$7UGyImjn5OERU8TG-W_Zn0fdFtY
# PROCESSED-NOT: _$$Lambda$RetrofitProvider$7UGyImjn5OERU8TG-W_Zn0fdFtY

.class public final L-$$Lambda$RetrofitProvider$7UGyImjn5OERU8TG-W_Zn0fdFtY;
.super Ljava/lang/Object;
