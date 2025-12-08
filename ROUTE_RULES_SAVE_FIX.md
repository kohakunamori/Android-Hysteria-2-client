# 域名路由规则保存功能修复

**修复日期**: 2025年12月8日  
**问题**: 域名路由规则配置无法保存到配置中

## 🐛 问题描述

### 原始问题
用户在域名路由规则界面中添加、编辑或修改规则后,返回主界面时所有修改都会丢失。规则配置无法持久化保存。

### 根本原因
1. `RouteRulesActivity` 只在内存中管理状态,没有将修改保存回主配置
2. 缺少从主界面传递现有配置到路由规则界面的机制
3. 缺少从路由规则界面回传修改结果到主界面的机制
4. `MainActivityViewModel` 缺少更新路由规则的方法

## ✅ 修复内容

### 1. RouteRulesActivity 改进

#### 添加保存功能
```kotlin
companion object {
    const val EXTRA_CONFIG_ID = "CONFIG_ID"
    const val EXTRA_CONFIG_NAME = "CONFIG_NAME"
    const val EXTRA_ROUTE_RULES = "ROUTE_RULES"
    const val RESULT_ROUTE_RULES = "RESULT_ROUTE_RULES"
}
```

#### 加载现有配置
```kotlin
val existingRules = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    intent.getSerializableExtra(EXTRA_ROUTE_RULES, RouteRulesConfig::class.java)
} else {
    @Suppress("DEPRECATION")
    intent.getSerializableExtra(EXTRA_ROUTE_RULES) as? RouteRulesConfig
}
```

#### 保存修改结果
```kotlin
onSave = { rules ->
    val resultIntent = Intent().apply {
        putExtra(RESULT_ROUTE_RULES, rules)
    }
    setResult(RESULT_OK, resultIntent)
    finish()
}
```

### 2. 添加保存按钮和变更追踪

#### TopBar 保存按钮
```kotlin
actions = {
    // Save button
    IconButton(
        onClick = { onSave(routeRulesConfig) },
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
    // Preset button...
}
```

#### 变更追踪
在所有修改操作中添加 `hasChanges = true`:
- 启用/禁用路由规则
- 修改默认模式
- 添加/编辑/删除规则
- 切换规则启用状态
- 应用预设规则

#### 视觉反馈
```kotlin
colors = TopAppBarDefaults.topAppBarColors(
    containerColor = if (hasChanges) {
        MaterialTheme.colorScheme.primaryContainer  // 有变更时高亮
    } else {
        MaterialTheme.colorScheme.surface
    }
)
```

### 3. MainActivity 集成

#### 添加 Result Launcher
```kotlin
val routeRulesLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val routeRules = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra(
                    RouteRulesActivity.RESULT_ROUTE_RULES, 
                    RouteRulesConfig::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra(
                    RouteRulesActivity.RESULT_ROUTE_RULES
                ) as? RouteRulesConfig
            }
            routeRules?.let {
                viewModel.updateRouteRules(it)
            }
        }
    }
```

#### 传递现有配置
```kotlin
OutlinedButton(
    onClick = {
        val intent = Intent(context, RouteRulesActivity::class.java).apply {
            putExtra(RouteRulesActivity.EXTRA_CONFIG_ID, state.configDataV2.id)
            putExtra(RouteRulesActivity.EXTRA_CONFIG_NAME, state.configDataV2.name)
            putExtra(RouteRulesActivity.EXTRA_ROUTE_RULES, state.configDataV2.routeRules)
        }
        routeRulesLauncher.launch(intent)
    },
    ...
)
```

### 4. MainActivityViewModel 更新

#### 添加更新路由规则方法
```kotlin
fun updateRouteRules(routeRules: RouteRulesConfig) {
    val updatedConfig = _state.value.configDataV2.copy(routeRules = routeRules)
    _state.update {
        it.copy(configDataV2 = updatedConfig)
    }
    // Update in-memory storage
    allConfigs[updatedConfig.id] = updatedConfig
    Log.d("MainActivityViewModel", "Route rules updated for config: ${updatedConfig.id}")
}
```

## 🎯 修复效果

### 修复前 ❌
1. 用户在路由规则界面添加规则
2. 点击返回按钮
3. 规则全部丢失
4. 生成的 Hysteria 配置中没有 ACL 规则

### 修复后 ✅
1. 用户在路由规则界面添加规则
2. TopBar 变为高亮状态,显示有未保存的变更
3. 点击 ✓ 保存按钮
4. 自动返回主界面
5. 规则已保存到配置中
6. 生成的 Hysteria 配置包含正确的 ACL 规则
7. 下次进入路由规则界面时,之前的配置仍然存在

## 🎨 UI 改进

### 保存状态指示
- **未保存**: TopBar 正常颜色,保存按钮灰色且禁用
- **有变更**: TopBar 高亮显示,保存按钮可用且蓝色高亮
- **已保存**: 自动返回主界面

### 用户体验
1. **明确的保存流程**: 用户知道何时需要保存
2. **防止误操作**: 未保存时可以点击返回放弃修改
3. **视觉反馈**: TopBar 颜色变化提示有未保存的变更
4. **快捷操作**: 点击 ✓ 即可保存并返回

## 📋 测试步骤

### 测试场景 1: 添加新规则
1. 进入主界面
2. 点击"规则"按钮
3. 点击 + 添加规则
4. 输入域名: `*.example.com`
5. 选择模式: 直连
6. 点击保存规则
7. 观察 TopBar 变为高亮
8. 点击 TopBar 的 ✓ 按钮
9. 返回主界面
10. 点击"连接"生成配置
11. 在日志中查看生成的配置应包含 ACL 规则

### 测试场景 2: 编辑现有规则
1. 进入路由规则界面
2. 点击现有规则的菜单
3. 选择"编辑"
4. 修改域名或模式
5. 保存
6. 点击 TopBar 的 ✓ 按钮
7. 退出并重新进入
8. 验证修改已保存

### 测试场景 3: 使用预设规则
1. 进入路由规则界面
2. 点击 ⭐ 预设按钮
3. 选择"中国大陆直连"
4. 观察 TopBar 变为高亮
5. 点击 ✓ 保存
6. 退出并重新进入
7. 验证预设规则已保存

### 测试场景 4: 放弃修改
1. 进入路由规则界面
2. 添加或修改规则
3. 观察 TopBar 高亮
4. 点击返回按钮(不点击保存)
5. 退出并重新进入
6. 验证修改未保存

## 🔧 技术细节

### 数据流
```
MainActivity (配置) 
    ↓ (Intent Extra)
RouteRulesActivity (编辑)
    ↓ (Result Intent)
MainActivity (接收结果)
    ↓ (调用 ViewModel)
MainActivityViewModel (更新配置)
    ↓
内存存储 (allConfigs)
```

### Serializable 支持
`RouteRulesConfig` 实现了 `Serializable` 接口,可以通过 Intent 传递:
```kotlin
data class RouteRulesConfig(...) : Serializable
```

### Android 版本兼容
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    // Android 13+ 使用新 API
    intent.getSerializableExtra(key, RouteRulesConfig::class.java)
} else {
    // Android 12 及以下使用旧 API
    @Suppress("DEPRECATION")
    intent.getSerializableExtra(key) as? RouteRulesConfig
}
```

## 📦 构建信息

### Debug APK
- ✅ 编译成功
- 保存功能已实现
- 可用于测试

### Release APK
- ✅ 编译成功
- 已签名
- 可用于发布

### 文件大小
```
app-arm64-v8a-release.apk      9.33 MB
app-armeabi-v7a-release.apk    9.57 MB
app-x86_64-release.apk         9.94 MB
app-x86-release.apk           10.19 MB
app-universal-release.apk     33.05 MB
```

## 🚀 后续优化建议

### 1. 持久化存储
当前配置仅保存在内存中,应用重启后会丢失。建议实现:
- DataStore 持久化存储
- JSON 文件导出/导入
- SharedPreferences 备份

### 2. 自动保存
考虑添加自动保存功能:
- 退出界面时自动保存
- 定时自动保存
- 可配置的自动保存选项

### 3. 撤销/重做
添加撤销重做功能:
- 支持撤销最近的修改
- 保存修改历史
- 快捷操作

### 4. 配置同步
多配置管理时的规则同步:
- 在配置间复制规则
- 规则模板共享
- 批量应用规则

## 📝 修改文件列表

```
修改的文件:
- RouteRulesActivity.kt        (添加保存功能、变更追踪、配置传递)
- MainActivity.kt               (添加 Result Launcher、传递配置)
- MainActivityViewModel.kt      (添加 updateRouteRules 方法)

新增常量:
- RouteRulesActivity.EXTRA_CONFIG_ID
- RouteRulesActivity.EXTRA_CONFIG_NAME
- RouteRulesActivity.EXTRA_ROUTE_RULES
- RouteRulesActivity.RESULT_ROUTE_RULES
```

## ✅ 总结

修复完成了域名路由规则无法保存的关键问题:

✅ **配置传递**: 从主界面正确传递现有配置  
✅ **结果回传**: 通过 Result API 回传修改后的配置  
✅ **变更追踪**: 实时跟踪用户修改  
✅ **保存按钮**: 明确的保存操作和视觉反馈  
✅ **数据更新**: ViewModel 正确更新配置  
✅ **兼容性**: 支持 Android 13+ 和旧版本  

用户现在可以安全地添加、编辑和保存域名路由规则,配置会正确应用到 Hysteria VPN 连接中! 🎉
