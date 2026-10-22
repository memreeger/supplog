# Kullanıcı Testi Geri Bildirimleri – Düzeltme Planı (Tur 1)

> **Kaynak:** Burak Polat Yavan'ın canlı site testi (2026-10-21).
> **Durum:** Uygulandı ve lokalde iki test hesabıyla uçtan uca doğrulandı (2026-10-22, §5). Backend `fix/feedback-round-1`, frontend `fix/feedback-round-1` branch'lerinde; henüz commit/canlı yok.
> **İlişkili:** [PROJECT_MAP.md](PROJECT_MAP.md), [FEATURE_DESIGN.md](FEATURE_DESIGN.md)

## 0. Önce şunu bil: "sistem neden patladı?"
Frontend'e 2026-10-22 04:13'te `c7c8f86` commit'i eklenmişti. Bu commit backend'de **olmayan** bir sözleşmeyi kullanıyordu:
- `PATCH /routine-executions/routines/{id}/today/undo` (geri alma) → backend'de yoktu, 404.
- Hata cevabında `messageKey`, `details` alanları → backend göndermiyordu.
- Geçmiş kayıtlarında `supplementName`, `supplementDosage`, `routineDeleted` → backend göndermiyordu.

Yani frontend backend'den önce değişmişti. Bu commit'i silmeden `backup/backend-contract-c7c8f86` branch'ine aldık. **Plan:** Backend'i bu sözleşmeye göre yazmak, ardından frontend commit'ini geri getirip eksiklerini tamamlamak. Böylece iki taraf yeniden uyumlu olacak.

---

## 1. Sorunlar, kök nedenleri ve çözümler

| # | Sorun (geri bildirim) | Kök neden | Çözüm | Taraf |
|---|---|---|---|---|
| 1 | Kayıt ekranında `auth.validation.birthDatePast` yazıyor | Çeviri anahtarı tr/en dosyalarında yok | Anahtar eklenir, metin netleşir | FE |
| 2 | Çok uzun isimler mobilde sayfayı bozuyor (rutinler sayfası yana taşıyor, takviyelerde sağdaki menü kullanılamıyor) | Kartlarda `min-w-0` / satır kırma yok; boşluksuz uzun metin kartı genişletiyor | Kartlara `min-w-0`, isimlere `line-clamp` + `overflow-wrap:anywhere`, input'lara `maxLength=100` | FE |
| 3 | Bugün eklenen ama saati geçmiş rutin anında "Kaçırıldı" oluyor | `datesToCreate` bugünün kaydını, rutin saatten **sonra** oluşturulsa bile yaratıyor; 1 dk sonra scheduler MISSED yapıyor | Bugünün dozu, rutin oluşturulduğu anda **kaçırma süresi (missedAt) zaten geçmişse oluşturulmaz** → ilk doz yarın. Saat geçmiş ama tolerans içindeyse (ör. 08:00 rutin, 08:10'da eklendi) bugünkü doz yine işaretlenebilir. Formda "İlk doz yarın" notu | BE + FE |
| 4 | 08:00 rutinine 12:34'te "Tamamla/Atla" deyince "Kaçırıldı" oluyor | Tasarım gereği `missedAt` (08:30) geçtikten sonra işaretleme reddediliyor (`routine.execution.deadline.passed`), ama UI butonları göstermeye ve durumu **kendi saatine göre** hesaplamaya devam ediyordu | Durum etiketi **sadece backend'den** gelir. `missedAt` geçmiş dozlarda butonlar yerine "Süre doldu" yazar. Hata gelirse liste yenilenir | FE |
| 5 | "Kaydederken UTC, gösterirken lokal" kafa karışıklığı | Rutin saati profil saat dilimindeki duvar saati (08:00 İstanbul); ekranda bazı tarihler tarayıcının saat dilimiyle biçimleniyordu | Tüm tarih/saatler **profil saat dilimiyle** gösterilir. Cihaz saat dilimi profilden farklıysa uyarı bandı çıkar | FE |
| 6 | Saat dilimi değişince rutinler değişmiyor, hangi dilimde olduğu yazmıyor | Davranış doğru (rutin 08:00 = yeni dilimde de 08:00, bekleyen kayıtlar zaten yeniden hesaplanıyor) ama arayüz bunu anlatmıyor | Profilde ve rutin formunda "Saatler profil saat diliminize (Europe/Istanbul) göredir" bilgisi | FE |
| 7 | Yanlışlıkla tamamlanan/atlanan doz geri alınamıyor | Endpoint yok | **Yeni:** `PATCH /api/v1/routine-executions/routines/{routineId}/today/undo` → COMPLETED/SKIPPED → PENDING, **sadece missedAt'ten önce**. UI: işaretledikten sonra "Geri al" snackbar'ı + "Değiştir" menüsü | BE + FE |
| 8 | Rutinde kullanılan takviye silinince hata ancak onaydan sonra geliyor ve **rutin ID'leri** gösteriyor `[6, 9]` | Mesaj ID listesiyle kuruluyor; cevapta yapılandırılmış veri yok | Hata cevabına `messageKey` + `details.routines` [{id, routineTime, frequency}] eklenir, mesajda ID yok. UI: "Bu takviye N rutinde kullanılıyor" diyalogu, rutinleri saat/sıklıkla listeler, "Rutinlere git" butonu | BE + FE |
| 9 | Silinen rutinler geçmişte **ID ile** görünüyor | Geçmiş kaydı sadece `routineId` döndürüyor; frontend adı aktif rutin listesinden buluyor, silinmiş rutin orada yok | Execution cevabına `supplementName`, `supplementDosage`, `routineDeleted` eklenir (rutin silinse de ad korunur). Geçmişte silinmiş rutin "silindi" etiketiyle gösterilir | BE + FE |
| 10 | Takviye aramasında sonuç yoksa "Henüz takviye eklenmedi" yazıyor | Boş liste ile boş arama sonucu aynı mesajı kullanıyor | "'xyz' için sonuç bulunamadı" + "Aramayı temizle" butonu | FE |
| 11 | "Belirli tarihe kadar" seçip bitiş tarihi girmeyince "Bitiş tarihi başlangıçtan önce olamaz" yazıyor | Form şeması iki durumu tek mesajla veriyor | Ayrı mesajlar: "Bitiş tarihi zorunlu" / "Bitiş tarihi başlangıçtan önce olamaz" | FE |
| 12 | Aynı isimde takviye eklenebiliyor | İsim kontrolü yok | Aynı kullanıcının **aktif** takviyeleri arasında büyük/küçük harf ve baştaki/sondaki boşluk fark etmeksizin aynı isim reddedilir: `400`, `messageKey=supplement.name.already.exists`. Güncellemede kendisi hariç. Formda alanın altında gösterilir | BE + FE |
| 13 | İsimde `<img src=x onerror=alert(1)>` görünüyor | **Güvenlik açığı değil:** React metni HTML olarak değil düz metin olarak basıyor, script çalışmadı. Mail şablonları da escape ediliyor | Değişiklik yok. Ek önlem: Vercel'e CSP ve güvenlik başlıkları (`script-src 'self'`, `X-Content-Type-Options`, `Referrer-Policy`) | FE (Vercel) |
| 14 | Genel bakış ekranı `routines` listesini iki kez çekiyor | Aynı veri iki bileşen/efekt tarafından ayrı ayrı isteniyor | Sayfa veriyi tek yerden çeker ve paylaşır | FE |
| 15 | Destek isteğinde (ör. olmayan kullanıcı) hata mesajı popup'ın **arkasındaki** sayfada çıkıyor | Hata sayfa seviyesindeki banner'a yazılıyor | Hata diyalogun içinde, ilgili alanın altında gösterilir; diyalog açık kalır | FE |
| 16 | Oluşturulma/yanıt zamanları 3 saat geride görünüyor (testte bulundu; "UTC kaydedip lokal gösteriyor" şikayetinin kaynağı) | Backend audit zamanlarını UTC ve **ofsetsiz** gönderiyor; frontend `new Date()` bunları tarayıcının yerel saati sanıyor. Ayrıca destek servisi bazı alanları sunucu saatiyle yazıyordu | Backend: destek zamanları `LocalDateTime.now(ZoneOffset.UTC)`. Frontend: `parseServerTimestamp` ofsetsiz zamanları UTC okur, profil saat diliminde gösterir | BE + FE |
| 17 | "Değiştir" menüsü (tamamlandı → atla) çalışmazdı (testte bulundu) | Backend işaretlenmiş dozu tekrar işaretlemeyi reddeder (`already.resolved`) | Menü önce `undo`, sonra yeni işlemi çağırır | FE |
| 18 | Süresi dolmuş ama scheduler'ın henüz MISSED yapmadığı dozda butonlar duruyordu (testte bulundu; madde 4'ün asıl nedeni) | `open = status === PENDING` saate bakmıyordu | Butonlar `missedAt`'e kadar; sonrası "Süre doldu" etiketi | FE |

---

## 2. Yeni API sözleşmesi (geriye uyumlu: sadece alan ekleniyor)

### 2.1 Hata cevabı
```json
{
  "status": 400,
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Bu takviye 2 aktif rutinde kullanıldığı için silinemez.",
  "messageKey": "supplement.cannot.delete.in.use",
  "details": { "routines": [ { "id": 6, "routineTime": "08:00:00", "frequency": "DAILY" } ] },
  "timeStamp": "…",
  "errors": null
}
```
- `messageKey`: dile bağlı olmayan, frontend'in karar vermesi için sabit anahtar. Tüm `BusinessException` ve `ResourceNotFoundException` cevaplarında dolu.
- `details`: Sadece gerektiğinde dolu (şu an yalnız takviye silme engeli).

### 2.2 Execution cevabı (today + history)
Mevcut alanlara ek:
| Alan | Tip | Not |
|---|---|---|
| `supplementName` | string | Rutin/takviye silinmiş olsa da kayıt anındaki takviye adı |
| `supplementDosage` | string | |
| `routineDeleted` | boolean | Rutin veya takviyesi silinmişse `true` |

Geçmiş sorguları rutin + takviyeyi tek sorguda getirir (N+1 yok).

### 2.3 Geri alma
`PATCH /api/v1/routine-executions/routines/{routineId}/today/undo` → `200` + güncel execution.
| Durum | Sonuç |
|---|---|
| Bugünün kaydı COMPLETED/SKIPPED ve `now < missedAt` | PENDING'e döner, `resolvedAt = null` |
| Kayıt zaten PENDING | `400 routine.execution.not.resolved` |
| `missedAt` geçmiş veya MISSED | `400 routine.execution.undo.deadline.passed` |
| Bugünün kaydı yok / rutin bugün planlı değil | `400 routine.execution.not.scheduled.today` veya `404` |

### 2.4 Takviye ismi tekrarı
`POST/PUT /api/v1/supplements` → `400`, `messageKey = supplement.name.already.exists`.

---

## 3. Bilinçli kararlar
1. **Süre dolduktan sonra işaretleme yok (değişmedi).** "Geç aldım" senaryosu istenirse ayrı bir özellik olarak (`LATE` durumu) tasarlanır.
2. **Geri alma sadece süre dolmadan.** Süre dolduktan sonra geri almak kaydı PENDING yapıp hemen MISSED'e düşürürdü.
3. **İsim tekrarı için DB'de unique index yok.** Canlı DB'de zaten aynı isimli aktif takviyeler olabilir; index migration'ı deploy'u kırardı. Uygulama seviyesinde kontrol yeterli (aynı anda iki istek gibi uç durum kabul edildi).
4. **Bugünkü doz kuralı `missedAt`'e göre**, `scheduledAt`'e göre değil: Tolerans içinde eklenen rutin bugünkü dozu kaçırmaz.

---

## 4. Görevler
### Backend (`fix/feedback-round-1`)
- [x] **BE-1** – `ErrorResponse`'a `messageKey`, `details`; tüm handler'lar doldurur.
- [x] **BE-2** – Takviye silme engeli: mesaj ID'siz, `details.routines` (id, routineTime, frequency).
- [x] **BE-3** – Takviye ismi tekrarı kontrolü (create + update, case-insensitive, aktifler arasında).
- [x] **BE-4** – Execution DTO'ya `supplementName`, `supplementDosage`, `routineDeleted`; geçmiş sorgularında fetch.
- [x] **BE-5** – `undo` endpoint'i + kurallar.
- [x] **BE-6** – Bugünkü doz: rutin missedAt'ten sonra oluşturulduysa bugünün kaydı oluşturulmaz.
- [x] **BE-7** – i18n mesajları (tr/en), birim + Postgres testleri.

### Frontend (`fix/feedback-round-1`)
- [x] **FE-1** – `c7c8f86` (backup) geri getirilir.
- [x] **FE-2** – Eksikler: rutinler sayfası kart taşması, `birthDatePast` anahtarı, saat dilimi bilgisi.
- [x] **FE-3** – Typecheck, lint, test, build.
- [x] **FE-4** – Lokal uçtan uca test (tüm 13 madde).

### Canlıya çıkış
Önce backend PR merge → Railway; sonra frontend push → Vercel (sıra önemli; ters olursa §0'daki bozulma tekrarlanır).

---

## 5. Lokal uçtan uca test sonuçları (2026-10-22)
İki test hesabı (A: rutin sahibi, B: destekçi), lokal backend (dev, `MAIL_PROVIDER=log`) + Vite proxy, mobil (375 px) ve masaüstü.

| # | Senaryo | Sonuç |
|---|---|---|
| 1 | Kayıtta gelecek doğum tarihi | ✅ "Doğum tarihi geçmiş bir tarih olmalıdır." |
| 2 | 100 karakterlik boşluksuz isim (takviyeler + rutinler + genel bakış, 375 px) | ✅ Kesiliyor, menüler erişilebilir, yatay taşma yok (`scrollWidth = 375`) |
| 3 | Saati 2 saat önce geçmiş rutin bugün eklendi | ✅ Bugünün listesine girmedi; formda "ilk doz yarın" notu. Tolerans içindeki rutin (10 dk önce) bugün işaretlenebilir |
| 4 / 18 | Süresi dolmuş doz | ✅ Buton yok; backend MISSED yapınca "Kaçırıldı" |
| 5 / 16 | Oluşturulma zamanı | ✅ Gerçek saat (04:35) görünüyor, 3 saat kayma yok |
| 6 | Profil saat dilimi cihazdan farklı | ✅ Uyarı bandı, saatler/"bugün" profile göre |
| 7 / 17 | Tamamla → Geri al | ✅ Snackbar'dan geri alındı, doz beklemeye döndü |
| 8 | Rutinde kullanılan takviyeyi silme | ✅ Onaydan **önce** diyalog: "2 aktif rutinde kullanılıyor" + "Her gün 02:36 / 04:26", ID yok |
| 9 | Geçmişi olan rutini silme | ✅ Geçmişte "Vitamin D · 1000 IU (silindi)" |
| 10 | Takviye aramasında sonuç yok | ✅ '"zzz" ile eşleşen…' + "Aramayı temizle" |
| 11 | Belirli tarihe kadar, bitiş boş | ✅ "Bitiş tarihi zorunludur." |
| 12 | Aynı isim (büyük/küçük harf + boşluk farkıyla) | ✅ Alanın altında "'VITAMIN d' adında bir takviyeniz zaten var." |
| 14 | Genel bakış istek sayısı | ✅ `routines` diğer uçlarla aynı sayıda (dev'de StrictMode ×2, prod'da ×1) |
| 15 | Olmayan kullanıcıya destek isteği | ✅ Hata popup'ın içinde; gerçek kullanıcıya davet → B kabul etti |
| CSP | Production build | ✅ Inline script yok, `script-src 'self'` güvenli |

**Otomatik testler:** Backend 80/80 (Postgres dahil), frontend 74/74 + typecheck + lint + build.

**Gözlem (bu turun kapsamı dışında):** Yeni destek bağlantısında varsayılan erişim `SELECTED_ROUTINES` ve hiç rutin seçili değil; destekçi rutin sahibi seçim yapana kadar hiçbir şey görmez. Varsayılanın "tüm rutinler" olması düşünülebilir (yerel `ux-easy-mode` branch'inde böyle bir değişiklik var).
