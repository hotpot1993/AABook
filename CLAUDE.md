# AA 记账 (AA Ledger) — Claude Code 项目文档

## 项目概述

一款面向多人场景（聚餐、合租、旅行）的安卓 AA 制记账与债务清算工具。
核心差异化：多人视角的债务自动计算，最小转账次数算法。

## 技术栈

- **语言**: Kotlin 2.0.21
- **UI**: Jetpack Compose + Material Design 3 (动态取色, 深色模式, iOS 26「Liquid Glass」动效体系)
- **架构**: MVVM + Repository + Hilt DI
- **数据库**: Room (SQLite), 离线优先
- **导航**: Navigation Compose
- **Widget**: Jetpack Glance (AppWidget 桌面小组件)
- **OCR**: Google ML Kit Text Recognition v2 (中文) 本地识别 + GLM-4.1V 视觉大模型结构化识别
- **网络**: Retrofit 2 (汇率刷新 / 云同步 / GLM 识别)
- **后端**: 自建 Node.js + Express + Prisma(PostgreSQL)，JWT 认证 + 云备份同步（见 `server/`）
- **最低 SDK**: API 29 (Android 10)，compileSdk/targetSdk 34，Java 17

## 项目结构

```
app/src/main/java/com/aa/ledger/
├── App.kt                    # @HiltAndroidApp Application
├── MainActivity.kt           # 单 Activity 入口
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt    # Room DB (6 个 Entity)
│   │   ├── dao/              # 6 个 DAO 接口
│   │   └── entity/           # 6 个 Entity
│   ├── remote/
│   │   ├── ExchangeRateApi.kt # Retrofit 汇率接口
│   │   ├── GlmApi.kt          # GLM-4.1V 视觉识别接口
│   │   ├── CloudApi.kt        # 云端认证/备份接口
│   │   ├── CloudSyncManager.kt# 云同步调度
│   │   └── dto/               # AuthDtos, GlmDtos
│   ├── di/
│   │   └── DatabaseModule.kt  # Hilt DI 模块
│   └── repository/           # 6 个 Repository (Auth/ExchangeRate/Expense/Glm/Ledger/Settlement)
├── domain/
│   ├── model/                # 领域模型 (Ledger, Member, Expense, SettlementResult)
│   └── calculator/
│       ├── SplitCalculator.kt    # 分摊计算 (均分/按份/自定义/百分比)
│       └── DebtCalculator.kt     # 债务合并最小转账算法
├── ui/
│   ├── theme/                # Material 3 主题 (Color.kt, Theme.kt, Animation.kt)
│   ├── navigation/           # NavGraph.kt, BottomNavBar.kt
│   ├── auth/                 # 登录 (JWT 认证)
│   ├── home/                 # 首页 (账本列表 + 创建对话框)
│   ├── ledger/               # 账本详情 (消费流水 + 摘要)
│   ├── expense/              # 记账表单 (核心交互：金额/币种/分摊/拍照/OCR 复核)
│   ├── settlement/           # 结算清单 (转账建议 + 分享)
│   ├── member/               # 成员管理
│   ├── stats/                # 统计图表 (饼图/柱状图/时间轴)
│   ├── admin/                # 管理页
│   ├── common/               # 通用组件 (DeleteConfirmDialog 等)
│   └── settings/             # 设置 (导出/导入/汇率/云同步)
├── widget/                   # 桌面小组件 (QuickAddWidget 快捷 + OverviewWidget 概览, Glance)
└── util/                     # 工具类 (CurrencyFormatter, ShareUtil, PreferencesManager, NotificationHelper)

server/                       # Node.js + Express + Prisma(PostgreSQL) 后端
├── src/                      # index.ts, routes/, middleware/
├── prisma/schema.prisma      # User/Ledger/LedgerMember/Expense/Settlement/UserBackup...
└── Dockerfile, docker-compose.yml
```

## 开发要点

### 运行前准备
1. 用 Android Studio (Hedgehog 或更新) 打开项目根目录
2. 等待 Gradle Sync 完成
3. 连接 Android 10+ 设备或启动模拟器
4. Run 'app' configuration
5. 本地 `secrets.properties` 需配置 `GLM_API_KEY` 与 `EXCHANGE_RATE_API_KEY`（CI 中通过环境变量注入）

### 关键依赖版本
- AGP 8.2.2, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Compose BOM 2025.01.00
- Room 2.6.1, Hilt 2.51.1, Navigation 2.8.0
- ML Kit text-recognition-chinese:16.0.1
- Glance 1.0.0, Coil 2.5.0, Retrofit 2.9.0, WorkManager 2.9.0, Security-crypto 1.1.0-alpha06

### 数据库 Schema
6 个表: ledgers, members, expenses, expense_splits, exchange_rates, settlements
详见 `data/local/entity/` 目录。

### 核心算法
- **SplitCalculator**: 四种分摊方式，输入验证
- **DebtCalculator**: 净额法 + 贪心匹配 → 最少转账次数

### OCR 识别流程
- 拍照/选图后先用 ML Kit 本地识别金额、货币，弹出「确认金额」复核对话框
- 复核不满意可点「AI识别」走 GLM-4.1V (`glm-4.1v-thinking-flash`) 做更精确的结构化识别

### 离线优先 + 云同步
- 汇率使用本地缓存，网络不可用时使用过期缓存 + UI 提示
- 首次启动使用内置默认汇率
- 所有 CRUD 操作纯本地；云备份/账号同步通过 `server/` 后端按需触发，离线不受影响

### 权限策略
- CAMERA: 仅拍照上传小票/头像时请求
- READ_EXTERNAL_STORAGE: 仅 API ≤ 28
- INTERNET: 汇率刷新 / 云同步 / GLM 识别
