# 动效与手势系统

动效和手势阈值只允许从 `core:design` 读取。功能页面不得写入自己的动画时长、滑动距离或方向比例。

```text
LedgerMotionTokens
└── 立即反馈 / 控件反馈 / 页面导航时长

LedgerMotionSpecs
└── 页面导航 / 控件反馈动画规格

LedgerGestureTokens
└── 局部横滑触发距离 / 横纵方向优势比例

rememberLedgerLocalSwipeConfig
└── 合并 Android 系统 touchSlop 与项目手势 Token
```

当前主页面分页动画统一读取 `LedgerMotionSpecs.navigation()`；流水月份、日历月份和记账日期选择器
统一读取 `rememberLedgerLocalSwipeConfig()`。新增动效必须先进入组件实验室形成独立样本，再由正式组件调用。
