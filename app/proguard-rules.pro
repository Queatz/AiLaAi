-keep class io.ktor.** { *; }
-keep class kotlinx.** { *; }
-keep class androidx.** { *; }
-keep class com.queatz.** { *; }
-keep class com.huawei.** { *; }
-keep class org.bouncycastle.** { *; }

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

-renamesourcefileattribute SourceFile
-ignorewarnings
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
