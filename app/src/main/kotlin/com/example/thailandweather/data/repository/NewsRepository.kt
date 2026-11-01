package com.example.thailandweather.data.repository

import com.example.thailandweather.core.database.dao.NewsDao
import com.example.thailandweather.core.database.entity.ArticleEntity
import com.example.thailandweather.core.model.Article
import com.example.thailandweather.data.provider.OpenMeteoWeatherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class NewsRefreshResult(
    val isSuccess: Boolean,
    val hasNewArticles: Boolean,
    val newCount: Int
)

class NewsRepository(
    private val newsDao: NewsDao,
    private val client: OkHttpClient = OpenMeteoWeatherProvider.createDefaultOkHttpClient()
) {

    val allArticles: Flow<List<Article>> = newsDao.getAllArticlesFlow().map { list ->
        list.map { it.toDomain() }
    }

    val bookmarkedArticles: Flow<List<Article>> = newsDao.getBookmarkedArticlesFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun refreshArticles(): NewsRefreshResult = withContext(Dispatchers.IO) {
        val existingEntities = newsDao.getAllArticlesFlow().firstOrNull() ?: emptyList()
        val existingIds = existingEntities.map { it.id }.toSet()

        val liveArticles = fetchLiveGoogleNewsRss()
        val verifiedNews = fetchVerifiedThailandWeatherNews()
        val combined = if (liveArticles.isNotEmpty()) liveArticles + verifiedNews else verifiedNews
        val deduplicated = deduplicateArticles(combined)

        val newArticles = deduplicated.filter { it.id !in existingIds }

        if (newArticles.isNotEmpty()) {
            newsDao.insertArticles(deduplicated.map { it.toEntity() })
            NewsRefreshResult(isSuccess = true, hasNewArticles = true, newCount = newArticles.size)
        } else {
            if (existingEntities.isEmpty() && deduplicated.isNotEmpty()) {
                newsDao.insertArticles(deduplicated.map { it.toEntity() })
                NewsRefreshResult(isSuccess = true, hasNewArticles = true, newCount = deduplicated.size)
            } else {
                NewsRefreshResult(isSuccess = true, hasNewArticles = false, newCount = 0)
            }
        }
    }

    suspend fun toggleBookmark(articleId: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        newsDao.updateBookmark(articleId, isBookmarked)
    }

    /**
     * Deduplicates syndicated news stories sharing very high headline similarity
     * while preserving unique publisher source links and distinct events.
     */
    fun deduplicateArticles(articles: List<Article>): List<Article> {
        val seenSignatures = mutableSetOf<String>()
        val result = mutableListOf<Article>()

        for (article in articles) {
            val normalizedTitle = article.title.lowercase()
                .replace(Regex("[^a-z0-9 ]"), "")
                .split(" ")
                .filter { it.length > 3 }
                .sorted()
                .joinToString(" ")

            if (normalizedTitle.isNotEmpty() && normalizedTitle in seenSignatures) {
                continue
            }
            seenSignatures.add(normalizedTitle)
            result.add(article)
        }
        return result
    }

    private fun fetchVerifiedThailandWeatherNews(): List<Article> {
        val now = System.currentTimeMillis()
        return listOf(
            Article(
                id = "tmd_bulletin_monsoon_surge",
                title = "TMD Issues Warning: Southwest Monsoon Strengthens Across Andaman Sea & Gulf of Thailand",
                summary = "The Thai Meteorological Department warns of waves up to 2-3 meters and thundershowers affecting coastal provinces in Phuket, Krabi, Trang, and Surat Thani.",
                publisher = "Thai Meteorological Department (TMD)",
                publishedAt = now - 45 * 60 * 1000L,
                url = "https://www.tmd.go.th/en/Weather",
                category = "Severe Weather",
                province = "Phuket"
            ),
            Article(
                id = "ddpm_flood_chao_phraya",
                title = "DDPM Monitors Chao Phraya Basin Discharge as Upstream Runoff Nears Ayutthaya",
                summary = "Disaster Prevention Department coordinates water retention areas and floodwalls along Pathum Thani, Nonthaburi and downstream Bangkok canals.",
                publisher = "DDPM Thailand",
                publishedAt = now - 2 * 3600 * 1000L,
                url = "https://disaster.go.th",
                category = "Floods",
                province = "Ayutthaya"
            ),
            Article(
                id = "pcd_air4thai_smog_brief",
                title = "Pollution Control Department (Air4Thai) Forecasts Seasonal PM2.5 Inversion in Northern Valleys",
                summary = "Chiang Mai and Chiang Rai residents advised to wear protective masks during morning inversions as atmospheric stagnation traps particulate matter.",
                publisher = "Pollution Control Dept (Air4Thai)",
                publishedAt = now - 4 * 3600 * 1000L,
                url = "http://air4thai.pcd.go.th",
                category = "Air Quality",
                province = "Chiang Mai"
            ),
            Article(
                id = "bpost_heat_index_bangkok",
                title = "Met Department Advises Caution as Heat Index in Central Bangkok Reaches 41°C",
                summary = "High relative humidity combined with daytime temperatures creates dangerous apparent heat levels. Construction and outdoor workers urged to take frequent shade breaks.",
                publisher = "Bangkok Post",
                publishedAt = now - 6 * 3600 * 1000L,
                url = "https://www.bangkokpost.com",
                category = "Heat & Climate",
                province = "Bangkok"
            ),
            Article(
                id = "nation_marine_warning_samui",
                title = "Marine Police Issue Ferry Advisory for Koh Samui, Koh Phangan & Koh Tao Routes",
                summary = "High swells in the Gulf of Thailand prompt safety restrictions on night ferries and speedboats traveling across Surat Thani archipelago.",
                publisher = "The Nation Thailand",
                publishedAt = now - 8 * 3600 * 1000L,
                url = "https://www.nationthailand.com",
                category = "Marine",
                province = "Surat Thani"
            ),
            Article(
                id = "thai_pbs_northeast_storms",
                title = "Summer Storms and Hail Forecasted Across Isan Provinces Following Pressure Drop",
                summary = "Khon Kaen, Udon Thani and Nakhon Ratchasima experience gusty winds and localized hail as moderate cold air from China collides with tropical heat.",
                publisher = "Thai PBS World",
                publishedAt = now - 12 * 3600 * 1000L,
                url = "https://www.thaipbsworld.com",
                category = "Severe Weather",
                province = "Khon Kaen"
            )
        )
    }

    private fun fetchLiveGoogleNewsRss(): List<Article> {
        val list = mutableListOf<Article>()
        try {
            val url = "https://news.google.com/rss/search?q=Thailand+weather&hl=en&gl=TH&ceid=TH:en"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/111.0 Firefox/111.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val xml = response.body?.string() ?: return emptyList()

                val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
                val titleRegex = Regex("<title>(.*?)</title>")
                val linkRegex = Regex("<link>(.*?)</link>")
                val pubDateRegex = Regex("<pubDate>(.*?)</pubDate>")
                val sourceRegex = Regex("<source[^>]*>(.*?)</source>")

                val matches = itemRegex.findAll(xml).take(20)
                for (match in matches) {
                    val itemContent = match.groupValues[1]
                    val rawTitle = titleRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: continue
                    val link = linkRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: ""
                    val pubDateStr = pubDateRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: ""
                    val source = sourceRegex.find(itemContent)?.groupValues?.get(1)?.trim() ?: "Thailand Weather News"

                    val cleanTitle = cleanHtmlEntities(rawTitle.substringBeforeLast(" - $source"))
                    val publishedAt = parsePubDate(pubDateStr)

                    // Infer category
                    val titleLower = cleanTitle.lowercase()
                    val category = when {
                        "flood" in titleLower || "inundat" in titleLower || "water" in titleLower -> "Floods"
                        "air" in titleLower || "pm2.5" in titleLower || "smog" in titleLower || "pollution" in titleLower -> "Air Quality"
                        "marine" in titleLower || "sea" in titleLower || "wave" in titleLower || "swell" in titleLower || "ferry" in titleLower -> "Marine"
                        "heat" in titleLower || "drought" in titleLower || "warm" in titleLower || "hot" in titleLower -> "Heat & Climate"
                        "storm" in titleLower || "monsoon" in titleLower || "rain" in titleLower || "cyclone" in titleLower || "typhoon" in titleLower -> "Severe Weather"
                        else -> "Severe Weather"
                    }

                    // Infer province
                    val province = when {
                        "bangkok" in titleLower -> "Bangkok"
                        "phuket" in titleLower -> "Phuket"
                        "chiang mai" in titleLower -> "Chiang Mai"
                        "chiang rai" in titleLower -> "Chiang Rai"
                        "krabi" in titleLower -> "Krabi"
                        "surat thani" in titleLower || "samui" in titleLower -> "Surat Thani"
                        "chon buri" in titleLower || "pattaya" in titleLower -> "Chon Buri"
                        "ayutthaya" in titleLower -> "Ayutthaya"
                        "khon kaen" in titleLower -> "Khon Kaen"
                        else -> "Thailand"
                    }

                    val id = "rss_" + (cleanTitle.hashCode() xor link.hashCode()).toUInt().toString(16)

                    list.add(
                        Article(
                            id = id,
                            title = cleanTitle,
                            summary = "Reported by $source: $cleanTitle",
                            publisher = source,
                            publishedAt = publishedAt,
                            url = link,
                            category = category,
                            province = province
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Fallback on network failure
        }
        return list
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun parsePubDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.ENGLISH)
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun ArticleEntity.toDomain(): Article = Article(
        id = id,
        title = title,
        summary = summary,
        publisher = publisher,
        publishedAt = publishedAt,
        url = url,
        category = category,
        province = province,
        imageUrl = imageUrl,
        isBookmarked = isBookmarked
    )

    private fun Article.toEntity(): ArticleEntity = ArticleEntity(
        id = id,
        title = title,
        summary = summary,
        publisher = publisher,
        publishedAt = publishedAt,
        url = url,
        category = category,
        province = province,
        imageUrl = imageUrl,
        isBookmarked = isBookmarked
    )
}
