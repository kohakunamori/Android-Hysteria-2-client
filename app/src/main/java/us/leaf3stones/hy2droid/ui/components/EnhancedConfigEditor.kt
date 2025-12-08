package us.leaf3stones.hy2droid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import us.leaf3stones.hy2droid.data.model.ConfigListItem
import us.leaf3stones.hy2droid.data.model.HysteriaConfigV2

/**
 * Enhanced Configuration Editor with multiple configs support
 * Based on Hysteria 2.6.5 official documentation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedConfigEditor(
    config: HysteriaConfigV2,
    configList: List<ConfigListItem>,
    onConfigChange: (HysteriaConfigV2) -> Unit,
    onConfigSelect: (String) -> Unit,
    onConfigDelete: (String) -> Unit,
    onConfigDuplicate: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableStateOf<ConfigSection?>(ConfigSection.BASIC) }
    var showConfigList by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Configuration Selector
        ConfigSelector(
            currentConfig = config,
            configList = configList,
            showConfigList = showConfigList,
            onShowConfigListChange = { showConfigList = it },
            onConfigSelect = onConfigSelect,
            onConfigDelete = onConfigDelete,
            onConfigDuplicate = onConfigDuplicate,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Configuration Sections
        ConfigSection.values().forEach { section ->
            ConfigSectionCard(
                section = section,
                isExpanded = expandedSection == section,
                onExpandChange = { expandedSection = if (expandedSection == section) null else section },
                content = {
                    when (section) {
                        ConfigSection.BASIC -> BasicConfigSection(config, onConfigChange)
                        ConfigSection.TLS -> TLSConfigSection(config, onConfigChange)
                        ConfigSection.OBFUSCATION -> ObfuscationConfigSection(config, onConfigChange)
                        ConfigSection.BANDWIDTH -> BandwidthConfigSection(config, onConfigChange)
                        ConfigSection.QUIC -> QUICConfigSection(config, onConfigChange)
                        ConfigSection.PROXY -> ProxyConfigSection(config, onConfigChange)
                        ConfigSection.ADVANCED -> AdvancedConfigSection(config, onConfigChange)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("保存配置")
        }
    }
}

@Composable
fun ConfigSelector(
    currentConfig: HysteriaConfigV2,
    configList: List<ConfigListItem>,
    showConfigList: Boolean,
    onShowConfigListChange: (Boolean) -> Unit,
    onConfigSelect: (String) -> Unit,
    onConfigDelete: (String) -> Unit,
    onConfigDuplicate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentConfig.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = currentConfig.server.ifBlank { "未配置" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onConfigDuplicate) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "复制配置",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { onShowConfigListChange(!showConfigList) }) {
                        Text(
                            if (showConfigList) "▲" else "▼",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Configuration List
            AnimatedVisibility(visible = showConfigList) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "所有配置 (${configList.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    configList.forEach { item ->
                        val isSelected = item.id == currentConfig.id
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else 
                                MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isSelected) onConfigSelect(item.id) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else null
                                    )
                                    Text(
                                        text = item.server.ifBlank { "未配置" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                if (!isSelected && configList.size > 1) {
                                    IconButton(
                                        onClick = { onConfigDelete(item.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigSectionCard(
    section: ConfigSection,
    isExpanded: Boolean,
    onExpandChange: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                supportingContent = {
                    Text(
                        text = section.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingContent = {
                    Icon(section.icon, contentDescription = null)
                },
                trailingContent = {
                    IconButton(onClick = onExpandChange) {
                        Text(if (isExpanded) "▲" else "▼")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// ==================== Configuration Sections ====================

@Composable
fun BasicConfigSection(
    config: HysteriaConfigV2,
    onConfigChange: (HysteriaConfigV2) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = config.name,
            onValueChange = { onConfigChange(config.copy(name = it)) },
            label = { Text("配置名称") },
            placeholder = { Text("我的配置") },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = config.server,
            onValueChange = { onConfigChange(config.copy(server = it)) },
            label = { Text("服务器地址") },
            placeholder = { Text("example.com:443 或 example.com:30000-50000") },
            leadingIcon = { Icon(Icons.Default.Star, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { 
                Text(
                    "支持端口范围格式用于端口跳跃，如: domain.com:30000-50000",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )
        
        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = config.auth,
            onValueChange = { onConfigChange(config.copy(auth = it)) },
            label = { Text("认证密码") },
            placeholder = { Text("your_password") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "👁" else "🔒")
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Text(
            text = "提示：用户名密码认证格式为 username:password",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TLSConfigSection(
    config: HysteriaConfigV2,
    onConfigChange: (HysteriaConfigV2) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = config.tlsSni,
            onValueChange = { onConfigChange(config.copy(tlsSni = it)) },
            label = { Text("SNI (可选)") },
            placeholder = { Text("another.example.com") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("跳过证书验证", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "仅用于测试！生产环境不要开启",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = config.tlsInsecure,
                onCheckedChange = { onConfigChange(config.copy(tlsInsecure = it)) }
            )
        }
        
        OutlinedTextField(
            value = config.tlsPinSHA256,
            onValueChange = { onConfigChange(config.copy(tlsPinSHA256 = it)) },
            label = { Text("证书指纹 (可选)") },
            placeholder = { Text("BA:88:45:17:A1...") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun ObfuscationConfigSection(
    config: HysteriaConfigV2,
    onConfigChange: (HysteriaConfigV2) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("启用混淆 (Salamander)", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "绕过DPI检测，伪装成随机流量",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.obfsEnabled,
                onCheckedChange = { onConfigChange(config.copy(obfsEnabled = it)) }
            )
        }
        
        AnimatedVisibility(visible = config.obfsEnabled) {
            OutlinedTextField(
                value = config.obfsPassword,
                onValueChange = { onConfigChange(config.copy(obfsPassword = it)) },
                label = { Text("混淆密码") },
                placeholder = { Text("cry_me_a_r1ver") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        if (config.obfsEnabled) {
            Text(
                text = "⚠️ 混淆密码必须与服务器配置完全一致！",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun BandwidthConfigSection(
    config: HysteriaConfigV2,
    onConfigChange: (HysteriaConfigV2) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "设置为0将使用BBR拥塞控制而非Brutal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        OutlinedTextField(
            value = config.bandwidthUp.toString(),
            onValueChange = { 
                it.toIntOrNull()?.let { value ->
                    onConfigChange(config.copy(bandwidthUp = value))
                }
            },
            label = { Text("上传带宽 (Mbps)") },
            leadingIcon = { Icon(Icons.Default.Star, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = config.bandwidthDown.toString(),
            onValueChange = {
                it.toIntOrNull()?.let { value ->
                    onConfigChange(config.copy(bandwidthDown = value))
                }
            },
            label = { Text("下载带宽 (Mbps)") },
            leadingIcon = { Icon(Icons.Default.Star, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Text(
            text = "⚠️ 请按实际网络能力填写，过高会导致拥塞！",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun QUICConfigSection(
    config: HysteriaConfigV2,
    onConfigChange: (HysteriaConfigV2) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = config.quicMaxIdleTimeout.toString(),
            onValueChange = {
                it.toIntOrNull()?.let { value ->
                    onConfigChange(config.copy(quicMaxIdleTimeout = value))
                }
            },
            label = { Text("最大空闲超时 (秒)") },
            leadingIcon = { Icon(Icons.Default.Star, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("推荐: 30秒") },
            singleLine = true
        )
        
        OutlinedTextField(
            value = config.quicKeepAlivePeriod.toString(),
            onValueChange = {
                it.toIntOrNull()?.let { value ->
                    onConfigChange(config.copy(quicKeepAlivePeriod = value))
                }
            },
            label = { Text("保活周期 (秒)") },
            leadingIcon = { Icon(Icons.Default.Favorite, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("推荐: 10秒") },
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("禁用路径MTU发现", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "特殊网络环境可能需要",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.quicDisablePathMTUDiscovery,
                onCheckedChange = { onConfigChange(config.copy(quicDisablePathMTUDiscovery = it)) }
            )
        }
    }
}

@Composable
fun ProxyConfigSection(
    config: HysteriaConfigV2,
    onConfigChange: (HysteriaConfigV2) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("双协议模式", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "SOCKS5和HTTP使用同一端口",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.dualModeProxy,
                onCheckedChange = { onConfigChange(config.copy(dualModeProxy = it)) }
            )
        }
        
        OutlinedTextField(
            value = config.socks5Listen,
            onValueChange = { onConfigChange(config.copy(socks5Listen = it)) },
            label = { Text("SOCKS5监听地址") },
            placeholder = { Text("127.0.0.1:1080") },
            leadingIcon = { Icon(Icons.Default.Settings, null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !config.dualModeProxy,
            singleLine = true
        )
        
        if (!config.dualModeProxy) {
            OutlinedTextField(
                value = config.httpListen,
                onValueChange = { onConfigChange(config.copy(httpListen = it)) },
                label = { Text("HTTP代理监听地址") },
                placeholder = { Text("127.0.0.1:1081") },
                leadingIcon = { Icon(Icons.Default.Settings, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            Text(
                text = "双协议模式下，HTTP代理将使用SOCKS5相同端口",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AdvancedConfigSection(
    config: HysteriaConfigV2,
    onConfigChange: (HysteriaConfigV2) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Fast Open", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "减少一个RTT延迟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.fastOpen,
                onCheckedChange = { onConfigChange(config.copy(fastOpen = it)) }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("懒惰模式", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "仅在需要时连接服务器",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.lazy,
                onCheckedChange = { onConfigChange(config.copy(lazy = it)) }
            )
        }
        
        OutlinedTextField(
            value = config.portHopInterval.toString(),
            onValueChange = {
                it.toIntOrNull()?.let { value ->
                    onConfigChange(config.copy(portHopInterval = value))
                }
            },
            label = { Text("端口跳跃间隔 (秒)") },
            leadingIcon = { Icon(Icons.Default.Star, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("0 = 禁用端口跳跃") },
            singleLine = true
        )
    }
}

// ==================== Configuration Section Enum ====================

enum class ConfigSection(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    BASIC("基础配置", "服务器地址和认证信息", Icons.Default.Settings),
    TLS("TLS设置", "证书验证和SNI配置", Icons.Default.Lock),
    OBFUSCATION("混淆配置", "流量伪装和反审查", Icons.Default.Lock),
    BANDWIDTH("带宽设置", "拥塞控制算法配置", Icons.Default.Star),
    QUIC("QUIC参数", "连接超时和保活设置", Icons.Default.Star),
    PROXY("代理配置", "SOCKS5和HTTP代理", Icons.Default.Settings),
    ADVANCED("高级选项", "性能优化和特殊功能", Icons.Default.Settings)
}
