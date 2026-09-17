package com.v2ray.ang.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ProfileItem
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.QRCodeDialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class HomeTab { Home, Providers, Settings }

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    // Full list (empty "Default" pushed to the end) — used on the Providers tab.
    val orderedGroups = remember(uiState.groups) {
        uiState.groups.sortedBy { if (it.id.isEmpty()) 1 else 0 }
    }
    // Only groups that actually have servers — used for the Home tabs, so the
    // empty "Default" group no longer shows up and blocks the real subscription.
    val serverGroups = remember(uiState.groups) {
        orderedGroups.filter { MmkvManager.decodeServerList(it.id).isNotEmpty() }
            .ifEmpty { orderedGroups }
    }
    val hasServers = remember(uiState.groups) {
        orderedGroups.any { MmkvManager.decodeServerList(it.id).isNotEmpty() }
    }
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val displayText = mainViewModel.formatStatus(uiState.status)
    val selectedGuid = uiState.selectedGuid
    val doubleColumnDisplay = uiState.doubleColumnDisplay
    val confirmRemove = uiState.confirmRemove
    val shareQRCodeBitmap = uiState.shareQRCodeBitmap

    val isDarkTheme = LocalDarkTheme.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableStateOf(HomeTab.Home) }

    var shareTarget by remember { mutableStateOf<Triple<String, ProfileItem, Boolean>?>(null) }
    val removeServer: (String) -> Unit = { guid ->
        if (confirmRemove) showRemoveConfirm = guid else onAction(MainAction.RemoveServer(guid))
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { serverGroups.size.coerceAtLeast(1) }
    )

    val lazyListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val lazyGridStates = remember { mutableStateMapOf<String, LazyGridState>() }

    LaunchedEffect(serverGroups) {
        val validGroupIds = serverGroups.map { it.id }.toSet()
        lazyListStates.keys.retainAll(validGroupIds)
        lazyGridStates.keys.retainAll(validGroupIds)
    }

    LaunchedEffect(serverGroups, uiState.selectedGroupId) {
        if (serverGroups.isEmpty()) return@LaunchedEffect
        val selectedIndex = serverGroups.indexOfFirst { it.id == uiState.selectedGroupId }
            .takeIf { it >= 0 } ?: 0
        if (!pagerState.isScrollInProgress && pagerState.settledPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    val latestGroups by rememberUpdatedState(serverGroups)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val currentGroups = latestGroups
                if (page in currentGroups.indices) {
                    onAction(MainAction.SelectGroup(currentGroups[page].id))
                }
            }
    }

    LaunchedEffect(Unit) {
        mainViewModel.removeEmptySubscriptions()
    }

    MainDialogs(
        showDelAllConfirm = showDelAllConfirm,
        onDismissDelAll = { showDelAllConfirm = false },
        onConfirmDelAll = { showDelAllConfirm = false; onAction(MainAction.RemoveAllServers) },
        showDelDuplicateConfirm = showDelDuplicateConfirm,
        onDismissDelDuplicate = { showDelDuplicateConfirm = false },
        onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onAction(MainAction.RemoveDuplicateServers) },
        showDelInvalidConfirm = showDelInvalidConfirm,
        onDismissDelInvalid = { showDelInvalidConfirm = false },
        onConfirmDelInvalid = { showDelInvalidConfirm = false; onAction(MainAction.RemoveInvalidServers) },
        showRemoveConfirm = showRemoveConfirm,
        onDismissRemove = { showRemoveConfirm = null },
        onConfirmRemove = { guid -> showRemoveConfirm = null; onAction(MainAction.RemoveServer(guid)) }
    )

    if (shareTarget != null) {
        val (guid, profile, more) = shareTarget!!
        ShareMethodDialog(
            guid = guid,
            profile = profile,
            more = more,
            onDismiss = { shareTarget = null },
            onAction = onAction,
            onRemove = removeServer,
        )
    }
    if (shareQRCodeBitmap != null) {
        QRCodeDialog(bitmap = shareQRCodeBitmap, onDismiss = { onAction(MainAction.DismissQRCodeDialog) })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            topBar = {
                if (selectedTab == HomeTab.Home) {
                    MainTopBar(
                        isLoading = isLoading,
                        showSearch = showSearch,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { query: String ->
                            searchQuery = query
                            onAction(MainAction.Search(query))
                        },
                        onSearchClose = {
                            searchQuery = ""
                            onAction(MainAction.Search(""))
                            showSearch = false
                        },
                        onSearchToggle = { show: Boolean -> showSearch = show },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onAction = onAction,
                        onMoreMenuAction = { action ->
                            when (action) {
                                MainMoreMenuAction.SubscribeBot -> runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://t.me/maxachkalavpn_bot")
                                        )
                                    )
                                }
                                MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
                                MainMoreMenuAction.DeleteAll -> showDelAllConfirm = true
                                MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.Home,
                        onClick = { selectedTab = HomeTab.Home },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_shield_24dp),
                                contentDescription = null
                            )
                        },
                        label = { Text("Подключение") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.Providers,
                        onClick = { selectedTab = HomeTab.Providers },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_subscriptions_24dp),
                                contentDescription = null
                            )
                        },
                        label = { Text("Сервера") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.Settings,
                        onClick = { selectedTab = HomeTab.Settings },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_settings_24dp),
                                contentDescription = null
                            )
                        },
                        label = { Text("Настройки") }
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    HomeTab.Home -> {
                        val currentGroupId = serverGroups.firstOrNull()?.id ?: ""
                        val homeGroupState by mainViewModel.serverGroupState(currentGroupId)
                            .collectAsStateWithLifecycle()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        if (isDarkTheme) listOf(
                                            Color(0xFF1A1636),
                                            Color(0xFF141029),
                                            Color(0xFF0A0814)
                                        ) else listOf(
                                            Color(0xFFEFEBFF),
                                            Color(0xFFF6F4FF),
                                            Color(0xFFFFFFFF)
                                        )
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(300.dp)
                                    .offset(x = (-40).dp, y = (-70).dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0x557C5CF5), Color(0x00000000))
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 60.dp, y = 10.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0x44506EF0), Color(0x00000000))
                                        )
                                    )
                            )
                            val bgTransition = rememberInfiniteTransition(label = "bg")
                            val bgDrift by bgTransition.animateFloat(
                                initialValue = -8f,
                                targetValue = 8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(9000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bgDrift"
                            )
                            Image(
                                painter = painterResource(R.drawable.ic_bg_mountains),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(1.15f)
                                    .offset(x = bgDrift.dp),
                                contentScale = ContentScale.FillBounds,
                                alpha = if (isDarkTheme) 0.5f else 0.12f
                            )
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BrandTitle(
                                    isRunning = isRunning,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://t.me/maxachkalavpn_bot")
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_telegram_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Бот",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.clickable {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://t.me/Maxachkala_vpn_bot")
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_feedback_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Поддержка",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            ConnectButton(
                                displayText = displayText,
                                isRunning = isRunning,
                                isDarkTheme = isDarkTheme,
                                onToggle = { onAction(MainAction.ToggleService) },
                                onStatusClick = { onAction(MainAction.TestCurrentServer) }
                            )

                            PullToRefreshBox(
                                isRefreshing = isLoading,
                                onRefresh = { onAction(MainAction.UpdateSubscriptions) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                            if (!hasServers) {
                                EmptyStateActions(
                                    onGetViaBot = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://t.me/maxachkalavpn_bot")
                                                )
                                            )
                                        }
                                    },
                                    onPaste = { onAction(MainAction.ImportClipboard) }
                                )
                            }
                            val currentGroup = serverGroups.getOrNull(pagerState.currentPage)
                            val renewUrl = currentGroup?.let { MmkvManager.decodeSubscription(it.id)?.url }
                            if (currentGroup != null && !renewUrl.isNullOrEmpty()) {
                                val subInfo by produceState<SubInfo?>(initialValue = null, key1 = renewUrl) {
                                    value = SubInfoFetcher.fetch(renewUrl)
                                }
                                val renewTransition = rememberInfiniteTransition(label = "renew")
                                val renewPulse by renewTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.06f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(900, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "renewPulse"
                                )
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = subInfo?.profileTitle ?: currentGroup.remarks,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(99.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.scale(renewPulse).clickable {
                                                    runCatching {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, Uri.parse(renewUrl))
                                                        )
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "Продлить",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                                                )
                                            }
                                        }

                                        val userId = subInfo?.userId
                                        if (!userId.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "ID:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = userId,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            runCatching {
                                                                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("id", userId))
                                                            }
                                                        }
                                                )
                                            }
                                        }

                                        val info = subInfo
                                        if (info != null) {
                                            val fraction = if (info.total > 0L) {
                                                (info.used.toFloat() / info.total.toFloat()).coerceIn(0f, 1f)
                                            } else {
                                                0f
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(99.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(fraction)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(99.dp))
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = formatTraffic(info.used, info.total),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = formatExpire(info.expireEpochSec),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        val expireSec = subInfo?.expireEpochSec ?: 0L
                                        if (expireSec > 0L) {
                                            val daysLeft = (expireSec - System.currentTimeMillis() / 1000L) / 86400L
                                            if (daysLeft < 0L) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.errorContainer
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text(
                                                            text = "Подписка истекла",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onErrorContainer
                                                        )
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Surface(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    runCatching {
                                                                        context.startActivity(
                                                                            Intent(
                                                                                Intent.ACTION_VIEW,
                                                                                Uri.parse("https://t.me/maxachkalavpn_bot")
                                                                            )
                                                                        )
                                                                    }
                                                                },
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = MaterialTheme.colorScheme.primary
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(vertical = 12.dp),
                                                                horizontalArrangement = Arrangement.Center,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.ic_telegram_24dp),
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(
                                                                    text = "Продлить в боте",
                                                                    style = MaterialTheme.typography.titleSmall,
                                                                    color = MaterialTheme.colorScheme.onPrimary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (daysLeft <= 3L) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_flash_on_24dp),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (daysLeft == 0L) "Подписка истекает сегодня — продлите заранее"
                                                        else "Подписка истекает через $daysLeft дн. — продлите заранее",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }

                                        val announce = subInfo?.announce
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (!announce.isNullOrBlank()) announce
                                            else "Чтобы продлить подписку — нажмите «Продлить» выше. Откроется страница оплаты со всеми тарифами.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    painterResource(R.drawable.ic_refresh_24dp),
                                    contentDescription = "Обновить",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onAction(MainAction.UpdateSubscriptions) }
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .clickable { onAction(MainAction.TestRealAllServers) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_flash_on_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Проверить все",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            homeGroupState.rows.forEach { row ->
                                ServerRowCompact(
                                    row = row,
                                    isSelected = row.guid == selectedGuid,
                                    onSelect = { onAction(MainAction.SelectServer(row.guid)) },
                                    onMore = { shareTarget = Triple(row.guid, row.profile, true) }
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        }
                        }
                        }
                    }

                    HomeTab.Providers -> ProvidersContent(
                        groups = orderedGroups,
                        onOpenGroup = { id ->
                            onAction(MainAction.SelectGroup(id))
                            selectedTab = HomeTab.Home
                        },
                        onSelectServer = { guid ->
                            onAction(MainAction.SelectServer(guid))
                            selectedTab = HomeTab.Home
                        },
                        onManage = { onNavigate(MainDestination.Subscriptions) }
                    )

                    HomeTab.Settings -> SettingsContent(onNavigate = onNavigate)
                }
            }
        }
    }
}

@Composable
private fun ProvidersContent(
    groups: List<GroupMapItem>,
    onOpenGroup: (String) -> Unit,
    onSelectServer: (String) -> Unit,
    onManage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Сервера",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(R.drawable.ic_add_24dp),
                contentDescription = "Добавить",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { onManage() }
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clickable { onManage() },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Добавить подписку",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val shownGroups = groups.filter { MmkvManager.decodeServerList(it.id).isNotEmpty() }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            shownGroups.forEach { group ->
                val guids = MmkvManager.decodeServerList(group.id)
                item(key = "grp_${group.id}") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clickable { onOpenGroup(group.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_subscriptions_24dp),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (group.id.isEmpty()) "Default" else group.remarks,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${guids.size} серверов",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_24dp),
                                contentDescription = "Настроить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { onManage() }
                            )
                        }
                    }
                }
                itemsIndexed(guids, key = { _, guid -> "srv_$guid" }) { index, guid ->
                    val name = remember(guid) { MmkvManager.decodeServerConfig(guid)?.remarks ?: guid }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 3.dp, bottom = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectServer(guid) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(26.dp)
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item(key = "spc_${group.id}") { Spacer(modifier = Modifier.height(14.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyStateActions(onGetViaBot: () -> Unit, onPaste: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Нет активной подписки",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Получите подписку в боте, затем вставьте ссылку",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGetViaBot() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_telegram_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Получить подписку в боте",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPaste() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_paste_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Вставить из буфера",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun BrandTitle(isRunning: Boolean, modifier: Modifier = Modifier) {
    val text = "Maxachkala VPN"
    if (!isRunning) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = modifier
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "brand")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brandPulse"
    )
    val glow = lerp(Color(0xFF7C6CF5), Color(0xFFFF6B9D), pulse)
    val blur = 5f + pulse * 18f
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            color = Color(0xFFC9BEFF),
            shadow = Shadow(color = glow, offset = Offset.Zero, blurRadius = blur)
        ),
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

private fun formatTraffic(used: Long, total: Long): String {
    val gb = 1024.0 * 1024.0 * 1024.0
    val usedGb = used / gb
    return if (total > 0L) {
        val totalGb = total / gb
        String.format(java.util.Locale.US, "%.1f ГБ / %.1f ГБ", usedGb, totalGb)
    } else {
        String.format(java.util.Locale.US, "%.1f ГБ / ∞", usedGb)
    }
}

private fun formatExpire(expireEpochSec: Long): String {
    if (expireEpochSec <= 0L) return "Бессрочно"
    val nowSec = System.currentTimeMillis() / 1000L
    val days = (expireEpochSec - nowSec) / 86400L
    return if (days >= 0L) "осталось $days дн." else "Истекла"
}

private val settingsGroup1 = listOf(
    MainDestination.PerAppProxy,
    MainDestination.Routing,
    MainDestination.UserAssets,
    MainDestination.Settings,
)

private val settingsGroup2 = listOf(
    MainDestination.Logcat,
    MainDestination.BackupRestore,
    MainDestination.CheckUpdate,
    MainDestination.About,
)

@Composable
private fun SettingsContent(onNavigate: (MainDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(items = settingsGroup1, onNavigate = onNavigate)
        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(items = settingsGroup2, onNavigate = onNavigate)
    }
}

@Composable
private fun SettingsCard(
    items: List<MainDestination>,
    onNavigate: (MainDestination) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(item) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}
