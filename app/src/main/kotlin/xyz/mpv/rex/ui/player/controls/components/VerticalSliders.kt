package xyz.mpv.rex.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.FastOutSlowInEasing
import org.koin.compose.koinInject
import xyz.mpv.rex.preferences.AppearancePreferences
import xyz.mpv.rex.preferences.preference.collectAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import xyz.mpv.rex.ui.player.controls.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.R
import xyz.mpv.rex.ui.theme.spacing
import kotlin.math.roundToInt

fun percentage(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
): Float = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

fun percentage(
  value: Int,
  range: ClosedRange<Int>,
): Float = ((value - range.start - 0f) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

@Composable
fun VerticalSlider(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
  overflowValue: Float? = null,
  overflowRange: ClosedFloatingPointRange<Float>? = null,
  isActive: Boolean = false
) {
  val coercedValue = value.coerceIn(range)

  // Read Toggle for Bounce Animation from Preferences
  val appearancePrefs = koinInject<AppearancePreferences>()
  val enableBounceAnimation by appearancePrefs.enableBounceAnimation.collectAsState()
  val enableGlass by appearancePrefs.enableGlassPlayerControls.collectAsState()

  val trackWidthAnim = remember { Animatable(22f) }

  // Listen for changes to the interaction state (touching vs. released)
  LaunchedEffect(isActive, enableBounceAnimation) {
    if (!enableBounceAnimation) {
      trackWidthAnim.snapTo(22f)
      return@LaunchedEffect
    }
    if (isActive) {
      trackWidthAnim.animateTo(
        targetValue = 32f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMedium
        )
      )
    } else {
      kotlinx.coroutines.delay(150)
      trackWidthAnim.animateTo(
        targetValue = 22f,
        animationSpec = spring(
          dampingRatio = 0.4f,
          stiffness = Spring.StiffnessLow
        )
      )
    }
  }

  val trackWidth = trackWidthAnim.value.dp

  val trackModifier = if (enableGlass) {
    Modifier.glassSurface(
      shape = RoundedCornerShape(16.dp),
      backgroundColor = Color.White.copy(alpha = 0.05f),
      borderColor = Color.White.copy(alpha = 0.15f),
      borderWidth = 1.dp,
      outerShadowColor = Color.Black.copy(alpha = 0.00f),
      outerShadowBlur = 0.dp,
      outerShadowOffsetX = 0.dp,
      outerShadowOffsetY = 0.dp,
      innerHighlightColor = Color.White.copy(alpha = 0.35f),
      innerHighlightBlur = 5.dp,
      innerHighlightOffsetX = (-2).dp,
      innerHighlightOffsetY = (-2).dp,
      innerShadowColor = Color.Black.copy(alpha = 0.35f),
      innerShadowBlur = 5.dp,
      innerShadowOffsetX = 2.dp,
      innerShadowOffsetY = 2.dp
    )
  } else {
    Modifier
      .background(MaterialTheme.colorScheme.background)
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
      )
  }

  // Outer fixed-width container prevents layout shifts
  Box(
    modifier =
      modifier
      .height(120.dp)
      .width(32.dp),
    contentAlignment = Alignment.BottomCenter
  ) {
    // Inner track that actually animates
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(trackWidth)
        .clip(RoundedCornerShape(16.dp))
        .then(trackModifier),
      contentAlignment = Alignment.BottomCenter,
    ) {
      val targetHeight by animateFloatAsState(percentage(coercedValue, range), label = "vsliderheight")
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(targetHeight)
          .background(MaterialTheme.colorScheme.primary),
      )
      if (overflowRange != null && overflowValue != null) {
        val overflowHeight by animateFloatAsState(
          percentage(overflowValue, overflowRange),
          label = "vslideroverflowheight",
        )
        Box(
          Modifier
            .fillMaxWidth()
            .fillMaxHeight(overflowHeight)
            .background(MaterialTheme.colorScheme.errorContainer),
        )
      }
    }
  }
}

@Composable
fun VerticalSlider(
  value: Int,
  range: ClosedRange<Int>,
  modifier: Modifier = Modifier,
  overflowValue: Int? = null,
  overflowRange: ClosedRange<Int>? = null,
  isActive: Boolean = false
) {
  val coercedValue = value.coerceIn(range)

  // Read Toggle for Bounce Animation from Preferences
  val appearancePrefs = koinInject<AppearancePreferences>()
  val enableBounceAnimation by appearancePrefs.enableBounceAnimation.collectAsState()
  val enableGlass by appearancePrefs.enableGlassPlayerControls.collectAsState()

  val trackWidthAnim = remember { Animatable(22f) }

  // Listen for changes to the interaction state (touching vs. released)
  LaunchedEffect(isActive, enableBounceAnimation) {
    if (!enableBounceAnimation) {
      trackWidthAnim.snapTo(22f)
      return@LaunchedEffect
    }
    if (isActive) {
      trackWidthAnim.animateTo(
        targetValue = 32f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMedium
        )
      )
    } else {
      kotlinx.coroutines.delay(150)
      trackWidthAnim.animateTo(
        targetValue = 22f,
        animationSpec = spring(
          dampingRatio = 0.4f,
          stiffness = Spring.StiffnessLow
        )
      )
    }
  }

  val trackWidth = trackWidthAnim.value.dp

  val trackModifier = if (enableGlass) {
    Modifier.glassSurface(
      shape = RoundedCornerShape(16.dp),
      backgroundColor = Color.White.copy(alpha = 0.05f),
      borderColor = Color.White.copy(alpha = 0.15f),
      borderWidth = 1.dp,
      outerShadowColor = Color.Black.copy(alpha = 0.00f),
      outerShadowBlur = 0.dp,
      outerShadowOffsetX = 0.dp,
      outerShadowOffsetY = 0.dp,
      innerHighlightColor = Color.White.copy(alpha = 0.35f),
      innerHighlightBlur = 5.dp,
      innerHighlightOffsetX = (-2).dp,
      innerHighlightOffsetY = (-2).dp,
      innerShadowColor = Color.Black.copy(alpha = 0.35f),
      innerShadowBlur = 5.dp,
      innerShadowOffsetX = 2.dp,
      innerShadowOffsetY = 2.dp
    )
  } else {
    Modifier
      .background(MaterialTheme.colorScheme.background)
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
      )
  }

  // Outer fixed-width container
  Box(
    modifier = modifier
      .height(120.dp)
      .width(32.dp),
    contentAlignment = Alignment.BottomCenter
  ) {
    // Inner track that actually animates
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(trackWidth)
        .clip(RoundedCornerShape(16.dp))
        .then(trackModifier),
      contentAlignment = Alignment.BottomCenter,
    ) {
      val targetHeight by animateFloatAsState(percentage(coercedValue, range), label = "vsliderheight")
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(targetHeight)
          .background(MaterialTheme.colorScheme.primary),
      )
      if (overflowRange != null && overflowValue != null) {
        val overflowHeight by animateFloatAsState(
          percentage(overflowValue, overflowRange),
          label = "vslideroverflowheight",
        )
        Box(
          Modifier
            .fillMaxWidth()
            .fillMaxHeight(overflowHeight)
            .background(MaterialTheme.colorScheme.errorContainer),
        )
      }
    }
  }
}

@Composable
fun BrightnessSlider(
  brightness: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
  isActive: Boolean = false
) {
  val coercedBrightness = brightness.coerceIn(range)
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Text(
        (coercedBrightness * 100).toInt().toString(),
        style = MaterialTheme.typography.bodySmall,
      )
      VerticalSlider(
        coercedBrightness,
        range,
        isActive = isActive
      )
      Icon(
        when (percentage(coercedBrightness, range)) {
          in 0f..0.3f -> Icons.Default.BrightnessLow
          in 0.3f..0.6f -> Icons.Default.BrightnessMedium
          in 0.6f..1f -> Icons.Default.BrightnessHigh
          else -> Icons.Default.BrightnessMedium
        },
        contentDescription = null,
      )
    }
  }
}

@Composable
fun VolumeSlider(
  volume: Int,
  mpvVolume: Int,
  range: ClosedRange<Int>,
  boostRange: ClosedRange<Int>?,
  modifier: Modifier = Modifier,
  displayAsPercentage: Boolean = false,
  isActive: Boolean = false,
  /** Called with the new absolute volume level when the user drags the slider. */
  onVolumeChange: ((Int) -> Unit)? = null,
  /** Called with the new boost volume level when the user drags into the boost range. */
  onBoostVolumeChange: ((Int) -> Unit)? = null,
  /** Called with true when the user starts dragging, false when they release.
   *  The caller uses this to keep the slider visible while the user is interacting. */
  onInteractionChange: ((Boolean) -> Unit)? = null,
) {
  val currentVolume by rememberUpdatedState(volume)
  val currentMpvVolume by rememberUpdatedState(mpvVolume)
  val currentOnVolumeChange by rememberUpdatedState(onVolumeChange)
  val currentOnBoostVolumeChange by rememberUpdatedState(onBoostVolumeChange)
  val currentOnInteractionChange by rememberUpdatedState(onInteractionChange)

  var isDragging by remember { mutableStateOf(false) }
  var activeLevel by remember { mutableFloatStateOf(0f) }
  val trackHeightPx = remember { mutableFloatStateOf(300f) }

  val normalRangeSize = (range.endInclusive - range.start).coerceAtLeast(1)
  val boostRangeSize = boostRange?.let { (it.endInclusive - it.start).coerceAtLeast(0) } ?: 0
  val totalRangeSize = normalRangeSize + boostRangeSize

  val dragModifier = if (onVolumeChange != null) {
    Modifier.pointerInput(range, boostRange) {
      detectVerticalDragGestures(
        onDragStart = {
          isDragging = true
          val currentBoost = if (boostRange != null) (currentMpvVolume - 100).coerceAtLeast(0) else 0
          activeLevel = (currentVolume - range.start).toFloat() + currentBoost.toFloat()
          currentOnInteractionChange?.invoke(true)
        },
        onDragEnd = {
          isDragging = false
          currentOnInteractionChange?.invoke(false)
        },
        onDragCancel = {
          isDragging = false
          currentOnInteractionChange?.invoke(false)
        },
        onVerticalDrag = { change, dragAmount ->
          change.consume()
          val sliderHeightPx = trackHeightPx.floatValue.coerceAtLeast(1f)
          val delta = (-dragAmount / sliderHeightPx) * totalRangeSize
          activeLevel = (activeLevel + delta).coerceIn(0f, totalRangeSize.toFloat())

          if (activeLevel <= normalRangeSize.toFloat()) {
            val targetVol = (range.start + activeLevel.roundToInt()).coerceIn(range.start, range.endInclusive)
            currentOnVolumeChange?.invoke(targetVol)
            if (boostRange != null && currentMpvVolume > 100) {
              currentOnBoostVolumeChange?.invoke(0)
            }
          } else {
            currentOnVolumeChange?.invoke(range.endInclusive)
            if (boostRange != null && currentOnBoostVolumeChange != null) {
              val targetBoost = (activeLevel - normalRangeSize.toFloat()).roundToInt().coerceIn(boostRange.start, boostRange.endInclusive)
              currentOnBoostVolumeChange?.invoke(targetBoost)
            }
          }
        },
      )
    }
  } else {
    Modifier
  }

  val displayVol = if (isDragging) {
    (range.start + minOf(activeLevel.roundToInt(), normalRangeSize)).coerceIn(range.start, range.endInclusive)
  } else {
    volume
  }

  val displayBoost = if (isDragging && boostRange != null) {
    maxOf(0, (activeLevel - normalRangeSize.toFloat()).roundToInt()).coerceIn(boostRange.start, boostRange.endInclusive)
  } else {
    (mpvVolume - 100).coerceAtLeast(0)
  }

  val percentage = (percentage(displayVol, range) * 100).roundToInt()

  Surface(
    modifier = modifier
      .then(dragModifier)
      .onSizeChanged { if (it.height > 0) trackHeightPx.floatValue = it.height.toFloat() },
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
  ) {
    Column(
      modifier = Modifier
        .padding(horizontal = 12.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Text(
        getVolumeSliderText(displayVol, 100 + displayBoost, displayBoost, percentage, displayAsPercentage),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
      )
      VerticalSlider(
        if (displayAsPercentage) percentage else displayVol,
        if (displayAsPercentage) 0..100 else range,
        overflowValue = displayBoost,
        overflowRange = boostRange,
        isActive = isActive || isDragging
      )
      Icon(
        when (percentage) {
          0 -> Icons.AutoMirrored.Default.VolumeOff
          in 0..30 -> Icons.AutoMirrored.Default.VolumeMute
          in 30..60 -> Icons.AutoMirrored.Default.VolumeDown
          in 60..100 -> Icons.AutoMirrored.Default.VolumeUp
          else -> Icons.AutoMirrored.Default.VolumeOff
        },
        contentDescription = null,
      )
    }
  }
}

val getVolumeSliderText: @Composable (Int, Int, Int, Int, Boolean) -> String =
  { volume, mpvVolume, boostVolume, percentage, displayAsPercentage ->
    when {
      mpvVolume == 100 ->
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }

      mpvVolume > 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      mpvVolume < 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      else -> {
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }
      }
    }
  }
