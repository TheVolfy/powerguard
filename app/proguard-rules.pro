# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.powerguard.app.**$$serializer { *; }
-keepclassmembers class com.powerguard.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.powerguard.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room — keep generated DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
