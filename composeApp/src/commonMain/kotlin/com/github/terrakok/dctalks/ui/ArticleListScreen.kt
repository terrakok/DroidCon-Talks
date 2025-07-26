package com.github.terrakok.dctalks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import com.github.terrakok.dctalks.ImageLoader
import com.github.terrakok.dctalks.service.Article
import com.github.terrakok.dctalks.viewmodel.ArticleUiState
import com.github.terrakok.dctalks.viewmodel.ArticlesViewModel
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.vectorResource
import droidcon_talks.composeapp.generated.resources.Res
import droidcon_talks.composeapp.generated.resources.ic_rotate_right

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    viewModel: ArticlesViewModel = viewModel { ArticlesViewModel() }
) {
    val uriHandler = LocalUriHandler.current
    val uiState = viewModel.uiState
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                shadowElevation = 8.dp,
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("DroidCon Talks") },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_rotate_right),
                                contentDescription = "Refresh"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is ArticleUiState.Loading if uiState.articles.isEmpty() -> {
                    // Show loading indicator only if we don't have any articles yet
                    LoadingIndicator(Modifier.align(Alignment.Center).padding(paddingValues))
                }

                is ArticleUiState.Error if uiState.articles.isEmpty() -> {
                    // Show error message if we don't have any articles
                    ErrorMessage(
                        message = uiState.message,
                        onRetry = viewModel::refresh,
                        modifier = Modifier.align(Alignment.Center).padding(paddingValues)
                    )
                }

                else -> {
                    ArticleList(
                        articles = uiState.articles,
                        onArticleClick = { article ->
                            article.url?.let { uriHandler.openUri(it) }
                        },
                        isLoading = uiState is ArticleUiState.Loading,
                        onRefresh = viewModel::refresh,
                        onLoadMore = viewModel::loadMore,
                        contentPadding = paddingValues,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleList(
    articles: List<Article>,
    onArticleClick: (Article) -> Unit,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    errorMessage: String? = null,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val direction = LocalLayoutDirection.current
    val pad = PaddingValues(
        start = contentPadding.calculateStartPadding(direction) + 16.dp,
        end = contentPadding.calculateEndPadding(direction) + 16.dp,
        top = contentPadding.calculateTopPadding() + 16.dp,
        bottom = contentPadding.calculateBottomPadding() + 16.dp,
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(350.dp),
        contentPadding = pad,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        itemsIndexed(articles) { index, article ->
            ArticleItem(
                article = article,
                onClick = { onArticleClick(article) }
            )
            if (index == articles.lastIndex) {
                onLoadMore()
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }
        }

        if (errorMessage != null) {
            item {
                ErrorMessage(
                    message = errorMessage,
                    onRetry = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ArticleItem(
    article: Article,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.clickable(onClick = onClick).height(240.dp)
        ) {

            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xffff7256))
                    .weight(1f),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = article.avatarUrl,
                            imageLoader = ImageLoader,
                            contentDescription = "Speaker avatar",
                            modifier = Modifier.size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background, CircleShape)
                        )
                        Column {
                            Text(
                                text = article.speakerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                maxLines = 1,
                            )
                            Text(
                                text = article.speakerDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatDate(article.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (article.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = article.tags.joinToString(", ") { "#$it" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private fun formatDate(date: LocalDate): String {
    return "${date.month.name.lowercase().capitalize()} ${date.dayOfMonth}, ${date.year}"
}

private fun String.capitalize(): String {
    return if (isNotEmpty()) {
        this[0].uppercase() + substring(1)
    } else {
        this
    }
}