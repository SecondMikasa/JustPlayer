package xyz.mpv.rex.ui.browser.playlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import xyz.mpv.rex.database.entities.PlaylistEntity
import xyz.mpv.rex.database.repository.PlaylistRepository
import xyz.mpv.rex.repository.MediaFileRepository
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.preferences.PlaylistSortType
import xyz.mpv.rex.preferences.SortOrder
import xyz.mpv.rex.preferences.MediaLayoutMode
import xyz.mpv.rex.ui.browser.base.BaseBrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

data class PlaylistWithCount(
  val playlist: PlaylistEntity,
  val itemCount: Int,
)

class PlaylistViewModel(
  application: Application,
) : BaseBrowserViewModel<PlaylistWithCount>(application),
  KoinComponent {
  private val repository: PlaylistRepository by inject()
  private val browserPreferences: BrowserPreferences by inject()

  val playlistsWithCount: StateFlow<List<PlaylistWithCount>> = items

  val playlistSortType = browserPreferences.playlistSortType.stateIn(viewModelScope)
  val playlistSortOrder = browserPreferences.playlistSortOrder.stateIn(viewModelScope)
  val playlistLayoutMode = browserPreferences.playlistLayoutMode.stateIn(viewModelScope)
  val gridColumnsPortrait = browserPreferences.folderGridColumnsPortrait.stateIn(viewModelScope)
  val gridColumnsLandscape = browserPreferences.folderGridColumnsLandscape.stateIn(viewModelScope)

  // Track if initial load has completed to prevent empty state flicker
  private val _hasCompletedInitialLoad = MutableStateFlow(false)
  val hasCompletedInitialLoad: StateFlow<Boolean> = _hasCompletedInitialLoad.asStateFlow()

  companion object {
    private const val TAG = "PlaylistViewModel"

    fun factory(application: Application) =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlaylistViewModel(application) as T
      }
  }

  init {
    loadData()

    // Observe all playlists and update items
    viewModelScope.launch(Dispatchers.IO) {
      repository.observeAllPlaylists().collectLatest {
        loadData()
      }
    }

    // Observe music playlist integration preference
    viewModelScope.launch(Dispatchers.IO) {
      browserPreferences.showMusicPlaylistsInMainTab.changes().collectLatest {
        loadData()
      }
    }
  }

  override fun loadData() {
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      try {
        val playlists = repository.getAllPlaylists()
        
        val currentSortType = browserPreferences.playlistSortType.get()
        val currentSortOrder = browserPreferences.playlistSortOrder.get()
        val showMusicPlaylists = browserPreferences.showMusicPlaylistsInMainTab.get()

        // Filter out music playlists if the user toggled them off
        val filteredPlaylists = if (showMusicPlaylists) playlists
          else playlists.filter { !it.isMusicPlaylist }

        val playlistsWithCounts = filteredPlaylists.map { playlist ->
          val count = repository.getPlaylistItemCount(playlist.id)
          PlaylistWithCount(playlist, count)
        }
        
        val sortedList = when (currentSortType) {
          PlaylistSortType.Title -> playlistsWithCounts.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.playlist.name })
          PlaylistSortType.DateCreated -> playlistsWithCounts.sortedBy { it.playlist.createdAt }
          PlaylistSortType.DateUpdated -> playlistsWithCounts.sortedBy { it.playlist.updatedAt }
          PlaylistSortType.ItemCount -> playlistsWithCounts.sortedBy { it.itemCount }
        }.let {
          if (currentSortOrder == SortOrder.Descending) it.reversed() else it
        }

        _items.value = sortedList
        _hasCompletedInitialLoad.value = true
      } finally {
        _isLoading.value = false
      }
    }
  }

  override fun refresh(silent: Boolean) {
    loadData()
  }

  suspend fun createPlaylist(name: String): Long {
    return repository.createPlaylist(name)
  }

  suspend fun deletePlaylists(playlistsToDelete: List<PlaylistWithCount>) {
    playlistsToDelete.forEach {
      repository.deletePlaylist(it.playlist)
    }
    loadData()
  }

  fun setSortType(type: PlaylistSortType) {
    if (browserPreferences.playlistSortType.get() == type) {
      val newOrder = if (browserPreferences.playlistSortOrder.get() == SortOrder.Ascending) SortOrder.Descending else SortOrder.Ascending
      browserPreferences.playlistSortOrder.set(newOrder)
    } else {
      browserPreferences.playlistSortType.set(type)
      browserPreferences.playlistSortOrder.set(SortOrder.Ascending)
    }
    loadData()
  }

  fun setSortOrder(order: SortOrder) {
    browserPreferences.playlistSortOrder.set(order)
    loadData()
  }

  fun setLayoutMode(mode: MediaLayoutMode) {
    browserPreferences.playlistLayoutMode.set(mode)
  }
}
