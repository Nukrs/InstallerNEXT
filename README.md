# Installer NEXT

一个开源的的Android APK安装器，仅支持Root权限安装。

## 功能特性


-  现代化Material Design 3界面
-  详细的APK信息展示
-  支持多种架构（ARM64、ARM、x86_64、x86）

## 技术栈

- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代化UI框架
- **Material Design 3** - 设计系统
- **LibSU** - Root权限管理
- **Android SDK 24+** - 最低支持版本

## 构建要求

- Android Studio
- Kotlin 1.9+
- Gradle 8.13+

## 快速开始

1. 克隆项目
```bash
git clone https://github.com/nukrs/InstallerNEXT.git
```

2. 在Android Studio中打开项目

3. 构建并运行
```bash
./gradlew assembleDebug
```

## 许可证

MIT开源项目，具体许可证信息请查看项目仓库。

## 贡献

欢迎提交Pull Request到原项目仓库。

---

⚠️ **重要提示**: 某些设备可能需要Xposed模块锁定功能（爱玩机锁定）。