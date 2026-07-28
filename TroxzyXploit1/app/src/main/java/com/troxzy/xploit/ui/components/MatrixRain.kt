package com.troxzy.xploit.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import com.troxzy.xploit.ui.theme.NeonGreen
import kotlin.random.Random

/**
 * A Canvas-based Matrix rain animation effect.
 * Renders falling characters (random glyphs from a defined set) in multiple columns,
 * each with its own speed and position, creating the iconic "Matrix digital rain" effect.
 *
 * @param modifier Modifier for the composable.
 * @param charColor The color of the falling characters. Defaults to NeonGreen.
 */
@Composable
fun MatrixRain(
    modifier: Modifier = Modifier,
    charColor: Color = NeonGreen
) {
    val textMeasurer = rememberTextMeasurer()

    // Character set for the rain — mixture of katakana, latin, digits, symbols
    val charSet = remember {
        "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン" +
                "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ@#$%&*<>{}[]"
    }

    val textStyle = remember(charColor) {
        TextStyle(
            color = charColor,
            fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }

    // Infinite transition to drive the animation
    val infiniteTransition = rememberInfiniteTransition(label = "matrix_rain")

    // Animation progress that loops continuously
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50, easing = LinearEasing)
        ),
        label = "matrix_rain_progress"
    )

    // Track the current "tick" to know when to advance columns
    val currentTick = remember { mutableIntStateOf(0) }
    currentTick.intValue++

    // Column data: each column has its own Y offset, speed, and character array
    // We use a data holder that persists across recompositions
    val columnsState = remember { mutableMapOf<Int, ColumnData>() }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        val charSize = 14f * density
        val columnWidth = charSize * 1.2f
        val columnCount = (canvasWidth / columnWidth).toInt().coerceAtLeast(1)

        // Initialize or update column data
        for (i in 0 until columnCount) {
            val existing = columnsState[i]
            if (existing == null) {
                columnsState[i] = ColumnData(
                    y = Random.nextFloat() * -canvasHeight,
                    speed = Random.nextFloat() * 4f + 2f,
                    chars = Array(30) { charSet[Random.nextInt(charSet.length)].toString() },
                    trailLength = Random.nextInt(8, 20),
                    charChangeCounter = Random.nextInt(0, 10)
                )
            }
        }

        // Remove extra columns if canvas shrunk
        val keysToRemove = columnsState.keys.filter { it >= columnCount }
        keysToRemove.forEach { columnsState.remove(it) }

        // Draw each column
        for (i in 0 until columnCount) {
            val col = columnsState[i] ?: continue

            // Occasionally change a random character in the column for the "flicker" effect
            col.charChangeCounter++
            if (col.charChangeCounter > 5) {
                col.charChangeCounter = 0
                val changeIdx = Random.nextInt(col.chars.size)
                col.chars[changeIdx] = charSet[Random.nextInt(charSet.length)].toString()
            }

            // Advance the column's Y position
            col.y += col.speed * (charSize * 0.5f)

            // Reset column when it goes fully off screen
            if (col.y > canvasHeight + col.trailLength * charSize) {
                col.y = Random.nextFloat() * -canvasHeight * 0.5f
                col.speed = Random.nextFloat() * 4f + 2f
                col.trailLength = Random.nextInt(8, 20)
                col.chars = Array(30) { charSet[Random.nextInt(charSet.length)].toString() }
            }

            // Draw the trail of characters for this column
            for (j in 0 until col.trailLength) {
                val charY = col.y - j * charSize
                if (charY < -charSize || charY > canvasHeight + charSize) continue

                // Calculate alpha: head is brightest, tail fades out
                val alpha = when {
                    j == 0 -> 1.0f           // Head character: full brightness
                    j == 1 -> 0.85f
                    j < 3 -> 0.6f
                    else -> (1f - j.toFloat() / col.trailLength).coerceAtLeast(0.05f)
                }

                // Head character is bright white-green, rest are the specified color
                val drawColor = when {
                    j == 0 -> Color(
                        red = 0.7f,
                        green = 1f,
                        blue = 0.7f,
                        alpha = alpha
                    )
                    else -> charColor.copy(alpha = alpha)
                }

                val charIdx = j % col.chars.size
                drawText(
                    textMeasurer = textMeasurer,
                    text = col.chars[charIdx],
                    topLeft = Offset(
                        x = i * columnWidth + columnWidth * 0.1f,
                        y = charY
                    ),
                    style = TextStyle(
                        color = drawColor,
                        fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

/**
 * Holds the state for a single column in the Matrix rain.
 */
private class ColumnData(
    var y: Float,
    var speed: Float,
    var chars: Array<String>,
    var trailLength: Int,
    var charChangeCounter: Int
)
