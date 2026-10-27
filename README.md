# Shadowrocket for Android (SR-Android)

基于 iOS 版 **Shadowrocket 2.2.92 (Build 3445)** 核心架构完全自主重构与移植的 Android 客户端。支持 **手机、平板/折叠屏、Android 智能电视 (TV)** 三端自适应。

---

## 🌟 核心特性

- 📱 **多端全平台自适应**：
  - **手机端**：经典单列流式卡片，一键开关、节点实时测速与全局路由切换。
  - **平板/折叠屏**：`NavigationRail` 配合双栏展开式布局，充分利用大屏幕。
  - **Android TV**：针对 10-foot 远距离遥控操作优化的全屏沉浸式界面，完整支持 D-Pad 方向键焦点控制。
- ⚡ **全协议覆盖**：原生兼容 Shadowsocks (2022/AEAD), VMess, VLESS (XTLS/Reality), Trojan, Hysteria 1/2, TUIC v5, WireGuard, Snell。
- 📜 **100% 兼容 Shadowrocket 规则配置**：直接导入解析 `.conf` / `.rule` 格式，支持 `DOMAIN-SUFFIX`, `IP-CIDR`, `GEOIP`, `FINAL` 智能分流。
- 🛡️ **Android 专属增强**：
  - **分应用代理 (Per-App Proxy)**：精准控制哪些 App 走代理、哪些直连。
  - **快捷设置磁贴 (Quick Settings Tile)**：下拉通知栏一键开关。
- 🚀 **高性能底层**：基于 Android 原生 `VpnService` 与现代化 `sing-box` 核心桥接。

---

## 📂 项目模块结构

```
shadowrocket/
├── core-model/       # 协议节点、分流规则、配置实体模型
├── core-parser/      # Shadowrocket .conf 规则语法解析器、节点链接解析器与 sing-box 转换器
├── core-vpn/         # Android VpnService、前台通知保活、JNI 桥接与磁贴服务
└── app/              # 主应用界面 (Jetpack Compose, 包含手机/平板/TV 界面)
```

---

## 🛠️ 编译与构建

### 1. 准备环境
- **JDK 17 / 21**
- **Android SDK (API 34, Min API 26)**
- **Android NDK** (可选，若自行编译 Go 核心)

### 2. 编译 APK
在项目根目录下执行 Gradle 命令：
```bash
./gradlew assembleDebug
```
生成的 APK 文件位于：
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 授权与免责声明
本项目基于公开网络协议规范与开源架构重写，与 Shadow Launch Technology Limited 官方无关。仅供个人技术研究与合规网络测试使用。
