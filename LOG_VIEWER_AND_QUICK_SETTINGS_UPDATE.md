# Hysteria 2 Android Client - 日志查看器和快速设置更新

**更新日期**: 2025年12月8日  
**版本**: v2.6.5 + 日志查看器 + 快速设置磁贴

## 📋 更新内容

### 1. 日志查看器界面 (LogViewerActivity)

#### 功能特性
- ✅ **实时日志显示**: 显示 Hysteria VPN 服务的所有日志输出
- ✅ **日志级别颜色编码**: 
  - 🔴 **ERROR**: 红色
  - 🟠 **WARN**: 橙色
  - 🔵 **INFO**: 蓝色
  - 🟢 **DEBUG**: 绿色
  - ⚪ **VERBOSE**: 灰色
- ✅ **自动滚动开关**: 可选择是否自动滚动到最新日志
- ✅ **清空日志**: 一键清空所有日志记录
- ✅ **统计信息**: 实时显示日志总数
- ✅ **空状态提示**: 无日志时显示友好提示

#### 界面位置
主界面 → 点击 **"查看日志"** 按钮

#### 实现细节
- **文件**: `app/src/main/java/us/leaf3stones/hy2droid/ui/activities/LogViewerActivity.kt`
- **日志管理**: `app/src/main/java/us/leaf3stones/hy2droid/proxy/LogManager.kt`
- **最大日志数**: 1000 条 (自动清理旧日志)
- **响应式更新**: 使用 StateFlow 实现实时日志流

---

### 2. 快速设置磁贴 (Quick Settings Tile)

#### 功能特性
- ✅ **控制中心快捷开关**: 从系统控制中心快速启动/停止 VPN
- ✅ **动态状态显示**: 
  - **激活状态**: 磁贴高亮,显示 "VPN 已连接"
  - **未激活状态**: 磁贴灰色,显示 "VPN 已断开"
- ✅ **点击操作**:
  - VPN 未连接时: 点击打开主界面
  - VPN 已连接时: 点击停止 VPN
- ✅ **实时状态同步**: 自动监听 VPN 状态变化并更新磁贴

#### 添加磁贴方法
1. 打开系统控制中心 (下拉通知栏)
2. 点击 **编辑按钮** (铅笔图标)
3. 找到 **"Hysteria 2"** 磁贴
4. 拖动到快速设置区域
5. 完成!

#### 实现细节
- **文件**: `app/src/main/java/us/leaf3stones/hy2droid/tile/Hysteria2VpnTileService.kt`
- **最低 Android 版本**: Android 7.0 (API 24)
- **权限**: `android.permission.BIND_QUICK_SETTINGS_TILE`

---

## 🔧 技术实现

### 日志系统架构

#### LogManager (单例日志管理器)
```kotlin
object LogManager {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs
    
    enum class LogLevel { ERROR, WARN, INFO, DEBUG, VERBOSE }
    
    data class LogEntry(
        val timestamp: String,
        val level: LogLevel,
        val message: String
    )
    
    fun log(message: String) // INFO 级别
    fun error(message: String) // ERROR 级别
    fun warn(message: String) // WARN 级别
    fun debug(message: String) // DEBUG 级别
    fun verbose(message: String) // VERBOSE 级别
    fun clearLogs() // 清空日志
}
```

#### VPN 服务日志集成
在 `Hysteria2VpnService` 中集成了以下日志点:
- **启动操作**: 记录 VPN 启动请求
- **停止操作**: 记录 VPN 停止和资源清理
- **重复启动警告**: 检测并警告重复启动请求
- **配置加载**: 记录配置路径和验证状态
- **隧道建立**: 记录 VPN 隧道建立成功
- **Hysteria 核心输出**: 实时解析并分类 Hysteria 2.6.5 的日志输出
- **错误处理**: 记录所有异常和清理错误

#### Hysteria 日志解析
```kotlin
when {
    nextLog.contains("ERROR", ignoreCase = true) || 
    nextLog.contains("FATAL", ignoreCase = true) -> 
        LogManager.error("[Hysteria] $nextLog")
    nextLog.contains("WARN", ignoreCase = true) -> 
        LogManager.warn("[Hysteria] $nextLog")
    nextLog.contains("DEBUG", ignoreCase = true) -> 
        LogManager.debug("[Hysteria] $nextLog")
    else -> 
        LogManager.verbose("[Hysteria] $nextLog")
}
```

---

### 快速设置磁贴架构

#### Hysteria2VpnTileService
```kotlin
class Hysteria2VpnTileService : TileService(), VpnStatusObserver {
    override fun onStartListening() {
        // 磁贴可见时更新状态
        Hysteria2VpnService.addObserver(this)
        updateTile()
    }
    
    override fun onClick() {
        // 处理点击: 停止 VPN 或打开主界面
    }
    
    override fun onVpnStarted() {
        // VPN 启动时更新磁贴为激活状态
        updateTile()
    }
    
    override fun onVpnStopped() {
        // VPN 停止时更新磁贴为未激活状态
        updateTile()
    }
    
    private fun updateTile() {
        qsTile?.apply {
            state = if (isVpnActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Hysteria 2"
            subtitle = if (isVpnActive) "VPN 已连接" else "VPN 已断开"
            updateTile()
        }
    }
}
```

#### AndroidManifest 注册
```xml
<service
    android:name=".tile.Hysteria2VpnTileService"
    android:exported="true"
    android:icon="@drawable/ic_launcher"
    android:label="Hysteria 2"
    android:permission="android.permission.BIND_QUICK_SETTINGS_TILE">
    <intent-filter>
        <action android:name="android.service.quicksettings.action.QS_TILE" />
    </intent-filter>
</service>
```

---

## 📦 构建信息

### 构建结果
- ✅ **Debug APK**: 构建成功
- ✅ **Release APK**: 构建成功并签名

### Release APK 列表
```
app-arm64-v8a-release.apk      9.28 MB   (64位 ARM 设备)
app-armeabi-v7a-release.apk    9.51 MB   (32位 ARM 设备)
app-x86_64-release.apk         9.89 MB   (64位 x86 设备/模拟器)
app-x86-release.apk           10.14 MB   (32位 x86 设备/模拟器)
app-universal-release.apk     33.00 MB   (通用版本,包含所有架构)
```

### 编译警告 (不影响功能)
- `android:extractNativeLibs` 属性已过时
- `startActivityAndCollapse(Intent!)` 方法已过时 (Android API 变更)
- `ArrowBack: ImageVector` 建议使用自动镜像版本
- `Divider` 组件已重命名为 `HorizontalDivider`

---

## 🎯 使用指南

### 查看日志
1. 启动 Hysteria 2 应用
2. 点击主界面的 **"查看日志"** 按钮
3. 查看实时日志输出:
   - 红色日志表示错误
   - 橙色日志表示警告
   - 蓝色日志表示信息
   - 绿色日志表示调试信息
   - 灰色日志表示详细输出
4. 使用顶部按钮:
   - **自动滚动开关**: 切换是否自动滚动到最新日志
   - **清空日志按钮**: 清除所有日志记录

### 使用快速设置磁贴
1. **添加磁贴到控制中心**:
   - 下拉通知栏,打开控制中心
   - 点击编辑按钮 (通常是铅笔图标)
   - 找到 "Hysteria 2" 磁贴
   - 拖动到快速设置区域
   
2. **使用磁贴**:
   - **VPN 未连接时**: 点击磁贴打开主界面
   - **VPN 已连接时**: 点击磁贴立即停止 VPN
   - **状态指示**: 磁贴颜色和副标题显示当前 VPN 状态

### 日志级别说明
- **ERROR** 🔴: 严重错误,导致功能无法正常工作
- **WARN** 🟠: 警告信息,可能影响性能或功能
- **INFO** 🔵: 重要信息,记录关键操作
- **DEBUG** 🟢: 调试信息,帮助开发者诊断问题
- **VERBOSE** ⚪: 详细输出,包括 Hysteria 核心的所有日志

---

## 🔍 故障排查

### 日志查看器问题

**Q: 日志查看器显示为空?**
- A: 确保 VPN 服务已启动。日志仅在服务运行时生成。

**Q: 自动滚动不工作?**
- A: 检查自动滚动开关是否开启。如果手动滚动了列表,开关会自动关闭。

**Q: 日志太多,性能变慢?**
- A: 点击"清空日志"按钮。系统会自动保留最新 1000 条日志。

### 快速设置磁贴问题

**Q: 找不到 Hysteria 2 磁贴?**
- A: 
  1. 确保应用已安装
  2. 确认 Android 版本 ≥ 7.0 (API 24)
  3. 重启应用后重新查找

**Q: 磁贴点击无反应?**
- A: 
  1. 检查应用是否有 VPN 权限
  2. 确认配置已正确设置
  3. 查看日志了解详细错误

**Q: 磁贴状态不更新?**
- A: 
  1. 下拉通知栏刷新
  2. 重新添加磁贴
  3. 重启设备

---

## 📝 技术说明

### 新增文件
```
app/src/main/java/us/leaf3stones/hy2droid/
├── proxy/
│   └── LogManager.kt                          # 日志管理器
├── tile/
│   └── Hysteria2VpnTileService.kt            # 快速设置磁贴服务
└── ui/
    └── activities/
        └── LogViewerActivity.kt               # 日志查看器界面
```

### 修改文件
```
app/src/main/
├── AndroidManifest.xml                        # 注册新服务和Activity
└── java/us/leaf3stones/hy2droid/
    ├── proxy/
    │   └── Hysteria2VpnService.kt            # 集成 LogManager
    └── ui/
        └── activities/
            └── MainActivity.kt                # 添加日志查看器按钮
```

### 依赖项
无需添加新的外部依赖。所有功能使用以下现有库实现:
- **Jetpack Compose**: UI 框架
- **Material 3**: UI 组件库
- **Kotlin Coroutines**: 异步处理
- **StateFlow**: 响应式状态管理
- **Android TileService API**: 快速设置集成

---

## 🚀 未来计划

### 日志系统增强
- [ ] 日志导出功能 (保存为文本文件)
- [ ] 日志搜索和过滤
- [ ] 日志级别筛选器
- [ ] 日志统计分析 (错误计数、警告趋势)

### 快速设置增强
- [ ] 长按显示配置选择
- [ ] 磁贴显示连接速度
- [ ] 多磁贴支持 (不同配置)

### 性能优化
- [ ] 日志分页加载
- [ ] 虚拟滚动优化
- [ ] 内存占用优化

---

## 📄 更新历史

### v2.6.5 + 日志查看器 + 快速设置 (2025-12-08)
- ✅ 新增实时日志查看器界面
- ✅ 新增快速设置磁贴支持
- ✅ 集成 LogManager 到 VPN 服务
- ✅ Hysteria 2.6.5 核心日志解析
- ✅ 5 级日志分类和颜色编码
- ✅ 自动滚动和日志清理功能
- ✅ 动态磁贴状态更新
- ✅ VPN 状态观察者模式实现

### v2.6.5 + 多配置 (2025-12-08)
- ✅ 多配置管理系统
- ✅ 端口范围支持 (30000-50000 格式)
- ✅ 配置选择、删除、复制、重命名
- ✅ 移除配置预设

### v2.6.5 + 配置重写 (2025-12-08)
- ✅ 完整配置页面重写
- ✅ 15+ 配置参数支持
- ✅ Material 3 设计语言
- ✅ 可折叠配置分组

### v2.6.5 初始版本 (2025-12-08)
- ✅ Hysteria 核心更新到 2.6.5
- ✅ 支持所有架构 (arm64-v8a, armeabi-v7a, x86, x86_64)
- ✅ Release 签名配置

---

## 📞 支持与反馈

如有问题或建议,请:
1. 查看日志查看器中的错误信息
2. 检查本文档的故障排查部分
3. 提交 GitHub Issue 并附带日志截图

---

**祝使用愉快!** 🎉
