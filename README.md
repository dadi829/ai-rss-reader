# AI 资讯 RSS 阅读器

一个只做 AI 方向内容的轻量 RSS 阅读器，Kotlin + Jetpack Compose 实现。

课程设计项目。核心目标是**把「订阅 → 拉取 → 缓存 → 检索」这条链路做完整**，
而不是堆功能：没有推送、没有后台任务、没有账号体系，所有网络动作都由用户主动触发。

---

## 1. 功能

### 源管理
- 预置 6 个 AI 垂类 RSS 源，覆盖论文 / 官方博客 / 国内媒体 / 海外媒体
- 手动添加任意 RSS 地址：**添加时先真实拉取一次**，验证地址有效性并抓取源标题
- 删除源：外键级联，该源已缓存的资讯一并清理
- 每个源显示条目数、未读数、上次拉取时间

### 资讯流
- 顶部横向切换源，切换只查本地缓存，**不发起网络请求**
- 点击刷新按钮或下拉列表，才去请求当前源的 RSS
- 卡片展示标题、摘要、发布时间；未读用左侧圆点标记
- 点击卡片进入内置 WebView 阅读原文，同时置为已读

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

需要说明的是：**下拉刷新和切换源都不违反这个原则**——
下拉是用户手势，切源只读本地 Room 缓存。真正的约束是「没有用户在等的时候，不联网」。

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
│   ├── state/FeedSelection.kt      跨页面共享的「当前选中源」
│   └── util/
│       ├── TextSanitizer.kt        HTML → 纯文本、摘要截断
│       ├── RssDateParser.kt        多格式发布时间解析
│       └── DateFormatter.kt        相对时间展示
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
    ├── theme/                      颜色、尺寸、字号、形状、主题
    ├── component/                  顶栏、搜索框、资讯卡片、状态页等
    ├── navigation/AppNavHost.kt    路由表
    ├── feed/                       资讯流
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

---

## 5. 预置源

| 源 | 地址 |
| --- | --- |
| arXiv cs.AI | `https://export.arxiv.org/rss/cs.AI` |
| HuggingFace Blog | `https://huggingface.co/blog/feed.xml` |
| 量子位 | `https://www.qbitai.com/feed` |
| InfoQ AI | `https://www.infoq.cn/feed/AI` |
| Google AI Blog | `https://blog.google/technology/ai/rss/` |
| MIT Tech Review · AI | `https://www.technologyreview.com/topic/artificial-intelligence/feed/` |

两处与常见写法不同，是实测后的调整：

1. **arXiv** 用 `export.arxiv.org` 而不是 `arxiv.org/rss/cs.AI`——后者会 302 跳转一次。
   另外 arXiv 单次返回 300+ 条，`RssMapper.MAX_ITEMS_PER_FETCH` 限制为 60 条，
   避免首屏和搜索被拖慢。
2. **机器之心** `jiqizhixin.com/rss` 已下线（302 到网页），
   **DeepLearning.AI The Batch** 的 `/rss` 返回 404，因此替换为 InfoQ AI 与 Google AI Blog。
   这两个地址实测稳定返回 20 条。

HuggingFace 与 Google / MIT 的源在境内直连可能不通，需要代理环境。
若演示环境无代理，可在源管理里删掉它们，其余三个国内源（量子位、InfoQ、arXiv）可正常拉取。

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

1. 首次启动 → 自动写入 6 个预置源，资讯流默认选中第一个
2. 点右上角 **刷新**（或下拉）→ 拉取 arXiv，入库并显示卡片
3. 顶部横向切到「量子位」→ 列表为空，因为还没拉过 → 再点刷新
4. 点右上角 **搜索** → 输入 `模型` → 结果跨多个源，命中词高亮
5. 点卡片 → WebView 打开原文 → 返回后该条未读点消失
6. 点右上角 **源管理** → 添加一个 RSS 地址（会先验证）→ 删除一个源（确认后级联清缓存）
7. 断网 → 搜索和已缓存列表仍可用

---

## 8. 已知限制

- 只做了 `fallbackToDestructiveMigration`，没有写正式的 Migration（课程设计阶段表结构仍在调整）
- 摘要截断按字符数而非按行高，超长英文标题在窄屏上会占满 3 行
- 部分站点用 JS 渲染正文，WebView 里可能显示不全，可点右上角用系统浏览器打开
- 没有实现全文抓取（只存 RSS 提供的摘要），因此搜索范围是摘要而非正文

---

## License

课程设计作业，仅供学习参考。
