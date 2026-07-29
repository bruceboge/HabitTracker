# Habit Tracker ProGuard Rules

# Retrofit
-keepattributes Signature
-keepattributes Annotation
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keep class com.habittracker.app.data.remote.dto.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
