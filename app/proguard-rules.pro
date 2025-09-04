# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line number information for debugging stack traces in release builds
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep crash reporting information
-keepattributes *Annotation*

# === SECURITY RULES ===
# Remove debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# === ROOM DATABASE RULES ===
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Keep database entities
-keep class com.watxaut.myjumpapp.data.database.entities.** { *; }
-keep class com.watxaut.myjumpapp.data.database.dao.** { *; }

# === HILT DEPENDENCY INJECTION RULES ===
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class *

# === JETPACK COMPOSE RULES ===
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class androidx.compose.** {
    *;
}

# === ML KIT RULES ===
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Keep pose detection models and landmarks
-keep class com.google.mlkit.vision.pose.** { *; }

# === KOTLINX COROUTINES RULES ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# === GSON/JSON SERIALIZATION RULES (if used) ===
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# === APPLICATION SPECIFIC RULES ===
# Keep jump detection data classes
-keep class com.watxaut.myjumpapp.domain.jump.JumpData { *; }
-keep class com.watxaut.myjumpapp.domain.jump.DebugInfo { *; }

# Keep ViewModels and their state
-keep class com.watxaut.myjumpapp.presentation.viewmodels.** { *; }

# === CAMERA X RULES ===
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# === GENERAL KOTLIN RULES ===
-keepclassmembers class **$WhenMappings {
    <fields>;
}

-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$Companion {
    *;
}

# === REFLECTION USAGE RULES ===
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault