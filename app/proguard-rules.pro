# ProGuard rules for RivikAuthenticator
-keepattributes *Annotation*

# Bouncy Castle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# SQLCipher
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# argon2kt
-keep class com.lambdapioneer.argon2kt.** { *; }
-dontwarn com.lambdapioneer.argon2kt.**

# cbor-java
-keep class co.nstant.in.cbor.** { *; }
-dontwarn co.nstant.in.cbor.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Navigation routes (type-safe navigation uses class names)
-keep class dev.rivikauth.app.navigation.Screen { *; }
-keep class dev.rivikauth.app.navigation.Screen$* { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.rivikauth.**$$serializer { *; }
-keepclassmembers class dev.rivikauth.** { *** Companion; }
-keepclasseswithmembers class dev.rivikauth.** { kotlinx.serialization.KSerializer serializer(...); }
