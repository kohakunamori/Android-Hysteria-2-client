package us.leaf3stones.hy2droid.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import us.leaf3stones.hy2droid.proxy.LogManager
import us.leaf3stones.hy2droid.ui.theme.Hy2droidTheme

class LogViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hy2droidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LogViewerScreen(
                        onBackPressed = { finish() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logs by LogManager.logs.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var autoScroll by remember { mutableStateOf(true) }
    
    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (autoScroll && logs.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Hysteria 日志") },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                // Auto-scroll toggle
                IconButton(onClick = { autoScroll = !autoScroll }) {
                    Icon(
                        if (autoScroll) Icons.Default.Close else Icons.Default.Clear,
                        contentDescription = if (autoScroll) "禁用自动滚动" else "启用自动滚动",
                        tint = if (autoScroll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                // Clear logs
                IconButton(onClick = { LogManager.clearLogs() }) {
                    Icon(Icons.Default.Clear, contentDescription = "清除日志")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Log stats bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "总计: ${logs.size} 条",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (autoScroll) "🔵 自动滚动" else "⚪ 手动滚动",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Log content
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📋",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "暂无日志",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "连接VPN后将显示Hysteria运行日志",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E)),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(logs) { logEntry ->
                    LogItem(logEntry)
                }
            }
        }
    }
}

@Composable
fun LogItem(logEntry: LogManager.LogEntry) {
    val backgroundColor = when (logEntry.level) {
        LogManager.LogLevel.ERROR -> Color(0xFF4D1F1F)
        LogManager.LogLevel.WARN -> Color(0xFF4D3D1F)
        LogManager.LogLevel.INFO -> Color(0xFF1F2D4D)
        LogManager.LogLevel.DEBUG -> Color(0xFF1F4D2D)
        LogManager.LogLevel.VERBOSE -> Color(0xFF2D2D2D)
    }
    
    val textColor = when (logEntry.level) {
        LogManager.LogLevel.ERROR -> Color(0xFFFF6B6B)
        LogManager.LogLevel.WARN -> Color(0xFFFFB86C)
        LogManager.LogLevel.INFO -> Color(0xFF8BE9FD)
        LogManager.LogLevel.DEBUG -> Color(0xFF50FA7B)
        LogManager.LogLevel.VERBOSE -> Color(0xFFBDBDBD)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = backgroundColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Timestamp
            Text(
                text = logEntry.timestamp,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.width(80.dp)
            )
            
            // Level
            Text(
                text = logEntry.level.name.first().toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = textColor,
                modifier = Modifier.width(16.dp)
            )
            
            // Message
            Text(
                text = logEntry.message,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
