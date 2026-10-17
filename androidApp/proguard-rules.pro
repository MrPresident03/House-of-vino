# kotlinx.serialization: keep generated serializers (navigation routes, sync payloads, settings DTOs).
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.houseofvino.**$$serializer { *; }
-keepclassmembers class com.houseofvino.** { *** Companion; }
-keepclasseswithmembers class com.houseofvino.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor / OkHttp optional platform classes.
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.lang.management.**

# WorkManager instantiates workers reflectively.
-keep class com.houseofvino.android.*Worker { <init>(...); }
