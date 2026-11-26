@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)
package com.Nightjar.Th3GradeLibraryKMP.adaptation

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ملف واحد موحّد لكل منطق التكيف (Single Source of Truth) —
 * مطابق لإرشادات Jetpack / Material 3 Adaptive الرسمية:
 *
 *  - Window Size Classes (width): COMPACT (<600dp) / MEDIUM (600–839dp) / EXPANDED (≥840dp)
 *    المصدر: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
 *  - Window Size Classes (height): COMPACT (short screens / landscape) / MEDIUM / EXPANDED
 *    المصدر: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
 *  - أجهزة الطي (Foldables) وحوامل سطح المكتب (DeX/desktops) تُعامل أصلاً كـ MEDIUM/EXPANDED،
 *    والتكيف يعتمد على WindowInsets الحقيقية في وقت التشغيل (وليس قياسات ثابتة).
 *
 * أي قرار تكيفي يُتخذ هنا وفقط هنا — لا يوجد منطق تكيف مبعثر داخل الشاشات.
 */
object AdaptiveLayout {

    // ── Window classification helpers ──────────────────────────────────────────
    fun isWide(widthClass: WindowWidthSizeClass): Boolean = widthClass != WindowWidthSizeClass.COMPACT
    fun isCompactHeight(heightClass: WindowHeightSizeClass): Boolean = heightClass == WindowHeightSizeClass.COMPACT

    /** Phone landscape / compact-height windows are treated as "phone", never as tablets. */
    fun isPhoneLike(widthClass: WindowWidthSizeClass, heightClass: WindowHeightSizeClass): Boolean =
        widthClass == WindowWidthSizeClass.COMPACT || isCompactHeight(heightClass)

    /**
     * دعم وضع الطي المكتبي / فلكس مود (Foldables Flex / Tabletop Posture):
     * عندما يكون الجهاز مطوياً في وضع الطاولة (نصف شاشة للأعلى ونصف شاشة للأسفل).
     */
    fun isTabletop(posture: androidx.compose.material3.adaptive.Posture): Boolean = posture.isTabletop

    /**
     * التحقق من النوافذ المنبثقة والعائمة وسامسونج ديكس (Pop-up Window / Freeform / Samsung DeX Windowed):
     * النوافذ المنبثقة المنفصلة التي يمكن للمستخدم تغيير أبعادها ديناميكياً.
     */
    fun isWindowed(widthClass: WindowWidthSizeClass, heightClass: WindowHeightSizeClass): Boolean =
        widthClass == WindowWidthSizeClass.COMPACT || isCompactHeight(heightClass)

    // ── شريط العنوان (Floating Top Bar) ───────────────────────────────────────
    /**
     * الارتفاع المضبوط حسب متريال:
     *  - الهاتف الأفقي ونوافذ فلكس مود (compact height / tabletop) → 44.dp
     *  - الهاتف العمودي العادي والنوافذ المنبثقة الصغيرة → 48.dp (أقل من 50 كما طُلب)
     *  - الشاشات الكبيرة والمكتبية (DeX / Tablets / Foldables 펼친 상태) → 58.dp
     */
    fun topBarHeight(
        heightClass: WindowHeightSizeClass,
        widthClass: WindowWidthSizeClass,
        isTabletop: Boolean = false
    ): Dp = when {
        isTabletop || isCompactHeight(heightClass) -> 44.dp
        widthClass == WindowWidthSizeClass.COMPACT -> 48.dp
        else -> 58.dp
    }

    /** الحشوة العمودية داخل شريط العنوان العائم حسب ارتفاع النافذة وحالة الطي. */
    fun topBarPaddingV(heightClass: WindowHeightSizeClass, isTabletop: Boolean = false): Dp =
        if (isTabletop || isCompactHeight(heightClass)) 4.dp else 6.dp

    /** الارتفاع الكلي لكتلة شريط العنوان (Inset + ارتفاع + حشوة) — يُستخدم لتموضع المحتوى والبحث. */
    fun topBarBlockHeight(
        currentStatusBar: Dp,
        heightClass: WindowHeightSizeClass,
        widthClass: WindowWidthSizeClass,
        isTabletop: Boolean = false
    ): Dp = currentStatusBar + topBarHeight(heightClass, widthClass, isTabletop) + topBarPaddingV(heightClass, isTabletop) * 2

    /** قفزة إضافية بين شريط العنوان وأول بيكسل من المحتوى (تمنع التداخل تماماً، أقل من 7 دي بي ديناميكياً). */
    const val TOP_BAR_CONTENT_GAP_DP = 6

    /** المساحة العلوية للمحتوى — محسوبة ديناميكياً من Insets النظام دائماً. */
    fun contentTopPadding(
        currentStatusBar: Dp,
        heightClass: WindowHeightSizeClass,
        widthClass: WindowWidthSizeClass,
        reachabilityZone: Dp = 0.dp,
        isTabletop: Boolean = false
    ): Dp = topBarBlockHeight(currentStatusBar, heightClass, widthClass, isTabletop) + TOP_BAR_CONTENT_GAP_DP.dp + reachabilityZone

    /** منطقة السحب للوصول (One UI Reachability): للهواتف العمودية فقط — ثابتة منطقياً ولا تُطبق أفقياً. */
    fun reachabilityZone(widthClass: WindowWidthSizeClass, heightClass: WindowHeightSizeClass): Dp =
        if (!isWide(widthClass) && !isCompactHeight(heightClass)) 40.dp else 0.dp

    // ── شبكة الأعمدة (Grid column policies) ───────────────────────────────────
    /** المكتبة: 2 أعمدة في الوضع الشبكي الصغير (strictly)، 4 في MEDIUM، 4–7 في EXPANDED؛ القائمة عمودان في EXPANDED وإلا عمود واحد. */
    fun libraryColumns(widthClass: WindowWidthSizeClass, screenWidthDp: Dp, isGridView: Boolean): GridCells =
        GridCells.Fixed(
            if (isGridView) {
                when {
                    widthClass == WindowWidthSizeClass.COMPACT -> 2
                    widthClass == WindowWidthSizeClass.MEDIUM -> 4
                    else -> {
                        val availableWidth = (screenWidthDp - 120.dp).coerceAtLeast(600.dp)
                        (availableWidth / 175.dp).toInt().coerceIn(4, 7)
                    }
                }
            } else {
                if (widthClass == WindowWidthSizeClass.EXPANDED) 2 else 1
            }
        )

    /** الأخبار: ~380.dp لكل عمود، حتى 3 أعمدة، وإلا عمود واحد. */
    fun newsColumns(widthClass: WindowWidthSizeClass, screenWidthDp: Dp): GridCells {
        val isMedium = isWide(widthClass)
        val availableWidth = (screenWidthDp - (if (isMedium) 120.dp else 32.dp)).coerceAtLeast(280.dp)
        val computed = (availableWidth / 380.dp).toInt().coerceIn(1, 3)
        return GridCells.Fixed(computed)
    }

    /** الاختصارات: عمودان دائماً في الشاشات الصغيرة/التقسيم، 4 في MEDIUM، 4–7 في EXPANDED. */
    fun shortcutsColumns(widthClass: WindowWidthSizeClass, screenWidthDp: Dp): GridCells {
        val cols = when {
            widthClass == WindowWidthSizeClass.COMPACT || screenWidthDp < 600.dp -> 2
            widthClass == WindowWidthSizeClass.MEDIUM || screenWidthDp < 840.dp -> 4
            else -> {
                val availableWidth = (screenWidthDp - 120.dp).coerceAtLeast(600.dp)
                (availableWidth / 160.dp).toInt().coerceIn(4, 7)
            }
        }
        return GridCells.Fixed(cols)
    }

    /** المحفوظات: عمود واحد في الشاشات الصغيرة، عمودان في MEDIUM، وحتى 4 أعمدة في EXPANDED. */
    fun bookmarksColumns(widthClass: WindowWidthSizeClass, screenWidthDp: Dp): GridCells {
        val cols = when {
            widthClass == WindowWidthSizeClass.COMPACT || screenWidthDp < 600.dp -> 1
            widthClass == WindowWidthSizeClass.MEDIUM || screenWidthDp < 840.dp -> 2
            else -> {
                val availableWidth = (screenWidthDp - 120.dp).coerceAtLeast(600.dp)
                (availableWidth / 360.dp).toInt().coerceIn(2, 4)
            }
        }
        return GridCells.Fixed(cols)
    }
}
