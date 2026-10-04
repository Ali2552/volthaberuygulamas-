package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Article
import com.example.ui.components.SettingsDialog
import com.example.ui.components.WebViewScreen
import com.example.ui.screens.ElectricScreen
import com.example.ui.screens.NewsScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PriceSearchScreen
import com.example.ui.screens.WeatherScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppPreferencesViewModel
import com.example.ui.viewmodel.ElectricViewModel
import com.example.ui.viewmodel.NewsViewModel
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.PriceSearchViewModel
import com.example.ui.viewmodel.WeatherViewModel
import kotlinx.coroutines.delay

enum class MainTab(val title: String, val icon: ImageVector) {
    NEWS("Haberler", Icons.Default.Newspaper),
    ELECTRIC("Elektrik", Icons.Default.ElectricBolt),
    WEATHER("Hava", Icons.Default.WbSunny),
    PRICE_SEARCH("Fiyat Ara", Icons.Default.Search),
    NOTES("Notlar", Icons.Default.EditNote)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefViewModel: AppPreferencesViewModel = viewModel()
            val themeMode by prefViewModel.themeMode.collectAsStateWithLifecycle()
            val showDebugTags by prefViewModel.showDebugTags.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1500) // Splash 1.5s
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    MainAppContent(
                        prefViewModel = prefViewModel,
                        themeMode = themeMode,
                        showDebugTags = showDebugTags
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "TK",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "T.K. Bilişim",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "HaberVolt",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MainAppContent(
    prefViewModel: AppPreferencesViewModel,
    themeMode: String,
    showDebugTags: Boolean
) {
    var currentTab by remember { mutableStateOf(MainTab.NEWS) }
    var activeWebViewUrl by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // ViewModels for 5 tabs
    val newsViewModel: NewsViewModel = viewModel()
    val electricViewModel: ElectricViewModel = viewModel()
    val weatherViewModel: WeatherViewModel = viewModel()
    val priceSearchViewModel: PriceSearchViewModel = viewModel()
    val notesViewModel: NotesViewModel = viewModel()

    if (showSettingsDialog) {
        SettingsDialog(
            themeMode = themeMode,
            showDebugTags = showDebugTags,
            onThemeChange = { prefViewModel.setThemeMode(it) },
            onDebugTagsChange = { prefViewModel.setShowDebugTags(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // In-App WebView Navigation (For news or store products)
    if (activeWebViewUrl != null) {
        val (url, title) = activeWebViewUrl!!
        WebViewScreen(
            url = url,
            title = title,
            onBack = { activeWebViewUrl = null }
        )
        return
    }

    // Back button navigation: If not on NEWS, go back to NEWS tab first
    BackHandler(enabled = currentTab != MainTab.NEWS) {
        currentTab = MainTab.NEWS
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.NEWS -> NewsScreen(
                    viewModel = newsViewModel,
                    showDebugTag = showDebugTags,
                    onArticleClick = { article ->
                        activeWebViewUrl = Pair(article.url, article.title)
                    }
                )
                MainTab.ELECTRIC -> ElectricScreen(
                    viewModel = electricViewModel,
                    showDebugTag = showDebugTags,
                    onArticleClick = { article ->
                        activeWebViewUrl = Pair(article.url, article.title)
                    }
                )
                MainTab.WEATHER -> WeatherScreen(
                    viewModel = weatherViewModel,
                    showDebugTag = showDebugTags
                )
                MainTab.PRICE_SEARCH -> PriceSearchScreen(
                    viewModel = priceSearchViewModel,
                    onOpenUrlInWebView = { url, title ->
                        activeWebViewUrl = Pair(url, title)
                    }
                )
                MainTab.NOTES -> NotesScreen(
                    viewModel = notesViewModel
                )
            }

            // Quick floating settings icon button at top end
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp)
                    .size(36.dp)
            ) {
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ayarlar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
