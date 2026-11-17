package xyz.mpv.rex.utils.media

import android.media.audiofx.Visualizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/** Number of log-spaced spectrum bins exposed to the visualizers. */
const val SPECTRUM_BINS = 16
const val SPECTROGRAM_COLS = 80

/**
 * One raw analysis snapshot. Values are un-normalised magnitudes (roughly 0..1, but quiet
 * tracks stay well below 1) — [BandProcessor] applies auto-gain, envelopes and beat detection.
 */
class RawBands(
  val bass: Float,
  val mid: Float,
  val treble: Float,
  val overall: Float,
  /** [SPECTRUM_BINS] log-spaced magnitudes, or null when only a mic amplitude is available. */
  val spectrum: FloatArray?,
  /** True when derived from the microphone amplitude (no real frequency data). */
  val fromMic: Boolean,
)

/**
 * Source of raw audio analysis.
 *
 * Preferred path: [Visualizer] on the output mix (real FFT of what is *playing*, no room
 * noise, much lower latency than the mic).
 * Fallback: microphone amplitude via [AudioAmplitudeTracker].
 * If neither is running, [sample] returns null and the UI plays its synthetic idle animation.
 */
object AudioAnalyzer {
  enum class Source { NONE, VISUALIZER, MIC }

  @Volatile
  var source: Source = Source.NONE
    private set

  /** True once the Visualizer has delivered non-silent data since it was started. */
  @Volatile
  var visualizerSignalSeen: Boolean = false
    private set

  @Volatile
  private var latest: RawBands? = null
  private var visualizer: Visualizer? = null

  /** Latest waveform snapshot for the oscilloscope visualizer; null before first capture. */
  @Volatile
  var latestWaveform: FloatArray? = null
    private set

  /**
   * Circular history of spectrum snapshots for the spectrogram visualizer.
   * Each entry is a FloatArray of size SPECTRUM_BINS. Newest entry is last.
   */
  val spectrogramHistory: ArrayDeque<FloatArray> = ArrayDeque(SPECTROGRAM_COLS)

  /**
   * Attaches a [Visualizer] to [audioSessionId] (0 = global output mix).
   * Requires RECORD_AUDIO. Returns false if the device/OS refuses (common on newer Android
   * for session 0) — the caller should then fall back to the mic.
   * If you can obtain mpv's real audio session id, pass it here for best results.
   */
  fun startVisualizer(audioSessionId: Int = 0): Boolean {
    stopVisualizer()
    var v: Visualizer? = null
    return try {
      v = Visualizer(audioSessionId)
      v.enabled = false
      v.captureSize = Visualizer.getCaptureSizeRange()[1]
      v.setDataCaptureListener(fftListener, Visualizer.getMaxCaptureRate(), true, true)
      v.enabled = true
      visualizer = v
      visualizerSignalSeen = false
      latest = null
      source = Source.VISUALIZER
      true
    } catch (e: Exception) {
      runCatching { v?.release() }
      false
    }
  }

  fun stopVisualizer() {
    runCatching {
      visualizer?.enabled = false
      visualizer?.release()
    }
    visualizer = null
    latest = null
    latestWaveform = null
    synchronized(spectrogramHistory) { spectrogramHistory.clear() }
    if (source == Source.VISUALIZER) source = Source.NONE
  }

  /** Marks the mic amplitude as the active source (caller runs AudioAmplitudeTracker). */
  fun setMicActive() {
    stopVisualizer()
    source = Source.MIC
  }

  fun stopAll() {
    stopVisualizer()
    source = Source.NONE
  }

  /** Latest raw snapshot, or null when no audio source is running. */
  fun sample(): RawBands? = when (source) {
    Source.NONE -> null
    Source.VISUALIZER -> latest
    Source.MIC -> {
      val amp = AudioAmplitudeTracker.currentAmplitude.coerceIn(0f, 1f)
      RawBands(amp, amp, 0f, amp, null, fromMic = true)
    }
  }

  private val fftListener = object : Visualizer.OnDataCaptureListener {
    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
      if (waveform == null || waveform.isEmpty()) return
      // Normalize to -1..1 (the byte is unsigned centered at 128)
      val normalized = FloatArray(waveform.size) { i -> (waveform[i].toInt() and 0xFF - 128) / 128f }
      latestWaveform = normalized
    }

    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
      if (fft == null || fft.size < 4) return
      val n = fft.size / 2
      val binHz = (samplingRate / 1000f) / fft.size // fs / N

      // fft layout: [Re0, ReNyquist, Re1, Im1, Re2, Im2, ...]
      val mags = FloatArray(n)
      for (k in 1 until n) {
        val re = fft[2 * k].toInt()
        val im = fft[2 * k + 1].toInt()
        mags[k] = hypot(re.toFloat(), im.toFloat()) / 181f // 181 ≈ max magnitude of a signed byte pair
      }

      fun avg(loHz: Float, hiHz: Float): Float {
        val a = (loHz / binHz).roundToInt().coerceIn(1, n - 1)
        val b = (hiHz / binHz).roundToInt().coerceIn(a, n - 1)
        var s = 0f
        for (i in a..b) s += mags[i]
        return s / (b - a + 1)
      }

      val bass = avg(20f, 250f)
      val mid = avg(250f, 4000f)      // vocals + most melodic content
      val treble = avg(4000f, 16000f) // hats, air, sibilance

      val spectrum = FloatArray(SPECTRUM_BINS)
      val ratio = 16000f / 40f
      for (i in 0 until SPECTRUM_BINS) {
        val lo = 40f * ratio.pow(i.toFloat() / SPECTRUM_BINS)
        val hi = 40f * ratio.pow((i + 1f) / SPECTRUM_BINS)
        // Frequency tilt compensation: natural music FFT drops off with frequency (~1/f).
        // Scaling up higher frequencies balances the visual spectrum across all bands.
        val tilt = 1f + (i.toFloat() / (SPECTRUM_BINS - 1)).pow(1.1f) * 2.8f
        spectrum[i] = avg(lo, hi) * tilt
      }

      val overall = (bass + mid + treble) / 3f
      if (overall > 0.01f) visualizerSignalSeen = true

      latest = RawBands(bass, mid, treble, overall, spectrum, fromMic = false)
    }
  }
}

/** Frame-rate independent one-pole follower: fast attack, slow release. */
internal fun follow(current: Float, target: Float, dt: Float, attackTau: Float, releaseTau: Float): Float {
  val tau = if (target > current) attackTau else releaseTau
  val k = 1f - exp(-dt / tau)
  return current + (target - current) * k
}

/**
 * Turns raw analysis into visualizer-ready signals: per-band auto-gain (every track looks
 * expressive regardless of loudness), asymmetric envelopes, spectrum smoothing and a simple
 * bass beat detector.
 */
class BandProcessor {
  var bass = 0f; private set
  var mid = 0f; private set
  var treble = 0f; private set
  var overall = 0f; private set

  /** 1.0 on a detected kick, decaying to 0 over ~250 ms. */
  var beat = 0f; private set
  var beatTriggered = false; private set
  val spectrum = FloatArray(SPECTRUM_BINS)

  private val peak = floatArrayOf(MIN_PEAK, MIN_PEAK, MIN_PEAK)
  private val specPeaks = FloatArray(SPECTRUM_BINS) { MIN_PEAK }
  private var bassAvg = 0f
  private var cooldown = 0f
  private var micSlow = 0f

  val isSettled: Boolean
    get() = bass < 0.002f && mid < 0.002f && treble < 0.002f && beat < 0.002f &&
      spectrum.all { it < 0.002f }

  private fun normalize(i: Int, v: Float, dt: Float): Float {
    // Slowly decaying peak tracker = auto-gain.
    peak[i] = max(max(peak[i] * exp(-dt * 0.3f), v), MIN_PEAK)
    return (v / peak[i]).coerceIn(0f, 1f)
  }

  fun update(dt: Float, raw: RawBands?, active: Boolean, time: Float = 0f, paused: Boolean = false) {
    beatTriggered = false
    cooldown = (cooldown - dt).coerceAtLeast(0f)
    beat = (beat - dt * 4f).coerceAtLeast(0f)

    if (paused) {
      // Settle smoothly instead of freezing mid-pulse.
      bass = follow(bass, 0f, dt, 0.05f, 0.25f)
      mid = follow(mid, 0f, dt, 0.05f, 0.30f)
      treble = follow(treble, 0f, dt, 0.05f, 0.18f)
      overall = bass * 0.4f + mid * 0.4f + treble * 0.2f
      for (i in spectrum.indices) spectrum[i] = follow(spectrum[i], 0f, dt, 0.05f, 0.25f)
      return
    }

    if (!active || raw == null) {
      // Playback is active but no real-time audio stream (e.g. mic disabled or waiting for signal).
      // Provide an organic, elegant idle wave so bars are visibly alive rather than frozen flat!
      val idleBass = (kotlin.math.sin(time * 2.2f) * 0.15f + 0.18f).coerceIn(0f, 1f)
      val idleMid = (kotlin.math.cos(time * 1.8f) * 0.12f + 0.15f).coerceIn(0f, 1f)
      val idleTreble = (kotlin.math.sin(time * 3.1f) * 0.10f + 0.12f).coerceIn(0f, 1f)
      bass = follow(bass, idleBass, dt, 0.05f, 0.20f)
      mid = follow(mid, idleMid, dt, 0.05f, 0.20f)
      treble = follow(treble, idleTreble, dt, 0.05f, 0.20f)
      overall = bass * 0.4f + mid * 0.4f + treble * 0.2f

      for (i in spectrum.indices) {
        val idleWave = (
          kotlin.math.sin(time * 2.8f + i * 0.45f) * 0.22f +
          kotlin.math.cos(time * 1.5f - i * 0.30f) * 0.14f +
          0.26f
        ).coerceIn(0.06f, 0.65f)
        spectrum[i] = follow(spectrum[i], idleWave, dt, 0.04f, 0.18f)
      }
      return
    }

    var b = raw.bass
    var m = raw.mid
    var t = raw.treble
    if (raw.fromMic) {
      // Only an amplitude is available: approximate the bands.
      // bass/mid follow the level; treble follows transients (level above its slow average).
      micSlow += (raw.overall - micSlow) * (1f - exp(-dt * 3f))
      b = raw.overall
      m = raw.overall * 0.85f
      t = (raw.overall - micSlow).coerceAtLeast(0f) * 3f
    }

    val bn = normalize(0, b, dt)
    val mn = normalize(1, m, dt)
    val tn = normalize(2, t, dt)

    // Beat detection on the *un-smoothed* normalised bass.
    bassAvg += (bn - bassAvg) * (1f - exp(-dt * 2.5f))
    if (bn > bassAvg * 1.35f + 0.06f && bn > 0.3f && cooldown <= 0f) {
      beat = 1f
      beatTriggered = true
      cooldown = 0.2f
    }

    bass = follow(bass, bn, dt, 0.035f, 0.22f)
    mid = follow(mid, mn, dt, 0.050f, 0.30f)
    treble = follow(treble, tn, dt, 0.025f, 0.15f)
    overall = bass * 0.4f + mid * 0.4f + treble * 0.2f

    val spec = raw.spectrum
    if (spec != null) {
      // Real FFT spectrum available from Visualizer
      for (i in spectrum.indices) {
        val v = spec[i]
        specPeaks[i] = max(max(specPeaks[i] * exp(-dt * 0.6f), v), 0.015f)
        val norm = (v / specPeaks[i]).coerceIn(0f, 1f)
        // Perceptual dynamic compression (sqrt curve) so quiet-to-mid sounds are clearly visible
        val compressed = kotlin.math.sqrt(norm)
        // Bass kick injection on lower frequency bins for punchiness
        val kick = if (i < 4) beat * 0.22f * (1f - i / 4f) else 0f
        // Golden-ratio jitter: gives each bin a unique, stable small offset so adjacent bins
        // that map to the same FFT slot still have distinguishable target heights.
        val jitter = kotlin.math.sin(i * 2.39996f) * 0.015f
        val target = (compressed * 0.85f + kick + jitter).coerceIn(0f, 1f)
        spectrum[i] = follow(spectrum[i], target, dt, 0.025f, 0.16f)
      }
    } else if (raw.fromMic) {
      // Synthesize 16 spectrum bins from microphone amplitude and frequency models
      for (i in spectrum.indices) {
        val x = i.toFloat() / (SPECTRUM_BINS - 1)
        val bassPart = (bn * (1f - x * 1.6f).coerceAtLeast(0f)) + (beat * (1f - x * 0.8f).coerceAtLeast(0f) * 0.45f)
        val midPart = mn * (1f - kotlin.math.abs(x - 0.42f) / 0.45f).coerceAtLeast(0f)
        val treblePart = tn * ((x - 0.52f) / 0.48f).coerceAtLeast(0f)
        val ripple = kotlin.math.sin(time * 3.5f + i * 0.75f) * 0.10f * mn
        val combined = (bassPart * 0.95f + midPart * 0.90f + treblePart * 0.85f + ripple).coerceIn(0f, 1f)
        val target = combined.pow(0.75f)
        spectrum[i] = follow(spectrum[i], target, dt, 0.025f, 0.16f)
      }
    } else {
      for (i in spectrum.indices) spectrum[i] = follow(spectrum[i], 0f, dt, 0.05f, 0.25f)
    }
  }

  private companion object {
    const val MIN_PEAK = 0.03f
  }
}

/**
 * Everything a visualizer needs per frame, advanced from a single frame-clock loop
 * ([rememberBandState]). Fields are plain values; read [tick] first inside draw / graphicsLayer
 * blocks so Compose re-runs them every frame without recomposing.
 */
@Stable
class BandState {
  /** Incremented every frame; read it in a draw scope to subscribe to updates. */
  var tick by mutableLongStateOf(0L)
    private set

  private val processor = BandProcessor()

  val bass: Float get() = processor.bass
  val mid: Float get() = processor.mid
  val treble: Float get() = processor.treble
  val overall: Float get() = processor.overall
  val beat: Float get() = processor.beat
  val spectrum: FloatArray get() = processor.spectrum

  /** True during the single frame when a beat was detected. */
  val beatTriggered: Boolean get() = processor.beatTriggered

  /** 0 = synthetic idle animation only, 1 = fully driven by real audio. */
  var audioMix = 0f; private set

  // Monotonic accumulators — continuous (no modulo wraps), but speed follows the music.
  var time = 0f; private set      // constant-rate idle clock
  var phase = 0f; private set     // morph speed driven by mid (vocals/melody)
  var rotation = 0f; private set  // rotation speed driven by treble
  var orbit = 0f; private set     // orbit speed driven by overall energy
  var spin = 0f; private set      // wireframe spin driven by mid

  /** Ages (0..1) of beat-spawned rings; negative = free slot. */
  val ringAges = FloatArray(MAX_RINGS) { -1f }
  private var timeSinceLastSpecSnapshot = 0f

  val isSettled: Boolean get() = processor.isSettled && audioMix < 0.01f

  /** The dt (seconds) consumed by the most recent step() call. Safe to read from draw code. */
  var lastDt: Float = 0.016f
    private set

  fun step(dt: Float, raw: RawBands?, paused: Boolean) {
    lastDt = dt
    if (!paused) {
      time += dt
      phase += dt * (1f + processor.mid * 2.2f)
      rotation += dt * (1f + processor.treble * 3f)
      orbit += dt * (0.85f + processor.overall * 0.8f)
      spin += dt * (0.6f + processor.mid * 1.6f)
      for (i in ringAges.indices) {
        if (ringAges[i] >= 0f) {
          ringAges[i] += dt / 1.3f
          if (ringAges[i] > 1f) ringAges[i] = -1f
        }
      }
    }

    val active = raw != null && !paused
    processor.update(dt, raw, active, time, paused)
    audioMix = follow(audioMix, if (active) 1f else 0f, dt, 0.4f, 0.5f)

    if (!paused) {
      timeSinceLastSpecSnapshot += dt
      if (timeSinceLastSpecSnapshot >= 0.04f) { // ~25 snapshots/sec for smooth 3.2s scroll
        timeSinceLastSpecSnapshot = 0f
        synchronized(AudioAnalyzer.spectrogramHistory) {
          if (AudioAnalyzer.spectrogramHistory.size >= SPECTROGRAM_COLS) {
            AudioAnalyzer.spectrogramHistory.removeFirst()
          }
          AudioAnalyzer.spectrogramHistory.addLast(processor.spectrum.copyOf())
        }
      }
    }

    if (processor.beatTriggered) {
      var slot = ringAges.indexOfFirst { it < 0f }
      if (slot < 0) {
        slot = 0
        for (i in ringAges.indices) if (ringAges[i] > ringAges[slot]) slot = i
      }
      ringAges[slot] = 0f
    }
    tick++
  }

  private companion object {
    const val MAX_RINGS = 6
  }
}

/**
 * Creates a [BandState] and drives it from one frame-clock loop (no polling jitter).
 * The loop sleeps once playback is paused and all envelopes have settled.
 */
@Composable
fun rememberBandState(paused: Boolean, silent: Boolean = false): BandState {
  val state = remember { BandState() }
  LaunchedEffect(paused, silent) {
    var last = 0L
    while (isActive) {
      withFrameNanos { nanos ->
        val dt = if (last == 0L) 0f else ((nanos - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
        last = nanos
        state.step(dt, AudioAnalyzer.sample(), paused || silent)
      }
      if ((paused || silent) && state.isSettled) break
    }
  }
  return state
}
