# AnimeCiX TV — Resmi Olmayan Android TV İstemcisi

> ⚠️ **Resmi değildir.** Bu uygulama **animecix.tv** ile bağlantılı, onun tarafından
> desteklenen veya onaylanan resmi bir uygulama **değildir**. Bağımsız geliştiriciler
> tarafından yapılmış **3. parti (unofficial) bir istemcidir**. Tüm içerik ve veriler
> animecix.tv'ye aittir; bu uygulama yalnızca o içeriği Android TV'de görüntülemek için
> bir arayüz sağlar. Eğitim ve kişisel kullanım amaçlıdır.

Android TV / Google TV için **Kotlin + Jetpack Compose for TV** ile yazılmış anime
izleme istemcisi. D-pad ile gezinme, ExoPlayer tabanlı oynatıcı, opening/ending atlama,
kalite/hız/fansub seçimi, izleme geçmişi ve görüntü iyileştirme (sharpen / Anime4K) içerir.

## Teknolojiler

- **Dil:** Kotlin
- **UI:** Jetpack Compose for TV (`androidx.tv:tv-material`)
- **Oynatıcı:** Media3 / ExoPlayer (+ `media3-effect` GL shader pipeline)
- **Ağ:** OkHttp + kotlinx.serialization
- **Yerel veri:** Room (izleme geçmişi / liste)
- **Görsel:** Coil
- **Min SDK:** 23 · **Target/Compile SDK:** 34 / 35

## Derleme

Gereksinimler: **JDK 17**, **Android SDK 35**, Android Studio (Ladybug+) veya komut satırı.

```bash
# 1) Android SDK yolunu tanımla (Android Studio kullanmıyorsan)
#    Proje kökünde local.properties oluştur:
echo "sdk.dir=/ANDROID/SDK/YOLU" > local.properties

# 2) Release APK derle
./gradlew assembleRelease
# Çıktı: app/build/outputs/apk/release/app-release.apk

# 3) TV'ye kur (adb ile)
adb install -r app/build/outputs/apk/release/app-release.apk
```

Debug için: `./gradlew assembleDebug`. Not: zayıf TV'lerde **release** build belirgin
şekilde daha akıcıdır (R8 + optimizasyon).

## Paket adı

`com.alifzys.an1mecix` — resmi animecix uygulamasından ayırt edilebilmesi için bilinçli
olarak farklı bir paket adı kullanılır.

## Sorumluluk Reddi

Bu proje hiçbir içeriği barındırmaz veya dağıtmaz; yalnızca herkese açık animecix.tv
uçlarına istek atar. Geliştiriciler, uygulamanın kullanımından doğabilecek sonuçlardan
sorumlu değildir. Marka ve telif hakları ilgili sahiplerine aittir.

## Lisans

[MIT](LICENSE) © 2026 alifzys

Not: MIT lisansı bu uygulamanın **kendi kaynak koduna** uygulanır. Erişilen içerik ve
veriler animecix.tv'ye aittir ve bu lisansın kapsamı dışındadır.
