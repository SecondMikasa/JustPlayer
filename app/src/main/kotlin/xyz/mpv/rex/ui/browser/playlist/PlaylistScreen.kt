package xyz.mpv.rex.ui.browser.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.mpv.rex.R
import xyz.mpv.rex.database.repository.PlaylistRepository
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.preferences.MediaLayoutMode
import xyz.mpv.rex.preferences.PlaylistSortType
import xyz.mpv.rex.preferences.SortOrder
import xyz.mpv.rex.preferences.UiSettings
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.browser.components.BrowserTopBar
import xyz.mpv.rex.ui.browser.components.UnifiedExplorerContent
import xyz.mpv.rex.ui.browser.dialogs.DeleteConfirmationDialog
import xyz.mpv.rex.ui.browser.dialogs.SortDialog
import xyz.mpv.rex.ui.browser.dialogs.ViewModeSelector
import xyz.mpv.rex.ui.browser.selection.rememberSelectionManager
import xyz.mpv.rex.ui.browser.selection.SelectionManager
import xyz.mpv.rex.ui.browser.sheets.PlaylistActionSheet
import xyz.mpv.rex.ui.browser.states.EmptyState
import xyz.mpv.rex.ui.utils.LocalBackStack
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object PlaylistScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    InternalContent(isEmbedded = false, externalSearchQuery = "")
  }

  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  fun EmbeddedContent(searchQuery: String = "", layoutModeOverride: MediaLayoutMode? = null) {
    InternalContent(isEmbedded = true, externalSearchQuery = searchQuery, layoutModeOverride = layoutModeOverride)
  }

  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  private fun InternalContent(isEmbedded: Boolean, externalSearchQuery: String, layoutModeOverride: MediaLayoutMode? = null) {
    val context = LocalContext.current
    val repository = koinInject<PlaylistRepository>()
    val backStack = LocalBackStack.current
    val scope = rememberCoroutineScope()

    // ViewModel
    val viewModel: PlaylistViewModel = viewModel(
      factory = PlaylistViewModel.factory(context.applicationContext as android.app.Application),
    )

    val playlistsWithCount by viewModel.playlistsWithCount.collectAsState()
    val uiSettings by viewModel.uiSettings.collectAsState()
    val hasCompletedInitialLoad by viewModel.hasCompletedInitialLoad.collectAsState()

    // Search state
    var localSearchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val activeSearchQuery = if (isEmbedded) externalSearchQuery else localSearchQuery
    val isSearchActive = if (isEmbedded) externalSearchQuery.isNotBlank() else (isSearching && localSearchQuery.isNotBlank())

    // Filter playlists based on search query
    val filteredPlaylists = if (isSearchActive) {
      playlistsWithCount.filter { playlistWithCount ->
        playlistWithCount.playlist.name.contains(activeSearchQuery, ignoreCase = true)
      }
    } else {
      playlistsWithCount
    }

    // Request focus when search is activated
    LaunchedEffect(isSearching) {
      if (!isEmbedded && isSearching) {
        focusRequester.requestFocus()
        keyboardController?.show()
      }
    }

    // Selection manager - use filtered list
    val selectionManager = rememberSelectionManager(
      items = filteredPlaylists,
      getId = { it.playlist.id },
      onDeleteItems = { itemsToDelete, _ ->
        // Delete all items sequentially
        scope.launch {
          itemsToDelete.forEach { item ->
            repository.deletePlaylist(item.playlist)
          }
          viewModel.refresh()
        }
        Pair(itemsToDelete.size, 0)
      },
      onOperationComplete = { viewModel.refresh() },
    )

    // Use the remembered states
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val sortType by viewModel.playlistSortType.collectAsState()
    val sortOrder by viewModel.playlistSortOrder.collectAsState()
    val mediaLayoutMode = layoutModeOverride ?: viewModel.playlistLayoutMode.collectAsState().value
    
    val sortDialogOpen = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
      xyz.mpv.rex.ui.browser.MainScreen.scrollToTopRequest.collect { tabId ->
        if (tabId == "playlists") {
          scope.launch {
            if (mediaLayoutMode == MediaLayoutMode.GRID) {
              gridState.animateScrollToItem(0)
            } else {
              listState.animateScrollToItem(0)
            }
          }
        }
      }
    }
    val isRefreshing = remember { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // Playlist action sheet state
    var showPlaylistActionSheet by remember { mutableStateOf(false) }

    // FAB visibility for scroll-based hiding
    val isFabVisible = remember { mutableStateOf(true) }

    // Predictive back: Intercept when in selection mode or searching
    BackHandler(enabled = selectionManager.isInSelectionMode || (!isEmbedded && isSearching)) {
      when {
        !isEmbedded && isSearching -> {
          isSearching = false
          localSearchQuery = ""
        }

        selectionManager.isInSelectionMode -> selectionManager.clear()
      }
    }

    // Track scroll for FAB visibility
    xyz.mpv.rex.ui.browser.fab.FabScrollHelper.trackScrollForFabVisibility(
      listState = listState,
      gridState = if (mediaLayoutMode == MediaLayoutMode.GRID) gridState else null,
      isFabVisible = isFabVisible,
      expanded = false,
      onExpandedChange = {},
    )

    Scaffold(
        topBar = {
          if (!isEmbedded || selectionManager.isInSelectionMode) {
            if (isSearching && !isEmbedded) {
              // Search mode - show search bar
              SearchBar(
                inputField = {
                  SearchBarDefaults.InputField(
                    query = localSearchQuery,
                    onQueryChange = { localSearchQuery = it },
                    onSearch = { },
                    expanded = false,
                    onExpandedChange = { },
                    placeholder = { Text(stringResource(R.string.search_playlists)) },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_empty_title),
                      )
                    },
                    trailingIcon = {
                      IconButton(
                        onClick = {
                          isSearching = false
                          localSearchQuery = ""
                        },
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Close,
                          contentDescription = stringResource(R.string.generic_cancel),
                        )
                      }
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                  )
                },
                expanded = false,
                onExpandedChange = { },
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
              ) {
                // Empty content for SearchBar
              }
            } else {
              BrowserTopBar(
                title = stringResource(R.string.playlists),
                isInSelectionMode = selectionManager.isInSelectionMode,
                selectedCount = selectionManager.selectedCount,
                totalCount = filteredPlaylists.size,
                onBackClick = null,
                onCancelSelection = { selectionManager.clear() },
                isSingleSelection = selectionManager.isSingleSelection,
                onSearchClick = { isSearching = true },
                onSortClick = { sortDialogOpen.value = true },
                onSettingsClick = {
                  backStack.add(xyz.mpv.rex.ui.preferences.PreferencesScreen)
                },
                onRenameClick = if (selectionManager.isSingleSelection) {
                  { showRenameDialog = true }
                } else null,
                onDeleteClick = { showDeleteDialog = true },
                onSelectAll = { selectionManager.selectAll() },
                onInvertSelection = { selectionManager.invertSelection() },
                onDeselectAll = { selectionManager.clear() },
              )
            }
          }
        },
        floatingActionButton = {
          val navigationBarHeight = xyz.mpv.rex.ui.browser.LocalNavigationBarHeight.current
          if (!selectionManager.isInSelectionMode && isFabVisible.value) {
            ExtendedFloatingActionButton(
              onClick = { showPlaylistActionSheet = true },
              icon = { Icon(Icons.Filled.Add, contentDescription = null) },
              text = { Text(stringResource(R.string.create_playlist)) },
              modifier = Modifier.padding(bottom = navigationBarHeight)
            )
          }
        }
      ) { paddingValues ->
        if (isSearchActive && filteredPlaylists.isEmpty() && activeSearchQuery.isNotBlank()) {
          // Show "no results" for search
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues),
            contentAlignment = Alignment.Center,
          ) {
            EmptyState(
              icon = Icons.Filled.Search,
              title = stringResource(R.string.no_playlists_found),
              message = stringResource(R.string.try_different_search_term),
            )
          }
        } else if (playlistsWithCount.isEmpty() && hasCompletedInitialLoad) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              EmptyState(
                icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                title = stringResource(R.string.no_playlists_yet),
                message = stringResource(R.string.no_playlists_yet_desc),
              )
            }
          }
        } else {
          PlaylistListContent(
            playlistsWithCount = filteredPlaylists,
            listState = listState,
            gridState = gridState,
            uiSettings = uiSettings,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            selectionManager = selectionManager,
            mediaLayoutMode = mediaLayoutMode,
            onPlaylistClick = { playlistWithCount ->
              if (selectionManager.isInSelectionMode) {
                selectionManager.toggle(playlistWithCount)
              } else {
                backStack.add(PlaylistDetailScreen(playlistWithCount.playlist.id))
              }
            },
            onPlaylistLongClick = { playlistWithCount ->
              selectionManager.handleLongClick(playlistWithCount)
            },
            modifier = Modifier.padding(paddingValues),
            isInSelectionMode = selectionManager.isInSelectionMode,
          )
        }
      }

      // Sort Dialog
      PlaylistSortDialog(
        isOpen = sortDialogOpen.value,
        onDismiss = { sortDialogOpen.value = false },
        sortType = sortType,
        sortOrder = sortOrder,
        layoutMode = mediaLayoutMode,
        onSortTypeChange = { viewModel.setSortType(it) },
        onSortOrderChange = { viewModel.setSortOrder(it) },
        onLayoutModeChange = { viewModel.setLayoutMode(it) }
      )

      // Playlist action sheets
      PlaylistActionSheet(
        isOpen = showPlaylistActionSheet,
        onDismiss = { showPlaylistActionSheet = false },
        repository = repository,
        context = context,
      )

      if (showRenameDialog && selectionManager.isSingleSelection) {
        val selectedPlaylist = selectionManager.getSelectedItems().firstOrNull()
        if (selectedPlaylist != null) {
          var playlistName by remember(selectedPlaylist) {
            mutableStateOf(
              TextFieldValue(
                text = selectedPlaylist.playlist.name,
                selection = TextRange(selectedPlaylist.playlist.name.length),
              ),
            )
          }
          val focusRequester = remember { FocusRequester() }

          LaunchedEffect(Unit) {
            focusRequester.requestFocus()
          }

          AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_playlist)) },
            text = {
              OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
              )
            },
            confirmButton = {
              TextButton(
                onClick = {
                  if (playlistName.text.isNotBlank()) {
                    scope.launch {
                      repository.updatePlaylist(selectedPlaylist.playlist.copy(name = playlistName.text.trim()))
                      showRenameDialog = false
                      selectionManager.clear()
                    }
                  }
                },
                enabled = playlistName.text.isNotBlank(),
              ) {
                Text(stringResource(R.string.rename))
              }
            },
            dismissButton = {
              TextButton(
                onClick = { showRenameDialog = false },
              ) {
                Text(stringResource(R.string.generic_cancel))
              }
            },
          )
        }
      }

      if (showDeleteDialog) {
        DeleteConfirmationDialog(
          isOpen = true,
          onDismiss = { showDeleteDialog = false },
          onConfirm = {
            selectionManager.deleteSelected()
            showDeleteDialog = false
          },
          itemTypePluralRes = R.plurals.item_type_playlist_plural,
          itemCount = selectionManager.selectedCount,
          itemNames = selectionManager.getSelectedItems().map { it.playlist.name },
        )
      }
    }

  @Composable
  private fun PlaylistListContent(
    playlistsWithCount: List<PlaylistWithCount>,
    listState: LazyListState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    uiSettings: UiSettings,
    isRefreshing: MutableState<Boolean>,
    onRefresh: suspend () -> Unit,
    selectionManager: SelectionManager<PlaylistWithCount, Int>,
    onPlaylistClick: (PlaylistWithCount) -> Unit,
    onPlaylistLongClick: (PlaylistWithCount) -> Unit,
    mediaLayoutMode: MediaLayoutMode,
    modifier: Modifier = Modifier,
    isInSelectionMode: Boolean = false,
  ) {
    UnifiedExplorerContent(
      items = playlistsWithCount,
      isLoading = false,
      uiSettings = uiSettings,
      isSelected = { selectionManager.isSelected(it) },
      onClick = onPlaylistClick,
      onLongClick = onPlaylistLongClick,
      onToggleSelection = { selectionManager.toggle(it) },
      modifier = modifier,
      emptyTitle = stringResource(R.string.no_playlists_found),
      emptyMessage = stringResource(R.string.empty_playlists_desc),
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      isInSelectionMode = isInSelectionMode,
      listState = listState,
      gridState = gridState,
      layoutModeOverride = mediaLayoutMode,
    )
  }
}

@Composable
fun PlaylistSortDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  sortType: PlaylistSortType,
  sortOrder: SortOrder,
  layoutMode: MediaLayoutMode,
  onSortTypeChange: (PlaylistSortType) -> Unit,
  onSortOrderChange: (SortOrder) -> Unit,
  onLayoutModeChange: (MediaLayoutMode) -> Unit,
) {
  SortDialog(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = stringResource(R.string.sort_and_view_options),
    sortType = sortType.displayName,
    onSortTypeChange = { newType ->
      PlaylistSortType.entries
        .find { it.displayName == newType }
        ?.let(onSortTypeChange)
    },
    sortOrderAsc = sortOrder == SortOrder.Ascending,
    onSortOrderChange = { asc ->
      onSortOrderChange(if (asc) SortOrder.Ascending else SortOrder.Descending)
    },
    types = PlaylistSortType.entries.map { it.displayName },
    icons = listOf(
      Icons.Filled.Title,
      Icons.Filled.CalendarToday,
      Icons.Filled.AccessTime,
      Icons.AutoMirrored.Filled.ViewList,
    ),
    getLabelForType = { type, _ ->
      when (type) {
        PlaylistSortType.Title.displayName -> Pair("A-Z", "Z-A")
        PlaylistSortType.DateCreated.displayName -> Pair("Oldest", "Newest")
        PlaylistSortType.DateUpdated.displayName -> Pair("Oldest", "Newest")
        PlaylistSortType.ItemCount.displayName -> Pair("Smallest", "Largest")
        else -> Pair(type, type)
      }
    },
    layoutModeSelector = ViewModeSelector(
      label = "Layout",
      firstOptionLabel = stringResource(R.string.list),
      secondOptionLabel = stringResource(R.string.grid),
      firstOptionIcon = Icons.AutoMirrored.Filled.ViewList,
      secondOptionIcon = Icons.Filled.GridView,
      isFirstOptionSelected = layoutMode == MediaLayoutMode.LIST,
      onViewModeChange = { isList ->
        onLayoutModeChange(if (isList) MediaLayoutMode.LIST else MediaLayoutMode.GRID)
      }
    ),
  )
}
