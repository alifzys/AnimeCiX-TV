# Project_Goals.md — AnimeCiX TV

## Vizyon
Android TV ve dpad-tabanlı cihazlar için, animecix.tv içeriğini akıcı şekilde keşfettiren ve Tau Video kaynakları üzerinden oynatan; Anime4K görüntü iyileştirmeli, açık kaynak (MIT) bir anime izleme istemcisi.

## Tespit Edilen Teknik Yığın
- **Dil:** Kotlin (JVM 17)
- **UI:** Jetpack Compose for TV (`tv-material 1.0.0`, `tv-foundation 1.0.0-alpha11`)
- **Oynatıcı:** Media3 / ExoPlayer 1.5.1 (+ media3-effect → Anime4K/VideoEnhance)
- **Ağ:** OkHttp 4.12 + kotlinx.serialization 1.7.3
- **Yerel veri:** Room 2.6.1 (KSP)
- **Görsel:** Coil 2.7 (+ svg)
- **Build:** Gradle (Kotlin DSL), compileSdk 35 / minSdk 23 / targetSdk 34
- **Paket:** `com.alifzys.an1mecix` — versionName 1.1.2 (versionCode 5)

## Mevcut Durum
- **Mimari:** MVVM + temiz katmanlama (ui → domain ← data), tek modül, tek-yönlü import. Circular dependency yok.
- **Tamamlanma Tahmini:** **Stabil/beta** — yayınlanmış (GitHub: alifzys/AnimeCiX-TV), çalışan tüm ana ekranlar mevcut (home, detail, player, search, saved, settings, categories), güncelleme (UpdateService) ve indirme (DownloadManager) altyapısı var.
- **Teknik Borç Puanı:** **4/10** — mimari sağlam; borç noktaları belirli (aşağıda).

## Kısa Vadeli Hedefler (Refaktör Sonrası)
- [ ] `PlayerScreen.kt` (998 satır) → 800 altına böl: sheet'ler + progress + overlay alt-Composable'lara.
- [ ] `AnimeRepository.kt` (413) → ağ orkestrasyonu ve `mapper`/`parser` (DTO dönüşümü, fansub/comment parse) ayrımı.
- [ ] Hardcoded URL'leri (`animecix.tv`, `tau-video.xyz`, GitHub API) tek bir `core/Constants` objesinde topla.
- [ ] İlk birim testleri: `mapper`, fansub parse ve `Xeh` (X-E-H algoritması) için davranış güvenlik ağı.
- [ ] `DetailScreen.kt` (600) izleme listesi → büyürse böl.

## Teknik Kısıtlar
- **Hedef cihaz:** Genelde zayıf donanımlı Android TV (32-bit ARM `armeabi-v7a` + `arm64-v8a`). Performans testi **release build** ile yapılmalı (debug zayıf TV'de kasar).
- **Etkileşim:** Yalnızca dpad/uzaktan kumanda — her etkileşimli öğe focusable olmalı, focus pattern korunmalı.
- **Dağıtım:** Sideload; release APK debug key ile imzalanır. ABI başına ayrı + universal APK.
- **Tek geliştirici:** Aşırı karmaşık mimariden kaçın; tek modül yapısı korunsun.
- **Saf Kotlin/Compose** — GDExtension/native modül yok (tek native: androidx.graphics.path).

## AI Agent'ların Bilmesi Gerekenler (proje-spesifik tuzaklar)
- **Bağımsız bir API yok:** Uygulama animecix.tv ve tau-video.xyz uçlarını doğrudan tüketir. **Site HTML/JSON yapısı veya X-E-H (`Xeh.kt`) algoritması değişirse istemci kırılır** — bu kısımlar kırılgan, dikkatli dokun.
- **Fansub alanı `extra`'da:** Video objesinde fansub adı `extra` alanında; `name` ise host'tur (Tau Video). Tau kaynaklar ≈ fansublar. Parse mantığı `AnimeRepository`'de (`cleanFansub`, `fansubFromRoles`) — mapper'a taşınacak.
- **Oynatma akışı:** animecix → video listesi (`tau-video.xyz/api/video/`) → ExoPlayer. Kalite/hız/fansub seçimi `PlayerScreen` sheet'lerinde.
- **VideoEnhance/Anime4K:** media3-effect tabanlı; GPU'ya duyarlı, zayıf TV'de devre dışı bırakılabilmeli.
- **Diğer klasörler arşivlendi:** Eski prototipler (`tvapp/`, `tvapp-native/`, `anime-tv-app/`, `backend/`) ve dağınık dump/not/görseller `_archive/` altına taşındı (2026-06-23). Aktif kod yalnızca `sourcecode/`'dadır; depo kökü artık `_archive/` + `sourcecode/`. X-E-H/endpoint referansı için `_archive/api-dumps/`.
