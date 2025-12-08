# Android-Hysteria-2-client

## 介绍

自用 android hysteria 2 客户端，基于[hysteria 2.6.5](https://github.com/apernet/hysteria/releases/tag/app/v2.6.5)构建. 支持 arm64-v8a、armeabi-v7a、x86、x86_64 架构，在 android 14上测试可用.

## 下载
- [2.6.5](https://github.com/kohakunamori/Android-Hysteria-2-client/releases/tag/v2.6.5) - 更新至Hysteria核心2.6.5版本
- [2.4.0](https://github.com/lry127/Android-Hysteria-2-client/releases/tag/v2.4.0r-2)


## 支持功能

- 基本hysteria 2客户端，支持暴露 socks5和http端口，支持全局代理

- 自定义sni

- 支持[混淆](https://v2.hysteria.network/zh/docs/advanced/Full-Client-Config/#_4)

- 支持 insecure

- 默认开启 fastopen以降低rrt

- 针对移动设备优化电量消耗

## 截图

![screenshot 1](./screenshots/s1.png)

## 使用介绍

- server address: 服务器域名或者ip

- auth: 密码

- sni: tls 的sni部分，可以不填，默认从server address中提取. 也可以与server address不同，此时server address仅作为报文对端发送地址，实际tls握手的sni由这个字段指定

- obfuscation password: 混淆密码，如服务器未启用混淆，将其留白即可

- tx 上传速率，以mbps为单位

- rx 下载速率，以mbps为单位

- 注意：tx和rx必须按真实值填写否则会带来性能损失，若都填0则会关闭hysteria的Brutal拥堵控制协议，转用BBR，参考[官方文档](https://v2.hysteria.network/zh/docs/advanced/Full-Client-Config/#_5)

- allow insecure: 允许不安全证书，通常不应开启，否则可能导致中间人攻击

## 更新日志

### v2.6.5 (2025-12-08)
- ✅ 更新Hysteria核心到2.6.5版本
- ✅ 修复服务器端内存泄漏问题 (来自Hysteria官方)
- ✅ 安全修复：TLS证书指纹验证改进 (来自Hysteria官方)
- ✅ 修复TUN模式UDP包问题 (来自Hysteria官方)
- ✅ 支持ClientHello分片以绕过部分防火墙SNI过滤 (来自Hysteria官方)
- ✅ 更新quic-go到v0.54.0 (来自Hysteria官方)

### v2.5.1
UI基于Jetpack Compose实现但并不完备，不过基础功能已经实现，若需使用请使用下载[pre-release](https://github.com/lry127/Android-Hysteria-2-client/releases/tag/v2.5.1.2). 详见此[issue](https://github.com/lry127/Android-Hysteria-2-client/issues/2). ![新版截图](./screenshots/s2.png)

## 关于

- 感谢 [hysteria](https://github.com/apernet/hysteria) 提供协议与底层实现. [LICENSE](https://github.com/apernet/hysteria/blob/master/LICENSE.md)

- 感谢 [eycorsican/go-tun2socks](https://github.com/eycorsican/go-tun2socks) 提供代理实现. [LICENSE](https://github.com/eycorsican/go-tun2socks/blob/master/LICENSE)

本应用仅限学习交流使用，使用请遵守当地法规。