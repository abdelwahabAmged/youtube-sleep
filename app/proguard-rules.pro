# NewPipeExtractor parses YouTube with reflection-heavy code and runs YouTube's player JavaScript
# in Rhino, which generates and loads classes at runtime. Keep both intact.
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-keep class com.grack.nanojson.** { *; }
-keep class org.jsoup.** { *; }

# Optional desktop-JVM dependencies referenced by Rhino/jsoup that don't exist on Android.
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn org.mozilla.javascript.tools.**
-dontwarn org.jspecify.annotations.**
-dontwarn com.google.re2j.**

# Keep line numbers so crash reports stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
