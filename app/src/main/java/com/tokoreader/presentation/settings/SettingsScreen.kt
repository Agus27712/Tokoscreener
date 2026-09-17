package com.tokoreader.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.TokoReaderApp
import com.tokoreader.domain.model.ApiCredentials
import com.tokoreader.presentation.settings.components.ApiCredentialsDialog
import com.tokoreader.presentation.settings.components.ModernChip
import com.tokoreader.presentation.settings.components.ModernToggleRow
import com.tokoreader.presentation.settings.components.SettingsCardContainer
import com.tokoreader.presentation.settings.components.StrategySelectionCard
import com.tokoreader.presentation.settings.components.SystemStatusHeaderCard
import com.tokoreader.presentation.settings.components.ThemeOptionPill
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsRepository = remember(context) { (context.applicationContext as TokoReaderApp).container.settingsRepository }
    
    val savedStrategy by settingsRepository.getTradingMode().collectAsState(initial = "Scalping")
    var strategyMode by remember(savedStrategy) { mutableStateOf(savedStrategy) }

    val savedTheme by settingsRepository.getThemeMode().collectAsState(initial = "Dark Navy")
    var theme by remember(savedTheme) { mutableStateOf(savedTheme) }

    val savedAccent by settingsRepository.getAccentColor().collectAsState(initial = "Electric Blue")
    var accent by remember(savedAccent) { mutableStateOf(savedAccent) }

    val apiCredentials by settingsRepository.getApiCredentials().collectAsState(initial = ApiCredentials("", ""))
    val isApiConfigured = apiCredentials.apiKey.isNotBlank() && apiCredentials.secret.isNotBlank()

    val savedRealBuyMode by settingsRepository.getRealBuyMode().collectAsState(initial = false)
    var realBuyMode by remember(savedRealBuyMode) { mutableStateOf(savedRealBuyMode) }

    // Live preference states
    var aiProvider by remember { mutableStateOf("Gemini") }
    var throttle by remember { mutableStateOf("200 ms") }
    var priceAlerts by remember { mutableStateOf(true) }
    var tradeNotifications by remember { mutableStateOf(true) }
    var showWipeDialog by remember { mutableStateOf(false) }
    var showApiDialog by remember { mutableStateOf(false) }

    if (showApiDialog) {
        ApiCredentialsDialog(
            initialApiKey = apiCredentials.apiKey,
            initialSecret = apiCredentials.secret,
            onDismiss = { showApiDialog = false },
            onSave = { key, secret ->
                coroutineScope.launch {
                    settingsRepository.saveApiCredentials(key, secret)
                }
                showApiDialog = false
            }
        )
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wipe Semua Kredensial?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "Semua API Key Tokocrypto, API Secret, PIN keamanan, dan data cache trading akan dihapus secara permanen dari perangkat ini.",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            settingsRepository.clearCredentials()
                        }
                        showWipeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Hapus Bersih", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWipeDialog = false }) {
                    Text("Batal")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pengaturan & Preferensi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Kustomisasi terminal, sinyal & koneksi",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO SYSTEM STATUS CARD
            item {
                SystemStatusHeaderCard()
            }

            // 2. STRATEGY & ENGINE PRESET
            item {
                SettingsCardContainer(
                    title = "STRATEGY & SINYAL ENGINE",
                    icon = Icons.Outlined.TrendingUp
                ) {
                    Text(
                        text = "Pilih model evaluasi sinyal trading",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // 3 Strategy Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StrategySelectionCard(
                            title = "Scalping",
                            subtitle = "1m - 5m TF",
                            icon = Icons.Filled.Bolt,
                            isSelected = strategyMode == "Scalping",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                strategyMode = "Scalping"
                                coroutineScope.launch { settingsRepository.saveTradingMode("Scalping") }
                            }
                        )
                        StrategySelectionCard(
                            title = "Intraday",
                            subtitle = "15m - 1h TF",
                            icon = Icons.Filled.Timeline,
                            isSelected = strategyMode == "Intraday",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                strategyMode = "Intraday"
                                coroutineScope.launch { settingsRepository.saveTradingMode("Intraday") }
                            }
                        )
                        StrategySelectionCard(
                            title = "Swing",
                            subtitle = "4h - 1d TF",
                            icon = Icons.Filled.ShowChart,
                            isSelected = strategyMode == "Swing",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                strategyMode = "Swing"
                                coroutineScope.launch { settingsRepository.saveTradingMode("Swing") }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // AI Provider Selection
                    Text(
                        text = "AI Market Assistant Provider",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModernChip(
                            label = "✨ Google Gemini 1.5",
                            isSelected = aiProvider == "Gemini",
                            modifier = Modifier.weight(1f),
                            onClick = { aiProvider = "Gemini" }
                        )
                        ModernChip(
                            label = "⚡ Groq Llama 3",
                            isSelected = aiProvider == "Groq",
                            modifier = Modifier.weight(1f),
                            onClick = { aiProvider = "Groq" }
                        )
                    }
                }
            }

            // 3. TAMPILAN & TEMA (VISUAL STYLING)
            item {
                SettingsCardContainer(
                    title = "TAMPILAN & PALET WARNA",
                    icon = Icons.Outlined.Palette
                ) {
                    Text(
                        text = "Theme Antarmuka Terminal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Theme Selector Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionPill(
                            title = "Dark Navy",
                            previewBg = Color(0xFF0F172A),
                            isSelected = theme == "Dark Navy",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                theme = "Dark Navy"
                                coroutineScope.launch { settingsRepository.saveThemeMode("Dark Navy") }
                            }
                        )
                        ThemeOptionPill(
                            title = "OLED Pure",
                            previewBg = Color(0xFF000000),
                            isSelected = theme == "OLED",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                theme = "OLED"
                                coroutineScope.launch { settingsRepository.saveThemeMode("OLED") }
                            }
                        )
                        ThemeOptionPill(
                            title = "Cyberpunk",
                            previewBg = Color(0xFF13091B),
                            isSelected = theme == "Cyberpunk",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                theme = "Cyberpunk"
                                coroutineScope.launch { settingsRepository.saveThemeMode("Cyberpunk") }
                            }
                        )
                        ThemeOptionPill(
                            title = "Light Clean",
                            previewBg = Color(0xFFF8FAFC),
                            isSelected = theme == "Light",
                            isLightText = false,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                theme = "Light"
                                coroutineScope.launch { settingsRepository.saveThemeMode("Light") }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aksen Warna Primer",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Accent Swatches Row
                    val accentOptions = listOf(
                        "Electric Blue" to Color(0xFF38BDF8),
                        "Neon Cyan" to Color(0xFF06B6D4),
                        "Cyber Gold" to Color(0xFFF59E0B),
                        "Emerald Green" to Color(0xFF10B981),
                        "Vibrant Purple" to Color(0xFFA855F7),
                        "Sunset Orange" to Color(0xFFF97316)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        accentOptions.forEach { (name, color) ->
                            val isSelected = accent == name
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        accent = name
                                        coroutineScope.launch { settingsRepository.saveAccentColor(name) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aksen aktif: $accent",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 4. MARKET DATA & WEBSOCKET ENGINE
            item {
                SettingsCardContainer(
                    title = "FEED HARGA & WEBSOCKET",
                    icon = Icons.Outlined.Speed
                ) {
                    Text(
                        text = "Price Feed Throttle Rate",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mengatur interval refresh orderbook dan ticker data",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val throttleOptions = listOf("Raw 0 ms", "100 ms", "200 ms", "500 ms")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        throttleOptions.forEach { opt ->
                            ModernChip(
                                label = opt,
                                isSelected = throttle == opt,
                                modifier = Modifier.weight(1f),
                                onClick = { throttle = opt }
                            )
                        }
                    }
                }
            }

            // 5. KEAMANAN & API TOKOCRYPTO
            item {
                SettingsCardContainer(
                    title = "KEAMANAN & KREDENSIAL API",
                    icon = Icons.Outlined.Security
                ) {
                    // Real Buy Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Mode Transaksi Real",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (realBuyMode) SuccessGreen.copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.2f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                 ) {
                                    Text(
                                        text = if (realBuyMode) "LIVE" else "DEMO",
                                        color = if (realBuyMode) SuccessGreen else Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (realBuyMode) "Order dikirim langsung ke Tokocrypto Spot" else "Paper trade simulasi saldo virtual",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = realBuyMode,
                            onCheckedChange = {
                                realBuyMode = it
                                coroutineScope.launch { settingsRepository.setRealBuyMode(it) }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // API Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "API Key Tokocrypto",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (isApiConfigured) "Tersimpan aman ••••••••${apiCredentials.apiKey.takeLast(4)}" else "Belum dikonfigurasi",
                                    fontSize = 11.sp,
                                    color = if (isApiConfigured) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFF59E0B)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isApiConfigured) SuccessGreen.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isApiConfigured) "Terhubung" else "Belum Disetel",
                                color = if (isApiConfigured) SuccessGreen else Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Configure API Button
                    Button(
                        onClick = { showApiDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isApiConfigured) "Ubah API Key & Secret" else "Atur API Key & Secret",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wipe Action Button
                    OutlinedButton(
                        onClick = { showWipeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wipe Kredensial & Reset Data", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 6. NOTIFIKASI
            item {
                SettingsCardContainer(
                    title = "NOTIFIKASI TRADING",
                    icon = Icons.Outlined.Notifications
                ) {
                    ModernToggleRow(
                        title = "Price Alert Trigger",
                        subtitle = "Notifikasi saat koin radar tembus target",
                        checked = priceAlerts,
                        onCheckedChange = { priceAlerts = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModernToggleRow(
                        title = "Eksekusi Order Notification",
                        subtitle = "Alert instan saat order buy/sell match",
                        checked = tradeNotifications,
                        onCheckedChange = { tradeNotifications = it }
                    )
                }
            }

            // 7. FOOTER APP INFO
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tokocrypto Spot Terminal v2.4",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Algorithmic RSI, MACD, Bollinger & Depth Pressure Engine",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
