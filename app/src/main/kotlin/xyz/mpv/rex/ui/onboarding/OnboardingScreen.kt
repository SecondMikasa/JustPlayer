package xyz.mpv.rex.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

private enum class OnboardingPage { NOTIFICATIONS, STORAGE, MICROPHONE }

private data class PageTheme(
    val gradientTop: Color,
    val gradientBottom: Color,
    val accentGlow: Color,
    val iconBackground: Color,
    val iconTint: Color,
)

private val notificationsTheme = PageTheme(
    gradientTop    = Color(0xFF0D1B2A),
    gradientBottom = Color(0xFF111318),
    accentGlow     = Color(0xFF4F8EF7),
    iconBackground = Color(0xFF1A3A6B),
    iconTint       = Color(0xFFADC6FF),
)

private val storageTheme = PageTheme(
    gradientTop    = Color(0xFF1A0D0D),
    gradientBottom = Color(0xFF111318),
    accentGlow     = Color(0xFFE06C5A),
    iconBackground = Color(0xFF5A1A1A),
    iconTint       = Color(0xFFFFDAD6),
)

private val microphoneTheme = PageTheme(
    gradientTop    = Color(0xFF130D1A),
    gradientBottom = Color(0xFF111318),
    accentGlow     = Color(0xFFA066C0),
    iconBackground = Color(0xFF402843),
    iconTint       = Color(0xFFFCD7FB),
)

// ---------------------------------------------------------------------------
// Root composable
// ---------------------------------------------------------------------------

/**
 * Full-screen onboarding permission flow shown once at first launch.
 * Covers notifications (skippable), storage (required), and microphone (skippable).
 *
 * @param onFinished Called when the user completes or skips the last page.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(OnboardingPage.NOTIFICATIONS, OnboardingPage.STORAGE, OnboardingPage.MICROPHONE)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    fun advance() {
        scope.launch {
            val next = pagerState.currentPage + 1
            if (next < pages.size) {
                pagerState.animateScrollToPage(next)
            } else {
                onFinished()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF111318),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ---- Pager (fills the whole screen) ----
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
            ) { pageIndex ->
                when (pages[pageIndex]) {
                    OnboardingPage.NOTIFICATIONS -> NotificationsPage(onAdvance = ::advance)
                    OnboardingPage.STORAGE       -> StoragePage(onAdvance = ::advance)
                    OnboardingPage.MICROPHONE    -> MicrophonePage(onAdvance = onFinished)
                }
            }

            // ---- Page indicator overlaid at the bottom ----
            PageDots(
                total   = pages.size,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Individual pages
// ---------------------------------------------------------------------------

@Composable
private fun NotificationsPage(onAdvance: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        onAdvance()
    }

    OnboardingPageLayout(
        theme          = notificationsTheme,
        icon           = Icons.Filled.Notifications,
        title          = "Stay in the Loop",
        body           = "Allow JustPlayer to send notifications for playback controls, media updates, and sleep timer alerts.",
        primaryLabel   = if (granted) "Continue" else "Allow Notifications",
        skippable      = true,
        onPrimary      = {
            if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onAdvance()
            } else {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onSkip  = onAdvance,
        granted = granted,
    )
}

@Composable
private fun StoragePage(onAdvance: () -> Unit) {
    val context = LocalContext.current

    fun hasStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    var granted by remember { mutableStateOf(hasStoragePermission()) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        granted = hasStoragePermission()
        if (granted) onAdvance()
    }

    val legacyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        if (isGranted) onAdvance()
    }

    LaunchedEffect(granted) {
        if (granted) onAdvance()
    }

    OnboardingPageLayout(
        theme        = storageTheme,
        icon         = Icons.Filled.Folder,
        title        = "Storage Access Required",
        body         = "JustPlayer requires \"All Files\" access to discover media and subtitles on your device — required by Android 11 and later.",
        primaryLabel = "Allow Access",
        skippable    = false,
        onPrimary    = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                )
                settingsLauncher.launch(intent)
            } else {
                legacyLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        },
        onSkip  = {},
        granted = granted,
    )
}

@Composable
private fun MicrophonePage(onAdvance: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        onAdvance()
    }

    OnboardingPageLayout(
        theme        = microphoneTheme,
        icon         = Icons.Filled.Mic,
        title        = "Make Music Come Alive",
        body         = "Allow microphone access to let the music visualizer react to your songs in real-time, pulsing with every beat and vocal.",
        primaryLabel = if (granted) "Let's Go!" else "Allow Microphone",
        skippable    = true,
        onPrimary    = {
            if (granted) onAdvance()
            else launcher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onSkip  = onAdvance,
        granted = granted,
    )
}

// ---------------------------------------------------------------------------
// Shared page layout  –  the aesthetic core
// ---------------------------------------------------------------------------

@Composable
private fun OnboardingPageLayout(
    theme: PageTheme,
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    skippable: Boolean,
    onPrimary: () -> Unit,
    onSkip: () -> Unit,
    granted: Boolean = false,
) {
    // ── Entry animation (runs once when this composable enters) ──────────────
    val enterAlpha  = remember { Animatable(0f) }
    val enterSlide  = remember { Animatable(60f) }
    LaunchedEffect(Unit) {
        launch { enterAlpha.animateTo(1f, tween(600, easing = EaseOutCubic)) }
        launch { enterSlide.animateTo(0f, tween(600, easing = EaseOutCubic)) }
    }

    // ── Idle floating animation for the icon badge ───────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue  = 8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatY",
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue  = 0.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    // ── Grant icon scale bounce ───────────────────────────────────────────────
    val iconScale by animateFloatAsState(
        targetValue  = if (granted) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Full-page gradient background
            .background(
                Brush.verticalGradient(
                    colors = listOf(theme.gradientTop, theme.gradientBottom),
                )
            ),
    ) {
        // ── Background ambient glow blobs ──────────────────────────────────
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-60).dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.accentGlow.copy(alpha = glowPulse * 0.35f),
                            Color.Transparent,
                        )
                    ),
                    shape = CircleShape,
                )
        )

        // ── Main content column ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    alpha       = enterAlpha.value
                    translationY = enterSlide.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.weight(0.6f))

            // ── Hero icon badge with glow ring ─────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp),
            ) {
                // Outer glow ring (blurred circle)
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .blur(32.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    theme.accentGlow.copy(alpha = glowPulse * 0.5f),
                                    Color.Transparent,
                                )
                            ),
                            shape = CircleShape,
                        )
                )

                // Icon card
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .offset(y = floatY.dp)
                        .scale(iconScale)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    theme.iconBackground.copy(alpha = 0.95f),
                                    theme.iconBackground.copy(alpha = 0.70f),
                                ),
                                start = Offset(0f, 0f),
                                end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                            )
                        )
                        // subtle inner stroke
                        .drawBehind {
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        theme.iconTint.copy(alpha = 0.35f),
                                        Color.Transparent,
                                    )
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(30.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = granted,
                        transitionSpec = {
                            (scaleIn(tween(350, easing = EaseOutBack)) +
                                    fadeIn(tween(300))) togetherWith
                                    (scaleOut(tween(200)) + fadeOut(tween(200)))
                        },
                        label = "iconContent",
                    ) { isGranted ->
                        Icon(
                            imageVector  = if (isGranted) Icons.Filled.CheckCircle else icon,
                            contentDescription = null,
                            tint         = theme.iconTint,
                            modifier     = Modifier.size(52.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(44.dp))

            // ── Title ──────────────────────────────────────────────────────
            Text(
                text       = title,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = Color.White,
                lineHeight = 36.sp,
            )

            Spacer(Modifier.height(18.dp))

            // ── Glassmorphic body card ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.04f),
                            )
                        )
                    )
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.05f),
                                )
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                        )
                    }
                    .padding(horizontal = 22.dp, vertical = 18.dp),
            ) {
                Text(
                    text      = body,
                    style     = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color     = Color.White.copy(alpha = 0.72f),
                    lineHeight = 24.sp,
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Primary gradient CTA button ────────────────────────────────
            Button(
                onClick  = onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                theme.accentGlow.copy(alpha = 0.90f),
                                theme.accentGlow.copy(alpha = 0.65f),
                            )
                        )
                    ),
                shape  = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor   = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                ),
            ) {
                Text(
                    text       = primaryLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp,
                    letterSpacing = 0.3.sp,
                )
            }

            // ── Skip button ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = skippable && !granted,
                enter   = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
                exit    = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick  = onSkip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.65f),
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.10f),
                                )
                            ),
                        ),
                    ) {
                        Text(
                            text     = "Skip for Now",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(72.dp)) // reserve space for dot indicator
        }
    }
}

// ---------------------------------------------------------------------------
// Page indicator dots  –  pill-style with accent color
// ---------------------------------------------------------------------------

@Composable
private fun PageDots(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    // Pick the accent for the active dot from the page theme
    val accentColors = listOf(
        notificationsTheme.accentGlow,
        storageTheme.accentGlow,
        microphoneTheme.accentGlow,
    )
    val activeColor = accentColors.getOrElse(current) { Color.White }

    Row(
        modifier = modifier.wrapContentHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val isActive = index == current
            val dotWidth by animateDpAsState(
                targetValue  = if (isActive) 28.dp else 8.dp,
                animationSpec = tween(350, easing = EaseOutQuart),
                label = "dotWidth",
            )
            val dotColor by animateFloatAsState(
                targetValue  = if (isActive) 1f else 0.35f,
                animationSpec = tween(350),
                label = "dotAlpha",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(
                        if (isActive) activeColor.copy(alpha = dotColor)
                        else Color.White.copy(alpha = dotColor),
                    ),
            )
        }
    }
}
