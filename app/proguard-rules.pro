# Proguard rules for BookReader

# Keep Compose rules
-keep class androidx.compose.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep native PDF renderer classes
-keep class android.graphics.pdf.** { *; }
