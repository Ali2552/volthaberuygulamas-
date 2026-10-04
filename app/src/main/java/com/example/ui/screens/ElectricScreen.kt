package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
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
import com.example.data.model.Article
import com.example.data.model.ElectricalCategory
import com.example.ui.components.ArticleCard
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorRetryView
import com.example.ui.components.LoadingView
import com.example.ui.components.OfflineBanner
import com.example.ui.viewmodel.ElectricViewModel
import com.example.ui.viewmodel.NewsTabMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricScreen(
    viewModel: ElectricViewModel,
    showDebugTag: Boolean,
    onArticleClick: (Article) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedArticles by viewModel.savedArticles.collectAsStateWithLifecycle()
    var articleToDelete by remember { mutableStateOf<Article?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()

    val sourceList = if (uiState.tabMode == NewsTabMode.LATEST) uiState.articles else savedArticles
    val currentArticles = viewModel.filterArticles(sourceList)

    if (articleToDelete != null) {
        ConfirmDeleteDialog(
            title = "Haberi Sil",
            message = "\"${articleToDelete?.title}\" kaydedilenlerden kaldırılsın mı?",
            onConfirm = {
                articleToDelete?.let { viewModel.deleteSaved(it.url) }
                articleToDelete = null
            },
            onDismiss = { articleToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elektrik & Enerji", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadArticles() },
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

            // Filters: Categories & Today
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Today filter
                item {
                    FilterChip(
                        selected = uiState.isTodayOnly,
                        onClick = { viewModel.toggleTodayOnly() },
                        leadingIcon = {
                            Icon(Icons.Default.Today, contentDescription = null)
                        },
                        label = { Text("Bugün") }
                    )
                }

                // Category chips
                items(ElectricalCategory.entries.toTypedArray()) { cat ->
                    FilterChip(
                        selected = uiState.selectedCategory == cat,
                        onClick = { viewModel.setCategory(cat) },
                        label = { Text(cat.title) }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.loadArticles() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.articles.isEmpty() -> {
                        LoadingView(message = "Sektörel haberler çekiliyor...")
                    }
                    uiState.errorMessage != null && currentArticles.isEmpty() -> {
                        ErrorRetryView(
                            message = uiState.errorMessage ?: "Haberler yüklenemedi",
                            onRetry = { viewModel.loadArticles() }
                        )
                    }
                    currentArticles.isEmpty() -> {
                        if (uiState.tabMode == NewsTabMode.SAVED) {
                            EmptyStateView(
                                icon = Icons.Default.BookmarkBorder,
                                title = "Kayıtlı Elektrik Haberi Yok",
                                description = "İlgilendiğiniz makaleleri yıldız ikonuna dokunarak kaydedebilirsiniz."
                            )
                        } else {
                            EmptyStateView(
                                icon = Icons.Default.ElectricBolt,
                                title = "Makale Bulunamadı",
                                description = "Seçilen filtre kriterlerine uygun makale bulunamadı.",
                                actionText = "Filtreleri Sıfırla",
                                onAction = {
                                    viewModel.setCategory(ElectricalCategory.ALL)
                                    if (uiState.isTodayOnly) viewModel.toggleTodayOnly()
                                }
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
