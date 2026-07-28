package com.troxzy.xploit.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.troxzy.xploit.ui.theme.DarkCard
import com.troxzy.xploit.ui.theme.NeonCyan

/**
 * A composable card with a neon glow border effect.
 *
 * When [selected] is true or the card is pressed, the border glows with the
 * [accentColor], creating a neon outline effect. The glow is achieved by
 * drawing a blurred border behind the card using [drawBehind].
 *
 * @param onClick Callback invoked when the card is clicked.
 * @param modifier Modifier for the composable.
 * @param selected Whether the card is in a selected/active state (glows persistently).
 * @param accentColor The neon color used for the glow border.
 * @param cornerRadius The corner radius of the card.
 * @param glowRadius The blur radius for the glow effect.
 * @param content The content composable inside the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accentColor: Color = NeonCyan,
    cornerRadius: Dp = 12.dp,
    glowRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    // Track press state
    var isPressed by remember { mutableStateOf(false) }

    // Determine if the glow should be active
    val isGlowing = selected || isPressed

    // Animate the glow pulse when active
    val infiniteTransition = rememberInfiniteTransition(label = "neon_card_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // The effective glow alpha
    val glowAlpha = if (isGlowing) glowPulse else 0f

    // Border color with alpha
    val borderColor = accentColor.copy(alpha = if (isGlowing) glowPulse else 0.3f)

    val shape = RoundedCornerShape(cornerRadius)

    Card(
        onClick = onClick,
        modifier = modifier
            .drawBehind {
                if (!isGlowing) return@drawBehind

                val cornerRadiusPx = cornerRadius.toPx()
                val glowRadiusPx = glowRadius.toPx()

                // Draw the neon glow effect using a blurred stroke
                val paint = Paint().apply {
                    this.color = accentColor.copy(alpha = glowAlpha * 0.6f)
                    this.style = PaintingStyle.Stroke
                    this.strokeWidth = 2.dp.toPx()
                    this.pathEffect = null
                }

                // Draw multiple layers of glow with decreasing alpha for blur effect
                for (i in 1..5) {
                    val layerAlpha = glowAlpha * (0.3f / i)
                    val layerStrokeWidth = 2.dp.toPx() + (i * glowRadiusPx * 0.4f)

                    drawRoundRect(
                        color = accentColor.copy(alpha = layerAlpha),
                        cornerRadius = CornerRadius(cornerRadiusPx),
                        style = Stroke(width = layerStrokeWidth)
                    )
                }

                // Draw the main bright border
                drawRoundRect(
                    color = accentColor.copy(alpha = glowAlpha * 0.9f),
                    cornerRadius = CornerRadius(cornerRadiusPx),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when {
                            event.changes.any { it.pressed } -> isPressed = true
                            else -> isPressed = false
                        }
                    }
                }
            },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        border = BorderStroke(
            width = if (isGlowing) 1.5.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        content = content
    )
}
