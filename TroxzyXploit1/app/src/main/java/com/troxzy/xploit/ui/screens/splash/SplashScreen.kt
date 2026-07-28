package com.troxzy.xploit.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.components.MatrixRain
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonPurple
import kotlinx.coroutines.delay

/**
 * Splash screen displayed on app launch.
 * Shows a MatrixRain background animation, glitched title text,
 * and a progress indicator that animates over 3 seconds before
 * invoking [onFinished].
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    // ------------------------------------------------------------------ state
    var progressTarget by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(
            durationMillis = 3000,
            easing = LinearEasing
        ),
        label = "splash_progress"
    )

    // -------------------------------------------------------------- effects
    LaunchedEffect(Unit) {
        // Kick off the progress bar animation
        progressTarget = 1f
        // Wait for the full 3‑second duration then navigate away
        delay(3000L)
        onFinished()
    }

    // --------------------------------------------------------------- layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1 – Matrix rain background
        MatrixRain(
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2 – Foreground content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glitch title
            GlitchText(
                text = "TROXZYXPLOIT",
                isAnimating = true,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Credit line
            Text(
                text = "by Troxzy | t.me/SoloBanNoTrash",
                color = NeonCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = NeonPurple,
                trackColor = Color(0x33FFFFFF),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
