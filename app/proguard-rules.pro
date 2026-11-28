# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep @dagger.hilt.EntryPoint interface * { *; }
-keep class com.piecer.app.widget.WidgetEntryPoint { *; }

# Widgets & Receivers
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class com.piecer.app.widget.** { *; }

# Quick Settings TileService
-keep class * extends android.service.quicksettings.TileService { *; }
-keep class com.piecer.app.quick.QuickAddTileService { *; }

# FileProvider
-keep class androidx.core.content.FileProvider { *; }

# Backup models & entities
-keep class com.piecer.app.data.backup.** { *; }
-keep class com.piecer.app.data.local.entity.** { *; }
-keepclassmembers enum com.piecer.app.data.local.entity.** { *; }
-keepclassmembers enum com.piecer.app.data.preferences.ThemeMode { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Update module (R8 keep untuk Hilt, model, parser JSON, dan BroadcastReceiver)
-keep class com.piecer.app.util.update.** { *; }
-keepclassmembers enum com.piecer.app.util.update.** { *; }
-keep class org.json.** { *; }

# Global CoroutineExceptionHandler (ServiceLoader) & CrashActivity
-keep class com.piecer.app.util.GlobalCoroutineExceptionHandler { *; }
-keep class com.piecer.app.ui.crash.CrashActivity { *; }
