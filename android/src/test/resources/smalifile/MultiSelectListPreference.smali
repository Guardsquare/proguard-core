.class Landroid/preference/MultiSelectListPreference;
.super Ljava/lang/Object;


# virtual methods
.method test(Ljava/util/Set;Ljava/lang/Object;)V
    .registers 3

    invoke-interface {p1, p2}, Ljava/util/Set;->add(Ljava/lang/Object;)Z

    move-result p1

    invoke-static {p0, p1}, Landroid/preference/MultiSelectListPreference;->access$076(Landroid/preference/MultiSelectListPreference;I)Z

    return-void
.end method
