# 数据模型

## 第一版核心实体

### Transaction

- `id: TransactionId`
- `type: EXPENSE | INCOME | TRANSFER`
- `money: Money`
- `occurredAt: Instant`
- `sourceAccountId: AccountId?`
- `destinationAccountId: AccountId?`
- `categoryId: CategoryId?`
- `note: String?`
- `createdAt / updatedAt`
- `importFingerprint: String?`
- `deletedAt: Instant?`

转账仍然是一笔交易，只是同时有来源和目标账户。金额本身保持非负，方向由交易类型决定，避免同时依赖“负数”和“支出类型”产生矛盾。

### Account

- `id`
- `name`
- `kind`
- `currency`
- `openingBalance`
- `displayOrder`
- `archivedAt`

余额是“期初余额 + 已确认交易”的投影，不在每个页面单独保存一个容易失真的数字。

### Category

- `id`
- `parentId?`
- `name`
- `transactionType`
- `displayOrder`
- `archivedAt`

第一版支持两级分类，但模型不把“一级/二级”写死在不同表中。

### Tag / TransactionTag

标签独立保存，并通过关联表与交易建立多对多关系。备注仍属于交易自身。

### Budget

- `period: YearMonth`
- `limit: Money`
- `categoryId: CategoryId?`

V1 只开放总月预算，字段保留分类扩展能力。

## 导入模型

导入不会直接生成正式交易，流程是：

```text
原始文件
  → SourceRow
  → 字段映射
  → ImportDraft
  → 校验 / 账户分类匹配 / 去重
  → 用户预览
  → 一次数据库事务提交
  → ImportReport
```

当前账单导出包含 23 列，包括日期、收支类型、金额、两级分类、账户、账本、退款、优惠、备注、标签、报销、多币种、地址和五个附件位。

第一版导入映射：

| 源字段 | V1 处理 |
|---|---|
| 日期、收支类型、金额 | 必填并严格校验 |
| 类别、二级分类 | 自动创建或映射，提交前可修改 |
| 账户 | 自动创建或映射 |
| 备注、标签 | 保留 |
| 地址 | 暂存为导入扩展字段，不在首页展示 |
| 账本 | 记录来源；V1 合并到默认账本 |
| 退款、优惠、报销、多币种、附件 | 原样进入扩展载荷并在报告中提示，V1 不伪装成已支持 |

## 金额与时间

- 金额使用 `Long` 保存最小货币单位，例如人民币分；
- 禁止用 `Float` 或 `Double` 做金额运算；
- 数据库存 UTC 时间点；
- “哪一天”由用户时区和可配置记账日边界计算；
- 月度统计通过统一的时间范围服务生成，禁止页面各算一套。

## 数据兼容

每次数据库变更都必须有：

1. 显式 schema 版本；
2. 向前迁移脚本；
3. 迁移前后数据断言；
4. 真实脱敏样本回归测试；
5. 可恢复备份。

