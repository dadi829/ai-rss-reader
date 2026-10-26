# AI 资讯 RSS 阅读器

一个只做 AI 方向内容的轻量 RSS 阅读器，Kotlin + Jetpack Compose 实现。

课程设计项目。核心目标是**把「订阅 → 拉取 → 缓存 → 检索」这条链路做完整**，
而不是堆功能：没有推送、没有后台任务、没有账号体系，所有网络动作都由用户主动触发。

---

## 1. 功能

### 资讯流（首页）
- 顶部第一项是**「全部」聚合视图**：所有源的内容按发布时间混排，
  按「今天 / 昨天 / 本周 / 更早」分组，卡片带源标识色点，一眼能看出这条来自哪个源
- 右侧横向切换单个源，切换只查本地缓存，**不发起网络请求**
- 聚合视图下刷新 = 并发拉取全部源（4 并发，单源失败不影响其他源）；
  单源视图下刷新 = 只拉当前源
- 卡片展示标题、摘要、发布时间；未读用左侧圆点标记
- 点击卡片进入**原生详情页**（本地渲染，断网可读），详情页底部再跳原文

### 资讯详情
- 标题、来源、完整发布时间、完整摘要全部从 Room 读取，**断网也能读完一条**
- 进入即标记已读
- 底部固定「阅读原文」按钮 → 内置 WebView

### 源管理
- 预置 8 个 AI 垂类 RSS 源，覆盖国内媒体 / 论文 / 官方博客 / 海外媒体
- 手动添加任意 RSS 地址：**添加时先真实拉取一次**，验证地址有效性并抓取源标题
- 删除源：外键级联，该源已缓存的资讯一并清理
- 每个源显示条目数、未读数、上次拉取时间

### 全局搜索
- 搜索范围是**所有源已缓存的资讯**，同时匹配标题与摘要
- 跨源检索，结果标注来源；关键词高亮
- 完全离线可用，断网也能搜

### 本地缓存
- 成功拉取的条目落 Room，下次打开直接读缓存，不需要先刷新
- 同一源内按 `(feedId, link)` 唯一，重复刷新不会重置已读状态

---

## 2. 关于「不做后台」

这是刻意的设计取舍，不是功能缺失：

| 不做 | 原因 |
| --- | --- |
| WorkManager / JobScheduler | 定时拉取会让「用户主动触发」这个前提失效，也带来耗电与流量问题 |
| 前台 / 后台 Service | 阅读器不需要常驻进程 |
| 推送 / FCM | 资讯阅读不需要打断式通知 |
| 定时任务 / AlarmManager | 同上 |

唯一申请的系统权限是 `INTERNET`。

需要说明的是：**下拉刷新、切源、聚合视图批量刷新都不违反这个原则**——
下拉是用户手势，切源只读本地 Room 缓存，批量刷新同样由用户点按钮触发。
真正的约束是「没有用户在等的时候，不联网」。

---

## 3. 技术栈

| 层 | 选型 |
| --- | --- |
| 语言 / UI | Kotlin 2.0.21、Jetpack Compose（Material 3，BOM 2024.10.01） |
| 架构 | 单 Activity + Navigation Compose，MVVM + Repository |
| 持久化 | Room 2.6.1（KSP 编译期生成 DAO 实现） |
| 网络 | OkHttp 4.12.0 |
| RSS 解析 | [rssparser](https://github.com/prof18/RSS-Parser) 6.0.4（Kotlin 实现，支持 RSS 2.0 / Atom / RDF） |
| 异步 | Kotlin Coroutines + Flow |
| 构建 | AGP 8.7.3、Gradle 8.9、JDK 17 |

`minSdk 26` / `targetSdk 35` / `compileSdk 35`。

---

## 4. 项目结构

```
app/src/main/java/com/example/airssreader/
├── App.kt                          Application，持有依赖容器 + 首次启动播种
├── MainActivity.kt                 唯一 Activity
├── core/
│   ├── state/FeedSelection.kt      跨页面共享的「当前选中源」（含「全部」虚拟 id）
│   └── util/
│       ├── TextSanitizer.kt        HTML → 纯文本、摘要截断
│       ├── RssDateParser.kt        多格式发布时间解析
│       ├── DateGrouper.kt          聚合视图的日期分组（今天/昨天/本周/更早）
│       └── DateFormatter.kt        相对时间 / 绝对时间展示
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          Room 数据库
│   │   ├── dao/                    FeedDao / ArticleDao
│   │   ├── entity/                 FeedEntity / ArticleEntity + 查询投影
│   │   └── seed/DefaultFeeds.kt    预置源
│   ├── remote/
│   │   ├── RssService.kt           OkHttp + rssparser 封装
│   │   └── RssMapper.kt            网络模型 → 实体
│   ├── repository/                 FeedRepository / ArticleRepository
│   └── mapper/EntityMapper.kt      实体 → 领域模型
├── domain/model/                   Feed / Article
├── di/AppContainer.kt              手写依赖容器
└── ui/
    ├── theme/                      颜色、尺寸、字号、形状、主题、源标识色
    ├── component/                  顶栏、搜索框、资讯卡片、源切换条、状态页
    ├── navigation/AppNavHost.kt    路由表
    ├── feed/                       资讯流（聚合 + 单源）
    ├── detail/                     资讯详情（本地渲染）
    ├── sources/                    源管理
    ├── search/                     全局搜索
    └── reader/                     WebView 阅读页
```

### 数据模型

```
feeds                          articles
├── id (PK)                    ├── id (PK)
├── title                      ├── feedId (FK → feeds.id, CASCADE)
├── url (UNIQUE)               ├── title
├── lastFetchedAt              ├── link
└── createdAt                  ├── summary
                               ├── publishedAt (INDEX)
                               ├── isRead
                               └── cachedAt
                               UNIQUE(feedId, link)
```

两个关键约束：
- **外键 CASCADE**：删源自动清理该源所有条目，不留孤儿数据
- **`(feedId, link)` 唯一 + `OnConflictStrategy.IGNORE`**：重复刷新静默跳过已存在条目，
  已读状态因此不会被刷新重置

### 三处值得讲的技术点

1. **聚合查询**：`ArticleDao.observeAll()` 用 `INNER JOIN feeds` 一次查出条目 + 来源名，
   避免在内存里按 feedId 逐条查源（N+1）。`LIMIT 300` 是刻意取舍——
   8 个源各缓存 60 条能到 480 条，全塞进 `LazyColumn` 会拖慢首帧。
2. **批量刷新的并发控制**：`refreshAll()` 用 `chunked(4)` + `async/awaitAll` 分块并发，
   块内并行、块间串行；单源异常用 `runCatching` 就地吞掉，计数用 `Mutex` 保护
   （并发协程写同一个 `var` 不是原子操作）。
3. **日期分组的边界**：用 `LocalDate` 比较而不是 `时间差 / 86400000`，
   后者在跨时区和夏令时切换时会算错，而且「今天」应按本地日历日界定。

---

## 5. 预置源

| 源 | 地址 |
| --- | --- |
| 量子位 | `https://www.qbitai.com/feed` |
| InfoQ AI | `https://www.infoq.cn/feed/AI` |
| 雷锋网 | `https://www.leiphone.com/feed` |
| arXiv cs.AI | `https://export.arxiv.org/rss/cs.AI` |
| HuggingFace Blog | `https://huggingface.co/blog/feed.xml` |
| Google AI Blog | `https://blog.google/technology/ai/rss/` |
| DeepMind Blog | `https://deepmind.google/blog/rss.xml` |
| MIT Tech Review · AI | `https://www.technologyreview.com/topic/artificial-intelligence/feed/` |

顺序不是随意的：中文源排在最前，首次打开加载最快、内容也最直观。

几处与常见写法不同，都是 2026-10 逐个实测后的调整：

1. **arXiv** 用 `export.arxiv.org` 而不是 `arxiv.org/rss/cs.AI`——后者会 302 跳转一次。
   另外 arXiv 单次返回 300+ 条，`RssMapper.MAX_ITEMS_PER_FETCH` 限制为 60 条，
   避免首屏和搜索被拖慢。
2. **机器之心** `jiqizhixin.com/rss` 已下线（302 到网页），
   **DeepLearning.AI The Batch** 的 `/rss`、`/feed/`、`/feed` 全部 404，
   因此替换为 InfoQ AI 与 Google AI Blog。
3. **Anthropic** 官方 RSS 返回 404、**VentureBeat** 返回 429 限流，未采用。

HuggingFace 与 Google / DeepMind / MIT 的源在境内直连可能不通，需要代理环境。
无代理时聚合视图刷新会提示「已更新 N 条，M 个源刷新失败」，其余国内源不受影响。

---

## 6. 构建

### 环境要求
- JDK 17
- Android SDK Platform 35 + Build-Tools 35.0.0
- Gradle 8.9（仓库自带 wrapper）

### 命令行

```bash
# 指向本机 SDK
echo "sdk.dir=/path/to/android-sdk" > local.properties

# 编译
./gradlew :app:assembleDebug

# 产物
app/build/outputs/apk/debug/app-debug.apk
```

Windows 下用 `gradlew.bat`。

### Android Studio
`File → Open` 选择项目根目录，等待 Gradle Sync 完成后
直接点 Run，或 `Build → Build APK(s)`。

---

## 7. 使用流程（演示路径）

1. 首次启动 → 自动写入 8 个预置源，默认停在**「全部」聚合视图**
2. 点右上角 **刷新** → 并发拉取全部源，列表按时间倒序、按日期分组出现卡片
3. 切到「量子位」→ 只显示该源内容，刷新只拉这一个源
4. 点任意卡片 → **原生详情页**，看到完整摘要；点底部「阅读原文」进 WebView
5. 点右上角 **搜索** → 输入 `模型` → 结果跨多个源，命中词高亮
6. 点右上角 **源管理** → 添加一个 RSS 地址（会先验证）→ 删除一个源（确认后级联清缓存）
7. 断网 → 详情页、列表、搜索全部仍可用（走本地缓存）

---

## 8. 已知限制

- 只做了 `fallbackToDestructiveMigration`，没有写正式的 Migration（课程设计阶段表结构仍在调整）
- 只存 RSS 提供的摘要，不做正文抓取，因此详情页展示的是摘要而非全文
- 摘要截断按字符数而非按行高，超长英文标题在窄屏上会占满 3 行
- 部分站点用 JS 渲染正文，WebView 里可能显示不全
- 预置源只在库中一个源都没有时写入；装过旧版本后升级，需清数据或手动添加新增的两个源

---

## License

课程设计作业，仅供学习参考。
