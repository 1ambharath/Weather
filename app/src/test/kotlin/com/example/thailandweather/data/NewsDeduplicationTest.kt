package com.example.thailandweather.data

import com.example.thailandweather.core.database.dao.NewsDao
import com.example.thailandweather.core.database.entity.ArticleEntity
import com.example.thailandweather.core.model.Article
import com.example.thailandweather.data.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsDeduplicationTest {

    private val fakeDao = object : NewsDao {
        override fun getAllArticlesFlow(): Flow<List<ArticleEntity>> = flowOf(emptyList())
        override fun getBookmarkedArticlesFlow(): Flow<List<ArticleEntity>> = flowOf(emptyList())
        override suspend fun insertArticles(articles: List<ArticleEntity>) {}
        override suspend fun updateBookmark(id: String, isBookmarked: Boolean) {}
        override suspend fun deleteOldArticles(cutoff: Long) {}
    }

    private val repository = NewsRepository(fakeDao)

    @Test
    fun testSyndicatedDuplicateArticlesAreDeduplicated() {
        val now = System.currentTimeMillis()
        val article1 = Article(
            id = "art_1",
            title = "Heavy Rain and Flooding Threatens Coastal Provinces in Thailand",
            summary = "TMD warns of sudden flash floods.",
            publisher = "Bangkok Post",
            publishedAt = now,
            url = "https://bangkokpost.com/1",
            category = "Severe Weather"
        )

        // Nearly identical syndicated re-post with minor word ordering / punctuation
        val article2 = Article(
            id = "art_2",
            title = "Heavy Rain and Flooding Threatens Coastal Provinces in Thailand!",
            summary = "Flash flooding risks announced.",
            publisher = "Syndicated Wire",
            publishedAt = now + 1000,
            url = "https://news.com/2",
            category = "Severe Weather"
        )

        val article3 = Article(
            id = "art_3",
            title = "Chiang Mai Experiences Severe PM2.5 Inversion Hazard",
            summary = "Air quality reaches unhealthy levels.",
            publisher = "Thai PBS",
            publishedAt = now + 2000,
            url = "https://thaipbs.com/3",
            category = "Air Quality"
        )

        val rawList = listOf(article1, article2, article3)
        val deduplicated = repository.deduplicateArticles(rawList)

        // article2 should be filtered out, leaving article1 and article3
        assertEquals(2, deduplicated.size)
        assertTrue(deduplicated.any { it.id == "art_1" })
        assertTrue(deduplicated.any { it.id == "art_3" })
    }

    @Test
    fun testDistinctArticlesArePreserved() {
        val now = System.currentTimeMillis()
        val list = listOf(
            Article("1", "TMD Warning No. 1", "desc 1", "TMD", now, "url1", "Severe Weather"),
            Article("2", "Phuket Marine Advisory: High Swells", "desc 2", "The Nation", now, "url2", "Marine"),
            Article("3", "Chao Phraya River Discharge Update", "desc 3", "DDPM", now, "url3", "Floods")
        )

        val deduplicated = repository.deduplicateArticles(list)
        assertEquals(3, deduplicated.size)
    }
}
