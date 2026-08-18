package xyz.mpv.rex.ui.browser.music

import androidx.activity.compose.BackHandler
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
import xyz.mpv.rex.ui.browser.cards.VideoCard
import xyz.mpv.rex.ui.browser.components.BrowserTopBar
import xyz.mpv.rex.ui.browser.playlist.PlaylistScreen
import xyz.mpv.rex.utils.media.MediaUtils

/**
 * Music library tab: Songs / Albums / Artists / Playlists.
 *
 * Songs and playback reuse existing components (VideoCard, MediaUtils.playFile,
 * PlaylistScreen) unmodified. Album/Artist detail is shown inline (not a separate
 * navigation destination) to keep this self-contained; promote to a dedicated
 * Screen route later if you want deep-linking into a specific album/artist.
 */
object MusicLibraryScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val viewModel: MusicLibraryViewModel = viewModel(
      factory = MusicLibraryViewModel.factory(context.applicationContext as android.app.Application),
    )

    val selectedTab by viewModel.selectedTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val uiSettings by viewModel.uiSettings.collectAsState()

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }

    // Inline drill-in state: when set, shows that album/artist's song list instead of the grid/list.
    var openAlbum by remember { mutableStateOf<MusicAlbum?>(null) }
    var openArtist by remember { mutableStateOf<MusicArtist?>(null) }

    LaunchedEffect(searchQuery) { viewModel.setSearchQuery(searchQuery) }

    BackHandler(enabled = openAlbum != null || openArtist != null) {
      openAlbum = null
      openArtist = null
    }

    val topBarTitle = when {
      openAlbum != null -> openAlbum!!.title
      openArtist != null -> openArtist!!.name
      else -> stringResource(R.string.music)
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
            isInSelectionMode = false,
            selectedCount = 0,
            totalCount = 0,
            onCancelSelection = {},
            onBackClick = if (openAlbum != null || openArtist != null) {
              { openAlbum = null; openArtist = null }
            } else {
              null
            },
            onSearchClick = if (openAlbum == null && openArtist == null) {
              { isSearching = true }
            } else {
              null
            },
            additionalActions = {
              if (openAlbum == null && openArtist == null && selectedTab == MusicTab.SONGS) {
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
                        } else {
                          null
                        },
                        onClick = {
                          viewModel.setSortField(field)
                          showSortMenu = false
                        },
                      )
                    }
                    DropdownMenuItem(
                      text = {
                        Text(if (sortOrder == MusicSortOrder.ASCENDING) "Ascending" else "Descending")
                      },
                      onClick = {
                        viewModel.toggleSortOrder()
                        showSortMenu = false
                      },
                    )
                  }
                }
              }
            },
          )
        }
      },
    ) { paddingValues ->
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
              SongList(songs = songs, uiSettings = uiSettings, onSongClick = { MediaUtils.playFile(it, context, "music_album") })
            }
            openArtist != null -> {
              val songs = remember(openArtist) { viewModel.songsForArtist(openArtist!!.name) }
              SongList(songs = songs, uiSettings = uiSettings, onSongClick = { MediaUtils.playFile(it, context, "music_artist") })
            }
            isLoading && filteredSongs.isEmpty() -> {
              CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else -> when (selectedTab) {
              MusicTab.SONGS -> SongList(
                songs = filteredSongs,
                uiSettings = uiSettings,
                onSongClick = { MediaUtils.playFile(it, context, "music_library") },
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
}

@Composable
private fun SongList(
  songs: List<Video>,
  uiSettings: UiSettings,
  onSongClick: (Video) -> Unit,
) {
  if (songs.isEmpty()) {
    EmptyState(text = stringResource(R.string.no_songs_found))
    return
  }
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(vertical = 8.dp),
  ) {
    items(songs, key = { it.id }) { song ->
      VideoCard(
        video = song,
        onClick = { onSongClick(song) },
        uiSettings = uiSettings,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
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
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 160.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(8.dp),
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
  LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
    items(artists, key = { it.name }) { artist ->
      Card(
        onClick = { onArtistClick(artist) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Icon(Icons.Filled.Person, contentDescription = null)
          Text(artist.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
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
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
