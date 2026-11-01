package com.Nightjar.Th3GradeLibraryKMP.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.Nightjar.Th3GradeLibraryKMP.MainActivity
import com.Nightjar.Th3GradeLibraryKMP.R

class LibraryGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LibraryGlanceWidget()
}

class LibraryGlanceWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_BOX = DpSize(120.dp, 80.dp)
        private val MEDIUM_ROW = DpSize(220.dp, 90.dp)
        private val LARGE_GRID = DpSize(260.dp, 160.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_BOX, MEDIUM_ROW, LARGE_GRID)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                when {
                    size.width >= LARGE_GRID.width && size.height >= LARGE_GRID.height -> {
                        LargeWidgetLayout(context)
                    }
                    size.width >= MEDIUM_ROW.width -> {
                        MediumWidgetLayout(context)
                    }
                    else -> {
                        SmallWidgetLayout(context)
                    }
                }
            }
        }
    }

    private fun createNavIntent(context: Context, page: String, category: String? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("page", page)
            if (category != null) {
                putExtra("category", category)
                data = android.net.Uri.parse("th3books://library?page=$page&category=$category")
            } else {
                data = android.net.Uri.parse("th3books://library?page=$page")
            }
        }
    }

    @Composable
    private fun SmallWidgetLayout(context: Context) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp)
                .clickable(actionStartActivity(createNavIntent(context, "home"))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_shortcut_book),
                    contentDescription = "Library",
                    modifier = GlanceModifier.size(32.dp)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "مكتبة الثالث",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }

    @Composable
    private fun MediumWidgetLayout(context: Context) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(22.dp)
                .background(GlanceTheme.colors.widgetBackground)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WidgetQuickAction(
                context = context,
                title = "الكتب",
                iconRes = R.drawable.ic_shortcut_book,
                page = "list",
                category = "books"
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            WidgetQuickAction(
                context = context,
                title = "الملازم",
                iconRes = R.drawable.ic_shortcut_notes,
                page = "list",
                category = "notes"
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            WidgetQuickAction(
                context = context,
                title = "المحفوظات",
                iconRes = R.drawable.ic_shortcut_bookmark,
                page = "bookmarks",
                category = null
            )
        }
    }

    @Composable
    private fun LargeWidgetLayout(context: Context) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.widgetBackground)
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(createNavIntent(context, "home"))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.app_icon),
                    contentDescription = "Logo",
                    modifier = GlanceModifier.size(24.dp)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "مكتبة الثالث متوسط",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Actions Grid
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WidgetQuickCard(
                    context = context,
                    title = "الكتب المدرسية",
                    iconRes = R.drawable.ic_shortcut_book,
                    page = "list",
                    category = "books",
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                WidgetQuickCard(
                    context = context,
                    title = "ملازم وملخصات",
                    iconRes = R.drawable.ic_shortcut_notes,
                    page = "list",
                    category = "notes",
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WidgetQuickCard(
                    context = context,
                    title = "الصفحات المحفوظة",
                    iconRes = R.drawable.ic_shortcut_bookmark,
                    page = "bookmarks",
                    category = null,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                WidgetQuickCard(
                    context = context,
                    title = "أخبار التربية",
                    iconRes = R.drawable.ic_shortcut_news,
                    page = "news",
                    category = null,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }

    @Composable
    private fun WidgetQuickAction(
        context: Context,
        title: String,
        iconRes: Int,
        page: String,
        category: String?
    ) {
        Column(
            modifier = GlanceModifier
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(actionStartActivity(createNavIntent(context, page, category))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = title,
                modifier = GlanceModifier.size(26.dp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            )
        }
    }

    @Composable
    private fun WidgetQuickCard(
        context: Context,
        title: String,
        iconRes: Int,
        page: String,
        category: String?,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .padding(8.dp)
                .clickable(actionStartActivity(createNavIntent(context, page, category))),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = title,
                    modifier = GlanceModifier.size(22.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = title,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
