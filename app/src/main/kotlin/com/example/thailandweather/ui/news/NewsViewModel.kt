package com.example.thailandweather.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thailandweather.core.model.Article
import com.example.thailandweather.data.repository.NewsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NewsUiState(
    val articles: List<Article> = emptyList(),
    val filteredArticles: List<Article> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val selectedArticle: Article? = null
)

class NewsViewModel(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState(isRefreshing = false))
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        observeNews()
    }

    private fun observeNews() {
        viewModelScope.launch {
            newsRepository.allArticles.collect { list ->
                _uiState.update { state ->
                    val filtered = applyFilter(list, state.selectedCategory, state.searchQuery)
                    state.copy(articles = list, filteredArticles = filtered, isRefreshing = false)
                }
            }
        }
    }

    fun refreshNews(isUserInitiated: Boolean = true) {
        if (_uiState.value.isRefreshing && isUserInitiated) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val startTime = System.currentTimeMillis()

            try {
                val result = newsRepository.refreshArticles()
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 600) {
                    delay(600 - elapsed)
                }
                if (isUserInitiated) {
                    val message = if (result.hasNewArticles) "loaded fresh contents" else "No fresh updates"
                    _toastMessage.emit(message)
                }
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 600) {
                    delay(600 - elapsed)
                }
                if (isUserInitiated) {
                    _toastMessage.emit("No fresh updates")
                }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.articles, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredArticles = filtered)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.articles, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredArticles = filtered)
        }
    }

    fun toggleBookmark(article: Article) {
        viewModelScope.launch {
            newsRepository.toggleBookmark(article.id, !article.isBookmarked)
        }
    }

    fun openArticle(article: Article?) {
        _uiState.update { it.copy(selectedArticle = article) }
    }

    private fun applyFilter(list: List<Article>, category: String, query: String): List<Article> {
        return list.filter { item ->
            val matchesCategory = when (category) {
                "All" -> true
                "Bookmarks" -> item.isBookmarked
                else -> item.category.equals(category, ignoreCase = true)
            }
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.summary.contains(query, ignoreCase = true) ||
                    item.publisher.contains(query, ignoreCase = true) ||
                    item.province?.contains(query, ignoreCase = true) == true
            matchesCategory && matchesQuery
        }
    }

    class Factory(private val newsRepo: NewsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewsViewModel(newsRepo) as T
        }
    }
}
