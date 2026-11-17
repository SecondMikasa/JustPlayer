package xyz.mpv.rex.ui.player.controls
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.DisposableEffect
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import xyz.mpv.rex.preferences.MusicVisualizerStyle
import xyz.mpv.rex.utils.media.AudioAmplitudeTracker
import xyz.mpv.rex.utils.media.AudioAnalyzer
import xyz.mpv.rex.utils.media.BandState
import xyz.mpv.rex.utils.media.SPECTRUM_BINS
import xyz.mpv.rex.utils.media.SPECTROGRAM_COLS
import xyz.mpv.rex.utils.media.rememberBandState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Audiotrack
import xyz.mpv.rex.preferences.PlayerButton
import xyz.mpv.rex.R
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import `is`.xyz.mpv.MPVLib
import xyz.mpv.rex.ui.player.controls.components.VolumeSlider
import xyz.mpv.rex.ui.player.controls.components.BrightnessSlider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import org.koin.compose.koinInject
import xyz.mpv.rex.preferences.AudioPreferences
import xyz.mpv.rex.preferences.AppearancePreferences
import xyz.mpv.rex.preferences.PlayerPreferences
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.preferences.preference.deleteAndGet
import xyz.mpv.rex.preferences.preference.minusAssign
import xyz.mpv.rex.preferences.preference.plusAssign
import xyz.mpv.rex.ui.player.Decoder
import xyz.mpv.rex.ui.player.EqualizerPreset
import xyz.mpv.rex.ui.player.EqualizerState
import xyz.mpv.rex.ui.player.Panels
import xyz.mpv.rex.ui.player.PlayerActivity
import xyz.mpv.rex.ui.player.PlayerViewModel
import xyz.mpv.rex.ui.player.RepeatMode
import xyz.mpv.rex.ui.player.ResumePlaybackOverlay
import xyz.mpv.rex.ui.player.Sheets
import xyz.mpv.rex.ui.player.controls.components.SeekbarWithTimers
import xyz.mpv.rex.ui.player.controls.components.sheets.PlaylistItem
import xyz.mpv.rex.ui.player.controls.components.sheets.toFixed
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.ui.browser.dialogs.AddToPlaylistDialog
import xyz.mpv.rex.ui.theme.spacing
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Now Playing screen for audio-only media.
 *
 * UI matches the provided screenshots:
 *  ┌─────────────────────────────────┐
 *  │  ‹  │  Now Playing  │  ⓘ        │
 *  │                                 │
 *  │   [Cover art / wireframe sphere]│
 *  │                                 │
 *  │  Title                          │
 *  │  Artist                         │
 *  │  Track N/M  · ⏱1.00x · A·B  ≡+ │
 *  │  ───── seekbar ──────────────── │
 *  │  |◄   ◄◄   ▶   ▶▶   ►|         │
 *  │  [  EQ  shuf  rpt  spd  ⏱  ≡ ] │
 *  └─────────────────────────────────┘
 */
@OptIn(
  ExperimentalAnimationGraphicsApi::class,
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class,
  ExperimentalFoundationApi::class,
)
@Composable
fun AudioPlayerControls(
  viewModel: PlayerViewModel,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activity = LocalActivity.current as PlayerActivity
  val spacing = MaterialTheme.spacing

  // ── Playback state ───────────────────────────────────────────────────────
  val paused       by MPVLib.propBoolean["pause"].collectAsState()
  val mpvVolume    by MPVLib.propInt["volume"].collectAsState()
  val mpvMute      by MPVLib.propBoolean["mute"].collectAsState()
  val isSilent     = (mpvVolume ?: 100) <= 0 || mpvMute == true
  val duration     by MPVLib.propInt["duration"].collectAsState()
  val position     by MPVLib.propInt["time-pos"].collectAsState()
  val playbackSpeed by MPVLib.propFloat["speed"].collectAsState()

  // ── Metadata ─────────────────────────────────────────────────────────────
  val currentPath     by MPVLib.propString["path"].collectAsState()
  val rawArtist       by MPVLib.propString["metadata/by-key/artist"].collectAsState()
  val rawAlbumArtist  by MPVLib.propString["metadata/by-key/album_artist"].collectAsState()
  val mediaTitle = activity.getTitleForControls()
  
  var activeArtist by remember { mutableStateOf<String?>(null) }
  
  LaunchedEffect(currentPath, rawArtist, rawAlbumArtist) {
    val directArtist = runCatching { MPVLib.getPropertyString("metadata/by-key/artist") }.getOrNull()
    val directAlbumArtist = runCatching { MPVLib.getPropertyString("metadata/by-key/album_artist") }.getOrNull()
    
    activeArtist = directArtist?.takeIf { it.isNotBlank() }
      ?: directAlbumArtist?.takeIf { it.isNotBlank() }
  }
  
  val artist = activeArtist ?: "Unknown Artist"
  val artwork by viewModel.currentThumbnail.collectAsState()
  val currentArtwork = artwork
  val artworkResolved = true // extracted eagerly in VM/Activity

  // ── Playlist ─────────────────────────────────────────────────────────────
  val hasPlaylist  = viewModel.hasPlaylistSupport()
  val hasNext      = hasPlaylist && viewModel.hasNext()
  val hasPrevious  = hasPlaylist && viewModel.hasPrevious()
  val playlistInfo = if (hasPlaylist) viewModel.getPlaylistInfo() else null

  // ── Repeat / Shuffle ─────────────────────────────────────────────────────
  val repeatMode     by viewModel.repeatMode.collectAsState()
  val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()

  // ── Equalizer ─────────────────────────────────────────────────────────────
  val equalizerState by viewModel.equalizerState.collectAsState()

  // ── A-B Loop ─────────────────────────────────────────────────────────────
  val abLoopA          by viewModel.abLoopA.collectAsState()
  val abLoopB          by viewModel.abLoopB.collectAsState()
  val isABLoopExpanded by viewModel.isABLoopExpanded.collectAsState()
  val abLoopActive = abLoopA != null || abLoopB != null || isABLoopExpanded

  // ── Preferences ──────────────────────────────────────────────────────────
  val playerPrefs      = koinInject<PlayerPreferences>()
  val audioPrefs       = koinInject<AudioPreferences>()
  val appearancePrefs  = koinInject<AppearancePreferences>()
  val seekbarStyle     by appearancePrefs.seekbarStyle.collectAsState()
  val invertDuration   by playerPrefs.invertDuration.collectAsState()
  val customSkipDuration by playerPrefs.customSkipDuration.collectAsState()
  val speedPresets     by playerPrefs.speedPresets.collectAsState()
  val audioBottomControlsPref by appearancePrefs.audioBottomControls.collectAsState()
  val audioBottomButtons = remember(audioBottomControlsPref) {
    appearancePrefs.parseButtons(audioBottomControlsPref, mutableSetOf())
  }

  val visualizerStyle  by audioPrefs.musicPlaceholderVisualizer.collectAsState()
  val audioReactive    by audioPrefs.audioReactiveVisualizer.collectAsState()
  val context = LocalContext.current
  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  // If the user already has mic permission granted but the reactive visualizer preference hasn't
  // been explicitly set yet, turn it on by default. If the user denies or revokes mic permission,
  // ensure reactive mode stays off.
  LaunchedEffect(hasMicPermission) {
    if (hasMicPermission && !audioPrefs.audioReactiveVisualizer.isSet()) {
      audioPrefs.audioReactiveVisualizer.set(true)
    } else if (!hasMicPermission && audioReactive) {
      audioPrefs.audioReactiveVisualizer.set(false)
    }
  }

  // Re-check on resume so granting or revoking permission from system settings takes effect
  // without leaving the screen.
  DisposableEffect(activity) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        val granted =
          ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasMicPermission = granted
        if (granted && !audioPrefs.audioReactiveVisualizer.isSet()) {
          audioPrefs.audioReactiveVisualizer.set(true)
        } else if (!granted && audioReactive) {
          audioPrefs.audioReactiveVisualizer.set(false)
        }
      }
    }
    activity.lifecycle.addObserver(observer)
    onDispose { activity.lifecycle.removeObserver(observer) }
  }

  // Single frame-clock loop that turns raw analysis into bass / mid / treble / beat / spectrum.
  val bandState = rememberBandState(paused = paused == true, silent = isSilent)

  // Audio source selection. Preferred: Visualizer on the output mix (real FFT of the playing
  // audio, low latency, no room noise). Fallback: microphone amplitude. If neither is
  // available the visualizers fall back to their synthetic idle animation.
  LaunchedEffect(paused, audioReactive, hasMicPermission, isSilent) {
    if (paused == true || !audioReactive || !hasMicPermission || isSilent) {
      AudioAnalyzer.stopAll()
      return@LaunchedEffect
    }
    var trackerJob: Job? = null
    try {
      var useMic = !AudioAnalyzer.startVisualizer()
      if (!useMic) {
        // Some devices/OS versions accept the Visualizer but deliver only silence.
        delay(1200L)
        if (!AudioAnalyzer.visualizerSignalSeen) {
          AudioAnalyzer.stopVisualizer()
          useMic = true
        }
      }
      if (useMic) {
        AudioAnalyzer.setMicActive()
        trackerJob = launch { AudioAmplitudeTracker.startListening() }
      }
      awaitCancellation()
    } finally {
      trackerJob?.cancel()
      AudioAnalyzer.stopAll()
    }
  }

  // ── Visualizer Toggle State ───────────────────────────────────────────────
  var showVisualizerOverArt by remember { mutableStateOf(false) }
  val displayVisualizer = showVisualizerOverArt

  // ── Cover Art Animation ──────────────────────────────────────────────────
  val isPaused = paused == true
  val artScale by animateFloatAsState(
    targetValue = if (isPaused) 0.94f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "artScale",
  )

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

  // Visualizers that require a rectangular card container to avoid clipping:
  // - Mirrored EQ Bars (horizontal linear spectrum)
  // - Spectrogram Scroll (rectangular time x frequency waterfall)
  // All other styles (Radial Spectrum, Oscilloscope Ring) and cover art (with or without art)
  // use a circular container.
  val isRectangularVisualizer = displayVisualizer && (
    visualizerStyle == MusicVisualizerStyle.MIRRORED_EQ_BARS ||
    visualizerStyle == MusicVisualizerStyle.SPECTROGRAM_SCROLL
  )

  val circleCornerRadius = if (isLandscape) {
    (configuration.screenHeightDp.dp * 0.85f) / 2f
  } else {
    (configuration.screenWidthDp.dp * 0.92f) / 2f
  }

  // Circular vinyl disc container for cover art; rectangular when special visualizers active
  val targetCornerRadius = if (isRectangularVisualizer) {
    if (isPaused) 22.dp else 28.dp
  } else {
    circleCornerRadius
  }

  val artCornerRadius by animateDpAsState(
    targetValue = targetCornerRadius,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "artCornerRadius",
  )
  val artElevation by animateDpAsState(
    targetValue = if (isPaused) 6.dp else 14.dp,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "artElevation",
  )

  // Vinyl disc rotation — continuously spins while playing, freezes in place when paused
  val vinylRotation = remember { Animatable(0f) }
  LaunchedEffect(isPaused) {
    if (!isPaused) {
      while (isActive) {
        val currentAngle = vinylRotation.value
        vinylRotation.animateTo(
          targetValue = currentAngle + 360f,
          animationSpec = tween(durationMillis = 8000, easing = LinearEasing),
        )
        vinylRotation.snapTo(vinylRotation.value % 360f)
      }
    }
  }

  // ── Play/Pause Button Animation ──────────────────────────────────────────
  val playToPauseIcon = AnimatedImageVector.animatedVectorResource(R.drawable.anim_play_to_pause)
  val playButtonInteractionSource = remember { MutableInteractionSource() }
  val isPlayButtonPressed by playButtonInteractionSource.collectIsPressedAsState()
  val playButtonScale by animateFloatAsState(
    targetValue = if (isPlayButtonPressed) 0.90f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "playButtonScale",
  )

  // ── Sleep timer ───────────────────────────────────────────────────────────
  val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()

  // ── Sheet / panel state ──────────────────────────────────────────────────
  val sheetShown by viewModel.sheetShown.collectAsState()
  val panelShown by viewModel.panelShown.collectAsState()
  val resumeDialogState by viewModel.resumeDialogState.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val currentResumeState = resumeDialogState
  val noSheetOpen = sheetShown == Sheets.None && panelShown == Panels.None
  val overlayDurationSec = playerPrefs.resumeOverlayDuration.get()

  val onOpenSheet: (Sheets) -> Unit = { viewModel.sheetShown.value = it }
  val onOpenPanel: (Panels) -> Unit = { viewModel.panelShown.value = it }

  var showAudioProperties by remember { mutableStateOf(false) }
  var showAddToPlaylistDialog by remember { mutableStateOf(false) }

  val swipeDownModifier = if (noSheetOpen) {
    Modifier.pointerInput(Unit) {
      var totalDrag = 0f
      var triggered = false
      detectVerticalDragGestures(
        onDragStart = {
          totalDrag = 0f
          triggered = false
        },
        onDragEnd = {
          if (!triggered && totalDrag > 50f) {
            triggered = true
            onBackPress()
          }
          totalDrag = 0f
        },
        onDragCancel = {
          totalDrag = 0f
          triggered = false
        },
        onVerticalDrag = { change, dragAmount ->
          if (dragAmount > 0) {
            totalDrag += dragAmount
            if (!triggered && totalDrag > 80f) {
              triggered = true
              change.consume()
              onBackPress()
            }
          }
        }
      )
    }
  } else {
    Modifier
  }

  @Composable
  fun AudioBottomBar(modifier: Modifier = Modifier) {
    if (audioBottomButtons.isEmpty()) return

    Row(
      modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        .padding(vertical = 4.dp, horizontal = 4.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      audioBottomButtons.forEach { button ->
        when (button) {
          PlayerButton.EQUALIZER -> {
            ActionBarButton(
              icon = { Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.btn_label_equalizer)) },
              active = equalizerState.isEnabled,
              onClick = { onOpenSheet(Sheets.Equalizer) },
            )
          }
          PlayerButton.SHUFFLE -> {
            ActionBarButton(
              icon = {
                Icon(
                  if (shuffleEnabled) Icons.Filled.ShuffleOn else Icons.Filled.Shuffle,
                  contentDescription = stringResource(R.string.btn_label_shuffle),
                )
              },
              active = shuffleEnabled,
              onClick = { viewModel.toggleShuffle() },
            )
          }
          PlayerButton.REPEAT_MODE -> {
            ActionBarButton(
              icon = {
                Icon(
                  when (repeatMode) {
                    RepeatMode.OFF -> Icons.Filled.Repeat
                    RepeatMode.ONE -> Icons.Filled.RepeatOne
                    RepeatMode.ALL -> Icons.Filled.RepeatOn
                  },
                  contentDescription = stringResource(R.string.btn_label_repeat_mode),
                )
              },
              active = repeatMode != RepeatMode.OFF,
              onClick = { viewModel.cycleRepeatMode() },
            )
          }
          PlayerButton.SLEEP_TIMER -> {
            ActionBarButton(
              icon = { Icon(Icons.Filled.Timer, contentDescription = stringResource(R.string.btn_label_sleep_timer)) },
              active = sleepTimerTimeRemaining > 0,
              onClick = { onOpenSheet(Sheets.SleepTimer) },
            )
          }
          PlayerButton.VOLUME -> {
            val isMuted by MPVLib.propBoolean["mute"].collectAsState()
            ActionBarButton(
              icon = {
                Icon(
                  if (isMuted == true) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = stringResource(R.string.btn_label_volume),
                )
              },
              active = isMuted == true,
              onClick = { viewModel.toggleMute() },
              onLongClick = { viewModel.displayVolumeSlider() },
            )
          }
          PlayerButton.PLAYBACK_SPEED -> {
            ActionBarButton(
              icon = { Icon(Icons.Filled.Speed, contentDescription = stringResource(R.string.btn_label_speed)) },
              active = (playbackSpeed ?: 1f) != 1f,
              onClick = {
                val cur = playbackSpeed ?: 1f
                val next = if (cur >= 2f) 0.25f else cur + 0.25f
                MPVLib.setPropertyFloat("speed", next.toFixed(2))
              },
              onLongClick = { onOpenSheet(Sheets.PlaybackSpeed) },
            )
          }
          PlayerButton.AB_LOOP -> {
            ActionBarButton(
              icon = {
                Text(
                  text = "AB",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
              },
              active = abLoopActive,
              onClick = { viewModel.toggleABLoopExpanded() },
            )
          }
          PlayerButton.CUSTOM_SKIP -> {
            ActionBarButton(
              icon = { Icon(Icons.Filled.FastForward, contentDescription = stringResource(R.string.btn_label_custom_skip)) },
              active = false,
              onClick = { viewModel.seekBy(customSkipDuration) },
              onLongClick = { onOpenSheet(Sheets.CustomSkipDuration) },
            )
          }
          PlayerButton.CUSTOM_SKIP_BACK -> {
            ActionBarButton(
              icon = { Icon(Icons.Filled.FastRewind, contentDescription = stringResource(R.string.btn_label_custom_skip_back)) },
              active = false,
              onClick = { viewModel.seekBy(-customSkipDuration) },
              onLongClick = { onOpenSheet(Sheets.CustomSkipDuration) },
            )
          }
          PlayerButton.AUDIO_TRACK -> {
            ActionBarButton(
              icon = { Icon(Icons.Filled.Audiotrack, contentDescription = stringResource(R.string.btn_label_audio)) },
              active = false,
              onClick = { onOpenSheet(Sheets.AudioTracks) },
            )
          }
          PlayerButton.MORE_OPTIONS -> {
            ActionBarButton(
              icon = { Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.btn_label_more)) },
              active = false,
              onClick = { showAudioProperties = true },
            )
          }
          PlayerButton.BOOKMARKS_CHAPTERS, PlayerButton.CURRENT_CHAPTER -> {
            ActionBarButton(
              icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.btn_label_bookmarks)) },
              active = false,
              onClick = { onOpenSheet(Sheets.Playlist) },
            )
          }
          else -> {
            ActionBarButton(
              icon = { Icon(button.icon, contentDescription = null) },
              active = false,
              onClick = {},
            )
          }
        }
      }
    }
  }

  // Show the system status bar and navigation bar while the audio player is visible,
  // and restore the video-player fullscreen state when it is disposed.
  val view = LocalView.current
  val backgroundColor = MaterialTheme.colorScheme.background
  val isLight = backgroundColor.luminance() > 0.5f

  DisposableEffect(view, activity, isLight) {
    val window = activity.window
    if (window != null) {
      // Drop FLAG_LAYOUT_NO_LIMITS so the system can push insets into our layout when the
      // notification shade is pulled down (status bar expanding moves the UI downward).
      // Keep edge-to-edge (setDecorFitsSystemWindows = false) so our own padding modifiers
      // still control the insets; only remove the flag that pins the window at no-limit size.
      window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
      val controller = WindowCompat.getInsetsController(window, view)
      controller.show(WindowInsetsCompat.Type.systemBars())
      // BEHAVIOR_DEFAULT: status bar responds normally to swipe gestures and the
      // notification shade expands on pull, reflowing the layout via statusBarsPadding().
      controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
      controller.isAppearanceLightStatusBars = isLight
      controller.isAppearanceLightNavigationBars = isLight
    }
    onDispose {
      // Restore fullscreen / hide bars when switching back to video player
      if (window != null) {
        // Re-add FLAG_LAYOUT_NO_LIMITS for the video player's immersive mode
        window.setFlags(
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
        val controller = WindowCompat.getInsetsController(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
          WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
      }
    }
  }

  LaunchedEffect(hasPlaylist) {
    if (hasPlaylist) viewModel.refreshPlaylistItems()
  }

  // Keep playlist fresh whenever the sheet is open
  LaunchedEffect(sheetShown) {
    if (sheetShown == Sheets.Playlist) viewModel.refreshPlaylistItems()
  }

  // ── Root ─────────────────────────────────────────────────────────────────
  CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
    ) {

    // Blurred album-art backdrop (only when artwork exists)
    if (currentArtwork != null && !currentArtwork.isRecycled) {
      Image(
        bitmap = remember(currentArtwork) { currentArtwork.asImageBitmap() },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().blur(60.dp),
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
      )
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f),
                Color.Transparent,
              ),
              center = Offset(Float.POSITIVE_INFINITY, 0f),
            )
          ),
      )
    }

    if (isLandscape) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = spacing.medium, vertical = spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // ── Left Side: Album Art / Wireframe Sphere ──
        val haptic = LocalHapticFeedback.current
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight(0.85f)
              .aspectRatio(1f)
              .graphicsLayer {
                scaleX = artScale
                scaleY = artScale
                rotationZ = if (!displayVisualizer && !isRectangularVisualizer) vinylRotation.value else 0f
                this.shadowElevation = artElevation.toPx()
                this.shape = RoundedCornerShape(artCornerRadius)
                this.clip = true
              }
              .clip(RoundedCornerShape(artCornerRadius))
              .then(swipeDownModifier),
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .then(swipeDownModifier)
                .combinedClickable(
                  onClick = {},
                  onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showVisualizerOverArt = !showVisualizerOverArt
                  },
                ),
            ) {
              if (displayVisualizer) {
                if (artworkResolved) {
                  AnimatedMusicBlob(
                    paused = paused == true,
                    style = visualizerStyle,
                    bands = bandState,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              } else {
                AudioCoverArt(currentArtwork)
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(spacing.medium))

        // ── Right Side: Controls ──
        Column(
          modifier = Modifier
            .weight(1.25f)
            .fillMaxHeight()
            .padding(horizontal = spacing.small, vertical = spacing.extraSmall),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // Top bar: Back, Now Playing, Info
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .then(swipeDownModifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(onClick = onBackPress) {
              Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
              )
            }
            Text(
              text = "Now Playing",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = { showAudioProperties = true }) {
              Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "File info",
                tint = MaterialTheme.colorScheme.onBackground,
              )
            }
          }

          if (currentResumeState != null && noSheetOpen && !isLoading) {
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = Alignment.Center,
            ) {
              ResumePlaybackOverlay(
                positionText = currentResumeState.formattedTime,
                durationMs = overlayDurationSec * 1000L,
                onRestart = {
                  viewModel.dismissResumeDialog()
                  viewModel.seekTo(0)
                  MPVLib.setPropertyInt("time-pos", 0)
                },
                onAutoDismiss = {
                  viewModel.dismissResumeDialog()
                },
              )
            }
          }

          // Title & artist
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = mediaTitle,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (!artist.isNullOrBlank()) {
              Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          // Info row: Track N/M · ⏱1.00x · A·B  |  Add to playlist
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              if (hasPlaylist || playlistInfo != null) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onOpenSheet(Sheets.Playlist) },
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                      contentDescription = "Open queue",
                      tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                      modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (playlistInfo != null) "Track $playlistInfo" else "Queue",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    )
                  }
                }
                InfoRowDot()
              }
              // Speed chip
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                  .clip(CircleShape)
                  .combinedClickable(
                    onClick = {
                      val cur = playbackSpeed ?: 1f
                      val next = if (cur >= 2f) 0.25f else cur + 0.25f
                      MPVLib.setPropertyFloat("speed", next.toFixed(2))
                    },
                    onLongClick = { onOpenSheet(Sheets.PlaybackSpeed) },
                  ),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    modifier = Modifier.size(13.dp),
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "${(playbackSpeed ?: 1f).toFixed(2)}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                  )
                }
              }
              InfoRowDot()
              // A-B chip
              Surface(
                shape = CircleShape,
                color = if (abLoopActive)
                  MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                else
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(CircleShape).clickable { viewModel.toggleABLoopExpanded() },
              ) {
                Text(
                  text = "A·B",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = if (abLoopActive)
                    MaterialTheme.colorScheme.onTertiaryContainer
                  else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
            IconButton(
              onClick = { showAddToPlaylistDialog = true },
              modifier = Modifier.size(32.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                contentDescription = "Add to playlist",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
              )
            }
          }

          // Seekbar
          SeekbarWithTimers(
            position            = { (position ?: 0).toFloat() },
            duration            = (duration ?: 0).toFloat(),
            onValueChange       = { viewModel.seekTo(it.toInt()) },
            onValueChangeFinished = {},
            timersInverted      = Pair(false, invertDuration),
            positionTimerOnClick = {},
            durationTimerOnCLick = { playerPrefs.invertDuration.set(!invertDuration) },
            chapters            = persistentListOf(),
            paused              = paused == true,
            seekbarStyle        = seekbarStyle,
            timerColor          = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            timersBelow         = true,
            modifier            = Modifier.fillMaxWidth(),
          )

          // Transport: |◄  ◄◄  ▶  ▶▶  ►|
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(onClick = { viewModel.playPrevious() }, enabled = hasPrevious) {
              Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = if (hasPrevious) MaterialTheme.colorScheme.onBackground
                       else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(30.dp),
              )
            }
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .combinedClickable(
                  onClick = { viewModel.seekBy(-customSkipDuration) },
                  onLongClick = { onOpenSheet(Sheets.CustomSkipDuration) },
                ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.FastRewind,
                contentDescription = "Rewind",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp),
              )
            }
            Surface(
              onClick = { viewModel.pauseUnpause() },
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .size(58.dp)
                .graphicsLayer {
                  scaleX = playButtonScale
                  scaleY = playButtonScale
                },
              interactionSource = playButtonInteractionSource,
            ) {
              Box(contentAlignment = Alignment.Center) {
                Image(
                  painter = rememberAnimatedVectorPainter(playToPauseIcon, atEnd = paused == false),
                  contentDescription = if (paused == true) "Play" else "Pause",
                  modifier = Modifier.size(32.dp),
                  colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                )
              }
            }
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .combinedClickable(
                  onClick = { viewModel.seekBy(customSkipDuration) },
                  onLongClick = { onOpenSheet(Sheets.CustomSkipDuration) },
                ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.FastForward,
                contentDescription = "Forward",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp),
              )
            }
            IconButton(onClick = { viewModel.playNext() }, enabled = hasNext) {
              Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = if (hasNext) MaterialTheme.colorScheme.onBackground
                       else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(30.dp),
              )
            }
          }

          // Bottom action bar
          AudioBottomBar()
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(
            if (isPortrait) WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
            else WindowInsets.statusBars
          )
          .navigationBarsPadding()
          .padding(horizontal = spacing.large, vertical = spacing.small),
      ) {

        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .then(swipeDownModifier),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = onBackPress) {
            Icon(
              imageVector = Icons.Filled.KeyboardArrowDown,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }
          Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          IconButton(onClick = { showAudioProperties = true }) {
            Icon(
              imageVector = Icons.Filled.Info,
              contentDescription = "File info",
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }
        }

        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .then(swipeDownModifier)
        )

        // ── Cover art / animated wireframe sphere ────────────────────────────
        val haptic = LocalHapticFeedback.current
        Surface(
          modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1f)
            .align(Alignment.CenterHorizontally)
            .graphicsLayer {
              scaleX = artScale
              scaleY = artScale
              rotationZ = if (!displayVisualizer && !isRectangularVisualizer) vinylRotation.value else 0f
              this.shadowElevation = artElevation.toPx()
              this.shape = RoundedCornerShape(artCornerRadius)
              this.clip = true
            }
            .clip(RoundedCornerShape(artCornerRadius))
            .then(swipeDownModifier),
          shape = RoundedCornerShape(artCornerRadius),
          shadowElevation = artElevation,
          color = Color.Transparent,
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .then(swipeDownModifier)
              .combinedClickable(
                onClick = {},
                onLongClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  showVisualizerOverArt = !showVisualizerOverArt
                },
              ),
          ) {
            if (displayVisualizer) {
              if (artworkResolved) {
                AnimatedMusicBlob(
                  paused = paused == true,
                  style = visualizerStyle,
                  bands = bandState,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            } else {
              AudioCoverArt(currentArtwork)
            }
          }
        }

        // ── Resume playback overlay positioned between art and controls ──────
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center,
        ) {
          if (currentResumeState != null && noSheetOpen && !isLoading) {
            ResumePlaybackOverlay(
              positionText = currentResumeState.formattedTime,
              durationMs = overlayDurationSec * 1000L,
              onRestart = {
                viewModel.dismissResumeDialog()
                viewModel.seekTo(0)
                MPVLib.setPropertyInt("time-pos", 0)
              },
              onAutoDismiss = {
                viewModel.dismissResumeDialog()
              },
            )
          }
        }

        // ── Title / artist ───────────────────────────────────────────────────
        Text(
          text = mediaTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (!artist.isNullOrBlank()) {
          Text(
            text = artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.height(spacing.small))

        // ── Info row: Track N/M · ⏱1.00x · A·B  |  Add to playlist ─────────────────────────────
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (hasPlaylist || playlistInfo != null) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                  .clip(CircleShape)
                  .clickable { onOpenSheet(Sheets.Playlist) },
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Open queue",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    modifier = Modifier.size(13.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (playlistInfo != null) "Track $playlistInfo" else "Queue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                  )
                }
              }
              InfoRowDot()
            }
            // Speed chip — tap cycles, long-press opens full speed sheet
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier
                .clip(CircleShape)
                .combinedClickable(
                  onClick = {
                    val cur = playbackSpeed ?: 1f
                    val next = if (cur >= 2f) 0.25f else cur + 0.25f
                    MPVLib.setPropertyFloat("speed", next.toFixed(2))
                  },
                  onLongClick = { onOpenSheet(Sheets.PlaybackSpeed) },
                ),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Speed,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                  modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "${(playbackSpeed ?: 1f).toFixed(2)}x",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
              }
            }
            InfoRowDot()
            // A-B chip
            Surface(
              shape = CircleShape,
              color = if (abLoopActive)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
              else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.clip(CircleShape).clickable { viewModel.toggleABLoopExpanded() },
            ) {
              Text(
                text = "A·B",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (abLoopActive)
                  MaterialTheme.colorScheme.onTertiaryContainer
                else
                  MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
          IconButton(
            onClick = { showAddToPlaylistDialog = true },
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
              contentDescription = "Add to playlist",
              tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
          }
        }

        Spacer(modifier = Modifier.height(spacing.small))

        // ── Seekbar (full width + timestamps below at ends) ───────────────
        SeekbarWithTimers(
          position            = { (position ?: 0).toFloat() },
          duration            = (duration ?: 0).toFloat(),
          onValueChange       = { viewModel.seekTo(it.toInt()) },
          onValueChangeFinished = {},
          timersInverted      = Pair(false, invertDuration),
          positionTimerOnClick = {},
          durationTimerOnCLick = { playerPrefs.invertDuration.set(!invertDuration) },
          chapters            = persistentListOf(),
          paused              = paused == true,
          seekbarStyle        = seekbarStyle,
          timerColor          = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
          timersBelow         = true,
          modifier            = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // ── Transport: |◄  ◄◄  ▶  ▶▶  ►| ────────────────────────────────────
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = { viewModel.playPrevious() }, enabled = hasPrevious) {
            Icon(
              imageVector = Icons.Filled.SkipPrevious,
              contentDescription = "Previous",
              tint = if (hasPrevious) MaterialTheme.colorScheme.onBackground
                     else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
              modifier = Modifier.size(32.dp),
            )
          }
          // Rewind: single tap seeks back, long-press opens skip-duration picker
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .combinedClickable(
                onClick = { viewModel.seekBy(-customSkipDuration) },
                onLongClick = { onOpenSheet(Sheets.CustomSkipDuration) },
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.FastRewind,
              contentDescription = "Rewind",
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(30.dp),
            )
          }
          // Large play/pause with morphing vector animation and bounce
          Surface(
            onClick = { viewModel.pauseUnpause() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .size(68.dp)
              .graphicsLayer {
                scaleX = playButtonScale
                scaleY = playButtonScale
              },
            interactionSource = playButtonInteractionSource,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Image(
                painter = rememberAnimatedVectorPainter(playToPauseIcon, atEnd = paused == false),
                contentDescription = if (paused == true) "Play" else "Pause",
                modifier = Modifier.size(36.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
              )
            }
          }
          // Forward: single tap seeks forward, long-press opens skip-duration picker
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .combinedClickable(
                onClick = { viewModel.seekBy(customSkipDuration) },
                onLongClick = { onOpenSheet(Sheets.CustomSkipDuration) },
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.FastForward,
              contentDescription = "Forward",
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(30.dp),
            )
          }
          IconButton(onClick = { viewModel.playNext() }, enabled = hasNext) {
            Icon(
              imageVector = Icons.Filled.SkipNext,
              contentDescription = "Next",
              tint = if (hasNext) MaterialTheme.colorScheme.onBackground
                     else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
              modifier = Modifier.size(32.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // ── Bottom action bar ─────────────────────────────────────────────────
        AudioBottomBar()

        Spacer(modifier = Modifier.height(spacing.small))
      } // end Column
    }

    // ── Volume Slider ───────────────────────────────────────────────────
    val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
    val volume by viewModel.currentVolume.collectAsState()
    val mpvVolume by MPVLib.propInt["volume"].collectAsState()
    val volumeSliderTimestamp by viewModel.volumeSliderTimestamp.collectAsState()
    // True while the user's finger is on the slider — used to pause the auto-hide timer.
    var isVolumeSliderInteracting by remember { mutableStateOf(false) }

    // Auto-hide: 3 s after the last interaction ends. The timer resets whenever
    // volumeSliderTimestamp changes (new long-press) and is suspended while the user drags.
    LaunchedEffect(volumeSliderTimestamp, isVolumeSliderInteracting) {
        if (isVolumeSliderShown && volumeSliderTimestamp > 0 && !isVolumeSliderInteracting) {
            delay(3000L)
            viewModel.isVolumeSliderShown.update { false }
        }
    }

    AnimatedVisibility(
        isVolumeSliderShown,
        enter = fadeIn() + slideInHorizontally { it },
        exit = fadeOut() + slideOutHorizontally { it },
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = spacing.extraLarge)
    ) {
        val boostCap by audioPrefs.volumeBoostCap.collectAsState()
        val displayVolumeAsPercentage by playerPrefs.displayVolumeAsPercentage.collectAsState()
        
        val currentBoost = (mpvVolume ?: 100) - 100
        val showBoost = boostCap > 0 || currentBoost > 0
        val effBoostCap = maxOf(boostCap, currentBoost)
        
        VolumeSlider(
            volume,
            mpvVolume = mpvVolume ?: 100,
            range = 0..viewModel.maxVolume,
            boostRange = if (showBoost) 0..effBoostCap else null,
            displayAsPercentage = displayVolumeAsPercentage,
            isActive = isVolumeSliderInteracting,
            onVolumeChange = { newVol -> viewModel.changeVolumeTo(newVol) },
            onBoostVolumeChange = { newBoost -> viewModel.changeMPVVolumeTo(100 + newBoost) },
            onInteractionChange = { active ->
                isVolumeSliderInteracting = active
                // Reset the auto-hide countdown each time the user starts a new drag.
                if (active) viewModel.displayVolumeSlider()
            },
        )
    }

    // ── A-B Loop floating panel ───────────────────────────────────────────
    AnimatedVisibility(
      visible = isABLoopExpanded,
      enter = fadeIn(tween(200)) + slideInHorizontally { it },
      exit  = fadeOut(tween(200)) + slideOutHorizontally { it },
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = spacing.large),
    ) {
      val btnSize = 40.dp
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.height(btnSize),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 4.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = if (abLoopA != null) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
            modifier = Modifier
              .height(btnSize - 4.dp).widthIn(min = btnSize - 4.dp)
              .clip(CircleShape).clickable { viewModel.setLoopA() },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = if (abLoopA != null) viewModel.formatTimestamp(abLoopA!!) else "A",
                style = MaterialTheme.typography.labelLarge,
                color = if (abLoopA != null) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = if (abLoopA != null) 8.dp else 0.dp),
              )
            }
          }
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
              .size(btnSize - 4.dp).clip(CircleShape)
              .clickable { viewModel.clearABLoop(); viewModel.toggleABLoopExpanded() },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Filled.Close, "Clear loop",
                tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
            }
          }
          Surface(
            shape = CircleShape,
            color = if (abLoopB != null) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
            modifier = Modifier
              .height(btnSize - 4.dp).widthIn(min = btnSize - 4.dp)
              .clip(CircleShape).clickable { viewModel.setLoopB() },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = if (abLoopB != null) viewModel.formatTimestamp(abLoopB!!) else "B",
                style = MaterialTheme.typography.labelLarge,
                color = if (abLoopB != null) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = if (abLoopB != null) 8.dp else 0.dp),
              )
            }
          }
        }
      }
    }
  } // end root Box

  // ── Shared sheets (speed, playlist, sleep-timer, etc.) ────────────────────
  PlayerSheets(
    viewModel            = viewModel,
    sheetShown           = sheetShown,
    subtitles            = persistentListOf(),
    onAddSubtitle        = {},
    onToggleSubtitle     = {},
    isSubtitleSelected   = { false },
    onRemoveSubtitle     = {},
    audioTracks          = viewModel.audioTracks.collectAsState(persistentListOf()).value.toImmutableList(),
    onAddAudio           = viewModel::addAudio,
    onSelectAudio        = { viewModel.selectAudioTrack(it.id, it.title, it.lang) },
    chapter              = null,
    chapters             = persistentListOf(),
    onSeekToChapter      = {},
    decoder              = Decoder.AutoCopy,
    onUpdateDecoder      = {},
    speed                = playbackSpeed?.toFixed(2) ?: playerPrefs.defaultSpeed.get(),
    onSpeedChange        = { MPVLib.setPropertyFloat("speed", it.toFixed(2)) },
    onMakeDefaultSpeed   = { playerPrefs.defaultSpeed.set(it.toFixed(2)) },
    onAddSpeedPreset     = { playerPrefs.speedPresets += it.toFixed(2).toString() },
    onRemoveSpeedPreset  = { playerPrefs.speedPresets -= it.toFixed(2).toString() },
    onResetSpeedPresets  = playerPrefs.speedPresets::delete,
    speedPresets         = speedPresets.map { it.toFloat() }.sorted(),
    onResetDefaultSpeed  = {
      MPVLib.setPropertyFloat("speed", playerPrefs.defaultSpeed.deleteAndGet().toFixed(2))
    },
    sleepTimerTimeRemaining = sleepTimerTimeRemaining,
    onStartSleepTimer    = viewModel::startTimer,
    onOpenPanel          = onOpenPanel,
    onShowSheet          = onOpenSheet,
    onDismissRequest     = { onOpenSheet(Sheets.None) },
  )

  // Panels (EQ / audio delay)
  PlayerPanels(
    panelShown      = panelShown,
    onDismissRequest = { onOpenPanel(Panels.None) },
  )

  // ── Full-screen loading overlay to prevent thumbnail/cover flash ───────
  val isLoading by viewModel.isLoading.collectAsState()
  val isSpinnerVisible by viewModel.isSpinnerVisible.collectAsState()
  val showLoadingCircle by playerPrefs.showLoadingCircle.collectAsState()

  AnimatedVisibility(
    visible = isLoading,
    enter = EnterTransition.None,
    exit = fadeOut(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black),
      contentAlignment = Alignment.Center
    ) {
      if (showLoadingCircle && isSpinnerVisible) {
        LoadingIndicator(modifier = Modifier.size(96.dp))
      }
    }
  }

  // Audio Properties sheet
  if (showAudioProperties) {
    AudioPropertiesSheet(path = currentPath, onDismiss = { showAudioProperties = false })
  }

  // Add to Playlist dialog
  if (showAddToPlaylistDialog) {
    val resolvedPath = currentPath ?: ""
    val currentVideo = remember(resolvedPath, mediaTitle, activeArtist, duration) {
      val path = resolvedPath
      val uri = if (path.isNotBlank()) path.toUri() else android.net.Uri.EMPTY
      Video(
        id = 0L,
        title = mediaTitle,
        displayName = mediaTitle,
        path = path,
        uri = uri,
        duration = (duration ?: 0).toLong() * 1000L,
        durationFormatted = "",
        size = 0L,
        sizeFormatted = "",
        dateModified = 0L,
        dateAdded = 0L,
        mimeType = "audio/*",
        bucketId = "",
        bucketDisplayName = "",
        width = 0,
        height = 0,
        fps = 0f,
        resolution = "",
        isAudio = true,
        artist = activeArtist ?: "",
      )
    }
    AddToPlaylistDialog(
      isOpen = true,
      videos = listOf(currentVideo),
      onDismiss = { showAddToPlaylistDialog = false },
      onSuccess = { showAddToPlaylistDialog = false },
      isMusicContext = true,
    )
  }
}
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated Music Visualizer  — band-reactive visualizers for missing art
// ─────────────────────────────────────────────────────────────────────────────

private const val VIZ_POINTS = 180
private const val MAX_RADIUS_FRAC = 0.84f

/** Overall pulse scale: idle sine (shrinks when real audio drives it) + bass + beat kick. */
private fun blobPulse(bands: BandState, pausedScale: Float): Float {
  val idleAmp = (0.08f - 0.05f * bands.audioMix) * (1f - bands.audioMix)
  val raw = pausedScale * (1f + sin(bands.time * 2.4f) * idleAmp) +
    bands.bass * 0.28f + bands.beat * 0.16f
  return raw.coerceAtMost(1.0f)
}

private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
  val f = fraction.coerceIn(0f, 1f)
  return Color(
    red = c1.red + (c2.red - c1.red) * f,
    green = c1.green + (c2.green - c1.green) * f,
    blue = c1.blue + (c2.blue - c1.blue) * f,
    alpha = c1.alpha + (c2.alpha - c1.alpha) * f,
  )
}

/**
 * Blends a bar's base token color toward [onSurface].copy(0.6f) during quiet passages.
 * At [overall] == 1f (loud) the blend is 0 and the base color is returned unchanged.
 * At [overall] == 0f (silent) the blend is 0.25f, nudging the bar toward the surface foreground.
 */
private fun themedBarColor(base: Color, onSurface: Color, overall: Float): Color {
  val blend = (1f - overall) * 0.25f
  return Color(
    red   = base.red   + (onSurface.copy(alpha = 0.6f).red   - base.red)   * blend,
    green = base.green + (onSurface.copy(alpha = 0.6f).green - base.green) * blend,
    blue  = base.blue  + (onSurface.copy(alpha = 0.6f).blue  - base.blue)  * blend,
    alpha = base.alpha,
  )
}

/**
 * Modern Material 3 music visualizers:
 * 1. Radial spectrum: bars arranged in a circle around center core, peak-hold caps, pulsing core.
 * 2. Mirrored equalizer bars: classic bottom-up spectrum with falling peak dots.
 * 3. Oscilloscope ring: glowing ring following actual audio waveform.
 * 4. Scrolling history: spectrogram ribbon showing drops and builds over time.
 */
@Composable
fun AnimatedMusicBlob(
  paused: Boolean,
  bands: BandState,
  modifier: Modifier = Modifier,
  style: MusicVisualizerStyle = MusicVisualizerStyle.RADIAL_SPECTRUM,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
  val surface = MaterialTheme.colorScheme.surface
  val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
  val onSurface = MaterialTheme.colorScheme.onSurface

  val pausedScale by animateFloatAsState(
    targetValue = if (paused) 0.95f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "vizPausedScale",
  )

  // Persistent visualizer states across frames (draw phase)
  val radialPeaks = remember { FloatArray(64) }
  val radialPeakVels = remember { FloatArray(64) }

  val eqPeaks = remember { FloatArray(36) }
  val eqPeakVels = remember { FloatArray(36) }

  val oscPath = remember { Path() }

  Box(
    modifier = modifier
      .background(
        Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.12f + bands.bass * 0.08f),
            surface.copy(alpha = 0.95f),
          )
        )
      ),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Subscribe to per-frame updates (draw-phase only, no recomposition).
      if (bands.tick < 0L) return@Canvas

      val dt = bands.lastDt.coerceIn(0.001f, 0.05f)

      val bass = bands.bass
      val mid = bands.mid
      val treble = bands.treble
      val overall = bands.overall
      val beat = bands.beat
      val spectrum = bands.spectrum

      val cx = size.width / 2f
      val cy = size.height / 2f
      val minDimension = minOf(size.width, size.height)

      when (style) {
        MusicVisualizerStyle.RADIAL_SPECTRUM -> {
          // ── 1. Radial Spectrum: Circular bars radiating outward all around, bass=bright,
          //    treble=dim, bars go from coreRadius outward, peak-hold caps. ──
          val alphaMix = 0.55f + 0.45f * bands.audioMix
          val corePulse = blobPulse(bands, pausedScale)
          val coreRadiusRaw = minDimension * 0.26f * corePulse
          val coreRadius = coreRadiusRaw.coerceAtMost((minDimension / 2f) * 0.50f)
          val numBars = 64
          val barGap = 4.dp.toPx()
          val minBarLen = 5.dp.toPx()
          val startDist = coreRadius + barGap
          val maxReach = minDimension * 0.40f
          val maxBarLen = (maxReach - startDist).coerceAtLeast(minDimension * 0.16f)
          val barWidth = 3.6.dp.toPx()
          val angleStep = (2f * PI.toFloat()) / numBars
          val halfBars = numBars / 2

          // Core ambient glow
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = (0.40f + bass * 0.35f + beat * 0.20f).coerceAtMost(1f) * alphaMix),
                tertiaryColor.copy(alpha = (0.20f + mid * 0.20f).coerceAtMost(1f) * alphaMix),
                Color.Transparent,
              ),
              center = Offset(cx, cy),
              radius = coreRadius * 1.55f,
            ),
            radius = coreRadius * 1.55f,
            center = Offset(cx, cy),
          )

          // Inner solid pulsing core
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = (0.92f + overall * 0.08f).coerceAtMost(1f) * alphaMix),
                tertiaryColor.copy(alpha = (0.82f + overall * 0.15f).coerceAtMost(1f) * alphaMix),
              ),
              center = Offset(cx - coreRadius * 0.15f, cy - coreRadius * 0.15f),
              radius = coreRadius * 1.35f,
            ),
            radius = coreRadius,
            center = Offset(cx, cy),
          )

          // Core accent ring
          drawCircle(
            color = onPrimaryColor.copy(alpha = (0.25f + beat * 0.25f) * alphaMix),
            radius = coreRadius * 0.76f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.8.dp.toPx()),
          )

          for (bar in 0 until numBars) {
            val mirrorIdx = if (bar < halfBars) bar else (numBars - 1 - bar)
            val binFrac = (mirrorIdx.toFloat() / (halfBars - 1f)) * (SPECTRUM_BINS - 1)
            val binLow = binFrac.toInt().coerceIn(0, SPECTRUM_BINS - 1)
            val binHigh = (binLow + 1).coerceIn(0, SPECTRUM_BINS - 1)
            val f = binFrac - binLow
            val rawMag = spectrum[binLow] * (1f - f) + spectrum[binHigh] * f

            // Non-linear perceptual scaling so bars extend visibly even on subtle passages
            val dynamicMag = (rawMag.pow(0.80f) * 1.12f).coerceIn(0f, 1f)
            val barLen = minBarLen + dynamicMag * (maxBarLen - minBarLen)

            // Peak-hold with smooth gravity-based fall
            if (barLen >= radialPeaks[bar]) {
              radialPeaks[bar] = barLen
              radialPeakVels[bar] = 0f
            } else {
              radialPeakVels[bar] += 200.dp.toPx() * dt
              // Cap so a single frame cannot overshoot below the floor
              val maxVel = (radialPeaks[bar] - minBarLen) / dt
              radialPeakVels[bar] = radialPeakVels[bar].coerceAtMost(maxVel.coerceAtLeast(0f))
              radialPeaks[bar] = (radialPeaks[bar] - radialPeakVels[bar] * dt).coerceAtLeast(minBarLen)
            }

            // Angle: bar 0 = top (−π/2), going clockwise
            val angle = -PI.toFloat() / 2f + bar * angleStep
            val cosA = cos(angle)
            val sinA = sin(angle)
            val halfCapWidth = barWidth / 2f
            val maxEndDist = (minDimension / 2f) * MAX_RADIUS_FRAC - halfCapWidth
            val endDist = (startDist + barLen).coerceAtMost(maxEndDist)
            val peakDist = (startDist + radialPeaks[bar] + 3.dp.toPx()).coerceAtMost(maxEndDist)

            val fraction = mirrorIdx.toFloat() / (halfBars - 1f)
            val baseBarColor = if (fraction < 0.5f) {
              lerpColor(primaryColor, secondaryColor, fraction * 2f)
            } else {
              lerpColor(secondaryColor, tertiaryColor, (fraction - 0.5f) * 2f)
            }
            val barColor = themedBarColor(baseBarColor, onSurface, overall)

            // Spectrum bar line
            drawLine(
              color = barColor.copy(alpha = (0.82f + dynamicMag * 0.18f) * alphaMix),
              start = Offset(cx + cosA * startDist, cy + sinA * startDist),
              end   = Offset(cx + cosA * endDist,   cy + sinA * endDist),
              strokeWidth = barWidth,
              cap = StrokeCap.Round,
            )

            // Peak-hold cap dot
            if (radialPeaks[bar] > minBarLen + 2.dp.toPx()) {
              drawCircle(
                color = barColor.copy(alpha = 0.95f * alphaMix),
                radius = barWidth * 0.55f,
                center = Offset(cx + cosA * peakDist, cy + sinA * peakDist),
              )
            }
          }
        }

        MusicVisualizerStyle.MIRRORED_EQ_BARS -> {
          // ── 2. Mirrored Equalizer Bars: Classic left-right symmetric spectrum, centered vertically.
          //    Bars grow upward from a centered baseline; reflection fades downward. ──
          val alphaMix = 0.55f + 0.45f * bands.audioMix
          val numBars = 36
          val totalWidth = minDimension * 0.82f
          val barW = (totalWidth / numBars) * 0.65f
          val gapW = (totalWidth / numBars) * 0.35f
          val startX = cx - totalWidth / 2f
          // Baseline sits slightly below center so bars have room to grow up
          val baselineY = cy + minDimension * 0.08f
          val maxBarHeight = minDimension * 0.38f
          val minBarHeight = 5.dp.toPx()

          // Ambient bloom behind the equalizer bars
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = (0.28f + bass * 0.30f + beat * 0.15f).coerceAtMost(0.70f) * alphaMix),
                tertiaryColor.copy(alpha = (0.12f + mid * 0.15f).coerceAtMost(0.40f) * alphaMix),
                Color.Transparent,
              ),
              center = Offset(cx, baselineY - minDimension * 0.12f),
              radius = minDimension * 0.45f,
            ),
            radius = minDimension * 0.45f,
            center = Offset(cx, baselineY - minDimension * 0.12f),
          )

          // Illuminated gradient baseline
          drawLine(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color.Transparent,
                primaryColor.copy(alpha = (0.70f + beat * 0.25f) * alphaMix),
                secondaryColor.copy(alpha = (0.85f + beat * 0.15f) * alphaMix),
                primaryColor.copy(alpha = (0.70f + beat * 0.25f) * alphaMix),
                Color.Transparent,
              ),
              startX = startX - 12.dp.toPx(),
              endX   = startX + totalWidth + 12.dp.toPx(),
            ),
            start = Offset(startX - 12.dp.toPx(), baselineY),
            end   = Offset(startX + totalWidth + 12.dp.toPx(), baselineY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )

          for (b in 0 until numBars) {
            // Left half mirrors right half so bin 0 (bass) is in the center on both sides.
            val halfIdx = if (b < numBars / 2) (numBars / 2 - 1 - b) else (b - numBars / 2)
            val binIndex = (halfIdx.toFloat() / (numBars / 2 - 1f)) * (SPECTRUM_BINS - 1)
            val binLow  = binIndex.toInt().coerceIn(0, SPECTRUM_BINS - 1)
            val binHigh = (binLow + 1).coerceIn(0, SPECTRUM_BINS - 1)
            val fracBin = binIndex - binLow
            val rawMag = (spectrum[binLow] * (1f - fracBin) + spectrum[binHigh] * fracBin).coerceIn(0f, 1f)

            // Dynamic perceptual scaling: boosts low/mid amplitudes so movement is vivid and clear
            val dynamicMag = (rawMag.pow(0.75f) * 1.15f).coerceIn(0f, 1f)
            val barHeight = minBarHeight + dynamicMag * (maxBarHeight - minBarHeight)

            // Falling peak dots physics
            if (barHeight >= eqPeaks[b]) {
              eqPeaks[b] = barHeight
              eqPeakVels[b] = 0f
            } else {
              eqPeakVels[b] += 220.dp.toPx() * dt
              val maxVelEq = (eqPeaks[b] - minBarHeight) / dt
              eqPeakVels[b] = eqPeakVels[b].coerceAtMost(maxVelEq.coerceAtLeast(0f))
              eqPeaks[b] = (eqPeaks[b] - eqPeakVels[b] * dt).coerceAtLeast(minBarHeight)
            }

            val barX = startX + b * (barW + gapW)
            // Color: center (bass) = primary, edges (treble) = tertiary
            val colorFrac = halfIdx.toFloat() / (numBars / 2 - 1f)
            val baseBarColor  = lerpColor(primaryColor, tertiaryColor, colorFrac)
            val barColor = themedBarColor(baseBarColor, onSurface, overall)

            // Equalizer bar (grows upward from baseline)
            drawRoundRect(
              brush = Brush.verticalGradient(
                colors = listOf(
                  barColor.copy(alpha = 0.98f * alphaMix),
                  secondaryColor.copy(alpha = 0.88f * alphaMix),
                  primaryColor.copy(alpha = 0.70f * alphaMix),
                ),
                startY = baselineY - barHeight,
                endY   = baselineY,
              ),
              topLeft = Offset(barX, baselineY - barHeight),
              size    = Size(barW, barHeight),
              cornerRadius = CornerRadius(2.dp.toPx()),
            )

            // Mirror reflection (fades downward)
            val reflHeight = minBarHeight * 0.5f + dynamicMag * (maxBarHeight * 0.38f)
            drawRoundRect(
              brush = Brush.verticalGradient(
                colors = listOf(
                  barColor.copy(alpha = (0.35f + dynamicMag * 0.20f) * alphaMix),
                  primaryColor.copy(alpha = 0.15f * alphaMix),
                  Color.Transparent,
                ),
                startY = baselineY + 1.dp.toPx(),
                endY   = (baselineY + reflHeight + 1.dp.toPx()).coerceAtLeast(baselineY + 2.dp.toPx()),
              ),
              topLeft = Offset(barX, baselineY + 1.dp.toPx()),
              size    = Size(barW, reflHeight.coerceAtLeast(1.dp.toPx())),
              cornerRadius = CornerRadius(1.5.dp.toPx()),
            )

            // Falling peak cap
            if (eqPeaks[b] > minBarHeight + 2.dp.toPx()) {
              val peakDotY = baselineY - eqPeaks[b] - 3.dp.toPx()
              drawRoundRect(
                color    = barColor.copy(alpha = 0.95f * alphaMix),
                topLeft  = Offset(barX, peakDotY),
                size     = Size(barW, 2.5.dp.toPx()),
                cornerRadius = CornerRadius(1.2.dp.toPx()),
              )
            }
          }
        }

        MusicVisualizerStyle.OSCILLOSCOPE_RING -> {
          // ── 3. Oscilloscope Ring: Glowing ring following real PCM waveform, smooth curves ──
          val alphaMix = 0.55f + 0.45f * bands.audioMix
          val waveform = AudioAnalyzer.latestWaveform
          val pulse = blobPulse(bands, pausedScale)
          val baseRadius = minDimension * 0.32f * (0.92f + pulse * 0.08f)
          val pts = VIZ_POINTS
          val step = (2f * PI.toFloat()) / pts

          // Ambient radial bloom behind the ring
          val glowAlpha = (0.35f + overall * 0.35f + bass * 0.25f + beat * 0.15f).coerceAtMost(0.90f)
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                tertiaryColor.copy(alpha = glowAlpha * 0.65f * alphaMix),
                primaryColor.copy(alpha = glowAlpha * 0.40f * alphaMix),
                Color.Transparent,
              ),
              center = Offset(cx, cy),
              radius = baseRadius * 1.22f,
            ),
            radius = baseRadius * 1.22f,
            center = Offset(cx, cy),
          )

          // Inner solid core disc with depth and popping pulse
          val coreDiscRadius = (baseRadius * 0.55f * pulse).coerceAtMost(baseRadius * 0.78f)
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = (0.88f + overall * 0.12f).coerceAtMost(1f) * alphaMix),
                tertiaryColor.copy(alpha = (0.78f + overall * 0.18f).coerceAtMost(1f) * alphaMix),
                primaryColor.copy(alpha = 0.25f * alphaMix),
              ),
              center = Offset(cx - coreDiscRadius * 0.15f, cy - coreDiscRadius * 0.15f),
              radius = coreDiscRadius * 1.3f,
            ),
            radius = coreDiscRadius,
            center = Offset(cx, cy),
          )

          // Core accent ring that expands with beat
          drawCircle(
            color = onPrimaryColor.copy(alpha = (0.30f + beat * 0.35f) * alphaMix),
            radius = coreDiscRadius * 0.82f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.8.dp.toPx()),
          )

          // Pre-compute all ring points
          val ringPoints = Array(pts) { i ->
            val theta = i * step - PI.toFloat() / 2f
            val wSample = if (waveform != null && waveform.isNotEmpty()) {
              val idx = ((i.toFloat() / pts) * (waveform.size - 1)).toInt().coerceIn(0, waveform.size - 1)
              waveform[idx]
            } else {
              // Smooth idle harmonic: three overlapping sinusoids driven by phase
              sin(theta * 3f + bands.phase * 1.6f) * (0.075f + bass * 0.14f) +
                cos(theta * 7f - bands.phase * 2.1f) * (0.045f + mid * 0.18f) +
                sin(theta * 13f + bands.phase * 3.0f) * (0.022f + treble * 0.12f)
            }
            val waveAmp = baseRadius * 0.18f * (1f + mid * 0.40f + beat * 0.25f)
            val strokeHalf = (8.dp.toPx() + mid * 5.dp.toPx()) / 2f
            val maxRingR = (minDimension / 2f) * MAX_RADIUS_FRAC - strokeHalf
            val r = (baseRadius + wSample * waveAmp)
              .coerceIn(baseRadius * 0.65f, maxRingR)
            Offset(cx + cos(theta) * r, cy + sin(theta) * r)
          }

          // Smooth closed path using midpoint quadratic Béziers
          oscPath.reset()
          val first = ringPoints[0]
          val last  = ringPoints[pts - 1]
          oscPath.moveTo((first.x + last.x) / 2f, (first.y + last.y) / 2f)
          for (i in 0 until pts) {
            val curr = ringPoints[i]
            val next = ringPoints[(i + 1) % pts]
            val midX = (curr.x + next.x) / 2f
            val midY = (curr.y + next.y) / 2f
            oscPath.quadraticTo(curr.x, curr.y, midX, midY)
          }
          oscPath.close()

          val sweepBrush = Brush.sweepGradient(
            colors = listOf(
              themedBarColor(primaryColor, onSurface, overall).copy(alpha = 0.95f * alphaMix),
              themedBarColor(secondaryColor, onSurface, overall).copy(alpha = 0.90f * alphaMix),
              themedBarColor(tertiaryColor, onSurface, overall).copy(alpha = 0.95f * alphaMix),
              themedBarColor(primaryColor, onSurface, overall).copy(alpha = 0.95f * alphaMix),
            ),
            center = Offset(cx, cy),
          )

          // Outer soft bloom stroke
          drawPath(
            path  = oscPath,
            brush = sweepBrush,
            style = Stroke(width = 8.dp.toPx() + mid * 5.dp.toPx(), cap = StrokeCap.Round),
            alpha = 0.40f * alphaMix,
          )

          // Core crisp glowing ring
          drawPath(
            path  = oscPath,
            brush = sweepBrush,
            style = Stroke(width = 3.2.dp.toPx() + mid * 2.dp.toPx(), cap = StrokeCap.Round),
          )
        }

        MusicVisualizerStyle.SPECTROGRAM_SCROLL -> {
          // ── 4. Scrolling Spectrogram: Frequency on Y-axis, time scrolls left.
          //    Uses the global AudioAnalyzer.spectrogramHistory (updated at audio capture rate
          //    ~25 Hz) so the scroll speed is tied to audio, not the 60fps draw clock. ──
          val alphaMix = 0.55f + 0.45f * bands.audioMix
          val cols   = SPECTROGRAM_COLS
          val rows   = SPECTRUM_BINS
          val totalWidth  = minDimension * 0.90f
          val totalHeight = minDimension * 0.74f
          val startX = cx - totalWidth / 2f
          val startY = cy - totalHeight / 2f
          val colWidth  = totalWidth  / cols
          val rowHeight = totalHeight / rows

          // Ambient soft bloom behind spectrogram
          drawRoundRect(
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = (0.24f + bass * 0.25f + beat * 0.15f).coerceAtMost(0.60f) * alphaMix),
                tertiaryColor.copy(alpha = 0.12f * alphaMix),
                Color.Transparent,
              ),
              center = Offset(cx, cy),
              radius = minDimension * 0.45f,
            ),
            topLeft = Offset(startX - 10.dp.toPx(), startY - 10.dp.toPx()),
            size    = Size(totalWidth + 20.dp.toPx(), totalHeight + 20.dp.toPx()),
            cornerRadius = CornerRadius(16.dp.toPx()),
          )

          // Background card
          drawRoundRect(
            color  = surfaceVariant.copy(alpha = 0.40f * alphaMix),
            topLeft = Offset(startX - 4.dp.toPx(), startY - 4.dp.toPx()),
            size    = Size(totalWidth + 8.dp.toPx(), totalHeight + 8.dp.toPx()),
            cornerRadius = CornerRadius(14.dp.toPx()),
          )

          // Snapshot the deque under the lock once per draw frame.
          val history: Array<FloatArray> = synchronized(AudioAnalyzer.spectrogramHistory) {
            val h = AudioAnalyzer.spectrogramHistory
            if (h.isEmpty()) null
            else Array(h.size) { i -> h[i].copyOf() }
          } ?: Array(1) { FloatArray(SPECTRUM_BINS) } // blank until data arrives

          val numCols = history.size // may be < SPECTROGRAM_COLS until history fills up

          // Draw: column 0 (oldest) on left, column N-1 (newest) on right.
          // We pad on the left with empty space when history hasn't filled yet.
          val colOffset = cols - numCols // empty columns on the left
          for (c in 0 until numCols) {
            val specSlice = history[c]
            val drawCol = colOffset + c
            val colX = startX + drawCol * colWidth

            for (r in 0 until rows) {
              val rawMag = specSlice[r].coerceIn(0f, 1f)
              if (rawMag < 0.03f) continue

              // Non-linear perceptual scaling so cells illuminate vibrantly
              val mag = (rawMag.pow(0.78f) * 1.15f).coerceIn(0f, 1f)

              // Bin 0 = bass at bottom, bin (rows-1) = treble at top
              val rowY = startY + totalHeight - (r + 1) * rowHeight

              val cellColor = themedBarColor(
                base = when {
                  mag < 0.30f -> primaryColor.copy(alpha = (0.30f + mag * 1.6f) * alphaMix)
                  mag < 0.65f -> lerpColor(primaryColor, secondaryColor, (mag - 0.30f) / 0.35f)
                                .copy(alpha = (0.80f + mag * 0.20f) * alphaMix)
                  else        -> lerpColor(secondaryColor, tertiaryColor, (mag - 0.65f) / 0.35f)
                                .copy(alpha = 0.96f * alphaMix)
                },
                onSurface = onSurface,
                overall = overall,
              )

              drawRoundRect(
                color        = cellColor,
                topLeft      = Offset(colX, rowY),
                size         = Size((colWidth * 0.90f).coerceAtLeast(1.5.dp.toPx()), (rowHeight * 0.88f).coerceAtLeast(1.5.dp.toPx())),
                cornerRadius = CornerRadius(1.2.dp.toPx()),
              )
            }
          }

          // Glowing leading-edge cursor at the right (newest data side)
          val edgeX = startX + totalWidth
          drawLine(
            brush = Brush.verticalGradient(
              colors = listOf(
                tertiaryColor.copy(alpha = 0.95f * alphaMix),
                secondaryColor.copy(alpha = 0.90f * alphaMix),
                primaryColor.copy(alpha = 0.95f * alphaMix),
              ),
              startY = startY - 2.dp.toPx(),
              endY   = startY + totalHeight + 2.dp.toPx(),
            ),
            start = Offset(edgeX, startY - 2.dp.toPx()),
            end   = Offset(edgeX, startY + totalHeight + 2.dp.toPx()),
            strokeWidth = (2.5f + bass * 2.5f + beat * 2f).dp.toPx(),
            cap = StrokeCap.Round,
          )
        }
      }
    }

    // Stylized floating music glyph in center for circular visualizers
    if (style == MusicVisualizerStyle.RADIAL_SPECTRUM ||
      style == MusicVisualizerStyle.OSCILLOSCOPE_RING
    ) {
      val iconSize = if (style == MusicVisualizerStyle.OSCILLOSCOPE_RING) 56.dp else 50.dp
      val iconTint = if (primaryColor.luminance() > 0.4f) {
        onPrimaryColor.copy(alpha = 0.92f)
      } else {
        onSurface.copy(alpha = 0.85f)
      }
      Icon(
        imageVector = Icons.Filled.Audiotrack,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier
          .size(iconSize)
          .graphicsLayer {
            if (bands.tick < 0L) return@graphicsLayer
            val s = blobPulse(bands, pausedScale)
            scaleX = s
            scaleY = s
          },
      )
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoRowDot() {
  Text(
    text = "·",
    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
    style = MaterialTheme.typography.bodySmall,
  )
}

@Composable
private fun ActionBarButton(
  icon: @Composable () -> Unit,
  active: Boolean,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  // Use a plain Box with combinedClickable instead of wrapping IconButton.
  // IconButton consumes all pointer input internally, which prevents the combinedClickable
  // modifier from ever receiving the long-press gesture. Box has no competing gesture handler.
  Box(
    modifier = Modifier
      .size(48.dp)
      .clip(CircleShape)
      .then(
        if (active) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
        else Modifier
      )
      .combinedClickable(
        onClick = onClick,
        onLongClick = {
          if (onLongClick != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
          }
        },
      ),
    contentAlignment = Alignment.Center,
  ) {
    androidx.compose.runtime.CompositionLocalProvider(
      androidx.compose.material3.LocalContentColor provides (
        if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
      )
    ) {
      icon()
    }
  }
}

@Composable
private fun RotatingVinylDisc(
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = (kotlin.math.min(size.width, size.height) / 2f) * 0.96f

      // ── 1. Vinyl Body: Anisotropic lighting with authentic dual specular cones ──
      val vinylGradient = Brush.sweepGradient(
        colors = listOf(
          Color(0xFF0C0C0E),
          Color(0xFF1C1D24),
          Color(0xFF2E303C), // Specular reflection 1
          Color(0xFF1C1D24),
          Color(0xFF0C0C0E),
          Color(0xFF15161C),
          Color(0xFF0C0C0E),
          Color(0xFF1C1D24),
          Color(0xFF2E303C), // Specular reflection 2
          Color(0xFF1C1D24),
          Color(0xFF0C0C0E),
          Color(0xFF15161C),
          Color(0xFF0C0C0E),
        ),
        center = center
      )
      drawCircle(
        brush = vinylGradient,
        radius = radius,
        center = center
      )

      // ── 2. Outer Beveled Rim & Run-in Groove ──
      drawCircle(
        color = Color(0xFF383A47).copy(alpha = 0.5f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
      )
      drawCircle(
        color = Color(0xFF08080A),
        radius = radius * 0.985f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      // ── 3. Realistic Audio Groove Tracks (Clusters with silent band gaps) ──
      // Track 1 (Outer): 0.77f .. 0.95f
      for (i in 0..14) {
        val rFrac = 0.77f + 0.18f * (i / 14f)
        val alpha = if (i % 3 == 0) 0.35f else 0.18f
        drawCircle(
          color = Color(0xFF000000).copy(alpha = alpha),
          radius = radius * rFrac,
          center = center,
          style = Stroke(width = (0.75f + (i % 2) * 0.5f).dp.toPx())
        )
      }

      // Inter-track silence gap 1
      drawCircle(
        color = Color(0xFF0A0A0D),
        radius = radius * 0.755f,
        center = center,
        style = Stroke(width = 2.5.dp.toPx())
      )

      // Track 2 (Mid): 0.57f .. 0.74f
      for (i in 0..12) {
        val rFrac = 0.57f + 0.17f * (i / 12f)
        val alpha = if (i % 3 == 0) 0.32f else 0.16f
        drawCircle(
          color = Color(0xFF000000).copy(alpha = alpha),
          radius = radius * rFrac,
          center = center,
          style = Stroke(width = (0.75f + (i % 2) * 0.5f).dp.toPx())
        )
      }

      // Inter-track silence gap 2
      drawCircle(
        color = Color(0xFF0A0A0D),
        radius = radius * 0.555f,
        center = center,
        style = Stroke(width = 2.5.dp.toPx())
      )

      // Track 3 (Inner): 0.42f .. 0.54f
      for (i in 0..8) {
        val rFrac = 0.42f + 0.12f * (i / 8f)
        val alpha = if (i % 2 == 0) 0.30f else 0.15f
        drawCircle(
          color = Color(0xFF000000).copy(alpha = alpha),
          radius = radius * rFrac,
          center = center,
          style = Stroke(width = 0.8.dp.toPx())
        )
      }

      // ── 4. Dead Wax / Lead-out Groove (Smooth inner vinyl before label) ──
      drawCircle(
        color = Color(0xFF0F0F13),
        radius = radius * 0.40f,
        center = center
      )
      drawCircle(
        color = Color(0xFF1E2028).copy(alpha = 0.45f),
        radius = radius * 0.38f,
        center = center,
        style = Stroke(width = 0.9.dp.toPx())
      )
      drawCircle(
        color = Color(0xFF1E2028).copy(alpha = 0.35f),
        radius = radius * 0.36f,
        center = center,
        style = Stroke(width = 0.9.dp.toPx())
      )

      // ── 5. Center Label Edge & Paper Bevel ──
      val labelRadius = radius * 0.335f

      // Outer label shadow/groove
      drawCircle(
        color = Color(0xFF070709),
        radius = labelRadius + 1.5.dp.toPx(),
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      // Label background (luxurious radial gradient with primary tint)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.92f),
            primaryColor.copy(alpha = 0.78f),
            secondaryColor.copy(alpha = 0.85f),
          ),
          center = center,
          radius = labelRadius
        ),
        radius = labelRadius,
        center = center
      )

      // Decorative golden/subtle label border ring
      drawCircle(
        color = onPrimaryColor.copy(alpha = 0.35f),
        radius = labelRadius * 0.92f,
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
      )
      drawCircle(
        color = onPrimaryColor.copy(alpha = 0.20f),
        radius = labelRadius * 0.87f,
        center = center,
        style = Stroke(width = 0.8.dp.toPx())
      )

      // Lower decorative arc / divider on label
      drawCircle(
        color = onPrimaryColor.copy(alpha = 0.25f),
        radius = labelRadius * 0.48f,
        center = center,
        style = Stroke(width = 0.9.dp.toPx())
      )

      // ── 6. Center Spindle Hole ──
      // Outer black grommet shadow
      drawCircle(
        color = Color(0xFF0E0E12),
        radius = radius * 0.085f,
        center = center
      )
      // Metallic silver spindle washer/ring
      drawCircle(
        brush = Brush.sweepGradient(
          colors = listOf(
            Color(0xFFD4D6DC),
            Color(0xFF8A8D98),
            Color(0xFFECEEF2),
            Color(0xFF8A8D98),
            Color(0xFFD4D6DC)
          ),
          center = center
        ),
        radius = radius * 0.072f,
        center = center
      )
      // Spindle hole center (dark hole)
      drawCircle(
        color = Color(0xFF14151A),
        radius = radius * 0.052f,
        center = center
      )
    }

    // ── Label Typography: "JustPlayer" in italics ──
    Column(
      modifier = Modifier
        .fillMaxSize(0.32f),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = "JustPlayer",
        color = onPrimaryColor.copy(alpha = 0.95f),
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        maxLines = 1,
        letterSpacing = 0.4.sp,
      )
      Spacer(Modifier.height(18.dp))
      Text(
        text = "STEREO",
        color = onPrimaryColor.copy(alpha = 0.70f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 7.5.sp,
        letterSpacing = 1.2.sp,
      )
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cover art  (when artwork bitmap is present)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AudioCoverArt(
  bitmap: Bitmap?,
) {
  if (bitmap != null && !bitmap.isRecycled) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        bitmap = remember(bitmap) { bitmap.asImageBitmap() },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.surfaceVariant),
      )
      // Center spindle hole — gives authentic vinyl record aesthetic to the rotating artwork
      Surface(
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        color = Color(0xFF14151A),
        border = BorderStroke(2.dp, Color(0xFFD4D6DC).copy(alpha = 0.85f)),
        shadowElevation = 2.dp,
      ) {}
    }
  } else {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)),
      contentAlignment = Alignment.Center,
    ) {
      RotatingVinylDisc(
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Audio Properties bottom sheet  (screenshot 2)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AudioPropertiesSheet(
  path: String?,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Properties that MPV reliably pushes updates for when the track changes:
  val sampleRate by MPVLib.propInt["audio-params/samplerate"].collectAsState()
  val bitrateRaw by MPVLib.propInt["audio-bitrate"].collectAsState()
  val channels   by MPVLib.propString["audio-params/channels"].collectAsState()

  // Metadata & codec properties are read directly from MPV via produceState,
  // keyed on `path`. When a new file loads without certain tags (e.g. no artist),
  // MPV never emits a property-change event — the old StateFlow value would linger.
  // Direct reads return null for missing tags, preventing stale data.
  data class AudioMeta(
    val codec: String? = null,
    val fileFormat: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val album: String? = null,
  )

  val meta by produceState(initialValue = AudioMeta(), key1 = path) {
    // Wait for MPV to finish parsing the new file's tags
    kotlinx.coroutines.delay(200)
    value = AudioMeta(
      codec       = runCatching { MPVLib.getPropertyString("audio-codec-name") }.getOrNull(),
      fileFormat  = runCatching { MPVLib.getPropertyString("file-format") }.getOrNull(),
      title       = runCatching { MPVLib.getPropertyString("metadata/by-key/title") }.getOrNull(),
      artist      = runCatching { MPVLib.getPropertyString("metadata/by-key/artist") }.getOrNull(),
      albumArtist = runCatching { MPVLib.getPropertyString("metadata/by-key/album_artist") }.getOrNull(),
      album       = runCatching { MPVLib.getPropertyString("metadata/by-key/album") }.getOrNull(),
    )
  }

  // `meta` resets to all-null on path change, then populates after 200ms.
  // While meta hasn't loaded yet, show placeholders.
  val ready = meta.codec != null || meta.fileFormat != null || meta.title != null

  val codecStr = if (ready) meta.codec ?: "—" else "—"
  val formatStr = if (ready) meta.fileFormat
    ?.uppercase(Locale.US)?.split(",")?.firstOrNull() ?: "—" else "—"
  val sampleRateStr = if (ready && sampleRate != null && sampleRate!! > 0)
    "${sampleRate!! / 1000.0} kHz" else "—"
  val bitrateStr = if (ready && bitrateRaw != null && bitrateRaw!! > 0)
    "${bitrateRaw!! / 1000} kbps" else "—"
  val channelStr = if (ready) when {
    channels?.contains("stereo", ignoreCase = true) == true -> "Stereo (2.0)"
    channels?.contains("mono",   ignoreCase = true) == true -> "Mono (1.0)"
    channels?.contains("5.1") == true                       -> "Surround (5.1)"
    channels?.contains("7.1") == true                       -> "Surround (7.1)"
    else -> channels ?: "—"
  } else "—"
  val titleStr = if (ready) meta.title
    ?: path?.substringAfterLast('/')?.substringBeforeLast('.') ?: "—" else "—"
  val artistStr = if (ready) meta.artist ?: meta.albumArtist ?: "—" else "—"
  val albumStr  = if (ready) meta.album ?: "—" else "—"

  val fileSizeStr = remember(path) {
    if (path.isNullOrBlank()) return@remember "—"
    try {
      val bytes = java.io.File(path.removePrefix("file://")).length()
      when {
        bytes >= 1_048_576L -> String.format(Locale.US, "%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024L      -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else                -> "$bytes B"
      }
    } catch (_: Exception) { "—" }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxHeight(0.85f),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      item {
        Text(
          text = "AUDIO PROPERTIES",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
          ),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(bottom = 16.dp),
        )
      }
      item { PropRow("TITLE",          titleStr) }
      item { PropRow("ARTIST",         artistStr) }
      item { PropRow("ALBUM",          albumStr) }
      item { PropRow("FORMAT / CODEC", "$formatStr ($codecStr)") }
      item { PropRow("SAMPLE RATE",    sampleRateStr) }
      item { PropRow("BITRATE",        bitrateStr) }
      item { PropRow("CHANNELS",       channelStr) }
      item { PropRow("FILE SIZE",      fileSizeStr) }
      if (!path.isNullOrBlank()) {
        item { PropRow("FILE LOCATION", path.removePrefix("file://")) }
      }
      item { Spacer(Modifier.height(24.dp)) }
    }
  }
}

@Composable
private fun PropRow(label: String, value: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
      .padding(horizontal = 14.dp, vertical = 10.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.SemiBold,
      ),
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )
  }
  Spacer(Modifier.height(6.dp))
}
