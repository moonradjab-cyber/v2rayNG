package com.v2ray.ang.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val neonPurple = Color(0xFFB07CFF)
private val neonPink = Color(0xFFFF5CB0)
private val neonBlue = Color(0xFF6E9CFF)

private fun DrawScope.neonArc(
    color: Color,
    radius: Float,
    start: Float,
    sweep: Float,
    stroke: Float,
    alpha: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val tl = Offset(cx - radius, cy - radius)
    val sz = Size(radius * 2f, radius * 2f)
    // soft glow
    drawArc(
        color = color.copy(alpha = alpha * 0.28f),
        startAngle = start, sweepAngle = sweep, useCenter = false,
        topLeft = tl, size = sz,
        style = Stroke(width = stroke * 2.8f, cap = StrokeCap.Round)
    )
    // bright core
    drawArc(
        color = color.copy(alpha = alpha),
        startAngle = start, sweepAngle = sweep, useCenter = false,
        topLeft = tl, size = sz,
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
}

private fun DrawScope.neonDot(
    color: Color,
    radius: Float,
    angleDeg: Float,
    dotR: Float,
    alpha: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val rad = (angleDeg * PI / 180f).toFloat()
    val p = Offset(cx + radius * cos(rad), cy + radius * sin(rad))
    drawCircle(color.copy(alpha = alpha * 0.30f), dotR * 2.6f, p)
    drawCircle(Color.White.copy(alpha = alpha), dotR, p)
}

@Composable
fun ConnectButton(
    displayText: String,
    isRunning: Boolean,
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    onStatusClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "connect")
    val angle1 by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "a1"
    )
    val angle2 by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "a2"
    )
    val angle3 by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(5500, easing = LinearEasing), RepeatMode.Restart),
        label = "a3"
    )
    val glow by transition.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val breathe by transition.animateFloat(
        1f, 1.04f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(isRunning) {
        if (isRunning) {
            val start = System.currentTimeMillis()
            while (true) {
                elapsed = (System.currentTimeMillis() - start) / 1000
                delay(1000)
            }
        } else {
            elapsed = 0L
        }
    }
    val timerText = String.format(
        Locale.US, "%d:%02d:%02d",
        elapsed / 3600, (elapsed % 3600) / 60, elapsed % 60
    )
    val statusWord = if (isRunning) "ПОДКЛЮЧЕН" else "ОТКЛЮЧЕНО"

    // Show only the ping — hide IP address and country code
    val visibleStatus = remember(displayText) {
        displayText
            .replace(Regex("\\([A-Za-z]{2}\\)"), "")
            .replace(Regex("\\b\\d{1,3}(\\.\\d{1,3}){3}\\b"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // glow behind
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .alpha(if (isRunning) glow else 0.22f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0x88B07CFF), Color(0x00000000)))
                    )
            )

            // animated neon arcs with glow + dots
            Canvas(modifier = Modifier.size(220.dp)) {
                val a = if (isRunning) 1f else 0.12f
                val rOuter = size.minDimension * 0.44f
                val rMid = size.minDimension * 0.38f
                val rInner = size.minDimension * 0.32f
                val pivot = Offset(size.width / 2f, size.height / 2f)

                // faint full guide ring
                drawCircle(
                    color = Color(0xFF6B5CB0).copy(alpha = if (isRunning) 0.25f else 0.10f),
                    radius = rOuter,
                    style = Stroke(width = 1.dp.toPx())
                )

                rotate(if (isRunning) angle1 else 0f, pivot) {
                    neonArc(neonPurple, rOuter, 10f, 90f, 4.dp.toPx(), a)
                    neonArc(neonPurple, rOuter, 200f, 60f, 4.dp.toPx(), a)
                    neonDot(neonPurple, rOuter, 100f, 3.4.dp.toPx(), a)
                }
                rotate(if (isRunning) -angle2 else 0f, pivot) {
                    neonArc(neonPink, rMid, 60f, 76f, 3.2.dp.toPx(), a)
                    neonArc(neonPink, rMid, 250f, 50f, 3.2.dp.toPx(), a)
                    neonDot(neonPink, rMid, 136f, 3.0.dp.toPx(), a)
                }
                rotate(if (isRunning) angle3 else 0f, pivot) {
                    neonArc(neonBlue, rInner, 120f, 44f, 2.6.dp.toPx(), a)
                    neonArc(neonBlue, rInner, 300f, 44f, 2.6.dp.toPx(), a)
                    neonDot(neonBlue, rInner, 164f, 2.8.dp.toPx(), a)
                }
            }

            // inner button
            Box(
                modifier = Modifier
                    .size(if (isRunning) (128 * breathe).dp else 128.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRunning) {
                            Brush.radialGradient(listOf(Color(0xFF7A83FF), Color(0xFF33276E)))
                        } else {
                            Brush.radialGradient(listOf(Color(0xFF241F40), Color(0xFF15122A)))
                        }
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_power_24dp),
                        contentDescription = stringResource(
                            if (isRunning) R.string.acc_stop else R.string.acc_start
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusWord,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEAE8FF),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isRunning) {
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD8D4FA)
                        )
                    }
                }
            }
        }

        Text(
            text = visibleStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStatusClick)
                .padding(top = 10.dp, start = 24.dp, end = 24.dp)
        )
    }
}
