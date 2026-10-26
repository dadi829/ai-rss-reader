# 新工作区交接说明

这是第二个课程设计 App 的工作区。需求由用户在新会话里给出，此处只记录环境与约定，避免重复摸索。

## 本机工具链（已装好，不用重装）

| 组件 | 路径 |
| --- | --- |
| JDK 17 (Temurin) | `D:\agent\tools\jdk-17.0.20.1+1` |
| Android SDK | `D:\agent\tools\android-sdk`（platforms/android-35、build-tools/35.0.0） |
| Gradle 8.9 | `D:\agent\tools\gradle-8.9` |

用户级环境变量已设置：`JAVA_HOME`、`ANDROID_HOME`、`ANDROID_SDK_ROOT`、`GRADLE_HOME`，PATH 已含三者的 bin。**新开的终端才生效**。

## 项目初始化要点

1. 建 `local.properties`，内容 `sdk.dir=D\:\\agent\\tools\\android-sdk`（此文件不入库）。
2. `settings.gradle.kts` 的仓库顺序：阿里云 google / public 在前，`google()`、`mavenCentral()` 兜底。
3. 编译：`gradle :app:assembleDebug`，产物在 `app/build/outputs/apk/debug/app-debug.apk`。
   首次约 3 分钟（下依赖），之后 40-60 秒。

## 本机网络坑

- curl / git 走 HTTPS 报 `CRYPT_E_REVOCATION_OFFLINE` → 加 `--ssl-no-revoke`。
- 代理 `127.0.0.1:62703` 是 WorkBuddy 沙箱层，不是 CC Switch；系统代理是 Clash Verge（7900）。
- Gradle 发行包走 `mirrors.cloud.tencent.com/gradle`，JDK 走清华 Adoptium 镜像，GitHub 下载可用 `gh-proxy.com` 前缀。

## 沿用上个项目的技术约定

Kotlin + Jetpack Compose + Room + MVVM，单 Activity + Navigation Compose，纯本地无网络。
包名先用占位，App 名定了再改。

## 设计红线（用户反复强调）

- 禁满屏卡片、禁紫粉亮蓝主色、禁大面积渐变与装饰图形
- 圆角 4/8/12dp 封顶，按钮不用胶囊形
- 颜色 / 间距 / 字号全部走 `ui/theme` 常量，页面里不出现裸数值
- 区块优先 0.5dp 发丝分割线，卡片按需使用
- 列表用 Lazy 系列，状态页（加载/空/错误/无权限）统一封装
- 支持深色模式

上一个项目在 `D:\agent\projects\android-course-design`，可作参考实现。

---

## 交接状态（2026-10-26）

上一个项目「挂号通」已完成并交付：

- 代码：`D:/agent/projects/android-course-design`，git 已初始化，main 分支，tag v1.0.0
- 编译：本地 `gradle :app:assembleDebug` 通过，产物 17MB，debug 签名
- 交付：APK 已通过企业微信发给用户
- 未完成：GitHub 仓库尚未创建（缺凭据）；CI 工作流与 `scripts/publish_github.py` 已写好待用

### 本项目「AI 资讯 RSS 阅读器」已完成

- 包名 `com.example.airssreader`，App 名「AI资讯RSS」，minSdk 26 / target 35
- 编译通过（零警告），APK 17MB：`dist/AI资讯RSS阅读器-v1.0-debug.apk`
- 代码已 git init 并提交（main 分支，1 个 commit）
- **未做真机验证**：本机没有模拟器（SDK 无 emulator 组件）也没连设备，只做了静态验证

### 与原始提示词的偏离（用户确认过）

1. 保留了下拉刷新，切源只读缓存不联网——原提示词把「手动触发」写死成只有刷新按钮
2. 加了已读标记 + 未读数 + 全部已读
3. 预置源替换了两个：机器之心 RSS 已下线（302 到网页）、The Batch `/rss` 返回 404，
   换成 InfoQ AI 与 Google AI Blog；arXiv 改用 `export.arxiv.org` 直连地址
4. 预置源从 5 个变 6 个（加 MIT Tech Review AI）

### 下次注意

- rssparser 6.0.4 的 API 是 `RssParserBuilder(client, Charsets.UTF_8).build()` + `getRssChannel()`，
  不是老版本的 `Parser.Builder()`；`RssChannel`/`RssItem` 是模型类名
- 五个源的实际 pubDate 全是 RFC 1123 变体，`DateTimeFormatter.RFC_1123_DATE_TIME` 全覆盖
- arXiv 单次返回 321 条，已在 `RssMapper.MAX_ITEMS_PER_FETCH` 限制为 60

