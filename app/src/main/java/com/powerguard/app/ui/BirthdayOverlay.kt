package com.powerguard.app.ui

import android.app.Activity
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

// ── Confetti palette ──────────────────────────────────────────────────────────

private val CONFETTI_COLORS = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77), Color(0xFF4D96FF),
    Color(0xFFFF922B), Color(0xFFCC5DE8), Color(0xFFFF6EB4), Color(0xFFFFFFFF),
    Color(0xFFFFD700), Color(0xFF00CED1),
)

// ── Immutable seed per particle (generated once, never mutated) ───────────────

private data class ConfettiSeed(
    val startX: Float,   // 0..1  initial horizontal position
    val phase: Float,    // 0..1  time phase offset (staggers the fall)
    val speedY: Float,   // screen-heights per second
    val swayAmp: Float,  // horizontal sway amplitude (fraction of screen width)
    val swayFreq: Float, // sway cycles per screen-height travelled
    val size: Float,     // pixels
    val color: Color,
    val isCircle: Boolean,
    val rotSpeed: Float, // degrees per second
    val startRot: Float, // initial rotation
)

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun BirthdayOverlay(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Remove the system navigation bar contrast scrim for the duration of the overlay,
    // so the gradient fills behind the nav buttons without a darkening tint.
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val window = (view.context as Activity).window
            val wasEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced.also {
                    window.isNavigationBarContrastEnforced = false
                }
            } else false
            onDispose {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = wasEnforced
                }
            }
        }
    }

    // Trigger entrance animation on first composition
    LaunchedEffect(Unit) { visible = true }

    fun handleDismiss() {
        visible = false
        scope.launch {
            delay(480)   // let the exit animation finish before removing the composable
            onDismiss()
        }
    }

    // ── Frame timer ───────────────────────────────────────────────────────────
    var frameMs by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameMs = it - start }
        }
    }

    // ── Confetti seeds (stable across recompositions) ─────────────────────────
    val seeds = remember {
        val r = kotlin.random.Random(0xBD_2024) // fixed seed → deterministic layout
        List(75) {
            ConfettiSeed(
                startX    = r.nextFloat(),
                phase     = r.nextFloat(),
                speedY    = r.nextFloat() * 0.22f + 0.13f,
                swayAmp   = r.nextFloat() * 0.04f + 0.005f,
                swayFreq  = r.nextFloat() * 2.5f + 1f,
                size      = r.nextFloat() * 14f + 5f,
                color     = CONFETTI_COLORS[r.nextInt(CONFETTI_COLORS.size)],
                isCircle  = r.nextBoolean(),
                rotSpeed  = (r.nextFloat() - 0.5f) * 240f,
                startRot  = r.nextFloat() * 360f,
            )
        }
    }

    // ── Infinite text animations ──────────────────────────────────────────────
    val inf = rememberInfiniteTransition(label = "bd")

    val textScale by inf.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "scale",
    )

    val shimmer by inf.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "shimmer",
    )

    val mamaColor = lerpColor(Color(0xFFFFD700), Color(0xFFFFF59D), shimmer)

    // ── Composable tree ───────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(700)) + scaleIn(tween(700), initialScale = 0.88f),
        exit  = fadeOut(tween(480)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF3A0874),
                            Color(0xFF9C1458),
                            Color(0xFFD84315),
                        )
                    )
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = ::handleDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val t = frameMs / 1000f

            // Falling confetti (pure computation from time — no state mutation)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                seeds.forEach { s ->
                    val progress = (t * s.speedY + s.phase) % 1.15f
                    val cy = progress * h
                    val sway = sin((progress * s.swayFreq + s.phase) * 2f * PI.toFloat())
                    val cx = ((s.startX + sway * s.swayAmp) * w)
                        .coerceIn(0f, w)
                    val rot = s.startRot + t * s.rotSpeed
                    withTransform({
                        translate(cx, cy)
                        rotate(rot, pivot = Offset.Zero)
                    }) {
                        if (s.isCircle) {
                            drawCircle(s.color, radius = s.size / 2f, center = Offset.Zero)
                        } else {
                            drawRect(
                                s.color,
                                topLeft = Offset(-s.size / 2f, -s.size * 0.3f),
                                size = Size(s.size, s.size * 0.6f),
                            )
                        }
                    }
                }
            }

            // Text content on top of confetti
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🎂", fontSize = 100.sp)

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "С Днём Рождения,",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .graphicsLayer(scaleX = textScale, scaleY = textScale),
                )

                Text(
                    text = "мама!",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = mamaColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .graphicsLayer(scaleX = textScale, scaleY = textScale),
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "❤️  🎁  ✨  🌸  ✨  🎁  ❤️",
                    fontSize = 28.sp,
                )

                Spacer(Modifier.height(64.dp))

                Text(
                    text = "Нажмите, чтобы продолжить",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red   = a.red   + (b.red   - a.red)   * t,
    green = a.green + (b.green - a.green) * t,
    blue  = a.blue  + (b.blue  - a.blue)  * t,
    alpha = 1f,
)
