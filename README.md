# 3D Animation Studio - 安卓移动端专业级3D动画制作软件

[![Android CI/CD](https://github.com/animated/3d-animation-studio/actions/workflows/build.yml/badge.svg)](https://github.com/animated/3d-animation-studio/actions/workflows/build.yml)

**对标 Blender/Maya 桌面级体验的安卓 3D 动画编辑器**

> **注意：** 本项目完全由DeepSeek V4 Pro编写，作者本人并不懂编程。

## 📋 功能概览

### 🎯 模型导入
- ✅ **PMX (模之屋)** — 完整骨骼/权重/BlendShape 解析
- ✅ **GLTF/GLB** — PBR 材质管线
- ✅ **FBX** — 工业标准格式
- ✅ **OBJ** — 通用网格模型

### 🎨 3D 场景编辑
- ✅ Transform 三轴位移/旋转/缩放 (Gizmo 操作)
- ✅ 材质属性编辑 (漫反射色、高光、纹理)
- ✅ BlendShape 表情滑块 + 关键帧动画
- ✅ 多对象场景层次管理

### 💡 灯光系统
| 类型 | 强度 | 颜色 | 角度 | 阴影 |
|--------|-----------|-------|-------|--------|
| 环境光 | ✅ | ✅ | — | — |
| 平行光 | ✅ | ✅ | ✅ | ✅ |
| 点光源 | ✅ | ✅ | ✅ | ✅ |
| 聚光灯 | ✅ | ✅ | ✅ | ✅ |

**所有灯光参数均支持关键帧动画**

### 📷 摄像机系统
- 🎥 **自由漫游模式** — 第一人称 WASD 风格操控
- 🔄 **轨道环绕模式** — 围绕目标点旋转/缩放
- 🔑 位置/旋转/焦距/景深全部关键帧化
- 📐 可调 FOV、近/远裁剪面

### ⏱ 时间轴 & 关键帧编辑器
- 📊 **多轨道分离设计：**
  | 变换轨道 | ModelTransformTrack |
  | 表情轨道 | MorphTrack |
  | 相机轨道 | CameraTrack |
  | 灯光轨道 | LightTrack |
- 🔑 关键帧插值：线性/贝塞尔/阶跃/缓动
- 📈 曲线编辑器 (Graph Editor) + 贝塞尔手柄
- ✂️ 批量复制/删除/移动关键帧
- ⚡ 快速跳转指定帧、自定义帧率 (10-60fps)

### 🎬 渲染导出
- 📦 **MP4 / MOV** (H.264 / H.265 硬件编码)
- 🔧 自定义分辨率 (480p → 4K)
- ⚙️ MSAA 采样 ×1/×2/×4/×8 可选
- 📡 独立后台渲染线程 (不阻塞 UI)

### 📱 全机型性能优化
- 🧠 **三级设备适配** — 自动检测 GPU 型号档次
  | 档位 | MSAA | 阴影 | 骨骼上限 |
  |------|------|------|----------|
  | 低端 | ×1 | ❌ | 64 |
  | 中端 | ×2 | ✅ | 128 |
  | 高端 | ×4 | ✅ | 256 |
- 🧩 LOD 细节层次 + Instance 实例化
- 🧵 GPU 骨骼蒙皮 + 纹理压缩 (ETC2/ASTC)
- 🧠 内存池 + 纹理缓存 + OOM 防护

---

## 🏗 工程结构

```
3DAnimationApp/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/threedstudio/app/
│   │   │   ├── MainActivity.kt       # 编辑器入口
│   │   │   └── ThreeDStudioApp.kt    # Application 初始化
│   │   └── res/
│   │       ├── layout/
│   │       ├── values/
│   │       └── drawable/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── render/                       # 3D 渲染引擎
│   ├── src/main/java/com/threedstudio/render/
│   │   ├── core/
│   │   │   ├── MathTypes.kt      # Vec3/Vec4/Quat/Mat4
│   │   │   ├── Shader.kt         # GLSL 编译 & 着色器管理
│   │   │   └── GpuMesh.kt        # VAO/VBO/EBO GPU 管线
│   │   ├── LightCamera.kt        # 灯光 + 相机系统
│   │   └── GLRenderEngine.kt     # OpenGL ES 3.0 渲染核心
│   └── build.gradle.kts
│
├── modelio/                      # 模型导入解析
│   ├── src/main/java/com/threedstudio/modelio/
│   │   └── PmxParser.kt          # PMX/GLTF/FBX/OBJ 解析
│   └── build.gradle.kts
│
├── animation/                    # 动画系统
│   ├── src/main/java/com/threedstudio/animation/
│   │   └── AnimationSystem.kt    # 关键帧/轨道/曲线编辑器
│   └── build.gradle.kts
│
├── ui/                           # 编辑器 UI
│   ├── src/main/java/com/threedstudio/ui/
│   │   └── EditorLayout.kt       # Compose 专业编辑器布局
│   └── build.gradle.kts
│
├── export/                       # 视频导出
│   ├── src/main/java/com/threedstudio/export/
│   │   └── RenderExporter.kt     # MediaCodec + EGL 离屏渲染
│   └── build.gradle.kts
│
├── .github/workflows/
│   └── build.yml                 # GitHub Actions CI/CD
│
├── build.gradle.kts              # 根构建脚本
├── settings.gradle.kts           # 模块配置
├── gradle.properties
└── README.md
```

---

## 🚀 本地源码编写 & GitHub Actions 云端编译

### 零本地编译环境需求

本项目严格遵循**纯本地源码编写、云端自动编译**原则：

1. ✅ **本地仅编写 Kotlin/XML/GLSL 源码**
2. ✅ **不在本地安装 Android SDK / NDK / Java / Gradle**
3. ✅ **通过 GitHub Actions 自动拉取编译环境 → 构建 → 输出 APK**

### 📥 本地开发步骤

```bash
# 1. 克隆仓库到本地
git clone https://github.com/animated/3d-animation-studio.git
cd 3d-animation-studio

# 2. 使用任意代码编辑器编写源码
#    (推荐 Android Studio / VS Code)

# 3. 提交更改
git add .
git commit -m "feat: 更新模型导入模块"
git push origin main

# 4. 自动触发 GitHub Actions 构建
#    进入 Actions 面板查看编译进度
#    编译完成后在 Artifacts 下载 APK
```

### 🔧 GitHub Actions 配置解读

工作流文件：`.github/workflows/build.yml`

```yaml
触发条件:
  - push 到 main/master/develop 分支
  - Pull Request
  - 手动触发 (workflow_dispatch)

环境准备:
  1. actions/checkout@v4 —— 拉取源码
  2. actions/setup-java@v4  —— 安装 JDK 17
  3. android-actions/setup-android@v3 —— 安装:
     • Android SDK Platform 34
     • Build-tools 34.0.0
     • NDK 26.2.11394342
     • CMake 3.22.1

构建选项:
  - workflow_dispatch 可选手动选择 debug / release
  - debug: assembleDebug
  - release: assembleRelease (支持密钥签名)

产物输出:
  - APK 文件上传为 Actions Artifact
  - 保留 30 天
  - 命名: 3DAnimationStudio-{build_type}-{run_number}
```

### 📲 APK 下载教程

1. 访问仓库 GitHub 页面
2. 点击顶栏 **Actions** 标签
3. 选择最近一次成功的 Workflow Run
4. 在页面底部的 **Artifacts** 区域
5. 点击 `3DAnimationStudio-debug-{编号}` 下载
6. 解压 ZIP → 得到 APK → 安装到安卓设备

---

## 📊 技术架构

```
┌──────────────────────────────────────┐
│          Compose UI Layer            │
│  EditorLayout / Timeline / Inspector │
├──────────────────────────────────────┤
│       Animation Controller           │
│  TransformTrack / MorphTrack / ...   │
├──────────────────────────────────────┤
│       Model Import (modelio)         │
│  PMX / GLTF / FBX / OBJ Parser     │
├──────────────────────────────────────┤
│       Render Engine (OpenGL ES 3.0)  │
│  Shader / GpuMesh / Light / Camera   │
├──────────────────────────────────────┤
│       Export Engine (export)         │
│  MediaCodec + EGL Offscreen Render   │
└──────────────────────────────────────┘
```

### 光照模型
- **Phong/Blinn-Phong** 多光源混合
- 环境光 + 平行光 + 点光源 + 聚光灯同时支持
- 漫反射 + 镜面高光 + 距离衰减 + 聚光衰减

### 插值曲线
| 类型 | 函数 |
|------|------|
| 线性 | `lerp(a, b, t)` |
| 阶跃 | `t < 1 ? 0 : 1` |
| 缓入 | `t²` |
| 缓出 | `1 - (1-t)²` |
| 缓入缓出 | `t < 0.5 ? 2t² : -1+(4-2t)t` |
| 贝塞尔 | 三次 Bezier 曲线 |

---

## 📄 依赖说明

| 库 | 用途 |
|----|------|
| AndroidX Core KTX 1.12 | 核心扩展 |
| Jetpack Compose BOM 2024.01 | UI 框架 |
| Material Design 3 | 专业编辑器主题 |
| Kotlin Coroutines 1.7.3 | 异步加载/导出 |
| Gson 2.10.1 | 模型数据序列化 |
| OpenGL ES 3.0 | 硬件加速 3D 渲染 |
| MediaCodec | H.264/H.265 硬件编码 |

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

- **分支策略：** `main` 稳定版 / `develop` 开发版
- **代码规范：** Kotlin 官方风格，完整 KDoc 注释
- **测试：** 提交前确保 `./gradlew lintDebug` 通过

---

## 📜 许可证

MIT License - 自由使用、修改、分发

---

> **注意：** 本项目依赖 GitHub Actions 进行云端编译打包。
> 本地仅需代码编辑器即可参与开发，无需配置任何 Android 开发环境。
