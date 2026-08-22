# =====================================================================
# R8 / ProGuard Keep Rules for Th3GradeLibraryKMP
# =====================================================================

# Keep Kotlin Multiplatform & Compose internal reflective accesses
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }

# Keep data models & network serializations (Ktor / Serialization)
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class com.grade3.library.data.** { *; }
-keep class com.grade3.library.network.** { *; }
-keep class com.kar.th3grade.library.** { *; }

# Keep Coil image loading & bitmap drawables
-keep class io.coil_kt.coil3.** { *; }
-keepclassmembers class * extends androidx.compose.ui.graphics.painter.Painter { *; }

# Keep Android resource accessors & Compose generated resources
-keep class com.grade3.library.generated.resources.** { *; }

# Preserve line numbers and source file names for clean debugging logs
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,AnnotationDefault
