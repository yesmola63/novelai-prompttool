# NovelAI 提示词记录 · Android v1.1

- 应用包名：`com.novelai.prompttool`
- 版本：`1.1`（versionCode 2）
- 语言：`Kotlin`
- 网页加载方式：`内置 NanoHTTPD 本地服务器`
- 数据位置：`Android/data/com.novelai.prompttool/files/`（`prompt.json`、`picture/`、`update_cache.json`）

## 简介

应用启动后在本地起一个 NanoHTTPD 服务，用 WebView 加载内置网页界面，界面与电脑版一致；提示词与图片保存在应用自己的目录中，不依赖网络。

## 功能

- **提示词管理**：分组、角色、版本、描述、多张封面图
- **图片本地化**：系统相册 / 文件选择器导入
- **打包提示词**：导出 `.bundle`，可选密码加密（AES），保存到 `Download/prompt_pack/`
- **加载提示词**：导入 `.bundle`，支持「合并 / 替换」
- **详情查看**：全屏查看、图片切换、复制指令、保存图片、编辑、删除
- **数据保护**：`prompt.json` 自动备份，语法损坏时可恢复
- **应用内检查更新**（1.1 新增）

## 相较于 v1.0 更新了什么

### 新增功能

1. **应用内检查更新**
   - 启动时比对服务器版本，发现新版本弹窗展示更新说明
   - 「立即更新」下载 APK，显示进度与下载速度，完成后拉起系统安装器
   - 「暂不更新」直接关闭；勾选「当天不再显示」后当天不再提示，次日恢复
   - 更新信息缓存在**应用私有目录**的 `update_cache.json`，检测不到更新时自动删除

### 交互改进

2. **分类栏重做**
   - 由「左右翻页箭头」改为**横向拖动滚动**
   - 右侧箭头点击后展开全部分类（一次性列出，非分页）
   - 当前分类自动滚动到可视区域中间

3. **分组填写重做**
   - 由「下拉框选择 + 另填新分组」改为**单个输入框**
   - 输入时自动匹配已有分组，以下拉面板列出，点击即填入
   - 命中已有分组显示绿色 ✓ 提示，新建分组显示红色提示

4. **打包界面**
   - 勾选「需要密码」后才显示密码框（带展开动画）
   - 选择「打包所有提示词」时隐藏「选择提示词」按钮
   - 打包前检查同名文件并提示

5. **选择提示词界面改为全屏**，卡片尺寸与主页一致（原先为带边距的窗口）

### 体验与稳定性

6. **修复软键盘遮挡输入框**：视口随键盘收缩（`dvh` + `adjustResize` + 聚焦自动滚动），输入密码时不再被键盘顶出可视区
7. **滚动优化**：移除大面积毛玻璃效果、精简滚动容器、图片懒加载，减少卡顿与闪烁
8. **弹窗层级与返回键顺序修正**：返回键优先关闭最上层弹窗

## 目录结构

```
mobile/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/          # 网页界面（HTML/CSS/JS）
│       ├── java/            # Kotlin 原生层
│       └── res/
├── build.gradle
├── settings.gradle
└── gradle/                  # Gradle Wrapper
```

## 构建

需要 Android SDK（`local.properties` 中 `sdk.dir` 指向本机路径）与 JDK 17。

```bash
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release-unsigned.apk
```
