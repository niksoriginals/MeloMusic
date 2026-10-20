

package com.niksoriginals.melo.ui.screens.settings

import com.niksoriginals.melo.R
import android.os.Build
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.niksoriginals.melo.LocalPlayerAwareWindowInsets
import com.niksoriginals.melo.ui.component.IconButton
import com.niksoriginals.melo.ui.component.Material3SettingsGroup
import com.niksoriginals.melo.ui.component.Material3SettingsItem
import com.niksoriginals.melo.ui.utils.backToMain


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
highlightKey: String? = null) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchLower = searchQuery.lowercase()

    val appearanceText = stringResource(R.string.appearance)
    val playerText = stringResource(R.string.player_and_audio)
    val contentText = stringResource(R.string.content)
    val privacyText = stringResource(R.string.privacy)
    val storageText = stringResource(R.string.storage)
    val aboutText = stringResource(R.string.about)

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.search)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

        val itemsList = buildList {
            val statsText = stringResource(R.string.stats)
            if (statsText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == statsText),
                        icon = painterResource(R.drawable.stats),
                        title = { Text(statsText) },
                        onClick = { navController.navigate("stats") }
                    )
                )
            }

            val listenTogetherText = stringResource(R.string.listen_together)
            if (listenTogetherText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == listenTogetherText),
                        icon = painterResource(R.drawable.group_outlined),
                        title = { Text(listenTogetherText) },
                        onClick = { navController.navigate("settings/integrations/listen_together") }
                    )
                )
            }

            val instagramText = stringResource(R.string.instagram_notes)
            if (instagramText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == instagramText),
                        icon = painterResource(R.drawable.ic_instagram_new),
                        title = { Text(instagramText) },
                        onClick = { navController.navigate("settings/instagram") }
                    )
                )
            }

            if (appearanceText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == appearanceText),
                        icon = painterResource(R.drawable.palette),
                        title = { Text(appearanceText) },
                        onClick = { navController.navigate("settings/appearance") }
                    )
                )
            }
            if (playerText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == playerText),
                        icon = painterResource(R.drawable.play),
                        title = { Text(playerText) },
                        onClick = { navController.navigate("settings/player") }
                    )
                )
            }
            if (contentText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == contentText),
                        icon = painterResource(R.drawable.language),
                        title = { Text(contentText) },
                        onClick = { navController.navigate("settings/content") }
                    )
                )
            }

            if (privacyText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == privacyText),
                        icon = painterResource(R.drawable.security),
                        title = { Text(privacyText) },
                        onClick = { navController.navigate("settings/privacy") }
                    )
                )
            }
            if (storageText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == storageText),
                        icon = painterResource(R.drawable.storage),
                        title = { Text(storageText) },
                        onClick = { navController.navigate("settings/storage") }
                    )
                )
            }

            val systemUpdateText = stringResource(R.string.system_update)
            if (systemUpdateText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == systemUpdateText),
                        icon = painterResource(R.drawable.update),
                        title = { Text(systemUpdateText) },
                        onClick = { navController.navigate("settings/update") }
                    )
                )
            }

            if (aboutText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == aboutText),
                        icon = painterResource(R.drawable.info),
                        title = { Text(aboutText) },
                        onClick = { navController.navigate("settings/about") }
                    )
                )
            }
        }

        val finalItemsList = if (searchQuery.isNotEmpty()) {
            val subSettings = getAllSearchableSettings()

            val matchedSubSettings = subSettings
                .filter { it.first.lowercase().contains(searchLower) }
                .map { (title, parentTitle, route) ->
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.search),
                        title = { Text(title) },
                        description = { Text(parentTitle) },
                        onClick = { 
                            val encodedTitle = android.net.Uri.encode(title)
                            val finalRoute = if (route.contains("?")) "$route&highlightKey=$encodedTitle" else "$route?highlightKey=$encodedTitle"
                            navController.navigate(finalRoute)
                        }
                    )
                }
            
            itemsList + matchedSubSettings
        } else {
            itemsList
        }

        if (finalItemsList.isEmpty() && searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "No settings found for \"$searchQuery\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        } else {
            Material3SettingsGroup(scrollState = scrollState, items = finalItemsList)
        }
        
            Spacer(modifier = Modifier.height(50.dp))
            Spacer(
                Modifier.windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                )
            )
        }
    }
}
