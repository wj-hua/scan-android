# ScanApp

Android 智能扫码应用：支持实时摄像头扫描、相册识别和按内容类型提供快捷操作。

| 项 | 值 |
| --- | --- |
| Application ID | `com.scanapp.scanner` |
| 应用名称 | ScanApp |
| 模块 | `:app` |
| minSdk | 24 |
| targetSdk / compileSdk | 35 |
| UI | Jetpack Compose + Material 3 |
| 语言 | Kotlin |

## 功能

- **实时扫码**：后置摄像头预览，识别到码后自动弹出结果
- **相册识别**：从相册选择图片，离线识别二维码/条码
- **系统分享识别**：在相册等应用中将图片直接分享给 ScanApp
- **连续扫码**：无需逐条关闭结果页，并可配置相同内容的重复间隔
- **二维码生成器**：生成、保存或分享二维码图片
- **手电筒**：支持闪光灯的设备可开关补光
- **结果操作**
  - 自动识别网址、Wi-Fi、电话、邮箱、联系人、地图与普通文本
  - 按类型提供安全访问、打开 Wi-Fi、拨号、写邮件、添加联系人、打开地图等快捷操作
  - 复制到剪贴板
  - 通过 Android 系统分享面板分享
  - 网址显示实际域名，并在安全确认后打开
  - 继续扫描
- **扫描历史**
  - 展示扫描结果类型与扫描来源
  - 收藏/取消收藏单条记录
  - 按内容或类型搜索
  - 删除单条记录或确认后清空全部
  - 导出包含内容、类型、来源、时间与收藏状态的 CSV
- **扫描设置**
  - 配置连续扫码、重复内容间隔与成功振动
  - 隐私模式下不保存新历史，同时阻止截图与最近任务预览
  - 可选择 1、7、30 或 90 天后自动清理未收藏记录
  - 数据库升级会保留旧版本历史
- **基础体验**
  - 扫描成功短振动反馈
  - 相机权限被拒绝后可从扫码页重新授权

### 支持的码制

QR Code、Aztec、Codabar、Code 39/93/128、Data Matrix、EAN-8/13、ITF、PDF417、UPC-A/E。

## 技术栈

| 组件 | 用途 |
| --- | --- |
| CameraX | 预览与帧分析 |
| ML Kit Barcode Scanning | 二维码/条码识别 |
| Jetpack Compose | 界面 |
| Activity Result API | 相机权限、相册选图 |
| Kotlin Coroutines | 异步识别 |

主要依赖版本见 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)。

## 项目结构

```text
scan-android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/scanapp/scanner/
│       │   └── MainActivity.kt          # 扫码 UI 与业务逻辑
│       └── res/
│           ├── drawable/                # 启动图标前景/背景
│           ├── mipmap-anydpi-v26/       # Adaptive Icon
│           ├── mipmap-hdpi/             # 兼容图标
│           └── values/                  # 字符串、主题、颜色
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 环境要求

- JDK 17
- Android SDK（含 Platform 35、对应 Build-Tools）
- 推荐 Android Studio Ladybug 或更新版本
- 真机或模拟器 API 24+

本地 SDK 路径写在 `local.properties`：

```properties
sdk.dir=/path/to/Android/sdk
```

## 构建与安装

在项目根目录：

```bash
# Debug APK
./gradlew :app:assembleDebug

# 输出路径
# app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接设备：

```bash
# 建议先卸载旧包，避免桌面图标/名称缓存
adb uninstall com.scanapp.scanner
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

或用 Android Studio 打开工程后直接 Run。

## 权限

| 权限 | 说明 |
| --- | --- |
| `CAMERA` | 实时扫码（运行时申请） |
| `VIBRATE` | 扫描成功时提供短振动反馈 |

相册选图通过 Photo Picker（`PickVisualMedia`），无需额外存储权限。  
`AndroidManifest` 中 `camera` / `camera.flash` 为 `required="false"`，无相机设备仍可使用相册识别。

## 桌面名称与图标

- 应用名：`res/values/strings.xml` → `app_name`（当前为 **ScanApp**）
- 启动图标：`android:icon` / `android:roundIcon` 指向 `@mipmap/ic_launcher` 系列
- `MainActivity` 同步声明了 `label` 与 `icon`，保证启动器条目显示正常

> **注意**：缺少 `android:icon` 时，部分 Android 16 启动器可能不显示应用名称。更换图标后请卸载重装再验证。

## 使用说明

1. 打开应用，授予相机权限（可拒绝，仅用相册）
2. 将二维码/条码对准扫描框，成功后底部弹出结果
3. 右上角 **相册** 可从图片识别
4. 底部手电筒按钮在支持设备上开启补光
5. 结果页会按类型显示快捷操作，也可 **复制**、**分享**、**继续扫描**
6. 右上角 **历史** 可搜索、收藏、删除或清空扫描记录
7. 历史页可导出 **CSV**，设置页可配置连续扫码、振动、隐私模式和自动清理
8. 扫描首页右上角的二维码按钮可打开 **二维码生成器**
9. 在系统相册中分享图片并选择 ScanApp，可直接识别图片中的码

## 常见问题

**桌面看不到应用名或图标**  
确认 Manifest 已配置 `icon`/`roundIcon` 与 `label`，卸载后重装；必要时重启启动器或设备。

**相机启动失败**  
检查是否授予相机权限、是否被其他应用占用、或系统是否禁用摄像头。

**相册图片识别不到**  
确认图片清晰、码完整，且属于上述支持码制；部分截图缩放过小可能导致失败。

## 版本

当前 `versionName`：`1.3`，`versionCode`：`4`（见 `app/build.gradle.kts`）。
