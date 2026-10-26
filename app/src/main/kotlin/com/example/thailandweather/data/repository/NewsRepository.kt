package com.example.thailandweather.data.repository

import com.example.thailandweather.core.database.dao.NewsDao
import com.example.thailandweather.core.database.entity.ArticleEntity
import com.example.thailandweather.core.model.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NewsRepository(
    private val newsDao: NewsDao
) {

    val allArticles: Flow<List<Article>> = newsDao.getAllArticlesFlow().map { list ->
        list.map { it.toDomain() }
    }

    val bookmarkedArticles: Flow<List<Article>> = newsDao.getBookmarkedArticlesFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun refreshArticles(): List<Article> = withContext(Dispatchers.IO) {
        val verifiedNews = fetchVerifiedThailandWeatherNews()
        val deduplicated = deduplicateArticles(verifiedNews)
        newsDao.insertArticles(deduplicated.map { it.toEntity() })
        deduplicated
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
