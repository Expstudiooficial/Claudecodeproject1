# Keep JNI native method bindings for the llama.cpp bridge.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.expstudio.localai.inference.LlamaBridge { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
