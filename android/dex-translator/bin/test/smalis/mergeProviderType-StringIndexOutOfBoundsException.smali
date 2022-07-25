.class public LTest;
.super Ljava/lang/Object;


.method final e(II)V
    .registers 6
    iget-object v0, p0, Lb;->U:[[Ljavax/microedition/lcdui/Image;
    if-eqz v0, :cond_13
    iget-object v0, p0, Lb;->U:[[Ljavax/microedition/lcdui/Image;
    array-length v0, v0
    if-ge p1, v0, :cond_13
    const/4 v0, -0x1
    if-ne p2, v0, :cond_14
    iget-object v0, p0, Lb;->U:[[Ljavax/microedition/lcdui/Image;
    move-object v1, v0
    move v0, p1
    :goto_10
    const/4 v2, 0x0
    aput-object v2, v1, v0
    :cond_13
    return-void

    :cond_14
    iget-object v0, p0, Lb;->U:[[Ljavax/microedition/lcdui/Image;
    aget-object v0, v0, p1
    if-eqz v0, :cond_13
    iget-object v0, p0, Lb;->U:[[Ljavax/microedition/lcdui/Image;
    aget-object v0, v0, p1
    move-object v1, v0
    move v0, p2
    goto :goto_10
.end method
