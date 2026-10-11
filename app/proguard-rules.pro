# R8/ProGuard rules for the release build.
#
# The app has a small, standard dependency set (Compose, Room, AndroidX). Their
# consumer rules are bundled with the libraries, so we need very little here.

# Keep Room-generated DAO/database implementations (referenced reflectively at init).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Room entities are accessed by generated code; keep their members.
-keepclassmembers class com.shubham.cards.data.CardEntity { *; }

# Kotlin metadata needed for some reflective lookups.
-keep class kotlin.Metadata { *; }

# Strip verbose Android logging from release (defensive: we use our own logger,
# but this guarantees no stray android.util.Log leaks into the shipped binary).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
