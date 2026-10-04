package xyz.mpv.rex.ui.utils

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import xyz.mpv.rex.presentation.Screen

val LocalBackStack: ProvidableCompositionLocal<NavBackStack<Screen>> =
  compositionLocalOf { error("LocalBackStack not initialized!") }

/**
 * Carries the string resource ID of the preference to scroll-to and highlight
 * when navigating from settings search results to a preference screen.
 * Null means no highlight (normal navigation).
 */
val LocalHighlightedPrefKey: ProvidableCompositionLocal<Int?> =
  compositionLocalOf { null }
