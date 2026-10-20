

package com.niksoriginals.melo.utils

import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.niksoriginals.melo.utils.BotDetectionMitigator
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_65_10
import com.music.innertube.models.YouTubeClient.Companion.IOS
import com.music.innertube.models.YouTubeClient.Companion.IPADOS
import com.music.innertube.models.YouTubeClient.Companion.TVHTML5
import com.music.innertube.models.YouTubeClient.Companion.VISIONOS
import com.music.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.music.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.music.innertube.models.response.PlayerResponse
import com.niksoriginals.melo.constants.AudioQuality
import com.niksoriginals.melo.utils.cipher.CipherDeobfuscator
import com.niksoriginals.melo.utils.YTPlayerUtils.MAIN_CLIENT
import com.niksoriginals.melo.utils.YTPlayerUtils.STREAM_FALLBACK_CLIENTS
import com.niksoriginals.melo.utils.YTPlayerUtils.validateStatus
import com.niksoriginals.melo.utils.potoken.PoTokenGenerator
import com.niksoriginals.melo.utils.potoken.PoTokenResult
import com.niksoriginals.melo.utils.PlaybackLogLevel
import com.niksoriginals.melo.utils.PlaybackLogManager
import com.music.innertube.models.IpVersion
import okhttp3.Dns
import okhttp3.OkHttpClient
import timber.log.Timber
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.datastore.preferences.core.edit
import com.niksoriginals.melo.constants.VisitorDataKey
import com.niksoriginals.melo.utils.dataStore

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    private const val TAG = "YTPlayerUtils"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                return when (YouTube.ipVersion) {
                    IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                    IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                    IpVersion.AUTO -> addresses
                }
            }
        })
        .proxySelector(object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> = listOfNotNull(YouTube.proxy ?: Proxy.NO_PROXY)
            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                Timber.tag(TAG).e(ioe, "Proxy connection failed for URI: $uri")
            }
        })
        .proxyAuthenticator { _, response ->
            YouTube.proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    /**
     * Startup warm-up for the BotGuard generator. The cipher WebView is already prewarmed in
     * [com.niksoriginals.melo.App], but the poToken generator used to be created lazily on the
     * first playback resolution — meaning the first play after launch paid a fresh WebView +
     * HTML load + two BotGuard challenge round-trips inline (seconds) on the critical path, and
     * any hiccup there surfaced as a cold-start "playback error". Minting the session-bound pot
     * here (off the critical path) makes the first real play only mint the cheap per-video token.
     */
    suspend fun prewarmPoTokenForStartup() {
        if (!MAIN_CLIENT.useWebPoTokens) return
        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) {
            YouTube.dataSyncId
        } else {
            YouTube.visitorData ?: YouTube.refreshVisitorData().getOrNull()?.also { newVisitorData ->
                runCatching {
                    CipherDeobfuscator.appContext.dataStore.edit { settings ->
                        settings[VisitorDataKey] = newVisitorData
                    }
                }.onFailure { e ->
                    Timber.tag(logTag).w(e, "Startup prewarm: failed to persist visitorData: ${e.message}")
                }
            }
        } ?: run {
            Timber.tag(logTag).d("Startup poToken prewarm skipped: no session id available yet")
            return
        }
        val ok = runCatching { poTokenGenerator.prewarm(sessionId) }.getOrDefault(false)
        Timber.tag(logTag).d("Startup poToken prewarm ${if (ok) "complete" else "not ready"} (session=${sessionId.take(20)}...)")
    }

    /**
     * Waits until the two runtime warm-ups the first play needs (cipher WebView and BotGuard
     * generator) are ready, so an instant tap right after a fresh install blocks for a few seconds
     * and then resolves on the reliable warm path instead of racing cold WebView creation and
     * surfacing a "playback error". Idempotent — returns immediately once everything is warm.
     * Best-effort: logs but never throws into playback; if warm-up itself is impossible the normal
     * resolution attempt still runs (and may fall back to a pot-less client).
     */
    private suspend fun ensureWarmForPlayback() {
        withContext(Dispatchers.IO) {
            runCatching { CipherDeobfuscator.prewarm() }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    Timber.tag(logTag).w(e, "ensureWarm: cipher prewarm failed — attempting playback anyway")
                }
            runCatching { prewarmPoTokenForStartup() }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    Timber.tag(logTag).w(e, "ensureWarm: poToken prewarm failed — attempting playback anyway")
                }
        }
    }

    
    // ANDROID_VR needs no JS player (no signatureCipher) and, until recently, no poToken —
    // which is why playback worked here long before either was wired up. It does, however,
    // get rejected outright once its clientVersion goes stale, so this tracks yt-dlp's
    // current android_vr build. The older builds stay below as fallbacks.
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX


    private val METADATA_CLIENT: YouTubeClient = WEB_REMIX

    // Order is load-bearing. Melo's on-device measurements (see upstream docs):
    //   - VISIONOS is the only client measured to serve a COMPLETE file.
    //   - ANDROID_VR_1_65_10 is the current yt-dlp pin, also whole-file capable. The older VR
    //     builds (1_61_48 / 1_43_32) are bot-gated BY VERSION: LOGIN_REQUIRED "Sign in to
    //     confirm you're not a bot" even signed in, so they must NOT lead the chain.
    //   - IPADOS / IOS serve only a ~1 MiB preview and 403 beyond it — last resort.
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        VISIONOS,                        // only client measured to serve a complete file
        ANDROID_VR_1_65_10,              // current yt-dlp/YouTube.js pin; whole-file capable
        TVHTML5,
        ANDROID_VR_1_43_32,              // version-gated; kept as a control after 1.65.10
        IPADOS,                          // ~1 MiB preview only — last resort
        IOS,                             // ~1 MiB preview only — last resort
        // The only client that answers OK for age-restricted / explicit tracks (authenticated).
        WEB_CREATOR
    )

    // For normal content the MAIN_CLIENT (WEB_REMIX) stream attempt is skipped entirely: its
    // formats are behind the signature cipher / n-challenge that can no longer be solved
    // client-side, they 403 on validation, and reaching them costs a doomed round-trip before
    // the array above. Start at the top (VISIONOS) instead. (Mirrors upstream Melo.)
    private const val NORMAL_CONTENT_STREAM_START_INDEX = 0

    // Privately-owned (uploaded) tracks only answer through TVHTML5 + PoToken, so begin the
    // fallback walk there rather than wasting requests on whole-file-capable clients that will
    // return LOGIN_REQUIRED for the uploader's own content.
    private val PRIVATE_TRACK_STREAM_START_INDEX = STREAM_FALLBACK_CLIENTS.indexOf(TVHTML5).takeIf { it >= 0 } ?: 0

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )
    
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        context: android.content.Context? = null,
        knownArtist: String? = null,
        knownTitle: String? = null,
        knownDurationMs: Long? = null,
        isDownload: Boolean = false
    ): Result<PlaybackData> {
        val showFallbackToast = false

        var hasShownLosslessToast = false
        var hasShownOpusToast = false

        suspend fun tryOpus(): Result<PlaybackData> {
            // Guarantee the cipher WebView and BotGuard generator are warm before the first
            // resolution. On a fresh install / instant first tap this intentionally takes a few
            // seconds (base.js download + BotGuard challenge) — but then the warm path runs, so
            // the first play succeeds instead of racing a cold WebView and erroring out.
            ensureWarmForPlayback()
            val firstAttempt = resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager, context, knownArtist, knownTitle)
            if (firstAttempt.isFailure && YouTube.cookie == null) {
                Timber.tag(TAG).w("Playback failed for guest. Rotating session and retrying...")
                PlaybackLogManager.log(PlaybackLogLevel.BOT, "Playback failed for guest", "Triggering bot detection mitigation (rotating guest session)")
                BotDetectionMitigator.rotateGuestSession()
                val retryResult = resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager, context, knownArtist, knownTitle)
                retryResult.onSuccess { BotDetectionMitigator.notifyPlaybackSuccess() }
                return retryResult
            }
            firstAttempt.onSuccess { BotDetectionMitigator.notifyPlaybackSuccess() }
            return firstAttempt
        }

        suspend fun tryLossless(): Result<PlaybackData> {
            var attemptResult: Result<PlaybackData>? = null
            var lastException: Exception? = null
            try {
                attemptResult = kotlinx.coroutines.withTimeoutOrNull(15000L) {
                    val metadata = if (knownTitle == null || knownArtist == null) playerResponseForMetadata(videoId).getOrNull() else null
                    val title = knownTitle ?: metadata?.videoDetails?.title
                    val author = knownArtist ?: metadata?.videoDetails?.author?.replace(" - Topic", "")
                    if (title != null && author != null) {
                        val track = com.niksoriginals.melo.utils.LosslessAPI.search(title, author)
                        if (track != null) {
                            val format = com.music.innertube.models.response.PlayerResponse.StreamingData.Format(
                                itag = 0,
                                mimeType = "audio/flac; codecs=\"flac\"",
                                bitrate = 1411000,
                                audioSampleRate = 44100,
                                contentLength = 0L,
                                url = track.url,
                                cipher = null,
                                signatureCipher = null,
                                audioQuality = "LOSSLESS",
                                fps = null,
                                width = null,
                                height = null,
                                quality = "lossless",
                                qualityLabel = null,
                                averageBitrate = null,
                                approxDurationMs = null,
                                audioChannels = null,
                                loudnessDb = null,
                                lastModified = null,
                                audioTrack = null
                            )
                            val resolvedPlaybackData = PlaybackData(
                                audioConfig = null,
                                videoDetails = metadata?.videoDetails,
                                playbackTracking = null,
                                format = format,
                                streamUrl = track.url,
                                streamExpiresInSeconds = 3600
                            )
                            return@withTimeoutOrNull Result.success(resolvedPlaybackData)
                        } else {
                            throw Exception("No streamable match resolved on Lossless index")
                        }
                    } else {
                        throw Exception("Missing title or artist for lookup")
                    }
                }
                if (attemptResult == null) {
                    lastException = Exception("Timeout fetching Lossless stream")
                }
            } catch (e: Exception) {
                lastException = e
            }
            
            return attemptResult ?: Result.failure(lastException ?: Exception("Lossless resolution failed"))
        }

        fun showToastMsg(msg: String) {
            context?.let {
                if (showFallbackToast) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(it, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return when (audioQuality) {
            AudioQuality.LOSSLESS -> {
                val losslessRes = tryLossless()
                if (losslessRes.isSuccess) return losslessRes

                Timber.tag(TAG).e("Qobuz resolution failed, falling back to YouTube Opus")
                if (!hasShownLosslessToast) {
                    hasShownLosslessToast = true
                    showToastMsg(if (isDownload) "Lossless download unavailable, falling back to Opus" else "Lossless stream unavailable, falling back to Opus")
                }

                tryOpus()
            }
            else -> {
                tryOpus()
            }
        }
    }

    private suspend fun resolvePlaybackData(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        context: android.content.Context? = null,
        knownArtist: String? = null,
        knownTitle: String? = null
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        PlaybackLogManager.log(PlaybackLogLevel.INFO, "Resolving playback data", "Video: $videoId")
        
        
        val isUploadedTrack = playlistId == "MLPT" || playlistId?.contains("MLPT") == true

        val isLoggedIn = YouTube.cookie != null
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: ${signatureTimestamp.timestamp}")

        
        var poToken: PoTokenResult? = null
        // Ensure we have a valid visitorData/sessionId for guest users before requesting playback or PoToken
        val sessionId = if (isLoggedIn) {
            YouTube.dataSyncId
        } else {
            YouTube.visitorData ?: run {
                Timber.tag(logTag).d("visitorData is null, attempting on-demand fetch before generating PoToken...")
                YouTube.refreshVisitorData().getOrNull()?.also { newVisitorData ->
                    try {
                        CipherDeobfuscator.appContext.dataStore.edit { settings ->
                            settings[VisitorDataKey] = newVisitorData
                        }
                        Timber.tag(logTag).d("On-demand visitorData obtained and persisted: $newVisitorData")
                    } catch (e: Exception) {
                        Timber.tag(logTag).w(e, "Failed to persist on-demand visitorData: ${e.message}")
                    }
                }
            }
        }
        if (MAIN_CLIENT.useWebPoTokens && sessionId != null) {
            Timber.tag(logTag).d("Generating PoToken for MAIN_CLIENT with sessionId")
            try {
                poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                if (poToken != null) {
                    Timber.tag(logTag).d("PoToken generated successfully")
                }
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "PoToken generation failed: ${e.message}")
            }
        }

        
        Timber.tag(logTag).d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        PlaybackLogManager.log(PlaybackLogLevel.DEBUG, "Trying ${MAIN_CLIENT.clientName} (Main)")
        var mainPlayerResponse = YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp.timestamp, poToken?.playerRequestPoToken).getOrThrow()

        
        var metadataResponse: PlayerResponse? = null
        if (isLoggedIn) {
            Timber.tag(logTag).d("Fetching metadata from METADATA_CLIENT (WEB_REMIX) for authenticated tracking")
            try {
                
                var metaPoToken: PoTokenResult? = null
                val metaSessionId = YouTube.dataSyncId
                if (METADATA_CLIENT.useWebPoTokens && metaSessionId != null) {
                    try {
                        metaPoToken = poTokenGenerator.getWebClientPoToken(videoId, metaSessionId)
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "Metadata PoToken generation failed")
                    }
                }
                metadataResponse = YouTube.player(
                    videoId, playlistId, METADATA_CLIENT,
                    signatureTimestamp.timestamp, metaPoToken?.playerRequestPoToken
                ).getOrNull()
                Timber.tag(logTag).d("Metadata response obtained: ${metadataResponse?.playabilityStatus?.status}")
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "Failed to fetch metadata from METADATA_CLIENT")
            }
        }

        
        var usedAgeRestrictedClient: YouTubeClient? = null
        val wasOriginallyAgeRestricted: Boolean

        
        val mainStatus = mainPlayerResponse.playabilityStatus.status
        val isAgeRestrictedFromResponse = mainStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED"
        ) || (mainStatus == "LOGIN_REQUIRED" && mainPlayerResponse.playabilityStatus.reason?.contains("age", ignoreCase = true) == true)
        wasOriginallyAgeRestricted = isAgeRestrictedFromResponse

        if (isAgeRestrictedFromResponse && isLoggedIn) {
            
            Timber.tag(logTag).d("Age-restricted detected, using WEB_CREATOR")
            Log.i(TAG, "Age-restricted: using WEB_CREATOR for videoId=$videoId")
            val creatorResponse = YouTube.player(videoId, playlistId, WEB_CREATOR, null, null)
                .onFailure {
                    // Distinguish thrown request/parse failures from genuine playability
                    // rejections (both otherwise surface as a null response downstream).
                    Timber.tag(logTag).e(it, "player() request FAILED for WEB_CREATOR")
                }.getOrNull()
            if (creatorResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("WEB_CREATOR works for age-restricted content")
                mainPlayerResponse = creatorResponse
                usedAgeRestrictedClient = WEB_CREATOR
            }
        }

        
        val audioConfig = metadataResponse?.playerConfig?.audioConfig ?: mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = metadataResponse?.videoDetails ?: mainPlayerResponse.videoDetails
        val playbackTracking = metadataResponse?.playbackTracking ?: mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null
        var retryMainPlayerResponse: PlayerResponse? = if (usedAgeRestrictedClient != null) mainPlayerResponse else null

        
        val currentStatus = mainPlayerResponse.playabilityStatus.status
        var isAgeRestricted = currentStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED",
            "UNPLAYABLE",
            "LOGIN_REQUIRED"
        )

        if (isAgeRestricted) {
            Timber.tag(logTag).d("Content needs fallback (status: $currentStatus)")
            android.util.Log.i("YTPlayerUtils", "Unplayable content detected: videoId=$videoId, status=$currentStatus")
        }
        
        val isPrivateTrack = mainPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

        
        val startIndex = when {
            isPrivateTrack -> PRIVATE_TRACK_STREAM_START_INDEX
            isAgeRestricted -> 0
            // Already holding an OK response from WEB_CREATOR (age-restricted resolved while
            // logged in): reuse its streams via the MAIN path instead of discarding them.
            retryMainPlayerResponse != null -> -1
            // Normal content: skip the doomed MAIN_CLIENT (WEB_REMIX) stream attempt.
            else -> NORMAL_CONTENT_STREAM_START_INDEX
        }

        for (clientIndex in (startIndex until STREAM_FALLBACK_CLIENTS.size)) {
            
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            
            val client: YouTubeClient
            if (clientIndex == -1) {
                
                client = MAIN_CLIENT
                streamPlayerResponse = retryMainPlayerResponse ?: mainPlayerResponse
                Timber.tag(logTag).d("Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Timber.tag(logTag).d("Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")
                PlaybackLogManager.log(PlaybackLogLevel.DEBUG, "Trying fallback [${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}]", client.clientName)

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    
                    Timber.tag(logTag).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                
                if (client.useWebPoTokens && poToken == null && sessionId != null) {
                    Timber.tag(logTag).d("Lazily generating PoToken for fallback web client: ${client.clientName}")
                    try {
                        poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "Lazy PoToken generation failed")
                    }
                }

                Timber.tag(logTag).d("Fetching player response for fallback client: ${client.clientName}")
                
                val clientPoToken = if (client.useWebPoTokens) poToken?.playerRequestPoToken else null
                
                val clientSigTimestamp = if (wasOriginallyAgeRestricted) null else signatureTimestamp.timestamp
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, clientSigTimestamp, clientPoToken)
                        .onFailure {
                            Timber.tag(logTag).e(it, "player() request FAILED for %s", client.clientName)
                        }.getOrNull()
            }

            
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("Player response status OK for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                PlaybackLogManager.log(PlaybackLogLevel.INFO, "Player response OK", if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName)

                
                val hasDirectUrls = streamPlayerResponse.streamingData?.adaptiveFormats
                    ?.any { !it.url.isNullOrEmpty() } == true
                val hasSignatureCipher = streamPlayerResponse.streamingData?.adaptiveFormats
                    ?.any { !it.signatureCipher.isNullOrEmpty() || !it.cipher.isNullOrEmpty() } == true

                Timber.tag(logTag).d("URL check: hasDirectUrls=$hasDirectUrls, hasSignatureCipher=$hasSignatureCipher")

                
                val responseToUse = streamPlayerResponse

                format =
                    findFormat(
                        responseToUse,
                        audioQuality,
                        connectivityManager,
                    )

                if (format == null) {
                    Timber.tag(logTag).d("No suitable format found for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    continue
                }

                Timber.tag(logTag).d("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")

                streamUrl = findUrlOrNull(format, videoId, responseToUse, skipNewPipe = wasOriginallyAgeRestricted)
                if (streamUrl == null) {
                    Timber.tag(logTag).d("Stream URL not found for format")
                    continue
                }

                
                val currentClient = if (clientIndex == -1) {
                    usedAgeRestrictedClient ?: MAIN_CLIENT
                } else {
                    STREAM_FALLBACK_CLIENTS[clientIndex]
                }

                
                val isPrivatelyOwnedTrack = streamPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"


                // The n-transform is not idempotent: applying it to an already-transformed `n`
                // yields a value the server rejects. Track whether it ran so the
                // validation-failure path below retries only when this attempt did not.
                var nTransformApplied = false
                if (currentClient.useWebPoTokens) {
                    try {
                        Timber.tag(logTag).d("Applying n-transform to stream URL for ${currentClient.clientName}")
                        val transformed = CipherDeobfuscator.transformNParamInUrl(streamUrl)
                        if (transformed != streamUrl) {
                            streamUrl = transformed
                            nTransformApplied = true
                            Timber.tag(logTag).d("N-transform applied successfully")
                        }
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "N-transform failed: ${e.message}")
                    }
                }

                
                if (currentClient.useWebPoTokens && poToken?.streamingDataPoToken != null) {
                    Timber.tag(logTag).d("Appending pot= parameter to stream URL")
                    val separator = if ("?" in streamUrl) "&" else "?"
                    streamUrl = "${streamUrl}${separator}pot=${poToken.streamingDataPoToken}"
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Timber.tag(logTag).d("Stream expiration time not found")
                    continue
                }

                Timber.tag(logTag).d("Stream expires in: $streamExpiresInSeconds seconds")

                
                val urlHost = try { java.net.URL(streamUrl).host } catch (e: Exception) { "unknown" }
                Timber.tag(logTag).d("Stream URL host: $urlHost, pot length: ${poToken?.streamingDataPoToken?.length ?: 0}")

                
                val isPrivatelyOwned = streamPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1 || isPrivatelyOwned) {
                    
                    if (isPrivatelyOwned) {
                        Timber.tag(logTag).d("Skipping validation for privately owned track: ${currentClient.clientName}")
                    } else {
                        Timber.tag(logTag).d("Using last fallback client without validation: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    }
                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId, private=$isPrivatelyOwned")
                    break
                }

                if (validateStatus(streamUrl, format.contentLength, currentClient.clientName)) {
                    
                    Timber.tag(logTag).d("Stream validated successfully with client: ${currentClient.clientName}")
                    PlaybackLogManager.log(PlaybackLogLevel.INFO, "Stream validated", currentClient.clientName)
                    
                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId")
                    break
                } else {
                    Timber.tag(logTag).d("Stream validation failed for client: ${currentClient.clientName}")

                    
                    // Only worth retrying if the transform above did not already run: re-running it
                    // on an already-transformed URL produces a wrong `n` and burns another request.
                    if (currentClient.useWebPoTokens && !nTransformApplied) {
                        var nTransformWorked = false


                        try {
                            val nTransformed = CipherDeobfuscator.transformNParamInUrl(streamUrl)
                            if (nTransformed != streamUrl) {
                                Timber.tag(logTag).d("CipherDeobfuscator n-transform applied, re-validating...")
                                if (validateStatus(nTransformed, format.contentLength, currentClient.clientName)) {
                                    Timber.tag(logTag).d("N-transformed URL VALIDATED OK!")
                                    streamUrl = nTransformed
                                    nTransformWorked = true
                                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId (cipher n-transform)")
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag(logTag).e(e, "CipherDeobfuscator n-transform error")
                        }

                        if (nTransformWorked) break
                    }
                }
            } else {
                val status = streamPlayerResponse?.playabilityStatus?.status ?: "Unknown"
                val reason = streamPlayerResponse?.playabilityStatus?.reason ?: "No reason"
                Timber.tag(logTag).d("Player response status not OK: $status, reason: $reason")
                PlaybackLogManager.log(PlaybackLogLevel.WARNING, "Client failed: ${client.clientName}", "$status: $reason")
                
                
                Timber.tag(logTag).d("Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        if (streamPlayerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find format")
            throw Exception("Could not find format")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).d("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }.onFailure { e ->
        Timber.tag(logTag).e(e, "Playback resolution failed")
        PlaybackLogManager.log(PlaybackLogLevel.ERROR, "Playback failed", "${e::class.simpleName}: ${e.message}")
    }
    
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) 
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag).d("Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.OPUS, AudioQuality.LOSSLESS -> 1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) 
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return format
    }
    
    private fun validateStatus(url: String, contentLength: Long? = null, label: String = ""): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            // googlevideo serves IOS/IPADOS stream URLs only for a ~1 MiB fixed prefix and 403s
            // beyond it. Probing the FIRST byte (bytes=0-0, the old behaviour) is inside that
            // preview window, so a preview-capped URL validates OK and ExoPlayer's real full-file
            // request then 403s. Probe the LAST byte instead: it only succeeds for a URL that can
            // actually serve the whole file, so capped preview URLs get rejected and the fallback
            // walk moves to the next client. (Same range trick upstream Melo uses.)
            val range = if (contentLength != null && contentLength > 0) {
                "bytes=${contentLength - 1}-${contentLength - 1}"
            } else {
                "bytes=0-524287"
            }
            val requestBuilder = okhttp3.Request.Builder()
                .get()
                .url(url)
                .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                .header("Range", range)

            YouTube.cookie?.let { cookie ->
                requestBuilder.addHeader("Cookie", cookie)
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            response.close()
            val code = response.code
            val accepted = code in 200..299 || code == 405
            Timber.tag(logTag).d("Stream URL validation: code=$code range=$range ${if (accepted) "accepted" else "REJECTED"} client=$label")
            return accepted
        } catch (e: java.io.IOException) {
            // Network timeout / reset while probing. The stream URL itself may still be fine —
            // let ExoPlayer attempt the real GET rather than burning the fallback client.
            Timber.tag(logTag).w(e, "Stream URL probe failed (IO); accepting optimistically")
            return true
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }
    data class SignatureTimestampResult(
        val timestamp: Int?,
        val isAgeRestricted: Boolean
    )

    private fun getSignatureTimestampOrNull(videoId: String): SignatureTimestampResult {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        val result = NewPipeExtractor.getSignatureTimestamp(videoId)
        return result.fold(
            onSuccess = { timestamp ->
                Timber.tag(logTag).d("Signature timestamp obtained: $timestamp")
                SignatureTimestampResult(timestamp, isAgeRestricted = false)
            },
            onFailure = { error ->
                val isAgeRestricted = error.message?.contains("age-restricted", ignoreCase = true) == true ||
                    error.cause?.message?.contains("age-restricted", ignoreCase = true) == true
                if (isAgeRestricted) {
                    Timber.tag(logTag).d("Age-restricted content detected from NewPipe")
                    Log.i(TAG, "Age-restricted detected early via NewPipe: videoId=$videoId")
                } else {
                    Timber.tag(logTag).e(error, "Failed to get signature timestamp")
                    reportException(error)
                }
                SignatureTimestampResult(null, isAgeRestricted)
            }
        )
    }

    suspend fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        playerResponse: PlayerResponse,
        skipNewPipe: Boolean = false
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId, skipNewPipe: $skipNewPipe")

        
        if (!format.url.isNullOrEmpty()) {
            Timber.tag(logTag).d("Using URL from format directly")
            return format.url
        }

        
        val signatureCipher = format.signatureCipher ?: format.cipher
        if (!signatureCipher.isNullOrEmpty()) {
            Timber.tag(logTag).d("Format has signatureCipher, using custom deobfuscation")
            val customDeobfuscatedUrl = CipherDeobfuscator.deobfuscateStreamUrl(signatureCipher, videoId)
            if (customDeobfuscatedUrl != null) {
                Timber.tag(logTag).d("Stream URL obtained via custom cipher deobfuscation")
                return customDeobfuscatedUrl
            }
            Timber.tag(logTag).d("Custom cipher deobfuscation failed")
        }

        
        if (skipNewPipe) {
            Timber.tag(logTag).d("Skipping NewPipe methods for age-restricted content")
            return null
        }

        
        val deobfuscatedUrl = NewPipeExtractor.getStreamUrl(format, videoId)
        if (deobfuscatedUrl != null) {
            Timber.tag(logTag).d("Stream URL obtained via NewPipe deobfuscation")
            return deobfuscatedUrl
        }

        
        Timber.tag(logTag).d("Trying StreamInfo fallback for URL")
        val streamUrls = YouTube.getNewPipeStreamUrls(videoId)
        if (streamUrls.isNotEmpty()) {
            val streamUrl = streamUrls.find { it.first == format.itag }?.second
            if (streamUrl != null) {
                Timber.tag(logTag).d("Stream URL obtained from StreamInfo")
                return streamUrl
            }

            
            val audioStream = streamUrls.find { urlPair ->
                playerResponse.streamingData?.adaptiveFormats?.any {
                    it.itag == urlPair.first && it.isAudio
                } == true
            }?.second

            if (audioStream != null) {
                Timber.tag(logTag).d("Audio stream URL obtained from StreamInfo (different itag)")
                return audioStream
            }
        }

        Timber.tag(logTag).e("Failed to get stream URL")
        return null
    }

    fun forceRefreshForVideo(videoId: String) {
        Timber.tag(logTag).d("Force refreshing for videoId: $videoId")
    }
}


