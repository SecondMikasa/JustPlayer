package xyz.mpv.rex.ui.browser.miniplayer

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.mpv.rex.ui.player.PlayerActivity
import xyz.mpv.rex.ui.player.MediaPlaybackService
import xyz.mpv.rex.ui.player.HeadlessPlaybackController
import xyz.mpv.rex.ui.player.RepeatMode
import xyz.mpv.rex.preferences.PlayerPreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import `is`.xyz.mpv.MPVLib

data class MiniPlayerState(
  val isPlaybackActive: Boolean = false,
  val title: String = "",
  val artist: String = "",
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val isPaused: Boolean = false,
  val thumbnail: Bitmap? = null,
  val videoPath: String? = null,
  /** True when the current media is audio-only. Used by openPlayer() to pass the correct
   *  mode flag to PlayerActivity so it launches the audio UI instead of the video UI. */
  val isAudio: Boolean = false,
  val hasNext: Boolean = false,
  val hasPrevious: Boolean = false,
  val nextTitle: String? = null,
  val prevTitle: String? = null,
  val nextThumbnail: Bitmap? = null,
  val prevThumbnail: Bitmap? = null,
  val isExpanded: Boolean = false,
  val shuffleEnabled: Boolean = false,
  val repeatMode: RepeatMode = RepeatMode.OFF,
)

/**
 * Central state manager for the Mini Player component.
 * Coordinates real-time state between MediaPlaybackService, PlayerActivity, and MainScreen.
 */
class MiniPlayerStateManager : KoinComponent {
  private val playerPreferences: PlayerPreferences by inject()
  private val headlessPlaybackController: HeadlessPlaybackController by inject()

  private val _state = MutableStateFlow(MiniPlayerState())
  val state: StateFlow<MiniPlayerState> = _state.asStateFlow()

  @Volatile
  var onNextHandler: (() -> Unit)? = null

  @Volatile
  var onPreviousHandler: (() -> Unit)? = null

  init {
    runCatching {
      val initShuffle = playerPreferences.shuffleEnabled.get()
      val initRepeat = playerPreferences.repeatMode.get()
      _state.update {
        it.copy(
          shuffleEnabled = initShuffle,
          repeatMode = initRepeat,
        )
      }
    }
  }

  fun updateState(
    isPlaybackActive: Boolean = _state.value.isPlaybackActive,
    title: String? = null,
    artist: String? = null,
    currentPositionMs: Long = _state.value.currentPositionMs,
    durationMs: Long = _state.value.durationMs,
    isPaused: Boolean = _state.value.isPaused,
    thumbnail: Bitmap? = null,
    videoPath: String? = null,
    isAudio: Boolean = _state.value.isAudio,
    hasNext: Boolean = _state.value.hasNext,
    hasPrevious: Boolean = _state.value.hasPrevious,
    nextTitle: String? = _state.value.nextTitle,
    prevTitle: String? = _state.value.prevTitle,
    nextThumbnail: Bitmap? = null,
    prevThumbnail: Bitmap? = null,
    isExpanded: Boolean = _state.value.isExpanded,
    shuffleEnabled: Boolean = _state.value.shuffleEnabled,
    repeatMode: RepeatMode = _state.value.repeatMode,
    resetThumbnails: Boolean = false,
  ) {
    _state.update { current ->
      val effectiveVideoPath = videoPath ?: current.videoPath
      val trackChanged = videoPath != null && videoPath != current.videoPath
      val effectiveReset = resetThumbnails || trackChanged

      current.copy(
        isPlaybackActive = isPlaybackActive,
        title = if (trackChanged) (title ?: "") else (if (title.isNullOrBlank()) current.title else title),
        artist = if (trackChanged) (artist ?: "") else (if (artist.isNullOrBlank()) current.artist else artist),
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        isPaused = isPaused,
        thumbnail = if (effectiveReset) thumbnail else (thumbnail ?: current.thumbnail),
        videoPath = effectiveVideoPath,
        isAudio = isAudio,
        hasNext = hasNext,
        hasPrevious = hasPrevious,
        nextTitle = nextTitle,
        prevTitle = prevTitle,
        nextThumbnail = if (effectiveReset) nextThumbnail else (nextThumbnail ?: current.nextThumbnail),
        prevThumbnail = if (effectiveReset) prevThumbnail else (prevThumbnail ?: current.prevThumbnail),
        isExpanded = isExpanded,
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
      )
    }
  }

  fun setExpanded(expanded: Boolean) {
    _state.update { it.copy(isExpanded = expanded) }
  }

  fun togglePlayPause() {
    val newPaused = !_state.value.isPaused
    runCatching {
      MPVLib.setPropertyBoolean("pause", newPaused)
    }
    _state.update { it.copy(isPaused = newPaused) }
  }

  fun toggleShuffle() {
    val newShuffle = !_state.value.shuffleEnabled
    runCatching {
      playerPreferences.shuffleEnabled.set(newShuffle)
    }
    _state.update { it.copy(shuffleEnabled = newShuffle) }
    if (headlessPlaybackController.isSessionActive) {
      headlessPlaybackController.onShuffleToggled(newShuffle)
    }
  }

  fun cycleRepeatMode() {
    val nextMode = when (_state.value.repeatMode) {
      RepeatMode.OFF -> RepeatMode.ONE
      RepeatMode.ONE -> RepeatMode.ALL
      RepeatMode.ALL -> RepeatMode.OFF
    }
    runCatching {
      playerPreferences.repeatMode.set(nextMode)
      when (nextMode) {
        RepeatMode.OFF, RepeatMode.ALL -> {
          MPVLib.setPropertyString("loop-playlist", "no")
          MPVLib.setPropertyString("loop-file", "no")
        }
        RepeatMode.ONE -> {
          MPVLib.setPropertyString("loop-playlist", "no")
          MPVLib.setPropertyString("loop-file", "inf")
        }
      }
    }
    _state.update { it.copy(repeatMode = nextMode) }
    if (headlessPlaybackController.isSessionActive) {
      headlessPlaybackController.onRepeatModeChanged(nextMode)
    }
  }

  fun seekTo(positionMs: Long) {
    runCatching {
      MPVLib.setPropertyDouble("time-pos", positionMs / 1000.0)
    }
    _state.update { it.copy(currentPositionMs = positionMs) }
  }

  fun playNext() {
    val currentNextThumb = _state.value.nextThumbnail
    val currentNextTitle = _state.value.nextTitle
    if (!currentNextTitle.isNullOrBlank()) {
      MediaPlaybackService.thumbnail = currentNextThumb
      _state.update {
        it.copy(
          title = currentNextTitle,
          thumbnail = currentNextThumb,
          nextThumbnail = null,
          prevThumbnail = null,
        )
      }
    }
    val handler = onNextHandler
    if (handler != null) {
      handler.invoke()
    } else {
      runCatching {
        MPVLib.command("playlist-next")
      }
    }
  }

  fun playPrevious() {
    val currentPrevThumb = _state.value.prevThumbnail
    val currentPrevTitle = _state.value.prevTitle
    if (!currentPrevTitle.isNullOrBlank()) {
      MediaPlaybackService.thumbnail = currentPrevThumb
      _state.update {
        it.copy(
          title = currentPrevTitle,
          thumbnail = currentPrevThumb,
          nextThumbnail = null,
          prevThumbnail = null,
        )
      }
    }
    val handler = onPreviousHandler
    if (handler != null) {
      handler.invoke()
    } else {
      runCatching {
        MPVLib.command("playlist-prev")
      }
    }
  }

  @Volatile
  var savedPlayerIntent: Intent? = null

  @Volatile
  var onCloseHandler: (() -> Unit)? = null

  fun closeMiniPlayer(context: Context) {
    onCloseHandler?.invoke()
    runCatching {
      context.stopService(Intent(context, MediaPlaybackService::class.java))
    }
    clearState()
  }

  fun clearState() {
    savedPlayerIntent = null
    _state.update { it.copy(isPlaybackActive = false, isExpanded = false) }
  }

  fun openPlayer(context: Context) {
    val intent = Intent(context, PlayerActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
      // Tell PlayerActivity whether this is an audio-only session so it launches
      // AudioPlayerControls instead of the video player, regardless of file extension checks.
      putExtra("is_audio", _state.value.isAudio)
      // Direct mini player mode: hand the live headless MPV session over to PlayerActivity
      // instead of starting a fresh playback (which would double-create MPV).
      if (headlessPlaybackController.isSessionActive) {
        putExtra("attach_existing_session", true)
        putParcelableArrayListExtra("playlist", ArrayList(headlessPlaybackController.activeUris))
        putExtra("playlist_index", headlessPlaybackController.activeIndex)
        putExtra("title", headlessPlaybackController.activeTitle)
      }
    }
    context.startActivity(
      intent,
      ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, 0).toBundle()
    )
  }
}

