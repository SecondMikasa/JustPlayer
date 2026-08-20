package xyz.mpv.rex.ui.player.controls
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import xyz.mpv.rex.ui.player.Sheets
import xyz.mpv.rex.ui.player.controls.components.SeekbarWithTimers
import xyz.mpv.rex.ui.player.controls.components.sheets.PlaylistItem
import xyz.mpv.rex.ui.player.controls.components.sheets.toFixed
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
  val artist = rawArtist?.takeIf { it.isNotBlank() }
    ?: rawAlbumArtist?.takeIf { it.isNotBlank() }
  val artwork by viewModel.currentThumbnail.collectAsState()
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

  // ── Sleep timer ───────────────────────────────────────────────────────────
  val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()

  // ── Sheet / panel state ──────────────────────────────────────────────────
  val sheetShown by viewModel.sheetShown.collectAsState()
  val panelShown by viewModel.panelShown.collectAsState()

  val onOpenSheet: (Sheets) -> Unit = { viewModel.sheetShown.value = it }
  val onOpenPanel: (Panels) -> Unit = { viewModel.panelShown.value = it }

  var showAudioProperties by remember { mutableStateOf(false) }

  // Show the system status bar and navigation bar while the audio player is visible,
  // and restore the video-player fullscreen state when it is disposed.
  val view = LocalView.current
  DisposableEffect(view) {
    val window = (view.context as? android.app.Activity)?.window
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
  val currentArtwork = artwork
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
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
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
      Box(
        modifier = Modifier
          .fillMaxWidth(0.78f)
          .aspectRatio(1f)
          .align(Alignment.CenterHorizontally)
          .clip(if (currentArtwork != null) RoundedCornerShape(16.dp) else CircleShape),
      ) {
        if (currentArtwork != null) {
          AudioCoverArt(currentArtwork)
        } else if (artworkResolved) {
          // Only show the wireframe once we've confirmed there is no embedded artwork.
          // This prevents a one-frame sphere flash when switching back to video mode.
          WireframeSphere(
            paused = paused == true,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

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

      // ── Info row: Track N/M · ⏱1.00x · A·B ─────────────────────────────
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (playlistInfo != null) {
            Text(
              text = "Track $playlistInfo",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            )
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
        // Right side: Open current play queue  (same PlaylistSheet as video player)
        if (hasPlaylist) {
          IconButton(
            onClick = { onOpenSheet(Sheets.Playlist) },
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.QueueMusic,
              contentDescription = "Open queue",
              tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(spacing.small))

      // ── Seekbar (same component + settings as video player) ───────────────
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
        // Large play/pause
        Surface(
          onClick = { viewModel.pauseUnpause() },
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(64.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (paused == true) Icons.Filled.PlayArrow else Icons.Filled.Pause,
              contentDescription = if (paused == true) "Play" else "Pause",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(36.dp),
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
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Equalizer
        ActionBarButton(
          icon = { Icon(Icons.Filled.Tune, null) },
          active = equalizerState.isEnabled,
          onClick = { onOpenSheet(Sheets.Equalizer) },
        )
        // Shuffle
        ActionBarButton(
          icon = {
            Icon(
              if (shuffleEnabled) Icons.Filled.ShuffleOn else Icons.Filled.Shuffle,
              null,
            )
          },
          active = shuffleEnabled,
          onClick = { viewModel.toggleShuffle() },
        )
        // Repeat
        ActionBarButton(
          icon = {
            Icon(
              when (repeatMode) {
                RepeatMode.OFF -> Icons.Filled.Repeat
                RepeatMode.ONE -> Icons.Filled.RepeatOne
                RepeatMode.ALL -> Icons.Filled.RepeatOn
              },
              null,
            )
          },
          active = repeatMode != RepeatMode.OFF,
          onClick = { viewModel.cycleRepeatMode() },
        )
        // Sleep timer
        ActionBarButton(
          icon = { Icon(Icons.Filled.Timer, null) },
          active = sleepTimerTimeRemaining > 0,
          onClick = { onOpenSheet(Sheets.SleepTimer) },
        )
        // Sound / Volume
        val isMuted by MPVLib.propBoolean["mute"].collectAsState()
        ActionBarButton(
          icon = {
            Icon(
              if (isMuted == true) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
              null,
            )
          },
          active = isMuted == true,
          onClick = { viewModel.toggleMute() },
          onLongClick = { viewModel.displayVolumeSlider() }
        )
      }

      Spacer(modifier = Modifier.height(spacing.small))
    } // end Column

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
      if (showLoadingCircle) {
        LoadingIndicator(modifier = Modifier.size(96.dp))
      }
    }
  }

  // Audio Properties sheet
  if (showAudioProperties) {
    AudioPropertiesSheet(path = currentPath, onDismiss = { showAudioProperties = false })
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wireframe sphere  — drawn on Canvas with a slow rotation when playing
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders a 3-D wireframe sphere using latitude/longitude lines projected
 * with a simple orthographic projection.  When playing the sphere slowly
 * rotates on the Y-axis; when paused the rotation freezes.
 *
 * The visual matches the matrix-dot sphere shown in the screenshot: a dense
 * mesh of thin lines that gives a "3-D globe" impression.
 */
@Composable
fun WireframeSphere(
  paused: Boolean,
  modifier: Modifier = Modifier,
  color: Color = Color(0xFFD4A574),      // warm amber to match the screenshot
  lineAlpha: Float = 0.55f,
  latitudeBands: Int = 18,
  longitudeBands: Int = 24,
) {
  val transition = rememberInfiniteTransition(label = "sphere_rot")
  val rotationY by transition.animateFloat(
    initialValue = 0f,
    targetValue  = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation  = tween(durationMillis = 12_000, easing = LinearEasing),
      repeatMode = AnimRepeatMode.Restart,
    ),
    label = "sphere_rotY",
  )

  // When paused freeze the sphere by not advancing the angle — we still need
  // the composable to call the animated value so we just remember the last
  // frame when it was playing.
  var frozenAngle by remember { mutableStateOf(0f) }
  val angle = if (!paused) {
    frozenAngle = rotationY
    rotationY
  } else {
    frozenAngle
  }

  val strokeColor = color.copy(alpha = lineAlpha)

  Canvas(modifier = modifier.background(Color.Transparent)) {
    drawWireframeSphere(
      scope         = this,
      cx            = size.width / 2f,
      cy            = size.height / 2f,
      radius        = minOf(size.width, size.height) / 2f * 0.88f,
      rotationY     = angle,
      latBands      = latitudeBands,
      lonBands      = longitudeBands,
      color         = strokeColor,
    )
  }
}

private fun drawWireframeSphere(
  scope: DrawScope,
  cx: Float,
  cy: Float,
  radius: Float,
  rotationY: Float,
  latBands: Int,
  lonBands: Int,
  color: Color,
) {
  val strokeWidth = 1.2f

  // Helper: 3-D point → 2-D screen with simple Y-axis rotation + orthographic projection
  fun project(lat: Float, lon: Float): Offset {
    val x0 = cos(lat) * cos(lon)
    val y0 = sin(lat)
    val z0 = cos(lat) * sin(lon)
    // Rotate around Y axis
    val xr = x0 * cos(rotationY) + z0 * sin(rotationY)
    val yr = y0
    return Offset(cx + xr * radius, cy - yr * radius)
  }

  val latStep = PI.toFloat() / latBands
  val lonStep = (2f * PI.toFloat()) / lonBands

  // Latitude lines
  for (i in 0..latBands) {
    val lat = -PI.toFloat() / 2f + i * latStep
    for (j in 0 until lonBands) {
      val lon1 = j * lonStep
      val lon2 = lon1 + lonStep
      val p1 = project(lat, lon1)
      val p2 = project(lat, lon2)
      scope.drawLine(color, p1, p2, strokeWidth = strokeWidth)
    }
  }

  // Longitude lines
  for (j in 0 until lonBands) {
    val lon = j * lonStep
    for (i in 0 until latBands) {
      val lat1 = -PI.toFloat() / 2f + i * latStep
      val lat2 = lat1 + latStep
      val p1 = project(lat1, lon)
      val p2 = project(lat2, lon)
      scope.drawLine(color, p1, p2, strokeWidth = strokeWidth)
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
private fun AudioCoverArt(bitmap: Bitmap?) {
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
    WireframeSphere(
      paused = false,
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

  // Snapshot all MPV properties reactively, keyed on `path` so they automatically
  // refresh when the track changes. No `initial = remember { ... }` synchronous reads —
  // those captured stale data from the previous song because MPV metadata arrives
  // asynchronously after file load and the remember block ran too early.
  val codec      by MPVLib.propString["audio-codec-name"].collectAsState()
  val fileFormat by MPVLib.propString["file-format"].collectAsState()
  val sampleRate by MPVLib.propInt["audio-params/samplerate"].collectAsState()
  val bitrateRaw by MPVLib.propInt["audio-bitrate"].collectAsState()
  val channels   by MPVLib.propString["audio-params/channels"].collectAsState()
  val propTitle  by MPVLib.propString["metadata/by-key/title"].collectAsState()
  val propArtist by MPVLib.propString["metadata/by-key/artist"].collectAsState()
  val propAlbumArtist by MPVLib.propString["metadata/by-key/album_artist"].collectAsState()
  val propAlbum  by MPVLib.propString["metadata/by-key/album"].collectAsState()

  // When the path changes (new track), MPV emits updated metadata to the property flows above.
  // The 150 ms delay lets MPV finish parsing the file's tags before the sheet first renders,
  // preventing a brief flash of empty/stale values on fast machines.
  val ready by produceState(initialValue = false, key1 = path) {
    kotlinx.coroutines.delay(150)
    value = true
  }

  val codecStr = if (ready) codec ?: "—" else "—"
  val formatStr = if (ready) fileFormat
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
  val titleStr = if (ready) propTitle
    ?: path?.substringAfterLast('/')?.substringBeforeLast('.') ?: "—" else "—"
  val artistStr = if (ready) propArtist ?: propAlbumArtist ?: "—" else "—"
  val albumStr  = if (ready) propAlbum ?: "—" else "—"

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



