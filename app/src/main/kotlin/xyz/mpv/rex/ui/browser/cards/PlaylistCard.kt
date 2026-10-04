package xyz.mpv.rex.ui.browser.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import xyz.mpv.rex.database.entities.PlaylistEntity
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.domain.thumbnail.ThumbnailRepository
import xyz.mpv.rex.preferences.UiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * Card for displaying a playlist item
 * 
 * @param playlist The playlist entity to display
 * @param itemCount Number of items in the playlist
 * @param uiSettings Consolidated UI settings
 * @param onClick Action to perform when the card is clicked
 * @param onLongClick Action to perform when the card is long-pressed
 * @param onThumbClick Action to perform when the thumbnail is clicked
 * @param modifier Optional modifier for the card
 * @param isSelected Whether the card is in a selected state
 * @param isGridMode Whether the card should display in grid mode
 * @param gridColumns Number of columns if in grid mode
 * @param mostRecentVideoPath Path to the most recently played video in this playlist (for thumbnail)
 * @param thumbnailSize Width of the thumbnail
 * @param thumbnailAspectRatio Aspect ratio of the thumbnail
 */
@Composable
fun PlaylistCard(
  playlist: PlaylistEntity,
  itemCount: Int,
  modifier: Modifier = Modifier,
  uiSettings: UiSettings,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onThumbClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
  isGridMode: Boolean = false,
  gridColumns: Int = 1,
  mostRecentVideoPath: String? = null,
  thumbnailSize: Dp = 64.dp,
  thumbnailAspectRatio: Float = 1f,
) {
  // Thumbnail loading logic if a video path is provided
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val configuration = androidx.compose.ui.platform.LocalConfiguration.current
  val calculatedThumbnailSize = if (isGridMode) {
    if (gridColumns == 1) configuration.screenWidthDp.dp else 180.dp
  } else thumbnailSize

  var thumbnail by remember(mostRecentVideoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
  
  if (mostRecentVideoPath != null && uiSettings.showVideoThumbnails) {
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { calculatedThumbnailSize.toPx().roundToInt() }
    val thumbHeightPx = (thumbWidthPx / thumbnailAspectRatio).roundToInt()
    
    // Create a dummy Video object just for the thumbnail key/loading
    val dummyVideo = remember(mostRecentVideoPath) {
      Video(
        id = mostRecentVideoPath.hashCode().toLong(),
        title = "",
        displayName = "",
        path = mostRecentVideoPath,
        uri = if (mostRecentVideoPath.startsWith("/") || mostRecentVideoPath.startsWith("file://")) {
          val path = if (mostRecentVideoPath.startsWith("file://")) mostRecentVideoPath.removePrefix("file://") else mostRecentVideoPath
          android.net.Uri.fromFile(java.io.File(path))
        } else {
          android.net.Uri.parse(mostRecentVideoPath)
        },
        duration = 0,
        durationFormatted = "",
        size = 0,
        sizeFormatted = "",
        dateModified = 0,
        dateAdded = 0,
        mimeType = "video/*",
        bucketId = "",
        bucketDisplayName = "",
        width = 0,
        height = 0,
        fps = 0f,
        resolution = ""
      )
    }

    val thumbnailKey = remember(dummyVideo.id, thumbWidthPx, thumbHeightPx) {
      thumbnailRepository.thumbnailKey(dummyVideo, thumbWidthPx, thumbHeightPx)
    }

    LaunchedEffect(thumbnailKey) {
      thumbnailRepository.thumbnailReadyKeys.filter { it == thumbnailKey }.collect {
        thumbnail = thumbnailRepository.getThumbnailFromMemory(dummyVideo, thumbWidthPx, thumbHeightPx)
      }
    }

    LaunchedEffect(thumbnailKey) {
      if (thumbnail == null) {
        thumbnail = withContext(Dispatchers.IO) {
          thumbnailRepository.getThumbnail(dummyVideo, thumbWidthPx, thumbHeightPx)
        }
      }
    }
  }

  // Create a custom chip for playlist type
  val isNetwork = playlist.isM3uPlaylist
  val isMusic = playlist.isMusicPlaylist
  val chipText = if (isMusic) "Audio" else if (isNetwork) "Network" else "Video"
  val chipColor = if (isNetwork) androidx.compose.material3.MaterialTheme.colorScheme.tertiary else androidx.compose.material3.MaterialTheme.colorScheme.secondary
  val chipBgColor = if (isNetwork) androidx.compose.material3.MaterialTheme.colorScheme.tertiaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer

  val customThumbnail = thumbnail?.asImageBitmap()

  val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

  androidx.compose.material3.Card(
    modifier = modifier.combinedClickable(
      onClick = onClick,
      onLongClick = onLongClick
    ),
    shape = cardShape,
    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
      containerColor = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
      else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    ),
    elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
  ) {
    if (isGridMode) {
      androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
        // Thumbnail area
        androidx.compose.foundation.layout.Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(calculatedThumbnailSize)
            .background(
              androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                  androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                  androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                )
              )
            )
            .padding(16.dp),
          contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
          if (customThumbnail != null) {
            androidx.compose.foundation.Image(
              bitmap = customThumbnail,
              contentDescription = null,
              modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
              contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
          } else {
            androidx.compose.material3.Icon(
              Icons.AutoMirrored.Filled.PlaylistPlay,
              contentDescription = null,
              modifier = Modifier.fillMaxSize(0.6f),
              tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
          }
          
          // Badge overlay
          androidx.compose.foundation.layout.Box(
            modifier = Modifier
              .align(androidx.compose.ui.Alignment.TopEnd)
              .background(chipBgColor.copy(alpha = 0.8f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            androidx.compose.material3.Text(
              text = chipText,
              style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
              color = chipColor,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
          }
        }
        // Text area
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(12.dp)) {
          androidx.compose.material3.Text(
            text = playlist.name,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
          androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
          androidx.compose.material3.Text(
            text = "$itemCount Items",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
      ) {
        // Thumbnail area
        androidx.compose.foundation.layout.Box(
          modifier = Modifier
            .size(calculatedThumbnailSize)
            .background(
              androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                  androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                  androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                )
              ),
              androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ),
          contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
          if (customThumbnail != null) {
            androidx.compose.foundation.Image(
              bitmap = customThumbnail,
              contentDescription = null,
              modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
              contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
          } else {
            androidx.compose.material3.Icon(
              Icons.AutoMirrored.Filled.PlaylistPlay,
              contentDescription = null,
              modifier = Modifier.fillMaxSize(0.6f),
              tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
          }
        }
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
        
        // Text area
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
          androidx.compose.material3.Text(
            text = playlist.name,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
          androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
          androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
          ) {
            androidx.compose.material3.Text(
              text = "$itemCount Items",
              style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
              color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.Text(
              text = " • ",
              style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
              color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.Text(
              text = chipText,
              style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
              modifier = Modifier
                .background(chipBgColor, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
              color = chipColor,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
          }
        }
      }
    }
  }
}
