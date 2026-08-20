package xyz.mpv.rex.ui.browser.music

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import xyz.mpv.rex.database.repository.HybridMediaIndexRepository
import xyz.mpv.rex.domain.media.model.MusicAlbum
import xyz.mpv.rex.domain.media.model.MusicArtist
import xyz.mpv.rex.domain.media.model.MusicSortField
import xyz.mpv.rex.domain.media.model.MusicSortOrder
import xyz.mpv.rex.domain.media.model.MusicTab
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.ui.browser.base.BaseBrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Groups a song into its album bucket. Prefers the MediaStore ALBUM_ID when present
 * (fast path, always unique); falls back to a hash of (album, artist) for songs
 * that were indexed without MediaStore (SAF/custom folders), so albums still group
 * sensibly even without a numeric id.
 */
private fun Video.albumKey(): Long = albumId.takeIf { it != 0L } ?: (album to artist).hashCode().toLong()

private fun Video.artistKey(): String = artist.ifBlank { "Unknown Artist" }

class MusicLibraryViewModel(
  application: Application,
) : BaseBrowserViewModel<Video>(application),
  KoinComponent {
  private val hybridMediaIndexRepository: HybridMediaIndexRepository by inject()

  /** All audio items, alias of the base class's [items] for readability at call sites. */
  val songs: StateFlow<List<Video>> = items

  private val _selectedTab = MutableStateFlow(MusicTab.SONGS)
  val selectedTab: StateFlow<MusicTab> = _selectedTab.asStateFlow()

  private val _sortField = MutableStateFlow(MusicSortField.TITLE)
  val sortField: StateFlow<MusicSortField> = _sortField.asStateFlow()

  private val _sortOrder = MutableStateFlow(MusicSortOrder.ASCENDING)
  val sortOrder: StateFlow<MusicSortOrder> = _sortOrder.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  /** Songs filtered by [searchQuery] and sorted by [sortField]/[sortOrder]. */
  val filteredSongs: StateFlow<List<Video>> =
    combine(songs, searchQuery, sortField, sortOrder) { allSongs, query, field, order ->
      val filtered = if (query.isBlank()) {
        allSongs
      } else {
        allSongs.filter {
          it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
      }
      val sorted = when (field) {
        MusicSortField.TITLE -> filtered.sortedBy { it.title.lowercase() }
        MusicSortField.ARTIST -> filtered.sortedBy { it.artistKey().lowercase() }
        MusicSortField.ALBUM -> filtered.sortedBy { it.album.lowercase() }
        MusicSortField.DURATION -> filtered.sortedBy { it.duration }
        MusicSortField.DATE_ADDED -> filtered.sortedBy { it.dateAdded }
        MusicSortField.YEAR -> filtered.sortedBy { it.year }
      }
      if (order == MusicSortOrder.DESCENDING) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  /** Albums derived from the currently loaded songs (see [Video.albumKey]). */
  val albums: StateFlow<List<MusicAlbum>> =
    songs.combine(searchQuery) { allSongs, query ->
      val filtered = if (query.isBlank()) {
        allSongs
      } else {
        allSongs.filter { it.album.contains(query, ignoreCase = true) || it.artistKey().contains(query, ignoreCase = true) }
      }
      filtered.groupBy { it.albumKey() }
        .map { (key, songsInAlbum) ->
          val sample = songsInAlbum.first()
          MusicAlbum(
            id = key,
            title = sample.album.ifBlank { "Unknown Album" },
            artist = sample.artistKey(),
            songCount = songsInAlbum.size,
            year = songsInAlbum.maxOf { it.year },
          )
        }
        .sortedBy { it.title.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  /** Artists derived from the currently loaded songs. */
  val artists: StateFlow<List<MusicArtist>> =
    songs.combine(searchQuery) { allSongs, query ->
      val filtered = if (query.isBlank()) {
        allSongs
      } else {
        allSongs.filter { it.artistKey().contains(query, ignoreCase = true) }
      }
      filtered.groupBy { it.artistKey() }
        .map { (artistName, songsByArtist) ->
          MusicArtist(
            name = artistName,
            songCount = songsByArtist.size,
            albumCount = songsByArtist.map { it.albumKey() }.distinct().size,
          )
        }
        .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val tag = "MusicLibraryViewModel"

  init {
    loadData()
  }

  override fun loadData() {
    // Collect the reactive Flow from Room so the list updates in-place whenever background
    // metadata enrichment writes real durations back to the database — no manual refresh needed.
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      try {
        hybridMediaIndexRepository.getAllSongsFlow().collectLatest { songs ->
          _items.value = songs
          _isLoading.value = false
        }
      } catch (e: Exception) {
        Log.e(tag, "Error observing music library", e)
        _isLoading.value = false
      }
    }
    // Kick off background enrichment for any PENDING items (SAF / direct-scan files whose
    // duration was not available from MediaStore at index time).
    viewModelScope.launch(Dispatchers.IO) {
      hybridMediaIndexRepository.startBackgroundEnrichment()
    }
  }

  override fun refresh(silent: Boolean) {
    // The Flow observer already picks up every DB change, so refresh only needs to
    // re-trigger enrichment for newly added files that are still PENDING.
    viewModelScope.launch(Dispatchers.IO) {
      hybridMediaIndexRepository.startBackgroundEnrichment()
    }
  }

  fun selectTab(tab: MusicTab) {
    _selectedTab.value = tab
  }

  fun setSortField(field: MusicSortField) {
    _sortField.value = field
  }

  fun toggleSortOrder() {
    _sortOrder.value = if (_sortOrder.value == MusicSortOrder.ASCENDING) MusicSortOrder.DESCENDING else MusicSortOrder.ASCENDING
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun songsForAlbum(albumId: Long): List<Video> =
    songs.value
      .filter { it.albumKey() == albumId }
      .sortedWith(compareBy({ it.trackNumber.takeIf { n -> n != 0 } ?: Int.MAX_VALUE }, { it.title.lowercase() }))

  fun songsForArtist(artistName: String): List<Video> =
    songs.value.filter { it.artistKey() == artistName }

  companion object {
    fun factory(application: Application) = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T = MusicLibraryViewModel(application) as T
    }
  }
}
