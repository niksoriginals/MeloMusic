package com.niksoriginals.melo.ui.component

import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.niksoriginals.melo.R
import kotlinx.coroutines.launch

/**
 * Threads-Style Mechanical Header for Melo:
 *
 * 1. Pull-to-Refresh (Part A):
 *    - Pulling down: Ink drains backward inside the mould groove:
 *      First 'o' -> 'l' -> 'e' -> 'M', and then drains completely through the wave-sticks
 *      logo until ALL ink vanishes into the empty watermark groove!
 *    - Bidirectional hand tracking: If the user pulls down partway (e.g. to 'e' or to the logo)
 *      and moves hand back up, the ink directly tracks finger movement in real-time, refilling
 *      without triggering refresh!
 *    - Releasing after 100% threshold: Pen-cap pop audio plays, and ink animates smoothly from the
 *      very first stroke of the logo, across all wave sticks, and through M -> e -> l -> o (600ms refill).
 *    - Releasing before threshold: Ink smoothly springs back to full without refreshing.
 *
 * 2. Scroll Collapse (Part B):
 *    - Scrolling down: The container does NOT shrink or clip with a black shutter!
 *      Instead, the solid wave-sticks logo smoothly glides horizontally into the exact dead center
 *      of the screen, while the text "Melo" ink and mould cleanly wipe/drain away simultaneously (1 -> 0).
 *    - Scrolled state: ONLY the solid wave-sticks logo floats in the dead center.
 *    - Scrolling back to top: Logo smoothly glides back to the left, text mould reveals,
 *      and ink writes "Melo" live (0 -> 1) without any flickering or blinking of the logo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeloThreadsHeader(
    lazylistState: LazyListState,
    pullRefreshState: PullToRefreshState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    contentAlpha: Float = 1f,
    refreshKey: Any? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Authentic pen-cap sound effect from Threads
    val soundPlayer = remember {
        try {
            MediaPlayer.create(context, R.raw.barcelona_refresh_sfx_pencap_01)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                soundPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    // Press scale animation (0.92x spring)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "MeloHeaderPressScale"
    )

    // Detect if user has scrolled down the feed
    val isScrolled by remember {
        derivedStateOf {
            lazylistState.firstVisibleItemIndex > 0 || lazylistState.firstVisibleItemScrollOffset > 24
        }
    }

    // Scroll collapse transition: 0f = at top (full wordmark), 1f = scrolled (centered logo only)
    val scrollTransition by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(
            durationMillis = 750,
            easing = CubicBezierEasing(0.35f, 0.0f, 0.25f, 1.0f),
        ),
        label = "MeloScrollTransition",
    )

    // Animation driver for full wordmark refill (initial launch & refresh trigger)
    val autonomousWordmarkProgress = remember { Animatable(1f) }
    var isWordmarkRefilling by remember { mutableStateOf(false) }
    var isAwaitingPullReset by remember { mutableStateOf(false) }

    // Initial write-in on first app launch
    LaunchedEffect(Unit) {
        autonomousWordmarkProgress.snapTo(0f)
        autonomousWordmarkProgress.animateTo(
            1f,
            tween(durationMillis = 700, easing = FastOutSlowInEasing),
        )
    }

    // Refresh trigger:
    // 1. When isRefreshing == true: ink refills progressively (Logo -> M -> e -> l) over 2.4s while waiting for network.
    // 2. When isRefreshing == false: network finished! Rapidly and smoothly completes the remaining ink to 100% (280ms)!
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            isAwaitingPullReset = true
            isWordmarkRefilling = true
            try {
                soundPlayer?.seekTo(0)
                soundPlayer?.start()
            } catch (_: Exception) {}

            autonomousWordmarkProgress.snapTo(0f)
            autonomousWordmarkProgress.animateTo(
                targetValue = 0.82f,
                animationSpec = tween(durationMillis = 2400, easing = LinearOutSlowInEasing),
            )
        } else if (isAwaitingPullReset) {
            // Network fetch finished! Decisively complete remaining ink to 100%
            autonomousWordmarkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            )
            isWordmarkRefilling = false
        }
    }

    // UI Data Arrived Trigger: As soon as the main UI content updates (refreshKey changes while refreshing),
    // immediately complete ink to 100% without waiting for slower background API calls!
    var lastRefreshKey by remember { mutableStateOf(refreshKey) }
    LaunchedEffect(refreshKey) {
        if (isRefreshing && refreshKey != null && refreshKey != lastRefreshKey) {
            autonomousWordmarkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            )
            isWordmarkRefilling = false
        }
        lastRefreshKey = refreshKey
    }

    val pullFraction = pullRefreshState.distanceFraction

    // When pull indicator has fully sprung back to top after refresh, unlock pull tracking
    LaunchedEffect(pullFraction, isRefreshing) {
        if (!isRefreshing && pullFraction <= 0.005f) {
            isAwaitingPullReset = false
        }
    }

    // Haptic feedback tick when passing 100% threshold during pull
    var hasVibratedThreshold by remember { mutableStateOf(false) }
    LaunchedEffect(pullFraction, isRefreshing) {
        if (!isRefreshing && pullFraction >= 1.0f && !hasVibratedThreshold) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hasVibratedThreshold = true
        } else if (pullFraction < 0.1f) {
            hasVibratedThreshold = false
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                alpha = contentAlpha
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                coroutineScope.launch {
                    if (lazylistState.firstVisibleItemIndex == 0 && lazylistState.firstVisibleItemScrollOffset == 0) {
                        onRefresh()
                    } else {
                        lazylistState.animateScrollToItem(0)
                    }
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        if (isScrolled || scrollTransition > 0.001f) {
            // Synchronized unified scroll collapse:
            // 1. Groove (mould) hides immediately (zero ghost outlines).
            // 2. Logo + Text shift together to the right while text drains from right ('o' -> 'l' -> 'e' -> 'M').
            //    At the exact moment logo reaches dead center (s = 1f), 'M' finishes draining!
            // 3. On return (s -> 0f), Logo + Text shift back to left while text refills from 'M' -> 'o',
            //    finishing the refill at the exact moment logo arrives home!
            val s = scrollTransition
            val textInk = (1f - s).coerceIn(0f, 1f)
            val logoOffset = s.coerceIn(0f, 1f)

            MeloWordmark(
                progress = null, // Split mode
                textProgress = textInk,
                textMouldProgress = 0f, // Groove hidden immediately during scroll
                textMouldAlphaMultiplier = 0f,
                logoProgress = 1f, // Always 100% solid
                logoMouldAlphaMultiplier = 0f,
                logoOffsetFraction = logoOffset, // Shifts Logo + Text unit to dead center
                color = color,
                modifier = Modifier.size(width = 130.dp, height = 36.dp),
            )
        } else {
            // Unified mode for Pull-To-Refresh:
            // Drains through all 15 strokes: 'o' -> 'l' -> 'e' -> 'M' -> LOGO!
            // Reaches 0.0 (all ink vanishes into mould) at 100% pull threshold.
            // Finger up tracking: directly refills with hand in real time!
            // Release: flows from logo stick 0 through 'o' (600ms live refill)!
            val currentProgress = if (isWordmarkRefilling || isRefreshing || isAwaitingPullReset) {
                autonomousWordmarkProgress.value
            } else {
                (1f - pullFraction).coerceIn(0f, 1f)
            }

            MeloWordmark(
                progress = currentProgress, // Unified: drains through text AND logo!
                textMouldProgress = 1f,
                textMouldAlphaMultiplier = 1f,
                logoOffsetFraction = 0f,
                color = color,
                modifier = Modifier.size(width = 130.dp, height = 36.dp)
            )
        }
    }
}
