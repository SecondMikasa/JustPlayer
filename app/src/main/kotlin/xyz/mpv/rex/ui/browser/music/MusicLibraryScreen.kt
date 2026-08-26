package xyz.mpv.rex.ui.browser.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.mpv.rex.R
import xyz.mpv.rex.domain.media.model.*
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.preferences.MediaLayoutMode
import xyz.mpv.rex.preferences.UiSettings
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.presentation.components.pullrefresh.PullRefreshBox
import xyz.mpv.rex.ui.browser.LocalNavigationBarHeight
import xyz.mpv.rex.ui.browser.cards.VideoCard
import xyz.mpv.rex.ui.browser.components.BrowserBottomBar
import xyz.mpv.rex.ui.browser.components.BrowserTopBar
import xyz.mpv.rex.ui.browser.dialogs.*
import xyz.mpv.rex.ui.browser.playlist.PlaylistScreen
import xyz.mpv.rex.ui.browser.selection.rememberSelectionManager
import xyz.mpv.rex.ui.browser.selection.SelectionManager
import xyz.mpv.rex.ui.utils.LocalBackStack
import xyz.mpv.rex.utils.media.MediaUtils
import xyz.mpv.rex.utils.media.CopyPasteOps
import xyz.mpv.rex.utils.media.OpenDocumentTreeContract
import xyz.mpv.rex.ui.browser.cards.FolderCard
import xyz.mpv.rex.ui.browser.states.EmptyState as GlobalEmptyState
import java.io.File

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
    val folders        by viewModel.folders.collectAsState()
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

    // ── Copy/Move state ────────────────────────────────────────────────────
    val folderPickerOpen = rememberSaveable { mutableStateOf(false) }
    val operationType = remember { mutableStateOf<CopyPasteOps.OperationType?>(null) }
    val progressDialogOpen = rememberSaveable { mutableStateOf(false) }
    val operationProgress by CopyPasteOps.operationProgress.collectAsState()
    val treePickerLauncher =
      androidx.activity.compose.rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val selectedItems = selectionManager.getSelectedItems()
        if (selectedItems.isEmpty() || operationType.value == null) return@rememberLauncherForActivityResult

        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
          )
        }

        progressDialogOpen.value = true
        coroutineScope.launch {
          when (operationType.value) {
            is CopyPasteOps.OperationType.Copy -> CopyPasteOps.copyFilesToTreeUri(context, selectedItems, uri)
            is CopyPasteOps.OperationType.Move -> CopyPasteOps.moveFilesToTreeUri(context, selectedItems, uri)
            else -> {}
          }
        }
      }

    // ── Search / sort ──────────────────────────────────────────────────────
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Inline drill-in state
    var openAlbum  by remember { mutableStateOf<MusicAlbum?>(null) }
    var openArtist by remember { mutableStateOf<MusicArtist?>(null) }
    var openFolder by remember { mutableStateOf<VideoFolder?>(null) }

    val isRefreshing = remember { mutableStateOf(false) }

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
    BackHandler(enabled = openAlbum != null || openArtist != null || openFolder != null || selectionManager.isInSelectionMode) {
      when {
        selectionManager.isInSelectionMode -> selectionManager.clear()
        else -> { openAlbum = null; openArtist = null; openFolder = null }
      }
    }

    val topBarTitle = when {
      openAlbum  != null -> openAlbum!!.title
      openArtist != null -> openArtist!!.name
      openFolder != null -> openFolder!!.name
      else               -> stringResource(R.string.music)
    }

    // pass the current filtered view/album/artist list so all items are in queue
    fun playSongWithQueue(song: Video, queue: List<Video>) {
      val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
      MediaUtils.playPlaylist(queue, index, context, "media_library_list")
    }

    Scaffold(
      topBar = {
        Column {
          if (isSearching && openAlbum == null && openArtist == null && openFolder == null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .clip(CircleShape),
                placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                  IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                  }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                  focusedIndicatorColor = Color.Transparent,
                  unfocusedIndicatorColor = Color.Transparent,
                  disabledIndicatorColor = Color.Transparent,
                  focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                textStyle = MaterialTheme.typography.bodyMedium
              )
            }
          } else {
            BrowserTopBar(
              title = topBarTitle,
              isInSelectionMode = selectionManager.isInSelectionMode,
              selectedCount = selectionManager.selectedCount,
              totalCount = filteredSongs.size,
              onCancelSelection = { selectionManager.clear() },
              onBackClick = when {
                selectionManager.isInSelectionMode -> null
                openAlbum != null || openArtist != null || openFolder != null ->
                  ({ openAlbum = null; openArtist = null; openFolder = null })
                else -> null
              },
              onSearchClick = if (!selectionManager.isInSelectionMode && openAlbum == null && openArtist == null && openFolder == null) {
                { isSearching = true }
              } else null,
              onSortClick = if (!selectionManager.isInSelectionMode && openAlbum == null && openArtist == null && openFolder == null) {
                { showSortAndLayoutDialog = true }
              } else null,
              onSettingsClick = if (!selectionManager.isInSelectionMode && openAlbum == null && openArtist == null && openFolder == null) {
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

          if (openAlbum == null && openArtist == null && openFolder == null) {
            ScrollableTabRow(
              selectedTabIndex = pagerState.currentPage,
              edgePadding = 12.dp,
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.primary,
              divider = {},
            ) {
              musicTabs.forEachIndexed { index, tab ->
                val selected = pagerState.currentPage == index
                Tab(
                  selected = selected,
                  onClick = {
                    coroutineScope.launch {
                      pagerState.animateScrollToPage(index)
                    }
                  },
                  text = {
                    Text(
                      text = tab.title,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 14.sp
                    )
                  },
                )
              }
            }
          }
        }
      },
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        PullRefreshBox(
          isRefreshing = isRefreshing,
          onRefresh = { viewModel.refresh() },
          modifier = Modifier.fillMaxSize(),
        ) {
          when {
            openAlbum != null -> {
              val songs = remember(openAlbum) { viewModel.songsForAlbum(openAlbum!!.id) }
              SongList(
                songs = songs,
                uiSettings = uiSettings,
                selectionManager = null,
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
            openFolder != null -> {
              val songs = remember(openFolder) { viewModel.songsForFolder(openFolder!!.path) }
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
                  MusicTab.ALBUMS -> AlbumGrid(albums = albums, layoutMode = musicLayoutMode, coverArtSize = musicCoverArtSize, onAlbumClick = { openAlbum = it })
                  MusicTab.ARTISTS -> ArtistList(artists = artists, layoutMode = musicLayoutMode, coverArtSize = musicCoverArtSize, onArtistClick = { openArtist = it })
                  MusicTab.FOLDERS -> FolderList(folders = folders, layoutMode = musicLayoutMode, coverArtSize = musicCoverArtSize, onFolderClick = { openFolder = it })
                  MusicTab.PLAYLISTS -> PlaylistScreen.Content()
                }
              }
            }
          }
        }

        AnimatedVisibility(
          visible = selectionManager.isInSelectionMode,
          enter = fadeIn(),
          exit  = fadeOut(),
          modifier = Modifier.align(Alignment.BottomCenter),
        ) {
          BrowserBottomBar(
            isSelectionMode = true,
            onCopyClick  = {
              operationType.value = CopyPasteOps.OperationType.Copy
              if (CopyPasteOps.canUseDirectFileOperations()) folderPickerOpen.value = true else treePickerLauncher.launch(null)
            },
            onMoveClick  = {
              operationType.value = CopyPasteOps.OperationType.Move
              if (CopyPasteOps.canUseDirectFileOperations()) folderPickerOpen.value = true else treePickerLauncher.launch(null)
            },
            onRenameClick = { renameDialogOpen.value = true },
            onDeleteClick = { deleteDialogOpen.value = true },
            onAddToPlaylistClick = { addToPlaylistDialogOpen.value = true },
            showCopy   = true,
            showMove   = true,
            showRename = selectionManager.isSingleSelection,
          )
        }
      }

      FolderPickerDialog(
        isOpen = folderPickerOpen.value,
        currentPath = filteredSongs.firstOrNull()?.let { File(it.path).parent } ?: android.os.Environment.getExternalStorageDirectory().absolutePath,
        onDismiss = { folderPickerOpen.value = false },
        onFolderSelected = { destinationPath ->
          folderPickerOpen.value = false
          val selectedItems = selectionManager.getSelectedItems()
          if (selectedItems.isNotEmpty() && operationType.value != null) {
            progressDialogOpen.value = true
            coroutineScope.launch {
              when (operationType.value) {
                is CopyPasteOps.OperationType.Copy -> CopyPasteOps.copyFiles(context, selectedItems, destinationPath)
                is CopyPasteOps.OperationType.Move -> CopyPasteOps.moveFiles(context, selectedItems, destinationPath)
                else -> {}
              }
            }
          }
        },
      )

      if (operationType.value != null) {
        FileOperationProgressDialog(
          isOpen = progressDialogOpen.value,
          operationType = operationType.value!!,
          progress = operationProgress,
          onCancel = { CopyPasteOps.cancelOperation() },
          onDismiss = {
            progressDialogOpen.value = false
            operationType.value = null
            selectionManager.clear()
            viewModel.refresh()
          },
        )
      }
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

@Composable
private fun SongList(
  songs: List<Video>,
  uiSettings: UiSettings,
  selectionManager: SelectionManager<Video, Long>?,
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
      columns = GridCells.Adaptive(minSize = (coverArtSize * 2.2f).dp.coerceAtLeast(120.dp)),
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
          thumbnailSize = (coverArtSize * 2.2f).dp,
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
          thumbnailSize = coverArtSize.dp,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        )
      }
    }
  }
}

@Composable
private fun AlbumGrid(
  albums: List<MusicAlbum>,
  layoutMode: MediaLayoutMode = MediaLayoutMode.GRID,
  coverArtSize: Int = 160,
  onAlbumClick: (MusicAlbum) -> Unit,
) {
  if (albums.isEmpty()) {
    EmptyState(text = stringResource(R.string.no_albums_found))
    return
  }
  val navBarHeight = LocalNavigationBarHeight.current

  if (layoutMode == MediaLayoutMode.GRID) {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = (coverArtSize).dp.coerceAtLeast(140.dp)),
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
              modifier = Modifier.size((coverArtSize/2).dp).align(Alignment.CenterHorizontally),
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
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + navBarHeight),
    ) {
      items(albums, key = { it.id }) { album ->
        Card(
          onClick = { onAlbumClick(album) },
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Filled.Album,
              contentDescription = null,
              modifier = Modifier.size(coverArtSize.dp.coerceAtMost(64.dp)),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
              Text(
                album.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
  }
}

@Composable
private fun ArtistList(
  artists: List<MusicArtist>,
  layoutMode: MediaLayoutMode = MediaLayoutMode.LIST,
  coverArtSize: Int = 160,
  onArtistClick: (MusicArtist) -> Unit,
) {
  if (artists.isEmpty()) {
    EmptyState(text = stringResource(R.string.no_artists_found))
    return
  }
  val navBarHeight = LocalNavigationBarHeight.current

  if (layoutMode == MediaLayoutMode.GRID) {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = (coverArtSize).dp.coerceAtLeast(140.dp)),
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + navBarHeight,
      ),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(artists, key = { it.name }) { artist ->
        Card(
          onClick = { onArtistClick(artist) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Icon(
              Icons.Filled.Person,
              contentDescription = null,
              modifier = Modifier.size((coverArtSize/2).dp).align(Alignment.CenterHorizontally),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              artist.name,
              style = MaterialTheme.typography.titleSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(top = 8.dp),
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
  } else {
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
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Filled.Person,
              contentDescription = null,
              modifier = Modifier.size(coverArtSize.dp.coerceAtMost(64.dp)),
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
}

@Composable
private fun FolderList(
  folders: List<VideoFolder>,
  layoutMode: MediaLayoutMode = MediaLayoutMode.LIST,
  coverArtSize: Int = 160,
  onFolderClick: (VideoFolder) -> Unit,
) {
  if (folders.isEmpty()) {
    EmptyState(text = "No folders found")
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
      items(folders, key = { it.path }) { folder ->
        FolderCard(
          folder = folder,
          uiSettings = UiSettings(
            showVideoThumbnails = true,
            showSizeChip = true,
            showProgressBar = true
          ),
          onClick = { onFolderClick(folder) },
          onLongClick = {},
          onThumbClick = null,
          isGridMode = true,
          gridColumns = 2,
          thumbnailSize = coverArtSize.dp,
        )
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + navBarHeight),
    ) {
      items(folders, key = { it.path }) { folder ->
        FolderCard(
          folder = folder,
          uiSettings = UiSettings(
            showVideoThumbnails = true,
            showSizeChip = true,
            showProgressBar = true
          ),
          onClick = { onFolderClick(folder) },
          onLongClick = {},
          onThumbClick = null,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
  }
}

@Composable
private fun EmptyState(text: String) {
  GlobalEmptyState(
    icon = Icons.Filled.MusicNote,
    title = text,
    message = ""
  )
}
