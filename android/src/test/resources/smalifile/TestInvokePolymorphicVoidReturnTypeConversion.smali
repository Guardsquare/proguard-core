# T3710: Tests that invoke-polymorphic instructions with void return types can be
#        converted correctly to Java bytecode.

.class public LTestInvokePolymorphicVoidReturnTypeConversion;
.super Ljava/lang/Object;

.method foo(Ljava/lang/invoke/MethodHandle;)V
    .registers 2
    const/16 v0, 0xa
    const/16 v1, 0x14

    invoke-polymorphic {p1, v0, v1}, Ljava/lang/invoke/MethodHandle;->invoke([Ljava/lang/Object;)Ljava/lang/Object;, (II)V

    return-void
.end method
