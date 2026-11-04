package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Nightjar.Th3GradeLibraryKMP.data.PlatformActionHandler
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine

@Composable
fun OnboardingScreen(
    platformActionHandler: PlatformActionHandler,
    onComplete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .elasticOverscroll()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.statusBars.add(WindowInsets.captionBar))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
            val density = androidx.compose.ui.platform.LocalDensity.current
            val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
            val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
            val isWide = screenWidthDp >= 600.dp || screenWidthDp > screenHeightDp

            Column(
                modifier = Modifier.widthIn(max = 1100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(if (isWide && screenHeightDp < 520.dp) 16.dp else 28.dp))
                
                // Welcome Hero Card
                val welcomeCardMaxWidth = if (isWide) 640.dp else 520.dp
                Card(
                    modifier = Modifier
                        .widthIn(max = welcomeCardMaxWidth)
                        .fillMaxWidth()
                        .padding(bottom = if (isWide) 20.dp else 28.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = if (isWide && screenHeightDp < 520.dp) 16.dp else 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🎓",
                                fontSize = if (isWide && screenHeightDp < 520.dp) 36.sp else 48.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = "أهلاً بك في مكتبة الثالث متوسط!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isWide && screenHeightDp < 520.dp) 19.sp else 22.sp
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Text(
                                text = "نوفر لك أحدث الكتب المدرسية والملازم الوزارية والأخبار الموثوقة لتعمل في هاتفك أوفلاين مدى الحياة بأعلى سرعة وأقل استهلاك للإنترنت.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = if (isWide && screenHeightDp < 520.dp) 14.sp else 16.sp
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section Title
                Text(
                    text = "دليلك السريع للبدء واستخدام التطبيق:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isWide && screenHeightDp < 520.dp) 16.sp else 18.sp
                    ),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .widthIn(max = if (isWide) 680.dp else 520.dp)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Educational and Action Steps
                val cardWidth = if (isWide) Modifier.widthIn(min = 280.dp, max = 340.dp) else Modifier.fillMaxWidth()
                val platformContext = coil3.compose.LocalPlatformContext.current
                val isBatchDownloading by com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager.isBatchDownloading.collectAsState()

                if (isWide) {
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.widthIn(max = 1100.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OnboardingStepCard(
                            modifier = cardWidth,
                            icon = { Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary) },
                            title = "1. تحميل وتصفح الكتب:",
                            description = "اضغط على أي كتاب أو ملزمة لتنزيلها، أو حمّل كل المناهج الآن لتصفحها أوفلاين بالكامل بدون إنترنت.",
                            buttonText = if (isBatchDownloading) "إيقاف التنزيل" else "تنزيل كل المناهج أوفلاين",
                            onClick = {
                                if (!isBatchDownloading) {
                                    val all = com.Nightjar.Th3GradeLibraryKMP.data.AllItems.books + com.Nightjar.Th3GradeLibraryKMP.data.AllItems.notes
                                    com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager.downloadAll(platformContext, all)
                                    platformActionHandler.showToast("بدأ تنزيل كافة كتب وملازم الثالث متوسط للعمل أوفلاين...")
                                } else {
                                    com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager.cancelAll()
                                    platformActionHandler.showToast("تم إيقاف التنزيل الجماعي")
                                }
                            }
                        )
                        OnboardingStepCard(
                            modifier = cardWidth,
                            icon = { Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary) },
                            title = "2. إشعارات الكتب والتحديثات:",
                            description = "فعّل الإشعارات لتصلك تنبيهات الملازم الجديدة، الأخبار الوزارية، والنتائج فور صدورها.",
                            buttonText = "تفعيل الإشعارات",
                            onClick = { platformActionHandler.requestNotificationPermission() }
                        )
                        OnboardingStepCard(
                            modifier = cardWidth,
                            icon = { Icon(Icons.Default.BatteryAlert, null, tint = MaterialTheme.colorScheme.secondary) },
                            title = "3. استقرار التنزيل (اختياري):",
                            description = "تخطي قيود البطارية لضمان استمرار تحميل الكتب والملازم في الخلفية حتى عند قفل الشاشة.",
                            buttonText = "ضبط البطارية",
                            onClick = {
                                platformActionHandler.showToast("يرجى اختيار 'بدون قيود' لضمان التحميل عند قفل الشاشة")
                                platformActionHandler.requestBatteryOptimizationExemption()
                            }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OnboardingStepCard(
                            modifier = cardWidth,
                            icon = { Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary) },
                            title = "1. تحميل وتصفح الكتب:",
                            description = "اضغط على أي كتاب أو ملزمة لتنزيلها، أو حمّل كل المناهج الآن لتصفحها أوفلاين بالكامل بدون إنترنت.",
                            buttonText = if (isBatchDownloading) "إيقاف التنزيل" else "تنزيل كل المناهج أوفلاين",
                            onClick = {
                                if (!isBatchDownloading) {
                                    val all = com.Nightjar.Th3GradeLibraryKMP.data.AllItems.books + com.Nightjar.Th3GradeLibraryKMP.data.AllItems.notes
                                    com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager.downloadAll(platformContext, all)
                                    platformActionHandler.showToast("بدأ تنزيل كافة كتب وملازم الثالث متوسط للعمل أوفلاين...")
                                } else {
                                    com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager.cancelAll()
                                    platformActionHandler.showToast("تم إيقاف التنزيل الجماعي")
                                }
                            }
                        )
                        OnboardingStepCard(
                            modifier = cardWidth,
                            icon = { Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary) },
                            title = "2. إشعارات الكتب والتحديثات:",
                            description = "فعّل الإشعارات لتصلك تنبيهات الملازم الجديدة، الأخبار الوزارية، والنتائج فور صدورها.",
                            buttonText = "تفعيل الإشعارات",
                            onClick = { platformActionHandler.requestNotificationPermission() }
                        )
                        OnboardingStepCard(
                            modifier = cardWidth,
                            icon = { Icon(Icons.Default.BatteryAlert, null, tint = MaterialTheme.colorScheme.secondary) },
                            title = "3. استقرار التنزيل (اختياري):",
                            description = "تخطي قيود البطارية لضمان استمرار تحميل الكتب والملازم في الخلفية حتى عند قفل الشاشة.",
                            buttonText = "ضبط البطارية",
                            onClick = {
                                platformActionHandler.showToast("يرجى اختيار 'بدون قيود' لضمان التحميل عند قفل الشاشة")
                                platformActionHandler.requestBatteryOptimizationExemption()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                // Extra space for bottom button clearance
                Spacer(modifier = Modifier.height(130.dp))
            }
        }

        // Bottom Continue Button (Completely unblocked)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { onComplete() },
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(50),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp,
                    hoveredElevation = 8.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "متابعة إلى التطبيق",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OnboardingStepCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    buttonText: String?,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (buttonText != null && onClick != null) {
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
