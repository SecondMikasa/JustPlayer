package xyz.mpv.rex.utils.media

import android.util.Log
import `is`.xyz.mpv.MPVLib
import xyz.mpv.rex.preferences.AudioChannels
import xyz.mpv.rex.preferences.AudioPreferences

class AudioEqualizerManager(
  private val audioPreferences: AudioPreferences,
) {
  companion object {
    private const val TAG = "AudioEqualizerManager"
    val FREQUENCIES = listOf(60, 230, 910, 3600, 14000)
  }

  private var isEnabled = false
  private var bandGains = listOf(0, 0, 0, 0, 0)
  private var volumeBoostDb = 0

  init {
    isEnabled = audioPreferences.equalizerEnabled.get()
    val gainsStr = audioPreferences.equalizerGains.get()
    bandGains = gainsStr.split(",")
      .mapNotNull { it.toIntOrNull() }
      .takeIf { it.size == 5 } ?: listOf(0, 0, 0, 0, 0)
    volumeBoostDb = audioPreferences.equalizerVolumeBoost.get()
  }

  fun initSession(sessionId: Int = 0) {
    applyAudioFilters()
  }

  fun updateState(
    enabled: Boolean,
    bandGains: List<Int>,
    volumeBoostDb: Int,
  ) {
    this.isEnabled = enabled
    this.bandGains = bandGains
    this.volumeBoostDb = volumeBoostDb
    applyAudioFilters()
  }

  fun buildCurrentFilterString(): String {
    val filters = mutableListOf<String>()

    val volumeNorm = runCatching { audioPreferences.volumeNormalization.get() }.getOrDefault(false)
    if (volumeNorm) {
      filters.add("dynaudnorm")
    }

    val channels = runCatching { audioPreferences.audioChannels.get() }.getOrDefault(AudioChannels.AutoSafe)
    if (channels == AudioChannels.ReverseStereo) {
      filters.add("pan=[stereo|c0=c1|c1=c0]")
    }

    if (isEnabled) {
      val lavfiChain = mutableListOf<String>()

      FREQUENCIES.forEachIndexed { index, freq ->
        val gain = bandGains.getOrElse(index) { 0 }
        if (gain != 0) {
          lavfiChain.add("equalizer=f=$freq:width_type=o:w=1:g=$gain")
        }
      }

      if (volumeBoostDb > 0) {
        lavfiChain.add("volume=volume=${volumeBoostDb}dB")
      }

      if (lavfiChain.isNotEmpty()) {
        filters.add("lavfi=[${lavfiChain.joinToString(",")}]")
      }
    }

    return filters.joinToString(",")
  }

  fun applyAudioFilters() {
    val filterString = buildCurrentFilterString()
    try {
      MPVLib.setPropertyString("af", filterString)
      if (filterString.isNotBlank()) {
        runCatching { MPVLib.command("af", "set", filterString) }
      } else {
        runCatching { MPVLib.command("af", "clr", "") }
      }
      Log.d(TAG, "Applied audio filters to MPV: '$filterString'")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to apply audio filters: ${e.message}")
    }
  }

  fun release() {
    // Keep filter state or clear if needed
  }
}
