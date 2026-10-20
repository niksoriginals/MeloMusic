package com.niksoriginals.melo.utils.potoken

import android.content.Context
import android.util.Base64
import java.io.File
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Supplies the BotGuard HTML payload for [PoTokenWebView]: the bundled asset is the offline
 * default, overlaid by the same HTML fetched from the public GitHub repo so a rotated BotGuard
 * payload can be fixed without an APK rebuild. The remote copy is cached to disk and refreshed
 * periodically; any failure silently keeps the bundled asset.
 */
object PoTokenAssetStore {
    private const val TAG = "PoTokenAssetStore"
    private const val ASSET_NAME = "po_token.html"
    private const val CACHE_FILE = "po_token_remote.html"
    private const val REFRESH_TTL_MS = 24 * 60 * 60 * 1000L

    private val REMOTE_URL by lazy {
        val encoded = "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL25pa3NvcmlnaW5hbHMvTWVsb1l0Y2lwaGVyL21haW4vcG9fdG9rZW4uaHRtbA=="
        String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8)
    }

    private val httpClient = OkHttpClient.Builder().build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fetchLock = Any()
    private var lastFetchMs = 0L
    private var fetchInFlight = false

    private fun cacheFile(context: Context): File = File(context.filesDir, CACHE_FILE)

    fun getHtml(context: Context): String {
        val cached = cacheFile(context).takeIf { it.exists() }?.readText()
        requestRefresh(context)
        return cached ?: loadAsset(context)
    }

    private fun loadAsset(context: Context): String {
        return runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }.getOrElse { e ->
            Timber.tag(TAG).e(e, "Failed to read bundled $ASSET_NAME")
            ""
        }
    }

    private fun requestRefresh(context: Context) {
        val now = System.currentTimeMillis()
        synchronized(fetchLock) {
            if (fetchInFlight || now - lastFetchMs < REFRESH_TTL_MS) return
            fetchInFlight = true
            lastFetchMs = now
        }
        scope.launch {
            try {
                val request = Request.Builder().url(REMOTE_URL).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        if (body.isNotBlank()) {
                            cacheFile(context).writeText(body)
                            Timber.tag(TAG).d("Remote $ASSET_NAME refreshed (${body.length} bytes)")
                        }
                    } else {
                        Timber.tag(TAG).w("Remote $ASSET_NAME fetch HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Remote $ASSET_NAME fetch failed — using bundled asset")
            } finally {
                synchronized(fetchLock) { fetchInFlight = false }
            }
        }
    }
}