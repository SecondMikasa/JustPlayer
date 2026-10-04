package xyz.mpv.rex.ui.preferences

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import xyz.mpv.rex.R
import xyz.mpv.rex.preferences.PlayerPreferences
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.player.BackgroundPlaybackMode
import xyz.mpv.rex.ui.player.PlayerOrientation
import xyz.mpv.rex.ui.player.controls.components.sheets.toFixed
import xyz.mpv.rex.ui.utils.LocalBackStack
import xyz.mpv.rex.ui.utils.LocalHighlightedPrefKey
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import xyz.mpv.rex.ui.preferences.components.SwitchPreference
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Serializable
object PlayerPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val preferences = koinInject<PlayerPreferences>()
    val highlightedKey = LocalHighlightedPrefKey.current

    // Maps each preference titleRes → the LazyColumn item index of the card that contains it.
    // Item indices: 0=header_general, 1=card_general, 2=header_mini, 3=card_mini,
    //   4=header_seeking, 5=card_seeking, 6=header_gestures, 7=card_gestures,
    //   8=header_controls, 9=card_controls, 10=header_display, 11=card_display
    val prefToItemIndex = remember {
      mapOf(
        R.string.pref_player_orientation to 1,
        R.string.pref_player_save_position_on_quit to 1,
        R.string.pref_player_close_after_eof to 1,
        R.string.pref_player_autoplay_next_video to 1,
        R.string.pref_autoplay_title to 1,
        R.string.pref_player_remember_brightness to 1,
        R.string.pref_player_auto_pip to 1,
        R.string.pref_player_keep_screen_on_when_paused_title to 1,
        R.string.pref_player_resume_on_unlock_title to 1,
        R.string.pref_player_background_playback to 3,
        R.string.pref_player_play_in_mini_player to 3,
        R.string.show_splash_ovals_on_double_tap_to_seek to 5,
        R.string.pref_player_show_circular_double_tap_seek_title to 5,
        R.string.show_time_on_double_tap_to_seek to 5,
        R.string.pref_player_use_precise_seeking to 5,
        R.string.pref_player_show_seekbar_when_seeking_title to 5,
        R.string.pref_player_white_seekbar_title to 5,
        R.string.pref_player_hide_osd_text_title to 5,
        R.string.pref_player_custom_skip_duration_title to 5,
        R.string.pref_player_gestures_brightness to 7,
        R.string.pref_player_gestures_volume to 7,
        R.string.pref_player_gestures_pinch_to_zoom to 7,
        R.string.pref_player_gestures_pan_and_zoom to 7,
        R.string.pref_player_gestures_horizontal_swipe_to_seek to 7,
        R.string.pref_player_gestures_swipe_to_subtitle_seek_title to 7,
        R.string.pref_player_gestures_move_subtitle_by_dragging_title to 7,
        R.string.pref_player_gestures_horizontal_swipe_sensitivity to 7,
        R.string.pref_player_gestures_hold_for_multiple_speed to 7,
        R.string.pref_player_dynamic_speed_overlay to 7,
        R.string.pref_player_show_speed_indicator_overlay to 7,
        R.string.pref_player_controls_disable_media_buttons_title to 9,
        R.string.pref_player_controls_allow_gestures_in_panels to 9,
        R.string.swap_the_volume_and_brightness_slider to 9,
        R.string.pref_player_controls_show_loading_circle to 9,
        R.string.pref_player_display_show_status_bar to 11,
        R.string.pref_player_display_show_navigation_bar to 11,
        R.string.pref_player_display_reduce_player_animation to 11,
      )
    }

    val listState = rememberLazyListState()

    // Scroll to the card that contains the highlighted preference
    LaunchedEffect(highlightedKey) {
      if (highlightedKey != null) {
        val itemIndex = prefToItemIndex[highlightedKey]
        if (itemIndex != null) {
          kotlinx.coroutines.delay(150)
          listState.scrollToItem(itemIndex)
        }
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = { 
            Text(
              text = stringResource(id = R.string.pref_player),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            ) 
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Outlined.ArrowBack, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
        )
      },
    ) { padding ->
      val navBarHeight = xyz.mpv.rex.ui.browser.LocalNavigationBarHeight.current
      ProvidePreferenceLocals {
        LazyColumn(
          state = listState,
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = navBarHeight + 16.dp),
        ) {
          // General Section
          item(key = "header_general") {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_general))
          }

          item(key = "card_general") {
            PreferenceCard {
              val orientation by preferences.orientation.collectAsState()
              HighlightablePreference(R.string.pref_player_orientation) {
                ListPreference(
                  value = orientation,
                  onValueChange = preferences.orientation::set,
                  values = PlayerOrientation.entries,
                  valueToText = { AnnotatedString(context.getString(it.titleRes)) },
                  title = { Text(text = stringResource(id = R.string.pref_player_orientation)) },
                  summary = { 
                    Text(
                      text = stringResource(id = orientation.titleRes),
                      color = MaterialTheme.colorScheme.outline,
                    ) 
                  },
                )
              }

              PreferenceDivider()

              val savePositionOnQuit by preferences.savePositionOnQuit.collectAsState()
              HighlightablePreference(R.string.pref_player_save_position_on_quit) {
                SwitchPreference(
                  value = savePositionOnQuit,
                  onValueChange = preferences.savePositionOnQuit::set,
                  title = { Text(stringResource(R.string.pref_player_save_position_on_quit)) },
                )
              }

              PreferenceDivider()

              val closeAfterEndOfVideo by preferences.closeAfterReachingEndOfVideo.collectAsState()
              HighlightablePreference(R.string.pref_player_close_after_eof) {
                SwitchPreference(
                  value = closeAfterEndOfVideo,
                  onValueChange = preferences.closeAfterReachingEndOfVideo::set,
                  title = { Text(stringResource(id = R.string.pref_player_close_after_eof)) },
                )
              }

              PreferenceDivider()

              val autoplayNextVideo by preferences.autoplayNextVideo.collectAsState()
              HighlightablePreference(R.string.pref_player_autoplay_next_video) {
                SwitchPreference(
                  value = autoplayNextVideo,
                  onValueChange = preferences.autoplayNextVideo::set,
                  title = { Text(text = stringResource(R.string.pref_player_autoplay_next_video)) },
                  summary = {
                    Text(
                      text = if (autoplayNextVideo)
                        stringResource(R.string.pref_player_autoplay_next_video_summary_on)
                      else
                        stringResource(R.string.pref_player_autoplay_next_video_summary_off),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }

              PreferenceDivider()

              val playlistMode by preferences.playlistMode.collectAsState()
              HighlightablePreference(R.string.pref_autoplay_title) {
                SwitchPreference(
                  value = playlistMode,
                  onValueChange = preferences.playlistMode::set,
                  title = { Text(text = stringResource(R.string.pref_autoplay_title)) },
                  summary = {
                    Text(
                      text = if (playlistMode)
                        stringResource(R.string.pref_player_playlist_mode_summary_on)
                      else
                        stringResource(R.string.pref_player_playlist_mode_summary_off),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }

              PreferenceDivider()

              val rememberBrightness by preferences.rememberBrightness.collectAsState()
              HighlightablePreference(R.string.pref_player_remember_brightness) {
                SwitchPreference(
                  value = rememberBrightness,
                  onValueChange = preferences.rememberBrightness::set,
                  title = { Text(text = stringResource(R.string.pref_player_remember_brightness)) },
                )
              }

              PreferenceDivider()

              val autoPiPOnNavigation by preferences.autoPiPOnNavigation.collectAsState()
              HighlightablePreference(R.string.pref_player_auto_pip) {
                SwitchPreference(
                  value = autoPiPOnNavigation,
                  onValueChange = preferences.autoPiPOnNavigation::set,
                  title = { Text(stringResource(R.string.pref_player_auto_pip)) },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_player_auto_pip_summary),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }

              PreferenceDivider()

              val keepScreenOnWhenPaused by preferences.keepScreenOnWhenPaused.collectAsState()
              HighlightablePreference(R.string.pref_player_keep_screen_on_when_paused_title) {
                SwitchPreference(
                  value = keepScreenOnWhenPaused,
                  onValueChange = preferences.keepScreenOnWhenPaused::set,
                  title = { Text(stringResource(R.string.pref_player_keep_screen_on_when_paused_title)) },
                  summary = {
                    Text(
                      text = if (keepScreenOnWhenPaused)
                        stringResource(R.string.pref_player_keep_screen_on_when_paused_summary_on)
                      else
                        stringResource(R.string.pref_player_keep_screen_on_when_paused_summary_off),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }

              PreferenceDivider()

              val resumeOnUnlock by preferences.resumeOnUnlock.collectAsState()
              HighlightablePreference(R.string.pref_player_resume_on_unlock_title) {
                SwitchPreference(
                  value = resumeOnUnlock,
                  onValueChange = preferences.resumeOnUnlock::set,
                  title = { Text(stringResource(R.string.pref_player_resume_on_unlock_title)) },
                  summary = {
                    Text(
                      text = if (resumeOnUnlock)
                        stringResource(R.string.pref_player_resume_on_unlock_summary_on)
                      else
                        stringResource(R.string.pref_player_resume_on_unlock_summary_off),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }
            }
          }

          // Mini Player Section
          item(key = "header_mini") {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_section_mini_player))
          }

          item(key = "card_mini") {
            PreferenceCard {
              val backgroundPlayback by preferences.backgroundPlayback.collectAsState()
              HighlightablePreference(R.string.pref_player_background_playback) {
                ListPreference(
                  value = backgroundPlayback,
                  onValueChange = preferences.backgroundPlayback::set,
                  values = BackgroundPlaybackMode.entries,
                  valueToText = { AnnotatedString(context.getString(it.titleRes)) },
                  title = { Text(text = stringResource(id = R.string.pref_player_background_playback)) },
                  summary = {
                    Text(
                      text = stringResource(id = backgroundPlayback.titleRes),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }

              PreferenceDivider()

              val playInMiniPlayerDirectly by preferences.playInMiniPlayerDirectly.collectAsState()
              HighlightablePreference(R.string.pref_player_play_in_mini_player) {
                SwitchPreference(
                  value = playInMiniPlayerDirectly,
                  onValueChange = preferences.playInMiniPlayerDirectly::set,
                  title = { Text(text = stringResource(R.string.pref_player_play_in_mini_player)) },
                  summary = {
                    Text(
                      text = if (playInMiniPlayerDirectly)
                        stringResource(R.string.pref_player_play_in_mini_player_summary_on)
                      else
                        stringResource(R.string.pref_player_play_in_mini_player_summary_off),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }
            }
          }
          // Seeking Section
          item(key = "header_seeking") {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_seeking_title))
          }

          item(key = "card_seeking") {
            PreferenceCard {
              val showDoubleTapOvals by preferences.showDoubleTapOvals.collectAsState()
              HighlightablePreference(R.string.show_splash_ovals_on_double_tap_to_seek) {
                SwitchPreference(
                  value = showDoubleTapOvals,
                  onValueChange = preferences.showDoubleTapOvals::set,
                  title = { Text(stringResource(R.string.show_splash_ovals_on_double_tap_to_seek)) },
                )
              }

              PreferenceDivider()

              val showCircularDoubleTapSeek by preferences.showCircularDoubleTapSeek.collectAsState()
              HighlightablePreference(R.string.pref_player_show_circular_double_tap_seek_title) {
                SwitchPreference(
                  value = showCircularDoubleTapSeek,
                  onValueChange = preferences.showCircularDoubleTapSeek::set,
                  title = { Text(stringResource(R.string.pref_player_show_circular_double_tap_seek_title)) },
                  summary = { Text(stringResource(R.string.pref_player_show_circular_double_tap_seek_summary)) },
                )
              }

              PreferenceDivider()

              val showSeekTimeWhileSeeking by preferences.showSeekTimeWhileSeeking.collectAsState()
              HighlightablePreference(R.string.show_time_on_double_tap_to_seek) {
                SwitchPreference(
                  value = showSeekTimeWhileSeeking,
                  onValueChange = preferences.showSeekTimeWhileSeeking::set,
                  title = { Text(stringResource(R.string.show_time_on_double_tap_to_seek)) },
                )
              }

              PreferenceDivider()

              val usePreciseSeeking by preferences.usePreciseSeeking.collectAsState()
              HighlightablePreference(R.string.pref_player_use_precise_seeking) {
                SwitchPreference(
                  value = usePreciseSeeking,
                  onValueChange = preferences.usePreciseSeeking::set,
                  title = { Text(stringResource(R.string.pref_player_use_precise_seeking)) },
                )
              }

              PreferenceDivider()

              val showSeekBarWhenSeeking by preferences.showSeekBarWhenSeeking.collectAsState()
              HighlightablePreference(R.string.pref_player_show_seekbar_when_seeking_title) {
                SwitchPreference(
                  value = showSeekBarWhenSeeking,
                  onValueChange = preferences.showSeekBarWhenSeeking::set,
                  title = { Text(stringResource(R.string.pref_player_show_seekbar_when_seeking_title)) },
                  summary = { Text(stringResource(R.string.pref_player_show_seekbar_when_seeking_summary)) },
                )
              }

              PreferenceDivider()

              val whiteSeekBar by preferences.whiteSeekBar.collectAsState()
              HighlightablePreference(R.string.pref_player_white_seekbar_title) {
                SwitchPreference(
                  value = whiteSeekBar,
                  onValueChange = preferences.whiteSeekBar::set,
                  title = { Text(stringResource(R.string.pref_player_white_seekbar_title)) },
                  summary = { Text(stringResource(R.string.pref_player_white_seekbar_summary)) },
                )
              }

              PreferenceDivider()

              val hideOsdText by preferences.hideOsdText.collectAsState()
              HighlightablePreference(R.string.pref_player_hide_osd_text_title) {
                SwitchPreference(
                  value = hideOsdText,
                  onValueChange = preferences.hideOsdText::set,
                  title = { Text(stringResource(R.string.pref_player_hide_osd_text_title)) },
                  summary = { Text(stringResource(R.string.pref_player_hide_osd_text_summary)) },
                )
              }

              PreferenceDivider()

              val customSkipDuration by preferences.customSkipDuration.collectAsState()
              HighlightablePreference(R.string.pref_player_custom_skip_duration_title) {
                SliderPreference(
                  value = customSkipDuration.toFloat(),
                  onValueChange = { preferences.customSkipDuration.set(it.roundToInt()) },
                  title = { Text(stringResource(R.string.pref_player_custom_skip_duration_title)) },
                  valueRange = 5f..180f,
                  summary = {
                     val summaryText = stringResource(R.string.pref_player_custom_skip_duration_summary)
                     Text(
                       "$summaryText ($customSkipDuration s)",
                       color = MaterialTheme.colorScheme.outline,
                     )
                  },
                  onSliderValueChange = { preferences.customSkipDuration.set(it.roundToInt()) },
                  sliderValue = customSkipDuration.toFloat(),
                )
              }
            }
          }
          // Gestures Section
          item(key = "header_gestures") {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_gestures))
          }

          item(key = "card_gestures") {
            PreferenceCard {
              val brightnessGesture by preferences.brightnessGesture.collectAsState()
              HighlightablePreference(R.string.pref_player_gestures_brightness) {
                SwitchPreference(
                  value = brightnessGesture,
                  onValueChange = preferences.brightnessGesture::set,
                  title = { Text(stringResource(R.string.pref_player_gestures_brightness)) },
                )
              }

              PreferenceDivider()

              val volumeGesture by preferences.volumeGesture.collectAsState()
              HighlightablePreference(R.string.pref_player_gestures_volume) {
                SwitchPreference(
                  value = volumeGesture,
                  onValueChange = preferences.volumeGesture::set,
                  title = { Text(stringResource(R.string.pref_player_gestures_volume)) },
                )
              }

              PreferenceDivider()

              val pinchToZoomGesture by preferences.pinchToZoomGesture.collectAsState()
              HighlightablePreference(R.string.pref_player_gestures_pinch_to_zoom) {
                SwitchPreference(
                  value = pinchToZoomGesture,
                  onValueChange = preferences.pinchToZoomGesture::set,
                  title = { Text(stringResource(R.string.pref_player_gestures_pinch_to_zoom)) },
                )
              }

              PreferenceDivider()

              val panAndZoomEnabled by preferences.panAndZoomEnabled.collectAsState()
              HighlightablePreference(R.string.pref_player_gestures_pan_and_zoom) {
                SwitchPreference(
                  value = panAndZoomEnabled,
                  onValueChange = preferences.panAndZoomEnabled::set,
                  title = { Text(stringResource(R.string.pref_player_gestures_pan_and_zoom)) },
                  summary = { Text(stringResource(R.string.pref_player_gestures_pan_and_zoom_summary)) },
                )
              }

              PreferenceDivider()

              val horizontalSwipeToSeek by preferences.horizontalSwipeToSeek.collectAsState()
              HighlightablePreference(R.string.pref_player_gestures_horizontal_swipe_to_seek) {
                SwitchPreference(
                  value = horizontalSwipeToSeek,
                  onValueChange = preferences.horizontalSwipeToSeek::set,
                  title = { Text(stringResource(R.string.pref_player_gestures_horizontal_swipe_to_seek)) },
                )
              }

              PreferenceDivider()

              val swipeToSubtitleSeek by preferences.swipeToSubtitleSeek.collectAsState()
              HighlightablePreference(R.string.pref_player_gestures_swipe_to_subtitle_seek_title) {
                SwitchPreference(
                  value = swipeToSubtitleSeek,
                  onValueChange = preferences.swipeToSubtitleSeek::set,
                  title = { Text(stringResource(R.string.pref_player_gestures_swipe_to_subtitle_seek_title)) },
                  summary = { Text(stringResource(R.string.pref_player_gestures_swipe_to_subtitle_seek_summary)) },
                )
              }

              PreferenceDivider()

              val moveSubtitleByDragging by preferences.moveSubtitleByDragging.collectAsState()
              HighlightablePreference(R.string.pref_player_gestures_move_subtitle_by_dragging_title) {
                SwitchPreference(
                  value = moveSubtitleByDragging,
                  onValueChange = preferences.moveSubtitleByDragging::set,
                  title = { Text(stringResource(R.string.pref_player_gestures_move_subtitle_by_dragging_title)) },
                  summary = { Text(stringResource(R.string.pref_player_gestures_move_subtitle_by_dragging_summary)) },
                )
              }

              PreferenceDivider()

              val horizontalSwipeSensitivity by preferences.horizontalSwipeSensitivity.collectAsState()
              HighlightablePreference(R.string.pref_player_gestures_horizontal_swipe_sensitivity) {
                SliderPreference(
                  value = horizontalSwipeSensitivity,
                  onValueChange = { preferences.horizontalSwipeSensitivity.set(it.toFixed(3)) },
                  title = { Text(stringResource(R.string.pref_player_gestures_horizontal_swipe_sensitivity)) },
                  valueRange = 0.020f..0.1f,
                  summary = {
                    // ملاحظة: هذا الجزء يحتوي على النص المركب المستثنى (Low/Medium/High) — لم يُلمس عمداً
                    val sensitivityPercent = (horizontalSwipeSensitivity * 1000).toInt()
                    Text(
                      "Current: ${sensitivityPercent}/100 (${if (sensitivityPercent < 30) "Low" else if (sensitivityPercent < 55) "Medium" else "High"})",
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  onSliderValueChange = { preferences.horizontalSwipeSensitivity.set(it.toFixed(3)) },
                  sliderValue = horizontalSwipeSensitivity,
                )
              }

              PreferenceDivider()

              val holdForMultipleSpeed by preferences.holdForMultipleSpeed.collectAsState()
              HighlightablePreference(R.string.pref_player_gestures_hold_for_multiple_speed) {
                SliderPreference(
                  value = holdForMultipleSpeed,
                  onValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
                  title = { Text(stringResource(R.string.pref_player_gestures_hold_for_multiple_speed)) },
                  valueRange = 0f..6f,
                  summary = {
                    Text(
                      if (holdForMultipleSpeed == 0F) {
                        stringResource(R.string.generic_disabled)
                      } else {
                        "%.2fx".format(holdForMultipleSpeed)
                      },
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  onSliderValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
                  sliderValue = holdForMultipleSpeed,
                )
              }

              PreferenceDivider()

              val showDynamicSpeedOverlay by preferences.showDynamicSpeedOverlay.collectAsState()
              HighlightablePreference(R.string.pref_player_dynamic_speed_overlay) {
                SwitchPreference(
                  value = showDynamicSpeedOverlay,
                  onValueChange = preferences.showDynamicSpeedOverlay::set,
                  title = { Text(stringResource(R.string.pref_player_dynamic_speed_overlay)) },
                  summary = {
                    Text(
                      stringResource(R.string.pref_player_dynamic_speed_overlay_summary),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  }
                )
              }

              PreferenceDivider()

              val showSpeedIndicatorOverlay by preferences.showSpeedIndicatorOverlay.collectAsState()
              HighlightablePreference(R.string.pref_player_show_speed_indicator_overlay) {
                SwitchPreference(
                  value = showSpeedIndicatorOverlay,
                  onValueChange = preferences.showSpeedIndicatorOverlay::set,
                  title = { Text(stringResource(R.string.pref_player_show_speed_indicator_overlay)) },
                  summary = {
                    Text(
                      stringResource(R.string.pref_player_show_speed_indicator_overlay_summary),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  }
                )
              }
            }
          }
          // Controls Section
          item(key = "header_controls") {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_controls))
          }

          item(key = "card_controls") {
            PreferenceCard {
              val disableMediaButtons by preferences.disableMediaButtons.collectAsState()
              HighlightablePreference(R.string.pref_player_controls_disable_media_buttons_title) {
                SwitchPreference(
                  value = disableMediaButtons,
                  onValueChange = preferences.disableMediaButtons::set,
                  title = { Text(stringResource(id = R.string.pref_player_controls_disable_media_buttons_title)) },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_player_controls_disable_media_buttons_summary),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }

              PreferenceDivider()

              val allowGesturesInPanels by preferences.allowGesturesInPanels.collectAsState()
              HighlightablePreference(R.string.pref_player_controls_allow_gestures_in_panels) {
                SwitchPreference(
                  value = allowGesturesInPanels,
                  onValueChange = preferences.allowGesturesInPanels::set,
                  title = {
                    Text(
                      text = stringResource(id = R.string.pref_player_controls_allow_gestures_in_panels),
                    )
                  },
                )
              }

              PreferenceDivider()

              val swapVolumeAndBrightness by preferences.swapVolumeAndBrightness.collectAsState()
              HighlightablePreference(R.string.swap_the_volume_and_brightness_slider) {
                SwitchPreference(
                  value = swapVolumeAndBrightness,
                  onValueChange = preferences.swapVolumeAndBrightness::set,
                  title = { Text(stringResource(R.string.swap_the_volume_and_brightness_slider)) },
                )
              }

              PreferenceDivider()

              val showLoadingCircle by preferences.showLoadingCircle.collectAsState()
              HighlightablePreference(R.string.pref_player_controls_show_loading_circle) {
                SwitchPreference(
                  value = showLoadingCircle,
                  onValueChange = preferences.showLoadingCircle::set,
                  title = { Text(stringResource(R.string.pref_player_controls_show_loading_circle)) },
                )
              }
            }
          }
          // Display Section
          item(key = "header_display") {
            PreferenceSectionHeader(title = stringResource(R.string.pref_player_display))
          }

          item(key = "card_display") {
            PreferenceCard {
              val showSystemStatusBar by preferences.showSystemStatusBar.collectAsState()
              HighlightablePreference(R.string.pref_player_display_show_status_bar) {
                SwitchPreference(
                  value = showSystemStatusBar,
                  onValueChange = preferences.showSystemStatusBar::set,
                  title = { Text(stringResource(R.string.pref_player_display_show_status_bar)) },
                )
              }

              PreferenceDivider()

              val showSystemNavigationBar by preferences.showSystemNavigationBar.collectAsState()
              HighlightablePreference(R.string.pref_player_display_show_navigation_bar) {
                SwitchPreference(
                  value = showSystemNavigationBar,
                  onValueChange = preferences.showSystemNavigationBar::set,
                  title = { Text(stringResource(R.string.pref_player_display_show_navigation_bar)) },
                )
              }

              PreferenceDivider()

              val reduceMotion by preferences.reduceMotion.collectAsState()
              HighlightablePreference(R.string.pref_player_display_reduce_player_animation) {
                SwitchPreference(
                  value = reduceMotion,
                  onValueChange = preferences.reduceMotion::set,
                  title = { Text(stringResource(R.string.pref_player_display_reduce_player_animation)) },
                )
              }
            }
          }
        }
      }
    }
  }
}
