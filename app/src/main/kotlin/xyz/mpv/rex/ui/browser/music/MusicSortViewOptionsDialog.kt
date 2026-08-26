package xyz.mpv.rex.ui.browser.music

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import xyz.mpv.rex.domain.media.model.MusicSortField
import xyz.mpv.rex.domain.media.model.MusicSortOrder
import xyz.mpv.rex.preferences.MediaLayoutMode
import kotlin.math.roundToInt

private data class SortFieldItem(
  val field: MusicSortField,
  val label: String,
  val icon: ImageVector,
)

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
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  val sortFields = remember {
    listOf(
      SortFieldItem(MusicSortField.TITLE, "Title", Icons.Filled.Title),
      SortFieldItem(MusicSortField.ARTIST, "Artist", Icons.Filled.Mic),
      SortFieldItem(MusicSortField.ALBUM, "Album", Icons.Filled.MusicNote),
      SortFieldItem(MusicSortField.DURATION, "Duration", Icons.Filled.AccessTime),
      SortFieldItem(MusicSortField.DATE_ADDED, "Date", Icons.Filled.CalendarToday),
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    shape = RoundedCornerShape(32.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = modifier.padding(24.dp),
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
      ) {
        // Title
        Text(
          text = "Sort & View Options",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 16.dp),
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        // ── 1. Sort by ────────────────────────────────────────────────────────
        Text(
          text = "Sort by",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sort Field Items Row - Scrollable for better fit
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          sortFields.forEach { item ->
            val isSelected = selectedSortField == item.field

            val animatedCardColor by animateColorAsState(
              targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              animationSpec = tween(300),
              label = "sort_card_bg",
            )
            val animatedIconColor by animateColorAsState(
              targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              animationSpec = tween(300),
              label = "sort_card_icon",
            )

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .width(64.dp)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = { onSortFieldChange(item.field) },
                ),
            ) {
              Box(
                modifier = Modifier
                  .size(60.dp)
                  .clip(RoundedCornerShape(20.dp))
                  .background(animatedCardColor),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.label,
                  tint = animatedIconColor,
                  modifier = Modifier.size(28.dp),
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sort Order Segmented Pill
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
              BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
              RoundedCornerShape(24.dp),
            ),
        ) {
          // Ascending Button
          val isAscending = sortOrder == MusicSortOrder.ASCENDING
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(24.dp))
              .background(if (isAscending) MaterialTheme.colorScheme.primary else Color.Transparent)
              .clickable { onSortOrderChange(MusicSortOrder.ASCENDING) },
            contentAlignment = Alignment.Center,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = if (isAscending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(Modifier.width(6.dp))
              Text(
                "A-Z",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isAscending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Descending Button
          val isDescending = sortOrder == MusicSortOrder.DESCENDING
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(24.dp))
              .background(if (isDescending) MaterialTheme.colorScheme.primary else Color.Transparent)
              .clickable { onSortOrderChange(MusicSortOrder.DESCENDING) },
            contentAlignment = Alignment.Center,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isDescending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(Modifier.width(6.dp))
              Text(
                "Z-A",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDescending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── 2. View Layout ──────────────────────────────────────────────────
        Text(
          text = "View Layout",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
              BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
              RoundedCornerShape(24.dp),
            ),
        ) {
          // List Button
          val isList = layoutMode == MediaLayoutMode.LIST
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(24.dp))
              .background(if (isList) MaterialTheme.colorScheme.primary else Color.Transparent)
              .clickable { onLayoutModeChange(MediaLayoutMode.LIST) },
            contentAlignment = Alignment.Center,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.AutoMirrored.Filled.ViewList,
                contentDescription = null,
                tint = if (isList) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                "List",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isList) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Grid Button
          val isGrid = layoutMode == MediaLayoutMode.GRID
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(24.dp))
              .background(if (isGrid) MaterialTheme.colorScheme.primary else Color.Transparent)
              .clickable { onLayoutModeChange(MediaLayoutMode.GRID) },
            contentAlignment = Alignment.Center,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Filled.GridView,
                contentDescription = null,
                tint = if (isGrid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                "Grid",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isGrid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── 3. Cover Art Size ─────────────────────────────────────────────────
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Art Cover Size",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
          ) {
            Text(
              text = "$coverArtSize dp",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
          value = coverArtSize.toFloat(),
          onValueChange = { onCoverArtSizeChange(it.roundToInt()) },
          valueRange = 40f..120f,
          steps = 15,
          colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
          ),
          modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
      ) {
        Text("Done", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {},
  )
}
