package com.niksoriginals.melo.ui.screens.settings.integrations

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.niksoriginals.melo.LocalPlayerAwareWindowInsets
import com.niksoriginals.melo.R
import com.niksoriginals.melo.constants.InstagramNotesAudienceKey
import com.niksoriginals.melo.constants.InstagramNotesCustomCaptionKey
import com.niksoriginals.melo.constants.InstagramNotesEnabledKey
import com.niksoriginals.melo.constants.InstagramNotesListeningThresholdKey
import com.niksoriginals.melo.constants.InstagramSessionIdKey
import com.niksoriginals.melo.constants.InstagramUserIdKey
import com.niksoriginals.melo.constants.InstagramUsernameKey
import com.niksoriginals.melo.ui.component.IntegrationCard
import com.niksoriginals.melo.ui.component.IntegrationCardItem
import com.niksoriginals.melo.utils.rememberPreference
import kotlinx.coroutines.launch

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.niksoriginals.melo.viewmodels.InstagramViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstagramSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    viewModel: InstagramViewModel = hiltViewModel()
) {
    val instagramNotesManager = viewModel.instagramNotesManager
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (enabled, onEnabledChange) = rememberPreference(InstagramNotesEnabledKey, defaultValue = false)
    val (sessionId, _) = rememberPreference(InstagramSessionIdKey, defaultValue = "")
    val (userId, _) = rememberPreference(InstagramUserIdKey, defaultValue = "")
    val (username, _) = rememberPreference(InstagramUsernameKey, defaultValue = "")
    val (audience, onAudienceChange) = rememberPreference(InstagramNotesAudienceKey, defaultValue = 0)
    val (thresholdSec, onThresholdSecChange) = rememberPreference(InstagramNotesListeningThresholdKey, defaultValue = 20)
    val (customCaption, onCustomCaptionChange) = rememberPreference(InstagramNotesCustomCaptionKey, defaultValue = "")

    val isLoggedIn = sessionId.isNotBlank()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.instagram_notes)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Account / Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_instagram_new),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLoggedIn) {
                                    if (username.isNotBlank()) "@$username" else "Instagram Connected"
                                } else {
                                    stringResource(R.string.instagram_not_logged_in)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isLoggedIn) {
                                    if (userId.isNotBlank()) "User ID: $userId" else "Ready to sync music"
                                } else {
                                    "Connect to sync currently playing songs"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isLoggedIn) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    instagramNotesManager.clearSession()
                                    Toast.makeText(context, "Logged out of Instagram", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.instagram_logout))
                        }
                    } else {
                        Button(
                            onClick = { navController.navigate("settings/instagram/login") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.instagram_login))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Integration Card: Enable Toggle
            IntegrationCard(
                title = "Playback Sync",
                items = listOf(
                    IntegrationCardItem(
                        icon = painterResource(R.drawable.music_note),
                        title = { Text(stringResource(R.string.instagram_enable_notes)) },
                        description = { Text(stringResource(R.string.instagram_notes_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enabled && isLoggedIn,
                                onCheckedChange = {
                                    if (isLoggedIn) {
                                        onEnabledChange(it)
                                    } else {
                                        navController.navigate("settings/instagram/login")
                                    }
                                }
                            )
                        },
                        onClick = {
                            if (isLoggedIn) {
                                onEnabledChange(!enabled)
                            } else {
                                navController.navigate("settings/instagram/login")
                            }
                        }
                    )
                )
            )

            AnimatedVisibility(visible = enabled && isLoggedIn) {
                Column {
                    Spacer(Modifier.height(16.dp))

                    // Audience Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.instagram_audience),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = audience == 0,
                                    onClick = { onAudienceChange(0) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.instagram_audience_followers),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = audience == 1,
                                    onClick = { onAudienceChange(1) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.instagram_audience_close_friends),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Listening Threshold Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val currentThreshold = thresholdSec.coerceIn(15, 60)
                            Text(
                                text = "${stringResource(R.string.instagram_threshold)}: ${currentThreshold}s",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.instagram_threshold_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Slider(
                                value = currentThreshold.toFloat(),
                                onValueChange = { onThresholdSecChange(it.toInt()) },
                                valueRange = 15f..60f,
                                steps = 8
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Optional Caption Field
                    OutlinedTextField(
                        value = customCaption,
                        onValueChange = onCustomCaptionChange,
                        label = { Text(stringResource(R.string.instagram_custom_caption)) },
                        placeholder = { Text("e.g. 𝗟𝗶𝘀𝘁𝗲𝗻𝗶𝗻𝗴 𝗼𝗻 𝗠𝗲𝗹𝗼") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
