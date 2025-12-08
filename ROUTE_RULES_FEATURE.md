# Hysteria 2 Android Client - 域名路由规则功能

**更新日期**: 2025年12月8日  
**版本**: v2.6.5 + 域名路由规则 (ACL)

## 🎯 新增功能

### 域名路由规则 (ACL - Access Control List)

为 Hysteria 2 客户端添加了强大的域名路由规则功能,支持白名单、黑名单和自定义路由策略。

---

## ✨ 功能特性

### 1. **三种路由模式**

#### 🟢 直连模式 (DIRECT - 白名单)
- **用途**: 绕过代理,直接连接
- **适用场景**: 
  - 中国大陆网站 (*.cn, *.baidu.com, *.qq.com)
  - 局域网地址
  - 不需要代理的服务
- **优势**: 降低延迟,节省流量

#### 🔵 代理模式 (PROXY)
- **用途**: 通过 Hysteria 代理连接
- **适用场景**: 
  - 国外网站
  - 需要隐藏IP的服务
  - 突破网络限制
- **优势**: 保护隐私,访问受限资源

#### 🔴 拦截模式 (BLOCK - 黑名单)
- **用途**: 阻止访问特定域名
- **适用场景**: 
  - 广告域名 (*.doubleclick.net)
  - 追踪器 (*.google-analytics.com)
  - 恶意网站
- **优势**: 提升浏览体验,保护隐私

### 2. **域名匹配规则**

#### 支持的域名格式
```
精确匹配:    example.com
通配符匹配:  *.example.com      (匹配 sub.example.com)
后缀匹配:    .example.com       (匹配所有 .example.com 结尾的域名)
顶级域名:    *.cn               (匹配所有 .cn 域名)
```

### 3. **预设规则模板**

#### 📋 中国大陆直连
包含常见的中国大陆域名,走直连路由:
- `*.cn` - 所有 .cn 域名
- `*.com.cn`, `*.gov.cn` - 中国域名
- `*.baidu.com` - 百度
- `*.qq.com` - 腾讯
- `*.taobao.com` - 淘宝
- `*.alipay.com` - 支付宝
- `*.jd.com` - 京东
- `*.weibo.com` - 微博
- `*.163.com` - 网易
- `*.bilibili.com` - 哔哩哔哩
- `*.douyin.com` - 抖音
- `*.xiaomi.com` - 小米

#### 🛡️ 拦截广告追踪
拦截常见的广告和追踪域名:
- `*.doubleclick.net` - Google 广告
- `*.googleadservices.com` - Google 广告服务
- `*.googlesyndication.com` - Google 广告联盟
- `*.facebook.com/tr` - Facebook 追踪
- `*.google-analytics.com` - Google 分析
- `ad.*.com` - 通用广告域名
- `ads.*.com` - 通用广告服务
- `tracker.*.com` - 通用追踪器

#### 🚀 中国直连 + 拦截广告
组合规则,同时实现中国大陆直连和广告拦截

### 4. **规则管理功能**

- ✅ **添加规则**: 自定义域名和路由模式
- ✅ **编辑规则**: 修改现有规则配置
- ✅ **删除规则**: 移除不需要的规则
- ✅ **启用/禁用**: 快速切换规则状态
- ✅ **规则描述**: 为每条规则添加说明
- ✅ **规则排序**: 按添加顺序生效
- ✅ **规则统计**: 显示总计、已启用、白名单、黑名单数量
- ✅ **默认模式**: 设置未匹配域名的默认行为

---

## 🎨 界面设计

### 主界面新增入口
- 位置: 主界面底部,日志按钮旁边
- 按钮: **"规则"** (Settings 图标)
- 点击: 进入域名路由规则管理界面

### 路由规则管理界面

#### 顶部栏
- **标题**: 域名路由规则
- **副标题**: 显示当前配置名称
- **预设按钮**: 快速应用预设规则模板

#### 配置卡片
- **启用开关**: 控制路由规则总开关
- **默认模式选择**: 直连/代理/拦截三选一
- **统计信息**: 
  - 总计: 规则总数
  - 已启用: 启用的规则数
  - 白名单: 直连规则数
  - 黑名单: 拦截规则数

#### 规则列表
每条规则显示:
- **启用开关**: 快速启用/禁用
- **模式标签**: 彩色标签 (绿色=直连, 蓝色=代理, 红色=拦截)
- **域名**: 规则匹配的域名
- **描述**: 规则说明 (可选)
- **菜单按钮**: 编辑、删除操作

#### 添加/编辑对话框
- **域名输入**: 支持通配符提示
- **模式选择**: 单选按钮,包含详细说明
- **描述输入**: 可选的规则说明
- **保存按钮**: 验证通过后启用

#### 预设对话框
三个预设选项卡片:
- 中国大陆直连
- 拦截广告追踪
- 中国直连 + 拦截广告

---

## 🔧 技术实现

### 数据模型

#### RouteMode (路由模式枚举)
```kotlin
enum class RouteMode {
    DIRECT,  // 直连 (白名单)
    PROXY,   // 代理
    BLOCK    // 拦截 (黑名单)
}
```

#### RouteRule (路由规则)
```kotlin
data class RouteRule(
    val id: String,              // 唯一ID
    val domain: String,          // 域名或域名模式
    val mode: RouteMode,         // 路由模式
    val description: String,     // 规则描述
    val enabled: Boolean         // 是否启用
)
```

#### RouteRulesConfig (规则配置)
```kotlin
data class RouteRulesConfig(
    val configId: String,              // 配置ID
    val defaultMode: RouteMode,        // 默认模式
    val rules: List<RouteRule>,        // 规则列表
    val enabled: Boolean               // 是否启用
)
```

### 核心功能

#### 1. 域名匹配算法
```kotlin
fun matches(domain: String): Boolean {
    when {
        // 精确匹配
        pattern == testDomain -> true
        
        // 通配符匹配 (*.example.com)
        pattern.startsWith("*.") -> {
            val baseDomain = pattern.substring(2)
            testDomain.endsWith(baseDomain)
        }
        
        // 后缀匹配 (.example.com)
        pattern.startsWith(".") -> {
            testDomain.endsWith(pattern)
        }
        
        else -> false
    }
}
```

#### 2. ACL 配置生成
```kotlin
fun generateACL(): String {
    val sb = StringBuilder()
    
    // 直连规则 (白名单)
    sb.appendLine("direct:")
    directRules.forEach { rule ->
        sb.appendLine("  - ${rule.domain}")
    }
    
    // 拦截规则 (黑名单)
    sb.appendLine("block:")
    blockRules.forEach { rule ->
        sb.appendLine("  - ${rule.domain}")
    }
    
    return sb.toString()
}
```

#### 3. Hysteria 配置集成
在 `HysteriaConfigV2.generateConfig()` 中:
```kotlin
// Route Rules (ACL)
if (routeRules != null && routeRules.enabled) {
    val aclContent = routeRules.generateACL()
    if (aclContent.isNotBlank()) {
        sb.appendLine("acl:")
        sb.append(aclContent)
    }
}
```

#### 4. 规则验证
```kotlin
fun validate(): RouteRulesValidationResult {
    // 检查空域名
    // 验证域名格式
    // 检查重复域名
    // 检查冲突规则
    
    return RouteRulesValidationResult(
        isValid = errors.isEmpty(),
        errors = errors,
        warnings = warnings
    )
}
```

### 生成的 Hysteria 配置示例

```yaml
server: example.com:443
auth: password123

acl:
  direct:
    # 中国大陆域名直连
    - *.cn
    # 百度
    - *.baidu.com
    # 腾讯
    - *.qq.com
  block:
    # Google 广告
    - *.doubleclick.net
    # Google 分析
    - *.google-analytics.com

socks5:
  listen: 127.0.0.1:1080

http:
  listen: 127.0.0.1:1081
```

---

## 📱 使用指南

### 1. 进入规则管理界面

1. 打开 Hysteria 2 应用
2. 在主界面底部找到 **"规则"** 按钮
3. 点击进入域名路由规则管理

### 2. 启用路由规则

1. 在规则管理界面顶部
2. 打开 **"启用路由规则"** 开关
3. 选择 **默认模式** (建议选择 "代理")

### 3. 使用预设规则 (推荐新手)

1. 点击右上角 **⭐ 预设按钮**
2. 选择适合的预设模板:
   - **中国大陆直连**: 国内网站走直连,国外走代理
   - **拦截广告追踪**: 拦截广告域名
   - **中国直连 + 拦截广告**: 组合使用
3. 预设规则会自动加载并启用

### 4. 添加自定义规则

1. 点击右下角 **+ 按钮**
2. 输入域名 (例如: `*.google.com`)
3. 选择路由模式:
   - **直连**: 绕过代理
   - **代理**: 通过代理
   - **拦截**: 阻止访问
4. 添加描述 (可选)
5. 点击 **"保存"**

### 5. 管理现有规则

#### 启用/禁用规则
- 点击规则卡片左侧的 **开关** 即可

#### 编辑规则
1. 点击规则卡片右侧的 **⋮ 菜单**
2. 选择 **"编辑"**
3. 修改后点击 **"保存"**

#### 删除规则
1. 点击规则卡片右侧的 **⋮ 菜单**
2. 选择 **"删除"**

### 6. 测试规则效果

1. 配置并启用路由规则
2. 连接 VPN
3. 访问测试网站:
   - 直连规则: 访问国内网站 (如 baidu.com)
   - 代理规则: 访问国外网站 (如 google.com)
   - 拦截规则: 尝试访问广告域名 (应被拦截)
4. 在 **"日志"** 界面查看路由匹配情况

---

## 💡 使用场景

### 场景 1: 国内外网站分流

**需求**: 国内网站直连,国外网站走代理

**配置**:
1. 使用预设: **"中国大陆直连"**
2. 设置默认模式: **代理**
3. 效果:
   - 访问 baidu.com → 直连
   - 访问 google.com → 代理

### 场景 2: 拦截广告和追踪

**需求**: 屏蔽广告和追踪域名

**配置**:
1. 使用预设: **"拦截广告追踪"**
2. 设置默认模式: **代理**
3. 效果:
   - 广告请求被拦截
   - 追踪器无法记录

### 场景 3: 特定网站强制代理

**需求**: 某些特定网站必须走代理

**配置**:
1. 添加规则: `*.specificsite.com`
2. 模式: **代理**
3. 设置默认模式: **直连**
4. 效果:
   - 大部分网站直连
   - 指定网站走代理

### 场景 4: 局域网直连

**需求**: 局域网设备直连,不走代理

**配置**:
1. 添加规则:
   - `*.local`
   - `192.168.*.*`
   - `10.*.*.*`
2. 模式: **直连**
3. 效果: 局域网设备快速访问

### 场景 5: 游戏加速

**需求**: 游戏服务器走代理,其他直连

**配置**:
1. 添加规则: `*.game-server.com`
2. 模式: **代理**
3. 设置默认模式: **直连**
4. 效果: 降低游戏延迟

---

## 🎯 最佳实践

### 1. **规则优先级**
- 规则按添加顺序匹配
- 第一个匹配的规则生效
- 未匹配使用默认模式

### 2. **域名格式建议**
```
推荐: *.example.com  (匹配所有子域名)
不推荐: example.com   (仅匹配主域名)

推荐: *.cn           (匹配所有 .cn 域名)
不推荐: .cn          (可能匹配失败)
```

### 3. **性能优化**
- 避免过多规则 (建议 < 100 条)
- 禁用不需要的规则
- 使用通配符减少规则数量

### 4. **安全建议**
- 谨慎使用 **拦截模式**
- 定期更新广告黑名单
- 测试规则避免误杀

### 5. **配置备份**
- 为重要规则添加描述
- 定期导出配置 (计划中)
- 使用多配置功能保存不同方案

---

## 🔍 故障排查

### Q: 规则不生效?

**A**: 检查以下项目:
1. ✅ 路由规则总开关是否启用
2. ✅ 具体规则是否启用
3. ✅ 域名格式是否正确
4. ✅ VPN 是否已连接
5. ✅ 查看日志了解匹配情况

### Q: 无法访问某些网站?

**A**: 可能被规则拦截:
1. 检查是否有 **拦截模式** 规则匹配该域名
2. 临时禁用规则测试
3. 调整规则配置

### Q: 国内网站变慢?

**A**: 可能走了代理:
1. 检查是否缺少 **直连规则**
2. 使用预设: "中国大陆直连"
3. 添加具体域名的直连规则

### Q: 广告仍然显示?

**A**: 广告域名可能未覆盖:
1. 使用预设: "拦截广告追踪"
2. 手动添加广告域名到黑名单
3. 参考常见广告域名列表

### Q: 规则太多,管理困难?

**A**: 优化建议:
1. 合并相似规则 (使用通配符)
2. 禁用不常用规则
3. 使用多配置功能分类管理

---

## 📋 技术细节

### 新增文件

```
app/src/main/java/us/leaf3stones/hy2droid/
├── data/
│   └── model/
│       └── RouteRules.kt              # 路由规则数据模型
└── ui/
    └── activities/
        └── RouteRulesActivity.kt      # 路由规则管理界面
```

### 修改文件

```
app/src/main/
├── AndroidManifest.xml                # 注册 RouteRulesActivity
└── java/us/leaf3stones/hy2droid/
    ├── data/
    │   └── model/
    │       └── HysteriaConfigV2.kt    # 添加 routeRules 字段
    └── ui/
        └── activities/
            └── MainActivity.kt         # 添加规则入口按钮
```

### ACL 配置格式

Hysteria 2.6.5 ACL 格式:
```yaml
acl:
  direct:
    - domain1.com
    - *.domain2.com
  block:
    - ad.domain3.com
    - *.tracker.com
```

### 域名匹配算法

支持以下匹配方式:
- **精确匹配**: `example.com`
- **通配符匹配**: `*.example.com` (匹配 sub.example.com)
- **后缀匹配**: `.example.com` (匹配所有 .example.com 结尾)
- **顶级域名**: `*.cn` (匹配所有 .cn 域名)

---

## 📦 构建信息

### Release APK

```
app-arm64-v8a-release.apk      9.33 MB   (64位 ARM 设备)
app-armeabi-v7a-release.apk    9.57 MB   (32位 ARM 设备)
app-x86_64-release.apk         9.94 MB   (64位 x86 设备/模拟器)
app-x86-release.apk           10.19 MB   (32位 x86 设备/模拟器)
app-universal-release.apk     33.05 MB   (通用版本,包含所有架构)
```

### 编译警告
- `showValidationErrors` 变量未使用 (保留用于未来功能)
- `ArrowBack` 图标已过时 (不影响功能)
- `Divider` 组件已重命名 (不影响功能)

---

## 🚀 未来计划

### 功能增强
- [ ] **导入/导出规则**: JSON 格式规则文件
- [ ] **规则模板分享**: 社区规则模板
- [ ] **IP 地址规则**: 支持 IP 段匹配
- [ ] **正则表达式**: 高级域名匹配
- [ ] **规则组管理**: 按类别分组规则
- [ ] **规则测试工具**: 测试域名匹配情况
- [ ] **规则统计**: 显示匹配次数和流量
- [ ] **自动更新**: 在线规则库自动更新

### UI 改进
- [ ] **拖拽排序**: 规则优先级调整
- [ ] **批量操作**: 批量启用/禁用/删除
- [ ] **搜索过滤**: 快速查找规则
- [ ] **规则预览**: 显示生成的 ACL 配置
- [ ] **暗黑模式**: 界面主题优化

### 性能优化
- [ ] **规则缓存**: 加快匹配速度
- [ ] **异步加载**: 大量规则性能优化
- [ ] **内存优化**: 减少规则占用

---

## 📄 版本历史

### v2.6.5 + 域名路由规则 (2025-12-08)
- ✅ 新增域名路由规则 (ACL) 功能
- ✅ 支持直连、代理、拦截三种模式
- ✅ 支持通配符域名匹配
- ✅ 预设规则模板 (中国直连、拦截广告)
- ✅ 完整的规则管理界面
- ✅ 规则验证和错误提示
- ✅ 集成到 Hysteria 配置生成
- ✅ 规则统计和状态显示

---

## 📚 参考文档

- [Hysteria 2 官方文档](https://v2.hysteria.network/)
- [Hysteria ACL 配置](https://v2.hysteria.network/docs/advanced/ACL/)
- [域名匹配规则说明](https://v2.hysteria.network/docs/advanced/ACL/#domain-matching)

---

## 🎉 总结

域名路由规则功能为 Hysteria 2 Android 客户端带来了强大的流量控制能力:

✅ **灵活的路由策略**: 白名单、黑名单、自定义路由  
✅ **智能分流**: 国内外网站自动分流  
✅ **广告拦截**: 内置广告和追踪器黑名单  
✅ **易于使用**: 预设模板一键配置  
✅ **高度可定制**: 完整的规则管理功能  

享受更快、更安全、更自由的网络体验! 🚀
