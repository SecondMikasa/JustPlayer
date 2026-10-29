package xyz.mpv.rex.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.mpv.rex.R
import xyz.mpv.rex.ui.player.controls.components.PlayerUpdate

/**
 * Non-blocking overlay shown below the play/pause area when a video resumes
 * from a saved mid-way position. Matches the existing player UI (PlayerUpdate
 * style — respects glass/non-glass theme). Auto-dismisses after [durationMs]
 * without blocking playback. Tapping "Restart" seeks back to 0.
 *
 * @param positionText  Formatted resume position, e.g. "31:42"
 * @param durationMs    How long (ms) the overlay stays before auto-dismissing
 * @param onRestart     Called when the user taps "Restart"
 * @param onAutoDismiss Called when the timer expires
 */
@Composable
fun ResumePlaybackOverlay(
    positionText: String,
    durationMs: Long,
    onRestart: () -> Unit,
    onAutoDismiss: () -> Unit,
) {
    // Auto-dismiss after durationMs
    LaunchedEffect(Unit) {
        delay(durationMs)
        onAutoDismiss()
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit  = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
    ) {
        // PlayerUpdate is the same composable used by seek-time, speed, and chapter
        // overlays — it automatically applies the glass or flat surface style that
        // matches whatever theme the user has set.
        PlayerUpdate {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.resume_overlay_message, positionText),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.width(4.dp))

                TextButton(onClick = onRestart) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.resume_overlay_restart),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
