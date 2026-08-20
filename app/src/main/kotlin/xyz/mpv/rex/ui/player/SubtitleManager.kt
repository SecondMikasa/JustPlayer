package xyz.mpv.rex.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages subtitle state and operations, including local scanning.
 */
class SubtitleManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onShowToast: (String) -> Unit
) {
    companion object {
        private const val TAG = "SubtitleManager"
    }

    // ==================== State ====================

    private val _externalSubtitles = mutableListOf<String>()
    val externalSubtitles: List<String> get() = _externalSubtitles.toList()

    private val mpvPathToUriMap = mutableMapOf<String, String>()

    // ==================== Actions ====================

    fun addSubtitle(uri: Uri, select: Boolean = true, silent: Boolean = false) {
        val uriString = uri.toString()
        if (_externalSubtitles.contains(uriString)) {
            Log.d(TAG, "Subtitle already tracked, skipping: $uriString")
            return
        }

        val fileName = uri.lastPathSegment ?: "subtitle"
        if (!isValidSubtitleFile(fileName)) {
            if (!silent) onShowToast("Invalid subtitle file")
            return
        }

        // Take persistent URI permission for content:// URIs
        if (uri.scheme == "content") {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.i(TAG, "Persistent permission not taken for $uri")
            }
        }

        scope.launch(Dispatchers.IO) {
            runCatching {
                val mpvPath = uri.resolveUri(context) ?: uri.toString()
                val mode = if (select) "select" else "auto"
                
                // Store mapping for reliable physical deletion later
                mpvPathToUriMap[mpvPath] = uri.toString()
                
                MPVLib.command("sub-add", mpvPath, mode)

                if (!_externalSubtitles.contains(uriString)) {
                    _externalSubtitles.add(uriString)
                }

                if (!silent) {
                    withContext(Dispatchers.Main) {
                        onShowToast("Subtitle added: ${fileName.take(30)}")
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to add subtitle", e)
                if (!silent) {
                    withContext(Dispatchers.Main) {
                        onShowToast("Failed to load subtitle")
                    }
                }
            }
        }
    }

    fun removeSubtitle(id: Int, tracks: List<TrackNode>) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val trackToRemove = tracks.firstOrNull { it.id == id }
                
                if (trackToRemove?.external == true && trackToRemove.externalFilename != null) {
                    val mpvPath = trackToRemove.externalFilename
                    val originalUriString = mpvPathToUriMap[mpvPath] ?: mpvPath
                    _externalSubtitles.remove(originalUriString)
                    mpvPathToUriMap.remove(mpvPath)
                }
                
                MPVLib.command("sub-remove", id.toString())
                withContext(Dispatchers.Main) {
                    onShowToast("Subtitle removed")
                }
            }.onFailure {
                Log.e(TAG, "Failed to remove subtitle", it)
            }
        }
    }

    fun clearExternalSubtitles() {
        _externalSubtitles.clear()
        mpvPathToUriMap.clear()
    }

    // ==================== Utilities = ====================

    private fun isValidSubtitleFile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".srt") || lower.endsWith(".vtt") ||
                lower.endsWith(".ssa") || lower.endsWith(".ass")
    }
}
