package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.config.SourceConfig
import com.example.data.model.Article
import com.example.ui.components.ArticleCard
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorRetryView
import com.example.ui.components.LoadingView
import com.example.ui.components.OfflineBanner
import com.example.ui.viewmodel.NewsSourceFilter
import com.example.ui.viewmodel.NewsTabMode
import com.example.ui.viewmodel.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    showDebugTag: Boolean,
    onArticleClick: (Article) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedArticles by viewModel.savedArticles.collectAsStateWithLifecycle()
    var articleToDelete by remember { mutableStateOf<Article?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()

    val currentArticles = when (uiState.tabMode) {
        NewsTabMode.LATEST -> {
            when (uiState.sourceFilter) {
                NewsSourceFilter.ALL -> uiState.articles
                NewsSourceFilter.BBC -> uiState.articles.filter { it.sourceId == SourceConfig.News.BbcTurkce.ID }
                NewsSourceFilter.SON_DAKIKA -> uiState.articles.filter { it.sourceId == SourceConfig.News.SonDakika.ID }
            }
        }
        NewsTabMode.SAVED -> savedArticles
    }

    if (articleToDelete != null) {
        ConfirmDeleteDialog(
            title = "Haberi Sil",
            message = "\"${articleToDelete?.title}\" kaydedilenlerden kaldırılsın mı?",
            onConfirm = {
                articleToDelete?.let { viewModel.deleteSavedArticle(it.url) }
                articleToDelete = null
            },
            onDismiss = { articleToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HaberVolt", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadNews() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OfflineBanner(visible = uiState.isOfflineWarning)

            PrimaryTabRow(
                selectedTabIndex = uiState.tabMode.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                NewsTabMode.entries.forEach { mode ->
                    Tab(
                        selected = uiState.tabMode == mode,
                        onClick = { viewModel.setTabMode(mode) },
                        text = {
                            Text(
                                if (mode == NewsTabMode.SAVED) "${mode.displayName} (${savedArticles.size})"
                                else mode.displayName
                            )
                        }
                    )
                }
            }

            if (uiState.tabMode == NewsTabMode.LATEST) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(NewsSourceFilter.entries.toTypedArray()) { filter ->
                        FilterChip(
                            selected = uiState.sourceFilter == filter,
                            onClick = { viewModel.setSourceFilter(filter) },
                            label = { Text(filter.displayName) }
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.loadNews() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.articles.isEmpty() -> {
                        LoadingView(message = "Son dakika haberleri yükleniyor...")
                    }
                    uiState.errorMessage != null && currentArticles.isEmpty() -> {
                        ErrorRetryView(
                            message = uiState.errorMessage ?: "Hata oluştu",
                            onRetry = { viewModel.loadNews() }
                        )
                    }
                    currentArticles.isEmpty() -> {
                        if (uiState.tabMode == NewsTabMode.SAVED) {
                            EmptyStateView(
                                icon = Icons.Default.BookmarkBorder,
                                title = "Henüz Kaydedilen Haber Yok",
                                description = "İlgilendiğiniz haberleri yıldız ikonuna dokunarak buraya kaydedebilirsiniz."
                            )
                        } else {
                            EmptyStateView(
                                icon = Icons.Default.Newspaper,
                                title = "Haber Bulunamadı",
                                description = "Seçilen kaynak için şu anda içerik bulunmuyor.",
                                actionText = "Yenile",
                                onAction = { viewModel.loadNews() }
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(currentArticles, key = { it.url }) { article ->
                                ArticleCard(
                                    article = article,
                                    showDebugTag = showDebugTag,
                                    showDeleteButton = uiState.tabMode == NewsTabMode.SAVED,
                                    onClick = { onArticleClick(article) },
                                    onToggleSave = { viewModel.toggleSave(article) },
                                    onDelete = { articleToDelete = article }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
