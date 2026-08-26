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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.mpv.rex.R
import xyz.mpv.rex.domain.media.model.MusicAlbum
import xyz.mpv.rex.domain.media.model.MusicArtist
import xyz.mpv.rex.domain.media.model.MusicTab
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.preferences.MediaLayoutMode
import xyz.mpv.rex.preferences.UiSettings
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.browser.LocalNavigationBarHeight
import xyz.mpv.rex.ui.browser.cards.VideoCard
import xyz.mpv.rex.ui.browser.components.BrowserBottomBar
import xyz.mpv.rex.ui.browser.components.BrowserTopBar
import xyz.mpv.rex.ui.browser.dialogs.AddToPlaylistDialog
import xyz.mpv.rex.ui.browser.dialogs.DeleteConfirmationDialog
import xyz.mpv.rex.ui.browser.dialogs.RenameDialog
import xyz.mpv.rex.ui.browser.playlist.PlaylistScreen
import xyz.mpv.rex.ui.browser.selection.rememberSelectionManager
import xyz.mpv.rex.ui.utils.LocalBackStack
import xyz.mpv.rex.utils.media.MediaUtils

/**
 * Music library tab: Songs / Albums / Artists / Playlists.
 */
object MusicLibraryScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()
    val browserPreferences = koinInject<BrowserPreferences>()

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

    val musicLayoutMode by browserPreferences.musicLayoutMode.collectAsState()
    val musicCoverArtSize by browserPreferences.musicCoverArtSize.collectAsState()

    // ── Selection ──────────────────────────────────────────────────────────
    val selectionManager = rememberSelectionManager(
      items = filteredSongs,
      getId = { it.id },
      onDeleteItems = { items, _ -> viewModel.deleteVideos(items) },
      onRenameItem  = { video, newName -> viewModel.renameVideo(video, newName) },
      onOperationComplete = { viewModel.refresh() },
    )

    // ── Dialog state ───────────────────────────────────────────────────────
    val deleteDialogOpen        = rememberSaveable { mutableStateOf(false) }
    val renameDialogOpen        = rememberSaveable { mutableStateOf(false) }
    val addToPlaylistDialogOpen = rememberSaveable { mutableStateOf(false) }
    var showSortAndLayoutDialog by rememberSaveable { mutableStateOf(false) }

    // ── Search / sort ──────────────────────────────────────────────────────
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Inline drill-in state
    var openAlbum  by remember { mutableStateOf<MusicAlbum?>(null) }
    var openArtist by remember { mutableStateOf<MusicArtist?>(null) }

    val musicTabs = remember { MusicTab.entries }
    val pagerState = rememberPagerState(
      initialPage = selectedTab.ordinal.coerceIn(0, maxOf(0, musicTabs.lastIndex)),
      pageCount = { musicTabs.size }
    )

    LaunchedEffect(pagerState.currentPage) {
      if (pagerState.currentPage in musicTabs.indices) {
        viewModel.selectTab(musicTabs[pagerState.currentPage])
      }
    }

    LaunchedEffect(selectedTab) {
      if (selectedTab.ordinal in musicTabs.indices && selectedTab.ordinal != pagerState.currentPage) {
        pagerState.animateScrollToPage(selectedTab.ordinal)
      }
    }

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
            onSortClick = if (!selectionManager.isInSelectionMode && openAlbum == null && openArtist == null) {
              { showSortAndLayoutDialog = true }
            } else null,
            onSettingsClick = if (!selectionManager.isInSelectionMode && openAlbum == null && openArtist == null) {
              { backstack.add(xyz.mpv.rex.ui.preferences.PreferencesScreen) }
            } else null,
            onSelectAll    = { selectionManager.selectAll() },
            onInvertSelection = { selectionManager.invertSelection() },
            onDeselectAll  = { selectionManager.clear() },
            onPlayClick    = if (selectionManager.isInSelectionMode) {
              { selectionManager.playSelected() }
            } else null,
          )
        }
      },
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          if (openAlbum == null && openArtist == null) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
              musicTabs.forEachIndexed { index, tab ->
                Tab(
                  selected = pagerState.currentPage == index,
                  onClick = {
                    coroutineScope.launch {
                      pagerState.animateScrollToPage(index)
                    }
                  },
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
                  layoutMode = musicLayoutMode,
                  coverArtSize = musicCoverArtSize,
                  onSongClick = { song -> playSongWithQueue(song, songs) },
                )
              }
              openArtist != null -> {
                val songs = remember(openArtist) { viewModel.songsForArtist(openArtist!!.name) }
                SongList(
                  songs = songs,
                  uiSettings = uiSettings,
                  selectionManager = null,
                  layoutMode = musicLayoutMode,
                  coverArtSize = musicCoverArtSize,
                  onSongClick = { song -> playSongWithQueue(song, songs) },
                )
              }
              isLoading && filteredSongs.isEmpty() ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
              else -> {
                HorizontalPager(
                  state = pagerState,
                  modifier = Modifier.fillMaxSize(),
                  beyondViewportPageCount = 1,
                  key = { page -> musicTabs[page].name }
                ) { page ->
                  when (musicTabs[page]) {
                    MusicTab.SONGS -> SongList(
                      songs = filteredSongs,
                      uiSettings = uiSettings,
                      selectionManager = selectionManager,
                      layoutMode = musicLayoutMode,
                      coverArtSize = musicCoverArtSize,
                      onSongClick = { song -> playSongWithQueue(song, filteredSongs) },
                    )
                    MusicTab.ALBUMS -> AlbumGrid(albums = albums, onAlbumClick = { openAlbum = it })
                    MusicTab.ARTISTS -> ArtistList(artists = artists, onArtistClick = { openArtist = it })
                    MusicTab.PLAYLISTS -> PlaylistScreen.Content()
                  }
                }
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
      MusicSortViewOptionsDialog(
        isOpen = showSortAndLayoutDialog,
        onDismiss = { showSortAndLayoutDialog = false },
        selectedSortField = sortField,
        onSortFieldChange = { viewModel.setSortField(it) },
        sortOrder = sortOrder,
        onSortOrderChange = { viewModel.setSortOrder(it) },
        layoutMode = musicLayoutMode,
        onLayoutModeChange = { browserPreferences.musicLayoutMode.set(it) },
        coverArtSize = musicCoverArtSize,
        onCoverArtSizeChange = { browserPreferences.musicCoverArtSize.set(it) },
      )

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
  layoutMode: MediaLayoutMode = MediaLayoutMode.LIST,
  coverArtSize: Int = 56,
  onSongClick: (Video) -> Unit,
) {
  if (songs.isEmpty()) {
    EmptyState(text = stringResource(R.string.no_songs_found))
    return
  }
  val navBarHeight = LocalNavigationBarHeight.current
  if (layoutMode == MediaLayoutMode.GRID) {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = (coverArtSize * 2.5f).dp.coerceAtLeast(140.dp)),
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + navBarHeight,
      ),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
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
          isGridMode = true,
          gridColumns = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  } else {
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
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        androidx.compose.foundation.layout.Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
              artist.name,
              style = MaterialTheme.typography.titleSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              "${artist.songCount} songs • ${artist.albumCount} albums",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyState(text: String) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
