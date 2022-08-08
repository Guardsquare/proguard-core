# T5347: Tests the merging of integer and boolean types, when accessed via an array.
# See dex2jar:TypeTransformer#mergeTypeEx

.class public LTestTypeTransformerMergeIZArray;
.super Ljava/lang/Object;

.method public static foo()Z
    .registers 2

    const/4 v0, 0x2
    new-array v0, v0, [I

    fill-array-data v0, :array_e
    const/4 v1, 0x0
    aget v0, v0, v1

    return v0

    :array_e
    .array-data 4
        0x0
        0x1
    .end array-data
.end method



.method public static final main([Ljava/lang/String;)V
    .registers 4

    invoke-static {}, LTestTypeTransformerMergeIZArray;->foo()Z
    move-result v3
    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;
    new-instance v2, Ljava/lang/StringBuilder;
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
    const-string v3, "The answer is "
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    if-nez v3, :cond_14
    const/4 v3, 0x0
    goto :goto_16
    :cond_14
    const/16 v3, 0x2a
    :goto_16
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v3
    invoke-virtual {v1, v3}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    return-void
.end method
