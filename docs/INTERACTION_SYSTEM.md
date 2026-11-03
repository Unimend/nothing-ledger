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
## 点阵涟漪

- `LedgerFeedbackHost` 是全应用唯一的反馈覆盖层，位于页面与导航结果之上。
- `LedgerDotRippleSurface` 是可点击圆角表面的统一入口，只负责上报根布局中的触点坐标。
- 点击反馈只绘制固定尺寸的方形点阵，不改变键帽或卡片本身亮度。
- 点阵从实际触点向外扩散，时长、点数、点大小与扩散半径统一由 `LedgerMotionTokens`、`LedgerMotionSpecs` 和 `LedgerRippleTokens` 管理。
- 点击事件与覆盖层动画彼此独立：即使事件立即触发导航，动画也会在新页面上方播放完整。
- 每次点击都会建立自己的动画实例；快速连续点击不会取消或重置前一个涟漪，最多同时保留 16 个实例。
- 涟漪颜色来自全项目语义色板，不在动画代码中维护另一套随机颜色。
- 组件不负责决定页面跳转；因此组件实验室可以只演示动效，产品页面可以接入导航系统。
