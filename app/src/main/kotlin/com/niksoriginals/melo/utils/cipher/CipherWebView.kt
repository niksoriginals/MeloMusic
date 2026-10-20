package com.niksoriginals.melo.utils.cipher

import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Headless WebView that runs YouTube's player JS so the `s` (signature) and `n` transforms can be
 * applied to stream URLs.
 *
 * Two properties of a *headless* WebView shape this class:
 *
 *  - It is never attached to a window, so [android.view.View.post] must never be used to reach it.
 *    `View.post()` parks the Runnable in a `HandlerActionQueue` until `dispatchAttachedToWindow()`
 *    and returns `true` regardless, so on a detached WebView the runnable never runs and the eval
 *    silently never happens. All renderer interaction goes through `withContext(Dispatchers.Main)`.
 *  - Its renderer would otherwise be deprioritised (and freezer-eligible) once it goes idle after
 *    load, because `rendererPriorityWaivedWhenNotVisible` defaults to true. See
 *    `setRendererPriorityPolicy` in [init].
 *
 * Results come back over two independent paths: the [WebView.evaluateJavascript] `ValueCallback`
 * (the JS returns a JSON envelope, so no request id has to survive a round trip) and the
 * `@JavascriptInterface` bridge. Whichever answers first wins via [RequestSlot]; the loser is a
 * no-op. Values crossing the bridge must match the Kotlin parameter types exactly: Chromium's Java
 * Bridge coerces a JS string reaching an `int` parameter to 0 rather than parsing it.
 */
class CipherWebView private constructor(
    context: Context,
    private val playerJs: String,
    private val sigInfo: FunctionNameExtractor.SigFunctionInfo?,
    private val nFuncInfo: FunctionNameExtractor.NFunctionInfo?,
    initContinuation: Continuation<CipherWebView>,
) {
    private val webView = WebView(context)

    private var initContinuation: Continuation<CipherWebView>? = initContinuation
    private val sigSlot = RequestSlot<String>()
    private val nSlot = RequestSlot<String>()

    private class RequestSlot<T> {
        private val pending = mutableMapOf<Int, Continuation<T>>()
        private var requestId = 0

        @Synchronized
        fun arm(cont: Continuation<T>): Int {
            val id = ++requestId
            pending[id] = cont
            return id
        }

        @Synchronized
        fun takeIfCurrent(id: Int): Continuation<T>? = pending.remove(id)

        @Synchronized
        fun takeAny(): List<Continuation<T>> {
            val all = pending.values.toList()
            pending.clear()
            return all
        }
    }

    @Volatile
    var isDead: Boolean = false
        private set

    @Volatile
    private var destroyed = false

    @Volatile
    var nFunctionAvailable: Boolean = false
        private set

    @Volatile
    var discoveredNFuncName: String? = null
        private set

    /**
     * Result of the init-time sig probe. Diagnostic only: it never gates [deobfuscateSignature], so
     * an unlucky probe cannot block a path that would otherwise work.
     */
    @Volatile
    var sigSelfTest: String = "not_run"
        private set

    init {
        val settings = webView.settings
        @Suppress("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        settings.blockNetworkLoads = true

        // Never attached to a window, so without this the renderer loses its priority binding once
        // it goes idle after load and becomes freezer-eligible; later evals would then never run.
        webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
        webView.onResume()
        webView.resumeTimers()

        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webViewClient = object : WebViewClient() {
            @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.O)
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                Timber.tag(TAG).e("=== RENDER PROCESS GONE === didCrash=${runCatching { detail.didCrash() }.getOrNull()}")
                onRendererGone("WebView render process gone (didCrash=${runCatching { detail.didCrash() }.getOrNull()})")
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.message().contains("Uncaught") && !m.message().contains("is not defined")) {
                    Timber.tag(TAG).e("WebView JS error: ${m.message()} at ${m.sourceId()}:${m.lineNumber()}")
                } else if (m.message().startsWith("[cipher]")) {
                    Timber.tag(TAG).d("JS: ${m.message()}")
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    private fun onRendererGone(reason: String) {
        isDead = true
        val e = CipherRendererGoneException(reason)
        takeInitContinuation()?.resumeSafely { it.resumeWithException(e) }
        sigSlot.takeAny().forEach { it.resumeSafely { c -> c.resumeWithException(e) } }
        nSlot.takeAny().forEach { it.resumeSafely { c -> c.resumeWithException(e) } }
        destroyWebView()
    }

    @Synchronized
    private fun takeInitContinuation(): Continuation<CipherWebView>? =
        initContinuation.also { initContinuation = null }

    private inline fun <T> T.resumeSafely(block: (T) -> Unit) {
        runCatching { block(this) }
    }

    private fun loadPlayerJsFromFile() {
        val sigFuncName = sigInfo?.name
        val nFuncName = nFuncInfo?.name
        val nArrayIdx = nFuncInfo?.arrayIndex
        Timber.tag(TAG).d("Loading player JS from file (${playerJs.length} chars), exporting sig=$sigFuncName, nFunc=$nFuncName[$nArrayIdx]")

        val exports = buildList {
            val sigJsExpr = sigInfo?.jsExpression
            if (sigJsExpr != null) {
                val expr = sigJsExpr.replace("INPUT", "sig")
                add("window._cipherSigFunc = function(sig) { try { return $expr; } catch(e) { return null; } };")
            } else if (sigFuncName != null) {
                add("window._cipherSigFunc = typeof $sigFuncName !== 'undefined' ? $sigFuncName : null;")
            }

            val nJsExpr = nFuncInfo?.jsExpression
            if (nJsExpr != null) {
                val expr = nJsExpr.replace("INPUT", "n")
                add("window._nTransformFunc = function(n) { try { return $expr; } catch(e) { return n; } };")
            } else if (nFuncName != null) {
                val nExpr = if (nArrayIdx != null) {
                    "$nFuncName[$nArrayIdx]"
                } else {
                    nFuncName
                }
                add("window._nTransformFunc = typeof $nFuncName !== 'undefined' ? $nExpr : null;")
            }
        }

        // Inject at the LAST occurrence of the IIFE terminator: that is the end of the main player
        // IIFE, the only scope where the sig function and the `g` namespace are visible. Injecting
        // at every occurrence lands on the same winner (last assignment wins) but costs an extra
        // pass over ~2.9 MB per site and hides a wrong-scope injection.
        val modifiedJs = if (exports.isEmpty()) {
            Timber.tag(TAG).e("No sig/n exports to inject — cipher cannot work")
            playerJs
        } else {
            val exportCode = "; " + exports.joinToString(" ") + " "
            val at = playerJs.lastIndexOf(IIFE_MARKER)
            if (at < 0) {
                Timber.tag(TAG).e("Export marker '$IIFE_MARKER' not found in player JS — exports NOT injected")
                playerJs
            } else {
                Timber.tag(TAG).d("Injecting ${exports.size} export(s) at offset $at/${playerJs.length} (${countOccurrences(playerJs, IIFE_MARKER)} marker site(s))")
                StringBuilder(playerJs.length + exportCode.length).apply {
                    append(playerJs, 0, at)
                    append(exportCode)
                    append(playerJs, at, playerJs.length)
                }.toString()
            }
        }

        val cacheDir = File(webView.context.cacheDir, "cipher")
        cacheDir.mkdirs()
        File(cacheDir, "player.js").writeText(modifiedJs)

        val html = """<!DOCTYPE html>
<html><head><script>
// Each bridge call is guarded separately so a bridge problem can never stop the function from
// returning its JSON envelope to evaluateJavascript's ValueCallback.
function safeSigResult(reqId, v) { try { CipherBridge.onSigResult(reqId, v); } catch (e) { console.log("[cipher] bridge onSigResult threw: " + e); } }
function safeSigError(reqId, m) { try { CipherBridge.onSigError(reqId, m); } catch (e) { console.log("[cipher] bridge onSigError threw: " + e); } }
function safeNResult(reqId, v) { try { CipherBridge.onNResult(reqId, v); } catch (e) { console.log("[cipher] bridge onNResult threw: " + e); } }
function safeNError(reqId, m) { try { CipherBridge.onNError(reqId, m); } catch (e) { console.log("[cipher] bridge onNError threw: " + e); } }

function computeSig(reqId, constantArg, obfuscatedSig) {
    console.log("[cipher] computeSig reqId=" + reqId + " inLen=" + obfuscatedSig.length);
    try {
        var func = window._cipherSigFunc;
        if (typeof func !== 'function') {
            var missing = "sig func not on window (type: " + (typeof func) + ")";
            console.log("[cipher] " + missing);
            safeSigError(reqId, missing);
            return {ok: false, e: missing};
        }
        var result;
        if (func.length === 1) {
            result = func(obfuscatedSig);
        } else if (constantArg !== null && constantArg !== undefined) {
            result = func(constantArg, obfuscatedSig);
        } else {
            result = func(obfuscatedSig);
        }
        if (result === undefined || result === null) {
            var nullish = "sig func returned null/undefined";
            console.log("[cipher] " + nullish);
            safeSigError(reqId, nullish);
            return {ok: false, e: nullish};
        }
        result = String(result);
        console.log("[cipher] computeSig ok reqId=" + reqId + " outLen=" + result.length);
        safeSigResult(reqId, result);
        return {ok: true, v: result};
    } catch (error) {
        var threw = error + "\n" + (error.stack || "");
        console.log("[cipher] computeSig threw: " + error);
        safeSigError(reqId, threw);
        return {ok: false, e: threw};
    }
}

function computeN(reqId, nValue) {
    console.log("[cipher] computeN reqId=" + reqId + " n=" + nValue);
    try {
        var func = window._nTransformFunc;
        if (typeof func !== 'function') {
            var missing = "n-transform func not available (type: " + (typeof func) + ")";
            console.log("[cipher] " + missing);
            safeNError(reqId, missing);
            return {ok: false, e: missing};
        }
        var result = func(nValue);
        if (result === undefined || result === null) {
            var nullish = "n-transform returned null/undefined";
            console.log("[cipher] " + nullish);
            safeNError(reqId, nullish);
            return {ok: false, e: nullish};
        }
        result = String(result);
        console.log("[cipher] computeN ok reqId=" + reqId + " outLen=" + result.length);
        safeNResult(reqId, result);
        return {ok: true, v: result};
    } catch (error) {
        var threw = error + "\n" + (error.stack || "");
        console.log("[cipher] computeN threw: " + error);
        safeNError(reqId, threw);
        return {ok: false, e: threw};
    }
}

function selfTestSig() {
    try {
        if (typeof window._cipherSigFunc !== 'function') {
            return "missing:" + (typeof window._cipherSigFunc);
        }
        var probe = "AKfxrLsHZXBcZ0iA5pOnfEuG3jRmVtQwYd9qLkNbT2sPxWc4vJyHgUiOaZeRfMdSlCnBvXkQtYuIoPaSdFgHjKlZxCvBnMqWeRtYu";
        var out = window._cipherSigFunc(probe);
        if (typeof out !== 'string') return "bad_type:" + (typeof out);
        if (out.length === 0) return "empty_result";
        if (out === probe) return "identity_result";
        return "ok,inLen=" + probe.length + ",outLen=" + out.length;
    } catch (e) {
        return "threw:" + e;
    }
}

function discoverAndInit() {
    var nFuncName = "";
    var info = "";
    if (typeof window._nTransformFunc === 'function') {
        try {
            var testResult = window._nTransformFunc("test_abc");
            if (typeof testResult === 'string' && testResult !== "test_abc") {
                nFuncName = "injected_from_iife";
                info = "exported_ok,test_result=" + testResult.substring(0, 30);
            } else {
                info = "exported_but_bad_result:" + typeof testResult;
                window._nTransformFunc = null;
            }
        } catch(e) {
            info = "exported_but_threw:" + e;
            window._nTransformFunc = null;
        }
    }

    if (!nFuncName) {
        try {
            var testInput = "T2Xw3pWQ_Wk0xbOg";
            var keys = Object.getOwnPropertyNames(window);
            var tested = 0;
            var candidates = [];
            for (var i = 0; i < keys.length; i++) {
                try {
                    var key = keys[i];
                    if (key.startsWith("webkit") || key === "CipherBridge" || key === "_cipherSigFunc" || key === "_nTransformFunc") continue;
                    var fn = window[key];
                    if (typeof fn !== 'function' || fn.length !== 1) continue;
                    tested++;
                    var result = fn(testInput);
                    if (typeof result === 'string' && result !== testInput && result.length > 5) {
                        candidates.push(key + ":" + result.substring(0, 50));
                        if (result.indexOf('_w8_') >= 0) {
                            window._nTransformFunc = fn;
                            nFuncName = key;
                            break;
                        }
                    }
                } catch(e) {}
            }
            info = "brute_force:tested=" + tested + "/" + keys.length;
            if (!nFuncName && candidates.length > 0) {
                info += ",near_misses=" + candidates.slice(0, 5).join("|");
            }
        } catch(e) {
            info = "brute_force_error:" + e;
        }
    }
    CipherBridge.onDiscoveryDone(nFuncName, info, selfTestSig());
    CipherBridge.onPlayerJsLoaded();
}
</script>
<script src="player.js"
    onload="discoverAndInit()"
    onerror="CipherBridge.onPlayerJsError('Failed to load player.js from file')">
</script>
</head><body></body></html>"""

        webView.loadDataWithBaseURL(
            "file://${cacheDir.absolutePath}/",
            html, "text/html", "utf-8", null
        )
    }

    @JavascriptInterface
    fun onDiscoveryDone(funcName: String, info: String, sigTest: String) {
        if (funcName.isNotEmpty()) {
            Timber.tag(TAG).d("N-function DISCOVERED: $funcName ($info)")
            discoveredNFuncName = funcName
            nFunctionAvailable = true
        } else {
            Timber.tag(TAG).e("N-function NOT found ($info)")
            nFunctionAvailable = false
        }

        sigSelfTest = sigTest
        if (sigTest.startsWith("ok")) {
            Timber.tag(TAG).d("Sig self-test OK: $sigTest")
        } else {
            Timber.tag(TAG).e("Sig self-test FAILED: $sigTest — signature deobfuscation will likely fail")
        }
    }

    @JavascriptInterface
    fun onPlayerJsLoaded() {
        Timber.tag(TAG).d("Player JS loaded, n-func=${discoveredNFuncName ?: "none"}, sigSelfTest=$sigSelfTest")
        takeInitContinuation()?.resumeSafely { it.resume(this) }
    }

    @JavascriptInterface
    fun onPlayerJsError(error: String) {
        Timber.tag(TAG).e("Player JS load error: $error")
        takeInitContinuation()?.resumeSafely {
            it.resumeWithException(CipherException("Player JS load failed: $error"))
        }
    }

    suspend fun deobfuscateSignature(obfuscatedSig: String): String {
        if (sigInfo == null) {
            throw CipherException("Signature function info not available")
        }
        val constArgJs = sigInfo.constantArg?.toString() ?: "null"
        return evaluate(sigSlot, "sig") { reqId ->
            "JSON.stringify(computeSig($reqId, $constArgJs, '${escapeJsString(obfuscatedSig)}'))"
        }
    }

    @JavascriptInterface
    fun onSigResult(reqId: Int, result: String) {
        Timber.tag(TAG).d("Signature deobfuscated (bridge): ${result.take(30)}...")
        sigSlot.takeIfCurrent(reqId)?.resumeSafely { it.resume(result) }
    }

    @JavascriptInterface
    fun onSigError(reqId: Int, error: String) {
        Timber.tag(TAG).e("Signature deobfuscation error: $error")
        sigSlot.takeIfCurrent(reqId)?.resumeSafely { it.resumeWithException(CipherException("Sig deobfuscation failed: $error")) }
    }

    suspend fun transformN(nValue: String): String {
        if (!nFunctionAvailable) {
            throw CipherException("N-transform function not discovered")
        }
        return evaluate(nSlot, "n-transform") { reqId ->
            "JSON.stringify(computeN($reqId, '${escapeJsString(nValue)}'))"
        }
    }

    @JavascriptInterface
    fun onNResult(reqId: Int, result: String) {
        Timber.tag(TAG).d("N-transform result (bridge): ${result.take(50)}...")
        nSlot.takeIfCurrent(reqId)?.resumeSafely { it.resume(result) }
    }

    @JavascriptInterface
    fun onNError(reqId: Int, error: String) {
        Timber.tag(TAG).e("N-transform error: $error")
        nSlot.takeIfCurrent(reqId)?.resumeSafely { it.resumeWithException(CipherException("N-transform failed: $error")) }
    }

    /**
     * Runs [buildScript] in the renderer and returns its result.
     *
     * The eval is issued from `Dispatchers.Main` directly — never `webView.post`, which would never
     * run on this detached WebView. The timeout wraps the main-thread hop from *outside*, so a
     * wedged main thread still times out instead of hanging forever.
     */
    private suspend fun evaluate(
        slot: RequestSlot<String>,
        label: String,
        buildScript: (Int) -> String,
    ): String {
        throwIfDead()
        return try {
            withTimeout(EVAL_TIMEOUT_MS) {
                withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { cont ->
                        val reqId = slot.arm(cont)
                        cont.invokeOnCancellation { slot.takeIfCurrent(reqId) }
                        Timber.tag(TAG).d("Evaluating $label reqId=$reqId")
                        try {
                            webView.evaluateJavascript(buildScript(reqId)) { json ->
                                onEvalReturn(slot, reqId, label, json)
                            }
                        } catch (t: Throwable) {
                            slot.takeIfCurrent(reqId)?.resumeSafely {
                                it.resumeWithException(CipherException("$label evaluateJavascript failed: ${t.message}"))
                            }
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag(TAG).e("$label eval timed out after ${EVAL_TIMEOUT_MS}ms")
            failAsEvalTimeout("$label eval timed out after ${EVAL_TIMEOUT_MS}ms")
        }
    }

    /**
     * Handles the `ValueCallback` result of an eval. A no-op when the bridge already answered this
     * request: [RequestSlot.takeIfCurrent] removes under lock, so exactly one path ever resumes.
     */
    private fun onEvalReturn(slot: RequestSlot<String>, reqId: Int, label: String, json: String?) {
        val cont = slot.takeIfCurrent(reqId) ?: return
        try {
            if (json == null || json == "null") {
                cont.resumeWithException(CipherException("$label returned no value from renderer"))
                return
            }
            // evaluateJavascript hands back a JSON-encoded value, and the script's own result is a
            // JSON string, so the envelope arrives double-encoded and needs unwrapping first.
            val envelope = JSONObject(JSONArray("[$json]").getString(0))
            if (envelope.optBoolean("ok", false)) {
                cont.resume(envelope.getString("v"))
            } else {
                cont.resumeWithException(
                    CipherException("$label failed: ${envelope.optString("e", "unknown error")}")
                )
            }
        } catch (t: Throwable) {
            cont.resumeSafely {
                it.resumeWithException(
                    CipherException("$label result unparseable (${t.message}), raw=${json?.take(120)}")
                )
            }
        }
    }

    private fun throwIfDead() {
        if (isDead) {
            throw CipherRendererGoneException("CipherWebView renderer is gone")
        }
    }

    /**
     * A single lost eval is reported as [CipherEvalTimeoutException] rather than a renderer death,
     * so the caller rebuilds and retries once without arming the renderer-death backoff.
     */
    private fun failAsEvalTimeout(reason: String): Nothing {
        isDead = true
        sigSlot.takeAny()
        nSlot.takeAny()
        throw CipherEvalTimeoutException(reason)
    }

    fun close() {
        destroyWebView()
        Timber.tag(TAG).d("CipherWebView closed")
    }

    private fun destroyWebView() {
        if (destroyed) return
        destroyed = true
        runCatching {
            webView.clearHistory()
            webView.clearCache(true)
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        }.onFailure { Timber.tag(TAG).w("WebView teardown threw: $it") }
    }

    private fun countOccurrences(haystack: String, needle: String): Int {
        var count = 0
        var idx = haystack.indexOf(needle)
        while (idx >= 0) {
            count++
            idx = haystack.indexOf(needle, idx + needle.length)
        }
        return count
    }

    private fun escapeJsString(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    companion object {
        private const val TAG = "melo_CipherWebView"
        private const val JS_INTERFACE = "CipherBridge"
        private const val IIFE_MARKER = "})(_yt_player);"
        private const val CREATE_TIMEOUT_MS = 60_000L
        private const val EVAL_TIMEOUT_MS = 15_000L

        suspend fun create(
            context: Context,
            playerJs: String,
            sigInfo: FunctionNameExtractor.SigFunctionInfo?,
            nFuncInfo: FunctionNameExtractor.NFunctionInfo? = null,
        ): CipherWebView {
            var created: CipherWebView? = null
            try {
                return withTimeout(CREATE_TIMEOUT_MS) {
                    withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { cont ->
                            val wv = CipherWebView(context, playerJs, sigInfo, nFuncInfo, cont)
                            created = wv
                            wv.loadPlayerJsFromFile()
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Timber.tag(TAG).e("CipherWebView init timed out after ${CREATE_TIMEOUT_MS}ms")
                destroyQuietly(created)
                throw CipherRendererGoneException("CipherWebView init timed out")
            } catch (e: CancellationException) {
                destroyQuietly(created)
                throw e
            }
        }

        private suspend fun destroyQuietly(wv: CipherWebView?) {
            if (wv == null) return
            withContext(NonCancellable + Dispatchers.Main) {
                wv.isDead = true
                wv.takeInitContinuation()
                wv.destroyWebView()
            }
        }
    }
}

class CipherException(message: String) : Exception(message)
class CipherRendererGoneException(message: String) : Exception(message)

/** A single eval never came back. Recoverable: rebuild the WebView and retry once. */
class CipherEvalTimeoutException(message: String) : Exception(message)
