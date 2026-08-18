package xyz.mpv.rex.domain.media.model

import androidx.compose.runtime.Immutable

/**
 * Top-level tabs within the Music library screen.
 * Songs/Albums/Artists are derived from [Video] items where isAudio == true;
 * Playlists reuses the existing playlist feature (playlists aren't video-specific).
 */
@Immutable
enum class MusicTab(val title: String) {
  SONGS("Songs"),
  ALBUMS("Albums"),
  ARTISTS("Artists"),
  PLAYLISTS("Playlists"),
}

@Immutable
data class MusicAlbum(
  val id: Long,
  val title: String,
  val artist: String,
  val songCount: Int,
  val year: Int = 0,
)

@Immutable
data class MusicArtist(
  val name: String,
  val songCount: Int,
  val albumCount: Int,
)

@Immutable
enum class MusicSortField(val displayName: String) {
  TITLE("Title"),
  ARTIST("Artist"),
  ALBUM("Album"),
  DURATION("Duration"),
  DATE_ADDED("Date Added"),
  YEAR("Year"),
}

@Immutable
enum class MusicSortOrder {
  ASCENDING,
  DESCENDING,
}
