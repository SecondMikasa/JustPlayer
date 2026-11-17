package xyz.mpv.rex.utils.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Lightweight real-time audio amplitude tracker using AudioRecord.
 * Computes root-mean-square (RMS) energy to allow visualizers to react dynamically to music.
 */
object AudioAmplitudeTracker {
  @Volatile
  var currentAmplitude: Float = 0f
    private set

  @SuppressLint("MissingPermission")
  suspend fun startListening() = withContext(Dispatchers.IO) {
    val sampleRate = 44100
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    val bufferSize = maxOf(minBufSize, 1024)

    var audioRecord: AudioRecord? = null
    try {
      audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        channelConfig,
        audioFormat,
        bufferSize,
      )
      if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
        return@withContext
      }
      audioRecord.startRecording()
      val buffer = ShortArray(512)

      while (isActive) {
        val read = audioRecord.read(buffer, 0, buffer.size)
        if (read > 0) {
          var sumSquares = 0.0
          for (i in 0 until read) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
          }
          val rms = sqrt(sumSquares / read)
          // Normalize RMS: typical sound is 300..4000 in 16-bit PCM
          val normalized = (rms / 3500.0).coerceIn(0.0, 1.0).toFloat()
          // Smooth with decay: fast attack, smooth decay
          currentAmplitude = if (normalized > currentAmplitude) {
            currentAmplitude * 0.3f + normalized * 0.7f
          } else {
            currentAmplitude * 0.82f + normalized * 0.18f
          }
        }
        kotlinx.coroutines.delay(35)
      }
    } catch (_: Exception) {
      // Ignore if mic unavailable or cancelled
    } finally {
      runCatching {
        if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
          audioRecord.stop()
        }
        audioRecord?.release()
      }
      currentAmplitude = 0f
    }
  }
}

