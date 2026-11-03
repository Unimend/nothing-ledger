# 参考原则

我们会研究其他项目的结构和取舍，但不以任何项目为代码基底，也不把 GPL 项目的实现复制到本仓库。

## 值得借鉴

- [Expense Manager](https://github.com/nkuppan/expensemanager)：Compose、Room、账户与预算的模块边界；
- [SpendWise](https://github.com/wansiong50-pixel/spendwise)：轻量本地账本、整数金额与 JSON 备份思路；
- [TraceLedger](https://github.com/GreenIcePhoenix/TraceLedger)：多来源导入、预览与去重流程；
- [My Expenses](https://github.com/mtotschnig/MyExpenses)：成熟产品对导入、账户和复杂交易的长期处理经验；
- [Ivy Wallet](https://github.com/Ivy-Apps/ivy-wallet)：已归档项目，可研究交互与产品取舍，不作为代码来源。

## 借鉴方式

1. 先写清楚我们的问题和约束；
2. 比较多个项目如何处理同一问题；
3. 记录设计结论，而不是复制实现；
4. 保留来源链接和许可证意识；
5. 用自己的领域模型和接口重新实现；
6. 所有设计必须通过我们的真实使用数据验证。

