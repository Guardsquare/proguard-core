# T6048: Fix merging of I+Z in ADD instructions.
#        Sample is a stripped down snippet based on real customer code.

.class public LTestTypeTransformerMergeIZArray2;
.super Ljava/lang/Object;

.method private static foo()Z
    .locals 6

    const/4 v0, 0x2
    new-array v0, v0, [Z
    fill-array-data v0, :array_e

    .line 126

    const/4 v2, 0x0
    const/4 v3, 0x0

    :startloop
    array-length v4, v0

    if-ge v2, v4, :endloop

    .line 127
    aget-boolean v5, v0, v2
    add-int/2addr v3, v5
    # The code generated here is like this:
    #     v3 = v3 (I) + v0[v2] (Z)
    # where v3 is integer (I) and v0[v2] is boolean used as an integer.
    # In Java bytecode, this should translate to
    #     iload(v3)
    #     aload(v0)
    #     iload(v2)
    #     baload
    #     iadd
    #     astore(v3)
    # Dex2Pro had an issue because it did not handle the case of I + Z during code generation
    # and the dex2jar TypeTransformer had not converted the Z to I in this case.

    .line 126
    add-int/lit8 v2, v2, 0x1

    goto :startloop

    .line 129

    :endloop
    const/4 v3, 0x0
    aget-boolean v0, v0, v3

    return v0

    :array_e
    .array-data 1
        0x0
        0x1
    .end array-data
.end method

.method public static final main([Ljava/lang/String;)V
    .registers 4

    invoke-static {}, LTestTypeTransformerMergeIZArray2;->foo()Z
    move-result v0
    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;
    new-instance v2, Ljava/lang/StringBuilder;
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
    const-string v3, "The answer is "
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    if-eqz v0, :cond_14
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
