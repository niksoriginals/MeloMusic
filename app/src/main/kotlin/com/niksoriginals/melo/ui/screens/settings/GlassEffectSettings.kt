package com.niksoriginals.melo.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.niksoriginals.melo.LocalPlayerAwareWindowInsets
import com.niksoriginals.melo.R
import com.niksoriginals.melo.constants.LiquidGlassGlobalEnabledKey
import com.niksoriginals.melo.constants.LiquidGlassMiniPlayerEnabledKey
import com.niksoriginals.melo.constants.LiquidGlassNavBarEnabledKey
import com.niksoriginals.melo.constants.LiquidGlassHomeEnabledKey
import com.niksoriginals.melo.constants.LiquidGlassPlayerEnabledKey
import com.niksoriginals.melo.constants.UseFloatingNavBarKey
import com.niksoriginals.melo.ui.component.IconButton as AppIconButton
import com.niksoriginals.melo.ui.component.Material3SettingsGroup
import com.niksoriginals.melo.ui.component.Material3SettingsItem
import com.niksoriginals.melo.ui.utils.backToMain
import com.niksoriginals.melo.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassEffectSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (globalEnabled, onGlobalEnabledChange) = rememberPreference(
        LiquidGlassGlobalEnabledKey, defaultValue = false
    )
    val (playerEnabled, onPlayerEnabledChange) = rememberPreference(
        LiquidGlassPlayerEnabledKey, defaultValue = true
    )
    val (miniPlayerEnabled, onMiniPlayerEnabledChange) = rememberPreference(
        LiquidGlassMiniPlayerEnabledKey, defaultValue = true
    )
    val (navBarEnabled, onNavBarEnabledChange) = rememberPreference(
        LiquidGlassNavBarEnabledKey, defaultValue = true
    )
    val (homeEnabled, onHomeEnabledChange) = rememberPreference(
        LiquidGlassHomeEnabledKey, defaultValue = true
    )
    val (useFloatingNavBar, onUseFloatingNavBarChange) = rememberPreference(
        UseFloatingNavBarKey, defaultValue = false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.liquid_glass_settings)) },
                navigationIcon = {
                    AppIconButton(
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
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Material3SettingsGroup(
            title = stringResource(R.string.nav_bar_style),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.floating_nav_bar)) },
                    description = { Text(stringResource(R.string.floating_nav_bar_desc)) },
                    trailingContent = {
                        Switch(
                            checked = useFloatingNavBar,
                            onCheckedChange = onUseFloatingNavBarChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (useFloatingNavBar) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onUseFloatingNavBarChange(!useFloatingNavBar) }
                ),
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.check),
                    title = { Text(stringResource(R.string.liquid_glass_global_enabled)) },
                    description = {
                        Text(
                            stringResource(
                                if (useFloatingNavBar) {
                                    R.string.liquid_glass_performance_warning
                                } else {
                                    R.string.liquid_glass_requires_floating_nav_bar
                                }
                            )
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = globalEnabled && useFloatingNavBar,
                            onCheckedChange = onGlobalEnabledChange,
                            enabled = useFloatingNavBar,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (globalEnabled && useFloatingNavBar) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (useFloatingNavBar) onGlobalEnabledChange(!globalEnabled) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass_per_component),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.liquid_glass_player)) },
                    trailingContent = {
                        Switch(
                            checked = playerEnabled,
                            onCheckedChange = onPlayerEnabledChange,
                            enabled = globalEnabled && useFloatingNavBar,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (playerEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (globalEnabled && useFloatingNavBar) onPlayerEnabledChange(!playerEnabled) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.liquid_glass_mini_player)) },
                    trailingContent = {
                        Switch(
                            checked = miniPlayerEnabled,
                            onCheckedChange = onMiniPlayerEnabledChange,
                            enabled = globalEnabled && useFloatingNavBar,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (miniPlayerEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (globalEnabled && useFloatingNavBar) onMiniPlayerEnabledChange(!miniPlayerEnabled) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.liquid_glass_nav_bar)) },
                    trailingContent = {
                        Switch(
                            checked = navBarEnabled,
                            onCheckedChange = onNavBarEnabledChange,
                            enabled = globalEnabled && useFloatingNavBar,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (navBarEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (globalEnabled && useFloatingNavBar) onNavBarEnabledChange(!navBarEnabled) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.home_outlined),
                    title = { Text(stringResource(R.string.liquid_glass_home)) },
                    trailingContent = {
                        Switch(
                            checked = homeEnabled,
                            onCheckedChange = onHomeEnabledChange,
                            enabled = globalEnabled && useFloatingNavBar,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (homeEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (globalEnabled && useFloatingNavBar) onHomeEnabledChange(!homeEnabled) }
                ),
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
            )
        )
        }
    }
}
