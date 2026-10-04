package com.example.data.config

/**
 * Merkezi Kaynak Yapılandırması (SourceConfig)
 *
 * Haberler, Elektrik Sektörü, MGM Hava Durumu ve Fiyat Arama yapılandırmaları.
 * Herhangi bir sitenin HTML yapısı veya servis URL'i değişirse yalnızca bu dosya güncellenir.
 */
object SourceConfig {

    // Ağ İstekleri Yapılandırması
    const val NETWORK_TIMEOUT_SECONDS = 15L
    const val MAX_RETRIES = 2
    const val RETRY_BACKOFF_BASE_MS = 800L
    const val CIRCUIT_BREAKER_DURATION_MS = 10 * 60 * 1000L // 10 dakika devre kesici

    val USER_AGENTS = listOf(
        // Mobil Chrome (Öncelikli)
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36",
        // Masaüstü Chrome
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
        // Mobil Safari / iOS
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
    )

    val DEFAULT_HEADERS = mapOf(
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        "Cache-Control" to "no-cache",
        "Pragma" to "no-cache"
    )

    // 1) HABER KAYNAKLARI
    object News {
        object BbcTurkce {
            const val ID = "bbc_turkce"
            const val DISPLAY_NAME = "BBC Türkçe"
            const val BASE_URL = "https://www.bbc.com/turkce"
            const val PRIMARY_RSS = "https://feeds.bbci.co.uk/turkce/rss.xml"
            val FALLBACK_RSS = listOf(
                "https://www.bbc.com/turkce/index.xml",
                "https://feeds.bbci.co.uk/turkce/rss"
            )
            const val HTML_ARTICLE_SELECTOR = "div[data-testid=\"curation-grid-normal\"] li, div.bbc-13ipdvs li, div.promo-text, [data-testid=\"card-headline\"]"
            const val HTML_TITLE_SELECTOR = "h2, h3, a"
            const val HTML_LINK_SELECTOR = "a"
            const val HTML_IMAGE_SELECTOR = "img"
        }

        object SonDakika {
            const val ID = "son_dakika"
            const val DISPLAY_NAME = "Son Dakika"
            const val BASE_URL = "https://www.sondakika.com/"
            const val PRIMARY_RSS = "https://www.sondakika.com/rss/haberler/"
            val FALLBACK_RSS = listOf(
                "https://www.sondakika.com/rss/",
                "https://www.sondakika.com/feed/",
                "https://www.sondakika.com/rss/sondakika/"
            )
            const val HTML_ARTICLE_SELECTOR = "div.news-box, div.fl-news, div.bx-item, li.news-item, div.material, a.news"
            const val HTML_TITLE_SELECTOR = "span.title, h3, h2, .news-title"
            const val HTML_LINK_SELECTOR = "a"
            const val HTML_IMAGE_SELECTOR = "img"
        }
    }

    // 2) ELEKTRİK HABER KAYNAKLARI
    object Electrical {
        object EEPower {
            const val ID = "eepower"
            const val DISPLAY_NAME = "EE Power"
            const val BASE_URL = "https://eepower.com/"
            const val PRIMARY_RSS = "https://eepower.com/market-insights/rss"
            val FALLBACK_RSS = listOf("https://eepower.com/rss", "https://eepower.com/feed")
            const val HTML_ARTICLE_SELECTOR = "article, div.article-card, div.news-item, div.card"
            const val IS_ENGLISH = true
        }

        object AllAboutCircuits {
            const val ID = "allaboutcircuits"
            const val DISPLAY_NAME = "All About Circuits"
            const val BASE_URL = "https://www.allaboutcircuits.com/"
            const val PRIMARY_RSS = "https://www.allaboutcircuits.com/rss/news"
            val FALLBACK_RSS = listOf("https://www.allaboutcircuits.com/rss", "https://www.allaboutcircuits.com/feed")
            const val HTML_ARTICLE_SELECTOR = "article, div.news-card, div.feed-item"
            const val IS_ENGLISH = true
        }

        object ElektrikHaber {
            const val ID = "elektrikhaber"
            const val DISPLAY_NAME = "Elektrik Haber"
            const val BASE_URL = "https://www.elektrikhaber.com/"
            const val PRIMARY_RSS = "https://www.elektrikhaber.com/feed/"
            val FALLBACK_RSS = listOf("https://www.elektrikhaber.com/rss/", "https://www.elektrikhaber.com/feed/rss")
            const val HTML_ARTICLE_SELECTOR = "article, div.post-item, div.td_module_wrap"
            const val IS_ENGLISH = false
        }

        val CATEGORY_KEYWORDS_LOW_VOLTAGE = listOf(
            "network", "kamera", "alarm", "yangın algılama", "data", "fiber",
            "telekom", "low voltage", "zayıf akım", "ethernet", "cat6", "sensör", "iot"
        )

        val CATEGORY_KEYWORDS_HIGH_VOLTAGE = listOf(
            "transformatör", "trafo", "enerji", "şebeke", "güç sistemleri",
            "high voltage", "power", "motor", "pano", "kuvvetli akım", "og", "yg", "jeneratör", "inverter"
        )

        val CATEGORY_KEYWORDS_SECURITY = listOf(
            "güvenlik", "cctv", "erişim kontrol", "yangın", "security",
            "safety", "cybersecurity", "siber güvenlik", "geçiş kontrol", "bariyer"
        )
    }

    // 3) METEOROLOJİ GENEL MÜDÜRLÜĞÜ (MGM) HAVA DURUMU
    object Weather {
        // MGM Web Servisleri URL'leri
        const val MGM_BASE_SERVICE_URL = "https://servis.mgm.gov.tr/web"
        const val MGM_WEB_URL = "https://www.mgm.gov.tr"

        // Uç Noktalar (Endpoints)
        const val ENDPOINT_CENTERS = "/merkezler" // İl ve İlçe arama / listeleme
        const val ENDPOINT_CURRENT_STATE = "/sondurumlar" // Anlık durum ?merkezid={merkezId}
        const val ENDPOINT_DAILY_FORECAST = "/tahminler/gunluk" // Günlük tahmin ?istno={istNo}
        const val ENDPOINT_HOURLY_FORECAST = "/tahminler/saatlik" // Saatlik tahmin ?istno={istNo}

        // MGM Resmi Sitesi İstek Başlıkları
        val MGM_HEADERS = mapOf(
            "Origin" to "https://www.mgm.gov.tr",
            "Referer" to "https://www.mgm.gov.tr/",
            "Accept" to "application/json, text/plain, */*",
            "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        )

        // Varsayılan İl ve İstasyon: İzmir Merkez (Konak)
        const val DEFAULT_IL = "İzmir"
        const val DEFAULT_ILCE = "Merkez"
        const val DEFAULT_MERKEZ_ID = 93500
        const val DEFAULT_IST_NO = 17220

        // Jsoup Yedekleme Seçicileri (Web sayfası için)
        const val HTML_CURRENT_SELECTOR = "div.anlik-durum, div.sondurum, div.havadurumu-box"
        const val HTML_FORECAST_ROW_SELECTOR = "table.tahmin tr, div.tahmin-item"
    }

    // 4) FİYAT KARŞILAŞTIRMA (FİYAT ARA) KAYNAKLARI
    data class StoreConfig(
        val id: String,
        val displayName: String,
        val layer: Int, // 1: Karşılaştırma, 2: Mağaza siteleri, 3: Büyük pazar yerleri
        val searchUrlTemplate: String,
        val appPackageName: String? = null,
        val itemSelectors: List<String>,
        val titleSelectors: List<String>,
        val priceSelectors: List<String>,
        val linkSelectors: List<String>,
        val imageSelectors: List<String>,
        val storeNameSelectors: List<String> = emptyList() // Cimri / Akakçe toplayıcıları için mağaza adı
    )

    object PriceSearch {
        val EXCLUDED_ACC_KEYWORDS = listOf(
            "kılıf", "aksesuar", "adaptör", "adaptörü", "yedek parça",
            "montaj aparatı", "aparatı", "koruyucu cam", "şarj aleti", "kablo"
        )

        val STORES = listOf(
            // KATMAN 1: Karşılaştırma Siteleri (Öncelikli)
            StoreConfig(
                id = "cimri",
                displayName = "Cimri",
                layer = 1,
                searchUrlTemplate = "https://www.cimri.com/arama?q={query}",
                appPackageName = "com.cimri.android",
                itemSelectors = listOf(
                    "article.product-card",
                    "div[data-testid='product-card']",
                    "div.s1w975s0-0",
                    "div.ProductCard_productCard__",
                    "div.s1wl6n6c-0"
                ),
                titleSelectors = listOf("h3", "h2", ".title", "[data-testid='product-title']", "a.link-detail"),
                priceSelectors = listOf(".price", ".s137y373-1", "[data-testid='product-price']", "span.price-text"),
                linkSelectors = listOf("a.link-detail", "a[href*='/fiyatlari']", "a"),
                imageSelectors = listOf("img[src*='cimri']", "img", "source"),
                storeNameSelectors = listOf(".merchant", ".merchant-name", ".s11n1v04-0", "span.store")
            ),
            StoreConfig(
                id = "akakce",
                displayName = "Akakçe",
                layer = 1,
                searchUrlTemplate = "https://www.akakce.com/arama/?q={query}",
                appPackageName = "com.akakce.akakce",
                itemSelectors = listOf(
                    "li[data-pr]",
                    "ul#APL li",
                    "div.p-card",
                    "li.item"
                ),
                titleSelectors = listOf("h3", ".pn_v8", ".name", "span.name", "a.item-title"),
                priceSelectors = listOf(".pt_v8", "span.pt_v8", ".price", "span.price"),
                linkSelectors = listOf("a[href*='/fiyati/']", "a[data-pr]", "a"),
                imageSelectors = listOf("img.rw_v8", "img[src*='akakce']", "img"),
                storeNameSelectors = listOf("span.v_v8", "span.seller", "span.store")
            ),

            // KATMAN 2: Mağaza Siteleri (Orta Bot Riski)
            StoreConfig(
                id = "vatan",
                displayName = "Vatan Bilgisayar",
                layer = 2,
                searchUrlTemplate = "https://www.vatanbilgisayar.com/arama/{query}/",
                appPackageName = "com.vatan.android",
                itemSelectors = listOf(
                    "div.product-list__content",
                    "div.product-list--item",
                    "div.product-card"
                ),
                titleSelectors = listOf("div.product-list__product-name", "h3", "a.product-list__link"),
                priceSelectors = listOf("span.product-list__price", "div.product-list__price-box", "span.price"),
                linkSelectors = listOf("a.product-list__link", "a"),
                imageSelectors = listOf("img.product-list__image", "img")
            ),
            StoreConfig(
                id = "teknosa",
                displayName = "Teknosa",
                layer = 2,
                searchUrlTemplate = "https://www.teknosa.com/arama?s={query}",
                appPackageName = "com.teknosa.teknosaandroid",
                itemSelectors = listOf(
                    "div.prd",
                    "div.product-item",
                    "div.prd-inner"
                ),
                titleSelectors = listOf("a.prd-title", "h3.prd-title", ".prd-title"),
                priceSelectors = listOf("span.prd-prc2", "div.prd-prices", "span.price"),
                linkSelectors = listOf("a.prd-link", "a"),
                imageSelectors = listOf("img.prd-img", "img")
            ),
            StoreConfig(
                id = "itopya",
                displayName = "İtopya",
                layer = 2,
                searchUrlTemplate = "https://www.itopya.com/AramaSonuclari?q={query}",
                appPackageName = null,
                itemSelectors = listOf(
                    "div.product-item",
                    "div.product-card",
                    "div.col-md-3.product"
                ),
                titleSelectors = listOf("div.product-name a", "h3", ".product-name"),
                priceSelectors = listOf("div.price-new", "div.product-price", ".price"),
                linkSelectors = listOf("a.product-link", "div.product-name a", "a"),
                imageSelectors = listOf("img.product-img", "img")
            ),
            StoreConfig(
                id = "gurgencler",
                displayName = "Gürgençler",
                layer = 2,
                searchUrlTemplate = "https://www.gurgencler.com.tr/arama?q={query}",
                appPackageName = null,
                itemSelectors = listOf(
                    "div.product-item",
                    "div.showcase",
                    "div.product-card"
                ),
                titleSelectors = listOf("div.showcase-title a", "h3", ".title"),
                priceSelectors = listOf("div.showcase-price-new", "div.showcase-price", ".price"),
                linkSelectors = listOf("a.showcase-label", "div.showcase-title a", "a"),
                imageSelectors = listOf("img.showcase-image", "img")
            ),
            StoreConfig(
                id = "n11",
                displayName = "N11",
                layer = 2,
                searchUrlTemplate = "https://www.n11.com/arama?q={query}",
                appPackageName = "com.dmall.mfandroid",
                itemSelectors = listOf(
                    "li.column",
                    "div.pro",
                    "li.product-item"
                ),
                titleSelectors = listOf("h3.productName", ".productName", "a.plink"),
                priceSelectors = listOf("ins", "span.newPrice ins", ".priceContainer ins", ".newPrice"),
                linkSelectors = listOf("a.plink", "a"),
                imageSelectors = listOf("img.lazy", "img.cardImage", "img")
            ),
            StoreConfig(
                id = "pazarama",
                displayName = "Pazarama",
                layer = 2,
                searchUrlTemplate = "https://www.pazarama.com/arama?q={query}",
                appPackageName = "com.pazarama.mobile",
                itemSelectors = listOf(
                    "div.product-card",
                    "div.item-card",
                    "div[data-testid='product-card']"
                ),
                titleSelectors = listOf("span.product-name", "h3", "p.product-title"),
                priceSelectors = listOf("span.price", "div.price-box", "span.current-price"),
                linkSelectors = listOf("a", "a[href*='/p/']"),
                imageSelectors = listOf("img", "img[src*='pazarama']")
            ),

            // KATMAN 3: Büyük Pazar Yerleri (Yüksek Bot Riski / Destek)
            StoreConfig(
                id = "hepsiburada",
                displayName = "Hepsiburada",
                layer = 3,
                searchUrlTemplate = "https://www.hepsiburada.com/ara?q={query}",
                appPackageName = "com.pozitron.hepsiburada",
                itemSelectors = listOf(
                    "li[id^='i']",
                    "div[data-test-id='product-card-container']",
                    "li.productListContent-item",
                    "article"
                ),
                titleSelectors = listOf("h3[data-test-id='product-card-name']", "h3", ".product-title", "span.product-name"),
                priceSelectors = listOf("div[data-test-id='price-current-price']", "span.price", "div.price"),
                linkSelectors = listOf("a[href*='-p-']", "a"),
                imageSelectors = listOf("img[data-test-id='product-image']", "img")
            ),
            StoreConfig(
                id = "trendyol",
                displayName = "Trendyol",
                layer = 3,
                searchUrlTemplate = "https://www.trendyol.com/sr?q={query}",
                appPackageName = "com.trendyol.stage",
                itemSelectors = listOf(
                    "div.p-card-wrppr",
                    "div.product-card",
                    "div.p-card-chldrn"
                ),
                titleSelectors = listOf("span.prdct-desc-cntnr-name", "div.product-desc", "span.name"),
                priceSelectors = listOf("div.prc-box-dscntd", "span.prc-slg", "div.price"),
                linkSelectors = listOf("a[href*='/p-']", "a"),
                imageSelectors = listOf("img.p-card-img", "img")
            ),
            StoreConfig(
                id = "amazon",
                displayName = "Amazon TR",
                layer = 3,
                searchUrlTemplate = "https://www.amazon.com.tr/s?k={query}",
                appPackageName = "com.amazon.mShop.android.shopping",
                itemSelectors = listOf(
                    "div[data-component-type='s-search-result']",
                    "div.s-result-item"
                ),
                titleSelectors = listOf("h2 a span", "h2", "span.a-text-normal"),
                priceSelectors = listOf("span.a-price span.a-offscreen", "span.a-price-whole", "span.a-price"),
                linkSelectors = listOf("h2 a", "a.a-link-normal"),
                imageSelectors = listOf("img.s-image", "img")
            )
        )
    }
}
