package com.niksoriginals.melo.utils

import com.niksoriginals.melo.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope

class ScrobbleManager(
    private val scope: CoroutineScope,
    var minSongDuration: Int = 30,
    var scrobbleDelayPercent: Float = 0.5f,
    var scrobbleDelaySeconds: Int = 50
) {
    var useNowPlaying = true
    var enableScrobbling = false
    var useSendLikes = false

    fun destroy() {}
    fun onSongStart(metadata: MediaMetadata?, duration: Long? = null) {}
    fun onSongResume(metadata: MediaMetadata) {}
    fun onSongPause() {}
    fun onPlayerStateChanged(isPlaying: Boolean, metadata: MediaMetadata?, duration: Long? = null) {}
    fun onSongStop() {}
}
