# Pulse Music R8 rules.
#
# The app uses reflection in a few places (Moshi's KotlinJsonAdapterFactory,
# Firebase Realtime Database model mapping, Retrofit and Kotlin serialization),
# so those models and annotations must survive shrinking and obfuscation.

# Disable aggressive whole-program optimizations (replaces old proguard-android.txt behavior)
# to prevent cross-class merging and method inlining across Compose and ViewModel boundaries.
-dontoptimize

# Preserve metadata used by Kotlin reflection and Moshi.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, AnnotationDefault, RuntimeVisibleParameterAnnotations

# Moshi + Kotlin reflection.
-keep class com.squareup.moshi.** { *; }
-keep class kotlin.reflect.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}

# Domain, DTO and Room models are read/written reflectively by Firebase and Moshi.
-keep class com.example.domain.model.** { *; }
-keepclassmembers class com.example.domain.model.** { *; }
-keep class com.example.data.remote.dto.** { *; }
-keep class com.example.data.remote.dto.**JsonAdapter { *; }
-keep class com.example.data.local.entity.** { *; }

# ViewModels and lifecycle
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class com.example.ui.viewmodel.** { *; }
-keepclassmembers class com.example.ui.viewmodel.** { *; }

# Room Database, DAOs, and generated implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.example.data.local.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class **_Impl { *; }

# Dependency injection and services
-keep class com.example.di.** { *; }
-keep class com.example.service.** { *; }

# Retrofit service interfaces and suspend functions.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Kotlin serialization & ytm-kt (YouTube Music API via Ktor).
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class dev.toastbits.ytmkt.** { *; }
-keepclassmembers class dev.toastbits.ytmkt.** { *; }

# Ktor client and engine loading
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# NewPipeExtractor loads service classes by name and parses remote JSON.
-keep class org.schabi.newpipe.extractor.** { *; }
-keepclassmembers class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# Rhino is used to execute NewPipe's JavaScript player code.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
-dontwarn org.mozilla.**
-dontwarn java.beans.**
-dontwarn java.awt.**

# Keep line numbers for readable crash reports; the release workflow uploads mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
