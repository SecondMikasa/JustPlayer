package xyz.mpv.rex.ui.player

enum class EqualizerPreset(
  val displayName: String,
  val gains: List<Int>,
) {
  FLAT("Flat", listOf(0, 0, 0, 0, 0)),
  ROCK("Rock", listOf(4, 2, -1, 2, 4)),
  POP("Pop", listOf(-1, 2, 4, 2, -1)),
  JAZZ("Jazz", listOf(3, 2, -1, 2, 3)),
  CLASSICAL("Classical", listOf(3, 1, -1, 2, 3)),
  ELECTRONIC("Electronic", listOf(5, 3, 0, 2, 4)),
  BASS_BOOST("Bass Boost", listOf(5, 3, 0, -1, -2)),
  TREBLE_BOOST("Treble Boost", listOf(-2, -1, 0, 3, 5)),
  VOICE_BOOST("Voice Boost", listOf(2, 4, 5, 3, 1)),
  LOUDNESS("Loudness", listOf(4, 2, 0, 2, 4)),
  CUSTOM("Custom", listOf(0, 0, 0, 0, 0)),
  ;

  companion object {
    val MUSIC =
      listOf(
        FLAT,
        ROCK,
        POP,
        JAZZ,
        CLASSICAL,
        ELECTRONIC,
        BASS_BOOST,
        TREBLE_BOOST,
        VOICE_BOOST,
        LOUDNESS,
      )
  }
}

data class EqualizerState(
  val isEnabled: Boolean = false,
  val currentPreset: EqualizerPreset = EqualizerPreset.FLAT,
  val bandGains: List<Int> = List(5) { 0 },
  val volumeBoostDb: Int = 0,
)
