package xyz.mpv.rex.ui.browser.music

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.R
import xyz.mpv.rex.domain.media.model.MusicSortField
import xyz.mpv.rex.domain.media.model.MusicSortOrder
import xyz.mpv.rex.preferences.MediaLayoutMode
import xyz.mpv.rex.ui.browser.dialogs.SortDialog
import xyz.mpv.rex.ui.browser.dialogs.ViewModeSelector
import xyz.mpv.rex.ui.browser.dialogs.VisibilityToggle
import kotlin.math.roundToInt

@Composable
fun MusicSortViewOptionsDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  selectedSortField: MusicSortField,
  onSortFieldChange: (MusicSortField) -> Unit,
  sortOrder: MusicSortOrder,
  onSortOrderChange: (MusicSortOrder) -> Unit,
  layoutMode: MediaLayoutMode,
  onLayoutModeChange: (MediaLayoutMode) -> Unit,
  coverArtSize: Int,
  onCoverArtSizeChange: (Int) -> Unit,
  visibilityToggles: List<VisibilityToggle> = emptyList(),
  modifier: Modifier = Modifier,
) {
  SortDialog(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = stringResource(R.string.sort_and_view_options),
    sortType = selectedSortField.displayName,
    onSortTypeChange = { newLabel ->
      MusicSortField.entries
        .find { it.displayName == newLabel }
        ?.let(onSortFieldChange)
    },
    sortOrderAsc = sortOrder == MusicSortOrder.ASCENDING,
    onSortOrderChange = { asc ->
      onSortOrderChange(if (asc) MusicSortOrder.ASCENDING else MusicSortOrder.DESCENDING)
    },
    types = MusicSortField.entries.map { it.displayName },
    icons = listOf(
      Icons.Filled.Title,
      Icons.Filled.Mic,
      Icons.Filled.MusicNote,
      Icons.Filled.AccessTime,
      Icons.Filled.CalendarToday,
    ),
    getLabelForType = { type, _ ->
      when (type) {
        MusicSortField.TITLE.displayName -> Pair("A-Z", "Z-A")
        MusicSortField.ARTIST.displayName -> Pair("A-Z", "Z-A")
        MusicSortField.ALBUM.displayName -> Pair("A-Z", "Z-A")
        MusicSortField.DURATION.displayName -> Pair("Shortest", "Longest")
        MusicSortField.DATE_ADDED.displayName -> Pair("Oldest", "Newest")
        else -> Pair(type, type)
      }
    },
    visibilityToggles = visibilityToggles,
    layoutModeSelector = ViewModeSelector(
      label = "Layout",
      firstOptionLabel = "List",
      secondOptionLabel = "Grid",
      firstOptionIcon = Icons.AutoMirrored.Filled.ViewList,
      secondOptionIcon = Icons.Filled.GridView,
      isFirstOptionSelected = layoutMode == MediaLayoutMode.LIST,
      onViewModeChange = { isList ->
        onLayoutModeChange(if (isList) MediaLayoutMode.LIST else MediaLayoutMode.GRID)
      }
    ),
    additionalContent = {
      // Cover art size slider
      Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Art Cover Size",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = androidx.compose.foundation.shape.CircleShape,
          ) {
            Text(
              text = "$coverArtSize dp",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
          }
        }

        Slider(
          value = coverArtSize.toFloat(),
          onValueChange = { onCoverArtSizeChange(it.roundToInt()) },
          valueRange = 40f..120f,
          steps = 15,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    modifier = modifier,
  )
}
