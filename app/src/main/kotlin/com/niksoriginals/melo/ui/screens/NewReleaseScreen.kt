

package com.niksoriginals.melo.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.niksoriginals.melo.LocalPlayerAwareWindowInsets
import com.niksoriginals.melo.LocalPlayerConnection
import com.niksoriginals.melo.R
import com.niksoriginals.melo.constants.GridItemSize
import com.niksoriginals.melo.constants.GridItemsSizeKey
import com.niksoriginals.melo.constants.GridThumbnailHeight
import com.niksoriginals.melo.ui.component.IconButton
import com.niksoriginals.melo.ui.component.LocalMenuState
import com.niksoriginals.melo.ui.component.YouTubeGridItem
import com.niksoriginals.melo.ui.component.shimmer.GridItemPlaceHolder
import com.niksoriginals.melo.ui.component.shimmer.ShimmerHost
import com.niksoriginals.melo.ui.menu.YouTubeAlbumMenu
import com.niksoriginals.melo.ui.utils.backToMain
import com.niksoriginals.melo.utils.rememberEnumPreference
import com.niksoriginals.melo.viewmodels.NewReleaseViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NewReleaseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: NewReleaseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val newReleaseAlbums by viewModel.newReleaseAlbums.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_release_albums)) },
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
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            items(
                items = newReleaseAlbums.distinctBy { it.id },
                key = { it.id },
            ) { album ->
                YouTubeGridItem(
                    item = album,
                    isActive = mediaMetadata?.album?.id == album.id,
                    isPlaying = isPlaying,
                    fillMaxWidth = true,
                    coroutineScope = coroutineScope,
                    modifier =
                    Modifier
                        .combinedClickable(
                            onClick = {
                                navController.navigate("album/${album.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeAlbumMenu(
                                        albumItem = album,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ),
                )
            }

            if (newReleaseAlbums.isEmpty()) {
                items(8) {
                    ShimmerHost {
                        GridItemPlaceHolder(fillMaxWidth = true)
                    }
                }
            }
        }
    }
}
