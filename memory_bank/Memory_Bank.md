# 🧠 Memory Bank — AnimeCiX TV

**Son Güncelleme:** 2026-06-23 — ✅ Refaktör planının tamamı bitti + 3 bug fix doğrulandı
**Güncelleyen:** AI oturumu

---

## 📍 Şu Anki Durum
- **Aktif Proje:** `sourcecode/` (yayınlanan uygulama, `com.alifzys.an1mecix` v1.1.2)
- **Son Tamamlanan:** ✅ **Tüm oturum işi commit + push edildi** (2026-06-24). 3 mantıksal commit `origin/main`'e gitti: `fe7f7a4` indirme+izleme listesi, `6a915d6` refaktör+bug fix+testler, `e98e488` docs/CI. origin/main artık `e98e488` (v1.1.2). CI bu push ile çalışmaya başlar, badge yeşillenir. Öncesi: refaktörün 3 sırası da bitti + 3 bug fix gerçek TV'de doğrulandı.
- **Şu An Üzerinde Çalışılan:** — (planlı iş kalmadı). İsteğe bağlı izleme: DetailScreen/SearchScreen 800 altında ama büyük; "aşağı kaydırma kasması" konusu kullanıcıya göre animasyon yavaşlatılınca düzeldi.
- **Blokörler:** Yok. `Xeh` saf JVM'de test edilemez → ileride istenirse Robolectric/enstrümante test.

---

## 🗺️ Mimari Harita

Katmanlama tek-yönlü: **ui → domain ← data**. Circular dependency yok.

| Modül | Sorumluluk | Bağımlıları |
|-------|-----------|-------------|
| `ui/**` | Compose ekranları, ViewModel'ler, navigation, tema | `domain.model` |
| `domain/model` | Saf modeller (`Models.kt`), framework'süz | (bağımsız) |
| `data/api` | `AnimeCixService`, `TauVideoService`, `ApiDto`, `HttpClient`, `Xeh` (X-E-H algoritması) | `domain.model` |
| `data/local` | Room: `AppDatabase`, `Daos`, `entities` | — |
| `data/repository` | `AnimeRepository`, `UserDataRepository` (orkestrasyon + şu an parse mantığı) | `data.api`, `data.local`, `domain.model` |
| `data/download` · `data/update` | `DownloadManager`, `UpdateService` (GitHub release) | `data.api` |

### Kritik Dosyalar
| Dosya | Ne işe yarıyor | Dikkat |
|-------|----------------|--------|
| `data/api/Xeh.kt` | X-E-H imza/algoritması (animecix erişimi) | **Kırılgan** — site değişirse uygulama kırılır |
| `data/repository/AnimeRepository.kt` | Ağ orkestrasyon (yalnızca) | 172 satır; mapping/parse → `repository/mapper/` (Sıra 2) |
| `data/repository/mapper/` | DTO→model dönüşümü + fansub/JSON parse (`internal`, test edilir) | `AnimeMapper`/`FansubParser`/`JsonParse` |
| `ui/player/PlayerScreen.kt` | Oynatıcı ekran iskeleti + ExoPlayer + state | 376 satır; kontroller/sheet/skip → ayrı dosyalar (Sıra 3) |
| `ui/player/PlayerViewModel.kt` | Oynatma durumu/ExoPlayer yönetimi | Player state'in tek kaynağı |
| `ui/player/VideoEnhance.kt` | Anime4K/media3-effect görüntü iyileştirme | GPU'ya duyarlı, zayıf TV'de devre dışı bırakılabilmeli |
| `data/api/TauVideoService.kt` | Video kaynak listesi (`tau-video.xyz`) | Fansub adı `extra` alanında, `name` = host |

### Veri Akışı
Kullanıcı bir başlık seçer → `HomeViewModel`/`DetailViewModel` → `AnimeRepository` → `AnimeCixService` (animecix.tv, `Xeh` imzalı) ham DTO döner. Repository DTO'yu `domain.model`'e çevirir (fansub/comment parse dahil). Oynatma için `TauVideoService` (tau-video.xyz) video kaynaklarını verir; `PlayerViewModel` ExoPlayer'ı besler, `PlayerScreen` kalite/hız/fansub sheet'lerini çizer. İzleme geçmişi/kayıtlar Room (`UserDataRepository`) üzerinden tutulur.

---

## 🔧 Son Değişiklikler
### 2026-06-23 — GitHub repo iyileştirmeleri
- **CI:** `.github/workflows/build.yml` — push/PR'da JDK17/ubuntu `assembleRelease`, başarılıysa APK'lar artifact. README badge satırına Build badge eklendi.
- **Issue şablonları:** `.github/ISSUE_TEMPLATE/bug_report.md` (TV modeli/Android/uygulama sürümü/beklenen-gerçekleşen/logcat) + `feature_request.md` (TR).
- **CHANGELOG.md:** Keep a Changelog formatı. `[Unreleased]` = bu oturumun refaktör+bug fix işleri; `[1.1.1] - 2026-06-04` = ilk public sürüm (git geçmişinden çıkarıldı, tag v1.1.1 auto-update'i içeriyor).
- **README:** Anime4K tek-satır bullet'ı kaldırıldı → ayrı "🪄 Görüntü İyileştirme" bölümü (Anime4K nedir + Kapalı/Keskinlik/Anime4K modları + performans notu).
- **GitHub Topics:** kod değil, kullanıcı UI'dan ekleyecek. Önerilen: `android-tv`, `kotlin`, `anime`, `jetpack-compose`, `exoplayer`, `media3`, `google-tv`, `compose-for-tv`, `anime4k`.
- ✅ **2026-06-24: Hepsi commit + push edildi** — origin/main `9d52059` → `e98e488`. 3 commit: `fe7f7a4` (indirme+izleme listesi feature; meğer hiç commit'lenmemiş ama CHANGELOG'da 1.1.1 özelliği olarak yazılıydı), `6a915d6` (refaktör+bug fix+test), `e98e488` (docs/CI). _archive/ git deposunun (sourcecode/) DIŞINDA olduğu için push'a dahil değil — doğru.

### 2026-06-23 — Depo temizliği (dead code taraması + arşivleme)
- **Dead code taraması:** `sourcecode/` temiz çıktı — 0 dead top-level/private sembol, 0 TODO/FIXME, yorumlanmış kod bloğu yok, orphan dosya yok. **Silinen bir şey olmadı.**
- **Arşivleme:** Depo kökündeki aktif olmayan her şey `_archive/`'e taşındı (silinmedi). Kök artık yalnızca `_archive/` + `sourcecode/`.
  - `_archive/old-iterations/` ← tvapp, tvapp-native, anime-tv-app, backend
  - `_archive/api-dumps/` ← js-dumps, tau-dump, stream-curls.txt (X-E-H/endpoint referansı, kullanıcı isteğiyle ayrı klasör)
  - `_archive/notes/` ← technopat*/techopat/technoduzgun .txt
  - `_archive/assets-screenshots/` ← kök 18 png + references/ + screenshots/ + logobebegim/
- `_archive/README.md`: her öğe için "ne / neden / geri alınırsa ne lazım" satırı yazıldı.

### 2026-06-23 — Kullanıcı testinden 3 bug fix (✅ gerçek TV'de DOĞRULANDI)
Sıra 3 manuel testi sırasında bulunan 3 sorun düzeltildi, release APK ile kuruldu, kullanıcı onayladı:

- **Ana menü animasyon kasması:** `AnimeCard.PosterCard` — 60ms "settled" gecikme kapısı kaldırıldı; scale/ring/text doğrudan `focused`'a bağlandı. Süre iterasyonla 200→180→**300ms** (FastOutSlowInEasing); kullanıcı "çok daha iyi" dedi. (kullanılmayan `LaunchedEffect`/`delay` importları silindi). "Aşağı kaydırma kasması" da bu yavaşlatmayla giderildi.
- **Kalite değişiminde pozisyon kaybı:** `PlayerScreen.PlayerContent` — `LaunchedEffect(currentQuality.url)` artık `preparedEpisodeId` izliyor; **aynı bölümde kalite değişince** `exoPlayer.currentPosition` korunuyor (setMediaItem'dan önce yakala → prepare sonrası seek). Bölüm değişiminde eskisi gibi `resumeAt`. (Not: bu bug refaktörden önce de vardı, davranış birebir korunmuştu.)
- **Arama geçmişi her karakteri kaydediyordu:** `SearchViewModel` — `query()` artık geçmişe yazmaz (sadece canlı sonuç). Yeni `recordSearch(q)` yalnızca (a) arama sonucu açılınca, (b) "ARA" tuşunda çağrılır → sadece tam sorgu ("bleach") kaydedilir, önekler değil. Eski birikmiş önek kayıtları prefs'te kalır (× ile silinir).
- **Ekstra:** `HomeScreen.HomeContent` — `BackHandler(enabled = scrolledDown)` eklendi: aşağıdayken BACK → en üste kaydır + hero "İncele" focus; en üstteyken sistem çıkışı (önceki "ortadaki satıra atlama" davranışı düzeldi).

### 2026-06-23 — Sıra 3: PlayerScreen bölme
- **Oluşturulan (`ui/player/`, aynı paket — import churn'ü yok):** `PlayerControls.kt` (352 — PlayerOverlay + ProgressBar/SmallPlayBtn/CtrlPill/TimeText + fmtTime/numStr), `PlayerSheets.kt` (257 — QualitySheet/SpeedSheet/FansubSheet + SPEEDS + fullFansubLabel), `PlayerSkip.kt` (108 — SkipPrompt + SKIP_COUNTDOWN).
- **Değişen:** `PlayerScreen.kt` **998→376 satır** (PlayerScreen + PlayerContent + CenterText + shortFansubLabel + sabitler kaldı). 5 cross-file composable `private`→`internal`.
- **Sonuç:** BUILD SUCCESSFUL + 31/31 test yeşil. **Hiçbir dosya 800'ü aşmıyor** (800 satır ihlali çözüldü).
- **Neden:** AI_Guidelines 800 satır + tek sorumluluk kuralı.
- **Dikkat:** UI/TV focus mantığı birebir korundu (kod taşındı, değiştirilmedi) ama **JVM testiyle doğrulanamaz**. ⏳ Kullanıcının gerçek TV'de oynatma testi bekleniyor: overlay göster/gizle, kalite/hız/fansub sheet aç-seç-kapa, opening/ending skip geri sayımı, prev/next bölüm, BACK davranışı.

### 2026-06-23 — Sıra 2: AnimeRepository mapper/parser ayrımı
- **Oluşturulan (`data/repository/mapper/`):** `JsonParse.kt` (60 — asDouble/asCount/asReleaseDate/asNonEmptyString), `FansubParser.kt` (61 — cleanFansub/fansubFromRoles), `AnimeMapper.kt` (161 — toCard/toFeatured/toLastEpisode/toSeasonInfo/toSource/extractCast/groupEpisodes/toCommentOrNull). Tümü paket-içi `internal`.
- **Değişen:** `AnimeRepository.kt` 413→**172 satır** (yalnızca orkestrasyon kaldı; parse/mapping fonksiyonları taşındı, import edildi).
- **Eklenen testler:** `mapper/AnimeMapperTest` (6), `FansubParserTest` (8), `JsonParseTest` (6) → toplam suite 11→**31 test, hepsi GEÇTİ**.
- **Neden:** Tek sorumluluk (AI_Guidelines); mapping/parse artık test edilebilir ve izole.
- **Dikkat:** Fonksiyonlar `private`→`internal` oldu (paket dışına public değil). Davranış birebir korundu (BUILD SUCCESSFUL + 31 test yeşil).

### 2026-06-23 — Test iskelesi
- **Oluşturulan:** `app/src/test/java/.../data/api/ApiDtoTest.kt` (5 test — DTO serialization sözleşmesi, fansub=`extra`/host=`name` invariant'ı dahil), `data/update/UpdateServiceTest.kt` (6 test — `isNewer` sürüm karşılaştırma)
- **Değişen:** `app/build.gradle.kts` (junit 4.13.2 + kotlinx-coroutines-test testImplementation + `testOptions.unitTests.isReturnDefaultValues=true`), `UpdateService.isNewer` → `internal` + `@VisibleForTesting`
- **Sonuç:** `testDebugUnitTest` → 11/11 GEÇTİ (0 failure/error)
- **Neden:** Sıra 2 öncesi giriş sözleşmesi + sürüm mantığı için güvenlik ağı
- **Dikkat:** `Xeh` saf JVM'de test edilemiyor (Android Base64 + random IV); Robolectric/enstrümante teste bırakıldı. Mapper/fansub testleri Sıra 2'de fonksiyonlar `internal`'a taşınınca eklenecek.

### 2026-06-23 — Sıra 1: core/Constants
- **Oluşturulan:** `core/Constants.kt` (ANIMECIX_BASE, TAU_HOST, TAU_BASE, TAU_API, GITHUB_RELEASES_API)
- **Değişen:** `HttpClient.kt` (eski `ANIMECIX_BASE` const kaldırıldı), `AnimeCixService.kt`, `TauVideoService.kt`, `UpdateService.kt` (eski `API` const kaldırıldı), `AnimeRepository.kt`, `DetailScreen.kt`, `DetailViewModel.kt`, `PlayerViewModel.kt`
- **Ne yapıldı:** 5 hardcoded URL + `"tau-video.xyz"` host eşleştiricisi (6 kullanım) tek noktaya toplandı
- **Neden:** AI_Guidelines "hardcoded değer config'e taşınır" kuralı; uç adresi değişince tek dosya güncellenir
- **Dikkat:** String'ler birebir aynı → davranış değişmedi. `compileDebugKotlin` BUILD SUCCESSFUL. (TauVideoService'teki `embedRe` regex pattern'i kasten yerinde bırakıldı — config değil parse deseni.)

### 2026-06-23 — İlk kurulum
- **Oluşturulan:** `AI_Guidelines.md`, `Project_Goals.md`, `memory_bank/Memory_Bank.md`
- **Ne yapıldı:** Otomatik analiz + Vibe Coding altyapısı + refaktör planı

---

## ⚠️ Teknik Borç Tablosu
| Öncelik | Sorun | Dosya | Not |
|---------|-------|-------|-----|
| ORTA | Test kapsamı dar: DTO sözleşmesi + sürüm mantığı var; mapper/fansub/`Xeh` henüz yok | `app/src/test` | mapper testleri Sıra 2'de; `Xeh` Robolectric ister |
| ~~YÜKSEK~~ ✅ | ~~800 satır ihlali, 11 Composable iç içe~~ ÇÖZÜLDÜ (Sıra 3) | `PlayerScreen.kt` (376) + Controls/Sheets/Skip | ✅ gerçek TV'de manuel test edildi, sorunsuz |
| ~~ORTA~~ ✅ | ~~4 sorumluluk iç içe~~ ÇÖZÜLDÜ (Sıra 2) | `AnimeRepository.kt` (172) + `mapper/` | mapper/parser ayrıldı, 20 test eklendi |
| ~~ORTA~~ ✅ | ~~Hardcoded URL'ler 3 dosyaya dağılmış~~ ÇÖZÜLDÜ (Sıra 1) | `core/Constants.kt` | Tek noktada toplandı |
| ORTA | 600 satır izleme listesinde | `ui/detail/DetailScreen.kt` | Büyürse böl |
| DÜŞÜK | Site/HTML yapısına bağımlılık (kırılgan) | `data/api/Xeh.kt`, servisler | Dış kaynak değişimine karşı kapsülle |
| DÜŞÜK | `search` UI ağır | `ui/search/SearchScreen.kt` (455) | İzle |

---

## 🧩 Mimari Kararlar (ADR)

### ADR-001: Jetpack Compose for TV + MVVM
- **Karar:** UI tamamen Compose for TV; ekran başına `Screen` + `ViewModel`.
- **Sebep:** Dpad/focus yönetimi için TV-material; tek geliştirici için XML'siz sade yapı.
- **Sonuç:** TV focus pattern'ine uyulmalı; her etkileşimli öğe focusable olmalı.

### ADR-002: Media3/ExoPlayer + media3-effect (Anime4K)
- **Karar:** Oynatma Media3 1.5.1; görüntü iyileştirme media3-effect (`VideoEnhance`).
- **Sebep:** OkHttp datasource entegrasyonu + GPU shader efekt zinciri.
- **Sonuç:** Efektler GPU'ya duyarlı; zayıf TV'de kapatılabilir olmalı.

### ADR-003: Room + OkHttp + kotlinx.serialization (Retrofit yok)
- **Karar:** Ağ doğrudan OkHttp + manuel serialization; yerel kalıcılık Room (KSP).
- **Sebep:** animecix/tau uçları standart REST değil; `Xeh` imzası ve esnek JSON parse için elle kontrol gerekiyor.
- **Sonuç:** DTO parse mantığı elle yazılır → mapper katmanı kritik, test edilmeli.

### ADR-004: Tek modül, ABI-split APK, debug-key imza
- **Karar:** Tek `app` modülü; armeabi-v7a + arm64-v8a split + universal; release debug key ile imzalı.
- **Sebep:** Sideload dağıtımı, tek geliştirici, zayıf 32-bit TV hedefi.
- **Sonuç:** Performans testi **release build** ile yapılmalı.

---

## 🔨 Refaktör Planı (Dependency-Safe)

> Protokol her adımda: (1) dosyayı oku → (2) import bağımlılarını listele → (3) yeni dosyayı oluştur (eskiyi silme) → (4) bağımlıları yönlendir → (5) çalıştır/test et → (6) eski dosyayı sil → (7) bu dosyayı güncelle.

### ✅ Sıra 1 — `core/Constants` (TAMAMLANDI 2026-06-23) — Risk: DÜŞÜK
- **Sorun:** 5 hardcoded URL, 3 dosyaya dağılmış.
- **Bölünme:** `core/Constants.kt` → `ANIMECIX_BASE`, `TAU_BASE`, `TAU_API`, `TAU_EMBED`, `GITHUB_RELEASES`.
- **Kırılma riski:** Düşük; sadece string referansları taşınır.
- **Önce yapılması gereken:** Yok — ilk adım bu olabilir.
- **Test stratejisi:** Build + her ekrana manuel dpad turu.

### ✅ Sıra 2 — `AnimeRepository` mapper/parser ayrımı (TAMAMLANDI 2026-06-23) — Risk: ORTA
- **Mevcut sorumluluklar:** (1) ağ orkestrasyon (`home`, `detail`, `search`...), (2) DTO→model mapping (`toCard`, `toFeatured`...), (3) fansub parse (`cleanFansub`, `fansubFromRoles`, `groupEpisodes`), (4) comment/JSON parse (`asDouble`, `asReleaseDate`...).
- **Önerilen bölünme:**
  - `data/repository/AnimeRepository.kt` → yalnızca orkestrasyon (suspend fun'lar).
  - `data/repository/mapper/AnimeMapper.kt` → DTO→model dönüşümleri.
  - `data/repository/mapper/FansubParser.kt` → fansub/episode gruplama.
  - `data/repository/mapper/JsonParse.kt` → `JsonElement` extension'ları + comment parse.
- **Kırılma riski:** `AnimeRepository` çağıranları (HomeVM, DetailVM, SearchVM) — imza değişmezse etkilenmez; sadece private fonksiyonlar taşınır.
- **Önce yapılması gereken:** Bu fonksiyonlar için birim testi (saf, framework'süz → kolay).
- **Test stratejisi:** `app/src/test` birim testleri + home/detail/search manuel doğrulama.

### ✅ Sıra 3 — `PlayerScreen` bölme (TAMAMLANDI 2026-06-23, gerçek TV'de manuel test EDİLDİ ✓) — Risk: YÜKSEK
- **Mevcut sorumluluklar:** PlayerScreen + PlayerContent + PlayerOverlay + TimeText/SmallPlayBtn/CtrlPill/ProgressBar + QualitySheet + SpeedSheet + FansubSheet + SkipPrompt (998 satır, 11 Composable).
- **Önerilen bölünme:**
  - `ui/player/PlayerScreen.kt` → ana ekran + `PlayerContent` (≈ <400 satır).
  - `ui/player/components/PlayerControls.kt` → `PlayerOverlay`, `TimeText`, `SmallPlayBtn`, `CtrlPill`, `ProgressBar`.
  - `ui/player/components/PlayerSheets.kt` → `QualitySheet`, `SpeedSheet`, `FansubSheet` (+ `shortFansubLabel`/`fullFansubLabel`).
  - `ui/player/components/SkipPrompt.kt` → `SkipPrompt`.
- **Kırılma riski:** TV focus akışı ve sheet açılış/kapanış state'i — taşırken `remember`/state-hoisting bozulmamalı. Player en kritik ekran.
- **Önce yapılması gereken:** Sıra 1 + 2 bitsin; player değişmeden önce davranış elle kayıt altına alınsın.
- **Test stratejisi:** Release build + gerçek TV/emülatörde tam oynatma + kalite/hız/fansub/skip dpad turu.

### Refaktör Sırası (özet) — ✅ HEPSİ TAMAMLANDI
1. ✅ `core/Constants` (Sıra 1)
2. ✅ `AnimeRepository` mapper/parser ayrımı + birim testleri (Sıra 2)
3. ✅ `PlayerScreen` bölme (Sıra 3) — gerçek TV'de manuel test edildi
4. (izleme — opsiyonel) `DetailScreen` (600) / `SearchScreen` (455): 800 altı, gerekirse bölünür

> Not: Sıra 3 dosyaları planda `ui/player/components/` altında öngörülmüştü; uygulamada
> import churn'ünü sıfırlamak için **aynı `ui.player` paketinde** (`PlayerControls.kt`,
> `PlayerSheets.kt`, `PlayerSkip.kt`) tutuldu. `shortFansubLabel` PlayerScreen'de kaldı.

---

## 📋 Bir Sonraki Oturum İçin Notlar
- Önce `AI_Guidelines.md`, `Project_Goals.md` ve bu dosyayı oku.
- **Planlı refaktör işi kalmadı** — 3 sıra da bitti, test suite 31 yeşil, hiçbir dosya 800'ü aşmıyor. Yeni iş gelirse 800 satır + tek sorumluluk + katman kuralına uy.
- Opsiyonel ileri adımlar: `DetailScreen`/`SearchScreen` izleme; `Xeh` için Robolectric testi; UI ekranları için enstrümante test.
- Her değişiklik sonrası bu dosyanın **Son Değişiklikler** + ilgili bölümünü güncelle.
- Performans şüphesinde `assembleRelease` ile test et (debug zayıf TV'de kasar). Türkçe locale çıktıda "BUİLD SUCCESSFUL" (dotlu İ) yazar — grep filtrelerinde dikkat.
- Kablosuz ADB: cihaz `Grundig Google UHD TV` (akropoli); eşleştirme penceresi her açılışta yeni port/kod üretir, açık kalmalı.
- `Xeh.kt` ve servis parse'ı kırılgan — site değişimine duyarlı, dikkatli dokun.
