package com.github.terrakok.dctalks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.dctalks.service.Article
import com.github.terrakok.dctalks.service.ArticleService
import kotlinx.coroutines.launch

class ArticlesViewModel : ViewModel() {
    private val articleService = ArticleService()

    private var currentPage = 1
    private var allArticles = listOf<Article>()

    var uiState by mutableStateOf<ArticleUiState>(ArticleUiState.Loading(allArticles))
        private set

    init {
        refresh()
    }

    fun loadMore() {
        if (uiState is ArticleUiState.Loading) return
        loadPage(currentPage + 1)
    }

    private fun loadPage(page: Int) {
        viewModelScope.launch {
            try {
                uiState = ArticleUiState.Loading(if (page == 1) emptyList() else allArticles)
                val articlePage = articleService.getArticles(page)
                val result = if (page == 1) articlePage.articles else allArticles + articlePage.articles
                allArticles = result.distinctBy { it.url }
                currentPage = articlePage.pagination.currentPage
                uiState = ArticleUiState.Success(allArticles)
            } catch (e: Exception) {
                uiState = ArticleUiState.Error(allArticles, e.message ?: "Unknown error occurred")
            }
        }
    }

    fun refresh() {
        loadPage(1)
    }
}

sealed class ArticleUiState {
    abstract val articles: List<Article>

    data class Loading(override val articles: List<Article>) : ArticleUiState()
    data class Success(override val articles: List<Article>) : ArticleUiState()
    data class Error(override val articles: List<Article>, val message: String) : ArticleUiState()
}