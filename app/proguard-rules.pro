# PM (Enhanced) — R8 rules. Components declared in the manifest are kept by AGP automatically.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes InnerClasses, Signature, Exceptions, *Annotation*

-keep class com.yahyaoui.prayermode.RebuildWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }

-dontwarn okhttp3.**
-dontwarn okio.**
