# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/nissimpardo/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep Kaltura classes that may be referenced by reflection 
-keep,includedescriptorclasses class com.kaltura.playersdk.KControlsView { *; }
-keep,includedescriptorclasses class com.kaltura.playersdk.PlayerViewController { *; }
-keep,includedescriptorclasses class com.kaltura.playersdk.players.KPlayerController { *; }
-keepattributes Signature


# Google IMA
-dontwarn com.google.ads.**
-keep class com.google.** { *; }
-keep interface com.google.** { *; }
-keep class com.google.ads.interactivemedia.** { *; }
-keep interface com.google.ads.interactivemedia.** { *; }
