# =====================================================================
# Optimized R8 / ProGuard Keep Rules for Th3GradeLibraryKMP
# =====================================================================

# 1. حماية كلاسات البيانات (Serialization) لمنع انهيار قراءة الـ JSON أو قاعدة البيانات
-keepattributes *Annotation*, InnerClasses
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# 2. حماية موارد الواجهة (Compose Resources) لتجنب انهيار الصور والخطوط
-keep class com.Nightjar.gradeiraqi3library.generated.resources.Res** { *; }
-keep class org.jetbrains.compose.resources.** { *; }

# 3. حماية نماذج البيانات الخاصة بمكتبة RSS 
-keep class com.prof18.rssparser.model.** { *; }

# 3.5 حماية مكتبات النظام الأساسية التي تعتمد على Reflection (WorkManager, Room, Startup, Coil)
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
-keep class androidx.startup.** { *; }
-keep class coil3.** { *; }
-keepclassmembers class * extends androidx.compose.ui.graphics.painter.Painter { *; }

# 4. الحفاظ على أسماء مكونات Coroutines الحساسة
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 5. الحفاظ على أسطر تتبع الأخطاء (Crashlytics/Logs) دون كشف اسم الكلاس الأصلي
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature,EnclosingMethod

# 6. تجاهل تحذيرات كلاسات النظام غير المستخدمة في أندرويد لـ Ktor و SLF4J
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn javax.annotation.**

# 7. توصيات غوغل للأداء الأقصى وتخفيف المعالجة بالخلفية
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
