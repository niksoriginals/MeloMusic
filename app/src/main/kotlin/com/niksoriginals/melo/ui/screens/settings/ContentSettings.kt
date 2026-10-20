

package com.niksoriginals.melo.ui.screens.settings

import android.content.Intent

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.niksoriginals.melo.LocalPlayerAwareWindowInsets
import com.niksoriginals.melo.R
import com.niksoriginals.melo.constants.AppLanguageKey
import com.niksoriginals.melo.constants.ContentCountryKey
import com.niksoriginals.melo.constants.ContentLanguageKey
import com.niksoriginals.melo.constants.SuggestionRegionKey
import com.niksoriginals.melo.constants.SuggestionRegionSlugToName
import com.niksoriginals.melo.ui.screens.search.suggestions.SuggestionRegionSheet
import com.niksoriginals.melo.constants.CountryCodeToName
import com.niksoriginals.melo.constants.EnableKugouKey
import com.niksoriginals.melo.constants.EnableLrcLibKey
import com.niksoriginals.melo.constants.EnablePaxsenixKey
import com.niksoriginals.melo.constants.HideExplicitKey
import com.niksoriginals.melo.constants.HideVideoSongsKey
import com.niksoriginals.melo.constants.HideYoutubeShortsKey
import com.niksoriginals.melo.constants.SponsorBlockEnabledKey
import com.niksoriginals.melo.constants.AlbumCanvasEnabledKey
import com.niksoriginals.melo.constants.LanguageCodeToName
import com.niksoriginals.melo.constants.QuickPicks
import com.niksoriginals.melo.constants.QuickPicksKey
import com.niksoriginals.melo.constants.RandomizeHomeOrderKey
import com.niksoriginals.melo.constants.SYSTEM_DEFAULT
import com.niksoriginals.melo.constants.ShowArtistDescriptionKey
import com.niksoriginals.melo.constants.ShowArtistSubscriberCountKey
import com.niksoriginals.melo.constants.ShowMonthlyListenersKey
import com.niksoriginals.melo.constants.ShowArtistVideoKey
import com.niksoriginals.melo.constants.ShowArtistBackgroundVideoKey
import com.niksoriginals.melo.constants.ShowSpeedDialKey
import com.niksoriginals.melo.constants.TopSize
import com.niksoriginals.melo.ui.component.EnumDialog
import com.niksoriginals.melo.ui.component.IconButton
import com.niksoriginals.melo.ui.component.Material3SettingsGroup
import com.niksoriginals.melo.ui.component.Material3SettingsItem
import com.niksoriginals.melo.ui.utils.backToMain
import com.niksoriginals.melo.utils.rememberEnumPreference
import com.niksoriginals.melo.utils.rememberPreference


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
highlightKey: String? = null) {
    val scrollState = androidx.compose.foundation.rememberScrollState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    
    val (appLanguage, onAppLanguageChange) = rememberPreference(key = AppLanguageKey, defaultValue = SYSTEM_DEFAULT)

    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (suggestionRegion, onSuggestionRegionChange) = rememberPreference(key = SuggestionRegionKey, defaultValue = "system")
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (hideVideoSongs, onHideVideoSongsChange) = rememberPreference(key = HideVideoSongsKey, defaultValue = false)

    val (hideYoutubeShorts, onHideYoutubeShortsChange) = rememberPreference(key = HideYoutubeShortsKey, defaultValue = false)
    val (sponsorBlockEnabled, onSponsorBlockEnabledChange) = rememberPreference(key = SponsorBlockEnabledKey, defaultValue = false)
    val (showArtistDescription, onShowArtistDescriptionChange) = rememberPreference(key = ShowArtistDescriptionKey, defaultValue = true)
    val (showArtistSubscriberCount, onShowArtistSubscriberCountChange) = rememberPreference(key = ShowArtistSubscriberCountKey, defaultValue = true)
    val (showMonthlyListeners, onShowMonthlyListenersChange) = rememberPreference(key = ShowMonthlyListenersKey, defaultValue = true)
    val (showArtistVideo, onShowArtistVideoChange) = rememberPreference(key = ShowArtistVideoKey, defaultValue = true)
    val (showArtistBackgroundVideo, onShowArtistBackgroundVideoChange) = rememberPreference(key = ShowArtistBackgroundVideoKey, defaultValue = true)
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(key = EnablePaxsenixKey, defaultValue = true)
    val (lengthTop, onLengthTopChange) = rememberPreference(key = TopSize, defaultValue = "50")
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(key = QuickPicksKey, defaultValue = QuickPicks.QUICK_PICKS)
    val (showSpeedDial, onShowSpeedDialChange) = rememberPreference(key = ShowSpeedDialKey, defaultValue = true)
    val (randomizeHomeOrder, onRandomizeHomeOrderChange) = rememberPreference(
        RandomizeHomeOrderKey,
        defaultValue = false
    )
    val (albumCanvasEnabled, onAlbumCanvasEnabledChange) = rememberPreference(key = AlbumCanvasEnabledKey, defaultValue = false)

    var showSuggestionSheet by rememberSaveable { mutableStateOf(false) }


    var showContentLanguageDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showContentLanguageDialog) {
        EnumDialog(
            onDismiss = { showContentLanguageDialog = false },
            onSelect = {
                onContentLanguageChange(it)
                showContentLanguageDialog = false
            },
            title = stringResource(R.string.content_language),
            current = contentLanguage,
            values = (listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()),
            valueText = {
                LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            }
        )
    }

    var showContentCountryDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showContentCountryDialog) {
        EnumDialog(
            onDismiss = { showContentCountryDialog = false },
            onSelect = {
                onContentCountryChange(it)
                showContentCountryDialog = false
            },
            title = stringResource(R.string.content_country),
            current = contentCountry,
            values = (listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList()),
            valueText = {
                CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            }
        )
    }

    var showAppLanguageDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showAppLanguageDialog) {
        EnumDialog(
            onDismiss = { showAppLanguageDialog = false },
            onSelect = {
                onAppLanguageChange(it)
                showAppLanguageDialog = false
            },
            title = stringResource(R.string.app_language),
            current = appLanguage,
            values = (listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()),
            valueText = {
                LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            }
        )
    }


    var showQuickPicksDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showQuickPicksDialog) {
        EnumDialog(
            onDismiss = { showQuickPicksDialog = false },
            onSelect = {
                onQuickPicksChange(it)
                showQuickPicksDialog = false
            },
            title = stringResource(R.string.set_quick_picks),
            current = quickPicks,
            values = QuickPicks.values().toList(),
            valueText = {
                when (it) {
                    QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                    QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                }
            }
        )
    }

    var showTopLengthDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showTopLengthDialog) {
        var tempLength by rememberSaveable { mutableFloatStateOf(lengthTop.toFloat()) }

        AlertDialog(
            onDismissRequest = { showTopLengthDialog = false },
            title = { Text(stringResource(R.string.top_length)) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tempLength.toInt().toString())
                    Slider(
                        value = tempLength,
                        onValueChange = { tempLength = it },
                        valueRange = 1f..100f,
                        steps = 98
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLengthTopChange(tempLength.toInt().toString())
                        showTopLengthDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        )
    }


    if (showSuggestionSheet) {
        SuggestionRegionSheet(
            currentRegionSlug = suggestionRegion,
            onRegionSelected = { onSuggestionRegionChange(it) },
            onDismiss = { showSuggestionSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.content)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                }
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
                .padding(horizontal = 16.dp),
        ) {
            Material3SettingsGroup(scrollState = scrollState, 
                title = stringResource(R.string.general),
            items = listOf(
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.content_language)),
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.content_language)) },
                    description = {
                        Text(
                            LanguageCodeToName.getOrElse(contentLanguage) { stringResource(R.string.system_default) }
                        )
                    },
                    onClick = { showContentLanguageDialog = true }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.content_country)),
                    icon = painterResource(R.drawable.location_on),
                    title = { Text(stringResource(R.string.content_country)) },
                    description = {
                        Text(
                            CountryCodeToName.getOrElse(contentCountry) { stringResource(R.string.system_default) }
                        )
                    },
                    onClick = { showContentCountryDialog = true }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == "Suggestions Region"),
                    icon = painterResource(R.drawable.globe_location_pin),
                    title = { Text("Suggestions Region") },
                    description = {
                        Text(
                            SuggestionRegionSlugToName.getOrElse(suggestionRegion) { "Global Charts" }
                        )
                    },
                    onClick = { showSuggestionSheet = true }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.hide_explicit)),
                    icon = painterResource(R.drawable.explicit),
                    title = { Text(stringResource(R.string.hide_explicit)) },
                    trailingContent = {
                        Switch(
                            checked = hideExplicit,
                            onCheckedChange = onHideExplicitChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (hideExplicit) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onHideExplicitChange(!hideExplicit) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.hide_video_songs)),
                    icon = painterResource(R.drawable.slow_motion_video),
                    title = { Text(stringResource(R.string.hide_video_songs)) },
                    trailingContent = {
                        Switch(
                            checked = hideVideoSongs,
                            onCheckedChange = onHideVideoSongsChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (hideVideoSongs) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onHideVideoSongsChange(!hideVideoSongs) }
                ),

                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.hide_youtube_shorts)),
                    icon = painterResource(R.drawable.hide_image),
                    title = { Text(stringResource(R.string.hide_youtube_shorts)) },
                    trailingContent = {
                        Switch(
                            checked = hideYoutubeShorts,
                            onCheckedChange = onHideYoutubeShortsChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (hideYoutubeShorts) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onHideYoutubeShortsChange(!hideYoutubeShorts) }
                ),
                Material3SettingsItem(
                    isHighlighted = (highlightKey == stringResource(R.string.content_sponsor_block)),
                    icon = painterResource(R.drawable.skip_next),
                    title = { Text(stringResource(R.string.content_sponsor_block)) },
                    description = { Text(stringResource(R.string.content_sponsor_block_desc)) },
                    trailingContent = {
                        Switch(
                            checked = sponsorBlockEnabled,
                            onCheckedChange = onSponsorBlockEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (sponsorBlockEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSponsorBlockEnabledChange(!sponsorBlockEnabled) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.artist_page_settings),
            items = listOf(
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.show_artist_description)),
                    icon = painterResource(R.drawable.info),
                    title = { Text(stringResource(R.string.show_artist_description)) },
                    trailingContent = {
                        Switch(
                            checked = showArtistDescription,
                            onCheckedChange = onShowArtistDescriptionChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showArtistDescription) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowArtistDescriptionChange(!showArtistDescription) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.show_artist_subscriber_count)),
                    icon = painterResource(R.drawable.person),
                    title = { Text(stringResource(R.string.show_artist_subscriber_count)) },
                    trailingContent = {
                        Switch(
                            checked = showArtistSubscriberCount,
                            onCheckedChange = onShowArtistSubscriberCountChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showArtistSubscriberCount) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowArtistSubscriberCountChange(!showArtistSubscriberCount) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.show_artist_monthly_listeners)),
                    icon = painterResource(R.drawable.person),
                    title = { Text(stringResource(R.string.show_artist_monthly_listeners)) },
                    trailingContent = {
                        Switch(
                            checked = showMonthlyListeners,
                            onCheckedChange = onShowMonthlyListenersChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showMonthlyListeners) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowMonthlyListenersChange(!showMonthlyListeners) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.show_artist_video)),
                    icon = painterResource(R.drawable.slow_motion_video),
                    title = { Text(stringResource(R.string.show_artist_video)) },
                    description = { Text(stringResource(R.string.show_artist_video_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showArtistVideo,
                            onCheckedChange = onShowArtistVideoChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showArtistVideo) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowArtistVideoChange(!showArtistVideo) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.show_artist_background_video)),
                    icon = painterResource(R.drawable.slow_motion_video),
                    title = { Text(stringResource(R.string.show_artist_background_video)) },
                    description = { Text(stringResource(R.string.show_artist_background_video_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showArtistBackgroundVideo,
                            onCheckedChange = onShowArtistBackgroundVideoChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showArtistBackgroundVideo) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowArtistBackgroundVideoChange(!showArtistBackgroundVideo) }
                )
            )
        )

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.album_text),
            items = listOf(
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.show_album_canvas)),
                    icon = painterResource(R.drawable.slow_motion_video),
                    title = { Text(stringResource(R.string.show_album_canvas)) },
                    description = { Text(stringResource(R.string.show_album_canvas_desc)) },
                    trailingContent = {
                        Switch(
                            checked = albumCanvasEnabled,
                            onCheckedChange = onAlbumCanvasEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (albumCanvasEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAlbumCanvasEnabledChange(!albumCanvasEnabled) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.app_language),
            items = listOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.app_language)),
                        icon = painterResource(R.drawable.language),
                        title = { Text(stringResource(R.string.app_language)) },
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APP_LOCALE_SETTINGS,
                                    "package:${context.packageName}".toUri()
                                )
                            )
                        }
                    )
                } else {
                    Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.app_language)),
                        icon = painterResource(R.drawable.language),
                        title = { Text(stringResource(R.string.app_language)) },
                        description = {
                            Text(
                                LanguageCodeToName.getOrElse(appLanguage) { stringResource(R.string.system_default) }
                            )
                        },
                        onClick = { showAppLanguageDialog = true }
                    )
                }
            )
        )


        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.lyrics),
            items = listOf(
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.enable_lrclib)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.enable_lrclib)) },
                    trailingContent = {
                        Switch(
                            checked = enableLrclib,
                            onCheckedChange = onEnableLrclibChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableLrclib) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onEnableLrclibChange(!enableLrclib) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.enable_kugou)),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.enable_kugou)) },
                    trailingContent = {
                        Switch(
                            checked = enableKugou,
                            onCheckedChange = onEnableKugouChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enableKugou) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onEnableKugouChange(!enableKugou) }

                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == "PaxSenix"),
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text("PaxSenix") },
                    description = { Text("Apple Music quality synced lyrics with syllable-level timing") },
                    trailingContent = {
                        Switch(
                            checked = enablePaxsenix,
                            onCheckedChange = onEnablePaxsenixChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enablePaxsenix) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onEnablePaxsenixChange(!enablePaxsenix) }
                ),

                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.lyrics_romanization)),
                    icon = painterResource(R.drawable.language_korean_latin),
                    title = { Text(stringResource(R.string.lyrics_romanization)) },
                    onClick = { navController.navigate("settings/content/romanization") }
                )
            )
        )


        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(scrollState = scrollState, 
            title = stringResource(R.string.misc),
            items = listOf(
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.randomize_home_order)),
                    icon = painterResource(R.drawable.shuffle),
                    title = { Text(stringResource(R.string.randomize_home_order)) },
                    description = { Text(stringResource(R.string.randomize_home_order_desc)) },
                    trailingContent = {
                        Switch(
                            checked = randomizeHomeOrder,
                            onCheckedChange = onRandomizeHomeOrderChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (randomizeHomeOrder) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onRandomizeHomeOrderChange(!randomizeHomeOrder) }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.top_length)),
                    icon = painterResource(R.drawable.trending_up),
                    title = { Text(stringResource(R.string.top_length)) },
                    description = { Text(lengthTop) },
                    onClick = { showTopLengthDialog = true }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == stringResource(R.string.set_quick_picks)),
                    icon = painterResource(R.drawable.home_outlined),
                    title = { Text(stringResource(R.string.set_quick_picks)) },
                    description = {
                        Text(
                            when (quickPicks) {
                                QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                                QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                            }
                        )
                    },
                    onClick = { showQuickPicksDialog = true }
                ),
                Material3SettingsItem(
    isHighlighted = (highlightKey == "Speed Dial"),
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text("Speed Dial") },
                    description = { Text("Show Speed Dial on the Home Screen") },
                    trailingContent = {
                        Switch(
                            checked = showSpeedDial,
                            onCheckedChange = { onShowSpeedDialChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    onClick = { onShowSpeedDialChange(!showSpeedDial) }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    
            Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)))
        }
    }
}
