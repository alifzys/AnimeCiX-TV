# Changelog

Bu projedeki önemli değişiklikler bu dosyada belgelenir.

Format [Keep a Changelog](https://keepachangelog.com/tr/1.1.0/) temellidir ve proje
[Semantic Versioning](https://semver.org/lang/tr/) kullanır.

## [Unreleased]

## [1.1.9] - 2026-07-12

### Değiştirildi
- **İleri/geri sarma tamamen yenilendi (hızlanan + smooth):** artık her tuşta ayrı ayrı sarma (ve her seferinde bekleme) yok. Sol/sağ **basılı tuttukça hızlanır** (adım büyür), bıraktığında **tek seferde** o konuma gidip normal hızda devam eder. Çubuk hem tek adımda hem hızlı sarmada yumuşak kayar. (Önceki 10 sn'lik "atla-bekle-atla" davranışı kaldırıldı.)

## [1.1.8] - 2026-07-12

### Eklendi
- **Altyazı Düzenleyici (canlı önizlemeli):** hem **Ayarlar → Altyazı → Altyazı Düzenleyici** (boş önizleme ekranı + sağda slider'lar), hem de oynatıcıda kontrol çubuğundaki **"Aa"** butonu (video üstünde canlı). Boyut ve dikey konum (slider), font, iç/dış renk ve kenar/gölge (Kontur/Gölge/Kabartma/Kapalı). Ayarlardaki eski "Altyazı Boyutu" düğmeleri kaldırıldı.
- **Font seçimi:** gömülü presetler (Amaranth, Quicksand, PT Sans, PT Serif — hepsi SIL OFL) **+** kendi fontun: `.ttf/.otf` dosyanı `Android/data/com.alifzys.an1mecix/files/fonts/` klasörüne atınca düzenleyicide listelenir.
- **Renk Canlandırma (AI) — Ayarlar → Renk:** Kapalı / Hafif / Güçlü. Eski/soluk anime renklerini otomatik canlandırır (akıllı siyah-beyaz nokta esnetme + kontrast + vibrance) → renkler yeniymiş gibi görünür. GPU shader ile; gerçek zamanlı ML değil. Değişiklik için bölümü yeniden açın.
- **Kontroller açılınca altyazı yukarı kayar** → ilerleme çubuğunun arkasında kalmaz.

### Düzeltildi
- **Her bölüm başında "Sonraki Bölüm" bandı çıkıyordu** → bölüm geçişinde bir önceki bölümün (sona yakın) konum/süre değerleri kısa süre taşınıp "bitişe kalan sn" sezgisini yanlış tetikliyordu. Artık geçişte sıfırlanır ve sezgi ilk 90 sn'de hiç çalışmaz.
- **Anime detayında bir kez aşağı inince en üste/fotoğrafa dönülemiyordu** (çok sezonlu başlıklarda) → sezon seçicisinden yukarı da üstteki butona yönlendirilir; ekrana girişte en üste odaklanılır.

## [1.1.7] - 2026-07-05

### Eklendi
- **Altyazı boyutu ayarı** (Ayarlar → Altyazı): Küçük / Orta / Büyük / Çok Büyük. Altyazılar okunurluk için siyah konturlu gösterilir.
- **Altyazı yazı tipi: Amaranth** (varsayılan). Font, [SIL Open Font License](docs/fonts/Amaranth-OFL.txt) altında pakete gömülüdür.

## [1.1.6] - 2026-07-05

### Düzeltildi
- **Yapay çeviri altyazıları artık geliyor (gerçek düzeltme).** tau'nun altyazı için istediği `vid`, embed adresinde değil animecix **video id'sinin** kendisiymiş; artık video id'si `?vid=` olarak API'ye iletiliyor. Gerçek bir bölümle uçtan uca doğrulandı (subs geliyor → `/vtt/` geçerli WebVTT). 1.1.5'teki "adresten vid çıkar" yaklaşımı bu yüzden yetersizdi.

## [1.1.5] - 2026-07-05

### Düzeltildi
- **Yapay çeviri altyazıları hâlâ gelmiyordu** → tau-video embed adresindeki `vid` parametresi API'ye iletilmiyordu; altyazılar (soft-sub) yalnızca bu parametreyle döndüğü için hiç gelmiyordu. Artık iletiliyor.

### Eklendi
- **Altyazı seçici:** oynatıcıda altyazı dilini seçme / kapatma (yapay çeviri dâhil).
- **Oynatıcı kontrolleri elden geçirildi:** OK artık videoyu duraklatır ve kontrolleri getirir (tekrar OK → devam + gizle); YUKARI kontrolleri gösterir, AŞAĞI gizler. Buton satırı ilerleme çubuğunun üstüne alındı.

## [1.1.4] - 2026-07-04

### Düzeltildi
- **Otomatik güncelleme "Kuruluyor…"da takılıyordu** → indirmeden sonra sistemin kurulum onay ekranı artık düzgün açılıyor (PackageInstaller `STATUS_PENDING_USER_ACTION` işleniyor).
- **Güncelleme ekranı arkadaki menüyü bloke etmiyordu** → indirme/kurulum sırasında ekran tam bloke edilir; D-pad ve dokunma arkaya geçmez.

## [1.1.3] - 2026-07-04

### Eklendi
- **Altyazı desteği:** yapay çeviri / soft-sub altyazılar artık oynatılıyor. tau-video WebVTT (`/vtt/`) altyazıları oynatıcıya bağlanır (Türkçe varsa varsayılan seçilir); indirilen bölümlerde altyazı da indirilip çevrimdışı gösterilir.
- **Oynatıcıda D-pad ile sarma:** kontroller kapalıyken sağ/sol tuşu doğrudan ileri/geri sarar (±10 sn).
- **Anime4K ölçek ayarı:** upscale oranı seçilebilir (1.5x / 2x / 2.5x / 3x) — Ayarlar → Görüntü.
- GitHub Actions CI: her push/PR'da `assembleRelease` çalışır, başarılıysa APK'lar artifact olarak yüklenir.
- Hata bildirimi ve özellik isteği için Türkçe issue şablonları.
- Bu `CHANGELOG.md`.
- Geliştirme rehberi ve "canlı bellek" dokümanları (`AI_Guidelines.md`, `Project_Goals.md`, `memory_bank/`).
- Birim test altyapısı (`app/src/test`): DTO ayrıştırma, mapper/fansub/JSON yardımcıları ve sürüm karşılaştırma için 31 test.

### Değiştirildi
- **Mimari refaktör** (davranış aynı): tüm sabit URL'ler `core/Constants`'a toplandı; `AnimeRepository` ağ orkestrasyonu ile DTO→model dönüşümü/parse mantığı `data/repository/mapper`'a ayrıldı (413 → 172 satır); `PlayerScreen` 998 satırdan 376'ya indirildi, kontrol/sheet/skip bileşenleri ayrı dosyalara taşındı.
- Ana ekranda poster odak animasyonu daha akıcı hale getirildi (gecikme kapısı kaldırıldı, geçiş süresi ayarlandı).

### Düzeltildi
- **Opening/Ending atlama yanlış zamanda çıkıyordu** (bölümün en başında, gerçek opening'e bakmadan) → artık tau-video'nun gerçek intro/outro zamanları kullanılır; "Opening Atla" bandı yalnızca opening sırasında çıkar.
- **Yapay çevirili animelerde altyazı görünmüyordu** → altyazı artık oynatılır (yukarıya bakınız).
- Oynatıcıda **kalite değiştirince bölüm baştan başlıyordu** → artık mevcut konum korunur.
- **Arama geçmişi her karakteri kaydediyordu** ("b", "bl", "ble"...) → artık yalnızca bir sonuç açıldığında veya "ARA"ya basıldığında tam sorgu kaydedilir.
- Ana ekranda aşağı kaydırılmışken **Geri tuşu ortadaki bir satıra atlıyordu** → artık en üste dönüp vitrindeki "İncele" butonuna odaklanır; en üstteyken uygulamadan çıkar.

## [1.1.1] - 2026-06-04

İlk herkese açık (açık kaynak, MIT) sürüm — Android TV / Google TV için resmi olmayan istemci.

### Eklendi
- **Ana ekran:** öne çıkan içerik vitrini, kategori satırları, son eklenen bölümler, kaldığın yerden devam et.
- **Detay sayfası:** puan, tür, süre, oyuncu kadrosu, benzer yapımlar, sezon/bölüm listesi, kişisel izleme listesi.
- **Oynatıcı (Media3 / ExoPlayer):** kalite seçimi (480p/720p/1080p), fansub/kaynak seçimi, oynatma hızı (0.5×–2×), Opening/Ending otomatik atlama, sonraki bölüme otomatik geçiş, kaldığın yerden devam (konum hatırlama).
- **Görüntü iyileştirme (GL shader):** Keskinlik ve Anime4K (upscale + çizgi netleştirme) modları.
- **Arama & kategoriler:** ekran üstü D-pad klavyesi, popüler animeler, arama geçmişi, türlere göre tarama.
- **İndirilenler:** çevrimdışı izleme.
- **Ayarlar:** varsayılan kalite, otomatik atlama, görüntü iyileştirme modu.
- **Otomatik güncelleme:** açılışta GitHub Releases'ten yeni sürüm kontrolü, indirme ve kurulum.
- **ABI-split APK'lar:** `universal`, `arm64-v8a`, `armeabi-v7a`.

[Unreleased]: https://github.com/alifzys/AnimeCiX-TV/compare/v1.1.9...HEAD
[1.1.9]: https://github.com/alifzys/AnimeCiX-TV/releases/tag/v1.1.9
[1.1.8]: https://github.com/alifzys/AnimeCiX-TV/releases/tag/v1.1.8
[1.1.7]: https://github.com/alifzys/AnimeCiX-TV/releases/tag/v1.1.7
[1.1.6]: https://github.com/alifzys/AnimeCiX-TV/releases/tag/v1.1.6
[1.1.5]: https://github.com/alifzys/AnimeCiX-TV/releases/tag/v1.1.5
[1.1.4]: https://github.com/alifzys/AnimeCiX-TV/releases/tag/v1.1.4
[1.1.3]: https://github.com/alifzys/AnimeCiX-TV/releases/tag/v1.1.3
[1.1.1]: https://github.com/alifzys/AnimeCiX-TV/releases/tag/v1.1.1
