package com.niksoriginals.melo.utils.potoken

import android.webkit.CookieManager
import com.niksoriginals.melo.utils.cipher.CipherDeobfuscator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class PoTokenGenerator {
    private val TAG = "PoTokenGenerator"

    private val webViewSupported by lazy { runCatching { CookieManager.getInstance() }.isSuccess }
    private var webViewBadImpl = false

    private val webPoTokenGenLock = Mutex()
    private var webPoTokenSessionId: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenWebView? = null

    fun getWebClientPoToken(videoId: String, sessionId: String): PoTokenResult? {
        Timber.tag(TAG).d("getWebClientPoToken called: videoId=$videoId, sessionId=$sessionId")
        Timber.tag(TAG).d("WebView state: supported=$webViewSupported, badImpl=$webViewBadImpl")
        if (!webViewSupported || webViewBadImpl) {
            Timber.tag(TAG).d("WebView not available: supported=$webViewSupported, badImpl=$webViewBadImpl")
            return null
        }

        return try {
            Timber.tag(TAG).d("Calling runBlocking to generate poToken...")
            runBlocking { getWebClientPoToken(videoId, sessionId, forceRecreate = false) }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "poToken generation exception: ${e.javaClass.simpleName}: ${e.message}")
            when (e) {
                is BadWebViewException -> {
                    Timber.tag(TAG).e(e, "Could not obtain poToken because WebView is broken")
                    webViewBadImpl = true
                    null
                }
                else -> throw e
            }
        }
    }

    /**
     * Eager startup warm-up: builds the BotGuard [PoTokenWebView] and mints the session-bound
     * (streaming) pot off the first-play critical path. A later [getWebClientPoToken] then only
     * pays the cheaper per-video mint on an already-loaded WebView, instead of creating the whole
     * generator inline during the first playback resolution. Idempotent: no-ops when a valid
     * generator for this [sessionId] already exists.
     */
    suspend fun prewarm(sessionId: String): Boolean {
        if (!webViewSupported || webViewBadImpl) {
            Timber.tag(TAG).d("prewarm skipped: supported=$webViewSupported, badImpl=$webViewBadImpl")
            return false
        }
        return try {
            webPoTokenGenLock.withLock {
                val needsBuild = webPoTokenGenerator == null || webPoTokenGenerator!!.isExpired ||
                    webPoTokenGenerator!!.isDead || webPoTokenSessionId != sessionId
                if (needsBuild) {
                    recreateGenerator(sessionId, forceRecreate = true)
                } else {
                    Timber.tag(TAG).d("prewarm: generator already warm for sessionId=${sessionId.take(20)}")
                }
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: BadWebViewException) {
            Timber.tag(TAG).e(e, "prewarm failed because WebView is broken")
            webViewBadImpl = true
            false
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "prewarm failed: ${e.message}")
            false
        }
    }


    private suspend fun getWebClientPoToken(videoId: String, sessionId: String, forceRecreate: Boolean): PoTokenResult {
        Timber.tag(TAG).d("Web poToken requested: videoId=$videoId, sessionId=$sessionId")

        val (generator, streamingPot, recreated) =
            webPoTokenGenLock.withLock {
                val shouldRecreate =
                    forceRecreate || webPoTokenGenerator == null || webPoTokenGenerator!!.isExpired ||
                        webPoTokenGenerator!!.isDead ||
                        webPoTokenSessionId != sessionId

                if (shouldRecreate) {
                    recreateGenerator(sessionId, forceRecreate)
                }

                Triple(webPoTokenGenerator!!, webPoTokenStreamingPot!!, shouldRecreate)
            }

        val playerPot = try {
            generator.generatePoToken(videoId)
        } catch (throwable: Throwable) {
            if (recreated) {


                throw throwable
            } else {



                Timber.tag(TAG).e(throwable, "Failed to obtain poToken, retrying")
                return getWebClientPoToken(videoId = videoId, sessionId = sessionId, forceRecreate = true)
            }
        }


        // The /player request expects the session-bound token (visitor session), while
        // the stream URL `pot=` must be the videoId-bound token (GVS tokens are
        // content-bound). Swapping these yields HTTP 403 on every stream URL even
        // though the /player response is valid.
        val playerRequestPoToken = streamingPot
        val streamingDataPoToken = playerPot
        Timber.tag(TAG).d("poToken generated successfully: player=${playerRequestPoToken.take(20)}..., streaming=${streamingDataPoToken.take(20)}...")

        return PoTokenResult(playerRequestPoToken, streamingDataPoToken)
    }

    /** Builds a fresh [PoTokenWebView] and mints the session-bound (streaming) pot for [sessionId]. */
    private suspend fun recreateGenerator(sessionId: String, forceRecreate: Boolean) {
        Timber.tag(TAG).d("Creating new PoTokenWebView (forceRecreate=$forceRecreate)")

        withContext(Dispatchers.Main) {
            webPoTokenGenerator?.close()
        }


        val newGenerator = PoTokenWebView.getNewPoTokenGenerator(CipherDeobfuscator.appContext)


        val newStreamingPot = try {
            newGenerator.generatePoToken(sessionId)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { newGenerator.close() }
            throw e
        }

        webPoTokenSessionId = sessionId
        webPoTokenGenerator = newGenerator
        webPoTokenStreamingPot = newStreamingPot
        Timber.tag(TAG).d("Streaming poToken generated for sessionId=${webPoTokenSessionId?.take(20)}...")
    }
}
