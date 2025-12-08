package us.leaf3stones.hy2droid.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import us.leaf3stones.hy2droid.data.model.*
import us.leaf3stones.hy2droid.ui.theme.Hy2droidTheme

class RouteRulesActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_CONFIG_ID = "CONFIG_ID"
        const val EXTRA_CONFIG_NAME = "CONFIG_NAME"
        const val EXTRA_ROUTE_RULES = "ROUTE_RULES"
        const val RESULT_ROUTE_RULES = "RESULT_ROUTE_RULES"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get config data from intent
        val configId = intent.getStringExtra(EXTRA_CONFIG_ID) ?: ""
        val configName = intent.getStringExtra(EXTRA_CONFIG_NAME) ?: "配置"
        val existingRules = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_ROUTE_RULES, RouteRulesConfig::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_ROUTE_RULES) as? RouteRulesConfig
        }
        
        setContent {
            Hy2droidTheme {
                RouteRulesScreen(
                    configId = configId,
                    configName = configName,
                    initialRules = existingRules,
                    onSave = { rules ->
                        // Return result to MainActivity
                        val resultIntent = Intent().apply {
                            putExtra(RESULT_ROUTE_RULES, rules)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteRulesScreen(
    configId: String,
    configName: String,
    initialRules: RouteRulesConfig?,
    onSave: (RouteRulesConfig) -> Unit,
    onBack: () -> Unit
) {
    // State management
    var routeRulesConfig by remember { 
        mutableStateOf(initialRules ?: RouteRulesConfig.createDefault(configId))
    }
    var hasChanges by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RouteRule?>(null) }
    var showValidationErrors by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("域名路由规则")
                        Text(
                            text = configName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // Save button
                    IconButton(
                        onClick = {
                            onSave(routeRulesConfig)
                        },
                        enabled = hasChanges
                    ) {
                        Icon(
                            Icons.Default.Check,
                            "保存",
                            tint = if (hasChanges) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                    }
                    // Preset button
                    IconButton(onClick = { showPresetDialog = true }) {
                        Icon(Icons.Default.Star, "预设规则")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (hasChanges) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, "添加规则")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Configuration card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Enable switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "启用路由规则",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "根据域名控制流量走向",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = routeRulesConfig.enabled,
                            onCheckedChange = { 
                                routeRulesConfig = routeRulesConfig.copy(enabled = it)
                                hasChanges = true
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Default mode selection
                    Text(
                        text = "默认模式",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RouteMode.values().forEach { mode ->
                            FilterChip(
                                selected = routeRulesConfig.defaultMode == mode,
                                onClick = { 
                                    routeRulesConfig = routeRulesConfig.copy(defaultMode = mode)
                                    hasChanges = true
                                },
                                label = { Text(getRouteModeText(mode)) },
                                leadingIcon = if (routeRulesConfig.defaultMode == mode) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    
                    // Stats
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("总计", routeRulesConfig.rules.size.toString())
                        StatItem("已启用", routeRulesConfig.rules.count { it.enabled }.toString())
                        StatItem("白名单", routeRulesConfig.rules.count { it.mode == RouteMode.DIRECT }.toString())
                        StatItem("黑名单", routeRulesConfig.rules.count { it.mode == RouteMode.BLOCK }.toString())
                    }
                }
            }
            
            // Rules list
            if (routeRulesConfig.rules.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "暂无路由规则",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "点击右下角 + 按钮添加规则",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showPresetDialog = true }) {
                            Icon(Icons.Default.Star, "预设", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("使用预设规则")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(routeRulesConfig.rules) { index, rule ->
                        RouteRuleItem(
                            rule = rule,
                            onToggle = { 
                                val updated = routeRulesConfig.rules.toMutableList()
                                updated[index] = rule.copy(enabled = !rule.enabled)
                                routeRulesConfig = routeRulesConfig.copy(rules = updated)
                                hasChanges = true
                            },
                            onEdit = { editingRule = rule },
                            onDelete = {
                                val updated = routeRulesConfig.rules.toMutableList()
                                updated.removeAt(index)
                                routeRulesConfig = routeRulesConfig.copy(rules = updated)
                                hasChanges = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add/Edit dialog
    if (showAddDialog || editingRule != null) {
        RouteRuleEditDialog(
            rule = editingRule,
            onDismiss = { 
                showAddDialog = false
                editingRule = null
            },
            onSave = { newRule ->
                if (editingRule != null) {
                    // Edit existing rule
                    val index = routeRulesConfig.rules.indexOfFirst { it.id == newRule.id }
                    if (index >= 0) {
                        val updated = routeRulesConfig.rules.toMutableList()
                        updated[index] = newRule
                        routeRulesConfig = routeRulesConfig.copy(rules = updated)
                    }
                } else {
                    // Add new rule
                    routeRulesConfig = routeRulesConfig.copy(
                        rules = routeRulesConfig.rules + newRule
                    )
                }
                hasChanges = true
                showAddDialog = false
                editingRule = null
            }
        )
    }
    
    // Preset dialog
    if (showPresetDialog) {
        PresetRulesDialog(
            configId = configId,
            onDismiss = { showPresetDialog = false },
            onSelect = { preset ->
                routeRulesConfig = preset
                hasChanges = true
                showPresetDialog = false
            }
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RouteRuleItem(
    rule: RouteRule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enable switch
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 12.dp)
            )
            
            // Rule info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mode badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getRouteModeColor(rule.mode)
                    ) {
                        Text(
                            text = getRouteModeText(rule.mode),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Domain
                    Text(
                        text = rule.domain,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (rule.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rule.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "菜单")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { 
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) 
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteRuleEditDialog(
    rule: RouteRule?,
    onDismiss: () -> Unit,
    onSave: (RouteRule) -> Unit
) {
    var domain by remember { mutableStateOf(rule?.domain ?: "") }
    var mode by remember { mutableStateOf(rule?.mode ?: RouteMode.PROXY) }
    var description by remember { mutableStateOf(rule?.description ?: "") }
    var enabled by remember { mutableStateOf(rule?.enabled ?: true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule == null) "添加路由规则" else "编辑路由规则") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Domain input
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("域名") },
                    placeholder = { Text("例如: *.example.com") },
                    supportingText = { 
                        Text("支持通配符: *.example.com 或 .example.com") 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Mode selection
                Text(
                    text = "路由模式",
                    style = MaterialTheme.typography.labelMedium
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RouteMode.values().forEach { routeMode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mode = routeMode }
                                .background(
                                    if (mode == routeMode) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == routeMode,
                                onClick = { mode = routeMode }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = getRouteModeText(routeMode),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = getRouteModeDescription(routeMode),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述 (可选)") },
                    placeholder = { Text("为这条规则添加说明") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (domain.isNotBlank()) {
                        onSave(
                            RouteRule(
                                id = rule?.id ?: java.util.UUID.randomUUID().toString(),
                                domain = domain.trim(),
                                mode = mode,
                                description = description.trim(),
                                enabled = enabled
                            )
                        )
                    }
                },
                enabled = domain.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun PresetRulesDialog(
    configId: String,
    onDismiss: () -> Unit,
    onSelect: (RouteRulesConfig) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择预设规则") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetOption(
                    title = "绝大多数直连",
                    description = "中国大陆IP、网站及局域网直连，默认模式直连。适合只代理特定域名的场景",
                    onClick = {
                        onSelect(RouteRulePresets.mostDirect(configId))
                    }
                )
                
                PresetOption(
                    title = "中国大陆直连",
                    description = "常见的中国大陆域名走直连,其他走代理",
                    onClick = {
                        onSelect(RouteRulePresets.chinaDirect(configId))
                    }
                )
                
                PresetOption(
                    title = "拦截广告追踪",
                    description = "拦截常见的广告和追踪域名",
                    onClick = {
                        onSelect(RouteRulePresets.blockAds(configId))
                    }
                )
                
                PresetOption(
                    title = "中国直连 + 拦截广告",
                    description = "组合规则: 中国大陆直连且拦截广告",
                    onClick = {
                        onSelect(RouteRulePresets.chinaDirectAndBlockAds(configId))
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun PresetOption(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getRouteModeText(mode: RouteMode): String {
    return when (mode) {
        RouteMode.DIRECT -> "直连"
        RouteMode.PROXY -> "代理"
        RouteMode.BLOCK -> "拦截"
    }
}

fun getRouteModeDescription(mode: RouteMode): String {
    return when (mode) {
        RouteMode.DIRECT -> "绕过代理,直接连接 (白名单)"
        RouteMode.PROXY -> "通过代理连接"
        RouteMode.BLOCK -> "阻止访问 (黑名单)"
    }
}

fun getRouteModeColor(mode: RouteMode): Color {
    return when (mode) {
        RouteMode.DIRECT -> Color(0xFF4CAF50) // Green
        RouteMode.PROXY -> Color(0xFF2196F3) // Blue
        RouteMode.BLOCK -> Color(0xFFF44336) // Red
    }
}
