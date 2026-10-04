package com.example.data.remote

import com.example.data.model.MgmLocation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object TurkeyProvinces {

    data class ProvinceData(
        val il: String,
        val merkezId: Int,
        val istNo: Int,
        val lat: Double,
        val lon: Double,
        val districts: List<String>
    )

    val PROVINCES = listOf(
        ProvinceData("Adana", 90100, 17351, 37.0000, 35.3213, listOf("Merkez", "Seyhan", "Yüreğir", "Çukurova", "Sarıçam", "Ceyhan", "Kozan")),
        ProvinceData("Adıyaman", 90200, 17260, 37.7648, 38.2786, listOf("Merkez", "Besni", "Kahta", "Gölbaşı", "Gerger")),
        ProvinceData("Afyonkarahisar", 90300, 17190, 38.7507, 30.5567, listOf("Merkez", "Sandıklı", "Dinar", "Bolvadin", "Emirdağ")),
        ProvinceData("Ağrı", 90400, 17099, 39.7191, 43.0503, listOf("Merkez", "Doğubayazıt", "Patnos", "Diyadin", "Eleşkirt")),
        ProvinceData("Amasya", 90500, 17085, 40.6500, 35.8333, listOf("Merkez", "Merzifon", "Suluova", "Taşova", "Gümüşhacıköy")),
        ProvinceData("Ankara", 90600, 17130, 39.9334, 32.8597, listOf("Merkez", "Çankaya", "Keçiören", "Yenimahalle", "Mamak", "Etimesgut", "Sincan", "Altındağ", "Gölbaşı", "Polatlı")),
        ProvinceData("Antalya", 90700, 17300, 36.8969, 30.7133, listOf("Merkez", "Muratpaşa", "Kepez", "Konyaaltı", "Alanya", "Manavgat", "Serik", "Kemer", "Kaş")),
        ProvinceData("Artvin", 90800, 17042, 41.1828, 41.8183, listOf("Merkez", "Hopa", "Borçka", "Yusufeli", "Arhavi", "Şavşat")),
        ProvinceData("Aydın", 90900, 17234, 37.8560, 27.8416, listOf("Merkez", "Efeler", "Nazilli", "Söke", "Kuşadası", "Didim", "İncirliova")),
        ProvinceData("Balıkesir", 91000, 17150, 39.6484, 27.8826, listOf("Merkez", "Karesi", "Altıeylül", "Bandırma", "Edremit", "Ayvalık", "Burhaniye", "Gönen")),
        ProvinceData("Bilecik", 91100, 17124, 40.1451, 29.9799, listOf("Merkez", "Bozüyük", "Osmaneli", "Söğüt", "Gölpazarı")),
        ProvinceData("Bingöl", 91200, 17203, 38.8854, 40.4983, listOf("Merkez", "Genç", "Karlıova", "Solhan", "Kiğı")),
        ProvinceData("Bitlis", 91300, 17205, 38.4006, 42.1095, listOf("Merkez", "Tatvan", "Ahlat", "Güroymak", "Adilcevaz", "Hizan")),
        ProvinceData("Bolu", 91400, 17070, 40.7350, 31.6061, listOf("Merkez", "Gerede", "Mudurnu", "Mengen", "Göynük")),
        ProvinceData("Burdur", 91500, 17246, 37.7203, 30.2908, listOf("Merkez", "Bucak", "Gölhisar", "Yeşilova", "Ağlasun")),
        ProvinceData("Bursa", 91600, 17116, 40.1885, 29.0610, listOf("Merkez", "Osmangazi", "Nilüfer", "Yıldırım", "İnegöl", "Gemlik", "Mustafakemalpaşa", "Mudanya")),
        ProvinceData("Çanakkale", 91700, 17112, 40.1553, 26.4142, listOf("Merkez", "Biga", "Çan", "Gelibolu", "Ayvacık", "Ezine", "Yenice")),
        ProvinceData("Çankırı", 91800, 17080, 40.6013, 33.6134, listOf("Merkez", "Çerkeş", "Ilgaz", "Orta", "Şabanözü")),
        ProvinceData("Çorum", 91900, 17084, 40.5506, 34.9556, listOf("Merkez", "Sungurlu", "Osmancık", "İskilip", "Alaca")),
        ProvinceData("Denizli", 92000, 17240, 37.7765, 29.0864, listOf("Merkez", "Pamukkale", "Merkezefendi", "Çivril", "Acıpayam", "Tavas", "Honaz")),
        ProvinceData("Diyarbakır", 92100, 17280, 37.9144, 40.2306, listOf("Merkez", "Bağlar", "Kayapınar", "Yenişehir", "Sur", "Bismil", "Ergani")),
        ProvinceData("Edirne", 92200, 17050, 41.6771, 26.5557, listOf("Merkez", "Keşan", "Uzunköprü", "İpsala", "Havsa")),
        ProvinceData("Elazığ", 92300, 17202, 38.6810, 39.2264, listOf("Merkez", "Kovancılar", "Karakoçan", "Palu", "Baskil")),
        ProvinceData("Erzincan", 92400, 17096, 39.7500, 39.5000, listOf("Merkez", "Tercan", "Üzümlü", "Refahiye", "Çayırlı")),
        ProvinceData("Erzurum", 92500, 17094, 39.9000, 41.2700, listOf("Merkez", "Yakutiye", "Palandöken", "Aziziye", "Horasan", "Oltu", "Pasinler")),
        ProvinceData("Eskişehir", 92600, 17126, 39.7767, 30.5206, listOf("Merkez", "Odunpazarı", "Tepebaşı", "Sivrihisar", "Çifteler", "Seyitgazi")),
        ProvinceData("Gaziantep", 92700, 17262, 37.0662, 37.3833, listOf("Merkez", "Şahinbey", "Şehitkamil", "Nizip", "İslahiye", "Nurdağı")),
        ProvinceData("Giresun", 92800, 17034, 40.9128, 38.3895, listOf("Merkez", "Bulancak", "Espiye", "Görele", "Tirebolu")),
        ProvinceData("Gümüşhane", 92900, 17090, 40.4600, 39.4700, listOf("Merkez", "Kelkit", "Şiran", "Kürtün", "Torul")),
        ProvinceData("Hakkari", 93000, 17290, 37.5833, 43.7333, listOf("Merkez", "Yüksekova", "Şemdinli", "Çukurca", "Derecik")),
        ProvinceData("Hatay", 93100, 17375, 36.4018, 36.3498, listOf("Merkez", "Antakya", "İskenderun", "Defne", "Dörtyol", "Samandağ", "Kırıkhan")),
        ProvinceData("Isparta", 93200, 17244, 37.7648, 30.5566, listOf("Merkez", "Yalvaç", "Eğirdir", "Şarkikaraağaç", "Gelendost")),
        ProvinceData("Mersin", 93300, 17352, 36.8000, 34.6333, listOf("Merkez", "Akdeniz", "Mezitli", "Toroslar", "Yenişehir", "Tarsus", "Erdemli", "Silifke")),
        ProvinceData("İstanbul", 93400, 17060, 41.0082, 28.9784, listOf("Merkez", "Kadıköy", "Beşiktaş", "Üsküdar", "Şişli", "Bakırköy", "Fatih", "Beylikdüzü", "Pendik", "Ümraniye")),
        ProvinceData("İzmir", 93500, 17220, 38.4192, 27.1287, listOf("Merkez", "Konak", "Karşıyaka", "Bornova", "Buca", "Çiğli", "Bayraklı", "Balçova", "Torbalı", "Menemen", "Gaziemir", "Urla", "Çeşme", "Aliağa", "Bergama")),
        ProvinceData("Kars", 93600, 17097, 40.6167, 43.1000, listOf("Merkez", "Kağızman", "Sarıkamış", "Selim", "Digor")),
        ProvinceData("Kastamonu", 93700, 17074, 41.3887, 33.7827, listOf("Merkez", "Tosya", "Taşköprü", "Cide", "İnebolu")),
        ProvinceData("Kayseri", 93800, 17196, 38.7312, 35.4787, listOf("Merkez", "Melikgazi", "Kocasinan", "Talas", "Develi", "Yahyalı")),
        ProvinceData("Kırklareli", 93900, 17056, 41.7333, 27.2167, listOf("Merkez", "Lüleburgaz", "Babaeski", "Vize", "Pınarhisar")),
        ProvinceData("Kırşehir", 94000, 17160, 39.1425, 34.1709, listOf("Merkez", "Kaman", "Mucur", "Çiçekdağı")),
        ProvinceData("Kocaeli", 94100, 17066, 40.8533, 29.8815, listOf("Merkez", "İzmit", "Gebze", "Darıca", "Körfez", "Gölcük", "Çayırova", "Kartepe")),
        ProvinceData("Konya", 94200, 17244, 37.8667, 32.4833, listOf("Merkez", "Selçuklu", "Meram", "Karatay", "Ereğli", "Akşehir", "Beyşehir", "Cihanbeyli")),
        ProvinceData("Kütahya", 94300, 17155, 39.4167, 29.9833, listOf("Merkez", "Tavşanlı", "Simav", "Gediz", "Emet")),
        ProvinceData("Malatya", 94400, 17200, 38.3552, 38.3095, listOf("Merkez", "Battalgazi", "Yeşilyurt", "Doğanşehir", "Akçadağ", "Darende")),
        ProvinceData("Manisa", 94500, 17180, 38.6191, 27.4289, listOf("Merkez", "Yunusemre", "Şehzadeler", "Akhisar", "Turgutlu", "Salihli", "Soma", "Alaşehir")),
        ProvinceData("Kahramanmaraş", 94600, 17255, 37.5858, 36.9371, listOf("Merkez", "Onikişubat", "Dulkadiroğlu", "Elbistan", "Afşin", "Göksun", "Pazarcık")),
        ProvinceData("Mardin", 94700, 17285, 37.3212, 40.7245, listOf("Merkez", "Artuklu", "Kızıltepe", "Midyat", "Nusaybin", "Derik")),
        ProvinceData("Muğla", 94800, 17292, 37.2153, 28.3636, listOf("Merkez", "Menteşe", "Bodrum", "Fethiye", "Milas", "Marmaris", "Ortaca", "Dalaman")),
        ProvinceData("Muş", 94900, 17204, 38.7432, 41.5064, listOf("Merkez", "Bulanık", "Malazgirt", "Varto", "Hasköy")),
        ProvinceData("Nevşehir", 95000, 17195, 38.6244, 34.7144, listOf("Merkez", "Ürgüp", "Avanos", "Gülşehir", "Derinkuyu")),
        ProvinceData("Niğde", 95100, 17250, 37.9667, 34.6833, listOf("Merkez", "Bor", "Çiftlik", "Ulukışla", "Altunhisar")),
        ProvinceData("Ordu", 95200, 17033, 40.9839, 37.8764, listOf("Merkez", "Altınordu", "Ünye", "Fatsa", "Gölköy", "Korgan")),
        ProvinceData("Rize", 95300, 17040, 41.0201, 40.5234, listOf("Merkez", "Çayeli", "Ardeşen", "Pazar", "Fındıklı")),
        ProvinceData("Sakarya", 95400, 17069, 40.7569, 30.3783, listOf("Merkez", "Adapazarı", "Serdivan", "Akyazı", "Erenler", "Hendek", "Karasu")),
        ProvinceData("Samsun", 95500, 17030, 41.2928, 36.3313, listOf("Merkez", "İlkadım", "Atakum", "Bafra", "Çarşamba", "Canik", "Vezirköprü")),
        ProvinceData("Siirt", 95600, 17282, 37.9333, 41.9500, listOf("Merkez", "Kurtalan", "Pervari", "Baykan", "Şirvan")),
        ProvinceData("Sinop", 95700, 17026, 42.0231, 35.1531, listOf("Merkez", "Boyabat", "Gerze", "Ayancık", "Durağan")),
        ProvinceData("Sivas", 95800, 17092, 39.7477, 37.0179, listOf("Merkez", "Şarkışla", "Yıldızeli", "Suşehri", "Zara", "Gemerek")),
        ProvinceData("Tekirdağ", 95900, 17059, 40.9833, 27.5167, listOf("Merkez", "Süleymanpaşa", "Çorlu", "Çerkezköy", "Kapaklı", "Ergene")),
        ProvinceData("Tokat", 96000, 17088, 40.3167, 36.5500, listOf("Merkez", "Erbaa", "Turhal", "Niksar", "Zile", "Reşadiye")),
        ProvinceData("Trabzon", 96100, 17038, 41.0027, 39.7168, listOf("Merkez", "Ortahisar", "Akçaabat", "Araklı", "Of", "Yomra", "Arsin")),
        ProvinceData("Tunceli", 96200, 17201, 39.1079, 39.5401, listOf("Merkez", "Pertek", "Mazgirt", "Çemişgezek", "Hozat")),
        ProvinceData("Şanlıurfa", 96300, 17270, 37.1591, 38.7969, listOf("Merkez", "Eyyübiye", "Haliliye", "Siverek", "Viranşehir", "Karaköprü")),
        ProvinceData("Uşak", 96400, 17188, 38.6823, 29.4082, listOf("Merkez", "Banaz", "Eşme", "Sivaslı", "Ulubey")),
        ProvinceData("Van", 96500, 17170, 38.4891, 43.4089, listOf("Merkez", "İpekyolu", "Tuşba", "Erciş", "Edremit", "Özalp", "Çaldıran")),
        ProvinceData("Yozgat", 96600, 17140, 39.8181, 34.8147, listOf("Merkez", "Sorgun", "Akdağmadeni", "Yerköy", "Boğazlıyan")),
        ProvinceData("Zonguldak", 96700, 17022, 41.4564, 31.7987, listOf("Merkez", "Ereğli", "Çaycuma", "Devrek", "Kozlu", "Kilimli")),
        ProvinceData("Aksaray", 96800, 17192, 38.3687, 34.0370, listOf("Merkez", "Ortaköy", "Eskil", "Gülağaç", "Güzelyurt")),
        ProvinceData("Bayburt", 96900, 17091, 40.2552, 40.2249, listOf("Merkez", "Demirözü", "Aydıntepe")),
        ProvinceData("Karaman", 97000, 17248, 37.1759, 33.2287, listOf("Merkez", "Ermenek", "Sarıveliler", "Ayrancı")),
        ProvinceData("Kırıkkale", 97100, 17135, 39.8468, 33.5153, listOf("Merkez", "Yahşihan", "Keskin", "Delice")),
        ProvinceData("Batman", 97200, 17281, 37.8812, 41.1294, listOf("Merkez", "Kozluk", "Sason", "Beşiri", "Gercüş")),
        ProvinceData("Şırnak", 97300, 17288, 37.5164, 42.4611, listOf("Merkez", "Cizre", "Silopi", "İdil", "Uludere", "Beytüşşebap")),
        ProvinceData("Bartın", 97400, 17020, 41.6344, 32.3375, listOf("Merkez", "Ulus", "Amasra", "Kurucaşile")),
        ProvinceData("Ardahan", 97500, 17098, 41.1105, 42.7022, listOf("Merkez", "Göle", "Çıldır", "Hanak", "Posof")),
        ProvinceData("Iğdır", 97600, 17099, 39.9196, 44.0454, listOf("Merkez", "Tuzluca", "Aralık", "Karakoyunlu")),
        ProvinceData("Yalova", 97700, 17114, 40.6500, 29.2667, listOf("Merkez", "Çiftlikköy", "Çınarcık", "Altınova", "Armutlu")),
        ProvinceData("Karabük", 97800, 17072, 41.2061, 32.6204, listOf("Merkez", "Safranbolu", "Yenice", "Eskipazar")),
        ProvinceData("Kilis", 97900, 17264, 36.7184, 37.1212, listOf("Merkez", "Musabeyli", "Elbeyli", "Polateli")),
        ProvinceData("Osmaniye", 98000, 17355, 37.0742, 36.2472, listOf("Merkez", "Kadirli", "Düziçi", "Bahçe", "Toprakkale")),
        ProvinceData("Düzce", 98100, 17071, 40.8438, 31.1565, listOf("Merkez", "Akçakoca", "Kaynaşlı", "Gölyaka", "Çilimli", "Yığılca"))
    )

    fun findNearestProvince(lat: Double, lon: Double): MgmLocation {
        var minDistance = Double.MAX_VALUE
        var bestProvince = PROVINCES.first { it.il == "İzmir" }

        for (p in PROVINCES) {
            val dist = calculateDistance(lat, lon, p.lat, p.lon)
            if (dist < minDistance) {
                minDistance = dist
                bestProvince = p
            }
        }

        return MgmLocation(
            il = bestProvince.il,
            ilce = "Merkez",
            merkezId = bestProvince.merkezId,
            istNo = bestProvince.istNo,
            latitude = bestProvince.lat,
            longitude = bestProvince.lon
        )
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Dünya yarıçapı km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
