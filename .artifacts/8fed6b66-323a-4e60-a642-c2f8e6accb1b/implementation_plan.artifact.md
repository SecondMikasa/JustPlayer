# Fix for Music Player: Cover Art, Queue, Volume, and Notifications

This plan addresses a comprehensive set of issues in the music player experience, including fullscreen video flashes for audio files, incomplete play queues, missing system volume HUD, and suppressed system notifications.

## User Review Required

> [!NOTE]
> All changes are backward compatible and maintain existing video player behaviors while fixing audio-specific issues.

## Proposed Changes

### Music Library & Queue Operations

#### [MODIFY] [FolderPlaylistOps.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/utils/media/FolderPlaylistOps.kt)
- In `generateFolderPlaylist()`: If `FileTypeUtils.isAudioFile(currentFile)` is true, include all sibling audio files in the folder playlist even if `browserPreferences.showAudioFiles.get()` is false.
- In `generateMediaLibraryPlaylist()`: Load all indexed songs using `MediaFileRepository.getAllSongs(context)` (if available, otherwise ensure `showAudioFiles` doesn't filter them out when we explicitly want music library). Actually, I should check if `MediaFileRepository.getAllSongs` exists or if I should use `getAllVideos` and filter differently.

#### [MODIFY] [MusicLibraryScreen.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/browser/music/MusicLibraryScreen.kt)
- In `Content()`: Update `playSong` to use `MediaUtils.playPlaylist` or similar if we want to pass the whole list immediately. The prompt says: "pass the full song list using `MediaUtils.playPlaylist(...)` so all songs in the view/album/artist are immediately passed to `PlayerActivity` as the play queue on frame 0."

### Player Activity & MPV Configuration

#### [MODIFY] [PlayerActivity.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/player/PlayerActivity.kt)
- `onCreate()` and `onNewIntent()`: Eagerly evaluate `isCurrentMediaAudio()` and set `volumeControlStream = AudioManager.STREAM_MUSIC` if true.
- Audio Detection on Frame 0: Set `audio-display=no` and `vid=no` for audio media to prevent fullscreen album art flash. Restore `vid="auto"` and `audio-display="attachment"` for video.
- `hasVideoTrack()`: Ignore tracks where `albumart == true`.
- `onKeyDown()` / `onKeyUp()`: If `viewModel.isAudioMedia.value` is true, forward volume keys to `super` so system HUD shows.
- Eager Cover Art: Extract cover art immediately in `onCreate`, `onNewIntent`, and `loadPlaylistItemInternal`.
- `createVideoForUri()`: Improve audio detection for `content://` URIs.

#### [MODIFY] [AudioPlayerControls.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/player/controls/AudioPlayerControls.kt)
- Update `DisposableEffect`: Use `WindowInsetsControllerCompat.BEHAVIOR_DEFAULT` and `show(WindowInsetsCompat.Type.systemBars())` for audio player.
- Guard bitmap rendering with `takeIf { !it.isRecycled }`.

#### [MODIFY] [PlayerViewModel.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/player/PlayerViewModel.kt)
- Initialize `currentThumbnail` with `SharingStarted.Eagerly` using initial value from `miniPlayerStateManager.state.value.thumbnail`. (I need to find where `currentThumbnail` is defined, maybe it's missing or I missed it).

### Mini Player & Background Service

#### [MODIFY] [MediaPlaybackService.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/player/MediaPlaybackService.kt)
- In `updateMediaSession()`: Use `miniPlayerStateManager.state.value.thumbnail` if `MediaPlaybackService.thumbnail` is null.

#### [MODIFY] [MiniPlayerStateManager.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/browser/miniplayer/MiniPlayerStateManager.kt)
- In `updateState()`: Preserve active thumbnails if not explicitly provided.

### Thumbnail Utilities

#### [MODIFY] [MediaThumbnailUtils.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/utils/media/MediaThumbnailUtils.kt)
- `extractThumbnailOrCoverArt()`: Allow audio thumbnails even if mostly solid (album covers can be solid).
- `createVideoForUri()`: Detect `isAudio` properly for `content://` URIs using MIME type.

## Verification Plan

### Automated Tests
- Compile check: `.\gradlew compileDebugKotlin`

### Manual Verification
- Verify no fullscreen flash when opening audio with cover art.
- Verify cover art shows in mini player and audio controls.
- Verify volume buttons show system HUD in audio player.
- Verify status bar and notifications are visible in audio player.
- Verify queue is populated with all songs when opening from Music Library.
