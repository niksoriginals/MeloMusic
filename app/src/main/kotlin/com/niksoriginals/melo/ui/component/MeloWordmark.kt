package com.niksoriginals.melo.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

/**
 * Two-layer Melo wordmark:
 *
 *  - MOULD: the faint groove (alpha = MOULD_ALPHA).
 *    Logo mould is always present. Text mould is controlled independently by
 *    [textMouldProgress] (stroke length) and [textMouldAlphaMultiplier] (opacity).
 *  - INK: the solid color liquid.
 *
 * Supported modes:
 *  1. Unified Mode ([progress] != null, used during pull-to-refresh & initial launch):
 *     Controls all 15 strokes together (0 = completely empty mould, 1 = fully inked wordmark).
 *     Pulling down drains text ('o' -> 'l' -> 'e' -> 'M') and then drains through the wave-sticks logo
 *     all the way until all ink vanishes into the empty watermark groove!
 *     Releasing refills the logo sticks first, then M -> e -> l -> o!
 *
 *  2. Split Mode ([progress] == null, used during scroll collapse and scroll return):
 *     Logo is kept solid ([logoProgress] = 1f) and glides to center via [logoOffsetFraction].
 *     Text ink and mould drain simultaneously (1 -> 0) only for the text without any box clipping!
 */
@Composable
fun MeloWordmark(
    progress: Float? = null,
    textProgress: Float = 1f,
    textMouldProgress: Float = 1f,
    textMouldAlphaMultiplier: Float = 1f,
    logoProgress: Float = 1f,
    logoMouldAlphaMultiplier: Float = 1f,
    logoOffsetFraction: Float = 0f, // 0f = resting left, 1f = exact dead center
    color: Color,
    modifier: Modifier = Modifier,
) {
    val paths = remember { MELO_STROKES.map { PathParser().parsePathString(it.pathData).toPath() } }
    val measures = remember(paths) { paths.map { PathMeasure().apply { setPath(it, false) } } }
    val scratchMouldPath = remember { Path() }
    val scratchInkPath = remember { Path() }

    Canvas(modifier) {
        val sx = size.width / VIEWPORT_WIDTH
        val sy = size.height / VIEWPORT_HEIGHT
        withTransform({ scale(sx, sy, pivot = Offset.Zero) }) {
            // Horizontal shift for centering the logo without shrinking the canvas or clipping
            val logoShiftX = logoOffsetFraction * LOGO_CENTER_SHIFT

            // Layer 1: The Mould (The watermark groove)
            // Logo mould (glides with the logo)
            if (logoMouldAlphaMultiplier > 0f) {
                withTransform({ translate(left = logoShiftX, top = 0f) }) {
                    drawStrokeSubset(
                        paths = paths,
                        measures = measures,
                        scratchPath = scratchMouldPath,
                        startIndex = 0,
                        endIndex = LOGO_STROKE_COUNT,
                        progress = 1f,
                        color = color.copy(alpha = MOULD_ALPHA * logoMouldAlphaMultiplier)
                    )
                }
            }
            // Text mould (stays in place, drains/fades 1 -> 0 on scroll)
            if (textMouldAlphaMultiplier > 0f && textMouldProgress > 0f) {
                drawTextStrokes(
                    paths = paths,
                    measures = measures,
                    scratchPath = scratchMouldPath,
                    textProgress = textMouldProgress,
                    color = color.copy(alpha = MOULD_ALPHA * textMouldAlphaMultiplier)
                )
            }

            // Layer 2: The Ink
            if (progress != null) {
                // Unified mode (Pull-to-refresh & app launch):
                // Drains/fills through all 15 strokes (logo sticks + Melo text)
                if (progress > 0f) {
                    drawAllStrokes(
                        paths = paths,
                        measures = measures,
                        scratchPath = scratchInkPath,
                        progress = progress,
                        color = color
                    )
                }
            } else {
                // Split mode (Scroll collapse & top return):
                // Logo and Text shift together horizontally via logoShiftX
                withTransform({ translate(left = logoShiftX, top = 0f) }) {
                    if (logoProgress > 0f) {
                        drawStrokeSubset(
                            paths = paths,
                            measures = measures,
                            scratchPath = scratchInkPath,
                            startIndex = 0,
                            endIndex = LOGO_STROKE_COUNT,
                            progress = logoProgress,
                            color = color,
                        )
                    }
                    // Text drains/writes at textProgress (1 -> 0 or 0 -> 1)
                    if (textProgress > 0f) {
                        drawTextStrokes(
                            paths = paths,
                            measures = measures,
                            scratchPath = scratchInkPath,
                            textProgress = textProgress,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws all 15 strokes normalized across 0..1 (Threads launch/refresh staging).
 * Progress 0.00..0.30: Wave-sticks logo (Indices 0 to 6).
 * Progress 0.30..1.00: Melo text (Indices 7 to 14).
 */
private fun DrawScope.drawAllStrokes(
    paths: List<Path>,
    measures: List<PathMeasure>,
    scratchPath: Path,
    progress: Float,
    color: Color,
) {
    for (i in MELO_STROKES.indices) {
        val stroke = MELO_STROKES[i]
        val local = if (progress >= 1f) 1f else ((progress - stroke.start) / (stroke.end - stroke.start)).coerceIn(0f, 1f)
        if (local <= 0f) continue

        val path = if (local >= 1f) {
            paths[i]
        } else {
            scratchPath.reset()
            measures[i].getSegment(0f, measures[i].length * local, scratchPath, true)
            scratchPath
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = stroke.strokeWidth,
                miter = 4f,
                cap = stroke.cap,
                join = stroke.join,
            ),
        )
    }
}

/**
 * Draws a subset of strokes (e.g. logo) with absolute timing.
 */
private fun DrawScope.drawStrokeSubset(
    paths: List<Path>,
    measures: List<PathMeasure>,
    scratchPath: Path,
    startIndex: Int,
    endIndex: Int,
    progress: Float,
    color: Color,
) {
    for (i in startIndex until endIndex) {
        val stroke = MELO_STROKES[i]
        val local = if (progress >= 1f) 1f else ((progress - stroke.start) / (stroke.end - stroke.start)).coerceIn(0f, 1f)
        if (local <= 0f) continue

        val path = if (local >= 1f) {
            paths[i]
        } else {
            scratchPath.reset()
            measures[i].getSegment(0f, measures[i].length * local, scratchPath, true)
            scratchPath
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = stroke.strokeWidth,
                miter = 4f,
                cap = stroke.cap,
                join = stroke.join,
            ),
        )
    }
}

/**
 * Draws text strokes (M, e, l, o) mapping textProgress (0..1) across text range (0.300f..1.000f).
 */
private fun DrawScope.drawTextStrokes(
    paths: List<Path>,
    measures: List<PathMeasure>,
    scratchPath: Path,
    textProgress: Float,
    color: Color,
) {
    val textStart = 0.300f
    val textEnd = 1.000f
    val effectiveProgress = textStart + textProgress.coerceIn(0f, 1f) * (textEnd - textStart)

    for (i in LOGO_STROKE_COUNT until MELO_STROKES.size) {
        val stroke = MELO_STROKES[i]
        val local = ((effectiveProgress - stroke.start) / (stroke.end - stroke.start)).coerceIn(0f, 1f)
        if (local <= 0f) continue

        val path = if (local >= 1f) {
            paths[i]
        } else {
            scratchPath.reset()
            measures[i].getSegment(0f, measures[i].length * local, scratchPath, true)
            scratchPath
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = stroke.strokeWidth,
                miter = 4f,
                cap = stroke.cap,
                join = stroke.join,
            ),
        )
    }
}

private const val LOGO_STROKE_COUNT = 7 // 6 sticks + 1 bottom dot
private const val VIEWPORT_WIDTH = 3.6111f
private const val VIEWPORT_HEIGHT = 1f
private const val MOULD_ALPHA = 0.24f

// Logo center is at x = 0.7350f. Viewport center is at 3.6111f / 2 = 1.80555f.
// Shift required to glide logo to exact dead center = 1.80555f - 0.7350f = 1.07055f.
private const val LOGO_CENTER_SHIFT = 1.07055f

private data class MeloStroke(
    val pathData: String,
    val strokeWidth: Float,
    val cap: StrokeCap,
    val join: StrokeJoin = StrokeJoin.Round,
    val start: Float,
    val end: Float,
)

private val MELO_STROKES = listOf(
    // Wave-sticks logo (Indices 0 to 6) — 0.000f to 0.300f
    // Equal 9.6px pitch (0.0960f) and 8.5px thickness (0.0850f)
    MeloStroke("M0.4950,0.2700 L0.4950,0.7300", 0.0850f, StrokeCap.Round, StrokeJoin.Round, 0.000f, 0.120f),
    MeloStroke("M0.5910,0.3700 L0.5910,0.5900", 0.0850f, StrokeCap.Round, StrokeJoin.Round, 0.030f, 0.150f),
    MeloStroke("M0.6870,0.4400 L0.6870,0.5300", 0.0850f, StrokeCap.Round, StrokeJoin.Round, 0.060f, 0.180f),
    MeloStroke("M0.7830,0.4400 L0.7830,0.5300", 0.0850f, StrokeCap.Round, StrokeJoin.Round, 0.090f, 0.210f),
    MeloStroke("M0.8790,0.3700 L0.8790,0.5900", 0.0850f, StrokeCap.Round, StrokeJoin.Round, 0.120f, 0.240f),
    MeloStroke("M0.9750,0.2500 L0.9750,0.7300", 0.0850f, StrokeCap.Round, StrokeJoin.Round, 0.150f, 0.270f),
    MeloStroke("M0.7350,0.6700 L0.7350,0.6900", 0.0850f, StrokeCap.Round, StrokeJoin.Round, 0.180f, 0.300f),

    // Letter M (Index 7) — 0.300f to 0.540f
    MeloStroke(
        "M1.2000,0.7450 L1.2500,0.4750 L1.3050,0.2000 L1.4700,0.6550 L1.7200,0.2050 L1.7620,0.4660 L1.8040,0.7240",
        0.1500f,
        StrokeCap.Round,
        StrokeJoin.Round,
        0.300f,
        0.540f,
    ),

    // Letter e (Index 8) — 0.500f to 0.700f (Tilted loop, authentic font geometry, single continuous contour)
    MeloStroke(
        "M1.9500,0.6072 C1.9500,0.4916 2.0408,0.4100 2.1241,0.4100 C2.1997,0.4100 2.2376,0.4780 2.2073,0.5460 " +
            "C2.1770,0.6072 2.0560,0.6072 1.9500,0.6072 " +
            "C1.9500,0.6956 2.0181,0.7500 2.1089,0.7500 C2.1619,0.7500 2.1997,0.7228 2.2300,0.6820",
        0.1100f,
        StrokeCap.Round,
        StrokeJoin.Round,
        0.500f,
        0.700f,
    ),

    // Letter l (Index 9) — 0.660f to 0.820f (Single upright stem)
    MeloStroke("M2.3700,0.7900 L2.5200,0.2100", 0.1581f, StrokeCap.Butt, StrokeJoin.Round, 0.660f, 0.820f),

    // Letter o (Index 10) — 0.780f to 1.000f (Seamless 1-piece closed loop, zero jod)
    MeloStroke(
        "M2.7800,0.4300 C2.8683,0.4300 2.9400,0.5017 2.9400,0.5900 C2.9400,0.6783 2.8683,0.7500 2.7800,0.7500 " +
            "C2.6917,0.7500 2.6200,0.6783 2.6200,0.5900 C2.6200,0.5017 2.6917,0.4300 2.7800,0.4300 Z",
        0.1500f,
        StrokeCap.Round,
        StrokeJoin.Round,
        0.780f,
        1.000f,
    ),
)
