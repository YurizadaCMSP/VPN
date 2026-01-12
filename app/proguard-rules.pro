# Add project specific ProGuard rules here.

# Keep WireGuard classes
-keep class com.wireguard.** { *; }
-keepclassmembers class com.wireguard.** { *; }

# Keep VPN Service
-keep class * extends android.net.VpnService { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Keep source file names and line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile
