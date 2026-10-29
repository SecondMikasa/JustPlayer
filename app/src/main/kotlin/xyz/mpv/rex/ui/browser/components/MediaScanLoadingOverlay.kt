package xyz.mpv.rex.ui.browser.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen loading overlay shown during initial media file scanning.
 * Displays progress information and scanning status.
 *
 * @param isVisible Whether to show the loading overlay
 * @param rootName Current storage root being scanned (e.g., "Internal Storage")
 * @param scannedItems Total number of media items found so far
 * @param completedRoots Number of storage roots completed
 * @param totalRoots Total number of storage roots to scan
 */
@Composable
fun MediaScanLoadingOverlay(
  isVisible: Boolean,
  rootName: String? = null,
  scannedItems: Int = 0,
  completedRoots: Int = 0,
  totalRoots: Int = 1,
) {
  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          color = MaterialTheme.colorScheme.background
        ),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .padding(32.dp)
          .fillMaxSize(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        // Circular progress indicator
        CircularProgressIndicator(
          modifier = Modifier.size(56.dp),
          color = MaterialTheme.colorScheme.primary,
          strokeWidth = 4.dp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Status text
        Text(
          text = "Searching for media files...",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Current root being scanned
        if (rootName != null) {
          Text(
            text = "Scanning: $rootName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(12.dp))
        }

        // Progress bar for roots completion
        if (totalRoots > 1) {
          val progress = if (totalRoots > 0) {
            (completedRoots.toFloat() / totalRoots.toFloat()).coerceIn(0f, 1f)
          } else {
            0f
          }

          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .height(4.dp)
              .fillMaxSize(0.6f),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Scanning $completedRoots of $totalRoots storage locations",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(12.dp))
        }

        // Items found counter
        Text(
          text = "$scannedItems media files found",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          fontSize = 14.sp,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
          text = "This is done only once per app installation",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
          fontSize = 11.sp,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
