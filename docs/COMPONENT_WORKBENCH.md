# Component Workbench

组件实验室现在是应用的默认启动入口。正式记账界面保留为 `PRODUCT PREVIEW`，只用于验证组件装配结果。

```text
Component Workbench（无数据库依赖）
├── Design Tokens
├── Base Components
├── Business Components
├── Page Frames
├── Interaction Tests
└── Product Preview（主动进入后才读取真实业务状态）
```

## 规则

1. 新组件必须先进入实验室，覆盖正常、禁用、空白、加载、错误、长文本和极端数值。
2. 实验室直接调用正式组件，不允许绘制一份仅用于展示的仿制品。
3. 正式页面只能组合经过实验室验证的组件，不得在页面内部重新实现同类控件。
4. 同一种业务对象默认只有一个组件和一个事件接口；确需变体时必须显式命名并共享业务规则。
5. 实验室使用固定假数据，不读取或修改用户账单。

当前第一批已覆盖颜色、字体、间距、圆角、按钮、Surface、金额、状态、长文本，以及流水条目和流水列表。
