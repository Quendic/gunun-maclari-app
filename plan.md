# IPTV-Mac-Projesi: Ticari ve Teknik Uygulama Planı (plan.md)

Bu belge, uygulamanın sıfırdan ticari bir ürüne dönüştürülme sürecini tüm teknik detaylarıyla kapsar.

> [!CAUTION]
> **Dağıtım Stratejisi**: Bu uygulama Google Play Store'a yüklenmeyecektir (IPTV içerik politikası riski). APK, GitHub Releases ve/veya kendi web sitesi üzerinden dağıtılacaktır. Uygulamanın mevcut oto-güncelleme sistemi (GitHub Releases) bu stratejiyi desteklemektedir.

---

## 1. Aşama: Kimlik Doğrulama ve Güvenlik Altyapısı
Uygulamanın giriş kapısı ve ücretli içerik koruması bu aşamada kurulur.

### 1.1 Firebase & Auth Kurulumu
* [x] **Firebase Proje Yapılandırması**: `google-services.json` dosyasının temini ve `build.gradle` entegrasyonu. ✅
* [x] **Google Sign-In**: Kullanıcıların tek tıkla kayıt olması için Firebase Auth ile Google entegrasyonu. *(AuthManager.kt + LoginActivity.kt oluşturuldu)*
* [x] **Login UI**: *(LoginActivity.kt — Glassmorphism giriş ekranı)*
    - Premium hissi veren (vibrant colors, glassmorphism) giriş ekranı.
    - "Google ile Devam Et" butonu ve loading animasyonları.
* [x] **Session Management**: Kullanıcı giriş bilgilerinin güvenli bir şekilde saklanması ve uygulama her açıldığında kontrol edilmesi. *(AuthManager.kt — oturum kontrolü LoginActivity'de yapılıyor)*

---

## 2. Aşama: M3U Filtreleme Web Servisi (Local Backend)
Büyük M3U listelerini kullanıcı cihazını yormadan işleyen merkezi bir motor kurulur.

### 2.1 Sunucu Mimarisi (Node.js Express)
* [x] **API Endpoint**: `POST /process-m3u` endpoint'i.
* [x] **M3U Parser Engine**:
    - Dev M3U dosyasını (100MB+) stream ederek okuma (bellek tasarrufu).
    - Regex pattern'leri ile sadece maç kanallarını (beIN Sports 1-5, S Sport, Exxen, Eurosport vb.) ayıklama.
    - Kanal adlarını normalize etme (TR: BEIN SPORTS 1 -> beIN Sports 1).
* [x] **Veri Çıkışı**: Filtrelenen kanalları `List<MatchChannel>` formatında hafif bir JSON'a dönüştürme.

### 2.2 Veri Depolama (Firebase Firestore/Storage)
* [ ] **Profil Eşleştirme**: Her kullanıcının kendi M3U URL'sini ve son oluşturulan JSON'unu Firestore üzerinde `users/{uid}/profile` altında saklama.
* [ ] **Oto-Update**: Sunucu, kullanıcının M3U URL'sini periyodik olarak veya manuel tetikleme ile tarayıp JSON'u güncelleyecek.

---

## 3. Aşama: Android Uygulama Entegrasyonu
Backend ve güvenlik sistemlerinin mobil uygulama ile birleştirilmesi.

### 3.1 Kullanıcı Profil Paneli
* [ ] **M3U URL Input**: Kullanıcının kendi linkini girebileceği şık bir ayarlar sekmesi.
* [ ] **Tarama Durumu**: Backend'in M3U'yu işleme sürecini gösteren Progress Bar (Backend'den gelen status bilgisi ile).

### 3.2 ChannelManager Geliştirmesi
* [ ] **Dinamik Veri Akışı**: 
    - `assets/channels.m3u` kullanımı tamamen kaldırılır.
    - Uygulama, kullanıcının profilindeki Firestore linkinden güncel JSON'u çeker.
    - Gelen JSON verisi `Match.kt` ve `ChannelManager.kt` içindeki veri modellerine map edilir.
* [ ] **Offline Cache**: İnternet olmadığında son başarılı listenin gösterilmesi için `Room` veya `SharedPreferences` ile önbelleğe alma.

---

## 4. Aşama: Kullanıcı Deneyimi ve Görsel Cila
Uygulamanın "Premium" hissini güçlendirecek dokunuşlar.

* [ ] **Tema Desteği**: Koyu mod (Dark Mode) optimizasyonu ve özel renk paleti.
* [ ] **Micro-Animations**: Liste yüklenirken skeleton ekranlar ve buton hover efektleri.
* [ ] **Hata Ayıklama**: "M3U Linki Geçersiz" veya "Kanal Bulunamadı" durumları için kullanıcı dostu uyarılar.

---

## 5. Aşama: Dağıtım ve Güncelleme
* [ ] **Kod Karartma (ProGuard/R8)**: APK'nın tersine mühendislikten korunması.
* [ ] **Release APK**: İmzalı (signed) APK paketinin oluşturulması.
* [ ] **Dağıtım Kanalı**: GitHub Releases + uygulama içi oto-güncelleme (mevcut sistem korunur).
* [ ] **Test Listesi**: Farklı cihazlarda (Telefon, Android TV vb.) son kontroller.

---

## 6. Aşama: Ödeme Sistemi (Son Aşama)
Ödeme sağlayıcı (iyzico / Stripe / Papara) belirlendiğinde bu aşama tamamlanacaktır.

* [ ] **Ödeme Sağlayıcı Seçimi**: Türkiye'de en uygun ödeme altyapısının belirlenmesi.
* [ ] **Ödeme Backend API**: Node.js sunucusuna ödeme endpoint'leri.
* [ ] **Premium Durum Yönetimi**: Firestore üzerinde abonelik/ödeme durumu saklama.
* [ ] **Aktivasyon Kontrolü**: Ödeme yapılmamışsa "Premium Ol" ekranına yönlendirme.
* [ ] **Anti-Piracy**: Cihaz ID + Firebase UID eşleştirmesi.
* [ ] **Ödeme Sayfası (Web)**: Kullanıcının ödeme yapabileceği web sayfası.

---
> [!IMPORTANT]
> **Adım Adım İlerleme**: Bu plan bir check-list olarak kullanılacaktır. Her bir alt madde tamamlandığında dosya güncellenecektir.
