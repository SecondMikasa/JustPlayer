package xyz.mpv.rex.ui.preferences.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.mpv.rex.R
import xyz.mpv.rex.preferences.BrowserPreferences

private data class TabOrderItem(
  val id: String,
  val titleRes: Int,
  val summaryRes: Int,
  val icon: ImageVector,
)

private val TAB_DEFINITIONS = mapOf(
  BrowserPreferences.TAB_HOME to TabOrderItem(
    id = BrowserPreferences.TAB_HOME,
    titleRes = R.string.pref_appearance_tab_home_title,
    summaryRes = R.string.pref_appearance_tab_home_summary,
    icon = Icons.Outlined.Home,
  ),
  BrowserPreferences.TAB_RECENTS to TabOrderItem(
    id = BrowserPreferences.TAB_RECENTS,
    titleRes = R.string.pref_appearance_tab_recents_title,
    summaryRes = R.string.pref_appearance_tab_recents_summary,
    icon = Icons.Outlined.History,
  ),
  BrowserPreferences.TAB_PLAYLISTS to TabOrderItem(
    id = BrowserPreferences.TAB_PLAYLISTS,
    titleRes = R.string.pref_appearance_tab_playlists_title,
    summaryRes = R.string.pref_appearance_tab_playlists_summary,
    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
  ),
  BrowserPreferences.TAB_NETWORK to TabOrderItem(
    id = BrowserPreferences.TAB_NETWORK,
    titleRes = R.string.pref_appearance_tab_network_title,
    summaryRes = R.string.pref_appearance_tab_network_summary,
    icon = Icons.Outlined.Language,
  ),
  BrowserPreferences.TAB_MUSIC to TabOrderItem(
    id = BrowserPreferences.TAB_MUSIC,
    titleRes = R.string.pref_appearance_tab_music_title,
    summaryRes = R.string.pref_appearance_tab_music_summary,
    icon = Icons.Outlined.LibraryMusic,
  ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RearrangeTabsSheet(
  currentOrder: String,
  onOrderChanged: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val parsedOrder = remember(currentOrder) {
    BrowserPreferences.parseBottomNavTabOrder(currentOrder)
  }

  val items = remember(parsedOrder) {
    mutableStateListOf<String>().apply {
      addAll(parsedOrder)
    }
  }

  val lazyListState = rememberLazyListState()
  val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
    val moved = items.removeAt(from.index)
    items.add(to.index, moved)
    onOrderChanged(items.joinToString(","))
  }

  fun moveItem(index: Int, direction: Int) {
    val targetIndex = index + direction
    if (targetIndex in items.indices) {
      val moved = items.removeAt(index)
      items.add(targetIndex, moved)
      onOrderChanged(items.joinToString(","))
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
      // Header with Title, Subtitle, and Reset button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.pref_appearance_tab_order_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.pref_appearance_tab_order_sheet_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
          )
        }

        TextButton(
          onClick = {
            val defaultOrder = BrowserPreferences.DEFAULT_BOTTOM_NAV_TAB_ORDER
            items.clear()
            items.addAll(BrowserPreferences.parseBottomNavTabOrder(defaultOrder))
            onOrderChanged(defaultOrder)
          }
        ) {
          Icon(
            imageVector = Icons.Outlined.RestartAlt,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = stringResource(R.string.pref_appearance_tab_order_reset))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        itemsIndexed(items, key = { _, tabId -> tabId }) { index, tabId ->
          val tabDef = TAB_DEFINITIONS[tabId]
          if (tabDef != null) {
            ReorderableItem(reorderState, key = tabId) { isDragging ->
              val elevation by animateDpAsState(
                targetValue = if (isDragging) 8.dp else 0.dp,
                label = "elevation",
              )

              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDragging) {
                  MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                  MaterialTheme.colorScheme.surfaceContainer
                },
                shadowElevation = elevation,
                tonalElevation = elevation,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  // Tab Icon in circle badge
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = tabDef.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(14.dp))

                  // Title and summary
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = stringResource(tabDef.titleRes),
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = stringResource(tabDef.summaryRes),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.outline,
                    )
                  }

                  // Step up / down buttons
                  IconButton(
                    onClick = { moveItem(index, -1) },
                    enabled = index > 0,
                    modifier = Modifier.size(32.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.ArrowUpward,
                      contentDescription = "Move up",
                      tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                      modifier = Modifier.size(18.dp),
                    )
                  }

                  IconButton(
                    onClick = { moveItem(index, 1) },
                    enabled = index < items.lastIndex,
                    modifier = Modifier.size(32.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.ArrowDownward,
                      contentDescription = "Move down",
                      tint = if (index < items.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                      modifier = Modifier.size(18.dp),
                    )
                  }

                  Spacer(modifier = Modifier.width(4.dp))

                  // Drag handle
                  IconButton(
                    onClick = {},
                    modifier = Modifier
                      .draggableHandle()
                      .size(36.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.DragHandle,
                      contentDescription = stringResource(R.string.drag_to_reorder),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(24.dp),
                    )
                  }
                }
              }
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}
