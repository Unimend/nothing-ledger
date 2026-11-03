# 边界审查

## 结论

当前主链路已经形成正确的单向关系：

```text
Compose -> ViewModel -> 用例 -> Repository 接口 <- Room 实现
```

页面没有访问 DAO，也没有自行写交易。金额、交易类型与提交校验分别由
`feature:entry` 和 `domain:ledger` 负责。Room 的 DAO 与 Entity 均保持在
`core:database` 内部。

## 已封闭的边界

- `LedgerDatabaseHandle` 只向外返回领域层的 Repository 接口，不再泄露
  `RoomTransactionRepository` 等具体实现。
- 同一个数据库句柄复用同一组 Repository 实例，避免页面或 ViewModel 在不知情时
  生成多套数据入口。
- 草稿状态只有 `EntryViewModel.uiState` 一个运行时来源；`SavedStateHandle` 只是进程
  重建快照，不是第二套可观察状态。
- 保存由 `CreateTransaction` 统一执行；Compose 仅发送 `EntryAction`。
- 数据库初始化只在 `Application` 级创建一次，Activity 重建不会重建 Room。

## 接下来必须保持的规则

1. `app` 是装配层，可以知道 Room 工厂，但页面和 feature 模块不能依赖
   `core:database`。
2. 新页面只能依赖用例、Repository 接口或稳定的 UI 模型，不接收 DAO、Entity、
   `LedgerRoomDatabase`。
3. UI 不得根据数据库对象临时推导余额或统计；这些投影应由领域/查询系统生成。
4. `SavedStateHandle` 只保存可恢复的编辑输入，不保存 `SAVING`、错误、列表结果等
   短暂状态。
5. 长任务放在 ViewModel 的协程作用域内；Repository 不创建无主协程。
6. Compose 不接收 Repository。账户只能通过 `ManageAccounts` 写入，分类只能通过
   `CategoryManager.dispatch` 写入，交易只能通过交易用例写入；Repository 仅交给这些
   统一写入口和只读观察器。
7. 同一功能的写入入口必须串行化“读取规则所需状态 -> 校验 -> 保存”这一整段，不能只
   串行最后一次 `save`。当前账户与分类管理器已用互斥区保护，避免快速连点生成重复顺序
   或绕过重名检查。
8. 记账模式切换后的账户/分类选择由 `EntrySelectionPolicy` 一次性完成。Compose 只发送
   `ChangeMode`，禁止再连续发送“切模式、选账户、选分类”等修正动作；否则页面顺序会
   变成业务规则，并可能暴露短暂的无效状态。

## 已知但不需要现在机械拆分的事项

- `EntryViewModel` 目前仍位于 `app`，同时承担默认数据引导与页面状态协调。功能增加后，
  应把默认数据引导变成独立用例，并将 ViewModel 移到 feature 的 Android/UI 子模块。
- `Clock` 目前通过构造参数注入，边界正确；后续可统一为 `LedgerClock`，但在出现记账日
  边界或时区规则前无需提前抽象。
- 当前没有依赖注入框架。以小型 `AppGraph` 进行显式装配即可，暂时引入大型框架不会
  提升正确性。

