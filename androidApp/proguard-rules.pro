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
-keep class com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res** { *; }
-keep class org.jetbrains.compose.resources.** { *; }

# 3. حماية نماذج البيانات الخاصة بمكتبة RSS 
-keep class com.prof18.rssparser.model.** { *; }

# 3.5 حماية مكتبات النظام الأساسية التي تعتمد على Reflection (WorkManager, Room, Startup, Coil, DataStore)
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
-keep class androidx.startup.** { *; }
-keep class androidx.datastore.** { *; }
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

# 7. توصيات غوغل و R8 الرسمية (AGP 9+) لإزالة السجلات بأمان تام دون كسر منطق الأكواد
# يزيل تلقائياً Verbose (2), Debug (3), Info (4) ويبقي التحذيرات والأخطاء Warn (5) و Error (6)
-maximumremovedandroidloglevel 4

# Keep all application classes
-keep class com.Nightjar.Th3GradeLibraryKMP.** { *; }
