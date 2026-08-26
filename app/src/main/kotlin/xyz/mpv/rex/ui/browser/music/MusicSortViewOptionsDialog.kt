package xyz.mpv.rex.ui.browser.music

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
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
      SortFieldItem(MusicSortField.DATE_ADDED, "Date Added", Icons.Filled.CalendarToday),
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = true),
    shape = RoundedCornerShape(28.dp),
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier,
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
      ) {
        // Title
        Text(
          text = "Sort & View Options",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 12.dp),
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        )

        // ── 1. Sort by ────────────────────────────────────────────────────────
        Text(
          text = "Sort by",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sort Field Items Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          sortFields.forEach { item ->
            val isSelected = selectedSortField == item.field

            val animatedCardColor by animateColorAsState(
              targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
              animationSpec = tween(200),
              label = "sort_card_bg",
            )
            val animatedIconColor by animateColorAsState(
              targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              animationSpec = tween(200),
              label = "sort_card_icon",
            )

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .width(56.dp)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = { onSortFieldChange(item.field) },
                ),
            ) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(animatedCardColor),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.label,
                  tint = animatedIconColor,
                  modifier = Modifier.size(24.dp),
                )
              }

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sort Order Segmented Pill (^ A-Z / v Z-A)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
              BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
              RoundedCornerShape(24.dp),
            ),
        ) {
          // Ascending Button
          val isAscending = sortOrder == MusicSortOrder.ASCENDING
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
              .then(
                if (isAscending) {
                  Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(
                      BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                      RoundedCornerShape(24.dp),
                    )
                } else Modifier
              )
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = { onSortOrderChange(MusicSortOrder.ASCENDING) },
              )
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = if (isAscending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "A-Z",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isAscending) FontWeight.Bold else FontWeight.Medium,
                color = if (isAscending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          // Descending Button
          val isDescending = sortOrder == MusicSortOrder.DESCENDING
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
              .then(
                if (isDescending) {
                  Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(
                      BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                      RoundedCornerShape(24.dp),
                    )
                } else Modifier
              )
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = { onSortOrderChange(MusicSortOrder.DESCENDING) },
              )
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isDescending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Z-A",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isDescending) FontWeight.Bold else FontWeight.Medium,
                color = if (isDescending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 2. Layout ─────────────────────────────────────────────────────────
        Text(
          text = "Layout",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Layout Mode Segmented Pill (List / Grid)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
              BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
              RoundedCornerShape(24.dp),
            ),
        ) {
          // List Button
          val isList = layoutMode == MediaLayoutMode.LIST
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
              .then(
                if (isList) {
                  Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(
                      BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                      RoundedCornerShape(24.dp),
                    )
                } else Modifier
              )
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = { onLayoutModeChange(MediaLayoutMode.LIST) },
              )
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = null,
                tint = if (isList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "List",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isList) FontWeight.Bold else FontWeight.Medium,
                color = if (isList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          // Grid Button
          val isGrid = layoutMode == MediaLayoutMode.GRID
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
              .then(
                if (isGrid) {
                  Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(
                      BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                      RoundedCornerShape(24.dp),
                    )
                } else Modifier
              )
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = { onLayoutModeChange(MediaLayoutMode.GRID) },
              )
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.GridView,
                contentDescription = null,
                tint = if (isGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Grid",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isGrid) FontWeight.Bold else FontWeight.Medium,
                color = if (isGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 3. Cover Art Size ─────────────────────────────────────────────────
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Cover Art Size",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "$coverArtSize dp",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
          value = coverArtSize.toFloat(),
          onValueChange = { onCoverArtSizeChange(it.roundToInt()) },
          valueRange = 40f..96f,
          steps = 13,
          colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
          ),
          modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── 4. Done Button ────────────────────────────────────────────────────
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onDismiss,
          ) {
            Text(
              text = "Done",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {},
  )
}
