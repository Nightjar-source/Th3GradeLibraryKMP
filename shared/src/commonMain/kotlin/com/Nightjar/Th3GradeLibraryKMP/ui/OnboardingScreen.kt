package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                .statusBarsPadding()
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
                
                // Welcome Card (Dynamic width on tablet/DeX/landscape vs portrait phone)
                val welcomeCardMaxWidth = if (isWide) 640.dp else 520.dp
                Card(
                    modifier = Modifier.widthIn(max = welcomeCardMaxWidth).fillMaxWidth().padding(bottom = if (isWide) 20.dp else 32.dp),
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
                                text = "أهلاً بكم في تطبيق مكتبة الثالث متوسط!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isWide && screenHeightDp < 520.dp) 19.sp else 22.sp
                                ),
                                textAlign = TextAlign.Justify,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Text(
                                text = "نوفر لك أحدث الملازم، الكتب المدرسية، الأخبار الرسمية، والمحفوظات لتعمل بدون إنترنت بسلاسة وبأفضل تجربة.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = if (isWide && screenHeightDp < 520.dp) 14.sp else 16.sp
                                ),
                                textAlign = TextAlign.Justify,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = "لضمان عمل التطبيق بشكل ممتاز على هاتفك واستمتع، نحتاج إلى 3 خطوات سريعة:",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isWide && screenHeightDp < 520.dp) 17.sp else 19.sp
                    ),
                    textAlign = TextAlign.Justify,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .widthIn(max = if (isWide) 680.dp else 520.dp)
                        .padding(bottom = if (isWide) 20.dp else 32.dp)
                )

                if (isWide) {
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.widthIn(max = 1100.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OnboardingStepCard(
                            modifier = Modifier.widthIn(min = 280.dp, max = 340.dp),
                            icon = { Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary) },
                            title = "1. إشعارات النظام:",
                            description = "لتبقى على اطلاع بآخر التحديثات والتنبيهات.",
                            buttonText = "تفعيل الإشعارات",
                            onClick = { platformActionHandler.requestNotificationPermission() }
                        )
                        OnboardingStepCard(
                            modifier = Modifier.widthIn(min = 280.dp, max = 340.dp),
                            icon = { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.error) },
                            title = "2. منع إيقاف نشاط التطبيق:",
                            description = "تنبيه هام: يُرجى الانتقال للإعدادات وإيقاف خيار \"إيقاف مؤقت لنشاط التطبيق في حال عدم استخدامه\" لضمان استمرار الخدمة في الخلفية.",
                            buttonText = "فتح إعدادات التطبيق",
                            onClick = { platformActionHandler.requestAutoRevokeExemption() }
                        )
                        OnboardingStepCard(
                            modifier = Modifier.widthIn(min = 280.dp, max = 340.dp),
                            icon = { Icon(Icons.Default.BatteryAlert, null, tint = MaterialTheme.colorScheme.primary) },
                            title = "3. تخطي خمول البطارية:",
                            description = "استثناء التطبيق من قيود توفير الطاقة لضمان المزامنة.",
                            buttonText = "تفعيل الآن",
                            onClick = {
                                platformActionHandler.showToast("يرجى اختيار 'بدون قيود' أو 'عدم التحسين' لضمان عمل التطبيق في الخلفية")
                                platformActionHandler.requestBatteryOptimizationExemption()
                            }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OnboardingStepCard(
                            modifier = Modifier.fillMaxWidth(),
                            icon = { Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary) },
                            title = "1. إشعارات النظام:",
                            description = "لتبقى على اطلاع بآخر التحديثات والتنبيهات.",
                            buttonText = "تفعيل الإشعارات",
                            onClick = { platformActionHandler.requestNotificationPermission() }
                        )
                        OnboardingStepCard(
                            modifier = Modifier.fillMaxWidth(),
                            icon = { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.error) },
                            title = "2. منع إيقاف نشاط التطبيق:",
                            description = "تنبيه هام: يُرجى الانتقال للإعدادات وإيقاف خيار \"إيقاف مؤقت لنشاط التطبيق في حال عدم استخدامه\" لضمان استمرار الخدمة في الخلفية.",
                            buttonText = "فتح إعدادات التطبيق",
                            onClick = { platformActionHandler.requestAutoRevokeExemption() }
                        )
                        OnboardingStepCard(
                            modifier = Modifier.fillMaxWidth(),
                            icon = { Icon(Icons.Default.BatteryAlert, null, tint = MaterialTheme.colorScheme.primary) },
                            title = "3. تخطي خمول البطارية:",
                            description = "استثناء التطبيق من قيود توفير الطاقة لضمان المزامنة.",
                            buttonText = "تفعيل الآن",
                            onClick = {
                                platformActionHandler.showToast("يرجى اختيار 'بدون قيود' أو 'عدم التحسين' لضمان عمل التطبيق في الخلفية")
                                platformActionHandler.requestBatteryOptimizationExemption()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                // Extra space so the user can scroll past the floating button
                Spacer(modifier = Modifier.height(140.dp))
            }
        }

        // Bottom Continue Button with soft progressive fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    val enabledCount = listOf(
                        platformActionHandler.areNotificationsEnabled(),
                        platformActionHandler.isBatteryOptimizationIgnored(),
                        platformActionHandler.isAutoRevokeWhitelisted()
                    ).count { it }

                    if (enabledCount >= 2) {
                        onComplete()
                    } else {
                        platformActionHandler.showToast("يرجى تفعيل صلاحيتين على الأقل للمتابعة")
                    }
                },
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(50), // Oval
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp,
                    hoveredElevation = 10.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "متابعة للتطبيق",
                    fontSize = 20.sp,
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
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
