# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Backup models & entities
-keep class com.piecer.app.data.backup.** { *; }
-keep class com.piecer.app.data.local.entity.** { *; }
-keepclassmembers enum com.piecer.app.data.local.entity.** { *; }
-keepclassmembers enum com.piecer.app.data.preferences.ThemeMode { *; }

# Biometric
-keep class androidx.biometric.** { *; }
