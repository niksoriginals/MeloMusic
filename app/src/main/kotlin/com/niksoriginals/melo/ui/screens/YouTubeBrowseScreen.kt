

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.niksoriginals.melo.LocalPlayerAwareWindowInsets
import com.niksoriginals.melo.LocalPlayerConnection
import com.niksoriginals.melo.R
import com.niksoriginals.melo.constants.GridItemSize
import com.niksoriginals.melo.constants.GridItemsSizeKey
import com.niksoriginals.melo.constants.GridThumbnailHeight
import com.niksoriginals.melo.models.toMediaMetadata
import com.niksoriginals.melo.playback.queues.YouTubeQueue
import com.niksoriginals.melo.ui.component.IconButton
import com.niksoriginals.melo.ui.component.LocalMenuState
import com.niksoriginals.melo.ui.component.YouTubeGridItem
import com.niksoriginals.melo.ui.component.shimmer.GridItemPlaceHolder
import com.niksoriginals.melo.ui.component.shimmer.ShimmerHost
import com.niksoriginals.melo.ui.menu.YouTubeAlbumMenu
import com.niksoriginals.melo.ui.menu.YouTubeArtistMenu
import com.niksoriginals.melo.ui.menu.YouTubePlaylistMenu
import com.niksoriginals.melo.ui.menu.YouTubeSongMenu
import com.niksoriginals.melo.ui.utils.backToMain
import com.niksoriginals.melo.utils.rememberEnumPreference
import com.niksoriginals.melo.viewmodels.YouTubeBrowseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YouTubeBrowseScreen(
    navController: NavController,
    viewModel: YouTubeBrowseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val browseResult by viewModel.result.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val allItems = browseResult?.items?.flatMap { it.items } ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(browseResult?.title.orEmpty()) },
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
                }
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
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (browseResult == null) {
                items(8) {
                    ShimmerHost {
                        GridItemPlaceHolder(fillMaxWidth = true)
                    }
                }
            }

            items(
                items = allItems.distinctBy { it.id },
                key = { it.id }
            ) { item ->
                YouTubeGridItem(
                    item = item,
                    isActive = when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        else -> false
                    },
                    isPlaying = isPlaying,
                    fillMaxWidth = true,
                    coroutineScope = coroutineScope,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is SongItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(item.toMediaMetadata())
                                            )
                                        }
                                    }
                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    when (item) {
                                        is SongItem ->
                                            YouTubeSongMenu(
                                                song = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        is AlbumItem ->
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        is ArtistItem ->
                                            YouTubeArtistMenu(
                                                artist = item,
                                                onDismiss = menuState::dismiss,
                                            )
                                        is PlaylistItem ->
                                            YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                    }
                                }
                            }
                        )
                        .animateItem()
                )
            }
        }
    }
}
