# 统一系统入口

页面只负责组合，不允许重新定义全局规则。当前系统入口如下：

```text
视觉系统 core:design
├── LedgerColorTokens / NothingLedgerTheme.colors   颜色
├── LedgerTypography / LedgerTextStyles             字体与语义字号
├── LedgerSpacing                                   间距
├── LedgerRadius                                    圆角
└── LedgerComponents                                基础控件

交互系统 core:design
├── LedgerMotionTokens / LedgerMotionSpecs          动效时长与规格
├── LedgerGestureTokens                             手势阈值
├── rememberLedgerLocalSwipeConfig                  平台相关滑动配置
└── HorizontalSwipeState                            方向锁和手势判定

导航系统
├── RootNavigationReducer                           实验室与产品预览
├── WorkbenchReducer                                实验室局部聚焦
└── LedgerNavigationReducer                         正式产品页面栈

业务展示系统
├── TransactionListItem / TransactionList           流水
└── CalendarDayCell                                 日历日期格
```

## 边界

- 新字体或字号先进入字体系统和实验室，页面不得直接指定新的字体族。
- 新颜色先成为语义颜色，页面不得写十六进制颜色。
- 新动画先定义语义动效规格并在实验室验证，页面不得直接写 `tween` 时长。
- 新手势先定义统一阈值和判定器，页面只提交操作结果。
- 完整页面跳转必须发送导航动作；组件只上报事件，不决定去哪个页面。
- 组件内部尺寸由该组件唯一持有；跨组件共享的尺寸才提升为全局 Token。
