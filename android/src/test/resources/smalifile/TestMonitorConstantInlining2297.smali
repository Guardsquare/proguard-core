# The following code was produced by the Kotlin compiler. When used in
# post-processing mode, dex2jar would inline the constants used by the monitor
# instructions. This would cause a VerifyError on dalvik machines as
# throwing instructions must be covered by a catch-all. See issue #2297.

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
    const-class v0, Ljava/lang/Object;
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
    monitor-exit v0
    goto :goto_14
    :catchall_10
    move-exception v1
    monitor-exit v0
    throw v1
    :try_end_13
    .catch Ljava/lang/InterruptedException; {:try_start_e .. :try_end_13} :catch_13
    :catch_13
    move-exception v0
    :goto_14
    return-void
.end method
