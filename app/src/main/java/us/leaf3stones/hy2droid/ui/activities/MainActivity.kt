package us.leaf3stones.hy2droid.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import us.leaf3stones.hy2droid.ui.components.EnhancedConfigEditor
import us.leaf3stones.hy2droid.ui.theme.Hy2droidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hy2droidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainActivityViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    val vpnRequestLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.startVpnService(context)
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with connection status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isVpnConnected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Hysteria 2.6.5",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.isVpnConnected) "已连接" else "未连接",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.isVpnConnected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (state.isVpnConnected) {
                            viewModel.stopVpnService(context)
                        } else {
                            val prepIntent = VpnService.prepare(context)
                            if (prepIntent != null) {
                                vpnRequestLauncher.launch(prepIntent)
                            } else {
                                viewModel.startVpnService(context)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isVpnConnected)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (state.isVpnConnected) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isVpnConnected) "断开连接" else "连接")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Log Viewer Button
            OutlinedButton(
                onClick = {
                    val intent = Intent(context, LogViewerActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("日志")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Enhanced Configuration Editor
        EnhancedConfigEditor(
            config = state.configDataV2,
            configList = state.configList,
            onConfigChange = viewModel::onConfigV2Changed,
            onConfigSelect = viewModel::onConfigSelected,
            onConfigDelete = viewModel::onConfigDeleted,
            onConfigDuplicate = viewModel::onConfigDuplicated,
            onSave = viewModel::onConfigV2Confirmed,
            modifier = Modifier.weight(1f)
        )
        
        // Validation error dialog
        if (state.shouldShowConfigInvalidReminder) {
            AlertDialog(
                onDismissRequest = { viewModel.onConfigInvalidReminderDismissed() },
                confirmButton = {
                    TextButton(onClick = { viewModel.onConfigInvalidReminderDismissed() }) {
                        Text("确定")
                    }
                },
                title = { Text("配置无效") },
                text = { Text(state.validationError ?: "配置数据不完整，请检查必填项。") }
            )
        }
    }
}