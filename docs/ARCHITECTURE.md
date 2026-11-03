# 系统架构

## 总览

```text
Nothing Ledger
├── 01 应用外壳系统 App Shell
│   ├── 启动、页面导航、深链
│   └── 模块装配，不保存业务状态
├── 02 输入会话系统 Entry Session
│   ├── 正在编辑的草稿
│   ├── 金额输入与校验
│   └── 提交、取消、恢复草稿
├── 03 交易系统 Transaction
│   ├── 支出 / 收入 / 转账
│   ├── 新增、修改、删除、撤回
│   └── 交易规则与事件
├── 04 账户系统 Account
│   ├── 现金、银行卡、支付账户等
│   ├── 余额投影
│   └── 调整与停用
├── 05 分类系统 Category
│   ├── 两级分类
│   ├── 排序、停用
│   └── 常用分类建议
├── 06 标签与备注系统 Annotation
│   ├── 标签
│   ├── 备注
│   └── 后续附件扩展口
├── 07 预算系统 Budget
│   ├── 月预算
│   ├── 预算进度
│   └── 超额事件
├── 08 查询系统 Query
│   ├── 日期、账户、分类、标签筛选
│   ├── 全文搜索
│   └── 分页与排序
├── 09 统计系统 Analytics
│   ├── 月度收支
│   ├── 趋势与分类聚合
│   └── 只读投影，不反写交易
├── 10 导入系统 Import
│   ├── 文件识别与字段映射
│   ├── 预览、校验、去重
│   └── 原子提交与导入报告
├── 11 备份交换系统 Backup & Exchange
│   ├── 本地完整备份 / 恢复
│   ├── 通用格式导出
│   └── 版本兼容与迁移
├── 12 数据系统 Data
│   ├── Room 数据库
│   ├── Repository 接口
│   ├── 事务与数据库迁移
│   └── 唯一可信数据源
├── 13 设置系统 Preferences
│   ├── 货币、记账日边界
│   ├── 外观与动效强度
│   └── 只存偏好，不存业务数据
├── 14 设计系统 Nothing Design
│   ├── 字体、颜色、间距、圆角
│   ├── 点阵图标与组件
│   └── 无障碍与缩放规则
├── 15 动效反馈系统 Motion & Feedback
│   ├── 动效语义和节奏
│   ├── 触觉反馈
│   └── 尊重系统“减少动画”设置
├── 16 桌面组件系统 Widget
│   ├── 快速记账
│   └── 月度摘要
├── 17 隐私安全系统 Privacy
│   ├── 敏感日志过滤
│   ├── 可选应用锁
│   └── 数据导出与删除边界
└── 18 质量系统 Quality
    ├── 领域规则单元测试
    ├── 数据迁移与导入回归测试
    └── 基准、崩溃与可访问性检查
```

## 四层边界

### 1. 表现层

Compose 页面、组件和桌面 Widget。只接收不可变的 UI 状态，只发送用户意图，例如“保存这笔支出”，不直接操作数据库。

### 2. 应用层

把一次完整操作组织起来。例如 `CreateTransaction` 会校验草稿、调用交易仓库、触发余额刷新并返回结果。这里负责流程，不负责画界面。

### 3. 领域层

保存真正的记账规则和模型。它是纯 Kotlin，不依赖 Android、Room 或 Compose，因此可以快速测试，也方便未来更换数据库或界面技术。

### 4. 基础设施层

Room、文件、导入解析、备份、系统时间等具体实现。它们实现领域层定义的接口，但领域层不知道它们的细节。

## 系统沟通规则

- 页面不能直接调用 DAO；
- 页面不能直接调用 `TransactionRepository` 的写方法；创建、编辑、删除、撤回必须经过
  对应领域用例，并使用乐观并发令牌保护用户刚看到的版本；
- ViewModel 不能自己拼 SQL 或修改账户余额；
- 统计系统只能读取交易投影，不能回写交易；
- 导入系统先生成 `ImportDraft`，用户确认后才交给交易系统提交；
- 账户余额由交易事件计算，不能由多个页面分别维护；
- 所有金额统一使用 `Money(minorUnits: Long, currency: CurrencyCode)`；
- 所有日期规则通过 `LedgerClock`，禁止在各处直接取系统时间；
- 跨系统行为使用用例和领域事件，不互相抓取内部对象。

## 推荐模块

```text
:app                 Android 外壳与依赖装配
:core:model          通用不可变模型
:core:design         Nothing 设计系统
:core:database       Room 与迁移
:core:testing        测试夹具
:domain:ledger       交易、账户、分类、预算规则
:domain:analytics    统计查询与投影
:feature:entry       快速记账
:feature:timeline    流水与搜索
:feature:shell       顶层目的地与返回规则（纯 Kotlin）
:feature:insights    统计
:feature:settings    设置、导入与备份入口
:feature:widget      桌面组件
:data:import         钱迹与通用表格适配器
:data:backup         本地备份和恢复
```

初期模块数量会克制。只有当边界已经稳定、编译依赖确实需要隔离时才拆成独立 Gradle 模块；不会为了“看起来架构化”机械拆文件。

## 单向数据流

```text
用户操作 → Intent → ViewModel → UseCase → Domain/Repository
   ↑                                            ↓
Compose ← UiState ← StateFlow ← Result / Domain Event
```

这个结构保证视觉升级不会改变交易规则，导入格式变化不会牵动首页，统计扩展也不会污染记账路径。

顶层导航只使用 `MainDestination` 表达 `TIMELINE / CALENDAR / STATISTICS / ACCOUNTS`。
记账和分类管理属于 `OverlayDestination`，覆盖在当前主页面上方；关闭后必须返回原主页面。
`ShellReducer` 统一处理切换、覆盖页和返回，页面文字不参与导航判断。

当前页面数据流如下：

```text
流水首页 ← TimelineState ← ObserveTimeline
日历页面 ← CalendarState ← ObserveCalendar
统计页面 ← TimelineStatisticsState ← TimelineState.toStatistics
账户页面 ← AccountsState ← ObserveAccounts
账单详情 ← TransactionDetailState ← ObserveTransactionDetail
独立记账页 → EntryAction → EntryReducer → CreateTransaction
```

多币种金额在日历、统计和账户中始终分桶展示，绝不直接相加。所有可点击表面统一使用
`ledgerClickable` 或 `LedgerControl`，禁止重新引入系统默认的白色点击涟漪。

具体的代码边界检查结果与后续约束见 [BOUNDARY_AUDIT.md](BOUNDARY_AUDIT.md)。

