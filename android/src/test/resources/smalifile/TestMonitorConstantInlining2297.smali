# The following code was produced by the Kotlin compiler. When used in
# post-processing mode, dex2jar would inline the constants used by the monitor
# instructions. This would cause a VerifyError on dalvik machines as
# throwing instructions must be covered by a catch-all. See issue #2297.
#
# REQUIRES: DALVIKVM
#
# RUN: %smali a %s -o %t/classes.dex
# RUN: %dexguard -injars %t/classes.dex \
# RUN:           -outjars %t/%basename_s.apk \
# RUN:           -ignorewarnings \
# RUN:           -dalvik \
# RUN:           -android \
# RUN:           -dontobfuscate \
# RUN:           -dontoptimize \
# RUN:           -dontshrink \
# RUN:           -keep class %basename_s

# RUN: %baksmali d %t/%basename_s.apk -o %t/smali
# RUN: cat %t/smali/%basename_s.smali | FileCheck %s -check-prefix PROCESSED
# RUN: %dalvikvm -cp %t/%basename_s.apk %basename_s | FileCheck %s -check-prefix OUTPUT
#
# OUTPUT: The answer is 42

.class public final LTestMonitorConstantInlining2297;
.super Ljava/lang/Object;

.method public static final main([Ljava/lang/String;)V
    .registers 1
    invoke-static {}, LTestMonitorConstantInlining2297;->crashHere()V
    return-void
.end method

.method public static final crashHere()V
    .registers 4
    :try_start_1
    # The constant should be loaded once and stored in a register
    # PROCESSED: const-class [[REGISTER:v[0-9]+]], Ljava/lang/Object;
    const-class v0, Ljava/lang/Object;
    # PROCESSED: monitor-enter [[REGISTER]]
    monitor-enter v0
    :try_end_4
    .catch Ljava/lang/InterruptedException; {:try_start_1 .. :try_end_4} :catch_13
    const/4 v1, 0x0
    :try_start_5
    const-string v2, "The answer is 42"
    sget-object v3, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-virtual {v3, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V
    :try_end_e
    .catchall {:try_start_5 .. :try_end_e} :catchall_10
    :try_start_e
    # PROCESSED-NOT: const-class v{{[0-9]+}}, Ljava/lang/Object;
    # PROCESSED: monitor-exit [[REGISTER]]
    monitor-exit v0
    goto :goto_14
    :catchall_10
    move-exception v1
    # PROCESSED-NOT: const-class v{{[0-9]+}}, Ljava/lang/Object;
    # PROCESSED: monitor-exit [[REGISTER]]
    monitor-exit v0
    throw v1
    :try_end_13
    .catch Ljava/lang/InterruptedException; {:try_start_e .. :try_end_13} :catch_13
    :catch_13
    move-exception v0
    :goto_14
    return-void
.end method
