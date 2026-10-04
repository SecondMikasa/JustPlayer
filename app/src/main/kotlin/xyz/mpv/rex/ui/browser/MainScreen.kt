package xyz.mpv.rex.ui.browser

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import xyz.mpv.rex.R
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.browser.folderlist.FolderListScreen
import xyz.mpv.rex.ui.browser.miniplayer.MiniPlayerStateManager
import xyz.mpv.rex.ui.browser.music.MusicLibraryScreen
import xyz.mpv.rex.ui.browser.networkstreaming.NetworkStreamingScreen
import xyz.mpv.rex.ui.browser.playlist.PlaylistScreen
import xyz.mpv.rex.ui.browser.recentlyplayed.RecentlyPlayedScreen
import xyz.mpv.rex.ui.browser.selection.SelectionManager

@Serializable
object MainScreen : Screen {
  // Use companion object to store state persistently across recompositions
  private var persistentSelectedTab: Int = 0
  private var persistentSelectedTabId: String = "home"
  private var persistentPreviousTab: Int = 0
  
  private val _tabRequest = MutableSharedFlow<Int>(extraBufferCapacity = 1)
  val tabRequest = _tabRequest.asSharedFlow()

  private val _scrollToTopRequest = MutableSharedFlow<String>(extraBufferCapacity = 1)
  val scrollToTopRequest = _scrollToTopRequest.asSharedFlow()

  fun requestTab(tab: Int) {
    _tabRequest.tryEmit(tab)
  }
  
  fun requestPreviousTab() {
    _tabRequest.tryEmit(persistentPreviousTab)
  }

  // Shared state that can be updated by FileSystemBrowserScreen
  private val _isInSelectionModeShared = MutableStateFlow(false)
  val isInSelectionModeShared = _isInSelectionModeShared.asStateFlow()
  
  private val _shouldHideNavigationBar = MutableStateFlow(false)
  val shouldHideNavigationBar = _shouldHideNavigationBar.asStateFlow()
  
  private val _isBrowserBottomBarVisible = MutableStateFlow(false)
  val isBrowserBottomBarVisible = _isBrowserBottomBarVisible.asStateFlow()
  
  private val _sharedVideoSelectionManager = MutableStateFlow<Any?>(null)
  val sharedVideoSelectionManager = _sharedVideoSelectionManager.asStateFlow()
  
  // Check if the selection contains only videos and update navigation bar visibility accordingly
  private val _onlyVideosSelected = MutableStateFlow(false)
  val onlyVideosSelected = _onlyVideosSelected.asStateFlow()
  
  // Track when permission denied screen is showing to hide FAB
  private val _isPermissionDenied = MutableStateFlow(false)
  val isPermissionDenied = _isPermissionDenied.asStateFlow()

  // Expose visible tab list and selected index so MainActivity can render
  // the tab bar on top of the MiniPlayer (they are siblings in MainActivity's Box).
  private val _visibleTabs = MutableStateFlow<List<VisibleTab>>(emptyList())
  val visibleTabs = _visibleTabs.asStateFlow()

  private val _selectedTabIndex = MutableStateFlow(0)
  val selectedTabIndex = _selectedTabIndex.asStateFlow()
  
  /**
   * Update selection state and navigation bar visibility
   * This method should be called whenever selection changes
   */
  fun updateSelectionState(
    isInSelectionMode: Boolean,
    isOnlyVideosSelected: Boolean,
    selectionManager: Any?
  ) {
    _isInSelectionModeShared.value = isInSelectionMode
    _onlyVideosSelected.value = isOnlyVideosSelected
    _sharedVideoSelectionManager.value = selectionManager
    
    // Only hide navigation bar when videos are selected AND in selection mode
    _shouldHideNavigationBar.value = isInSelectionMode && isOnlyVideosSelected
  }
  
  /**
   * Update permission state to control FAB visibility
   */
  fun updatePermissionState(isDenied: Boolean) {
    _isPermissionDenied.value = isDenied
  }

  /**
   * Get current permission denied state
   */
  fun getPermissionDeniedState(): Boolean = _isPermissionDenied.value

  /**
   * Update bottom navigation bar visibility based on floating bottom bar state
   */
  fun updateBottomBarVisibility(shouldShow: Boolean) {
    // Hide bottom navigation when floating bottom bar is visible
    _shouldHideNavigationBar.value = !shouldShow
  }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    val coroutineScope = rememberCoroutineScope()
    val browserPreferences = koinInject<BrowserPreferences>()
    val miniPlayerStateManager = koinInject<MiniPlayerStateManager>()
    val miniPlayerState by miniPlayerStateManager.state.collectAsState()
    val enableTabRecents by browserPreferences.enableTabRecents.collectAsState()
    val enableTabPlaylists by browserPreferences.enableTabPlaylists.collectAsState()
    val enableTabNetwork by browserPreferences.enableTabNetwork.collectAsState()
    val enableTabMusic by browserPreferences.enableTabMusic.collectAsState()
    val bottomNavTabOrderRaw by browserPreferences.bottomNavTabOrder.collectAsState()

    val homeLabel = stringResource(R.string.home)
    val recentsLabel = stringResource(R.string.recents)
    val playlistsLabel = stringResource(R.string.playlists)
    val networkLabel = stringResource(R.string.network)
    val musicLabel = stringResource(R.string.music)

    val tabOrder = remember(bottomNavTabOrderRaw) {
      BrowserPreferences.parseBottomNavTabOrder(bottomNavTabOrderRaw)
    }

    val visibleTabs = remember(
      tabOrder,
      enableTabRecents, enableTabPlaylists, enableTabNetwork, enableTabMusic,
      homeLabel, recentsLabel, playlistsLabel, networkLabel, musicLabel
    ) {
      buildList {
        for (tabId in tabOrder) {
          when (tabId) {
            BrowserPreferences.TAB_HOME -> add(
              VisibleTab("home", homeLabel, Icons.Filled.Home) {
                FolderListScreen.Content()
              }
            )
            BrowserPreferences.TAB_RECENTS -> if (enableTabRecents) {
              add(
                VisibleTab("recents", recentsLabel, Icons.Filled.History) {
                  RecentlyPlayedScreen.Content()
                }
              )
            }
            BrowserPreferences.TAB_PLAYLISTS -> if (enableTabPlaylists) {
              add(
                VisibleTab("playlists", playlistsLabel, Icons.AutoMirrored.Filled.PlaylistPlay) {
                  PlaylistScreen.Content()
                }
              )
            }
            BrowserPreferences.TAB_NETWORK -> if (enableTabNetwork) {
              add(
                VisibleTab("network", networkLabel, Icons.Filled.Language) {
                  NetworkStreamingScreen.Content()
                }
              )
            }
            BrowserPreferences.TAB_MUSIC -> if (enableTabMusic) {
              add(
                VisibleTab("music", musicLabel, Icons.Filled.LibraryMusic) {
                  MusicLibraryScreen.Content()
                }
              )
            }
          }
        }
      }
    }

    val pagerState = rememberPagerState(
      initialPage = persistentSelectedTab.coerceIn(0, maxOf(0, visibleTabs.lastIndex)),
      pageCount = { visibleTabs.size }
    )

    // Sync selected tab with persistent tracking
    LaunchedEffect(pagerState) {
      snapshotFlow { pagerState.currentPage }.collect { page ->
        if (page in visibleTabs.indices) {
          persistentPreviousTab = persistentSelectedTab
          persistentSelectedTab = page
          persistentSelectedTabId = visibleTabs[page].id
          _selectedTabIndex.value = page
        }
      }
    }

    // Keep the static visibleTabs state in sync so MainActivity can render the tab bar
    LaunchedEffect(visibleTabs) {
      _visibleTabs.value = visibleTabs
    }

    // Keep active tab stable when visibleTabs list changes
    LaunchedEffect(visibleTabs) {
      val targetIndex = visibleTabs.indexOfFirst { it.id == persistentSelectedTabId }
      if (targetIndex != -1 && targetIndex != pagerState.currentPage) {
        pagerState.scrollToPage(targetIndex)
      } else if (pagerState.currentPage >= visibleTabs.size && visibleTabs.isNotEmpty()) {
        pagerState.scrollToPage(0)
      }
    }

    // Shared state (across the app) collected reactively via StateFlow
    val isInSelectionMode by _isInSelectionModeShared.collectAsState()
    val hideNavigationBar by _shouldHideNavigationBar.collectAsState()
    val rawSelectionManager by _sharedVideoSelectionManager.collectAsState()
    val videoSelectionManager = rawSelectionManager as? SelectionManager<*, *>

    // Handle tab requests from other screens
    LaunchedEffect(Unit) {
      tabRequest.collect { tab ->
        if (tab in visibleTabs.indices) {
          pagerState.animateScrollToPage(tab)
        }
      }
    }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize()) {
        val fabBottomPadding = 92.dp
        val isNavBarVisible = !hideNavigationBar && visibleTabs.size > 1
        val navBarHeight = if (isNavBarVisible) fabBottomPadding else 0.dp
        val miniPlayerHeight = if (miniPlayerState.isPlaybackActive) 72.dp else 0.dp
        val totalBottomPadding = navBarHeight + miniPlayerHeight

        CompositionLocalProvider(
          LocalNavigationBarHeight provides totalBottomPadding
        ) {
          HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { page -> visibleTabs.getOrNull(page)?.id ?: page }
          ) { page ->
            if (page in visibleTabs.indices) {
              visibleTabs[page].content()
            } else {
              FolderListScreen.Content()
            }
          }
        }

        // Floating Pill Bottom Navigation Bar
        AnimatedVisibility(
          visible = !hideNavigationBar && visibleTabs.size > 1,
          enter = slideInVertically(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            initialOffsetY = { fullHeight -> fullHeight }
          ) + fadeIn(animationSpec = tween(durationMillis = 250)),
          exit = slideOutVertically(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            targetOffsetY = { fullHeight -> fullHeight }
          ) + fadeOut(animationSpec = tween(durationMillis = 200)),
          modifier = Modifier.align(Alignment.BottomCenter)
        ) {
          FloatingPillBottomBar(
            visibleTabs = visibleTabs,
            selectedIndex = pagerState.currentPage,
            onTabSelected = { index, tab ->
              if (pagerState.currentPage == index) {
                _scrollToTopRequest.tryEmit(tab.id)
              } else {
                coroutineScope.launch {
                  pagerState.animateScrollToPage(index)
                }
              }
            }
            // NO bottom padding! It stays at the very bottom.
          )
        }
      }
    }
  }
}

@Composable
private fun FloatingPillBottomBar(
  visibleTabs: List<VisibleTab>,
  selectedIndex: Int,
  onTabSelected: (Int, VisibleTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .navigationBarsPadding(),
    shape = RoundedCornerShape(36.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
    tonalElevation = 6.dp,
    shadowElevation = 10.dp,
    border = BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
    ),
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      visibleTabs.forEachIndexed { index, tab ->
        val isSelected = selectedIndex == index

        val animatedBgColor by animateColorAsState(
          targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
          animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
          label = "tab_bg_color",
        )
        val contentColor by animateColorAsState(
          targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
          animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
          label = "tab_content_color",
        )

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(animatedBgColor)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true, radius = 32.dp),
              onClick = { onTabSelected(index, tab) },
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = tab.icon,
              contentDescription = tab.label,
              tint = contentColor,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = tab.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = contentColor,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

// CompositionLocal for navigation bar height
val LocalNavigationBarHeight = compositionLocalOf { 0.dp }

data class VisibleTab(
  val id: String,
  val label: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val content: @Composable () -> Unit
)
