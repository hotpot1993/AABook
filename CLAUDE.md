# AA 记账 (AA Ledger) — Claude Code 项目文档

## 项目概述

一款面向多人场景（聚餐、合租、旅行）的安卓 AA 制记账与债务清算工具。
核心差异化：多人视角的债务自动计算，最小转账次数算法。

## 技术栈

- **语言**: Kotlin 1.9.22
- **UI**: Jetpack Compose + Material Design 3 (动态取色, 深色模式)
- **架构**: MVVM + Repository + Hilt DI
- **数据库**: Room (SQLite), 离线优先
- **导航**: Navigation Compose
- **Widget**: RemoteViews (传统 RemoteViews 实现)
- **OCR**: Google ML Kit Text Recognition v2 (中文)
- **网络**: Retrofit 2 (仅用于汇率刷新)
- **最低 SDK**: API 29 (Android 10)

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
│   │   └── ExchangeRateApi.kt # Retrofit 汇率接口
│   ├── di/
│   │   └── DatabaseModule.kt  # Hilt DI 模块
│   └── repository/           # 4 个 Repository
├── domain/
│   ├── model/                # 领域模型 (Ledger, Member, Expense, SettlementResult)
│   └── calculator/
│       ├── SplitCalculator.kt    # 分摊计算 (均分/按份/自定义/百分比)
│       └── DebtCalculator.kt     # 债务合并最小转账算法
├── ui/
│   ├── theme/                # Material 3 主题 (Color.kt, Theme.kt)
│   ├── navigation/NavGraph.kt
│   ├── home/                 # 首页 (账本列表 + 创建对话框)
│   ├── ledger/               # 账本详情 (消费流水 + 摘要)
│   ├── expense/              # 记账表单 (核心交互：金额/币种/分摊/拍照)
│   ├── settlement/           # 结算清单 (转账建议 + 分享)
│   ├── member/               # 成员管理
│   ├── stats/                # 统计图表 (饼图/柱状图/时间轴)
│   └── settings/             # 设置 (导出/导入/汇率)
├── widget/                   # 桌面小组件 (1x1 快捷 + 2x2 概览)
└── util/                     # 工具类 (CurrencyFormatter, ShareUtil)
```

## 开发要点

### 运行前准备
1. 用 Android Studio Hedgehog+ 打开项目根目录
2. 等待 Gradle Sync 完成
3. 连接 Android 10+ 设备或启动模拟器
4. Run 'app' configuration

### 关键依赖版本
- AGP 8.2.2, Kotlin 1.9.22, Compose BOM 2024.01.00
- Room 2.6.1, Hilt 2.50, Navigation 2.7.7
- ML Kit text-recognition-chinese:16.0.0

### 数据库 Schema
6 个表: ledgers, members, expenses, expense_splits, exchange_rates, settlements
详见 `data/local/entity/` 目录。

### 核心算法
- **SplitCalculator**: 四种分摊方式，输入验证
- **DebtCalculator**: 净额法 + 贪心匹配 → 最少转账次数

### 离线优先
- 汇率使用本地缓存，网络不可用时使用过期缓存 + UI 提示
- 首次启动使用内置默认汇率
- 所有 CRUD 操作纯本地，无网络依赖

### 权限策略
- CAMERA: 仅拍照上传小票/头像时请求
- READ_EXTERNAL_STORAGE: 仅 API ≤ 28
- INTERNET: 仅汇率刷新
