# Room and Compose provide their consumer ProGuard rules.
# Keep Kotlin serialization model metadata used by the backup format.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.vibecode.minilinknotes.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class com.vibecode.minilinknotes.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
