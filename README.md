# AA 记账 (AA Ledger)

> 一款面向多人场景的安卓 AA 制记账与债务清算工具。
> 聚餐、合租、旅行时的每一笔账，自动算清「谁该转给谁、转多少」，一键生成最少转账方案。

## ✨ 核心功能

- **多人债务自动计算**：按付款人 + 参与人自动分摊，净额法 + 贪心匹配，得出**最少转账次数**的清算方案
- **四种分摊方式**：均分 / 按份数 / 自定义金额 / 百分比，满足不同场景
- **小票 OCR 识别**：拍照或相册选图，本地 ML Kit 先识别，不满意可点「AI识别」走 GLM-4.1V 视觉大模型做更精确的结构化识别
- **多币种 + 汇率**：支持外币记账，汇率本地缓存、离线可用，手动刷新
- **账本共享**：邀请码加入同一账本，多人协同记账（云端同步）
- **桌面小组件**：1×1 快捷记账 + 2×2 账本概览（Jetpack Glance）
- **统计图表**：饼图 / 柱状图 / 时间轴，消费结构一目了然
- **iOS 26「Liquid Glass」动效**：统一的弹簧动画、按压反馈、转场手感
- **深色模式 / 动态取色**：Material 3 主题，跟随系统

## 📱 技术栈

| 层 | 技术 |
| --- | --- |
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM + Repository + Hilt DI |
| 数据库 | Room (SQLite)，离线优先 |
| 导航 | Navigation Compose |
| 小组件 | Jetpack Glance (AppWidget) |
| OCR | ML Kit Text Recognition v2（中文）+ GLM-4.1V |
| 网络 | Retrofit 2（汇率 / 云同步 / GLM） |
| 后端 | Node.js + Express + Prisma (PostgreSQL)，JWT 认证 |

最低支持 **Android 10 (API 29)**。

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog 或更新版本
- JDK 17
- Android 10+ 设备或模拟器

### 构建步骤

1. 克隆仓库：

   ```bash
   git clone https://github.com/hotpot1993/AABook.git
   cd AABook
   ```

2. 在项目根目录创建 `secrets.properties`：

   ```properties
   EXCHANGE_RATE_API_KEY=你的汇率 API Key
   GLM_API_KEY=你的 GLM API Key
   ```

   > 两项均为可选：缺少 GLM Key 时仅本地 OCR 可用；缺少汇率 Key 时使用内置默认汇率。

3. 用 Android Studio 打开项目，等待 Gradle Sync 完成，运行 `app` 配置。

   或命令行构建 Debug APK：

   ```bash
   ./gradlew :app:assembleDebug
   ```

### 服务端（可选，账号 / 云同步）

`server/` 目录是云同步后端（Node.js + Express + Prisma + PostgreSQL）。仅在需要「账号登录 / 账本共享 / 云备份」时启动：

```bash
cd server
npm install
# 配置 DATABASE_URL 后
npm run db:push
npm run dev
```

## 📦 发行

推送 `v*` 标签会自动触发 GitHub Actions 构建签名 Release APK 并创建 GitHub Release：

```bash
git tag v1.5.1
git push origin v1.5.1
```

## 🗂 目录结构

```
app/src/main/java/com/aa/ledger/
├── data/          # local (Room) / remote (汇率、GLM、云同步) / repository
├── domain/        # 领域模型 + 核心算法 (分摊 / 债务清算)
├── ui/            # Compose 页面 (home/ledger/expense/settlement/stats/…)
├── widget/        # 桌面小组件 (Glance)
└── util/          # 工具类
server/            # 云同步后端
```

## 📄 License

暂无开源许可证。
