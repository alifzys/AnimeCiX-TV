# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.alifzys.an1mecix.**$$serializer { *; }
-keepclassmembers class com.alifzys.an1mecix.** {
    *** Companion;
}
-keepclasseswithmembers class com.alifzys.an1mecix.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp / Okio (transitive warnings)
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Coil
-dontwarn coil.**

# Compose runtime (genelde keep'lemeye gerek yok, Compose plugin halletti — sadece warning sustur)
-dontwarn org.jetbrains.compose.**
