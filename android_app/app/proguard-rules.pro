# ProGuard configuration for ArabiyyahApp

# Preserve data models
-keepclassmembers class com.arabiyyah.app.data.** { *; }
-keep class com.arabiyyah.app.data.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
