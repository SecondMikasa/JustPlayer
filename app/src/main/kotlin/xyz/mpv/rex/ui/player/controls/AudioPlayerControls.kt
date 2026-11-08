package xyz.mpv.rex.ui.player.controls
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.mpv.rex.preferences.MusicVisualizerStyle
import xyz.mpv.rex.utils.media.AudioAmplitudeTracker
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.sin

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
  val hasMicPermission = remember(context) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
  }

  var audioEnergy by remember { mutableFloatStateOf(0f) }
  var smoothedEnergy by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(paused, audioReactive, hasMicPermission) {
    if (paused == true || !audioReactive || !hasMicPermission) {
      audioEnergy = 0f
      smoothedEnergy = 0f
      return@LaunchedEffect
    }
    val trackerJob = launch { AudioAmplitudeTracker.startListening() }
    try {
      while (isActive) {
        val raw = AudioAmplitudeTracker.currentAmplitude
        // Apply exponential scaling for more dramatic response
        // Low values (0.1-0.3) get boosted, high values stay punchy
        audioEnergy = if (raw > 0f) {
          val boosted = raw * raw * 0.6f + raw * 0.5f // Quadratic + linear for better dynamics
          boosted.coerceIn(0f, 1.2f) // Allow slight overdrive for impact
        } else 0f
        // Extra smoothing layer for fluid visual response
        smoothedEnergy = smoothedEnergy * 0.7f + audioEnergy * 0.3f
        delay(33)
      }
    } finally {
      trackerJob.cancel()
      audioEnergy = 0f
      smoothedEnergy = 0f
    }
  }

  // ── Visualizer Toggle State ───────────────────────────────────────────────
  var showVisualizerOverArt by remember { mutableStateOf(false) }
  val displayVisualizer = showVisualizerOverArt || currentArtwork == null || currentArtwork.isRecycled

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
  val artCornerRadius by animateDpAsState(
    targetValue = if (isPaused) 22.dp else 28.dp,
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

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
                this.shadowElevation = artElevation.toPx()
                this.shape = if (!displayVisualizer && currentArtwork != null) RoundedCornerShape(artCornerRadius) else CircleShape
                this.clip = true
              }
              .clip(if (!displayVisualizer && currentArtwork != null) RoundedCornerShape(artCornerRadius) else CircleShape),
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .then(
                  if (currentArtwork != null && !currentArtwork.isRecycled) {
                    Modifier.combinedClickable(
                      onClick = {},
                      onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showVisualizerOverArt = !showVisualizerOverArt
                      },
                    )
                  } else Modifier
                ),
            ) {
              if (displayVisualizer) {
                if (artworkResolved) {
                  AnimatedMusicBlob(
                    paused = paused == true,
                    style = visualizerStyle,
                    audioEnergy = smoothedEnergy,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              } else if (currentArtwork != null) {
                AudioCoverArt(currentArtwork, style = visualizerStyle, audioEnergy = smoothedEnergy)
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
            modifier = Modifier.fillMaxWidth(),
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
          modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.weight(1f))

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
              this.shadowElevation = artElevation.toPx()
              this.shape = if (!displayVisualizer && currentArtwork != null) RoundedCornerShape(artCornerRadius) else CircleShape
              this.clip = true
            },
          shape = if (!displayVisualizer && currentArtwork != null) RoundedCornerShape(artCornerRadius) else CircleShape,
          shadowElevation = artElevation,
          color = Color.Transparent,
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .then(
                if (currentArtwork != null && !currentArtwork.isRecycled) {
                  Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      showVisualizerOverArt = !showVisualizerOverArt
                    },
                  )
                } else Modifier
              ),
          ) {
            if (displayVisualizer) {
              if (artworkResolved) {
                AnimatedMusicBlob(
                  paused = paused == true,
                  style = visualizerStyle,
                  audioEnergy = smoothedEnergy,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            } else if (currentArtwork != null) {
              AudioCoverArt(currentArtwork, style = visualizerStyle, audioEnergy = smoothedEnergy)
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
// Animated Music Blob  — dynamic, mesmerizing fluid visualizer for missing art
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dynamic, mesmerizing animated fluid blob placeholder when no album art is available.
 * Creates an organic, multi-layered liquid aura with Material 3 dynamic colors
 * that smoothly pulses and morphs in real-time, pausing gracefully with the music.
 */
// ─────────────────────────────────────────────────────────────────────────────
// Animated Music Visualizer  — dynamic, continuous visualizers for missing art
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dynamic, mesmerizing music visualizer when no album art is available.
 * Supports multiple aesthetic styles (Fluid Aura, Cosmic Ripple, Chromatic Nebula, Retro Wireframe).
 * Uses a monotonic continuous time frame loop to guarantee 100% continuous, uninterrupted motion
 * without any cuts, snaps or seams.
 * Seamlessly reacts to music energy when microphone audio input is enabled.
 */
@Composable
fun AnimatedMusicBlob(
  paused: Boolean,
  modifier: Modifier = Modifier,
  style: MusicVisualizerStyle = MusicVisualizerStyle.FLUID_BLOB,
  audioEnergy: Float = 0f,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
  val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

  // Monotonic frame time accumulator: eliminates all modulo wraps & cuts completely
  var continuousTime by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(paused) {
    if (paused) return@LaunchedEffect
    var lastNanos = 0L
    while (isActive) {
      withFrameNanos { nanos ->
        if (lastNanos != 0L) {
          val dt = (nanos - lastNanos) / 1_000_000_000f
          // Smooth increment, clamped against background sleep jumps
          continuousTime += dt.coerceIn(0f, 0.1f)
        }
        lastNanos = nanos
      }
    }
  }

  // Smooth synthetic pulse + reactive audio boost
  val syntheticPulse = if (!paused) {
    1f + sin(continuousTime * 2.2f) * 0.045f
  } else {
    0.95f
  }
  val reactiveBoost = if (audioEnergy > 0f) audioEnergy * 0.32f else 0f
  val currentPulse = syntheticPulse + reactiveBoost
  val waveExcitation = 1f + (if (audioEnergy > 0f) audioEnergy * 0.9f else 0f)

  val outerPath = remember { Path() }
  val innerPath = remember { Path() }

  Box(
    modifier = modifier
      .background(
        Brush.radialGradient(
          colors = listOf(
            surfaceVariant.copy(alpha = 0.55f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            Color.Transparent,
          )
        )
      ),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val minDimension = minOf(size.width, size.height)
      val baseRadius = minDimension * 0.36f * currentPulse

      when (style) {
        MusicVisualizerStyle.FLUID_BLOB -> {
          // ── 1. Fluid Aura (Organic Liquid Amoeba) - Enhanced Audio Reactivity ──
          val glowRadius = baseRadius * (1.5f + (if (audioEnergy > 0f) audioEnergy * 0.8f else 0f))
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = 0.38f + (audioEnergy * 0.4f)),
                tertiaryColor.copy(alpha = 0.18f + (audioEnergy * 0.15f)),
                Color.Transparent,
              ),
              center = Offset(cx, cy),
              radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(cx, cy),
          )

          fun buildBlobPath(
            path: Path,
            radius: Float,
            speed1: Float,
            speed2: Float,
            harmonic1: Float,
            harmonic2: Float,
            harmonic3: Float,
            rotSpeed: Float,
          ) {
            path.reset()
            val numPoints = 64
            val step = (2f * PI.toFloat()) / numPoints
            val points = ArrayList<Offset>(numPoints)
            val p1 = continuousTime * speed1
            val p2 = continuousTime * speed2
            val rot = continuousTime * rotSpeed

            for (i in 0 until numPoints) {
              val theta = i * step + rot
              val rOffset = (
                sin(3f * theta + p1) * harmonic1 +
                cos(4f * theta - p2) * harmonic2 +
                sin(2f * theta + p1 * 0.5f) * harmonic3
              ) * radius * waveExcitation
              val r = radius + rOffset
              points.add(Offset(cx + r * cos(theta), cy + r * sin(theta)))
            }

            if (points.isNotEmpty()) {
              val first = points[0]
              val last = points.last()
              val startMid = Offset((first.x + last.x) / 2f, (first.y + last.y) / 2f)
              path.moveTo(startMid.x, startMid.y)

              for (i in 0 until numPoints) {
                val curr = points[i]
                val next = points[(i + 1) % numPoints]
                val mid = Offset((curr.x + next.x) / 2f, (curr.y + next.y) / 2f)
                path.quadraticTo(curr.x, curr.y, mid.x, mid.y)
              }
              path.close()
            }
          }

          // Outer Translucent Liquid Fringe - boosted reactivity
          buildBlobPath(
            path = outerPath,
            radius = baseRadius * (1.08f + audioEnergy * 0.15f),
            speed1 = 1.2f + audioEnergy * 0.8f,
            speed2 = 0.8f + audioEnergy * 0.6f,
            harmonic1 = 0.15f + audioEnergy * 0.1f,
            harmonic2 = 0.12f + audioEnergy * 0.08f,
            harmonic3 = 0.09f + audioEnergy * 0.06f,
            rotSpeed = -0.18f - audioEnergy * 0.3f,
          )
          drawPath(
            path = outerPath,
            brush = Brush.sweepGradient(
              colors = listOf(
                tertiaryColor.copy(alpha = 0.45f + audioEnergy * 0.15f),
                secondaryColor.copy(alpha = 0.35f + audioEnergy * 0.1f),
                primaryColor.copy(alpha = 0.40f + audioEnergy * 0.12f),
                tertiaryColor.copy(alpha = 0.45f + audioEnergy * 0.15f),
              ),
              center = Offset(cx, cy),
            ),
          )

          // Inner Vibrant Liquid Core - enhanced colors with audio
          buildBlobPath(
            path = innerPath,
            radius = baseRadius + audioEnergy * 0.2f,
            speed1 = 0.75f + audioEnergy * 0.6f,
            speed2 = 1.1f + audioEnergy * 0.7f,
            harmonic1 = 0.13f + audioEnergy * 0.12f,
            harmonic2 = 0.10f + audioEnergy * 0.1f,
            harmonic3 = 0.08f + audioEnergy * 0.08f,
            rotSpeed = 0.22f + audioEnergy * 0.4f,
          )
          drawPath(
            path = innerPath,
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = 0.92f + audioEnergy * 0.08f),
                tertiaryColor.copy(alpha = 0.82f + audioEnergy * 0.1f),
                secondaryColor.copy(alpha = 0.72f + audioEnergy * 0.15f),
              ),
              center = Offset(cx - baseRadius * 0.2f, cy - baseRadius * 0.25f),
              radius = baseRadius * (1.3f + audioEnergy * 0.3f),
            ),
          )

          // Specular glint - pulses with audio
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                Color.White.copy(alpha = 0.42f + audioEnergy * 0.25f),
                Color.White.copy(alpha = 0.08f + audioEnergy * 0.1f),
                Color.Transparent,
              ),
              center = Offset(cx - baseRadius * 0.28f, cy - baseRadius * 0.32f),
              radius = baseRadius * (0.5f + audioEnergy * 0.15f),
            ),
            radius = baseRadius * (0.5f + audioEnergy * 0.15f),
            center = Offset(cx - baseRadius * 0.28f, cy - baseRadius * 0.32f),
          )

          // Center core glow - reactive
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                onPrimaryColor.copy(alpha = 0.25f + audioEnergy * 0.3f),
                Color.Transparent,
              ),
              center = Offset(cx, cy),
              radius = baseRadius * (0.42f + audioEnergy * 0.2f),
            ),
            radius = baseRadius * (0.42f + audioEnergy * 0.2f),
            center = Offset(cx, cy),
          )
        }

        MusicVisualizerStyle.PULSE_RINGS -> {
          // ── 2. Cosmic Ripple (Concentric Undulating Sound Waves) ──
          val numRings = 4
          val audioBoost = 1f + audioEnergy * 1.5f // Rings expand with audio
          for (rIdx in 1..numRings) {
            val ringRatio = rIdx.toFloat() / numRings
            val ringRadius = baseRadius * (0.38f + ringRatio * 0.72f) * audioBoost
            val ringAlpha = (1f - ringRatio * 0.42f).coerceIn(0.2f, 0.95f + audioEnergy * 0.05f)
            val ringSpeed = 1.0f + rIdx * 0.35f + audioEnergy * 0.6f // Speed increases with audio

            outerPath.reset()
            val numPoints = 64
            val step = (2f * PI.toFloat()) / numPoints
            val points = ArrayList<Offset>(numPoints)
            val phaseOffset = continuousTime * ringSpeed + rIdx * 1.2f

            for (i in 0 until numPoints) {
              val theta = i * step
              val deformation = sin(4f * theta + phaseOffset) * (0.12f + audioEnergy * 0.15f) * waveExcitation
              val r = ringRadius * (1f + deformation)
              points.add(Offset(cx + r * cos(theta), cy + r * sin(theta)))
            }

            if (points.isNotEmpty()) {
              val first = points[0]
              val last = points.last()
              val startMid = Offset((first.x + last.x) / 2f, (first.y + last.y) / 2f)
              outerPath.moveTo(startMid.x, startMid.y)
              for (i in 0 until numPoints) {
                val curr = points[i]
                val next = points[(i + 1) % numPoints]
                val mid = Offset((curr.x + next.x) / 2f, (curr.y + next.y) / 2f)
                outerPath.quadraticTo(curr.x, curr.y, mid.x, mid.y)
              }
              outerPath.close()

              val ringColor = if (rIdx % 2 == 0) primaryColor else tertiaryColor
              drawPath(
                path = outerPath,
                color = ringColor.copy(alpha = ringAlpha),
                style = Stroke(
                  width = (3.5f - ringRatio * 1.5f + audioEnergy * 1.2f).dp.toPx(),
                  cap = StrokeCap.Round,
                ),
              )
            }
          }

          // Center pulsing aura - reacts to audio
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = 0.85f + audioEnergy * 0.15f),
                tertiaryColor.copy(alpha = 0.45f + audioEnergy * 0.1f),
                Color.Transparent,
              ),
              center = Offset(cx, cy),
              radius = baseRadius * (0.45f + audioEnergy * 0.2f),
            ),
            radius = baseRadius * (0.45f + audioEnergy * 0.2f),
            center = Offset(cx, cy),
          )
        }

        MusicVisualizerStyle.NEBULA_GLOW -> {
          // ── 3. Chromatic Nebula (Orbiting Multi-Colored Plasma Spheres) ──
          val numOrbs = 3
          val orbitRadius = baseRadius * (0.35f + audioEnergy * 0.4f) // Enhanced audio response
          val orbRadius = baseRadius * (0.65f + audioEnergy * 0.25f) // Orbs expand with audio
          val orbSpeedBoost = 0.85f + audioEnergy * 0.8f // Faster orbit with audio

          // Center ambient nebula cloud - reacts to audio
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                secondaryColor.copy(alpha = 0.35f + audioEnergy * 0.2f),
                Color.Transparent,
              ),
              center = Offset(cx, cy),
              radius = baseRadius * (1.4f + audioEnergy * 0.35f),
            ),
            radius = baseRadius * (1.4f + audioEnergy * 0.35f),
            center = Offset(cx, cy),
          )

          val orbColors = listOf(primaryColor, tertiaryColor, secondaryColor)
          for (i in 0 until numOrbs) {
            val orbAngle = continuousTime * orbSpeedBoost + (i * 2f * PI.toFloat() / numOrbs)
            val orbX = cx + cos(orbAngle) * orbitRadius
            val orbY = cy + sin(orbAngle) * orbitRadius
            val color = orbColors[i % orbColors.size]

            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(
                  color.copy(alpha = 0.80f + audioEnergy * 0.2f),
                  color.copy(alpha = 0.40f + audioEnergy * 0.15f),
                  Color.Transparent,
                ),
                center = Offset(orbX, orbY),
                radius = orbRadius,
              ),
              radius = orbRadius,
              center = Offset(orbX, orbY),
            )
          }

          // Overlapping core shimmer - more vibrant with audio
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                onPrimaryColor.copy(alpha = 0.45f + audioEnergy * 0.2f),
                Color.Transparent,
              ),
              center = Offset(cx, cy),
              radius = baseRadius * (0.35f + audioEnergy * 0.15f),
            ),
            radius = baseRadius * (0.35f + audioEnergy * 0.15f),
            center = Offset(cx, cy),
          )
        }

        MusicVisualizerStyle.WIREFRAME_SPHERE -> {
          // ── 4. Retro Wireframe Globe (NOW WITH AUDIO REACTIVITY!) ──
          val sphereRadius = baseRadius * (0.95f + audioEnergy * 0.15f)
          val rotationY = continuousTime * (0.6f + audioEnergy * 1.2f) // Spins faster with audio
          val rotationX = sin(continuousTime * 0.3f) * 0.3f + audioEnergy * 0.4f // Added X-axis wobble
          val baseStrokeWidth = 1.2f + audioEnergy * 1.5f // Thicker lines with audio
          val color = primaryColor.copy(alpha = 0.65f + audioEnergy * 0.3f)
          val latBands = 18
          val lonBands = 24

          fun project(lat: Float, lon: Float): Offset {
            val x0 = cos(lat) * cos(lon)
            val y0 = sin(lat)
            val z0 = cos(lat) * sin(lon)
            
            // Apply X and Y rotations for wobble effect
            val xr1 = x0 * cos(rotationY) + z0 * sin(rotationY)
            val yr1 = y0 * cos(rotationX) - z0 * sin(rotationX)
            val zr1 = y0 * sin(rotationX) + z0 * cos(rotationX)
            val xr = xr1
            val yr = yr1 * cos(rotationY) - zr1 * sin(rotationY)
            
            return Offset(cx + xr * sphereRadius, cy - yr * sphereRadius)
          }

          val latStep = PI.toFloat() / latBands
          val lonStep = (2f * PI.toFloat()) / lonBands

          // Latitude lines (horizontal bands)
          for (i in 0..latBands) {
            val lat = -PI.toFloat() / 2f + i * latStep
            for (j in 0 until lonBands) {
              val p1 = project(lat, j * lonStep)
              val p2 = project(lat, (j + 1) * lonStep)
              drawLine(color, p1, p2, strokeWidth = baseStrokeWidth)
            }
          }

          // Longitude lines (vertical meridians)
          for (j in 0 until lonBands) {
            val lon = j * lonStep
            for (i in 0 until latBands) {
              val p1 = project(-PI.toFloat() / 2f + i * latStep, lon)
              val p2 = project(-PI.toFloat() / 2f + (i + 1) * latStep, lon)
              drawLine(color, p1, p2, strokeWidth = baseStrokeWidth)
            }
          }
          
          // Audio-reactive glow effect around the wireframe
          if (audioEnergy > 0.1f) {
            val glowRadius = sphereRadius * (1.1f + audioEnergy * 0.2f)
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(
                  primaryColor.copy(alpha = audioEnergy * 0.3f),
                  Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowRadius,
              ),
              radius = glowRadius,
              center = Offset(cx, cy),
            )
          }
        }
      }
    }

    // Stylized floating music glyph in center (for non-wireframe styles)
    if (style != MusicVisualizerStyle.WIREFRAME_SPHERE) {
      Icon(
        imageVector = Icons.Filled.Audiotrack,
        contentDescription = null,
        tint = onPrimaryColor.copy(alpha = 0.88f),
        modifier = Modifier
          .size(46.dp)
          .graphicsLayer {
            scaleX = currentPulse
            scaleY = currentPulse
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

// ─────────────────────────────────────────────────────────────────────────────
// Cover art  (when artwork bitmap is present)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AudioCoverArt(
  bitmap: Bitmap?,
  style: MusicVisualizerStyle = MusicVisualizerStyle.FLUID_BLOB,
  audioEnergy: Float = 0f,
) {
  if (bitmap != null && !bitmap.isRecycled) {
    Image(
      bitmap = remember(bitmap) { bitmap.asImageBitmap() },
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant),
    )
  } else {
    AnimatedMusicBlob(
      paused = false,
      style = style,
      audioEnergy = audioEnergy,
      modifier = Modifier.fillMaxSize(),
    )
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



