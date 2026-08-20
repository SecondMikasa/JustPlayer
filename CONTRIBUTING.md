Looking to report an issue/bug or make a feature request? Please refer to the [README](README.md) file.

Thanks for your interest in contributing to JustPlayer!

This guide outlines our development practices, code guidelines, and git workflow.

---

## Code Contributions

We welcome bug fixes, performance optimizations, and feature enhancements.

### Build Setup & Verification

* **SDK/JDK Requirements:** Java 17
* **Build Tool:** Gradle

#### Standard Build Commands
* **Build Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
* **Build Release APK:**
  ```bash
  ./gradlew assembleRelease
  ```

#### Verification
There is no automated test suite. Contributors must perform manual verification on a physical Android device or emulator before submitting a Pull Request. Check Android Logcat during playback to ensure no unexpected exceptions or errors are thrown.

---

### Core Architecture & Key Patterns

We follow a modular, **Ops/Manager-driven architecture** to keep our UI controllers thin and testing simple.

1. **ViewModels as Coordinators:** ViewModels (like `PlayerViewModel`) should only manage UI state representation and delegate all business logic to dedicated Manager classes.
2. **Managers for Domain Logic:** Specialized operations belong in manager classes (e.g., `PlaybackManager` for seeking, `PlaylistManager` for shuffling, `SubtitleManager` for downloads).
3. **Unified Media Scanning:** Never scan the filesystem directly. Use `CoreMediaScanner` (via `FileSystemOps` and `MediaMetadataOps`) for all media file discovery to ensure folder counts and badge states are handled correctly.
4. **UI Consistency:** Use `BaseMediaCard` for any list or grid media representations to maintain aspect ratio and badge styling consistency.

#### Ops/Manager Pattern Example:
```kotlin
// 1. Player View (Jetpack Compose) triggers an action on the coordinator
PlayPauseButton(onClick = { playerViewModel.togglePlayPause() })

// 2. Coordinator (ViewModel) delegates straight to the PlaybackManager
class PlayerViewModel(
    private val playbackManager: PlaybackManager
) : ViewModel() {
    fun togglePlayPause() {
        playbackManager.togglePlay()
    }
}

// 3. Manager (PlaybackManager) handles the direct domain/video engine logic
class PlaybackManager(private val mpvLib: MPVLib) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    fun togglePlay() {
        val newState = !_isPlaying.value
        mpvLib.setPropertyBoolean("pause", !newState)
        _isPlaying.value = newState
    }
}
```

---

### License & Attribution

If you are porting code, patterns, or assets from other media players (e.g., `mpv-android`, `mpvEx`, `mpvKt`, `Next Player`, `Gramophone`), you **must** disclose this in your Pull Request description. Include links to the source repository and ensure the license is compatible with our **Apache License 2.0**.

---

### Git Workflow

To keep the repository history clean and manageable, we adhere to the following Git rules:

* **Feature Branches:** Create a new branch for every feature or fix, branched off `master`.
* **Atomic Commits:** Separate structural refactoring from visual/functional fixes into distinct commits.
* **Keep Branches Updated:** Use `rebase` instead of merge to bring your branch up to date with `master`.
* **Commit Messages:** We follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g. `fix:`, `feat:`, `refactor:`, `chore:`) to keep our commit history clear and organized. You are responsible for keeping your commits clean and conforming; non-conforming commits will need to be reworded or squashed by you before merge.

### Pull Requests

* Target the `master` branch.
* Keep PRs scoped to a single fix or feature — split unrelated changes into separate PRs.
* Reference the related issue number if one exists.
* Describe *what* changed and *why*; the diff already shows *how*.

### Communication

GitHub Issues and Pull Request discussions are our primary channels for communication. Feel free to open a draft issue to discuss design proposals before writing significant code.
