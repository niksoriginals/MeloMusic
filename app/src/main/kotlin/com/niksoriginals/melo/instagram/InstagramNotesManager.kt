package com.niksoriginals.melo.instagram

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import com.niksoriginals.melo.constants.InstagramCsrfTokenKey
import com.niksoriginals.melo.constants.InstagramNotesAudienceKey
import com.niksoriginals.melo.constants.InstagramNotesClearOnPauseKey
import com.niksoriginals.melo.constants.InstagramNotesCustomCaptionKey
import com.niksoriginals.melo.constants.InstagramNotesEnabledKey
import com.niksoriginals.melo.constants.InstagramNotesListeningThresholdKey
import com.niksoriginals.melo.constants.InstagramSessionIdKey
import com.niksoriginals.melo.constants.InstagramUserIdKey
import com.niksoriginals.melo.constants.InstagramUsernameKey
import com.niksoriginals.melo.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class InstagramAudioTrack(
    val audioClusterId: String,
    val id: String,
    val title: String,
    val artist: String,
    val highlightStartTimeMs: Int = 0
)

@Singleton
class InstagramNotesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var currentSyncJob: Job? = null
    private var currentSongTitle: String? = null
    private var currentArtistName: String? = null
    private var lastSyncedTitle: String? = null
    private var lastSyncedArtist: String? = null
    private var lastActiveNoteId: String? = null
    private val clientUuid = UUID.randomUUID().toString()

    companion object {
        private const val TAG = "InstagramNotesManager"
        private const val USER_AGENT = "Instagram 449.0.0.52.84 Android (34/14; 480dpi; 1344x2992; Google/google; Pixel 8 Pro; husky; husky; en_US; 1079242191)"
        private const val IG_APP_ID = "567067343352427"
    }

    suspend fun isConfigured(): Boolean {
        val prefs = context.dataStore.data.first()
        val enabled = prefs[InstagramNotesEnabledKey] ?: false
        val session = prefs[InstagramSessionIdKey]
        val csrf = prefs[InstagramCsrfTokenKey]
        return enabled && !session.isNullOrBlank() && !csrf.isNullOrBlank()
    }

    private fun buildBearerToken(userId: String, sessionId: String): String {
        val json = JSONObject().apply {
            put("ds_user_id", userId)
            put("sessionid", sessionId)
            put("should_use_header_over_cookies", true)
        }.toString()
        return "Bearer IGT:2:" + Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun buildCookie(sessionId: String, csrfToken: String, userId: String?): String {
        return buildString {
            append("sessionid=").append(sessionId).append("; ")
            append("csrftoken=").append(csrfToken).append("; ")
            if (!userId.isNullOrBlank()) {
                append("ds_user_id=").append(userId).append("; ")
            }
        }
    }

    private fun applyCommonHeaders(
        builder: Request.Builder,
        sessionId: String,
        csrfToken: String,
        userId: String?
    ): Request.Builder {
        builder
            .header("User-Agent", USER_AGENT)
            .header("X-IG-App-ID", IG_APP_ID)
            .header("X-CSRFToken", csrfToken)
            .header("Cookie", buildCookie(sessionId, csrfToken, userId))
            .header("X-IG-Capabilities", "3brTv10=")
            .header("X-IG-Connection-Type", "WIFI")
            .header("X-IG-App-Locale", "en_US")
            .header("X-IG-Device-Locale", "en_US")
            .header("X-IG-Mapped-Locale", "en_US")

        if (!userId.isNullOrBlank()) {
            builder.header("Authorization", buildBearerToken(userId, sessionId))
            builder.header("IG-INTENDED-USER-ID", userId)
            builder.header("IG-U-DS-USER-ID", userId)
        }
        return builder
    }

    suspend fun searchMusic(
        query: String,
        sessionId: String,
        csrfToken: String,
        userId: String?
    ): InstagramAudioTrack? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val browseSessionId = UUID.randomUUID().toString()
            // 1. Try audio_global_search (native music search used in notes and reels)
            val searchUrl = "https://i.instagram.com/api/v1/music/audio_global_search/?query=$encodedQuery&browse_session_id=$browseSessionId"

            val reqBuilder = Request.Builder().url(searchUrl).get()
            val request = applyCommonHeaders(reqBuilder, sessionId, csrfToken, userId).build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext null
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val items = json.optJSONArray("items")
                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            val track = item.optJSONObject("track") ?: item
                            val clusterId = track.optString("audio_cluster_id").takeIf { it.isNotBlank() }
                                ?: track.optString("id").takeIf { it.isNotBlank() }
                            val id = track.optString("audio_asset_id").takeIf { it.isNotBlank() }
                                ?: track.optString("id", clusterId)
                            val title = track.optString("title")
                            val artist = track.optString("display_artist")

                            val highlights = track.optJSONArray("highlight_start_times_in_ms")
                            val highlightStartTime = if (highlights != null && highlights.length() > 0) {
                                highlights.optInt(0, 0)
                            } else {
                                0
                            }

                            if (!clusterId.isNullOrBlank() && !id.isNullOrBlank()) {
                                Timber.tag(TAG).d("Found track match: $title by $artist (clusterId=$clusterId, highlight=${highlightStartTime}ms)")
                                return@withContext InstagramAudioTrack(
                                    audioClusterId = clusterId,
                                    id = id,
                                    title = title,
                                    artist = artist,
                                    highlightStartTimeMs = highlightStartTime
                                )
                            }
                        }
                    }
                }
            }

            // Fallback: Try legacy music/search
            val fallbackUrl = "https://i.instagram.com/api/v1/music/search/?q=$encodedQuery"
            val fallbackReq = applyCommonHeaders(Request.Builder().url(fallbackUrl).get(), sessionId, csrfToken, userId).build()
            client.newCall(fallbackReq).execute().use { response ->
                val body = response.body?.string() ?: return@withContext null
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val items = json.optJSONArray("items") ?: return@withContext null
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val track = item.optJSONObject("track") ?: item
                        val clusterId = track.optString("audio_cluster_id").takeIf { it.isNotBlank() }
                            ?: track.optString("id").takeIf { it.isNotBlank() }
                        val id = track.optString("audio_asset_id").takeIf { it.isNotBlank() }
                            ?: track.optString("id", clusterId)
                        val title = track.optString("title")
                        val artist = track.optString("display_artist")

                        val highlights = track.optJSONArray("highlight_start_times_in_ms")
                        val highlightStartTime = if (highlights != null && highlights.length() > 0) {
                            highlights.optInt(0, 0)
                        } else {
                            0
                        }

                        if (!clusterId.isNullOrBlank() && !id.isNullOrBlank()) {
                            return@withContext InstagramAudioTrack(
                                audioClusterId = clusterId,
                                id = id,
                                title = title,
                                artist = artist,
                                highlightStartTimeMs = highlightStartTime
                            )
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error searching Instagram music for: $query")
            null
        }
    }

    suspend fun postNote(
        title: String,
        artist: String,
        customCaption: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.dataStore.data.first()
            val sessionId = prefs[InstagramSessionIdKey]?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("Instagram session ID is not set"))
            val csrfToken = prefs[InstagramCsrfTokenKey]?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("Instagram CSRF token is not set"))
            val userId = prefs[InstagramUserIdKey]
            val audience = prefs[InstagramNotesAudienceKey] ?: 0

            // Search Instagram's music library to attach the native audio card
            val cleanTitle = title.replace(Regex("""\s*\(.*?\)\s*|\s*\[.*?\]\s*"""), " ").trim()
            val trackMatch = searchMusic(
                query = "$cleanTitle $artist",
                sessionId = sessionId,
                csrfToken = csrfToken,
                userId = userId
            ) ?: searchMusic(
                query = cleanTitle,
                sessionId = sessionId,
                csrfToken = csrfToken,
                userId = userId
            ) ?: searchMusic(
                query = title,
                sessionId = sessionId,
                csrfToken = csrfToken,
                userId = userId
            )

            val url = "https://i.instagram.com/api/v1/notes/create_note"
            val dataJson = JSONObject().apply {
                put("audience", audience)
                put("_uuid", clientUuid)

                if (trackMatch != null) {
                    val caption = customCaption?.takeIf { it.isNotBlank() } ?: "𝗟𝗶𝘀𝘁𝗲𝗻𝗶𝗻𝗴 𝗼𝗻 𝗠𝗲𝗹𝗼"
                    put("text", caption.take(60))
                    put("note_style", 1)

                    val musicNoteCreateInfo = JSONObject().apply {
                        put("audio_cluster_id", trackMatch.audioClusterId)
                        put("audio_asset_id", trackMatch.id)
                        put("start_time", trackMatch.highlightStartTimeMs)
                        put("duration", 30000)
                        put("browse_session_id", UUID.randomUUID().toString())
                    }
                    val noteCreateInfo = JSONObject().apply {
                        put("music_note_create_info", musicNoteCreateInfo)
                    }
                    put("note_create_info", noteCreateInfo.toString())
                    Timber.tag(TAG).i("Posting native Instagram Music Note: '${trackMatch.title}' by ${trackMatch.artist}")
                } else {
                    val fallbackText = customCaption?.takeIf { it.isNotBlank() } ?: "🎧 $title • $artist | 𝗟𝗶𝘀𝘁𝗲𝗻𝗶𝗻𝗴 𝗼𝗻 𝗠𝗲𝗹𝗼"
                    put("text", fallbackText.take(60))
                    put("note_style", 0)
                    Timber.tag(TAG).i("Posting fallback styled text Note: '$fallbackText'")
                }
            }

            val signedBody = "signed_body=SIGNATURE." + URLEncoder.encode(dataJson.toString(), "UTF-8")
            val requestBody = signedBody.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())

            val reqBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

            val request = applyCommonHeaders(reqBuilder, sessionId, csrfToken, userId).build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(responseBody) }.getOrNull()
                val status = json?.optString("status")

                if (response.isSuccessful && status == "ok") {
                    val noteId = json.optString("id").takeIf { it.isNotBlank() }
                        ?: json.optJSONObject("note")?.optString("id")
                    lastActiveNoteId = noteId
                    lastSyncedTitle = title
                    lastSyncedArtist = artist
                    Timber.tag(TAG).i("Instagram Note successfully published (ID=$noteId) for '$title - $artist'")
                    Result.success(responseBody)
                } else {
                    val errorMsg = json?.optString("message").takeIf { !it.isNullOrBlank() }
                        ?: "HTTP ${response.code}: $responseBody"
                    Timber.tag(TAG).w("Failed to post Instagram Note: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Exception while posting Instagram Note")
            Result.failure(e)
        }
    }

    suspend fun deleteNote(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.dataStore.data.first()
            val sessionId = prefs[InstagramSessionIdKey]?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
            val csrfToken = prefs[InstagramCsrfTokenKey]?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("CSRF missing"))
            val userId = prefs[InstagramUserIdKey]
            val noteId = lastActiveNoteId

            val url = "https://i.instagram.com/api/v1/notes/delete_note"
            val dataJson = JSONObject().apply {
                if (!noteId.isNullOrBlank()) {
                    put("id", noteId)
                }
                put("_uuid", clientUuid)
            }

            val signedBody = "signed_body=SIGNATURE." + URLEncoder.encode(dataJson.toString(), "UTF-8")
            val requestBody = signedBody.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())

            val reqBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

            val request = applyCommonHeaders(reqBuilder, sessionId, csrfToken, userId).build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(responseBody) }.getOrNull()
                val status = json?.optString("status")

                if (response.isSuccessful && status == "ok") {
                    lastSyncedTitle = null
                    lastSyncedArtist = null
                    lastActiveNoteId = null
                    Timber.tag(TAG).i("Instagram Note successfully deleted")
                    Result.success(Unit)
                } else {
                    val msg = json?.optString("message") ?: "HTTP ${response.code}"
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error deleting note")
            Result.failure(e)
        }
    }

    fun onTrackChanged(title: String?, artist: String?, isPlaying: Boolean) {
        if (title == currentSongTitle && artist == currentArtistName && isPlaying) return

        currentSongTitle = title
        currentArtistName = artist
        currentSyncJob?.cancel()

        if (title.isNullOrBlank()) return

        if (isPlaying) {
            scheduleSync(title, artist.orEmpty())
        }
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            val title = currentSongTitle
            val artist = currentArtistName.orEmpty()
            if (!title.isNullOrBlank() && (title != lastSyncedTitle || artist != lastSyncedArtist)) {
                scheduleSync(title, artist)
            }
        } else {
            currentSyncJob?.cancel()
            scope.launch {
                val prefs = context.dataStore.data.first()
                val clearOnPause = prefs[InstagramNotesClearOnPauseKey] ?: false
                if (clearOnPause) {
                    delay(30_000L) // Wait 30 seconds after pause before clearing
                    deleteNote()
                }
            }
        }
    }

    private fun scheduleSync(title: String, artist: String) {
        currentSyncJob?.cancel()
        currentSyncJob = scope.launch {
            val prefs = context.dataStore.data.first()
            val enabled = prefs[InstagramNotesEnabledKey] ?: false
            if (!enabled) return@launch

            val thresholdSec = prefs[InstagramNotesListeningThresholdKey] ?: 20
            val customCaption = prefs[InstagramNotesCustomCaptionKey]

            // Debounce: Wait for listening threshold to avoid spam on quick skips
            if (thresholdSec > 0) {
                delay(thresholdSec * 1000L)
            }

            postNote(title = title, artist = artist, customCaption = customCaption)
        }
    }

    suspend fun saveSession(
        sessionId: String,
        csrfToken: String,
        userId: String?,
        username: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[InstagramNotesEnabledKey] = true
            prefs[InstagramSessionIdKey] = sessionId
            prefs[InstagramCsrfTokenKey] = csrfToken
            if (!userId.isNullOrBlank()) prefs[InstagramUserIdKey] = userId
            if (!username.isNullOrBlank()) prefs[InstagramUsernameKey] = username
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[InstagramNotesEnabledKey] = false
            prefs.remove(InstagramSessionIdKey)
            prefs.remove(InstagramCsrfTokenKey)
            prefs.remove(InstagramUserIdKey)
            prefs.remove(InstagramUsernameKey)
        }
        lastSyncedTitle = null
        lastSyncedArtist = null
        lastActiveNoteId = null
        currentSongTitle = null
        currentArtistName = null
    }
}
