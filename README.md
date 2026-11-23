# AI 记账

一个自用的 Android 记账 App：手动记账 + 自动捕捉，数据全部留在本机。

当前版本：**v0.1.1** —— 手动记账、通知捕捉（6 个 App）、读屏记账（下单即记）、账单导入（支付宝/微信）、CSV/WebDAV 备份、商户→分类自学习都已可用。
范围与排期见 [docs/mvp-scope.md](docs/mvp-scope.md)。

## 界面

| 记账首页 | 报表 | 记一笔 |
| --- | --- | --- |
| ![首页](docs/screenshots/home.png) | ![报表](docs/screenshots/report_month.png) | ![记一笔](docs/screenshots/entry_sheet.png) |

选时间（记一笔里的「日期胶囊」）：

| 日期时间选择器 | 空状态 |
| --- | --- |
| ![选时间](docs/screenshots/date_picker.png) | ![空状态](docs/screenshots/home_empty.png) |

> 以上截图由 Paparazzi 在 JVM 上用 layoutlib 真实渲染，不是设计稿。

## 技术栈

- Kotlin 2.0.21 + Jetpack Compose（Material 3，自建蓝白主题）
- Room 2.6.1（KSP）+ 协程 Flow，本地 SQLite，无后端、无账号
- 单 Activity + Compose 状态驱动，无第三方图表库（趋势图用 Canvas 画），滚轮选择器也是自己实现
- 字体：**HarmonyOS Sans SC**（内置 Regular，全 App 覆盖系统字体；加粗由系统合成，授权文件见 `app/src/main/assets/fonts/`）
- 测试：JUnit4（领域逻辑）+ Paparazzi（界面快照）+ Compose UI 交互测试（真机/模拟器）

## 构建与运行

前置：JDK 17、Android SDK（platform 34 + build-tools 34.0.0）。

```bash
# 1. 指向本机 SDK
echo "sdk.dir=D:\\tools\\android-sdk" > local.properties   # 改成你的路径

# 2. 编译
./gradlew assembleDebug        # 产物 app/build/outputs/apk/debug/app-debug.apk

# 3. 测试（34 个单元测试 + 4 个界面快照）
./gradlew testDebugUnitTest

# 4. 交互测试（需要真机或模拟器）
./gradlew connectedDebugAndroidTest

# 5. 重新生成界面截图（会更新 app/src/test/snapshots 与 docs 用的图）
./gradlew recordPaparazziDebug
```

想在电脑上跑起来，可以建一个模拟器：

```bash
sdkmanager "emulator" "system-images;android-34;google_apis;x86_64" \
           "extras;google;Android_Emulator_Hypervisor_Driver"
avdmanager create avd -n aiacc -k "system-images;android-34;google_apis;x86_64" -d pixel_5
emulator -avd aiacc
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows 上要先装加速驱动（`silent_install.bat`，管理员权限），否则 x86 模拟器起不来。

## 目录结构

```
app/src/main/java/com/aiaccounting/
├── core/           金额(分)格式化、日期文案
├── domain/         纯逻辑：金额输入状态机、去重指纹、按天分组、报表聚合、日期草稿
├── data/           Room 实体/DAO/数据库、仓库、种子分类
├── ui/
│   ├── theme/      颜色与排版
│   ├── components/ 卡片、胶囊、底部导航、趋势柱状图、滚轮选择器
│   ├── home/       首页：渐变头卡 + 按日流水
│   ├── entry/      记一笔：自建键盘 + 分类宫格 + 表达式（12+8）+ 日期时间选择
│   ├── backup/     备份与恢复面板
│   └── report/     报表：周/月/年 + 趋势 + 分类占比
└── AccountingApp / MainActivity
```

## 已经能做什么

- 手动记一笔：支出/收入、10 个支出分类、4 个账户、备注、金额（支持 `12+8` 这类表达式）
- **记账时间可选**：点日期胶囊打开年/月/日 + 时:分 滚轮，确认后按所选时间入账（补记昨天、上周的账都能落到正确的那一天）
- 首页按天分组展示流水，显示今日支出、本月支出、本月收入
- 报表支持周/月/年切换与前后翻页，含支出趋势、分类占比、与上期比较
- **点任意一条流水即可编辑**（金额/分类/账户/备注/时间）**或删除**，编辑不会破坏来源与去重指纹
- **搜索**：按分类、商户、备注、账户名或金额筛选，头部今日/本月合计始终按全部数据算
- **备份与恢复**：首页右上角「设置」→「导出数据 / 导入数据」，导出成 CSV（Excel 能直接打开），也能从 CSV 覆盖恢复；走系统文件选择器，不需要存储权限
- **设置页**：只留三个入口——自动记账（收件箱 / 通知权限 / 捕捉日志）、导入数据（支付宝 / 微信 / 备份恢复 / 云端恢复）、导出数据（CSV / 上传云端 / WebDAV 配置）；账单导入带分步教程
- **支付宝账单导入**：选支付宝导出的 CSV（GBK/UTF-8 自动识别），自动跳过交易关闭/退款/不计收支的行，先给预览（可导入几笔、几笔重复）再落库；按「交易号 + 金额/10 分钟时间窗」双重判重，不会和手动记的、通知抓的重复
- **微信账单导入**：选微信导出的 xlsx，自己解 zip + XML（不引 Apache POI），把 Excel 序列号时间还原成真实时间；与支付宝共用预览和去重逻辑
- **商户 → 分类自学习**：手动改过一次某商户的分类，就记成规则；以后通知捕捉、账单导入遇到这个商户直接落到对应分类（支持「瑞幸咖啡（某店）」这类分店名的最长匹配）
- **WebDAV 云备份带填写引导**：坚果云地址自动预填，面板里直接给了三步操作说明和一键打开坚果云网页

![云备份](docs/screenshots/webdav.png)

![设置](docs/screenshots/settings.png)
- **WebDAV 云备份**：填坚果云地址 + 应用密码即可上传/恢复，可开「自动备份」——每次保存、删除、恢复后自动推一份到网盘。客户端是自己实现的（`HttpURLConnection` + Basic 认证），没引三方库
- **自动记账（通知捕捉）**：监听微信 / 支付宝 / 淘宝 / 京东 / 美团 / 抖音的支付通知，规则解析出金额/方向/商户，先落进「待确认收件箱」，一键确认才计入账本。聊天、红包、转账、营销推送一律放过——宁可漏判也不误判
- **读屏记账（下单即记）**：淘宝/京东/抖音/美团这类 App 内下单，真正扣款和通知可能延后 7 天甚至更久，所以用无障碍服务读订单页文字，下单当场就记一笔；退款页识别后记成收入。金额必须跟在「实付款 / 确认收货后付 / 退款金额」这类标签后面才取，页面上裸的 `¥xx`（商品原价）一律不认
- **捕捉日志**：设置里能看最近捕捉到的通知原文、是否解析成功、失败原因，方便按真实文案调规则
- **「在其他应用上层」状态保持**：挂一个 1×1 全透明、不接收触摸的 overlay 窗口，界面上看不到任何东西；用前台服务 + 通知监听服务双层承载，App 被划掉/被回收后会自动恢复
- 三路来源的数据模型已经预留：`TransactionEntity` 有 `source`（手动/通知/导入）、`dedupKey`、`status`，`AccountingRepository.addCapturedTransaction()` 已实现「同指纹不重复入库」

待确认收件箱：

![待确认](docs/screenshots/inbox.png)

## 已知限制

- **Android 17 真机上 Compose UI 测试跑不了**：Espresso 还在调用系统已移除的 `InputManager.getInstance()`。交互测试目前只在模拟器（Android 14）上跑；真机安装和使用不受影响。
- **通知捕捉需要手动授权**：系统「通知使用权」里给 App 打开，且支付 App 要允许「通知显示详情」，否则通知里没有金额。
- **淘宝/京东/抖音不推「支付成功」通知**：实测这类 App 内下单，推送里只有营销、未读提醒、退款评价，没有金额。所以 App 内消费要靠「读屏记账」或账单导入，通知这条路对它们无效。
- **前台支付可能抓不到**：在微信/支付宝里当面扫码付款时 App 在前台，系统未必弹通知；这类漏账靠手动补或账单导入兜底。
- **读屏记账需要无障碍权限**：覆盖安装 APK 后系统会自动关闭无障碍服务，需要重新到「设置 → 无障碍」里开启。
- **手动「强行停止」会清掉一切**：设置里点强制停止后，任何后台服务（含 overlay 窗口）都被系统封锁，需要重新打开 App 一次。
- **退款不计入原单**：退款目前记为一笔独立收入，不自动冲抵原来的支出，月度支出会略偏高。

## 下一步

1. 真机跑一周，收集真实通知文案，按 `raw_captures` 表里的原文调解析规则（见 MVP 文档 5.1）
2. **自动记账按来源细分开关（0.3 目标）**：设置 → 自动记账 改成「来源」列表，每个渠道一行带独立开关——
   短信通知 / 微信 / 支付宝 / 淘宝 / 京东 / 美团 / 抖音 / 小米支付 / 截屏记账 / 账单导入，
   想抓哪个勾哪个，关掉的不再监听、不再留日志
3. 云备份升级：改成后台定时（WorkManager），现在只在 App 运行时触发
4. 待确认记录的批量确认

## 开发约定

见 [AGENTS.md](AGENTS.md)：每次改动都要有对应的 commit，并在交付前保证测试通过。
