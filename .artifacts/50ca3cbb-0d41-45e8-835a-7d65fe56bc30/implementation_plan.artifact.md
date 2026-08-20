# Remove RexShorts Feature

This plan outlines the steps to completely remove the "RexShorts" feature from the app, including its UI components, preferences, database tables, and related resources.

## User Review Required

> [!IMPORTANT]
> This change includes a database migration (version 16 to 17) that drops the `shorts_media` table. This will permanently delete any data stored in that table (e.g., "loved" or "blocked" shorts).

## Proposed Changes

### [Core]

#### [MODIFY] [BrowserPreferences.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/preferences/BrowserPreferences.kt)
- Remove all RexShorts-related preference definitions (`enableShorts`, `autoSwipeShorts`, etc.).

#### [MODIFY] [MpvExDatabase.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/database/MpvExDatabase.kt)
- Remove `ShortsMediaEntity` from the `entities` list.
- Remove `shortsMediaDao()` abstract function.
- Increment version to 17.

#### [MODIFY] [DatabaseModule.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/di/DatabaseModule.kt)
- Add `MIGRATION_16_17` to drop the `shorts_media` table.
- Remove the `shortsMediaDao` injection from the Koin module.

---

### [UI & Navigation]

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/browser/MainScreen.kt)
- Remove the "Shorts" tab from the bottom navigation.
- Remove logic that handles special navigation behavior for the Shorts tab (e.g., back button handling, navigation bar visibility/color).

#### [MODIFY] [PreferencesScreen.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/preferences/PreferencesScreen.kt)
- Remove the "RexShorts" section from the main settings screen.

#### [MODIFY] [AppearancePreferencesScreen.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/preferences/AppearancePreferencesScreen.kt)
- Remove the toggle for enabling/disabling the Shorts tab in the "Bottom Navigation" section.

---

### [Deletions]

#### [DELETE] [xyz.mpv.rex.ui.browser.shorts package](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/browser/shorts)
- `ShortsScreen.kt`
- `ShortsViewModel.kt`
- `ShortsPlayerHost.kt`

#### [DELETE] [ShortsPreferencesScreen.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/preferences/ShortsPreferencesScreen.kt)
#### [DELETE] [BlockedShortsScreen.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/ui/preferences/BlockedShortsScreen.kt)
#### [DELETE] [ShortsMediaDao.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/database/dao/ShortsMediaDao.kt)
#### [DELETE] [ShortsMediaEntity.kt](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/kotlin/xyz/mpv/rex/database/entities/ShortsMediaEntity.kt)
#### [DELETE] [shorts_dummy_layout.xml](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/res/layout/shorts_dummy_layout.xml)

---

### [Resources]

#### [MODIFY] [strings.xml](file:///C:/Users/Kumar Arnim/TheCodes/Projects/justplayer/app/src/main/res/values/strings.xml) (and other locales)
- Remove all strings related to RexShorts (keys starting with `pref_category_rexshorts`, `pref_enable_rexshorts`, `shorts_`, etc.).

## Verification Plan

### Automated Tests
- Run `gradle :app:assembleDebug` to ensure the project builds successfully without any broken references.

### Manual Verification
1. Launch the app and verify the "Shorts" tab is gone from the bottom navigation.
2. Go to Settings and verify the "RexShorts" section is removed.
3. Go to Appearance settings and verify the "RexShorts" tab toggle is removed.
4. Verify that the app still functions normally for regular video playback.
