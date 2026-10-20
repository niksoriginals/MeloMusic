

package com.niksoriginals.melo.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.produceState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.niksoriginals.melo.ui.component.ActionPromptDialog
import com.niksoriginals.melo.ui.component.GlassComponent
import com.niksoriginals.melo.ui.component.LocalGlassEffectConfig
import com.niksoriginals.melo.ui.component.PLAYER_BLUR_MULTIPLIER
import com.niksoriginals.melo.ui.component.isGlassSupported
import com.niksoriginals.melo.ui.component.liquidGlass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import coil3.size.Size as CoilSize
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niksoriginals.melo.ui.menu.PlayerMenu
import com.niksoriginals.melo.ui.menu.ListenTogetherDialog
import com.niksoriginals.melo.ui.screens.CommentSheet
import androidx.core.view.WindowCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.niksoriginals.melo.LocalDatabase
import com.niksoriginals.melo.LocalDownloadUtil
import com.niksoriginals.melo.LocalListenTogetherManager
import com.niksoriginals.melo.LocalPlayerConnection
import com.niksoriginals.melo.R
import com.niksoriginals.melo.constants.AudioQuality
import com.niksoriginals.melo.constants.AudioQualityKey
import com.niksoriginals.melo.constants.CropAlbumArtKey
import com.niksoriginals.melo.constants.DarkModeKey
import com.niksoriginals.melo.constants.HidePlayerThumbnailKey
import com.niksoriginals.melo.constants.HideStatusBarOnFullscreenKey
import com.niksoriginals.melo.constants.EnableLyricsThumbnailPlayPauseKey
import com.niksoriginals.melo.constants.KeepScreenOn
import com.niksoriginals.melo.constants.MiniPlayerHeight
import com.niksoriginals.melo.constants.PlayerBackgroundStyle
import com.niksoriginals.melo.constants.PlayerBackgroundStyleKey
import com.niksoriginals.melo.constants.PlayerButtonsStyle
import com.niksoriginals.melo.constants.PlayerButtonsStyleKey
import com.niksoriginals.melo.constants.PlayerHorizontalPadding
import com.niksoriginals.melo.constants.QueuePeekHeight
import com.niksoriginals.melo.constants.SliderStyle
import com.niksoriginals.melo.constants.SliderStyleKey
import com.niksoriginals.melo.constants.SquigglySliderKey
import com.niksoriginals.melo.constants.SwipeLyricsKey
import com.niksoriginals.melo.constants.ThumbnailCornerRadius
import com.niksoriginals.melo.constants.UseNewPlayerDesignKey
import com.niksoriginals.melo.db.entities.LyricsEntity
import com.niksoriginals.melo.extensions.SwipeGesture
import com.niksoriginals.melo.extensions.togglePlayPause
import com.niksoriginals.melo.extensions.toggleRepeatMode
import com.niksoriginals.melo.models.MediaMetadata
import com.niksoriginals.melo.core.getConnectedBluetoothDeviceName
import com.niksoriginals.melo.core.isBuds
import com.niksoriginals.melo.core.isSpeaker
import com.niksoriginals.melo.ui.component.BottomSheet
import com.niksoriginals.melo.ui.component.BottomSheetState
import com.niksoriginals.melo.ui.component.LocalBottomSheetPageState
import com.niksoriginals.melo.ui.component.LocalMenuState
import com.niksoriginals.melo.ui.component.LiquidGlassThumb
import com.niksoriginals.melo.ui.component.Lyrics
import com.niksoriginals.melo.ui.component.PlayerSliderTrack
import com.niksoriginals.melo.ui.component.ResizableIconButton
import com.niksoriginals.melo.ui.component.SquigglySlider
import com.niksoriginals.melo.ui.component.WavySlider
import com.niksoriginals.melo.ui.component.rememberBottomSheetState
import com.niksoriginals.melo.ui.menu.OldPlayerMenu
import com.niksoriginals.melo.ui.menu.PlayerMenu
import com.niksoriginals.melo.ui.screens.settings.DarkMode
import com.niksoriginals.melo.ui.theme.PlayerColorExtractor
import com.niksoriginals.melo.ui.theme.PlayerSliderColors
import com.niksoriginals.melo.ui.utils.ShowMediaInfo
import com.niksoriginals.melo.ui.utils.ShowOffsetDialog
import com.niksoriginals.melo.utils.makeTimeString
import com.niksoriginals.melo.utils.isLocalMediaId
import com.niksoriginals.melo.utils.rememberEnumPreference
import com.niksoriginals.melo.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import com.niksoriginals.melo.ui.component.Icon as MIcon
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.DefaultLoadControl
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.niksoriginals.melo.applecanvas.AppleMusicCanvasProvider
import com.niksoriginals.melo.canvas.CanvasArtwork
import com.niksoriginals.melo.canvas.TidalCanvasProvider
import com.niksoriginals.melo.constants.CanvasThumbnailAnimationKey
import com.niksoriginals.melo.ui.player.CanvasArtworkPlaybackCache
import com.niksoriginals.melo.ui.player.normalizeCanvasArtistName
import com.niksoriginals.melo.ui.player.normalizeCanvasSongTitle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size

private data class WavyShape(
    val sides: Int,
    val indent: Float,
    val rotationDegrees: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val maxRadiusX = size.width / 2f
        val maxRadiusY = size.height / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        val steps = 120
        val rotationRad = rotationDegrees * Math.PI / 180.0
        for (i in 0..steps) {
            val angle = i * Math.PI * 2 / steps
            val bumpAngle = angle - rotationRad
            val r = 1f - indent + indent * cos(sides * bumpAngle)
            val x = cx + maxRadiusX * r * cos(angle)
            val y = cy + maxRadiusY * r * sin(angle)
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat())
            else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = true
    )
    val showCodecOnPlayer by rememberPreference(com.niksoriginals.melo.constants.ShowCodecOnPlayerKey, false)
    val hidePlayerSlider by rememberPreference(com.niksoriginals.melo.constants.HidePlayerSliderKey, false)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isLocalMedia = mediaMetadata?.id?.isLocalMediaId() == true

    val glassConfig = LocalGlassEffectConfig.current
    val isPlayerGlassActive = glassConfig.isEnabledFor(GlassComponent.PLAYER) && isGlassSupported()

    val playerBackgroundPref by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT
    )
    val playerBackground = if (isLocalMedia) PlayerBackgroundStyle.DEFAULT else playerBackgroundPref
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val dataSaverEnabled by rememberPreference(key = com.niksoriginals.melo.constants.DataSaverEnabledKey, defaultValue = false)
    val enableCanvasPref by rememberPreference(CanvasThumbnailAnimationKey, true)
    val enableCanvas = if (dataSaverEnabled) false else enableCanvasPref

    val shouldUseDarkButtonColors = remember(playerBackground, useDarkTheme, isPlayerGlassActive) {
        if (isPlayerGlassActive) true
        else when (playerBackground) {
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH, PlayerBackgroundStyle.LIQUID_GLASS -> true
            PlayerBackgroundStyle.DEFAULT -> useDarkTheme
        }
    }
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val isCrossfading by playerConnection.isCrossfading.collectAsState()
    val isAutomixing by playerConnection.isAutomixing.collectAsState()
    val automixDebug by playerConnection.automixDebugInfo.collectAsState()
    val automixDebugOverlayEnabled by rememberPreference(com.niksoriginals.melo.constants.AutomixDebugOverlayKey, false)

    var currentAudioFormat by remember { mutableStateOf<androidx.media3.common.Format?>(null) }
    DisposableEffect(playerConnection, isCrossfading) {
        val playerToListen = playerConnection.player
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val audioTrack = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
                currentAudioFormat = audioTrack?.getTrackFormat(0)
            }
        }
        playerToListen.addListener(listener)
        currentAudioFormat = playerToListen.currentTracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO }?.getTrackFormat(0)
        onDispose {
            playerToListen.removeListener(listener)
        }
    }
    val swipeLyrics by rememberPreference(SwipeLyricsKey, false)
    val enableLyricsThumbnailPlayPause by rememberPreference(EnableLyricsThumbnailPlayPauseKey, false)
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    DisposableEffect(playerBackground, state.isExpanded, useDarkTheme, keepScreenOn, mediaMetadata?.id) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            
            val isLocal = mediaMetadata?.id?.isLocalMediaId() == true
            if (isLocal || playerBackground in listOf(PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH)) {
                insetsController.isAppearanceLightStatusBars = false
            } else {
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
            }

            if (keepScreenOn && state.isExpanded)
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val currentFormatEntity by database.format(mediaMetadata?.id).collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val isMuted by playerConnection.isMuted.collectAsState()
    val playerVolume by playerConnection.service.playerVolume.collectAsState()

    val (audioQuality) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.OPUS
    )
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val squigglySlider by rememberPreference(SquigglySliderKey, defaultValue = false)
    
    
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest by listenTogetherManager?.guestPlaybackRestricted?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    
    
    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castDeviceName by castHandler?.castDeviceName?.collectAsState() ?: remember { mutableStateOf(null) }
    val castPosition by castHandler?.castPosition?.collectAsState() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsState() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsState() ?: remember { mutableFloatStateOf(1f) }
    
    
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    
    val positionState = remember { mutableLongStateOf(0L) }
    val durationState = remember { mutableLongStateOf(0L) }
    
    
    var position by positionState
    var duration by durationState
    
    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) {
                castPosition
            } else {
                position
            }
        }
    }
    
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    val automixDebugOverlay: @Composable () -> Unit = {
        if (automixDebugOverlayEnabled) {
            automixDebug?.let { dbg ->
                val mono = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = PlayerHorizontalPadding, vertical = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(6.dp)
                ) {
                    Text("AUTOMIX  ${dbg.status}", style = mono, color = Color.White)
                    Text(
                        "out: ${dbg.outBpm?.let { "%.1f bpm".format(it) } ?: "—"}" +
                            (dbg.outConfidence?.let { "  conf %.2f".format(it) } ?: "") +
                            (dbg.outMixOutMs?.takeIf { it > 0 }?.let { "  mixOut ${makeTimeString(it)}" } ?: ""),
                        style = mono, color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        "in:  ${dbg.inBpm?.let { "%.1f bpm".format(it) } ?: "—"}" +
                            (dbg.inConfidence?.let { "  conf %.2f".format(it) } ?: "") +
                            (dbg.inMixInMs?.takeIf { it > 0 }?.let { "  mixIn ${makeTimeString(it)}" } ?: ""),
                        style = mono, color = Color.White.copy(alpha = 0.85f)
                    )
                    if (dbg.triggerTimeMs != null) {
                        val remainingS = ((dbg.triggerTimeMs - (sliderPosition ?: effectivePosition)) / 1000).coerceAtLeast(0)
                        Text(
                            "mix @ ${makeTimeString(dbg.triggerTimeMs)} (in ${remainingS}s)" +
                                (dbg.incomingStartMs?.let { "  from ${makeTimeString(it)}" } ?: "") +
                                (dbg.tempoRatio?.let { "  ×%.3f".format(it) } ?: ""),
                            style = mono, color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }

    var lastManualSeekTime by remember { mutableLongStateOf(0L) }
    
    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val bluetoothDeviceName by produceState<String?>(initialValue = getConnectedBluetoothDeviceName(context)) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                value = getConnectedBluetoothDeviceName(context)
            }
        }

        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    value = getConnectedBluetoothDeviceName(context)
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    value = getConnectedBluetoothDeviceName(context)
                }
            }
        } else null

        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.media.AUDIO_BECOMING_NOISY")
        }
        
        context.registerReceiver(receiver, filter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }
        
        awaitDispose {
            context.unregisterReceiver(receiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxSystemVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    val systemVolume by produceState(initialValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                    value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(receiver, filter)
        awaitDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(currentMetadata.thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .memoryCacheKey("gradient_${currentMetadata.id}")
                        .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(8)
                                    .resizeBitmapArea(100 * 100)
                                    .generate()
                            }
                            val extractedColors = if (playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) {
                                listOfNotNull(
                                    palette.getVibrantColor(fallbackColor).let { Color(it) },
                                    palette.getLightVibrantColor(fallbackColor).let { Color(it) },
                                    palette.getDarkVibrantColor(fallbackColor).let { Color(it) },
                                    palette.getMutedColor(fallbackColor).let { Color(it) },
                                    palette.getLightMutedColor(fallbackColor).let { Color(it) },
                                    palette.getDarkMutedColor(fallbackColor).let { Color(it) }
                                ).distinct()
                            } else {
                                PlayerColorExtractor.extractGradientColors(
                                    palette = palette,
                                    fallbackColor = fallbackColor
                                )
                            }
                            gradientColorsCache[currentMetadata.id] = extractedColors
                            withContext(Dispatchers.Main) { gradientColors = extractedColors }
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val isLightMode = !useDarkTheme || MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val glassButtonColor = if (isLightMode) Color.Black else (if (glassConfig.textColor.isSpecified) glassConfig.textColor else Color.White)

    val TextBackgroundColor by animateColorAsState(
        targetValue = when {
            isLocalMedia -> if (isPlayerGlassActive && isLightMode) Color.Black else Color.White
            (isPlayerGlassActive || playerBackground == PlayerBackgroundStyle.LIQUID_GLASS) && isLightMode -> Color.Black
            playerBackground == PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            else -> Color.White
        },
        label = "TextBackgroundColor"
    )

    val icBackgroundColor by animateColorAsState(
        targetValue = when {
            isLocalMedia -> Color.Black
            (isPlayerGlassActive || playerBackground == PlayerBackgroundStyle.LIQUID_GLASS) && isLightMode -> Color.White
            playerBackground == PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            else -> Color.Black
        },
        label = "icBackgroundColor"
    )

    var canvasArtwork by remember(mediaMetadata?.id) { mutableStateOf<CanvasArtwork?>(null) }
    var canvasFetchInFlight by remember(mediaMetadata?.id) { mutableStateOf(false) }

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground != PlayerBackgroundStyle.APPLE_MUSIC || !enableCanvas) {
            canvasArtwork = null
            return@LaunchedEffect
        }
        val item = mediaMetadata ?: return@LaunchedEffect
        
        
        CanvasArtworkPlaybackCache.get(item.id)?.let { cached ->
            canvasArtwork = cached
            return@LaunchedEffect
        }

        if (canvasFetchInFlight) return@LaunchedEffect
        canvasFetchInFlight = true
        
        withContext(Dispatchers.IO) {
            val storefront = Locale.getDefault().country.lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "us"
            val requestedTitle = item.title
            val requestedArtist = item.artists.joinToString { it.name }
            val requestedAlbum = item.album?.title ?: ""
            
            val s = normalizeCanvasSongTitle(requestedTitle)
            val a = normalizeCanvasArtistName(requestedArtist)
            
            val fetched = TidalCanvasProvider.getBySongArtist(s, a, requestedAlbum)
                ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                ?: AppleMusicCanvasProvider.getBySongArtist(s, a, requestedAlbum, storefront)
                ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }

            val validated = fetched?.let { artwork ->
                val localArtists = splitAndNormalizeArtists(requestedArtist)
                val returnedArtists = splitAndNormalizeArtists(artwork.artist ?: "")
                val artistMatches = localArtists.isNotEmpty() && returnedArtists.isNotEmpty() &&
                    (localArtists.any { local -> returnedArtists.any { it.equals(local, ignoreCase = true) } })
                
                if (artistMatches) artwork else null
            }

            withContext(Dispatchers.Main) {
                canvasArtwork = validated
                if (validated != null) {
                    CanvasArtworkPlaybackCache.put(item.id, validated)
                }
                canvasFetchInFlight = false
            }
        }
    }

    val (textButtonColor, iconButtonColor) = when {
        isPlayerGlassActive || playerBackground == PlayerBackgroundStyle.LIQUID_GLASS -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT ->
                    if (isLightMode) Pair(Color.Black, Color.White)
                    else Pair(Color.White, Color.Black)
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            }
        }
        isLocalMedia ||
        playerBackground == PlayerBackgroundStyle.BLUR || 
        playerBackground == PlayerBackgroundStyle.GRADIENT ||
        playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED ||
        playerBackground == PlayerBackgroundStyle.APPLE_MUSIC ||
        playerBackground == PlayerBackgroundStyle.LIVE_MESH -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(Color.White, Color.Black)
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            }
        }
        else -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT ->
                    if (useDarkTheme) Pair(Color.White, Color.Black)
                    else Pair(Color.Black, Color.White)
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }

    
    val (sideButtonContainerColor, sideButtonContentColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT -> Pair(
            Color.White.copy(alpha = 0.2f),
            Color.White
        )
        PlayerButtonsStyle.PRIMARY -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        PlayerButtonsStyle.TERTIARY -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.sleep_timer),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = {
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
            },
            onCancel = {
                showSleepTimerDialog = false
            },
            onReset = {
                sleepTimerValue = 30f
            },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        }
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            }
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showCommentSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showListenTogetherDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showInlineLyrics by rememberSaveable {
        mutableStateOf(false)
    }

    var isFullScreen by rememberSaveable {
        mutableStateOf(false)
    }

    val hideStatusBarOnFullscreen by rememberPreference(HideStatusBarOnFullscreenKey, defaultValue = false)

    DisposableEffect(isFullScreen, hideStatusBarOnFullscreen) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen && hideStatusBarOnFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    
    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100) 
                if (sliderPosition == null) { 
                    position = playerConnection.player.currentPosition
                    duration = playerConnection.player.duration
                }
            }
        }
    }
    
    
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration
        }
    }
    
    
    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }

    val isModernPlayerDesign = isPlayerGlassActive || useNewPlayerDesign
    val dismissedBound = if (isModernPlayerDesign) 0.dp else (QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
    val collapsedBound = if (isModernPlayerDesign) 0.dp else (dismissedBound + 1.dp)

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = dismissedBound,
        expandedBound = state.expandedBound,
        collapsedBound = collapsedBound,
        initialAnchor = if (isModernPlayerDesign) com.niksoriginals.melo.ui.component.dismissedAnchor else com.niksoriginals.melo.ui.component.collapsedAnchor
    )

    val bottomSheetBackgroundColor = when {
        isLocalMedia -> Color.Black
        isPlayerGlassActive -> Color.Transparent
        playerBackground in listOf(PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC) ->
            MaterialTheme.colorScheme.surfaceContainer
        playerBackground == PlayerBackgroundStyle.LIVE_MESH || playerBackground == PlayerBackgroundStyle.LIQUID_GLASS -> Color.Black
        else ->
            if (useBlackBackground) Color.Black
            else MaterialTheme.colorScheme.surfaceContainer
    }

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            val backgroundThumbnailUrl = mediaMetadata?.thumbnailUrl ?: playerConnection.player.currentMediaItem?.mediaMetadata?.artworkUri?.toString()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomSheetBackgroundColor)
            ) {
                if (isPlayerGlassActive) {
                    if (backgroundThumbnailUrl != null) {
                        Box(modifier = Modifier.fillMaxSize().alpha(backgroundAlpha)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(backgroundThumbnailUrl)
                                    .size(100, 100)
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(100.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(backgroundAlpha)
                            .liquidGlass(
                                config = glassConfig,
                                applyEdgeEffects = false,
                                blurRadiusDp = glassConfig.blurRadius * PLAYER_BLUR_MULTIPLIER,
                            )
                    )
                } else {
                    when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        AnimatedContent(
                            targetState = backgroundThumbnailUrl,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "blurBackground"
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl)
                                            .size(100, 100)
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(if (useDarkTheme) 150.dp else 100.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "gradientBackground"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                val gradientColorStops = if (colors.size >= 3) {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.5f to colors[1],
                                        1.0f to colors[2]
                                    )
                                } else {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.6f to colors[0].copy(alpha = 0.7f),
                                        1.0f to Color.Black
                                    )
                                }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                    PlayerBackgroundStyle.GLOW_ANIMATED -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(1200)) togetherWith fadeOut(tween(1200))
                            },
                            label = "GlowAnimatedContent"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                val infiniteTransition =
                                    rememberInfiniteTransition(label = "GlowAnimation")

                                val progress by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(20000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "glowProgress"
                                )

                                fun rotatedColorAt(index: Int): Color {
                                    val size = colors.size
                                    val idx = index.toFloat() + progress * size
                                    val a = kotlin.math.floor(idx).toInt() % size
                                    val b = (a + 1) % size
                                    val frac = idx - kotlin.math.floor(idx)
                                    return androidx.compose.ui.graphics.lerp(
                                        colors.getOrElse(a) { Color.DarkGray },
                                        colors.getOrElse(b) { Color.DarkGray },
                                        frac
                                    )
                                }

                                fun oscillate(
                                    min: Float,
                                    max: Float,
                                    phase: Float,
                                    speed: Float = 1f
                                ): Float {
                                    val v = kotlin.math.sin(
                                        2f * kotlin.math.PI.toFloat() * (progress * speed + phase)
                                    )
                                    return min + (max - min) * ((v + 1f) * 0.5f)
                                }

                                val color1 = rotatedColorAt(0)
                                val color2 = rotatedColorAt(1)
                                val color3 = rotatedColorAt(2)
                                val color4 = rotatedColorAt(3)
                                val color5 = rotatedColorAt(4)
                                val color6 = rotatedColorAt(5)

                                val o1x = oscillate(0.0f, 1.0f, 0.00f, 1.0f)
                                val o1y = oscillate(0.0f, 0.5f, 0.07f, 1.0f)
                                val r1 = oscillate(0.8f, 1.6f, 0.12f, 1.0f)

                                val o2x = oscillate(1.0f, 0.0f, 0.2f, 1.0f)
                                val o2y = oscillate(0.5f, 1.0f, 0.25f, 1.0f)
                                val r2 = oscillate(0.7f, 1.5f, 0.18f, 1.0f)

                                val o3x = oscillate(0.2f, 0.8f, 0.33f, 1.0f)
                                val o3y = oscillate(0.8f, 0.2f, 0.36f, 1.0f)
                                val r3 = oscillate(0.6f, 1.4f, 0.29f, 1.0f)

                                val o4x = oscillate(0.3f, 0.7f, 0.44f, 1.0f)
                                val o4y = oscillate(0.2f, 0.8f, 0.41f, 1.0f)
                                val r4 = oscillate(0.9f, 1.7f, 0.47f, 1.0f)

                                val o5x = oscillate(0.4f, 0.6f, 0.55f, 1.0f)
                                val o5y = oscillate(0.0f, 1.0f, 0.51f, 1.0f)
                                val r5 = oscillate(0.7f, 1.5f, 0.58f, 1.0f)

                                val o6x = oscillate(0.0f, 1.0f, 0.66f, 1.0f)
                                val o6y = oscillate(0.5f, 0.7f, 0.62f, 1.0f)
                                val r6 = oscillate(0.8f, 1.8f, 0.69f, 1.0f)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .drawWithCache {
                                            val width = size.width
                                            val height = size.height
                                            val baseColor = Color(0xFF050505)

                                            val brush1 = Brush.radialGradient(
                                                colors = listOf(
                                                    color1.copy(alpha = 0.85f),
                                                    color1.copy(alpha = 0.5f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o1x, height * o1y),
                                                radius = width * r1
                                            )
                                            val brush2 = Brush.radialGradient(
                                                colors = listOf(
                                                    color2.copy(alpha = 0.8f),
                                                    color2.copy(alpha = 0.45f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o2x, height * o2y),
                                                radius = width * r2
                                            )
                                            val brush3 = Brush.radialGradient(
                                                colors = listOf(
                                                    color3.copy(alpha = 0.75f),
                                                    color3.copy(alpha = 0.4f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o3x, height * o3y),
                                                radius = width * r3
                                            )
                                            val brush4 = Brush.radialGradient(
                                                colors = listOf(
                                                    color4.copy(alpha = 0.7f),
                                                    color4.copy(alpha = 0.35f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o4x, height * o4y),
                                                radius = width * r4
                                            )
                                            val brush5 = Brush.radialGradient(
                                                colors = listOf(
                                                    color5.copy(alpha = 0.65f),
                                                    color5.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o5x, height * o5y),
                                                radius = width * r5
                                            )
                                            val brush6 = Brush.radialGradient(
                                                colors = listOf(
                                                    color6.copy(alpha = 0.6f),
                                                    color6.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o6x, height * o6y),
                                                radius = width * r6
                                            )

                                            onDrawBehind {
                                                drawRect(color = baseColor)
                                                drawRect(brush = brush1)
                                                drawRect(brush = brush2)
                                                drawRect(brush = brush3)
                                                drawRect(brush = brush4)
                                                drawRect(brush = brush5)
                                                drawRect(brush = brush6)
                                            }
                                        }
                                )
                            }
                        }
                    }
                    PlayerBackgroundStyle.APPLE_MUSIC -> {
                        AnimatedContent(
                            targetState = backgroundThumbnailUrl,
                            transitionSpec = {
                                fadeIn(tween(1200)).togetherWith(fadeOut(tween(1200)))
                            },
                            label = "appleMusicBackground"
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                ) {
                                    
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl)
                                            .size(128, 128) 
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(150.dp)
                                    )

                                    
                                    val clearArtworkAlpha by animateFloatAsState(
                                        targetValue = if (showInlineLyrics) 0f else 1f,
                                        animationSpec = tween(500),
                                        label = "clearArtworkAlpha"
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.65f) 
                                            .alpha(clearArtworkAlpha)
                                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                            .drawWithContent {
                                                drawContent()
                                                
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        colorStops = arrayOf(
                                                            0.00f to Color.Black,
                                                            0.75f to Color.Black,
                                                            0.92f to Color.Black.copy(alpha = 0.4f),
                                                            1.00f to Color.Transparent,
                                                        )
                                                    ),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(thumbnailUrl)
                                                .size(CoilSize.ORIGINAL)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (enableCanvas && canvasArtwork != null && backgroundAlpha > 0.01f) {
                                            BackgroundVideoView(
                                                videoUrl = canvasArtwork?.animated ?: canvasArtwork?.videoUrl ?: "",
                                                isPlaying = isPlaying,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Black.copy(alpha = 0.05f),
                                                        Color.Black.copy(alpha = 0.4f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.LIVE_MESH, PlayerBackgroundStyle.LIQUID_GLASS -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "liveMeshRotation")
                        
                        val anchorRotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = -360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(80000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "anchorRotation"
                        )
                        
                        val fastRotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(40000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "fastRotation"
                        )
                        
                        val slowRotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(60000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "slowRotation"
                        )

                        AnimatedContent(
                            targetState = backgroundThumbnailUrl,
                            transitionSpec = {
                                fadeIn(tween(1500)).togetherWith(fadeOut(tween(1500)))
                            },
                            label = "liveMeshBackground"
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .graphicsLayer {
                                            
                                            scaleX = 1.7f
                                            scaleY = 1.7f
                                        }
                                ) {
                                    val matrix = remember { 
                                        val m = ColorMatrix()
                                        m.setToSaturation(1.8f) 
                                        m
                                    }
                                    val colorFilter = ColorFilter.colorMatrix(matrix)

                                    
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl)
                                            .size(128, 128) 
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = colorFilter,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(100.dp)
                                            .graphicsLayer { rotationZ = anchorRotation }
                                    )

                                    
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl)
                                            .size(128, 128) 
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = colorFilter,
                                        alignment = Alignment.TopStart,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(120.dp)
                                            .graphicsLayer { 
                                                rotationZ = fastRotation
                                                alpha = 0.6f
                                            }
                                    )

                                    
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl)
                                            .size(128, 128) 
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = colorFilter,
                                        alignment = Alignment.BottomEnd,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(120.dp)
                                            .graphicsLayer { 
                                                rotationZ = slowRotation
                                                alpha = 0.5f
                                            }
                                    )
                                    
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.2f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.25f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.DEFAULT -> {
                        
                    }
                }
            }
        }
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                positionState = positionState,
                durationState = durationState,
                onClick = { state.expandSoft() }
            )
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness",
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                AnimatedContent(
                    targetState = showInlineLyrics,
                    label = "ThumbnailAnimation"
                ) { showLyrics ->
                    if (showLyrics) {
                        Row {
                            if (hidePlayerThumbnail) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_launcher_nobg),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp),
                                        tint = textButtonColor.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .clickable(enabled = isFullScreen && enableLyricsThumbnailPlayPause) {
                                            playerConnection.togglePlayPause()
                                        }
                                ) {
                                    AsyncImage(
                                        model = mediaMetadata.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (isFullScreen && enableLyricsThumbnailPlayPause) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = if (isPlaying) 0f else 0.4f))
                                        )

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = !isPlaying,
                                            enter = fadeIn(),
                                            exit = fadeOut()
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    if (playbackState == Player.STATE_ENDED) R.drawable.replay
                                                    else R.drawable.play
                                                ),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .SwipeGesture(
                            enabled = isFullScreen && swipeLyrics,
                            onSwipeRight = { playerConnection.seekToPrevious() },
                            onSwipeLeft = { playerConnection.seekToNext() }
                        )
                ) {
                    AnimatedContent(
                        targetState = mediaMetadata.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextBackgroundColor,
                            modifier =
                            Modifier
                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                .combinedClickable(
                                    enabled = true,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        if (mediaMetadata.album != null) {
                                            navController.navigate("album/${mediaMetadata.album.id}")
                                            state.collapseSoft()
                                        }
                                    },
                                    onLongClick = {
                                        val clip = ClipData.newPlainText(context.getString(R.string.copied_title), title)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast
                                            .makeText(context, context.getString(R.string.copied_title), Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            ,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    if (isCasting && castDeviceName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.cast_connected),
                                contentDescription = null,
                                tint = TextBackgroundColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Casting to $castDeviceName",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextBackgroundColor.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (mediaMetadata.explicit) MIcon.Explicit()

                        if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                            val annotatedString = buildAnnotatedString {
                                mediaMetadata.artists.forEachIndexed { index, artist ->
                                    val tag = "artist_${artist.id.orEmpty()}"
                                    pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                    withStyle(SpanStyle(color = TextBackgroundColor, fontSize = 16.sp)) {
                                        append(artist.name)
                                    }
                                    pop()
                                    if (index != mediaMetadata.artists.lastIndex) append(", ")
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                    .padding(end = 12.dp)
                            ) {
                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                var clickOffset by remember { mutableStateOf<Offset?>(null) }
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { layoutResult = it },
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val tapPosition = event.changes.firstOrNull()?.position
                                                    if (tapPosition != null) {
                                                        clickOffset = tapPosition
                                                    }
                                                }
                                            }
                                        }
                                        .combinedClickable(
                                            enabled = true,
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {
                                                val tapPosition = clickOffset
                                                val layout = layoutResult
                                                if (tapPosition != null && layout != null) {
                                                    val offset = layout.getOffsetForPosition(tapPosition)
                                                    annotatedString
                                                        .getStringAnnotations(offset, offset)
                                                        .firstOrNull()
                                                        ?.let { ann ->
                                                            val artistId = ann.item
                                                            if (artistId.isNotBlank()) {
                                                                navController.navigate("artist/$artistId")
                                                                state.collapseSoft()
                                                            }
                                                        }
                                                }
                                            },
                                            onLongClick = {
                                                val clip =
                                                    ClipData.newPlainText(
                                                        context.getString(R.string.copied_artist),
                                                        annotatedString
                                                    )
                                                clipboardManager.setPrimaryClip(clip)
                                                Toast
                                                    .makeText(
                                                        context,
                                                        context.getString(R.string.copied_artist),
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (isPlayerGlassActive) {
                    val adaptiveActionTint = glassButtonColor
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(targetState = showInlineLyrics, label = "GlassLyricsButtons") { showLyrics ->
                            if (showLyrics) {
                                val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Fullscreen Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .liquidGlass(
                                                config = glassConfig,
                                                shape = CircleShape,
                                                applyEdgeEffects = true,
                                            )
                                            .clickable { isFullScreen = !isFullScreen },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.fullscreen),
                                            contentDescription = "Fullscreen Lyrics",
                                            tint = adaptiveActionTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Lyrics Menu / Offset Dialog Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .liquidGlass(
                                                config = glassConfig,
                                                shape = CircleShape,
                                                applyEdgeEffects = true,
                                            )
                                            .clickable {
                                                menuState.show {
                                                    com.niksoriginals.melo.ui.menu.LyricsMenu(
                                                        lyricsProvider = { currentLyrics },
                                                        songProvider = { currentSong?.song },
                                                        mediaMetadataProvider = { mediaMetadata },
                                                        onDismiss = menuState::dismiss,
                                                        onShowOffsetDialog = {
                                                            bottomSheetPageState.show {
                                                                ShowOffsetDialog(
                                                                    songProvider = { currentSong?.song }
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_horiz),
                                            contentDescription = "Lyrics Options",
                                            tint = adaptiveActionTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Lyrics Toggle Button (active, click to close)
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .liquidGlass(
                                                config = glassConfig,
                                                shape = CircleShape,
                                                applyEdgeEffects = true,
                                            )
                                            .background(adaptiveActionTint.copy(alpha = 0.22f))
                                            .clickable { showInlineLyrics = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.lyrics),
                                            contentDescription = stringResource(R.string.lyrics),
                                            tint = adaptiveActionTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Lyrics Toggle Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .liquidGlass(
                                                config = glassConfig,
                                                shape = CircleShape,
                                                applyEdgeEffects = true,
                                            )
                                            .clickable { showInlineLyrics = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.lyrics),
                                            contentDescription = stringResource(R.string.lyrics),
                                            tint = adaptiveActionTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Like Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .liquidGlass(
                                                config = glassConfig,
                                                shape = CircleShape,
                                                applyEdgeEffects = true,
                                            )
                                            .clickable(onClick = playerConnection::toggleLike),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (currentSong?.song?.liked == true)
                                                    R.drawable.favorite
                                                else R.drawable.favorite_border
                                            ),
                                            contentDescription = null,
                                            tint = if (currentSong?.song?.liked == true) Color(0xFFFF4081) else adaptiveActionTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (useNewPlayerDesign) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(targetState = showInlineLyrics, label = "NewPlayerLyricsButtons") { showLyrics ->
                            if (showLyrics) {
                                val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Fullscreen Button
                                    FilledIconButton(
                                        onClick = { isFullScreen = !isFullScreen },
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.fullscreen),
                                            contentDescription = "Fullscreen Lyrics",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Lyrics Menu / Offset Dialog Button
                                    FilledIconButton(
                                        onClick = {
                                            menuState.show {
                                                com.niksoriginals.melo.ui.menu.LyricsMenu(
                                                    lyricsProvider = { currentLyrics },
                                                    songProvider = { currentSong?.song },
                                                    mediaMetadataProvider = { mediaMetadata },
                                                    onDismiss = menuState::dismiss,
                                                    onShowOffsetDialog = {
                                                        bottomSheetPageState.show {
                                                            ShowOffsetDialog(
                                                                songProvider = { currentSong?.song }
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        },
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_horiz),
                                            contentDescription = "Lyrics Options",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Lyrics Toggle Button (active, click to close)
                                    FilledIconButton(
                                        onClick = { showInlineLyrics = false },
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.lyrics),
                                            contentDescription = stringResource(R.string.lyrics),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Lyrics Toggle Button
                                    FilledIconButton(
                                        onClick = { showInlineLyrics = true },
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.lyrics),
                                            contentDescription = stringResource(R.string.lyrics),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Like Button
                                    val isLiked = currentSong?.song?.liked == true
                                    FilledIconButton(
                                        onClick = playerConnection::toggleLike,
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = if (isLiked) Color(0xFFFF4081) else iconButtonColor,
                                        ),
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    AnimatedContent(targetState = showInlineLyrics, label = "DownloadButton") { showLyrics ->
                        if (showLyrics) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(textButtonColor.copy(alpha = 0.2f))
                                    .clickable { isFullScreen = !isFullScreen },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.fullscreen),
                                    contentDescription = null,
                                    tint = textButtonColor,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(24.dp),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(textButtonColor.copy(alpha = 0.2f))
                                    .clickable {
                                        menuState.show {
                                            OldPlayerMenu(
                                                mediaMetadata = mediaMetadata,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                onShowDetailsDialog = {
                                                    mediaMetadata.id.let {
                                                        bottomSheetPageState.show {
                                                           ShowMediaInfo(it)
                                                        }
                                                    }
                                                },
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                    tint = textButtonColor,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                        if (showLyrics) {
                            val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(textButtonColor.copy(alpha = 0.2f))
                                    .clickable {
                                        menuState.show {
                                            com.niksoriginals.melo.ui.menu.LyricsMenu(
                                                lyricsProvider = { currentLyrics },
                                                songProvider = { currentSong?.song },
                                                mediaMetadataProvider = { mediaMetadata },
                                                onDismiss = menuState::dismiss,
                                                onShowOffsetDialog = {
                                                    bottomSheetPageState.show {
                                                        ShowOffsetDialog(
                                                            songProvider = { currentSong?.song }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_horiz),
                                    contentDescription = null,
                                    tint = textButtonColor,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(24.dp),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(textButtonColor.copy(alpha = 0.2f))
                                    .clickable(onClick = playerConnection::toggleLike),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (currentSong?.song?.liked == true)
                                            R.drawable.favorite
                                        else R.drawable.favorite_border
                                    ),
                                    contentDescription = null,
                                    tint = textButtonColor,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(24.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (useNewPlayerDesign) 24.dp else 20.dp))

            when (sliderStyle) {
                SliderStyle.DEFAULT -> {
                    val trackInteractionSource = remember { MutableInteractionSource() }
                    val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
                    val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                    val isTrackActive = isTrackDragged || isTrackPressed

                    val trackHeight by animateDpAsState(
                        targetValue = if (isTrackActive) 14.dp else 8.dp,
                        animationSpec = spring(
                            dampingRatio = 0.76f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "trackHeightDefault"
                    )

                    Slider(
                        value = (sliderPosition ?: effectivePosition).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            if (!isListenTogetherGuest) {
                                sliderPosition = it.toLong()
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherGuest) {
                                sliderPosition?.let {
                                    if (isCasting) {
                                        castHandler?.seekTo(it)
                                        lastManualSeekTime = System.currentTimeMillis()
                                    } else {
                                        playerConnection.player.seekTo(it)
                                    }
                                    position = it
                                }
                                sliderPosition = null
                            }
                        },
                        enabled = !isListenTogetherGuest,
                        interactionSource = trackInteractionSource,
                        thumb = {
                            if (isPlayerGlassActive || useNewPlayerDesign) {
                                LiquidGlassThumb(interactionSource = trackInteractionSource)
                            } else {
                                SliderDefaults.Thumb(interactionSource = trackInteractionSource)
                            }
                        },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                trackHeight = trackHeight,
                                isGlass = isPlayerGlassActive || useNewPlayerDesign,
                                colors = PlayerSliderColors.getSliderColors(
                                    activeColor = if (useNewPlayerDesign) textButtonColor else textButtonColor.copy(alpha = 0.7f),
                                    playerBackground = playerBackground,
                                    useDarkTheme = useDarkTheme
                                )
                            )
                        },
                        colors = PlayerSliderColors.getSliderColors(
                            activeColor = if (useNewPlayerDesign) textButtonColor else textButtonColor.copy(alpha = 0.7f),
                            playerBackground = playerBackground,
                            useDarkTheme = useDarkTheme
                        ),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }

                SliderStyle.WAVY -> {
                    if (squigglySlider) {
                        SquigglySlider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    if (isCasting) {
                                        castHandler?.seekTo(it)
                                        lastManualSeekTime = System.currentTimeMillis()
                                    } else {
                                        playerConnection.player.seekTo(it)
                                    }
                                    position = it
                                }
                                sliderPosition = null
                            },
                            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                            colors = PlayerSliderColors.getSliderColors(
                                activeColor = if (useNewPlayerDesign) textButtonColor else textButtonColor.copy(alpha = 0.7f),
                                playerBackground = playerBackground,
                                useDarkTheme = useDarkTheme
                            ),
                            isPlaying = effectiveIsPlaying,
                        )
                    } else {
                        WavySlider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    if (isCasting) {
                                        castHandler?.seekTo(it)
                                        lastManualSeekTime = System.currentTimeMillis()
                                    } else {
                                        playerConnection.player.seekTo(it)
                                    }
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = PlayerSliderColors.getSliderColors(
                                activeColor = if (useNewPlayerDesign) textButtonColor else textButtonColor.copy(alpha = 0.7f),
                                playerBackground = playerBackground,
                                useDarkTheme = useDarkTheme
                            ),
                            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                            isPlaying = effectiveIsPlaying
                        )
                    }
                }

                SliderStyle.SLIM -> {
                    val trackInteractionSource = remember { MutableInteractionSource() }
                    val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
                    val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                    val isTrackActive = isTrackDragged || isTrackPressed

                    val trackHeight by animateDpAsState(
                        targetValue = if (isTrackActive) 14.dp else 8.dp,
                        animationSpec = spring(
                            dampingRatio = 0.76f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "trackHeight"
                    )

                    Slider(
                        value = (sliderPosition ?: effectivePosition).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            if (!isListenTogetherGuest) {
                                sliderPosition = it.toLong()
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherGuest) {
                                sliderPosition?.let {
                                    if (isCasting) {
                                        castHandler?.seekTo(it)
                                        lastManualSeekTime = System.currentTimeMillis()
                                    } else {
                                        playerConnection.player.seekTo(it)
                                    }
                                    position = it
                                }
                                sliderPosition = null
                            }
                        },
                        enabled = !isListenTogetherGuest,
                        interactionSource = trackInteractionSource,
                        thumb = {
                            if (isPlayerGlassActive || useNewPlayerDesign) {
                                LiquidGlassThumb(interactionSource = trackInteractionSource)
                            } else {
                                Spacer(modifier = Modifier.size(0.dp))
                            }
                        },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                trackHeight = trackHeight,
                                isGlass = isPlayerGlassActive || useNewPlayerDesign,
                                colors = PlayerSliderColors.getSliderColors(
                                    activeColor = if (useNewPlayerDesign) textButtonColor else textButtonColor.copy(alpha = 0.7f),
                                    playerBackground = playerBackground,
                                    useDarkTheme = useDarkTheme
                                )
                            )
                        },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp),
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: effectivePosition),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    val formatText = remember(currentAudioFormat, currentFormatEntity) {
                        val localAudioFormat = currentAudioFormat
                        val localFormatEntity = currentFormatEntity
                        val codecStr = localAudioFormat?.sampleMimeType?.substringAfter("audio/")?.uppercase() ?: localFormatEntity?.codecs?.uppercase() ?: ""
                        var bitrateStr = ""
                        if (localFormatEntity?.bitrate != null && localFormatEntity.bitrate > 0) {
                            bitrateStr = "${localFormatEntity.bitrate / 1000} kbps"
                        } else if (localAudioFormat?.bitrate != null && localAudioFormat.bitrate > 0) {
                            bitrateStr = "${localAudioFormat.bitrate / 1000} kbps"
                        }
                        val isLossless = codecStr.contains("FLAC") || codecStr.contains("ALAC") || codecStr.contains("WAV")
                        val losslessStr = if (isLossless) "Lossless" else ""
                        listOf(codecStr, bitrateStr, losslessStr).filter { it.isNotEmpty() }.joinToString(" • ")
                    }

                    val isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING

                    // Beat-synced automix countdown: beats left until the planned mix point,
                    // ticking with playback position and pulsing at the track's tempo.
                    val mixBeatMs = automixDebug?.outBpm?.takeIf { it > 0f }?.let { 60_000f / it } ?: 500f
                    val mixBeatsLeft = automixDebug?.triggerTimeMs?.let {
                        kotlin.math.ceil((it - (sliderPosition ?: effectivePosition)) / mixBeatMs).toInt()
                    }
                    val mixCountdownActive = !isCrossfading && mixBeatsLeft != null && mixBeatsLeft in 1..16

                    val shouldShowCodecBox = (!isPlayerGlassActive && showCodecOnPlayer && formatText.isNotEmpty()) || isBuffering || isCrossfading || mixCountdownActive
                    if (sleepTimerEnabled || shouldShowCodecBox) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TextBackgroundColor.copy(alpha = 0.08f))
                                .border(
                                    width = 0.5.dp,
                                    color = TextBackgroundColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable(enabled = sleepTimerEnabled) {
                                    showSleepTimerDialog = true
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val codecBoxState = when {
                                sleepTimerEnabled -> 0
                                isCrossfading -> 1
                                mixCountdownActive -> 4
                                isBuffering -> 2
                                else -> 3
                            }
                            AnimatedContent(
                                targetState = codecBoxState,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                },
                                label = "QualityTimerSwitcher"
                            ) { state ->
                                when (state) {
                                    0 -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.sleep_timer),
                                                contentDescription = null,
                                                tint = TextBackgroundColor.copy(alpha = 0.8f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = makeTimeString(sleepTimerTimeLeft.coerceAtLeast(0)),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.5.sp
                                                ),
                                                color = TextBackgroundColor.copy(alpha = 0.8f),
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                    1 -> {
                                        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "ShiningCrossfade")
                                        val alpha by infiniteTransition.animateFloat(
                                            initialValue = 0.3f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(800, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "CrossfadeAlpha"
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(if (isAutomixing) R.drawable.graphic_eq else R.drawable.sync),
                                                contentDescription = if (isAutomixing) "Automixing" else "Crossfading",
                                                tint = TextBackgroundColor.copy(alpha = alpha),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = stringResource(if (isAutomixing) R.string.automixing else R.string.crossfading),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = TextBackgroundColor.copy(alpha = alpha),
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                    2 -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.size(10.dp),
                                                color = TextBackgroundColor.copy(alpha = 0.8f),
                                                strokeWidth = 1.5.dp
                                            )
                                            Text(
                                                text = stringResource(R.string.loading),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = TextBackgroundColor.copy(alpha = 0.8f),
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                    3 -> {
                                        Text(
                                            text = formatText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                fontSize = 10.sp
                                            ),
                                            color = TextBackgroundColor.copy(alpha = 0.8f),
                                            maxLines = 1,
                                        )
                                    }
                                    4 -> {
                                        val beatTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "MixCountdownBeat")
                                        val beatAlpha by beatTransition.animateFloat(
                                            initialValue = 1f,
                                            targetValue = 0.35f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(mixBeatMs.toInt().coerceIn(200, 1000), easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "MixCountdownAlpha"
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.graphic_eq),
                                                contentDescription = null,
                                                tint = TextBackgroundColor.copy(alpha = beatAlpha),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.automix_mix_in, mixBeatsLeft ?: 0),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.5.sp
                                                ),
                                                color = TextBackgroundColor.copy(alpha = beatAlpha),
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            automixDebugOverlay()

            Spacer(Modifier.height(if (isPlayerGlassActive) 20.dp else if (useNewPlayerDesign) 24.dp else 12.dp))

            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Column {
                    if (isPlayerGlassActive) {
                        val adaptiveControlColor = glassButtonColor
                        val backInteractionSource = remember { MutableInteractionSource() }
                        val nextInteractionSource = remember { MutableInteractionSource() }
                        val playPauseInteractionSource = remember { MutableInteractionSource() }

                        val isBackPressed by backInteractionSource.collectIsPressedAsState()
                        val isNextPressed by nextInteractionSource.collectIsPressedAsState()
                        val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()

                        val backButtonScale by animateFloatAsState(
                            targetValue = if (isBackPressed) 0.88f else 1f,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                            label = "glassBackScale"
                        )
                        val nextButtonScale by animateFloatAsState(
                            targetValue = if (isNextPressed) 0.88f else 1f,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                            label = "glassNextScale"
                        )
                        val playPauseScale by animateFloatAsState(
                            targetValue = if (isPlayPausePressed) 0.90f else 1f,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                            label = "glassPlayPauseScale"
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                                .widthIn(max = 340.dp)
                                .padding(horizontal = 12.dp)
                                .height(MiniPlayerHeight)
                                .liquidGlass(
                                    config = glassConfig,
                                    shape = RoundedCornerShape(percent = 50),
                                    applyEdgeEffects = true,
                                    blurRadiusDp = glassConfig.blurRadius * 1.5f,
                                )
                                .padding(horizontal = 8.dp),
                        ) {
                            // 1. Shuffle Button (left of Previous)
                            val shuffleModeEnabledInside by playerConnection.shuffleModeEnabled.collectAsState()
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = !isListenTogetherGuest,
                                        onClick = {
                                            playerConnection.player.shuffleModeEnabled = !shuffleModeEnabledInside
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = stringResource(R.string.shuffle),
                                    tint = if (shuffleModeEnabledInside) adaptiveControlColor else adaptiveControlColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // 2. Previous Track
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .graphicsLayer {
                                        scaleX = backButtonScale
                                        scaleY = backButtonScale
                                    }
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = backInteractionSource,
                                        indication = null,
                                        enabled = canSkipPrevious && !isListenTogetherGuest,
                                        onClick = playerConnection::seekToPrevious
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = null,
                                    tint = if (canSkipPrevious && !isListenTogetherGuest) adaptiveControlColor else adaptiveControlColor.copy(alpha = 0.35f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 3. Play / Pause Hero Button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .graphicsLayer {
                                        scaleX = playPauseScale
                                        scaleY = playPauseScale
                                    }
                                    .clip(CircleShape)
                                    .background(adaptiveControlColor.copy(alpha = 0.18f))
                                    .border(
                                        width = 1.dp,
                                        color = adaptiveControlColor.copy(alpha = 0.32f),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        interactionSource = playPauseInteractionSource,
                                        indication = null
                                    ) {
                                        if (isListenTogetherGuest) {
                                            playerConnection.toggleMute()
                                            return@clickable
                                        }
                                        if (isCasting) {
                                            if (castIsPlaying) {
                                                castHandler?.pause()
                                            } else {
                                                castHandler?.play()
                                            }
                                        } else if (playbackState == STATE_ENDED) {
                                            playerConnection.player.seekTo(0, 0)
                                            playerConnection.player.playWhenReady = true
                                        } else {
                                            playerConnection.togglePlayPause()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isListenTogetherGuest) {
                                            if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                        } else {
                                            if (effectiveIsPlaying) R.drawable.pause else R.drawable.play
                                        }
                                    ),
                                    contentDescription = if (isListenTogetherGuest) {
                                        if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                                    } else {
                                        if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                                    },
                                    tint = adaptiveControlColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // 4. Next Track
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .graphicsLayer {
                                        scaleX = nextButtonScale
                                        scaleY = nextButtonScale
                                    }
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = nextInteractionSource,
                                        indication = null,
                                        enabled = canSkipNext && !isListenTogetherGuest,
                                        onClick = playerConnection::seekToNext
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = null,
                                    tint = if (canSkipNext && !isListenTogetherGuest) adaptiveControlColor else adaptiveControlColor.copy(alpha = 0.35f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 5. Repeat Button (right of Next)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = !isListenTogetherGuest,
                                        onClick = {
                                            playerConnection.player.toggleRepeatMode()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                            else -> R.drawable.repeat
                                        }
                                    ),
                                    contentDescription = stringResource(R.string.repeat),
                                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) adaptiveControlColor else adaptiveControlColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Up Next preview floating glass pill
                        val nextMediaItem = remember(mediaMetadata, canSkipNext) {
                            runCatching {
                                val player = playerConnection.player
                                val nextIdx = player.nextMediaItemIndex
                                if (nextIdx != -1 && nextIdx < player.mediaItemCount) {
                                    player.getMediaItemAt(nextIdx)
                                } else null
                            }.getOrNull()
                        }
                        val nextTitle = nextMediaItem?.mediaMetadata?.title?.toString()

                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(horizontal = 32.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .liquidGlass(
                                    config = glassConfig,
                                    shape = RoundedCornerShape(percent = 50),
                                    applyEdgeEffects = true,
                                    blurRadiusDp = glassConfig.blurRadius * 1.2f,
                                )
                                .clickable { queueSheetState.expandSoft() }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = stringResource(R.string.queue),
                                tint = adaptiveControlColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (!nextTitle.isNullOrBlank()) "Up Next: $nextTitle" else stringResource(R.string.queue),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = adaptiveControlColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        val bottomActionTint = glassButtonColor

                        Spacer(Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            // Left side: Sleep Timer with timer icon (not bedtime/moon)
                            IconButton(
                                onClick = {
                                    if (sleepTimerEnabled) {
                                        playerConnection.service.sleepTimer.clear()
                                    } else {
                                        showSleepTimerDialog = true
                                    }
                                },
                                enabled = !isListenTogetherGuest,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.timer),
                                    contentDescription = stringResource(R.string.sleep_timer),
                                    tint = if (sleepTimerEnabled) bottomActionTint else bottomActionTint.copy(alpha = 0.65f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Right side: Comments and Listen Together (Comments on the left of Listen Together)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { showCommentSheet = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.chat_msg),
                                        contentDescription = stringResource(R.string.comments),
                                        tint = if (showCommentSheet) bottomActionTint else bottomActionTint.copy(alpha = 0.65f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showListenTogetherDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.group),
                                        contentDescription = stringResource(R.string.listen_together),
                                        tint = if (showListenTogetherDialog) bottomActionTint else bottomActionTint.copy(alpha = 0.65f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else if (useNewPlayerDesign) {
                        // Controls Island: Slim capsule layout with MiniPlayerHeight and matched width (Melo default non-glass)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                                .widthIn(max = 340.dp)
                                .padding(horizontal = 12.dp)
                                .height(MiniPlayerHeight)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xFF1E1E22))
                                .padding(horizontal = 8.dp)
                        ) {
                            val defaultIconColor = Color.White

                            // 1. Shuffle Button (left of Previous)
                            val shuffleModeEnabledInside by playerConnection.shuffleModeEnabled.collectAsState()
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = !isListenTogetherGuest,
                                        onClick = {
                                            playerConnection.player.shuffleModeEnabled = !shuffleModeEnabledInside
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = stringResource(R.string.shuffle),
                                    tint = if (shuffleModeEnabledInside) defaultIconColor else defaultIconColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // 2. Previous Button
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = canSkipPrevious && !isListenTogetherGuest,
                                        onClick = playerConnection::seekToPrevious
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = null,
                                    tint = if (canSkipPrevious && !isListenTogetherGuest) defaultIconColor else defaultIconColor.copy(alpha = 0.35f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 3. Play / Pause Hero Button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable {
                                        if (isListenTogetherGuest) {
                                            playerConnection.toggleMute()
                                            return@clickable
                                        }
                                        if (isCasting) {
                                            if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                                        } else if (playbackState == STATE_ENDED) {
                                            playerConnection.player.seekTo(0, 0)
                                            playerConnection.player.playWhenReady = true
                                        } else {
                                            playerConnection.togglePlayPause()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isListenTogetherGuest) {
                                            if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                        } else {
                                            if (effectiveIsPlaying) R.drawable.pause else R.drawable.play
                                        }
                                    ),
                                    contentDescription = if (isListenTogetherGuest) {
                                        if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                                    } else {
                                        if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                                    },
                                    tint = defaultIconColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // 4. Next Button
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = canSkipNext && !isListenTogetherGuest,
                                        onClick = playerConnection::seekToNext
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = null,
                                    tint = if (canSkipNext && !isListenTogetherGuest) defaultIconColor else defaultIconColor.copy(alpha = 0.35f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 5. Repeat Button (right of Next)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        enabled = !isListenTogetherGuest,
                                        onClick = {
                                            playerConnection.player.toggleRepeatMode()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                            else -> R.drawable.repeat
                                        }
                                    ),
                                    contentDescription = stringResource(R.string.repeat),
                                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) defaultIconColor else defaultIconColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Up Next preview floating pill (Melo default non-glass)
                        val nextMediaItem = remember(mediaMetadata, canSkipNext) {
                            runCatching {
                                val player = playerConnection.player
                                val nextIdx = player.nextMediaItemIndex
                                if (nextIdx != -1 && nextIdx < player.mediaItemCount) {
                                    player.getMediaItemAt(nextIdx)
                                } else null
                            }.getOrNull()
                        }
                        val nextTitle = nextMediaItem?.mediaMetadata?.title?.toString()

                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(horizontal = 32.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xFF1E1E22))
                                .clickable { queueSheetState.expandSoft() }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = stringResource(R.string.queue),
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (!nextTitle.isNullOrBlank()) "Up Next: $nextTitle" else stringResource(R.string.queue),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        val bottomActionTint = Color.White

                        Spacer(Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            // Left side: Sleep Timer with timer icon (not bedtime/moon)
                            IconButton(
                                onClick = {
                                    if (sleepTimerEnabled) {
                                        playerConnection.service.sleepTimer.clear()
                                    } else {
                                        showSleepTimerDialog = true
                                    }
                                },
                                enabled = !isListenTogetherGuest,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.timer),
                                    contentDescription = stringResource(R.string.sleep_timer),
                                    tint = if (sleepTimerEnabled) bottomActionTint else bottomActionTint.copy(alpha = 0.65f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Right side: Comments and Listen Together (Comments on the left of Listen Together)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { showCommentSheet = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.chat_msg),
                                        contentDescription = stringResource(R.string.comments),
                                        tint = if (showCommentSheet) bottomActionTint else bottomActionTint.copy(alpha = 0.65f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showListenTogetherDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.group),
                                        contentDescription = stringResource(R.string.listen_together),
                                        tint = if (showListenTogetherDialog) bottomActionTint else bottomActionTint.copy(alpha = 0.65f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PlayerHorizontalPadding),
                        ) {


                            Box(modifier = Modifier.weight(1f)) {
                                ResizableIconButton(
                                    icon = R.drawable.apple_skip_previous,
                                    enabled = canSkipPrevious && !isListenTogetherGuest,
                                    color = TextBackgroundColor,
                                    modifier =
                                Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center)
                                    .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    onClick = playerConnection::seekToPrevious,
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier =
                                Modifier
                                    .size(100.dp) 
                                    .clip(RoundedCornerShape(playPauseRoundness))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (isListenTogetherGuest) {
                                            playerConnection.toggleMute()
                                            return@clickable
                                        }
                                        if (isCasting) {
                                            if (castIsPlaying) {
                                                castHandler?.pause()
                                            } else {
                                                castHandler?.play()
                                            }
                                        } else if (playbackState == STATE_ENDED) {
                                            playerConnection.player.seekTo(0, 0)
                                            playerConnection.player.playWhenReady = true
                                        } else {
                                            playerConnection.player.togglePlayPause()
                                        }
                                    },
                            ) {
                                Image(
                                    painter =
                                    painterResource(
                                        if (isListenTogetherGuest) {
                                            if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                        } else if (playbackState ==
                                            STATE_ENDED
                                        ) {
                                            R.drawable.replay
                                        } else if (effectiveIsPlaying) {
                                            R.drawable.pause_applemusic
                                        } else {
                                            R.drawable.play_applemusic
                                        },
                                    ),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(TextBackgroundColor),
                                    modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .size(72.dp),
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                ResizableIconButton(
                                    icon = R.drawable.apple_skip_next,
                                    enabled = canSkipNext && !isListenTogetherGuest,
                                    color = TextBackgroundColor,
                                    modifier =
                                Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center)
                                    .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    onClick = playerConnection::seekToNext,
                                )
                            }


                        }

                        if (!hidePlayerSlider) {
                            Spacer(modifier = Modifier.height(8.dp)) 

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PlayerHorizontalPadding)
                            ) {
                                val volumeInteractionSource = remember { MutableInteractionSource() }
                                val isVolumeDragged by volumeInteractionSource.collectIsDraggedAsState()
                                val isVolumePressed by volumeInteractionSource.collectIsPressedAsState()
                                val isVolumeActive = isVolumeDragged || isVolumePressed

                                
                                var dragVolume by remember { mutableFloatStateOf(systemVolume) }
                                
                                
                                val scope = rememberCoroutineScope()
                                
                                LaunchedEffect(systemVolume) {
                                    if (!isVolumeActive) dragVolume = systemVolume
                                }

                                
                                val animatedSystemVolume by animateFloatAsState(
                                    targetValue = systemVolume,
                                    animationSpec = tween(150, easing = LinearOutSlowInEasing),
                                    label = "animatedSystemVolume"
                                )
                                
                                val volume = if (isCasting) castVolume else {
                                    if (isVolumeActive) dragVolume else animatedSystemVolume
                                }
                                
                                val volumeTrackHeight by animateDpAsState(
                                    targetValue = if (isVolumeActive) 16.dp else 10.dp,
                                    animationSpec = spring(
                                        dampingRatio = 0.7f, 
                                        stiffness = 600f 
                                    ),
                                    label = "volumeTrackHeight"
                                )

                                val volumeIconScale by animateFloatAsState(
                                    targetValue = if (isVolumeActive) 1.15f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = 0.7f,
                                        stiffness = 600f
                                    ),
                                    label = "volumeIconScale"
                                )

                                Icon(
                                    painter = painterResource(R.drawable.volume_mute),
                                    contentDescription = null,
                                    tint = textButtonColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(scaleX = volumeIconScale, scaleY = volumeIconScale)
                                )

                                Spacer(Modifier.width(12.dp))

                                Slider(
                                    value = volume,
                                    onValueChange = { newVolume ->
                                        dragVolume = newVolume
                                        if (isCasting) {
                                            castHandler?.setVolume(newVolume)
                                        } else {
                                            scope.launch(Dispatchers.Default) {
                                                val newStep = (newVolume * maxSystemVolume).roundToInt()
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newStep, 0)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    interactionSource = volumeInteractionSource,
                                    thumb = {},
                                    track = { sliderState ->
                                        PlayerSliderTrack(
                                            sliderState = sliderState,
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = textButtonColor.copy(alpha = 0.7f),
                                                inactiveTrackColor = textButtonColor.copy(alpha = 0.15f)
                                            ),
                                            trackHeight = volumeTrackHeight
                                        )
                                    }
                                )

                                Spacer(Modifier.width(12.dp))

                                Icon(
                                    painter = painterResource(R.drawable.volume_up),
                                    contentDescription = null,
                                    tint = textButtonColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(scaleX = volumeIconScale, scaleY = volumeIconScale)
                                )
                            }
                        }

                        val displayBluetoothName = remember(bluetoothDeviceName) {
                            if (bluetoothDeviceName != null) bluetoothDeviceName else bluetoothDeviceName
                        }
                        
                        var lastNonNullName by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(bluetoothDeviceName) {
                            if (bluetoothDeviceName != null) lastNonNullName = bluetoothDeviceName
                        }

                        AnimatedVisibility(
                            visible = !useNewPlayerDesign && bluetoothDeviceName != null,
                            enter = fadeIn(tween(400)) + expandVertically(tween(400)),
                            exit = fadeOut(tween(400)) + shrinkVertically(tween(400)),
                            label = "BluetoothInfoVisibility"
                        ) {
                            val nameToShow = bluetoothDeviceName ?: lastNonNullName
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            when {
                                                isSpeaker(nameToShow) -> R.drawable.speaker_applemusic
                                                isBuds(nameToShow) -> R.drawable.apple_airpods
                                                else -> R.drawable.apple_headset
                                            }
                                        ),
                                        contentDescription = null,
                                        tint = textButtonColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(
                                            when {
                                                isSpeaker(nameToShow) -> 18.dp
                                                isBuds(nameToShow) -> 20.dp
                                                else -> 16.dp
                                            }
                                        )
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = nameToShow ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textButtonColor.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                
                val density = LocalDensity.current
                val verticalPadding = max(
                    WindowInsets.systemBars.getTop(density),
                    WindowInsets.systemBars.getBottom(density)
                )
                val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                val verticalWindowInsets = WindowInsets(left = 0.dp, top = verticalPaddingDp, right = 0.dp, bottom = verticalPaddingDp)
                
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(verticalWindowInsets)
                        )
                        .padding(bottom = 24.dp)
                        .fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(state.preUpPostDownNestedScrollConnection)
                    ) {
                        val currentSliderPosition by rememberUpdatedState(sliderPosition)
                        val sliderPositionProvider = remember { { currentSliderPosition } }
                        val isExpandedProvider = remember(state) { { state.isExpanded } }
                        AnimatedContent(
                            targetState = showInlineLyrics,
                            label = "Lyrics",
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { showLyrics ->
                            if (showLyrics) {
                                InlineLyricsView(
                                    mediaMetadata = mediaMetadata,
                                    showLyrics = showLyrics,
                                    positionProvider = { effectivePosition }
                                )
                            } else {
                                Thumbnail(
                                    sliderPositionProvider = sliderPositionProvider,
                                    modifier = Modifier.animateContentSize(),
                                    isPlayerExpanded = isExpandedProvider,
                                    isLandscape = true,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                    onCollapseClick = { state.collapseSoft() },
                                    onMenuClick = {
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = mediaMetadata,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                onShowDetailsDialog = {
                                                    mediaMetadata?.id?.let {
                                                        bottomSheetPageState.show {
                                                            ShowMediaInfo(it)
                                                        }
                                                    }
                                                },
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(if (showInlineLyrics) 0.65f else 1f, false)
                            .animateContentSize()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                val bottomPadding by animateDpAsState(
                    targetValue = if (isFullScreen) 0.dp else queueSheetState.collapsedBound,
                    label = "bottomPadding"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = bottomPadding),
                ) {
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        val currentSliderPosition by rememberUpdatedState(sliderPosition)
                        val sliderPositionProvider = remember { { currentSliderPosition } }
                        val isExpandedProvider = remember(state) { { state.isExpanded } }
                        AnimatedContent(
                            targetState = showInlineLyrics,
                            label = "Lyrics",
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { showLyrics ->
                            if (showLyrics) {
                                InlineLyricsView(
                                    mediaMetadata = mediaMetadata,
                                    showLyrics = showLyrics,
                                    positionProvider = { effectivePosition }
                                )
                            } else {
                                Thumbnail(
                                    sliderPositionProvider = sliderPositionProvider,
                                    modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                    isPlayerExpanded = isExpandedProvider,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                    onCollapseClick = { state.collapseSoft() },
                                    onMenuClick = {
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = mediaMetadata,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                onShowDetailsDialog = {
                                                    mediaMetadata?.id?.let {
                                                        bottomSheetPageState.show {
                                                            ShowMediaInfo(it)
                                                        }
                                                    }
                                                },
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                            }
                        }


                    }

                    mediaMetadata?.let {
                        controlsContent(it)
                    }

                    Spacer(Modifier.height(if (useNewPlayerDesign) 16.dp else 8.dp))
                    Spacer(Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)))
                }
            }
        }

        AnimatedVisibility(
            visible = !isFullScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
            navController = navController,
            background =
            if (useBlackBackground) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surface 
            },
            onBackgroundColor = onBackgroundColor,
            TextBackgroundColor = TextBackgroundColor,
            textButtonColor = textButtonColor,
            iconButtonColor = iconButtonColor,
            pureBlack = pureBlack,
            showInlineLyrics = showInlineLyrics,
            playerBackground = playerBackground,
            onToggleLyrics = {
                showInlineLyrics = !showInlineLyrics
            },
            )
        }

        if (showCommentSheet) {
            CommentSheet(
                videoId = mediaMetadata?.id ?: "",
                onDismiss = { showCommentSheet = false }
            )
        }

        ListenTogetherDialog(
            visible = showListenTogetherDialog,
            mediaMetadata = mediaMetadata,
            onDismiss = { showListenTogetherDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val existing = database.lyrics(mediaMetadata.id).firstOrNull()
                    if (existing != null) return@launch
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.niksoriginals.melo.di.LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (e: Exception) {
                    
                }
            }
        }
    }

    Box (
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            lyrics == null -> {
                ContainedLoadingIndicator()
            }
            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    Lyrics(
                        sliderPositionProvider = positionProvider,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        showLyrics = showLyrics
                    )
                }
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                ) {
                    lyricsContent()
                }
            }
        }
    }
}


@Composable
fun MoreActionsButton(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(textButtonColor)
            .clickable {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = mediaMetadata,
                        navController = navController,
                        playerBottomSheetState = state,
                        onShowDetailsDialog = {
                            mediaMetadata.id.let {
                                bottomSheetPageState.show {
                                    ShowMediaInfo(it)
                                }
                            }
                        },
                        onDismiss = menuState::dismiss
                    )
                }
            }
    ) {
        Image(
            painter = painterResource(R.drawable.more_vert),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor)
        )
    }
}

@Composable
private fun PlayerMoreMenuButton(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(textButtonColor)
            .clickable {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = mediaMetadata,
                        navController = navController,
                        playerBottomSheetState = state,
                        onShowDetailsDialog = {
                            mediaMetadata.id.let {
                                bottomSheetPageState.show {
                                    ShowMediaInfo(it)
                                }
                            }
                        },
                        onDismiss = menuState::dismiss,
                    )
                }
            },
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
        )
    }
}

@Composable
private fun BackgroundVideoView(
    videoUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isVideoReady by remember(videoUrl) { mutableStateOf(false) }
    
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(4096, 4096)
                .setForceHighestSupportedBitrate(true)
                .build()
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setTargetBufferBytes(20 * 1024 * 1024) 
                    .build()
            )
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                playWhenReady = isPlaying
            }
    }

    val aspectRatioFrameLayout = remember {
        AspectRatioFrameLayout(context).apply {
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspectRatioFrameLayout.setAspectRatio(videoSize.width.toFloat() / videoSize.height)
                }
            }
            override fun onRenderedFirstFrame() {
                isVideoReady = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(videoUrl) {
        isVideoReady = false
        val mediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(if (videoUrl.contains("m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(800),
        label = "videoAlpha"
    )

    AndroidView(
        factory = { _ ->
            aspectRatioFrameLayout.apply {
                
                isEnabled = false
                isClickable = false
                isFocusable = false

                
                if (childCount == 0) {
                    val textureView = TextureView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                    addView(textureView)
                    exoPlayer.setVideoTextureView(textureView)
                }
            }
        },
        modifier = modifier.alpha(alpha)
    )
}
