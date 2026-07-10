# Add project-specific ProGuard rules here.

# kotlinx.serialization — keep the generated serializers for the backup
# schema classes so JSON export/restore keeps working under R8. The library
# ships consumer rules for its own internals; these cover the app's
# @Serializable models against aggressive full-mode shrinking.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.spendwise.app.export.** {
    *** Companion;
}
-keepclasseswithmembers class com.spendwise.app.export.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.spendwise.app.export.**$$serializer { *; }
