package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.config.SourceConfig
import com.example.data.model.PriceSearchResult
import com.example.data.model.StoreDiagnosticInfo
import com.example.data.model.StoreStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LoadingView
import com.example.ui.viewmodel.PriceSearchViewModel
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PriceSearchScreen(
    viewModel: PriceSearchViewModel,
    onOpenUrlInWebView: (String, String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    if (showDiagnosticsDialog) {
        StoreDiagnosticsDialog(
            diagnostics = uiState.storeDiagnostics,
            onToggleStore = { storeId, enabled -> viewModel.toggleStore(storeId, enabled) },
            onCopyDiagnostic = { info ->
                val text = viewModel.getDiagnosticClipboardText(info)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("StoreDiagnostic", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "${info.storeName} tanı bilgisi panoya kopyalandı!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDiagnosticsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fiyat Karşılaştır", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshDiagnostics()
                        showDiagnosticsDialog = true
                    }) {
                        Icon(Icons.Default.SettingsSuggest, contentDescription = "Kaynak Tanı Ekranı")
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
            // Arama Kutusu
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Örn: hikvision 2mp ip kamera...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.query.isNotBlank()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle")
                                }
                            }
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.search()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(end = 4.dp),
                                enabled = uiState.query.isNotBlank() && !uiState.isSearchingLayer1
                            ) {
                                Text("Ara")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        viewModel.search()
                    }),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Son Aramalar Öneri Çipleri
            if (recentSearches.isNotEmpty() && !uiState.isSearchingLayer1 && uiState.results.isEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "Son Aramalar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentSearches.forEach { query ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    viewModel.onQueryChange(query)
                                    viewModel.search(query)
                                },
                                label = { Text(query, maxLines = 1) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.deleteRecentSearch(query) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Sil", modifier = Modifier.size(12.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Arama İlerleme Çubuğu
            if (uiState.isSearchingLayer1 || uiState.isSearchingBackground) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isSearchingLayer1) "Karşılaştırma siteleri taranıyor (Cimri, Akakçe)..."
                        else "Mağazalar arka planda taranıyor (canlı güncelleniyor)...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Sonuçlar / Boş Durum
            when {
                uiState.isSearchingLayer1 && uiState.results.isEmpty() -> {
                    LoadingView(message = "En ucuz fiyatlar araştırılıyor...")
                }
                uiState.results.isEmpty() && uiState.searchedAtLeastOnce -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyStateView(
                            icon = Icons.Default.LocalOffer,
                            title = "Sonuç Bulunamadı",
                            description = "Aradığınız kriterlere uygun ürün doğrudan çekilemedi. Aşağıdaki mağazalarda tek tıkla arayabilirsiniz:"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FallbackDirectStoreButtons(query = uiState.query, context = context)
                    }
                }
                uiState.results.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EN UCUZ 5 SONUÇ",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${uiState.results.size} mağaza listelendi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        items(uiState.results, key = { it.id }) { item ->
                            PriceResultCard(
                                item = item,
                                onClick = {
                                    openStoreProduct(item, context, onOpenUrlInWebView)
                                }
                            )
                        }

                        // Listenin altında veri gelmeyen mağazalar için "Bu mağazada ara" butonları
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Diğer Mağazalarda Ara",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FallbackDirectStoreButtons(query = uiState.query, context = context)
                        }

                        // Bilgi & Uyarı Notu
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Fiyatlar anlık olarak değişebilir. Satın almadan önce lütfen mağaza sayfasından güncel fiyatı doğrulayın.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Bot engeli olan mağazalar atlanarak en hızlı sonuçlar sunulur.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                else -> {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "Fiyat Karşılaştırmaya Başlayın",
                        description = "Cimri, Akakçe, Vatan, Teknosa ve popüler mağazalardaki en ucuz 5 fiyatı bulmak için yukarıya ürün adı yazın."
                    )
                }
            }
        }
    }
}

@Composable
fun PriceResultCard(
    item: PriceSearchResult,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Görsel
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(72.dp)
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Bilgiler
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mağaza Etiketi
                    Surface(
                        color = getStoreBadgeColor(item.storeName),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.storeName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // En Ucuz Rozeti
                    if (item.isCheapest) {
                        Surface(
                            color = Color(0xFF16A34A),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "★ EN UCUZ",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.priceFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.isCheapest) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mağazaya Git",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FallbackDirectStoreButtons(query: String, context: Context) {
    val encoded = remember(query) {
        try {
            URLEncoder.encode(query.trim(), "UTF-8")
        } catch (_: Exception) {
            query.trim()
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SourceConfig.PriceSearch.STORES.forEach { store ->
            val directUrl = store.searchUrlTemplate.replace("{query}", encoded)
            OutlinedButton(
                onClick = {
                    openDirectStoreLink(directUrl, store.appPackageName, context)
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(store.displayName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Kaynak Durumu / Tanı Ekranı Dialogu
 */
@Composable
fun StoreDiagnosticsDialog(
    diagnostics: Map<String, StoreDiagnosticInfo>,
    onToggleStore: (String, Boolean) -> Unit,
    onCopyDiagnostic: (StoreDiagnosticInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kaynak Durumu & Tanı", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Bir mağaza hata verirse 'Tanıyı Kopyala' butonuna basarak hata raporunu panoya alabilir ve seçicileri düzelttirebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(diagnostics.values.toList(), key = { it.storeId }) { info ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = info.storeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Katman ${info.layer}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(
                                    checked = info.isEnabled,
                                    onCheckedChange = { onToggleStore(info.storeId, it) }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val statusColor = when (info.status) {
                                    StoreStatus.SUCCESS -> Color(0xFF16A34A)
                                    StoreStatus.BLOCKED -> Color(0xFFDC2626)
                                    StoreStatus.SELECTOR_BROKEN -> Color(0xFFD97706)
                                    StoreStatus.TIMEOUT -> Color(0xFF6B7280)
                                    StoreStatus.DISABLED -> Color.Gray
                                }
                                Text(
                                    text = "Durum: ${info.status.displayName}",
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Ürün: ${info.foundCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (info.status != StoreStatus.SUCCESS && info.status != StoreStatus.DISABLED) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { onCopyDiagnostic(info) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tanı Bilgisini Kopyala", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

private fun openStoreProduct(
    item: PriceSearchResult,
    context: Context,
    onOpenUrlInWebView: (String, String) -> Unit
) {
    if (!item.appPackageName.isNullOrBlank()) {
        try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(item.appPackageName)
            if (launchIntent != null) {
                val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.productUrl)).apply {
                    setPackage(item.appPackageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
                return
            }
        } catch (_: Exception) {}
    }

    // Mağaza uygulaması yoksa dahili WebView veya Tarayıcı
    try {
        onOpenUrlInWebView(item.productUrl, "${item.storeName} - ${item.title}")
    } catch (_: Exception) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.productUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    }
}

private fun openDirectStoreLink(url: String, appPackage: String?, context: Context) {
    if (!appPackage.isNullOrBlank()) {
        try {
            val pm = context.packageManager
            if (pm.getLaunchIntentForPackage(appPackage) != null) {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage(appPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appIntent)
                return
            }
        } catch (_: Exception) {}
    }

    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(browserIntent)
}

private fun getStoreBadgeColor(storeName: String): Color {
    val name = storeName.lowercase()
    return when {
        name.contains("cimri") -> Color(0xFF0284C7)
        name.contains("akakçe") || name.contains("akakce") -> Color(0xFF0369A1)
        name.contains("vatan") -> Color(0xFF1D4ED8)
        name.contains("teknosa") -> Color(0xFFF97316)
        name.contains("itopya") -> Color(0xFF7C3AED)
        name.contains("gürgençler") -> Color(0xFF0D9488)
        name.contains("n11") -> Color(0xFFE11D48)
        name.contains("pazarama") -> Color(0xFF4338CA)
        name.contains("hepsiburada") -> Color(0xFFEA580C)
        name.contains("trendyol") -> Color(0xFFD97706)
        name.contains("amazon") -> Color(0xFF1F2937)
        else -> Color(0xFF475569)
    }
}
