package xyz.mpv.rex.ui.preferences

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.mpv.rex.ui.utils.LocalHighlightedPrefKey

/**
 * A card container for grouping related preferences, mimicking modern Android settings UI.
 */
@Composable
fun PreferenceCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    elevation = CardDefaults.cardElevation(
      defaultElevation = 0.dp,
    ),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      content()
    }
  }
}

/**
 * A divider to separate preferences within a card.
 */
@Composable
fun PreferenceDivider(
  modifier: Modifier = Modifier,
) {
  HorizontalDivider(
    modifier = modifier.padding(horizontal = 16.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
  )
}

/**
 * A section header for preferences, displayed outside cards.
 */
@Composable
fun PreferenceSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier.padding(horizontal = 32.dp, vertical = 16.dp),
  )
}

/**
 * Wraps a preference composable and temporarily highlights it with a background
 * flash when its [titleRes] matches [LocalHighlightedPrefKey]. The highlight
 * fades in quickly, stays for 2 seconds, then fades out.
 *
 * Usage:
 * ```
 * HighlightablePreference(R.string.pref_player_orientation) {
 *     SwitchPreference(...)
 * }
 * ```
 */
@Composable
fun HighlightablePreference(
  titleRes: Int,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val highlightedKey = LocalHighlightedPrefKey.current
  val isTarget = highlightedKey == titleRes

  var showHighlight by remember { mutableStateOf(false) }

  LaunchedEffect(isTarget) {
    if (isTarget) {
      showHighlight = true
      delay(2500)
      showHighlight = false
    }
  }

  val bgColor by animateColorAsState(
    targetValue = if (showHighlight)
      MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
      Color.Transparent,
    animationSpec = tween(durationMillis = if (showHighlight) 300 else 800),
    label = "pref_highlight",
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(bgColor),
  ) {
    content()
  }
}
