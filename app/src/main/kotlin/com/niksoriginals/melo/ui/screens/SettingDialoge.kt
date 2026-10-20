package com.niksoriginals.melo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.niksoriginals.melo.BuildConfig
import com.niksoriginals.melo.R
import com.niksoriginals.melo.ui.component.Material3SettingsGroup
import com.niksoriginals.melo.ui.component.Material3SettingsItem
import com.niksoriginals.melo.ui.screens.Screens

@Composable
fun SettingDialoge(
    onDismissRequest: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val primaryColor = MaterialTheme.colorScheme.onSurface
        val onSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Spacer(modifier = Modifier.size(24.dp))
                    
                    Text(
                        text = "Melo",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = primaryColor,
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close",
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Quick Access / Features Group
                Material3SettingsGroup(
                    title = "Features",
                    compact = true,
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(androidx.compose.ui.res.stringResource(R.string.listen_together)) },
                            description = { Text("Listen to music in sync with friends") },
                            icon = painterResource(R.drawable.group_outlined),
                            onClick = {
                                onDismissRequest()
                                onNavigate(Screens.ListenTogether.route)
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text(androidx.compose.ui.res.stringResource(R.string.instagram_notes)) },
                            description = {
                                Text(
                                    text = "Sync current song to Notes",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            icon = painterResource(R.drawable.ic_instagram_new),
                            onClick = {
                                onDismissRequest()
                                onNavigate("settings/instagram")
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text(androidx.compose.ui.res.stringResource(R.string.stats)) },
                            icon = painterResource(R.drawable.stats),
                            onClick = {
                                onDismissRequest()
                                onNavigate("stats")
                            }
                        )
                    )
                )

                // App / Settings Group
                Material3SettingsGroup(
                    title = "App",
                    compact = true,
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(androidx.compose.ui.res.stringResource(R.string.settings)) },
                            icon = painterResource(R.drawable.settings),
                            onClick = {
                                onDismissRequest()
                                onNavigate("settings")
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text(androidx.compose.ui.res.stringResource(R.string.about)) },
                            icon = painterResource(R.drawable.info),
                            trailingContent = { Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                onDismissRequest()
                                onNavigate("settings/about")
                            }
                        )
                    )
                )

                // Footer Links
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSecondaryColor,
                        modifier = Modifier.clickable { uriHandler.openUri("https://niso.niksoriginals.in") }.padding(4.dp)
                    )
                    Text(text = " • ", color = onSecondaryColor, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Terms of Service",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSecondaryColor,
                        modifier = Modifier.clickable { uriHandler.openUri("https://niso.niksoriginals.in") }.padding(4.dp)
                    )
                }
            }
        }
    }
}
