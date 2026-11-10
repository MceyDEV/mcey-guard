# MCEY Guard — Uygulama/Mağaza/Oyun Engelleyici

Android uygulaması. Telefonda **uygulama mağazaları**, **oyunlar** ve **senin
seçtiğin uygulamalar** açıldığında, uygulamayı anında kapatır, ekranda uyarı
yazısı gösterir ve ana ekrana döner.

Bu klasör hazır bir **Android Studio projesidir**. APK'yı aşağıdaki iki yoldan
biriyle üretirsin.

---

## Yol 1 — Android Studio ile (en kolay, önerilen)

1. [Android Studio](https://developer.android.com/studio) indir ve kur (ücretsiz).
2. Android Studio > **Open** > bu `MceyGuard` klasörünü seç.
3. İlk açılışta Gradle bağımlılıkları internetten iner (birkaç dakika). Bekle.
4. Menüden **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Sağ altta çıkan bildirimde **locate**'e bas. APK şuradadır:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Bu `app-debug.apk` dosyasını telefona at ve kur.

## Yol 2 — Komut satırı ile (Android SDK kuruluysa)

```bash
# ANDROID_HOME / local.properties ayarlı olmalı
cd MceyGuard
./gradlew assembleDebug
# Çıktı: app/build/outputs/apk/debug/app-debug.apk
```

> Not: Proje Android SDK ister (compileSdk 34, minSdk 26). Android Studio bunu
> otomatik indirir. Komut satırında kuruluysa `local.properties` içine
> `sdk.dir=/SDK/yolu` yazman gerekir.

---

## Telefonda kurulum ve açılış

1. **Bilinmeyen kaynaklar / Bu kaynağa izin ver**: APK'yı kurarken Android
   izin isteyecek, onayla.
2. Uygulamayı aç. **"Erişilebilirliği Aç"** düğmesine bas, listeden
   **MCEY Guard**'ı bul ve aç. (Uygulamanın çalışması için bu şart.)
3. İstersen **"Ekran üstü uyarı iznini ver"** ile tam ekran kırmızı uyarıyı
   etkinleştir. (Vermezsen uyarı kısa bildirim olarak çıkar; engelleme yine çalışır.)
4. Hangi grupların engelleneceğini anahtarlarla seç:
   - Uygulama mağazaları (Play Store, Galaxy Store, Aptoide, APKPure, Cafe Bazaar…)
   - Oyunlar (otomatik tespit)
   - APK yükleyici (yandan kurulumu engeller)
5. **"Engellenecek uygulamaları seç"** ile tek tek uygulama da ekleyebilirsin.
6. Ekranda çıkacak uyarı yazısını istediğin gibi değiştir ve kaydet.

---

## Nasıl çalışır?

- `BlockerService` bir **AccessibilityService**'tir; ön plana gelen uygulamanın
  paket adını okur.
- Paket, ayarlara göre engelliyse: **ana ekrana dönülür** (uygulama kapanır) ve
  **uyarı yazısı** gösterilir.
- Oyunlar, Android'in uygulama kategorisi (`CATEGORY_GAME`) ile otomatik bulunur.
- Mağazalar bilinen paket adları listesinden tanınır (kod içinde
  `BlockRules.STORE_PACKAGES`).

## Önemli notlar

- **Erişilebilirlik iznini kullanıcı kapatabilir.** Bu, kendi telefonunu
  kontrol etmek / çocuk kontrolü içindir; kurcalamayı tamamen imkânsız kılan bir
  "kurumsal cihaz yönetimi" (Device Owner) çözümü değildir. Daha sıkı kilit
  istiyorsan söyle, Device Admin / kiosk yönüne çevirebiliriz.
- Bu **debug** APK'dır; kendi telefonuna kurman için yeterlidir. Paylaşmak /
  imzalı sürüm istersen `assembleRelease` + imzalama anahtarı gerekir.

## Paket adını / uygulama adını değiştirme

- Uygulama adı: `app/src/main/res/values/strings.xml` içindeki `app_name`.
- Paket adı (`com.mcey.guard`): `app/build.gradle` içindeki `namespace` ve
  `applicationId`, bir de Kotlin dosyalarının `package` satırları.
