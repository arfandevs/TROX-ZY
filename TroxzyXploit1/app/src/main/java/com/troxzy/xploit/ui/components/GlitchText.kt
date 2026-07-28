package com.troxzy.xploit.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonGreen
import com.troxzy.xploit.ui.theme.NeonPurple
import com.troxzy.xploit.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A composable that displays text with a glitch/hack animation effect.
 *
 * When [isAnimating] is true, the text will:
 * - Randomly shift its X/Y offset (jitter effect)
 * - Change color between neon purple, cyan, and green
 * - Occasionally show "corrupted" characters replacing the original text
 *
 * When [isAnimating] is false, the text is displayed statically with the default color.
 *
 * @param text The text to display.
 * @param modifier Modifier for the composable.
 * @param isAnimating Whether the glitch animation is active.
 * @param fontSize Font size for the text.
 * @param fontFamily Font family for the text (defaults to Monospace for code feel).
 * @param defaultColor The default color when not animating.
 */
@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true,
    fontSize: TextUnit = 16.sp,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontWeight: FontWeight = FontWeight.Bold,
    defaultColor: Color = TextPrimary
) {
    // Glitch colors cycle
    val glitchColors = remember {
        listOf(NeonPurple, NeonCyan, NeonGreen, NeonPurple, NeonCyan)
    }

    // Corrupted character replacements
    val corruptChars = remember {
        "!@#$%^&*<>{}[]|/\\~`?░▒▓█▄▀■□▪▫".toList()
    }

    // State for the current glitch offset
    var glitchOffsetX by remember { mutableStateOf(0f) }
    var glitchOffsetY by remember { mutableStateOf(0f) }

    // State for current color
    var currentColor by remember { mutableStateOf(defaultColor) }

    // State for the currently displayed (potentially corrupted) text
    var displayText by remember { mutableStateOf(text) }

    // Infinite transition for the glitch cycle timing
    val infiniteTransition = rememberInfiniteTransition(label = "glitch_text")

    // A rapidly cycling float to trigger glitch "frames"
    val glitchCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 80, easing = { it }),
            repeatMode = RepeatMode.Restart
        ),
        label = "glitch_cycle"
    )

    // Glitch intensity — randomly varies between "full glitch" and "calm"
    val intensityCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = { it }),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitch_intensity"
    )

    // Apply glitch effects on each cycle tick
    LaunchedEffect(glitchCycle, isAnimating) {
        if (!isAnimating) {
            glitchOffsetX = 0f
            glitchOffsetY = 0f
            currentColor = defaultColor
            displayText = text
            return@LaunchedEffect
        }

        // Intensity determines how aggressive the glitch is
        val intensity = intensityCycle
        val isGlitching = Random.nextFloat() < (0.3f + intensity * 0.4f)

        if (isGlitching) {
            // Random offset jitter — more intense when intensity is high
            val maxOffset = 2f + intensity * 4f
            glitchOffsetX = Random.nextFloat() * maxOffset * 2 - maxOffset
            glitchOffsetY = Random.nextFloat() * maxOffset * 2 - maxOffset

            // Random color from the glitch palette
            currentColor = glitchColors[Random.nextInt(glitchColors.size)]

            // Corrupt some characters
            val corrupted = text.map { char ->
                if (Random.nextFloat() < 0.15f * intensity) {
                    corruptChars[Random.nextInt(corruptChars.size)]
                } else {
                    char
                }
            }.joinToString("")
            displayText = corrupted
        } else {
            // Calm frame — minimal offset, original text
            glitchOffsetX = Random.nextFloat() * 0.5f - 0.25f
            glitchOffsetY = 0f
            currentColor = defaultColor
            displayText = text
        }
    }

    // Render the glitched text with offset
    Box(modifier = modifier) {
        // Shadow/ghost layer — offset in opposite direction for depth
        if (isAnimating) {
            Text(
                text = displayText,
                color = NeonPurple.copy(alpha = 0.3f),
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                modifier = Modifier.offset {
                    IntOffset(
                        (glitchOffsetX * -1.5f).roundToInt(),
                        (glitchOffsetY * -1.5f).roundToInt()
                    )
                }
            )
        }

        // Main text layer
        Text(
            text = displayText,
            color = currentColor,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            modifier = Modifier.offset {
                IntOffset(
                    glitchOffsetX.roundToInt(),
                    glitchOffsetY.roundToInt()
                )
            }
        )
    }
}
