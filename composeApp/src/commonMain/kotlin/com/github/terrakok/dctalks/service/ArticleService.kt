package com.github.terrakok.dctalks.service

import co.touchlab.kermit.Logger
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class Article(
    val title: String,
    val description: String,
    val url: String?,
    val avatarUrl: String?,
    val speakerTitle: String,
    val speakerDesc: String,
    val tags: List<String>,
    val date: LocalDate,
)

data class Pagination(
    val currentPage: Int,
    val totalPages: Int,
    val nextPageUrl: String?
)

data class ArticlePage(
    val articles: List<Article>,
    val pagination: Pagination
)

internal expect val corsProxy: String

class ArticleService(
    debug: Boolean = false,
) {
    private val droidconUrl = "https://www.droidcon.com/wp-admin/admin-ajax.php"
    private val allDroidcons = "droidcon-americas,droidcon-apac,droidcon-berlin,droidcon-emea,droidcon-lagos,droidcon-london,droidcon-new-york,droidcon_san_francisco,droidcon-turin"
    private val pageSize = 100

    private val parser = ArticleParser()
    private val httpClient = HttpClient {
        followRedirects = true
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    Logger.d("HTTP Client") { "\n\t" + message }
                }
            }
            level = if (debug) LogLevel.ALL else LogLevel.NONE
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

    suspend fun getArticles(page: Int): ArticlePage = withContext(Dispatchers.Default) {
        val response: HttpResponse = httpClient.submitForm(
            url = corsProxy + droidconUrl,
            formParameters = parameters {
                append("action", "us_ajax_grid")
                append("ajax_url", droidconUrl)
                append("infinite_scroll", "0")
                append("max_num_pages", "5000")
                append("template_vars", queryParamsJsonString(page, pageSize))
            }
        )
        val html = response.bodyAsText()
        parser.parseHtml(html)
    }

    private fun queryParamsJsonString(
        pageNumber: Int,
        pageSize: Int,
        tags: String = allDroidcons
    ): String = """
        {
          "columns": "4",
          "exclude_items": "none",
          "img_size": "default",
          "ignore_items_size": false,
          "items_layout": "1314",
          "load_animation": "none",
          "overriding_link": "none",
          "query_args": {
            "post_type": [ "post" ],
            "posts_per_page": "$pageSize",
            "paged": $pageNumber
          },
          "orderby_query_args": {
            "orderby": { "date": "DESC" }
          },
          "type": "grid",
          "us_grid_ajax_index": 1,
          "us_grid_filter_params": "filter_category=video&filter_post_event_tag=$tags",
          "us_grid_index": $pageNumber,
          "_us_grid_post_type": "post"
        }
    """.trimIndent().replace("\\s+".toRegex(), "")
}

@OptIn(ExperimentalTime::class)
private class ArticleParser {
    private val dateRegex = """https?://[^/]+/(\d{4})/(\d{2})/(\d{2})/.*""".toRegex()

    fun parseHtml(html: String): ArticlePage {
        val doc: Document = Ksoup.parse(html)

        // Parse articles
        val articleElements: Elements = doc.select("article.w-grid-item")
        val articles = articleElements.map { parseArticle(it) }
            .filter { it.tags.any { tag -> tag.startsWith("droidcon") } }

        // Parse pagination
        val pagination = parsePagination(doc)

        return ArticlePage(articles, pagination)
    }

    private fun parseArticle(articleElement: Element): Article {
        val titleElement = articleElement.selectFirst("h2.droidcon_conference_teaser_txt_block")
        val title = titleElement?.text().orEmpty()
        val description = articleElement.selectFirst("div.post_content")?.text().orEmpty()
        val url = articleElement.selectFirst("a.usg_btn_1")?.attr("href")

        val avatarUrl = corsProxy + articleElement.selectFirst("img.attachment-thumbnail")?.attr("src")
        val speakerTitle = articleElement.selectFirst("div.video_post_speaker_title")?.text().orEmpty()
        val speakerDesc = articleElement.selectFirst("div.video_post_speaker_desc")?.text().orEmpty()

        // Extract tags from class names (starts with "tag-")
        val tags = articleElement.classNames()
            .filter { it.startsWith("tag-") }
            .map { it.removePrefix("tag-") }

        val date = url?.let { dateRegex.find(it) }?.let { matchResult ->
            val (year, month, day) = matchResult.destructured
            LocalDate(year.toInt(), month.toInt(), day.toInt())
        } ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        return Article(
            title = title,
            description = description,
            url = url,
            avatarUrl = avatarUrl,
            speakerTitle = speakerTitle,
            speakerDesc = speakerDesc,
            tags = tags,
            date = date,
        )
    }

    private fun parsePagination(doc: Document): Pagination {
        val currentPage = doc.selectFirst("span.page-numbers.current")?.text()?.toIntOrNull() ?: 1
        val totalPages = doc.select("a.page-numbers")
            .mapNotNull { it.text().toIntOrNull() }
            .maxOrNull() ?: currentPage
        val nextPageUrl = doc.selectFirst("a.next.page-numbers")?.attr("href")

        return Pagination(
            currentPage = currentPage,
            totalPages = totalPages,
            nextPageUrl = nextPageUrl
        )
    }
}
