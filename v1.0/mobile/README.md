# NovelAI 提示词记录 · Android v1.0

- 应用包名：`com.novelai.prompttool`
- 版本：`1.0`（versionCode 1）
- 语言：`Kotlin`
- 网页加载方式：`内置 NanoHTTPD 本地服务器`
- 数据位置：`Android/data/com.novelai.prompttool/files/`（`prompt.json`、`picture/`）

## 简介

电脑版的安卓移植版：应用启动后在本地起一个 NanoHTTPD 服务，用 WebView 加载内置的网页界面，因此界面与电脑版完全一致。提示词、图片都保存在应用自己的外部目录里，不需要联网。

## 功能

- **提示词管理**：分组、角色、版本、描述、多张封面图
- **图片本地化**：用系统相册/文件选择器导入图片
- **打包提示词**：导出 `.bundle`，可选密码加密（AES），保存到 `Download/prompt_pack/`
- **加载提示词**：导入 `.bundle`，支持「合并」与「替换」
- **详情查看**：全屏查看、图片切换、复制指令、保存图片、编辑、删除
- **数据保护**：`prompt.json` 自动备份；检测到语法损坏时可提取内容并修复

## 相较于未发布的初代更新了什么

> 「初代」指此前用 Buildozer / WebToApp 尝试打包、但**始终无法正常运行**的手机版本（打开即提示「Python 服务器启动失败」）。

v1.0 是**第一个真正能用的手机版**：

1. **改用 Kotlin 原生工程**（IntelliJ IDEA + Android SDK），不再依赖 Python 打包工具
2. **用内置 NanoHTTPD 起本地服务 + WebView 加载网页**，彻底解决「服务器启动失败」
3. **完整移植电脑版功能**：查看、添加、编辑、删除、打包、加载、图片管理
4. **手机端适配**：
   - 详情页改为全屏查看
   - 使用系统文件选择器导入图片
   - 状态栏 / 导航栏配色与界面统一
   - 触控滚动与弹窗交互适配
5. **数据保存在应用自己的目录**，卸载应用即清理，不影响其他文件
6. **打包输出到系统下载目录**（`Download/prompt_pack/`），方便分享

---

> ⚠️ **以下为源码归档说明：原工程源码已丢失，本仓库是根据 APK 逆向重建的结果，不可靠，仅供存档与参考。**

- 原始 APK：见 [`original/NovelAIPromptTool-v1.0.apk`](original/NovelAIPromptTool-v1.0.apk)

## 重建方式

| 部分 | 来源 | 可靠性 |
|---|---|---|
| `app/src/main/assets/`（网页界面：HTML/CSS/JS） | 从 v1.0 APK 中**原样提取** | 与原版一致 |
| `app/src/main/res/`、`AndroidManifest.xml`、Gradle 配置 | 以 v1.0 资源为参考重建 | 基本一致 |
| `app/src/main/java/`（Kotlin 原生层） | **取自后续版本源码**（v1.0 的 Kotlin 源码已丢失） | 近似，可能与 v1.0 行为不同 |
| `decompiled/java/` | jadx 反编译 v1.0 APK 得到的 Java | 仅作阅读参考，**不能直接编译**（Kotlin 合成代码无法完整还原） |

由于 v1.0 的 Kotlin 原生层只存在于编译产物中，反编译得到的 Java 无法通过编译（大量调用了 Kotlin 内部合成方法），因此工程中的原生层使用了后续版本的可编译源码替代，仅保证**可以构建出 APK**。

## 构建

需要 Android SDK（`local.properties` 中 `sdk.dir` 指向本机路径）与 JDK 17。

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 目录结构

```
.
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/          # v1.0 网页界面（原样提取）
│       ├── java/            # Kotlin 原生层（近似重建）
│       └── res/
├── decompiled/java/         # jadx 反编译产物（阅读参考）
├── original/                # v1.0 原始安装包
├── build.gradle
├── settings.gradle
└── gradle/                  # Gradle Wrapper
```

## 免责声明

本仓库仅用于保存历史版本，不能保证与 v1.0 的实际行为一致；请勿将其视为原始工程。若需可靠的 v1.0 行为，请以 `original/` 中的 APK 为准。
