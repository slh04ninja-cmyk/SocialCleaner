# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keep class com.socialcleaner.model.** { *; }
-keep class com.socialcleaner.scanner.** { *; }

-dontwarn kotlinx.**
-dontwarn javax.annotation.**
