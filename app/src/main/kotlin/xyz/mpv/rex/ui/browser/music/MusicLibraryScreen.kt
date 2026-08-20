package xyz.mpv.rex.ui.browser.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.mpv.rex.R
import xyz.mpv.rex.domain.media.model.MusicAlbum
import xyz.mpv.rex.domain.media.model.MusicArtist
import xyz.mpv.rex.domain.media.model.MusicSortField
import xyz.mpv.rex.domain.media.model.MusicSortOrder
import xyz.mpv.rex.domain.media.model.MusicTab
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.preferences.UiSettings
import xyz.mpv.rex.ui.browser.LocalNavigationBarHeight
import xyz.mpv.rex.ui.browser.cards.VideoCard
import xyz.mpv.rex.ui.browser.components.BrowserBottomBar
import xyz.mpv.rex.ui.browser.components.BrowserTopBar
import xyz.mpv.rex.ui.browser.dialogs.AddToPlaylistDialog
import xyz.mpv.rex.ui.browser.dialogs.DeleteConfirmationDialog
import xyz.mpv.rex.ui.browser.dialogs.RenameDialog
import xyz.mpv.rex.ui.browser.playlist.PlaylistScreen
import xyz.mpv.rex.ui.browser.selection.rememberSelectionManager
import xyz.mpv.rex.utils.media.MediaUtils

/**
 * Music library tab: Songs / Albums / Artists / Playlists.
 *
 * Changes vs original:
 *  - SongList / ArtistList / AlbumGrid: bottom content padding uses
 *    [LocalNavigationBarHeight] so the last item is never hidden behind
 *    the bottom navigation bar.
 *  - Multi-select: long-press a song to enter selection mode.
 *    The shared [rememberSelectionManager] + [BrowserTopBar] +
 *    [BrowserBottomBar] components provide play, add-to-playlist, rename
 *    and delete — exactly the same as the video list screen.
 *  - Auto-playlist: tapping a song now passes `"media_library_list"` as
 *    the launch source so PlayerActivity calls generateMediaLibraryPlaylist()
 *    and the whole music library becomes the play queue.
 */
object MusicLibraryScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: MusicLibraryViewModel = viewModel(
      factory = MusicLibraryViewModel.factory(context.applicationContext as android.app.Application),
    )

    val selectedTab    by viewModel.selectedTab.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()
    val filteredSongs  by viewModel.filteredSongs.collectAsState()
    val albums         by viewModel.albums.collectAsState()
    val artists        by viewModel.artists.collectAsState()
    val sortField      by viewModel.sortField.collectAsState()
    val sortOrder      by viewModel.sortOrder.collectAsState()
    val uiSettings     by viewModel.uiSettings.collectAsState()

    // ── Selection ──────────────────────────────────────────────────────────
    val selectionManager = rememberSelectionManager(
      items = filteredSongs,
      getId = { it.id },
      onDeleteItems = { items, _ -> viewModel.deleteVideos(items) },
      onRenameItem  = { video, newName -> viewModel.renameVideo(video, newName) },
      onOperationComplete = { viewModel.refresh() },
    )

    // ── Dialog state ───────────────────────────────────────────────────────
    val deleteDialogOpen      = rememberSaveable { mutableStateOf(false) }
    val renameDialogOpen      = rememberSaveable { mutableStateOf(false) }
    val addToPlaylistDialogOpen = rememberSaveable { mutableStateOf(false) }

    // ── Search / sort ──────────────────────────────────────────────────────
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }

    // Inline drill-in state
    var openAlbum  by remember { mutableStateOf<MusicAlbum?>(null) }
    var openArtist by remember { mutableStateOf<MusicArtist?>(null) }

    LaunchedEffect(searchQuery) { viewModel.setSearchQuery(searchQuery) }

    // Exit drill-in or selection with back
    BackHandler(enabled = openAlbum != null || openArtist != null || selectionManager.isInSelectionMode) {
      when {
        selectionManager.isInSelectionMode -> selectionManager.clear()
        else -> { openAlbum = null; openArtist = null }
      }
    }

    val topBarTitle = when {
      openAlbum  != null -> openAlbum!!.title
      openArtist != null -> openArtist!!.name
      else               -> stringResource(R.string.music)
    }

    // pass the current filtered view/album/artist list so all items are in queue
    fun playSongWithQueue(song: Video, queue: List<Video>) {
      val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
      MediaUtils.playPlaylist(queue, index, context, "media_library_list")
    }

    Scaffold(
      topBar = {
        if (isSearching && openAlbum == null && openArtist == null) {
          SearchBar(
            inputField = {
              SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(stringResource(R.string.search)) },
                trailingIcon = {
                  IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                  }
                },
              )
            },
            expanded = false,
            onExpandedChange = {},
          ) {}
        } else {
          BrowserTopBar(
            title = topBarTitle,
            isInSelectionMode = selectionManager.isInSelectionMode,
            selectedCount = selectionManager.selectedCount,
            totalCount = filteredSongs.size,
            onCancelSelection = { selectionManager.clear() },
            onBackClick = when {
              selectionManager.isInSelectionMode -> null
              openAlbum != null || openArtist != null ->
                ({ openAlbum = null; openArtist = null })
              else -> null
            },
            onSearchClick = if (!selectionManager.isInSelectionMode && openAlbum == null && openArtist == null) {
              { isSearching = true }
            } else null,
            onSelectAll    = { selectionManager.selectAll() },
            onInvertSelection = { selectionManager.invertSelection() },
            onDeselectAll  = { selectionManager.clear() },
            onPlayClick    = if (selectionManager.isInSelectionMode) {
              { selectionManager.playSelected() }
            } else null,
            additionalActions = {
              if (!selectionManager.isInSelectionMode && openAlbum == null && openArtist == null && selectedTab == MusicTab.SONGS) {
                Box {
                  IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.sort))
                  }
                  DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    MusicSortField.entries.forEach { field ->
                      DropdownMenuItem(
                        text = { Text(field.displayName) },
                        trailingIcon = if (field == sortField) {
                          { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else null,
                        onClick = { viewModel.setSortField(field); showSortMenu = false },
                      )
                    }
                    DropdownMenuItem(
                      text = { Text(if (sortOrder == MusicSortOrder.ASCENDING) "Ascending" else "Descending") },
                      onClick = { viewModel.toggleSortOrder(); showSortMenu = false },
                    )
                  }
                }
              }
            },
          )
        }
      },
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          if (openAlbum == null && openArtist == null) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
              MusicTab.entries.forEach { tab ->
                Tab(
                  selected = selectedTab == tab,
                  onClick = { viewModel.selectTab(tab) },
                  text = { Text(tab.title) },
                )
              }
            }
          }

          Box(modifier = Modifier.fillMaxSize()) {
            when {
              openAlbum != null -> {
                val songs = remember(openAlbum) { viewModel.songsForAlbum(openAlbum!!.id) }
                SongList(
                  songs = songs,
                  uiSettings = uiSettings,
                  selectionManager = null,   // no multi-select in drill-in
                  onSongClick = { song -> playSongWithQueue(song, songs) },
                )
              }
              openArtist != null -> {
                val songs = remember(openArtist) { viewModel.songsForArtist(openArtist!!.name) }
                SongList(
                  songs = songs,
                  uiSettings = uiSettings,
                  selectionManager = null,
                  onSongClick = { song -> playSongWithQueue(song, songs) },
                )
              }
              isLoading && filteredSongs.isEmpty() ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
              else -> when (selectedTab) {
                MusicTab.SONGS -> SongList(
                  songs = filteredSongs,
                  uiSettings = uiSettings,
                  selectionManager = selectionManager,
                  onSongClick = { song -> playSongWithQueue(song, filteredSongs) },
                )
                MusicTab.ALBUMS  -> AlbumGrid(albums = albums, onAlbumClick = { openAlbum = it })
                MusicTab.ARTISTS -> ArtistList(artists = artists, onArtistClick = { openArtist = it })
                MusicTab.PLAYLISTS -> PlaylistScreen.Content()
              }
            }
          }
        }

        // ── Selection bottom bar ─────────────────────────────────────────
        AnimatedVisibility(
          visible = selectionManager.isInSelectionMode,
          enter = fadeIn(),
          exit  = fadeOut(),
          modifier = Modifier.align(Alignment.BottomCenter),
        ) {
          BrowserBottomBar(
            isSelectionMode = true,
            onCopyClick  = {},        // copy/move not needed for music; disable
            onMoveClick  = {},
            onRenameClick = { renameDialogOpen.value = true },
            onDeleteClick = { deleteDialogOpen.value = true },
            onAddToPlaylistClick = { addToPlaylistDialogOpen.value = true },
            showCopy   = false,
            showMove   = false,
            showRename = selectionManager.isSingleSelection,
          )
        }
      }

      // ── Dialogs ─────────────────────────────────────────────────────────
      DeleteConfirmationDialog(
        isOpen    = deleteDialogOpen.value,
        onDismiss = { deleteDialogOpen.value = false },
        onConfirm = { selectionManager.deleteSelected() },
        itemTypePluralRes = R.plurals.item_type_video_plural,
        itemCount = selectionManager.selectedCount,
        itemNames = selectionManager.getSelectedItems().map { it.displayName },
      )

      if (renameDialogOpen.value && selectionManager.isSingleSelection) {
        val song = selectionManager.getSelectedItems().firstOrNull()
        if (song != null) {
          val baseName  = song.displayName.substringBeforeLast('.')
          val extension = ".${song.displayName.substringAfterLast('.', "")}".takeIf { it != "." }
          RenameDialog(
            isOpen    = true,
            onDismiss = { renameDialogOpen.value = false },
            onConfirm = { newName -> selectionManager.renameSelected(newName) },
            currentName  = baseName,
            itemTypeRes  = R.string.item_type_file,
            extension    = extension,
          )
        }
      }

      AddToPlaylistDialog(
        isOpen    = addToPlaylistDialogOpen.value,
        videos    = selectionManager.getSelectedItems(),
        onDismiss = { addToPlaylistDialogOpen.value = false },
        onSuccess = { selectionManager.clear(); addToPlaylistDialogOpen.value = false },
      )
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SongList(
  songs: List<Video>,
  uiSettings: UiSettings,
  selectionManager: xyz.mpv.rex.ui.browser.selection.SelectionManager<Video, Long>?,
  onSongClick: (Video) -> Unit,
) {
  if (songs.isEmpty()) {
    EmptyState(text = stringResource(R.string.no_songs_found))
    return
  }
  // Add bottom padding equal to nav bar height so the last item is never
  // hidden behind the bottom navigation bar or the mini player bar.
  val navBarHeight = LocalNavigationBarHeight.current
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + navBarHeight),
  ) {
    items(songs, key = { it.id }) { song ->
      val isSelected = selectionManager?.isSelected(song) == true
      VideoCard(
        video = song,
        onClick = {
          when {
            selectionManager != null && selectionManager.isInSelectionMode ->
              selectionManager.toggle(song)
            else -> onSongClick(song)
          }
        },
        onLongClick = { selectionManager?.handleLongClick(song) },
        isSelected = isSelected,
        uiSettings = uiSettings,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun AlbumGrid(
  albums: List<MusicAlbum>,
  onAlbumClick: (MusicAlbum) -> Unit,
) {
  if (albums.isEmpty()) {
    EmptyState(text = stringResource(R.string.no_albums_found))
    return
  }
  val navBarHeight = LocalNavigationBarHeight.current
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 160.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + navBarHeight,
    ),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(albums, key = { it.id }) { album ->
      Card(
        onClick = { onAlbumClick(album) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Icon(
            Icons.Filled.Album,
            contentDescription = null,
            modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            album.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
          )
          Text(
            album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            "${album.songCount} songs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun ArtistList(
  artists: List<MusicArtist>,
  onArtistClick: (MusicArtist) -> Unit,
) {
  if (artists.isEmpty()) {
    EmptyState(text = stringResource(R.string.no_artists_found))
    return
  }
  val navBarHeight = LocalNavigationBarHeight.current
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + navBarHeight),
  ) {
    items(artists, key = { it.name }) { artist ->
      Card(
        onClick = { onArtistClick(artist) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Icon(Icons.Filled.Person, contentDescription = null)
          Text(
            artist.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
          )
          Text(
            "${artist.songCount} songs · ${artist.albumCount} albums",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyState(text: String) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(
      text,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
