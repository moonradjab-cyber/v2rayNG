package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R

private data class OnboardPage(
    val iconRes: Int,
    val title: String,
    val text: String,
    val showBotButton: Boolean,
)

@Composable
fun OnboardingScreen(onOpenBot: () -> Unit, onFinish: () -> Unit) {
    val pages = listOf(
        OnboardPage(
            R.drawable.ic_shield_24dp,
            "Maxachkala VPN",
            "Быстрый и безопасный доступ в интернет — без сложных настроек.",
            false
        ),
        OnboardPage(
            R.drawable.ic_telegram_24dp,
            "Возьми подписку в боте",
            "Открой Telegram-бота, получи свою ссылку подписки и скопируй её.",
            true
        ),
        OnboardPage(
            R.drawable.ic_paste_24dp,
            "Вставь и подключайся",
            "Нажми «Вставить из буфера» на главном экране, выбери сервер и жми кнопку подключения.",
            false
        ),
    )
    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]
    val isLast = page == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(current.iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = current.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = current.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (current.showBotButton) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(onClick = onOpenBot) {
                    Icon(
                        painter = painterResource(R.drawable.ic_telegram_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Открыть бота")
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == page) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == page) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                    )
                }
            }

            Button(
                onClick = { if (isLast) onFinish() else page++ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isLast) "Начать" else "Далее")
            }
        }
    }
}
