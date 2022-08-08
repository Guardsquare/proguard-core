# Tests that Dex2Jar's NewTransformer correctly transforms
# object creation and instantation instructions.
#
# T5005: NewTransformer sometimes has issues.

.class public LTestNewTransformer;
.super Ljava/lang/Object;

.method foo()LTestNewTransformer;
    .registers 2
    new-instance v1, LTestNewTransformer;
    invoke-direct {v1}, LTestNewTransformer;-><init>()V
    return-object v1
.end method
