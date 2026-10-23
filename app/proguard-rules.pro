# Add project specific ProGuard rules here.
-keepattributes *Annotation*,InnerClasses,Signature
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.macroai.tracker.** {
    *** Companion;
}

