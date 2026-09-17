package com.tokoreader.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tokoreader.data.local.logging.AppLogger
import com.tokoreader.data.local.logging.LogEntry
import com.tokoreader.data.local.logging.LogLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalLogcatDialog(
    onDismissRequest: () -> Unit
) {
    val logs by AppLogger.logs.collectAsState()
    var selectedLevelFilter by remember { mutableStateOf<LogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, selectedLevelFilter, searchQuery) {
        logs.filter { log ->
            val matchesLevel = selectedLevelFilter == null || log.level == selectedLevelFilter
            val matchesQuery = searchQuery.isBlank() || 
                log.tag.contains(searchQuery, ignoreCase = true) || 
                log.message.contains(searchQuery, ignoreCase = true)
            matchesLevel && matchesQuery
        }.reversed() // Show newest logs first for dynamic monitoring
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Logcat Internal",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Memonitor koneksi dan aktivitas API Tokocrypto",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val logText = AppLogger.getLogsAsText()
                            if (logText.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(logText))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Semua log disalin ke clipboard")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Log kosong")
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Salin Semua Log",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            AppLogger.clear()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Semua log berhasil dihapus")
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Log",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Toolbar Pencarian dan Filter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari tag, pesan, atau rute...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips Filter Level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LogLevelChip(
                            label = "ALL",
                            isSelected = selectedLevelFilter == null,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = { selectedLevelFilter = null }
                        )
                        LogLevelChip(
                            label = "ERROR",
                            isSelected = selectedLevelFilter == LogLevel.ERROR,
                            color = Color(0xFFE57373),
                            onClick = { selectedLevelFilter = LogLevel.ERROR }
                        )
                        LogLevelChip(
                            label = "WARN",
                            isSelected = selectedLevelFilter == LogLevel.WARN,
                            color = Color(0xFFFFB74D),
                            onClick = { selectedLevelFilter = LogLevel.WARN }
                        )
                        LogLevelChip(
                            label = "INFO",
                            isSelected = selectedLevelFilter == LogLevel.INFO,
                            color = Color(0xFF64B5F6),
                            onClick = { selectedLevelFilter = LogLevel.INFO }
                        )
                        LogLevelChip(
                            label = "DEBUG",
                            isSelected = selectedLevelFilter == LogLevel.DEBUG,
                            color = Color(0xFF81C784),
                            onClick = { selectedLevelFilter = LogLevel.DEBUG }
                        )
                    }
                }

                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada log yang terekam.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(filteredLogs) { log ->
                            LogItemRow(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogLevelChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun LogItemRow(log: LogEntry) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f

    val levelColor = when (log.level) {
        LogLevel.ERROR -> Color(0xFFEF5350)
        LogLevel.WARN -> Color(0xFFFF9800)
        LogLevel.INFO -> Color(0xFF2196F3)
        LogLevel.DEBUG -> Color(0xFF4CAF50)
    }

    val containerColor = when (log.level) {
        LogLevel.ERROR -> if (isDark) Color(0xFF2C1A1A) else Color(0xFFFFF1F1)
        LogLevel.WARN -> if (isDark) Color(0xFF2E2416) else Color(0xFFFFF9E6)
        else -> if (isDark) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
    }

    val tagColor = when (log.level) {
        LogLevel.ERROR -> if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
        LogLevel.WARN -> if (isDark) Color(0xFFFFD180) else Color(0xFFEF6C00)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val messageColor = when (log.level) {
        LogLevel.ERROR -> if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C)
        LogLevel.WARN -> if (isDark) Color(0xFFFFECB3) else Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .border(
                width = 0.5.dp,
                color = levelColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(levelColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = log.level.name,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = log.tag,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = tagColor
                )
            }
            Text(
                text = log.timestamp,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = log.message,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = messageColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
