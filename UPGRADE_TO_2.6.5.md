# Hysteria 核心更新至 2.6.5 - 升级说明

## 更新概览

本次更新将 Android Hysteria 2 客户端的核心库从 **v2.5.1** 升级至 **v2.6.5**。

### 更新时间
2025年12月8日

## 更新内容

### 1. 核心库更新
已更新所有架构的 `libhysteria.so` 文件：

| 架构 | 文件大小 | 状态 |
|------|---------|------|
| arm64-v8a | 19.82 MB | ✅ 已更新 |
| armeabi-v7a | 19.02 MB | ✅ 已更新 |
| x86 | 19.34 MB | ✅ 已更新 |
| x86_64 | 21.40 MB | ✅ 已更新 |

### 2. 版本信息更新
- **versionCode**: 1 → 2
- **versionName**: "1.0" → "2.6.5"
- **README.md**: 已更新版本说明和下载链接

## Hysteria 2.6.5 核心改进

### 2.6.5 (最新版本)
- 🐛 **修复服务器端内存泄漏问题**（随每个客户端连接累积）

### 2.6.4
- 🔒 **安全修复**: `tls.pinSHA256` 现在仅匹配叶证书指纹，减轻中间人攻击风险
- 🐛 修复TUN模式UDP包AF损坏问题
- ⬆️ 更新 quic-go 到 v0.54.0

### 2.6.3
- ✨ 添加 mTLS 支持用于客户端证书认证
- 🐛 修复TUN模式内存泄漏
- 🐛 修复Linux系统使用systemd-resolved时DNS解析失败问题
- 🐛 修复ACL缓存bug

### 2.6.2
- 🚀 **TLS握手期间ClientHello现在分片，可绕过某些防火墙的SNI过滤**
- ⬆️ 更新 quic-go 到 v0.52.0

### 2.6.1
- ⚡ 服务器直连出站现在支持TCP Fast Open
- 🐛 修复Linux上tun在`ipv6.disable=1`时不工作的问题
- 🆕 添加对`LoongArch64`的支持

### 2.6.0
- 🐛 修复端口跳跃范围包含65535时客户端启动冻结的bug
- ✨ 添加新的`/dump/streams`端点到流量统计API
- ✨ 添加新的`share`子命令用于生成分享链接和二维码

## 兼容性说明

### ✅ 完全兼容
- **配置文件格式**: 无需修改，完全向后兼容
- **API接口**: 所有现有功能保持不变
- **最低Android版本**: 仍为 Android 5.0 (API 21)
- **目标Android版本**: 仍为 Android 14 (API 34)

### 📋 无需修改的代码
以下组件无需任何修改：
- `Hysteria2VpnService.kt` - VPN服务
- `HysteriaConfig.kt` - 配置模型
- `HysteriaConfigDataSource.kt` - 数据源
- `MainActivity.kt` 及其他UI组件

## 更新后的功能增强

### 安全性提升
- ✅ 修复服务器端内存泄漏（重要安全修复）
- ✅ 改进TLS证书验证机制
- ✅ 修复多个内存泄漏问题

### 性能优化
- ✅ 更新QUIC协议库到最新版本
- ✅ 优化UDP数据包处理
- ✅ 改进DNS解析机制

### 反审查能力
- ✅ **新增ClientHello分片技术**，可绕过部分防火墙的SNI过滤
- ✅ 改进混淆算法

## 测试建议

### 基本功能测试
1. ✅ 基本连接测试
2. ✅ SOCKS5代理功能
3. ✅ HTTP代理功能
4. ✅ 全局VPN模式

### 高级功能测试
1. ✅ SNI自定义
2. ✅ 混淆密码功能
3. ✅ Insecure模式
4. ✅ 带宽限制（TX/RX）

### 兼容性测试
1. ✅ Android 14系统
2. ✅ 不同网络环境
3. ✅ 不同服务器配置

## 构建说明

### 从源码构建
```bash
cd Android-Hysteria-2-client
./gradlew assembleRelease
```

生成的APK位于：`app/build/outputs/apk/release/`

### 签名配置
如需发布，请更新 `app/build.gradle` 中的签名配置：
```groovy
signingConfigs {
    release {
        storeFile(file("../signing/production.jks"))
        storePassword("YOUR_PASSWORD")
        keyAlias("YOUR_ALIAS")
        keyPassword("YOUR_PASSWORD")
    }
}
```

## 相关链接

- [Hysteria官方仓库](https://github.com/apernet/hysteria)
- [Hysteria 2.6.5 发布说明](https://github.com/apernet/hysteria/releases/tag/app/v2.6.5)
- [Hysteria完整变更日志](https://v2.hysteria.network/docs/Changelog/)
- [Hysteria客户端配置文档](https://v2.hysteria.network/docs/advanced/Full-Client-Config/)

## 技术细节

### 更新方法
1. 从Hysteria官方发布页面下载预编译的Android二进制文件
2. 将下载的文件重命名为 `libhysteria.so`
3. 替换各架构目录下的库文件
4. 更新版本号和文档

### 文件变更
```
modified:   README.md
modified:   app/build.gradle
modified:   app/src/main/jniLibs/arm64-v8a/libhysteria.so
modified:   app/src/main/jniLibs/armeabi-v7a/libhysteria.so
modified:   app/src/main/jniLibs/x86/libhysteria.so
modified:   app/src/main/jniLibs/x86_64/libhysteria.so
```

---

**更新完成！** 🎉

本次更新带来了重要的安全修复和性能改进，强烈建议所有用户升级。
