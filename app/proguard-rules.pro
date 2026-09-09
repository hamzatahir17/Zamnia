# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\hamza\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# Add any custom keep rules here if needed.

# Supabase & Kotlinx Serialization Keep Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Google Credential Manager & Identity Keep Rules
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.CredentialManager { *; }
-keep class androidx.credentials.GetCredentialRequest { *; }
-keep class androidx.credentials.exceptions.** { *; }

# Supabase Auth Session Models & Serialization Keep Rules
-keep class io.github.jan.supabase.** { *; }
-keep class io.github.jan.supabase.auth.** { *; }
-keep class io.github.jan.supabase.auth.user.** { *; }
-keep class io.github.jan.supabase.auth.models.** { *; }
-keep class com.zamnia.quizapp.data.model.** { *; }
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public static final ** INSTANCE;
}

