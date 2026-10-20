

package com.niksoriginals.melo.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 10.dp,
    isGlass: Boolean = false,
) {
    val inactiveTrackColor = colors.inactiveTrackColor
    val activeTrackColor = colors.activeTrackColor
    val inactiveTickColor = colors.inactiveTickColor
    val activeTickColor = colors.activeTickColor
    val valueRange = sliderState.valueRange

    Canvas(
        modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        val fraction = calcFraction(
            valueRange.start,
            valueRange.endInclusive,
            sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive)
        )
        if (isGlass) {
            drawGlassTrack(
                activeFraction = fraction,
                trackHeight = trackHeight,
                activeColor = activeTrackColor,
                inactiveColor = inactiveTrackColor,
            )
        } else {
            drawTrack(
                stepsToTickFractions(sliderState.steps),
                0f,
                fraction,
                inactiveTrackColor,
                activeTrackColor,
                inactiveTickColor,
                activeTickColor,
                trackHeight
            )
        }
    }
}

@Composable
fun LiquidGlassThumb(
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color.White,
) {
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActive = isDragged || isPressed

    val thumbSize by animateDpAsState(
        targetValue = if (isActive) 18.dp else 12.dp,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassThumbSize"
    )

    val thumbElevation by animateDpAsState(
        targetValue = if (isActive) 6.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "glassThumbElevation"
    )

    Box(
        modifier = modifier
            .size(thumbSize)
            .shadow(
                elevation = thumbElevation,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.94f),
                        Color(0xFFCBD5E1).copy(alpha = 0.88f)
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.95f),
                shape = CircleShape
            )
    )
}

private fun DrawScope.drawGlassTrack(
    activeFraction: Float,
    trackHeight: Dp,
    activeColor: Color,
    inactiveColor: Color,
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val trackHeightPx = trackHeight.toPx()
    val radius = trackHeightPx / 2f
    val cornerRadius = CornerRadius(radius, radius)
    val width = size.width
    val top = center.y - radius
    val bottom = center.y + radius

    // 1. Inactive Glass Trough: Frosted translucent capsule with depth
    val troughGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.14f)
        ),
        startY = top,
        endY = bottom
    )
    drawRoundRect(
        brush = troughGradient,
        topLeft = Offset(0f, top),
        size = Size(width, trackHeightPx),
        cornerRadius = cornerRadius
    )

    // Glass Trough Specular Rim Outline
    drawRoundRect(
        color = Color.White.copy(alpha = 0.30f),
        topLeft = Offset(0.5f, top + 0.5f),
        size = Size(width - 1f, trackHeightPx - 1f),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.dp.toPx())
    )

    // Top Specular Highlight Glint along the glass groove
    if (width > radius * 2) {
        drawLine(
            color = Color.White.copy(alpha = 0.40f),
            start = Offset(radius, top + 1.dp.toPx()),
            end = Offset(width - radius, top + 1.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 2. Active Liquid Core
    val activeEndPx = (width * activeFraction).coerceIn(0f, width)
    if (activeEndPx > 0f) {
        val activeWidth = activeEndPx
        val startX = if (isRtl) width - activeWidth else 0f
        val activeGradient = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.98f),
                activeColor.copy(alpha = 0.88f),
                activeColor.copy(alpha = 0.75f)
            ),
            startY = top,
            endY = bottom
        )

        val activeCorner = CornerRadius(radius, radius)
        clipPath(
            Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset(startX, top), Size(activeWidth, trackHeightPx)),
                        topLeft = activeCorner,
                        bottomLeft = activeCorner,
                        topRight = if (activeFraction >= 0.98f) activeCorner else CornerRadius(radius * 0.7f, radius * 0.7f),
                        bottomRight = if (activeFraction >= 0.98f) activeCorner else CornerRadius(radius * 0.7f, radius * 0.7f)
                    )
                )
            }
        ) {
            drawRoundRect(
                brush = activeGradient,
                topLeft = Offset(startX, top),
                size = Size(activeWidth, trackHeightPx),
                cornerRadius = activeCorner
            )

            // Specular gloss crest along the top of active liquid fill
            drawLine(
                color = Color.White.copy(alpha = 0.92f),
                start = Offset(startX + radius * 0.5f, top + 1.dp.toPx()),
                end = Offset(startX + activeWidth - 1.dp.toPx(), top + 1.dp.toPx()),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    trackHeight: Dp = 2.dp
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val sliderLeft = Offset(0f, center.y)
    val sliderRight = Offset(size.width, center.y)
    val sliderStart = if (isRtl) sliderRight else sliderLeft
    val sliderEnd = if (isRtl) sliderLeft else sliderRight
    val tickSize = 2.0.dp.toPx()
    val trackStrokeWidth = trackHeight.toPx()
    drawLine(
        inactiveTrackColor,
        sliderStart,
        sliderEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )
    val sliderValueEnd = Offset(
        sliderStart.x +
                (sliderEnd.x - sliderStart.x) * activeRangeEnd,
        center.y
    )
    val sliderValueStart = Offset(
        sliderStart.x +
                (sliderEnd.x - sliderStart.x) * activeRangeStart,
        center.y
    )
    drawLine(
        activeTrackColor,
        sliderValueStart,
        sliderValueEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )
    for (tick in tickFractions) {
        val outsideFraction = tick > activeRangeEnd || tick < activeRangeStart
        drawCircle(
            color = if (outsideFraction) inactiveTickColor else activeTickColor,
            center = Offset(lerp(sliderStart, sliderEnd, tick).x, center.y),
            radius = tickSize / 2f
        )
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)
