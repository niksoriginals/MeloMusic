package com.niksoriginals.melo.ui.screens.settings.integrations

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.niksoriginals.melo.R
import com.niksoriginals.melo.viewmodels.InstagramViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramLoginScreen(
    navController: NavController,
    viewModel: InstagramViewModel = hiltViewModel()
) {
    val instagramNotesManager = viewModel.instagramNotesManager
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var showManualInput by remember { mutableStateOf(false) }

    var manualSessionId by remember { mutableStateOf("") }
    var manualCsrf by remember { mutableStateOf("") }
    var manualUserId by remember { mutableStateOf("") }

    var hasHandledSuccess by remember { mutableStateOf(false) }

    fun checkAndSaveCookies() {
        if (hasHandledSuccess) return
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie("https://www.instagram.com") ?: return

        var extractedSessionId: String? = null
        var extractedCsrf: String? = null
        var extractedUserId: String? = null

        cookies.split(";").forEach { rawCookie ->
            val parts = rawCookie.trim().split("=")
            if (parts.size >= 2) {
                val name = parts[0].trim()
                val value = parts.subList(1, parts.size).joinToString("=").trim()
                when (name) {
                    "sessionid" -> extractedSessionId = value
                    "csrftoken" -> extractedCsrf = value
                    "ds_user_id" -> extractedUserId = value
                }
            }
        }

        val finalSessionId = extractedSessionId
        val finalCsrf = extractedCsrf
        if (!finalSessionId.isNullOrBlank() && !finalCsrf.isNullOrBlank()) {
            hasHandledSuccess = true
            scope.launch {
                instagramNotesManager.saveSession(
                    sessionId = finalSessionId,
                    csrfToken = finalCsrf,
                    userId = extractedUserId,
                    username = null
                )
                Toast.makeText(context, "Instagram Connected Successfully!", Toast.LENGTH_SHORT).show()
                navController.navigateUp()
            }
        }
    }

    // Polling observer while on login screen
    LaunchedEffect(Unit) {
        while (isActive && !hasHandledSuccess) {
            checkAndSaveCookies()
            delay(1500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.instagram_login)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = { showManualInput = !showManualInput }) {
                        Text(if (showManualInput) "WebView" else "Manual")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading && !showManualInput) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showManualInput) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Enter Instagram Session Cookie",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Paste your sessionid from your browser cookies if you prefer not logging in via WebView.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = manualSessionId,
                        onValueChange = { manualSessionId = it },
                        label = { Text("sessionid *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualCsrf,
                        onValueChange = { manualCsrf = it },
                        label = { Text("csrftoken (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualUserId,
                        onValueChange = { manualUserId = it },
                        label = { Text("ds_user_id (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (manualSessionId.isBlank()) {
                                Toast.makeText(context, "sessionid is required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val csrf = if (manualCsrf.isNotBlank()) manualCsrf else "csrf_token_placeholder"
                            scope.launch {
                                instagramNotesManager.saveSession(
                                    sessionId = manualSessionId.trim(),
                                    csrfToken = csrf.trim(),
                                    userId = manualUserId.trim().takeIf { it.isNotBlank() },
                                    username = null
                                )
                                Toast.makeText(context, "Instagram session saved!", Toast.LENGTH_SHORT).show()
                                navController.navigateUp()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save & Enable")
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                // Use modern Chrome Desktop user agent so Instagram doesn't get stuck on mobile PWA splash/intent
                                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress / 100f
                                    if (newProgress >= 100) {
                                        isLoading = false
                                    }
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val reqUrl = request?.url?.toString().orEmpty()
                                    // Block redirects to external instagram:// or intent:// schemes
                                    if (!reqUrl.startsWith("http://") && !reqUrl.startsWith("https://")) {
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    checkAndSaveCookies()
                                }
                            }

                            loadUrl("https://www.instagram.com/accounts/login/")
                        }
                    }
                )
            }
        }
    }
}
