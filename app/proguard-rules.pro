# =============================================================================
# Thailand Weather Intelligence — ProGuard / R8 Rules
# =============================================================================
# These rules preserve classes that are accessed reflectively at runtime.
# R8 full-mode is in use; add rules here when you add new reflection-heavy libs.

# ---------- Kotlin ----------
-keepattributes *Annotation*, InnerClasses
-keepattributes SourceFile, LineNumberTable   # Keep stack traces readable
-keepattributes Signature                      # Keep generic type info for Retrofit/Gson

# ---------- Kotlin Serialization ----------
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    static ** INSTANCE;
    static ** Companion;
    ** serialVersionUID;
}

# ---------- Room ----------
# Room generates code at compile time via KSP — no runtime reflection needed.
# Keep entity / DAO class names for error messages.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep interface * extends androidx.room.RoomDatabase { *; }

# ---------- Retrofit + OkHttp ----------
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# ---------- DataStore ----------
-keep class androidx.datastore.** { *; }

# ---------- WorkManager ----------
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---------- Coroutines ----------
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.flow.**

# ---------- Android Keystore / Security ----------
-keep class android.security.keystore.** { *; }
-keep class javax.crypto.** { *; }

# ---------- Compose ----------
# Compose Compiler handles its own keep rules; nothing extra needed here.
-dontwarn androidx.compose.**

# ---------- Misc ----------
-dontwarn java.lang.invoke.**
-dontwarn **$$Lambda$*

