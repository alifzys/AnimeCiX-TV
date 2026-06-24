# AI_Guidelines.md — AnimeCiX TV

> Bu dosya, bu kod tabanında çalışan her AI agent'ı (ve insan geliştirici) için bağlayıcı kurallar setidir.
> Proje: **Compose for TV** tabanlı Android TV anime istemcisi (`com.alifzys.an1mecix`). Tek modül (`app`), MVVM mimarisi.

---

## 1. Kod Kalitesi

- **800 satır sınırı:** Hiçbir `.kt` dosyası 800 satırı geçemez. Geçerse tek-sorumluluk ekseninde bölünür.
  - 600 satıra ulaşan dosya **izleme listesine** alınır, agent bunu açıkça uyarır.
  - Bilinen ihlal: `ui/player/PlayerScreen.kt` (998) → bölünmeli (bkz. Memory Bank refaktör planı).
- **Tek Sorumluluk (Unix: Do One Thing Well):** Her dosya/sınıf/`@Composable` tek bir iş yapar.
  - Bir `@Composable` hem ekran düzeni hem ağ/iş mantığı içermez — iş mantığı `ViewModel`'e gider.
  - Bir `Repository` ham parse/regex mantığı barındırmaz — `mapper`/`parser` dosyasına taşınır.
- **Docstring zorunlu (KDoc `/** ... */`):** Her `public`/`internal` fonksiyon, sınıf ve `ViewModel` için KDoc yazılır.
  - `private` yardımcılar için ad yeterince açıklayıcı değilse tek satır KDoc eklenir.
- **Fonksiyon uzunluğu:** Mantık fonksiyonları 50 satırı geçmemeli. `@Composable`'lar için sınır esnek; 80 satırı geçen Composable alt-Composable'lara bölünür.
- **Magic number yasak:** Sabitler (zaman aşımı, sayfa boyutu, animasyon süresi, padding ölçüsü) ilgili `object Constants` / `Dimens` / tema dosyasına taşınır.

### Kotlin / Compose / TV — Yasaklı Pratikler
- ❌ `runBlocking` UI thread'inde — coroutine + `viewModelScope` kullan.
- ❌ Composable içinde `LaunchedEffect`/`remember` olmadan ağ veya I/O çağrısı (her recomposition'da tetiklenir).
- ❌ `GlobalScope` kullanımı — daima `viewModelScope` / structured concurrency.
- ❌ `!!` (force unwrap) — `?:`, `requireNotNull` veya erken `return` kullan.
- ❌ `MutableState`/`StateFlow` dışında public mutable state sızdırmak — UI'ya `State`/`StateFlow` (immutable) ver.
- ❌ Donanım focus'unu kırmak: TV'de her etkileşimli öğe `focusable` ve `dpad` ile erişilebilir olmalı.
- ❌ `Modifier` zincirinde ölçüleri hardcode etmek (bkz. magic number).

---

## 2. Katman Ayrımı & Import Hiyerarşisi

Katmanlar **tek yönlü** bağımlıdır. Ok yönü = "import edebilir":

```
ui  ──►  domain  ◄──  data
 │                      │
 └──────► (sadece domain.model'i paylaşır) ◄──┘
```

| Katman | Klasör | Sorumluluk | İmport edebilir | İmport EDEMEZ |
|--------|--------|-----------|-----------------|----------------|
| **ui** | `ui/**` | Compose ekranları, bileşenler, ViewModel'ler, navigation, tema | `domain.model`, kendi alt-paketleri | `data.api`, `data.local`, OkHttp/Room tipleri |
| **domain** | `domain/**` | Saf model (`Models.kt`), iş kuralları | (hiçbiri — bağımsız) | `ui`, `data`, framework tipleri |
| **data** | `data/**` | API servisleri, Room, repository, download/update | `domain.model` | `ui` |

**Kurallar:**
- UI/görsel katman **iş mantığı veya ağ/DB erişimi içeremez.** Ekran yalnızca `ViewModel`'in verdiği state'i çizer ve event gönderir.
- `ViewModel` → `Repository` çağırır; `Repository` → `Service`/`Dao` çağırır. Bu zincir atlanmaz.
- DTO (`ApiDto.kt`) **asla** UI'ya sızdırılmaz; `Repository` DTO'yu `domain.model`'e çevirir.
- `domain` hiçbir Android/framework tipine (`Context`, `okhttp`, `room`) bağımlı olamaz.

---

## 3. AI Agent Çalışma Kuralları

1. **Değişiklik öncesi etki analizi:** Her değişiklikten önce etkilenecek dosyaları listele. "Bu değişiklik X'i kırabilir mi?" kontrolünü yaz.
2. **Atomik değişiklik:** Birden fazla dosyayı etkileyen bir değişiklik (örn. fonksiyon imzası) tek seferde, tüm çağıranlar güncellenerek tamamlanır. **Yarım bırakma.**
3. **Refaktör = davranışı koru:** Önce davranışı sabitle (mümkünse test, değilse manuel doğrulama notu), sonra değiştir. Eski dosyayı yeni yapı çalışana kadar **silme**.
4. **Belirsizlikte sor, tahmin etme:** API yanıt şekli, fansub ayrıştırma kuralı, X-E-H algoritması gibi proje-spesifik noktalarda emin değilsen sor.
5. **Memory Bank güncelle:** Her anlamlı işlem sonunda `memory_bank/Memory_Bank.md` ilgili bölümünü güncelle (Son Değişiklikler + gerekiyorsa Teknik Borç / ADR).
6. **800 satır uyarısı:** 800'e yaklaşan veya geçen dosya görünce dur, uyar ve bölme öner.
7. **Release ile doğrula:** Performans şüphesinde `assembleRelease` ile test et — debug build zayıf TV'lerde kasar (proje notu).

---

## 4. Proje Yapısı & İsimlendirme

### Naming Convention (mevcut, standartlaştırıldı)
- Dosya & sınıf: `PascalCase` (`PlayerScreen.kt`, `AnimeRepository`).
- Composable fonksiyonlar: `PascalCase` (`AnimeCard`, `QualitySheet`).
- Normal fonksiyon / değişken: `camelCase`.
- Sabitler: `UPPER_SNAKE_CASE` (`const val`).
- Paket: `com.alifzys.an1mecix.<katman>.<özellik>` (`ui.player`, `data.api`).
- Bir ekran = bir `XScreen.kt` + (state'i varsa) bir `XViewModel.kt`, aynı özellik klasöründe.

### Hedef Klasör Yapısı (temizlenmiş)
```
app/src/main/java/com/alifzys/an1mecix/
├─ MainActivity.kt, AnimeCixApp.kt
├─ core/                  # YENİ: Constants (URL'ler), Dimens, ortak util
├─ data/
│  ├─ api/               # Service + DTO + HttpClient + Xeh
│  ├─ local/             # Room: AppDatabase, Daos, entities
│  ├─ repository/        # AnimeRepository, UserDataRepository
│  │  └─ mapper/         # YENİ: DTO→model dönüşümü + fansub/comment parse (repo'dan çıkarılacak)
│  ├─ download/  update/
├─ domain/model/          # Saf modeller (framework'süz)
└─ ui/
   ├─ <özellik>/         # home, detail, player, search, saved, settings, categories
   │  ├─ XScreen.kt + XViewModel.kt
   │  └─ (player için) components/  # YENİ: sheet'ler, progress, overlay parçaları
   ├─ components/         # paylaşılan: AnimeCard, Carousel, ...
   ├─ navigation/  theme/
```

### Test Dosyaları
- Birim testleri: `app/src/test/java/com/alifzys/an1mecix/...` (JVM, framework'süz mantık: mapper, parser, X-E-H).
- Enstrümante testler: `app/src/androidTest/...` (Room, UI).
- İsimlendirme: kaynak dosyayla aynı ad + `Test` son eki → `AnimeRepositoryMapperTest.kt`.
- **Öncelik:** Refaktör öncesi `mapper`/`parser` ve `Xeh` için birim testi yazmak (davranış güvenlik ağı).

---

## 5. Hızlı Checklist (her PR / oturum)
- [ ] Değişen dosya 800 satırı geçmiyor mu?
- [ ] Yeni iş mantığı UI yerine ViewModel/Repository'de mi?
- [ ] DTO UI'ya sızdırılmadı mı?
- [ ] Hardcoded URL/sabit `core/Constants`'a mı kondu?
- [ ] Public fonksiyonlarda KDoc var mı?
- [ ] `memory_bank/Memory_Bank.md` güncellendi mi?
