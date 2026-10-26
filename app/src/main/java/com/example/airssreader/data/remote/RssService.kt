package com.example.airssreader.data.remote

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * RSS 拉取服务。
 *
 * 只做一件事：给一个 URL，返回解析好的 [RssChannel]。不做缓存、不碰数据库。
 *
 * 说明：
 * 1. 解析交给 rssparser（Kotlin 实现，支持 RSS 2.0 / Atom / RDF），不手写 XML；
 * 2. 自定义 OkHttpClient 的原因——部分源会拒绝默认 UA，
 *    且移动网络下默认 10s 超时偏短，这里放宽到 20s / 总 30s；
 * 3. 本类无任何定时器或后台线程，只有被显式调用时才发起请求。
 */
class RssService {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) AiRssReader/1.0",
                )
                .header(
                    "Accept",
                    "application/rss+xml, application/atom+xml, application/xml, text/xml, */*",
                )
                .build()
            chain.proceed(request)
        }
        .build()

    private val parser: RssParser = RssParserBuilder(httpClient, Charsets.UTF_8).build()

    /**
     * 拉取并解析一个 RSS/Atom 源。
     *
     * 异常向上抛出，由 Repository 统一转成可展示的错误文案——
     * 这样错误提示的位置只有一处，不会散落在 UI 层。
     */
    suspend fun fetch(url: String): RssChannel = withContext(Dispatchers.IO) {
        parser.getRssChannel(url)
    }
}
