package com.v2ray.ang.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ConnectButton(
    displayText: String,
    isRunning: Boolean,
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    onStatusClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "connect")
    val ringRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )
    val glow by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // glow
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .alpha(if (isRunning) glow else 0.35f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xAA7C6CF5), Color(0x00000000))
                        )
                    )
            )
            // rotating gradient ring
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .rotate(if (isRunning) ringRotation else 0f)
                    .alpha(if (isRunning) 1f else 0.55f)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF7C6CF5),
                                Color(0xFF5A6BF0),
                                Color(0xFFB05CF0),
                                Color(0xFF7C6CF5)
                            )
                        )
                    )
            )
            // inner button
            Box(
                modifier = Modifier
                    .size(134.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRunning) {
                            Brush.radialGradient(listOf(Color(0xFF6E78F7), Color(0xFF454FCB)))
                        } else {
                            Brush.radialGradient(listOf(Color(0xFF2C2748), Color(0xFF1C1834)))
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
                        modifier = Modifier.size(38.dp)
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
                            color = Color(0xFFD3D0F5)
                        )
                    }
                }
            }
        }

        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStatusClick)
                .padding(top = 12.dp, start = 24.dp, end = 24.dp)
        )
    }
}
