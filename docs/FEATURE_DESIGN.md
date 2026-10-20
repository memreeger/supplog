# Supplog – Yeni Özellikler Tasarım Dokümanı

> **Kapsam:** Refresh token + logout, Brevo mail (MailSender) + şifremi unuttum, Firebase FCM bildirimleri (rutin hatırlatma + destekçi bildirimi), Nudge (supporter → owner).
> **Durum:** §9 kararları onaylandı (2026-10-19). Faz 1A (RT-01..20) ve 1A-2 cookie (RT-22..31) uygulandı, 52 test Postgres ile geçti. FE-01..04 frontend reposunda yapıldı (54 test geçti); OPS-01 (Railway/Vercel env) bekliyor.
> **İlişkili:** [PROJECT_MAP.md](PROJECT_MAP.md) (mevcut yapı). Bu doküman o yapıya göre yazıldı.
> **Tarih:** 2026-10-19 · baz commit `f616f14`

---

## 0. Kısıtlar ve genel ilkeler

| Kısıt | Etkisi |
|---|---|
| Tamamen ücretsiz | Brevo free (300 mail/gün), FCM free, ek ücretli servis yok. |
| Domain yok | Brevo'da **tek gönderici adresi doğrulaması** (kendi Gmail adresin). Resend'e geçiş domain alınınca. |
| Frontend Vercel, backend Railway (farklı domainler) | Cross-site cookie güvenilmez (Safari/üçüncü taraf cookie engeli) → refresh token **response body** ile döner. |
| Mobil (Flutter) sonra | Backend şimdiden mobil uyumlu tasarlanır: opak refresh token, device token kaydı, platform alanı. |
| iOS push | APNs için **ücretli Apple Developer hesabı** gerekir. Ücretsiz senaryoda: Android + Web push. |
| Railway | Scheduler'lar uygulama ayaktayken çalışır. Servis uyursa (sleep) hatırlatmalar kaçar → servisin uyumadığı doğrulanmalı. Tek instance varsayımı. |

**Kod ilkeleri (mevcut projeye uyum):**
- Paket düzeni: `service/<alan>/<Alan>Service` + `service/<alan>/impl/<Alan>ServiceImpl`, `controller/<Alan>Controller`, `dto/<alan>/...`, `repository/<Entity>Repository`.
- Şema değişikliği yalnızca yeni Flyway migration ile (`ddl-auto=validate`). Önerilen numaralar: **V9–V13** (§7).
- Hatalar `BusinessException("mesaj.anahtari")` / `ResourceNotFoundException`, metinler `messages.properties` + `messages_en.properties`.
- 401 cevapları `CustomAuthenticationEntryPoint.writeUnauthorizedResponse(response, CODE, key)` formatında (mevcut kodlar: `TOKEN_REVOKED`, `PASSWORD_CHANGE_REQUIRED`).
- Gizli değerler (token) DB'de **asla düz** tutulmaz → SHA-256 hash.
- Yeni env değişkenleri `.env.example` ve `application.properties`'e eklenir, PROJECT_MAP §8 güncellenir.

---

## 1. Refresh Token + Logout

### 1.1 Mevcut durum (kodda doğrulandı)
- `AuthServiceImpl.login/register` → `JwtService.generateToken(userId, tokenVersion)` ile tek access token, `AuthResponseDto(id, username, email, accessToken, tokenType, mustChangePassword)`.
- JWT claim'leri: `sub`, `tokenVersion`, `tokenSchemaVersion`. Süre `JWT_EXPIRATION_MS` (varsayılan 1 saat).
- `JwtAuthenticationFilter` her istekte kullanıcıyı DB'den yükler, `tokenVersion` uyuşmazsa `TOKEN_REVOKED`. `/api/v1/auth/**` filtre dışı.
- `tokenVersion++` yapılan yerler: register, `UserServiceImpl` (şifre değişimi, hesap silme ~satır 78/115), `AdminUserServiceImpl` (4 yer: deaktive, şifre reset, rol, profil vb.).
- Logout yok, refresh yok.

### 1.2 Hedef akış
```
LOGIN  ──► accessToken (15 dk JWT) + refreshToken (opak, 30 gün)
İSTEK  ──► Authorization: Bearer <access>
401 TOKEN_EXPIRED ──► POST /auth/refresh {refreshToken}
                     ──► yeni access + YENİ refresh (eski refresh "rotated" olur)
LOGOUT ──► POST /auth/logout {refreshToken} ──► o refresh revoke, (varsa) device token silinir
LOGOUT-ALL ──► POST /users/me/logout-all ──► tokenVersion++ + kullanıcının tüm refresh'leri revoke + tüm device token'lar silinir
```

### 1.3 Token tasarımı
- **Access token:** Mevcut JWT aynen kalır, sadece süre 15 dk (`JWT_EXPIRATION_MS=900000`). `tokenSchemaVersion` değişmez (yeni claim eklenmiyor).
- **Refresh token:** JWT değil, **opak rastgele değer**: 32 byte `SecureRandom` → base64url (~43 karakter). Neden opak: DB'den iptal edilebilmeli; imzalı JWT'ye gerek yok.
- DB'de `SHA-256(token)` hex (64 karakter) saklanır, düz değer sadece istemciye bir kez döner.
- **Aile (family):** Login'de yeni `family_id` (UUID). Her rotation aynı aileye yeni satır ekler.
- **tokenVersion snapshot:** Refresh satırına üretildiği andaki `user.tokenVersion` yazılır. Refresh sırasında `user.tokenVersion != row.tokenVersion` → reddedilir ve aile revoke edilir.
  → **Avantaj:** Mevcut 6 `tokenVersion++` noktasına dokunmadan şifre değişimi / admin deaktive / rol değişimi tüm refresh'leri otomatik geçersiz kılar.

### 1.4 Tablo: `refresh_tokens` (V9)
| Kolon | Tip | Not |
|---|---|---|
| id | bigserial PK | |
| user_id | bigint FK → users(id) NOT NULL | |
| token_hash | varchar(64) NOT NULL UNIQUE | SHA-256 hex |
| family_id | uuid NOT NULL | index |
| token_version | int NOT NULL | üretildiği andaki user.tokenVersion |
| expires_at | timestamptz NOT NULL | kayan bitiş: her rotation now+30 gün, family_expires_at ile sınırlı |
| family_expires_at | timestamptz NOT NULL | ailenin mutlak bitişi: login+90 gün (K1) |
| created_at | timestamptz NOT NULL | |
| last_used_at | timestamptz | |
| rotated_at | timestamptz | yenilendiğinde dolar |
| replaced_by_id | bigint FK → refresh_tokens(id) | zincir takibi |
| revoked_at | timestamptz | |
| revoke_reason | varchar(30) | LOGOUT, LOGOUT_ALL, REUSE_DETECTED, TOKEN_VERSION_CHANGED, USER_INACTIVE, EXPIRED_CLEANUP |
| device_name | varchar(100) | opsiyonel (login isteğinden) |
| platform | varchar(20) | ANDROID / IOS / WEB / UNKNOWN |
| ip_address | varchar(45) | opsiyonel, denetim için |
| user_agent | varchar(255) | opsiyonel |

Indexler: `(user_id)`, `(family_id)`, `(expires_at)` (temizlik için).

### 1.5 Endpoint'ler
| Metot | Yol | Auth | İstek | Cevap |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | public | mevcut + opsiyonel `deviceName`, `platform` | `AuthResponseDto` + `refreshToken`, `accessTokenExpiresAt`, `refreshTokenExpiresAt` |
| POST | `/api/v1/auth/register` | public | mevcut | aynı (register sonrası otomatik login) |
| POST | `/api/v1/auth/refresh` | public (refresh token kimlik yerine geçer) | `{ refreshToken }` | yeni `AuthResponseDto` |
| POST | `/api/v1/auth/logout` | public | `{ refreshToken, deviceToken? }` | 204 (idempotent; token geçersiz olsa da 204) |
| POST | `/api/v1/users/me/logout-all` | authenticated | – | 204 |
| GET | `/api/v1/users/me/sessions` | authenticated | – | aktif oturumlar (opsiyonel, Faz 2) |
| DELETE | `/api/v1/users/me/sessions/{familyId}` | authenticated | – | 204 (opsiyonel, Faz 2) |

`/auth/refresh` ve `/auth/logout` mevcut `/api/v1/auth/**` permitAll + `AuthRateLimitFilter` (20/dk/IP) kapsamına otomatik girer. JwtAuthenticationFilter onları atlar (doğru davranış: access token süresi dolmuş olabilir).

`AuthResponseDto` alan **eklenerek** genişler (mevcut alanlar korunur → frontend kırılmaz):
```
id, username, email, accessToken, tokenType, mustChangePassword,
refreshToken, accessTokenExpiresAt (Instant), refreshTokenExpiresAt (Instant)
```

### 1.6 Refresh algoritması (`RefreshTokenService.rotate`)
```
1. hash = sha256(request.refreshToken)
2. row = findByTokenHashForUpdate(hash)  // PESSIMISTIC_WRITE (proje zaten bu kalıbı kullanıyor)
3. row yok                         → 401 REFRESH_TOKEN_INVALID
4. row.revokedAt != null           → 401 REFRESH_TOKEN_INVALID
5. row.rotatedAt != null (yeniden kullanım):
      now - rotatedAt <= 10 sn     → 401 REFRESH_TOKEN_INVALID (aileyi revoke ETME; paralel istek yarışı)
      aksi halde                    → tüm aileyi revoke (REUSE_DETECTED), 401 REFRESH_TOKEN_REUSED
6. row.expiresAt <= now            → 401 REFRESH_TOKEN_EXPIRED
7. user = aktif değil (isDeleted)  → aileyi revoke (USER_INACTIVE), 401
8. user.tokenVersion != row.tokenVersion → aileyi revoke (TOKEN_VERSION_CHANGED), 401 TOKEN_REVOKED
9. yeni token üret, aynı family, row.rotatedAt=now, row.replacedById=new.id
10. yeni access JWT + yeni refresh döndür
```
Tümü tek `@Transactional` içinde.
**Frontend kuralı:** Aynı anda tek refresh isteği (tek "in-flight promise"), diğer istekler onu bekler. 10 sn tolerans sadece güvenlik ağıdır.

### 1.7 Hata kodları (yeni)
| HTTP | code | messages key |
|---|---|---|
| 401 | `TOKEN_EXPIRED` | (mevcut filtrede ExpiredJwtException yolu – frontend bu kodu görünce refresh dener; **mevcut kodu doğrula**) |
| 401 | `REFRESH_TOKEN_INVALID` | `auth.refresh.invalid` |
| 401 | `REFRESH_TOKEN_EXPIRED` | `auth.refresh.expired` |
| 401 | `REFRESH_TOKEN_REUSED` | `auth.refresh.reused` |

Frontend: Bu üç refresh kodundan biri → login ekranına yönlendir.

### 1.8 Temizlik
`@Scheduled(cron = "0 30 3 * * *")` → `expires_at < now - 7 gün` veya `revoked_at < now - 7 gün` satırları sil. Tablo şişmesini önler.

### 1.9 İstemciye taşıma: Web = httpOnly cookie, Mobil = body (2026-10-19 kararı)
> İlk taslaktaki "localStorage" kararı iptal edildi: XSS ile 30–90 günlük token çalınabilir.

**Neden Vercel proxy:** `*.vercel.app` ve `*.railway.app` Public Suffix List'te → tarayıcı için farklı siteler → cookie üçüncü taraf sayılır (Safari engeller). Çözüm: frontend `vercel.json` rewrite ile `/api/*` isteklerini Railway'e iletir; tarayıcı sadece kendi origin'iyle konuşur → cookie birinci taraf.
```json
{ "rewrites": [{ "source": "/api/:path*", "destination": "https://<backend>.up.railway.app/api/:path*" }] }
```
Lokal geliştirme: Vite `server.proxy` ile `/api` → `http://localhost:8080` (aynı mantık).

**İstemci modu:** `X-Client-Type: mobile` header'ı varsa **mobil (body) modu**, yoksa **web (cookie) modu**. Varsayılan web (güvenli taraf).

| | Web (cookie modu) | Mobil (body modu) |
|---|---|---|
| login/register/refresh cevabı | `Set-Cookie` ile refresh, body'de `refreshToken = null` | body'de `refreshToken` |
| refresh/logout isteği | token cookie'den okunur, body yok sayılır | token body'den okunur, cookie **asla** okunmaz |
| CSRF | `SameSite=Strict` + `Origin` header izinli listede olmalı | gerekmez (otomatik giden kimlik bilgisi yok) |
| Saklama | Access token bellekte, refresh JS'ye görünmez | `flutter_secure_storage` |

**Cookie:** `supplog_refresh=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=<refresh kalan süre>`. Logout ve logout-all `Max-Age=0` ile siler. Path dar tutulur → cookie sadece auth uçlarına gider. `Secure` dev profilinde kapatılabilir (`REFRESH_COOKIE_SECURE=false`).

**Origin kontrolü (sadece cookie modunda refresh/logout):** `Origin` yoksa veya `app.cors.allowed-origins` listesinde değilse → 403 `ORIGIN_NOT_ALLOWED`. Proxy arkasında Origin = frontend origin'i olduğu için Vercel domaini bu listede olmalı.

**CORS:** Proxy ile web istekleri same-origin olur. Proxy'siz lokal kullanım için `allowCredentials=true` (origin listesi zaten açık liste, `*` değil).

**Rate limit ve gerçek IP:** Proxy arkasında `getRemoteAddr()` proxy IP'sidir. `app.rate-limit.trusted-proxy-hops` (varsayılan 0 = remoteAddr): `X-Forwarded-For` listesinin **sağdan** N'inci girdisi istemci IP'si sayılır (soldaki girdiler istemci tarafından sahte yazılabilir). Railway tek başına: 1. Vercel → Railway: 2 (deploy sonrası log ile doğrulanmalı). Bilinen sınır: saldırgan Vercel'i atlayıp doğrudan Railway'e istek atarsa hops=2'de soldan sahte IP yazabilir; tam çözüm Vercel'den gizli header doğrulaması (Faz 3).

### 1.10 Testler
- Login → refresh → yeni token çalışıyor; eski refresh tekrar → REUSED + aile revoke.
- 10 sn içinde ikinci kullanım → INVALID, aile revoke edilmemiş.
- Şifre değişimi sonrası refresh → TOKEN_REVOKED.
- Admin deaktive sonrası refresh → reddedilir.
- Logout sonrası refresh → INVALID; logout idempotent.
- logout-all sonrası eski access token → TOKEN_REVOKED.
- Postgres entegrasyon: paralel iki refresh → yalnızca biri başarılı (mevcut `PostgresConcurrencyIntegrationTest` kalıbıyla).

---

## 2. Mail Altyapısı (MailSender + Brevo)

### 2.1 Mimari
```
service/mail/
  MailSender.java              (interface)  void send(MailMessage message)
  MailMessage.java             (record)     to, toName, subject, htmlBody, textBody, tags
  MailTemplateService.java     (interface)  MailMessage passwordReset(User, link, Locale) ...
  impl/BrevoMailSender.java    → Brevo HTTP API (RestClient)
  impl/LoggingMailSender.java  → dev/test: maili sadece loglar (link görünür)
  impl/MailTemplateServiceImpl.java
config/MailConfig.java          → app.mail.provider'a göre bean seçimi
```
- **Seçim:** `app.mail.provider=brevo|log` (`@ConditionalOnProperty`). Varsayılan `log` → dev'de API key gerekmez.
- **Resend'e geçiş:** `impl/ResendMailSender` eklenir, `MAIL_PROVIDER=resend`. Servis kodu değişmez.
- Neden SMTP değil HTTP API: Railway'de giden SMTP portları kısıtlı olabilir; HTTP 443 her zaman açık. Yeni bağımlılık gerekmez (Spring `RestClient`).

### 2.2 Brevo entegrasyonu
- Endpoint: `POST https://api.brevo.com/v3/smtp/email`
- Header: `api-key: ${BREVO_API_KEY}`, `Content-Type: application/json`
- Gövde:
```json
{
  "sender": { "email": "${MAIL_FROM_ADDRESS}", "name": "${MAIL_FROM_NAME}" },
  "to": [{ "email": "kullanici@ornek.com", "name": "Ad Soyad" }],
  "subject": "...",
  "htmlContent": "...",
  "textContent": "...",
  "tags": ["password-reset"]
}
```
- Başarı: 201 + `messageId`. 4xx → loglanır, `MailDeliveryException` (iş akışını bozmaz, §3.4). 5xx/timeout → 1 tekrar deneme.
- Timeout: connect 3 sn, read 5 sn.
- **Kurulum (manuel, senin yapacağın):** Brevo hesabı → Senders → Gmail adresini doğrula → SMTP & API → API key oluştur → Railway env'e ekle.
- **Risk:** `@gmail.com` gönderen adresiyle üçüncü taraf sunucudan gönderim DMARC nedeniyle spam'e düşebilir veya Brevo gönderen adresini değiştirebilir. Test edilerek görülecek; kalıcı çözüm domain + Resend/Brevo domain doğrulaması.
- Limit: 300/gün free. Uygulama tarafı koruma: §3.5.

### 2.3 Yeni env değişkenleri
| Env | Örnek | Not |
|---|---|---|
| `MAIL_PROVIDER` | `log` / `brevo` | prod'da `brevo` |
| `BREVO_API_KEY` | – | gizli |
| `MAIL_FROM_ADDRESS` | senin Gmail'in | Brevo'da doğrulanmış olmalı |
| `MAIL_FROM_NAME` | `Supplog` | |
| `FRONTEND_BASE_URL` | `https://<proje>.vercel.app` | maildeki link için |

### 2.4 Şablonlar
- Basit HTML string şablonları (`src/main/resources/mail/password-reset_tr.html`, `_en.html`) + `{{placeholder}}` değiştirme. Thymeleaf eklemeye gerek yok.
- Dil: İstek anındaki `Accept-Language` (`LocaleContextHolder`). Kullanıcıda dil alanı yok (§4.9'daki karar ile birlikte değerlendirilecek).
- Kullanıcıdan gelen değerler (ad) HTML-escape edilir.

---

## 3. Şifremi Unuttum (Brevo'nun ilk kullanımı)

### 3.1 Akış
```
1. POST /api/v1/auth/forgot-password { email }
   → HER ZAMAN 202 + aynı mesaj ("Hesap varsa mail gönderildi") – kullanıcı var/yok sızdırılmaz
   → kullanıcı aktifse: önceki kullanılmamış token'ları geçersiz kıl, yeni token üret, mail gönder (async)
2. Mail: ${FRONTEND_BASE_URL}/reset-password?token=<raw>
3. Frontend sayfası: yeni şifre formu
4. POST /api/v1/auth/reset-password { token, newPassword }
   → token geçerli/kullanılmamış/süresi dolmamış
   → password = encode(new), mustChangePassword=false, tokenVersion++ (→ tüm access + refresh geçersiz, §1.3)
   → token used_at=now, kullanıcının diğer reset token'ları silinir
   → 204
5. (Opsiyonel) "Şifren değiştirildi" bilgilendirme maili
```

### 3.2 Tablo: `password_reset_tokens` (V10)
| Kolon | Tip | Not |
|---|---|---|
| id | bigserial PK | |
| user_id | bigint FK NOT NULL | index |
| token_hash | varchar(64) UNIQUE NOT NULL | SHA-256 |
| expires_at | timestamptz NOT NULL | 30 dk |
| used_at | timestamptz | |
| created_at | timestamptz NOT NULL | |
| request_ip | varchar(45) | |

### 3.3 Kurallar
- Token: 32 byte SecureRandom, base64url. Süre 30 dk (`PASSWORD_RESET_TOKEN_TTL_MINUTES`).
- Şifre doğrulaması: register / change-password ile **aynı** validation kuralları (mevcut DTO anotasyonları tekrar kullanılır).
- E-posta `InputNormalizer.normalizeEmail` ile normalize edilir.
- Deaktive (isDeleted) kullanıcıya mail gönderilmez, cevap yine 202.

### 3.4 Zamanlama sızıntısı ve hata dayanıklılığı
- Mail gönderimi **transaction commit'ten sonra ve asenkron** (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`). Böylece "kullanıcı var" durumunda cevap süresi uzamaz (enumeration önlenir) ve Brevo hatası isteği bozmaz.
- `@EnableAsync` + küçük bir `ThreadPoolTaskExecutor` (core 2, max 4, queue 100) eklenir.

### 3.5 Kötüye kullanım koruması
- Mevcut IP rate limit (20/dk) zaten `/auth/**` için var.
- Ek: aynı kullanıcı için **saatte en fazla 3** reset maili (DB'den son 1 saatteki token sayısı). Aşılırsa yine 202, mail atılmaz (log).
- Günlük toplam mail sayacı (Brevo 300/gün): log uyarısı, 280'de gönderimi durdurma opsiyonu.

### 3.6 E-posta doğrulama (karar bekliyor)
Şu an kayıtta e-posta doğrulanmıyor → başkasının adresiyle kayıt olunabilir ve reset maili ona gider. Öneri: Faz 2'de `email_verified_at` + doğrulama maili (aynı token altyapısı, `purpose` kolonu ile tek tabloya genelleştirilebilir). Bu dokümanda görev olarak opsiyonel listelendi.

### 3.7 Testler
- Var olmayan e-posta → 202, mail gönderilmez.
- Geçerli akış → şifre değişir, eski access/refresh reddedilir.
- Süresi dolmuş / kullanılmış token → 400 `PASSWORD_RESET_TOKEN_INVALID`.
- Saatte 4. istek → mail gönderilmez.
- `LoggingMailSender` ile entegrasyon testi (mail içeriğinde link var).

---

## 4. Bildirimler (Firebase FCM)

### 4.1 Bileşenler
```
service/notification/
  PushSender.java                     (interface) send(PushMessage) → PushResult
  impl/FcmPushSender.java             Firebase Admin SDK
  impl/LoggingPushSender.java         dev/test
  DeviceTokenService (+impl)          cihaz kaydı/silme
  NotificationService (+impl)         inbox kaydı + push tetikleme (tek giriş noktası)
  ReminderScheduler                   rutin hatırlatma job'u
  SupporterAlertScheduler             MISSED → destekçi bildirimi
  ReminderSettingsService (+impl)     rutin bazlı hatırlatma ayarları
controller/NotificationController     /api/v1/notifications/**
```
- Bağımlılık: `com.google.firebase:firebase-admin` (ücretsiz).
- Kimlik: Firebase service account JSON → Railway env `FIREBASE_CREDENTIALS_BASE64` (base64). Uygulama açılışta decode edip `FirebaseApp.initializeApp`. Dosya repoya **asla** girmez.
- `app.push.provider=fcm|log`, varsayılan `log`.

### 4.2 Platform durumu
| Platform | Durum |
|---|---|
| Web (Vercel) | FCM Web Push ücretsiz: frontend'de `firebase-messaging-sw.js` + VAPID key. Flutter'dan önce test edilebilir. |
| Android (Flutter) | Ücretsiz. |
| iOS (Flutter) | APNs anahtarı için ücretli Apple Developer hesabı gerekir → şimdilik kapsam dışı. |

### 4.3 Cihaz token kaydı – tablo `device_tokens` (V11)
| Kolon | Tip | Not |
|---|---|---|
| id | bigserial PK | |
| user_id | bigint FK NOT NULL | index |
| token | varchar(512) UNIQUE NOT NULL | FCM registration token |
| platform | varchar(20) NOT NULL | ANDROID / IOS / WEB |
| locale | varchar(10) | `tr`, `en` – bildirim dili |
| refresh_token_family_id | uuid | logout'ta bu oturumun cihazını bulmak için |
| created_at / last_seen_at | timestamptz | |

Endpoint'ler:
| Metot | Yol | Not |
|---|---|---|
| PUT | `/api/v1/notifications/devices` | `{ token, platform, locale }` upsert. Token başka kullanıcıdaysa bu kullanıcıya taşınır (aynı cihaz, farklı hesap). |
| DELETE | `/api/v1/notifications/devices/{token}` | sadece kendi token'ı |

Temizlik: FCM `UNREGISTERED` / `INVALID_ARGUMENT` dönerse token silinir. Logout → ilgili token silinir. Logout-all / kullanıcı deaktive → tüm token'lar silinir. 60 gündür `last_seen_at` güncellenmeyen token'lar haftalık silinir.

### 4.4 Bildirim türleri
| Tür | Alıcı | Tetikleyici | Inbox'a yazılır mı |
|---|---|---|---|
| `ROUTINE_REMINDER` | rutin sahibi | ReminderScheduler | **Hayır** (günde ~9 adet olabilir; gürültü). Sadece push + log tablosu. |
| `ROUTINE_DUE` | rutin sahibi | T-0 anı | Hayır |
| `ROUTINE_MISSED` | rutin sahibi (opsiyonel) | MISSED geçişi | Evet |
| `SUPPORTED_ROUTINE_MISSED` | destekçiler | MISSED geçişi + izin | Evet |
| `NUDGE` | rutin sahibi | supporter gönderir | Evet |
| `SUPPORT_REQUEST_RECEIVED` / `ACCEPTED` | ilgili taraf | SupportService | Evet (Faz 2) |

### 4.5 Rutin hatırlatma kuralları (senin kararların)
- **Varsayılan (kullanıcı ayar yapmadıysa):** rutin saatinden önce **T-30, T-20, T-10** (10 dk arayla), son 5 dakikada **T-5, T-4, T-3, T-2, T-1** (her dakika), **T-0** "zamanı geldi". Toplam 9 bildirim.
- **Kullanıcı ayarı:** Rutin bazında hatırlatma kapatılabilir veya özel başlangıç süresi seçilebilir (ör. 60 dk önce başla). Özel süre seçildiğinde ritim aynı kalır: başlangıçtan T-10'a kadar 10 dk arayla, son 5 dk her dakika.
- **Durdurma:** Execution `PENDING` değilse (COMPLETED / SKIPPED / MISSED) hiçbir hatırlatma gönderilmez → job zaten sadece PENDING'leri sorgular. Kullanıcı T-20'de "yaptım" derse T-10 ve sonrası gitmez.
- **Kapsam dışı:** `Frequency.AS_NEEDED` rutinler (execution üretilmiyor) ve `routineTime` null olan rutinler.

Tablo `routine_reminder_settings` (V11):
| Kolon | Tip | Not |
|---|---|---|
| routine_id | bigint PK FK → routines | 1:1, satır yoksa varsayılan |
| enabled | boolean NOT NULL default true | |
| lead_minutes | int NOT NULL default 30 | izinli değerler: 5, 10, 15, 30, 45, 60, 90, 120 |
| updated_at | timestamptz | |

Endpoint: `GET/PUT /api/v1/routines/{routineId}/reminder-settings` (sadece rutin sahibi; mevcut `RoutineController` altına).

> **Not – "kullanıcıya sorup":** Push izni istemi (OS izin penceresi) frontend/mobil tarafında, rutin oluşturulurken gösterilir. Backend tarafı: izin verilirse device token kaydedilir, ayar ekranında `lead_minutes` seçilir.

### 4.6 ReminderScheduler algoritması
```
@Scheduled(fixedDelay = 60_000)  // app.notification.reminder-interval-ms
now = Instant.now()
windowStart = now - 2 dk   (gecikmeli tick toleransı)
adaylar = SELECT e FROM RoutineExecution e
          JOIN routine r (aktif, silinmemiş) LEFT JOIN reminder_settings s
          WHERE e.status = PENDING
            AND e.scheduledAt BETWEEN now AND now + 121 dk   (max lead)
            AND (s IS NULL OR s.enabled)
her aday için:
   offsets = offsetsFor(leadMinutes)        // 30 → [30,20,10,5,4,3,2,1,0]
   due = offsets.filter(o -> e.scheduledAt - o dk ∈ (windowStart, now])
   her due offset için:
      INSERT INTO routine_reminder_logs(execution_id, offset_minutes, ...) ON CONFLICT DO NOTHING
      eklendiyse → push gönder (transaction dışında), sonucu log'a yaz
```
- **Çift gönderim koruması:** `routine_reminder_logs` UNIQUE(execution_id, offset_minutes). Tick kayması veya restart'ta aynı bildirim iki kez gitmez.
- **Kaçan bildirimler:** Uygulama 2 dk'dan uzun kapalı kaldıysa eski offset'ler atlanır (eski hatırlatma göndermek anlamsız).
- `offsetsFor(lead)`: `lead, lead-10, …` (>5 olanlar) + `5,4,3,2,1,0`.
- Execution'ın varlığı: Lifecycle job (15 dk) bugünün execution'larını üretiyor; rutin güncellemelerinde `synchronizePendingAfterRoutineUpdate` çalışıyor. **Kontrol edilecek risk:** Gün içinde yeni oluşturulan ve 30 dk içinde saati gelen rutinin execution'ı hemen oluşuyor mu? Oluşmuyorsa `RoutineService.create` sonunda bugünkü execution senkronizasyonu tetiklenmeli (görev NT-14).
- Push gönderimi kullanıcı başına gruplanmaz; aynı dakikada çok rutin varsa ayrı bildirimler gider (Faz 2: birleştirme).

Tablo `routine_reminder_logs` (V11):
| Kolon | Tip |
|---|---|
| id | bigserial PK |
| execution_id | bigint FK NOT NULL |
| offset_minutes | int NOT NULL |
| sent_at | timestamptz NOT NULL |
| success_count / failure_count | int |
| UNIQUE(execution_id, offset_minutes) | |

Temizlik: 14 günden eski loglar günlük silinir.

### 4.7 Destekçi bildirimleri ve görünürlük
**Görünürlük (senin kararın):** Destekçi, izin verilen tüm rutinleri görür (kaçırılan, zamanı gelen, gelecek).
- Mevcut: `GET /support/connections/{relationshipId}/routines` → `SupportRoutineResponseDto` sadece rutin tanımı döndürüyor, **execution durumu yok** (kodda doğrulandı).
- Yeni: `GET /api/v1/support/connections/{relationshipId}/executions?from=&to=` → izinli rutinlerin execution'ları (`RoutineExecutionResponseDto` + rutin/supplement adı). Varsayılan aralık: bugün. "Gelecek" için: bugünkü PENDING'ler + rutin tanımından hesaplanan sonraki tarih (`RoutineScheduleMatcher` ile, max 7 gün) → `upcoming` listesi.
- Yetki: `getAcceptedRelationshipForSupporter` + `ALL_ROUTINES` ise tüm aktif rutinler, `SELECTED_ROUTINES` ise `SupportRoutinePermission` (mevcut `getAllSharedRoutines` / `getSelectedSharedRoutines` yeniden kullanılır).

**MISSED bildirimi:**
- Mevcut `markExpiredPendingAsMissed` toplu `@Modifying` UPDATE → satır bazlı hook yok.
- Çözüm: `routine_executions.missed_notified_at` kolonu (V11). `SupporterAlertScheduler` (1 dk) → `status = MISSED AND missed_notified_at IS NULL AND missed_at > now - 1 saat` satırları alır, `missed_notified_at = now` set eder (FOR UPDATE SKIP LOCKED), sonra:
  - Rutin sahibine `ROUTINE_MISSED` (tercih açıksa),
  - Rutini görebilen her ACCEPTED destekçiye `SUPPORTED_ROUTINE_MISSED`.
- 1 saatten eski MISSED'ler (ilk deploy, uzun kesinti) sadece işaretlenir, bildirim atılmaz.
- Destekçiye ayrıca "zamanı geldi" bildirimi: **varsayılan kapalı** (gürültü), tercih ile açılabilir (Faz 2).

### 4.8 Inbox – tablo `notifications` (V11)
| Kolon | Tip | Not |
|---|---|---|
| id | bigserial PK | |
| recipient_user_id | bigint FK NOT NULL | index (recipient, created_at desc) |
| type | varchar(40) NOT NULL | §4.4 |
| title / body | varchar(150) / varchar(500) | gönderim anındaki dilde render edilmiş metin |
| actor_user_id | bigint FK | nudge gönderen, destekçi vb. |
| routine_id / execution_id / nudge_id | bigint | derin link için |
| data | jsonb | ek veri |
| read_at | timestamptz | |
| created_at | timestamptz NOT NULL | |

Endpoint'ler:
| Metot | Yol |
|---|---|
| GET | `/api/v1/notifications?unreadOnly=&page=` (sayfalı) |
| GET | `/api/v1/notifications/unread-count` |
| PATCH | `/api/v1/notifications/{id}/read` |
| PATCH | `/api/v1/notifications/read-all` |

Saklama: 90 gün (günlük temizlik).

### 4.9 Bildirim tercihleri – tablo `notification_preferences` (V11)
`user_id PK, routine_reminders_enabled, own_missed_enabled, supporter_missed_enabled, nudges_enabled, quiet_hours_start, quiet_hours_end (nullable), updated_at`. Satır yoksa varsayılanlar (hepsi açık, sessiz saat yok).
Endpoint: `GET/PUT /api/v1/notifications/preferences`.
Sessiz saatler: Faz 2 (kullanıcının timeZone'una göre; hatırlatmalar sessiz saatte de gönderilsin mi kararı gerekir).

### 4.10 FCM mesaj formatı
```json
{
  "token": "<device token>",
  "notification": { "title": "Vitamin D zamanı", "body": "10 dk sonra: 1000 IU" },
  "data": {
    "type": "ROUTINE_REMINDER",
    "routineId": "12", "executionId": "345",
    "scheduledAt": "2026-10-19T09:00:00Z", "offsetMinutes": "10"
  },
  "android": { "priority": "high", "notification": { "channel_id": "routine_reminders", "tag": "exec-345" } },
  "webpush": { "headers": { "Urgency": "high" } }
}
```
- `tag = exec-<id>`: Android'de aynı execution'ın yeni hatırlatması eskisinin **yerine** geçer → bildirim çekmecesinde 9 ayrı bildirim birikmez.
- Metinler `messages*.properties`'ten, cihazın `locale`'ine göre render edilir.
- Gönderim: kullanıcının tüm cihazlarına `sendEachAsync` / `sendEach` (500'lük partiler). Hata türüne göre token temizliği.

### 4.11 Yeni env değişkenleri
| Env | Varsayılan |
|---|---|
| `PUSH_PROVIDER` | `log` |
| `FIREBASE_CREDENTIALS_BASE64` | – (gizli) |
| `NOTIFICATION_REMINDER_INTERVAL_MS` | `60000` |
| `NOTIFICATION_SUPPORTER_ALERT_INTERVAL_MS` | `60000` |

### 4.12 Testler
- `offsetsFor(30)` = [30,20,10,5,4,3,2,1,0]; `offsetsFor(60)`, `offsetsFor(5)`.
- Scheduler: PENDING → gönderilir; aynı tick iki kez → tek log; COMPLETED → gönderilmez.
- Ayar `enabled=false` → gönderilmez.
- MISSED → destekçi (ALL_ROUTINES) bildirim alır; SELECTED_ROUTINES dışındaki rutin için almaz; REJECTED/CANCELLED ilişki almaz.
- FCM `UNREGISTERED` → token silinir (`PushSender` mock).

---

## 5. Nudge (Supporter → Owner, tek yönlü)

### 5.1 Kurallar
- Gönderen: rutini görebilen **ACCEPTED** destekçi. Alıcı: rutin sahibi (supported user). Owner cevap veremez (tek yön).
- Nudge bir **rutine bağlı** olabilir (izinli rutin) veya genel (sadece ilişkiye bağlı). Öneri: `routineId` opsiyonel.
- İçerik iki yoldan biri (ikisi birden değil):
  1. **Hazır şablon** (`templateId`) → metin şablondan alınır.
  2. **Serbest metin** (`message`, 1–280 karakter, trim, kontrol karakterleri temizlenir).
- **Kişisel şablon kaydetme:** Destekçi serbest metni "şablon olarak kaydet" ile kendi listesine ekleyebilir; sonra tekrar seçebilir. (Senin "sisteme kaydedebilir" ifaden bu şekilde yorumlandı – §9'da onay bekliyor.)
- Gönderilen her nudge **kalıcı olarak kaydedilir** (metin snapshot'ı; şablon sonradan değişse de geçmiş bozulmaz).

### 5.2 Hazır şablonlar (sistem)
Migration ile seed edilir, `message_key` ile i18n:
| key | TR | EN |
|---|---|---|
| `nudge.template.reminder` | Rutinini unutma! 💊 | Don't forget your routine! 💊 |
| `nudge.template.proud` | Seninle gurur duyuyorum, böyle devam! | Proud of you, keep it up! |
| `nudge.template.missed` | Bugünkü rutinini kaçırdın, şimdi yapabilir misin? | You missed today's routine, can you do it now? |
| `nudge.template.water` | Bir bardak su ile almayı unutma. | Remember to take it with a glass of water. |
| `nudge.template.checkin` | Nasılsın? Rutinlerin yolunda mı? | How are you? Are your routines on track? |

### 5.3 Tablolar (V12)
**`nudge_templates`**
| Kolon | Tip | Not |
|---|---|---|
| id | bigserial PK | |
| owner_user_id | bigint FK NULL | NULL = sistem şablonu, dolu = destekçinin kişisel şablonu |
| message_key | varchar(80) | sistem şablonu için i18n key |
| text | varchar(280) | kişisel şablon metni |
| active | boolean NOT NULL default true | |
| sort_order | int | |
| created_at | timestamptz | |
| CHECK: (owner_user_id IS NULL AND message_key IS NOT NULL) OR (owner_user_id IS NOT NULL AND text IS NOT NULL) | | |

Kişisel şablon limiti: kullanıcı başına 20.

**`nudges`**
| Kolon | Tip | Not |
|---|---|---|
| id | bigserial PK | |
| support_relationship_id | bigint FK NOT NULL | |
| sender_user_id | bigint FK NOT NULL | supporter |
| recipient_user_id | bigint FK NOT NULL | owner |
| routine_id | bigint FK NULL | izinli rutin |
| template_id | bigint FK NULL | kullanıldıysa |
| message | varchar(280) NOT NULL | gönderim anındaki metin (snapshot) |
| created_at | timestamptz NOT NULL | |
| read_at | timestamptz | |
Indexler: `(recipient_user_id, created_at desc)`, `(sender_user_id, created_at desc)`, `(support_relationship_id, created_at)`.

### 5.4 Endpoint'ler
| Metot | Yol | Kim |
|---|---|---|
| GET | `/api/v1/support/nudge-templates` | destekçi – sistem + kendi şablonları |
| POST | `/api/v1/support/nudge-templates` `{ text }` | destekçi – kişisel şablon ekle |
| DELETE | `/api/v1/support/nudge-templates/{id}` | sadece kendi kişisel şablonu (soft: active=false) |
| POST | `/api/v1/support/connections/{relationshipId}/nudges` `{ templateId? , message?, routineId?, saveAsTemplate? }` | destekçi |
| GET | `/api/v1/support/connections/{relationshipId}/nudges?page=` | ilişkinin iki tarafı (geçmiş) |
| GET | `/api/v1/nudges/received?page=` | owner – gelen nudge'lar |
| PATCH | `/api/v1/nudges/{id}/read` | owner |

`/api/v1/support/**` altındakiler mevcut support rate limit'ine (120/dk/IP) otomatik girer.

### 5.5 Doğrulama sırası (`NudgeService.send`)
```
1. relationship = getAcceptedRelationshipForSupporter(relationshipId, currentUser)   (mevcut yardımcı)
2. ensureActiveParticipants(relationship)                                           (mevcut)
3. templateId XOR message (ikisi de/hiçbiri → 400 NUDGE_CONTENT_INVALID)
4. templateId → sistem şablonu veya currentUser'ın aktif kişisel şablonu olmalı
5. routineId → destekçinin görebildiği rutinler içinde olmalı (ALL vs SELECTED)
6. alıcının nudges_enabled tercihi kapalıysa → 409 NUDGE_DISABLED_BY_RECIPIENT (veya sessizce kaydet; karar §9)
7. spam limitleri:
     - ilişki başına 10 dk'da 1 (aynı rutin için)
     - ilişki başına saatte 5, günde 20
   aşılırsa 429 NUDGE_RATE_LIMITED
8. kaydet (message snapshot), saveAsTemplate=true ise kişisel şablon ekle
9. AFTER_COMMIT: NotificationService.notify(owner, NUDGE, ...) → inbox + push
```

### 5.6 İlişki sonlandığında
- Relationship revoke/cancel → yeni nudge gönderilemez (1. adım ACCEPTED kontrolü). Geçmiş nudge'lar kalır, owner görmeye devam eder.
- `SupportService.handleUserDeactivation` zinciri: nudge kaydı silinmez.
- Admin: `AdminSupportService`'e "ilişkinin nudge geçmişi" görüntüleme (Faz 2).

### 5.7 Testler
- Şablonla ve serbest metinle gönderim; ikisi birden → 400.
- Başkasının kişisel şablonu → 404.
- SELECTED_ROUTINES dışında rutin → 403/404.
- PENDING/REVOKED ilişki → reddedilir.
- Rate limit sınırları.
- Owner gelen kutusu + okundu işareti; owner nudge gönderemez (ters yön) → reddedilir.

---

## 6. Ortak altyapı kararları
- **Async:** `@EnableAsync` + `notificationExecutor` (mail ve push ortak, core 2 / max 4 / queue 500). Kuyruk dolarsa log + drop (hatırlatmalar için kabul edilebilir).
- **AFTER_COMMIT event'leri:** `PasswordResetRequestedEvent`, `NudgeSentEvent`, `RoutineMissedEvent` → dinleyiciler mail/push gönderir. Rollback'te bildirim gitmez.
- **Scheduler havuzu:** Varsayılan Spring scheduler tek thread → mevcut 2 job + yeni 3-4 job birbirini bekler. `spring.task.scheduling.pool.size=4` ayarlanır.
- **Zaman:** Tüm yeni kolonlar `timestamptz` / `Instant` (mevcut `scheduledAt`/`missedAt` ile uyumlu).
- **Çoklu instance:** Şimdilik tek instance. İleride ShedLock (ücretsiz, Postgres tabanlı) eklenebilir; unique log tabloları çift gönderimi zaten engeller.
- **Gözlemlenebilirlik:** Her job sonunda tek satır özet log (`reminders sent=…, failed=…`).

---

## 7. Migration planı
| Ver. | İçerik |
|---|---|
| V9 | `refresh_tokens` |
| V10 | `password_reset_tokens` |
| V11 | `device_tokens`, `notifications`, `notification_preferences`, `routine_reminder_settings`, `routine_reminder_logs`, `routine_executions.missed_notified_at` (+ mevcut MISSED satırları için `missed_notified_at = now()` backfill → deploy'da eski kaçırmalar için bildirim yağmuru olmaz) |
| V12 | `nudge_templates` (+ sistem şablonları seed), `nudges` |
| V13 | (opsiyonel) `users.email_verified_at`, token tablosuna `purpose` |

---

## 8. Görev listesi (küçük, bağımsız parçalar)

> Format: **ID – görev** · dokunulan yer · bitti kriteri. Sıra önerilen uygulama sırasıdır. Her görev ayrı commit olabilir.

### Faz 1A – Refresh token + Logout (RT)
- [x] **RT-01** – V9 migration: `refresh_tokens` tablosu + indexler · `db/migration/V9__add_refresh_tokens.sql` · uygulama açılıyor, Flyway başarılı.
- [x] **RT-02** – `RefreshToken` entity + `RefreshTokenRevokeReason` enum · `entity/`, `enums/` · `ddl-auto=validate` geçiyor.
- [x] **RT-03** – `RefreshTokenRepository`: `findByTokenHashForUpdate`, `revokeFamily`, `revokeAllByUser`, `deleteExpired` · `repository/` · derleniyor.
- [x] **RT-04** – `TokenHashing` util (SecureRandom 32 byte base64url üret + SHA-256 hex) · `util/` · birim testi.
- [x] **RT-05** – Config: `app.jwt.refresh-expiration-days` (30), access varsayılanı 15 dk; `.env.example` güncelle · properties.
- [x] **RT-06** – `RefreshTokenService.issue(user, deviceInfo)` (yeni aile) · `service/auth/` · birim testi.
- [x] **RT-07** – `RefreshTokenService.rotate(raw)` – §1.6 algoritması (kilit, reuse, 10 sn tolerans, tokenVersion snapshot) · birim testleri her dal için.
- [x] **RT-08** – `RefreshTokenService.revoke(raw)` ve `revokeAllForUser(userId, reason)`.
- [x] **RT-09** – `AuthResponseDto`'ya `refreshToken`, `accessTokenExpiresAt`, `refreshTokenExpiresAt` ekle; `JwtService` bitiş zamanını döndürsün.
- [x] **RT-10** – `LoginRequestDto`'ya opsiyonel `deviceName`, `platform`; `AuthServiceImpl.login/register` refresh üretsin.
- [x] **RT-11** – `RefreshTokenRequestDto`, `LogoutRequestDto` · `dto/login/`.
- [x] **RT-12** – `POST /auth/refresh` endpoint · `AuthController`.
- [x] **RT-13** – `POST /auth/logout` (idempotent 204) · `AuthController`.
- [x] **RT-14** – `POST /users/me/logout-all` (tokenVersion++ + revokeAll) · `UserController`/`UserServiceImpl`.
- [x] **RT-15** – Yeni hata kodları + `messages*.properties` anahtarları (§1.7); `TOKEN_EXPIRED` kodunun filtrede net döndüğünü doğrula.
- [x] **RT-16** – Refresh temizlik job'u (günlük) · `RefreshTokenServiceImpl`.
- [x] **RT-17** – Entegrasyon testi: login → refresh → reuse → aile revoke. _(RefreshTokenPostgresIntegrationTest, 2026-10-19 Postgres üzerinde geçti)_
- [x] **RT-18** – Entegrasyon testi: şifre değişimi / admin deaktive sonrası refresh reddi. _(RefreshTokenPostgresIntegrationTest, 2026-10-19 Postgres üzerinde geçti)_
- [x] **RT-19** – Postgres eşzamanlılık testi: paralel iki refresh. _(RefreshTokenPostgresIntegrationTest, 2026-10-19 Postgres üzerinde geçti)_
- [x] **RT-20** – Dokümantasyon: README auth bölümü + PROJECT_MAP §4/§5/§8 güncelle; frontend için "tek in-flight refresh" notu.
- [ ] **RT-21** *(Faz 2)* – `GET /users/me/sessions`, `DELETE /users/me/sessions/{familyId}`.

#### Faz 1A-2 – httpOnly cookie (§1.9)
- [x] **RT-22** – Cookie/istemci config: `app.auth.refresh-cookie.*` (name, path, secure, same-site), `app.auth.client-type-header`; dev profilinde secure=false; `.env.example`.
- [x] **RT-23** – `RefreshTokenCookieService`: istemci modu tespiti, cookie okuma/yazma/silme (`ResponseCookie`).
- [x] **RT-24** – `RequestOriginValidator`: cookie modunda `Origin` ∈ allowed-origins, değilse 403 `ORIGIN_NOT_ALLOWED`.
- [x] **RT-25** – login/register: web → `Set-Cookie` + body'de refreshToken null; mobil → body.
- [x] **RT-26** – refresh: web → cookie + Origin kontrolü; mobil → body. `RefreshTokenRequestDto` opsiyonel gövde.
- [x] **RT-27** – logout (cookie/body + cookie silme), logout-all cookie silme.
- [x] **RT-28** – CORS `allowCredentials=true`.
- [x] **RT-29** – Rate limit `trusted-proxy-hops` (X-Forwarded-For sağdan N).
- [x] **RT-30** – Testler: cookie servis, origin validator, client IP çözümü, AuthController web/mobil akışı.
- [x] **RT-31** – README + PROJECT_MAP güncelle.
- [x] **FE-01** *(supplog-frontEnd)* – `vercel.json` rewrite (`/api/:path*` → Railway).
- [x] **FE-02** *(supplog-frontEnd)* – Vite dev proxy `/api` → `http://localhost:8080`.
- [x] **FE-03** *(supplog-frontEnd)* – API base URL relative `/api/v1`, `fetch(..., { credentials: 'include' })` / axios `withCredentials: true`; localStorage'da token tutulmaz, access token bellekte.
- [x] **FE-04** *(supplog-frontEnd)* – Sayfa yenilenince önce `POST /api/v1/auth/refresh` (cookie ile) → access token al; tek in-flight refresh; `REFRESH_TOKEN_*`/`TOKEN_REVOKED` → login.
- [ ] **OPS-01** *(Railway + Vercel – sen)* – Railway: `CORS_ALLOWED_ORIGINS` içinde `https://supplog.vercel.app`, `TRUSTED_PROXY_HOPS=2`. Vercel: `VITE_API_BASE_URL=/api/v1` (yeniden build); deploy sonrası rate limit log'unda IP'nin doğru çözüldüğünü kontrol et.

### Faz 1B – Mail altyapısı (ML)
- [ ] **ML-01** – `MailSender` interface + `MailMessage` record · `service/mail/`.
- [ ] **ML-02** – `LoggingMailSender` (varsayılan) · `service/mail/impl/`.
- [ ] **ML-03** – `MailProperties` (`app.mail.*`) + `MailConfig` (`@ConditionalOnProperty` ile bean seçimi).
- [ ] **ML-04** – `BrevoMailSender` (RestClient, timeout, 5xx'te 1 retry, 4xx log) · birim testi (MockRestServiceServer).
- [ ] **ML-05** – `@EnableAsync` + `notificationExecutor` bean · `config/AsyncConfig`.
- [ ] **ML-06** – Basit şablon motoru (`{{key}}` değiştirme + HTML escape) + `MailTemplateService`.
- [ ] **ML-07** – Env değişkenleri: `MAIL_PROVIDER`, `BREVO_API_KEY`, `MAIL_FROM_*`, `FRONTEND_BASE_URL` · properties + `.env.example`.
- [ ] **ML-08** – *(Manuel – sen)* Brevo hesabı, Gmail gönderici doğrulama, API key, Railway env.
- [ ] **ML-09** – Prod'da tek test maili (geçici admin-only endpoint veya başlangıç log'u ile) → spam klasörü kontrolü; sonra kaldır.

### Faz 1C – Şifremi unuttum (PR)
- [ ] **PR-01** – V10 migration `password_reset_tokens`.
- [ ] **PR-02** – `PasswordResetToken` entity + repository (`countByUserIdAndCreatedAtAfter`, `deleteAllByUserId`).
- [ ] **PR-03** – `ForgotPasswordRequestDto`, `ResetPasswordRequestDto` (şifre validasyonu mevcut kurallarla aynı).
- [ ] **PR-04** – `PasswordResetService.request(email)` – her zaman sessiz, saatlik 3 limit, event yayınla.
- [ ] **PR-05** – `PasswordResetRequestedEvent` + AFTER_COMMIT async dinleyici → mail.
- [ ] **PR-06** – Şablonlar `mail/password-reset_tr.html`, `_en.html` + text versiyonu.
- [ ] **PR-07** – `PasswordResetService.reset(token, newPassword)` – tokenVersion++, mustChangePassword=false, token used.
- [ ] **PR-08** – Endpoint'ler `POST /auth/forgot-password` (202), `POST /auth/reset-password` (204).
- [ ] **PR-09** – Hata kodu `PASSWORD_RESET_TOKEN_INVALID` + messages.
- [ ] **PR-10** – Temizlik: süresi dolmuş/kullanılmış token'ları günlük sil.
- [ ] **PR-11** – Testler (§3.7).
- [ ] **PR-12** – *(Frontend)* `/forgot-password` ve `/reset-password` sayfaları (Vercel).
- [ ] **PR-13** *(opsiyonel)* – "Şifren değiştirildi" bilgilendirme maili.
- [ ] **PR-14** *(Faz 2, karar bekliyor)* – E-posta doğrulama (V13).

### Faz 2A – Bildirim temeli (NT)
- [ ] **NT-01** – V11 migration (tüm bildirim tabloları + `missed_notified_at` + backfill).
- [ ] **NT-02** – Entity'ler: `DeviceToken`, `Notification`, `NotificationPreference`, `RoutineReminderSetting`, `RoutineReminderLog` + `NotificationType`, `DevicePlatform` enum'ları.
- [ ] **NT-03** – Repository'ler.
- [ ] **NT-04** – `PushSender` interface + `PushMessage`/`PushResult` + `LoggingPushSender`.
- [ ] **NT-05** – `firebase-admin` bağımlılığı + `FirebaseConfig` (base64 credential, provider=fcm iken).
- [ ] **NT-06** – `FcmPushSender` (çoklu cihaz, hata → token temizliği, android tag, webpush).
- [ ] **NT-07** – `DeviceTokenService` + `PUT/DELETE /notifications/devices`.
- [ ] **NT-08** – Logout / logout-all / kullanıcı deaktive → device token silme entegrasyonu (RT-13, RT-14, `handleUserDeactivation`).
- [ ] **NT-09** – `NotificationService.notify(...)` (inbox kaydı + tercih kontrolü + async push).
- [ ] **NT-10** – Inbox endpoint'leri (liste, unread-count, read, read-all).
- [ ] **NT-11** – Tercihler endpoint'i `GET/PUT /notifications/preferences`.
- [ ] **NT-12** – i18n bildirim metinleri (`notification.*` anahtarları, tr/en).
- [ ] **NT-13** – `spring.task.scheduling.pool.size=4`.
- [ ] **NT-14** – Doğrula: gün içinde oluşturulan rutinin bugünkü execution'ı hemen oluşuyor mu; oluşmuyorsa `RoutineService.create` sonunda senkronize et.
- [ ] **NT-15** – *(Manuel – sen)* Firebase projesi, service account JSON → base64 → Railway env; Web için VAPID key.

### Faz 2B – Rutin hatırlatma (RM)
- [ ] **RM-01** – `ReminderOffsets.offsetsFor(lead)` saf fonksiyon + birim testleri.
- [ ] **RM-02** – Repository sorgusu: zaman penceresindeki PENDING execution'lar + ayarlar (tek sorgu, N+1 yok).
- [ ] **RM-03** – `ReminderScheduler` job (1 dk) – due offset hesaplama.
- [ ] **RM-04** – Reminder log insert (`ON CONFLICT DO NOTHING`) ile idempotency.
- [ ] **RM-05** – Push içeriği (rutin/supplement adı, dozaj, "X dk sonra" / "şimdi") + android tag `exec-<id>`.
- [ ] **RM-06** – `GET/PUT /routines/{id}/reminder-settings` + DTO + izinli `lead_minutes` doğrulaması.
- [ ] **RM-07** – Reminder log temizlik job'u (14 gün).
- [ ] **RM-08** – Testler: gönderim, tekrar yok, COMPLETED'da durma, enabled=false.

### Faz 2C – Destekçi görünürlüğü ve bildirimi (SP)
- [ ] **SP-01** – `GET /support/connections/{id}/executions?from=&to=` (izinli rutinler, bugün varsayılan).
- [ ] **SP-02** – "Gelecek" hesaplaması (`RoutineScheduleMatcher` ile sonraki 7 gün) → `upcoming` alanı.
- [ ] **SP-03** – Repository: rutini görebilen ACCEPTED destekçi id'leri (ALL + SELECTED birleşik sorgu).
- [ ] **SP-04** – `SupporterAlertScheduler` (MISSED + `missed_notified_at IS NULL`, SKIP LOCKED, 1 saat sınırı).
- [ ] **SP-05** – Sahibe `ROUTINE_MISSED`, destekçilere `SUPPORTED_ROUTINE_MISSED` bildirimleri.
- [ ] **SP-06** – Testler (§4.12 destekçi maddeleri).

### Faz 3 – Nudge (NG)
- [ ] **NG-01** – V12 migration: `nudge_templates` (+ 5 sistem şablonu seed), `nudges`.
- [ ] **NG-02** – Entity'ler + repository'ler.
- [ ] **NG-03** – Şablon metinleri `messages*.properties` (`nudge.template.*`).
- [ ] **NG-04** – `GET /support/nudge-templates` (sistem + kişisel, locale'e göre render).
- [ ] **NG-05** – `POST/DELETE /support/nudge-templates` (kişisel, limit 20).
- [ ] **NG-06** – `NudgeService.send` doğrulama zinciri (§5.5 adım 1–5).
- [ ] **NG-07** – Nudge spam limitleri (§5.5 adım 7) + `NUDGE_RATE_LIMITED`.
- [ ] **NG-08** – `saveAsTemplate` desteği.
- [ ] **NG-09** – `NudgeSentEvent` → `NotificationService` (inbox + push).
- [ ] **NG-10** – Geçmiş: `GET /support/connections/{id}/nudges`, `GET /nudges/received`, `PATCH /nudges/{id}/read`.
- [ ] **NG-11** – Hata kodları + messages (`NUDGE_CONTENT_INVALID`, `NUDGE_RATE_LIMITED`, `NUDGE_DISABLED_BY_RECIPIENT`).
- [ ] **NG-12** – Testler (§5.7).

### Faz 4 – Sonra
- [ ] Flutter: FCM entegrasyonu, `flutter_secure_storage`, refresh interceptor, push izin akışı.
- [ ] iOS push (Apple Developer hesabı gerektiğinde).
- [ ] Domain + Resend (`ResendMailSender`), httpOnly cookie değerlendirmesi.
- [ ] ShedLock (çoklu instance olursa), sessiz saatler, bildirim birleştirme.

---

## 9. Onay bekleyen kararlar
| # | Konu | Öneri |
|---|---|---|
| K1 | Refresh ömrü: mutlak 30 gün mü, her kullanımda kayan 30 gün mü? | **Kayan** (her rotation +30 gün, aktif kullanıcı hiç logout olmaz) + mutlak üst sınır 90 gün |
| K2 | Access token süresi | 15 dk |
| K3 | Nudge "sisteme kaydet" yorumu: destekçinin kişisel şablonu olarak saklanması doğru mu? | Evet (§5.1) |
| K4 | Owner nudge'ları kapatırsa: hata mı, sessizce kayıt mı? | Hata (409) – destekçi bilsin |
| K5 | T-0 ("zamanı geldi") bildirimi olsun mu? | Evet |
| K6 | Rutin sahibine kendi MISSED bildirimi | Evet (tercihle kapatılabilir) |
| K7 | Destekçiye "zamanı geldi" bildirimi | Varsayılan kapalı |
| K8 | E-posta doğrulama | Faz 2'de (PR-14) |
| K9 | Nudge limitleri | 10 dk/rutin, 5/saat, 20/gün |
| K10 | `lead_minutes` seçenekleri | 5,10,15,30,45,60,90,120 |
