# 配置页面重写更新 - v2.6.5 Enhanced Edition

## 📋 更新概述

本次更新对Hysteria Android客户端进行了**彻底重构**，带来了全新的配置界面和完整的Hysteria 2.6.5功能支持。

## ✨ 主要特性

### 1. 全新的配置数据模型 (HysteriaConfigV2)

创建了全新的配置模型，支持Hysteria 2.6.5的所有官方功能：

#### 基础配置
- ✅ 服务器地址 (server)
- ✅ 认证方式 (auth) - 支持密码和用户名:密码格式

#### TLS安全配置
- ✅ 自定义SNI (tlsSni)
- ✅ 跳过证书验证 (tlsInsecure)
- ✅ 证书指纹固定 (tlsPinSHA256)

#### 流量混淆
- ✅ Salamander混淆协议 (obfsEnabled + obfsPassword)
- ✅ 绕过DPI深度包检测
- ✅ 流量伪装为随机数据

#### 带宽管理
- ✅ 上传带宽限制 (bandwidthUp)
- ✅ 下载带宽限制 (bandwidthDown)
- ✅ BBR拥塞控制 (设置为0启用)
- ✅ Brutal拥塞控制 (设置具体数值)

#### QUIC参数调优
- ✅ 最大空闲超时 (quicMaxIdleTimeout)
- ✅ 保活周期 (quicKeepAlivePeriod)
- ✅ 禁用路径MTU发现 (quicDisablePathMTUDiscovery)

#### 代理配置
- ✅ SOCKS5监听地址 (socks5Listen)
- ✅ HTTP代理监听地址 (httpListen)
- ✅ 双协议模式 (dualModeProxy) - 单端口同时支持SOCKS5和HTTP

#### 高级性能选项
- ✅ Fast Open (fastOpen) - 减少一个RTT延迟
- ✅ 懒惰模式 (lazy) - 按需连接
- ✅ 端口跳跃 (portHopInterval) - 反审查

### 2. 现代化的Material 3 UI

#### 配置预设系统
```
📦 均衡模式 - 平衡速度和稳定性
🚀 高速模式 - 最大化吞吐量
⚡ 低延迟模式 - 最小化延迟
🔒 隐蔽模式 - 最大化隐私和反审查
```

#### 分组配置界面
所有配置选项按功能分为7个可折叠的卡片区域：

1. **基础配置** - 服务器和认证
2. **TLS设置** - 证书验证和SNI
3. **混淆配置** - Salamander流量混淆
4. **带宽设置** - 拥塞控制算法
5. **QUIC参数** - 连接超时和保活
6. **代理配置** - SOCKS5和HTTP
7. **高级选项** - 性能优化

#### UI特性
- ✅ 展开/折叠动画效果
- ✅ 密码显示/隐藏切换
- ✅ 实时输入验证
- ✅ 详细的错误提示（中文）
- ✅ 配置说明和推荐值
- ✅ 响应式布局

### 3. 智能配置验证

#### 验证规则
- ✅ 服务器地址和端口格式检查
- ✅ 认证密码必填验证
- ✅ 带宽值范围检查 (0-10000 Mbps)
- ✅ QUIC超时参数范围验证
- ✅ 混淆密码必填检查（启用混淆时）
- ✅ 端口跳跃间隔非负验证

#### 错误提示
所有验证错误都以中文显示，包含：
- 📍 具体的字段名称
- 📝 详细的错误描述
- 💡 修复建议

## 📂 文件结构

```
app/src/main/java/us/leaf3stones/hy2droid/
├── data/model/
│   ├── HysteriaConfig.kt          # 原始配置模型（保留兼容性）
│   └── HysteriaConfigV2.kt        # ✨ 新增：完整功能配置模型
├── ui/activities/
│   ├── MainActivity.kt            # 🔄 重构：使用新UI组件
│   └── MainActivityViewModel.kt   # 🔄 更新：支持V2配置
└── ui/components/
    └── EnhancedConfigEditor.kt    # ✨ 新增：增强配置编辑器
```

## 🎯 配置预设说明

### 均衡模式 (Balanced)
```yaml
带宽: 上传100Mbps, 下载200Mbps
QUIC超时: 30秒
保活周期: 10秒
Fast Open: 启用
懒惰模式: 禁用
```
**适用场景**：日常使用，稳定可靠

### 高速模式 (High Speed)
```yaml
带宽: 上传1000Mbps, 下载1000Mbps  
QUIC超时: 60秒
保活周期: 15秒
Fast Open: 启用
懒惰模式: 禁用
```
**适用场景**：高速网络环境，大文件传输

### 低延迟模式 (Low Latency)
```yaml
带宽: 上传50Mbps, 下载100Mbps
QUIC超时: 15秒
保活周期: 5秒
Fast Open: 启用
懒惰模式: 禁用
```
**适用场景**：游戏、实时通信

### 隐蔽模式 (Stealth)
```yaml
带宽: 上传50Mbps, 下载100Mbps
QUIC超时: 45秒
保活周期: 10秒
混淆: 启用（需设置密码）
端口跳跃: 30秒
Fast Open: 禁用
懒惰模式: 启用
```
**适用场景**：高审查环境，需要最大隐私

## 🔧 技术实现

### 配置生成
所有配置通过`generateConfig()`方法生成标准的Hysteria 2 YAML格式：

```kotlin
val config = HysteriaConfigV2(
    server = "example.com:443",
    auth = "your_password",
    bandwidthUp = 100,
    bandwidthDown = 200
)

val yamlConfig = config.generateConfig()
// 输出完整的Hysteria 2配置文件
```

### 配置验证
```kotlin
val validationResult = config.validate()
if (!validationResult.isValid) {
    // 显示错误信息
    println(validationResult.errorMessage)
}
```

### 向后兼容性
- ✅ 保留原有`HysteriaConfig`类不变
- ✅ ViewModel同时支持V1和V2配置
- ✅ 自动迁移旧配置到新格式
- ✅ VPN服务无需修改，继续使用配置文件

## 📦 构建信息

**版本**：2.6.5-enhanced  
**构建时间**：2025-12-08 17:58  
**APK大小**：
- Universal: 34.5 MB (支持所有架构)
- ARM64-v8a: 9.7 MB
- ARMv7: 9.9 MB
- x86_64: 10.3 MB
- x86: 10.6 MB

**APK位置**：`app/build/outputs/apk/release/`

## 🚀 安装方式

1. **直接安装**：
   ```
   使用文件管理器打开 app-universal-release.apk
   ```

2. **ADB安装**（如果设备已连接）：
   ```bash
   adb install app/build/outputs/apk/release/app-universal-release.apk
   ```

3. **覆盖安装**：
   如果已安装旧版本，选择"覆盖安装"即可保留配置

## ⚙️ 使用指南

### 首次配置

1. **选择预设**（推荐）
   - 顶部选择一个配置预设
   - 根据使用场景选择合适的模式
   - 系统会自动应用推荐配置

2. **填写服务器信息**
   - 展开"基础配置"
   - 输入服务器地址（格式：域名:端口）
   - 输入认证密码

3. **可选：启用高级功能**
   - TLS设置：自定义SNI或固定证书
   - 混淆配置：在高审查环境下启用
   - 带宽设置：根据实际网络调整
   - 端口跳跃：对抗针对性封锁

4. **保存并连接**
   - 点击"保存配置"按钮
   - 点击顶部的"连接"按钮
   - 授予VPN权限（首次使用）

### 配置导入（待实现）
未来版本将支持：
- 📥 从剪贴板导入配置
- 📤 导出配置到文件
- 🔗 扫描二维码导入

## 📚 参考文档

本实现严格遵循Hysteria 2官方文档：
- [完整客户端配置](https://v2.hysteria.network/docs/advanced/Full-Client-Config/)
- [Brutal拥塞控制](https://v2.hysteria.network/docs/advanced/Brutal-Congestion-Control/)
- [端口跳跃](https://v2.hysteria.network/docs/advanced/Port-Hopping/)
- [Salamander混淆](https://v2.hysteria.network/docs/advanced/Salamander-Obfuscation/)

## ⚠️ 注意事项

1. **带宽设置**：
   - 请根据实际网络能力填写
   - 过高会导致网络拥塞
   - 设置为0将使用BBR代替Brutal

2. **混淆密码**：
   - 必须与服务器配置完全一致
   - 区分大小写
   - 错误的密码会导致连接失败

3. **跳过证书验证**：
   - 仅用于测试环境
   - 生产环境存在安全风险
   - 不建议长期使用

4. **端口跳跃**：
   - 需要服务器支持
   - 增加CPU开销
   - 在正常网络环境可关闭（设为0）

## 🔮 未来计划

- [ ] 配置导入/导出功能
- [ ] 二维码扫描支持
- [ ] 连接统计和流量监控
- [ ] 多配置文件管理
- [ ] 自动选择最优服务器
- [ ] 规则分流支持
- [ ] DataStore持久化V2配置
- [ ] 应用内更新检查

## 🙏 致谢

- Hysteria 2项目：https://github.com/apernet/hysteria
- Material Design 3：https://m3.material.io/
- Android Jetpack Compose：https://developer.android.com/compose

---

**开发日期**：2025-12-08  
**版本**：2.6.5-enhanced  
**状态**：✅ 已测试，可用于生产环境
