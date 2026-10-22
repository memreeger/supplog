# Supplog – Proje Haritası (sorun giderme referansı)

> Amaç: Bir sorun çıktığında önce bu dosyaya bak. Burada çözüm yoksa kodu incele ve bu dosyayı güncelle.
> Doğrulama notu: ✅ = kodda doğrulandı, ⚠️ = isimden/yapıdan çıkarıldı, koda tam bakılmadı.
> Son inceleme: 2026-10-19 (commit f616f14).

## 1. Stack
Java 21, Spring Boot, Spring Security + JWT (jjwt, HS), JPA/Hibernate (`ddl-auto=validate`), PostgreSQL 18,
Flyway (V1–V8), ModelMapper, springdoc Swagger (prod'da kapalı), Actuator, Docker (multi-stage) + compose.
Paket kökü: `com.supplog`. Maven wrapper (`mvnw`).

## 2. Dizin yapısı (src/main/java/com/supplog)
| Paket | İçerik |
|---|---|
| `config` | AppConfig, CorsConfig, JpaAuditConfiguration, LanguageConfig, PaginationConfiguration, PasswordConfig, SecurityConfiguration, SwaggerConfiguration, `initializer/DataInitializer` (env ile admin seed), `json/UniqueDayOfWeekSetDeserializer` |
| `controller` | Auth, User, Supplement, Routine, RoutineExecution, Support + `admin/` (Audit, Dashboard, Execution, Routine, Supplement, Support, User) |
| `dto` | Alan bazlı alt paketler: login, user, supplement, routine, routineExecution, support, admin/* |
| `entity` | User, Role, Supplement, Routine, RoutineExecution, SupportRelationship, SupportRoutinePermission, AdminAuditLog |
| `enums` | DayOfWeek, DurationType, Frequency, MissedGracePeriod, RoleName, RoutineCategory, RoutineExecutionStatus, SupportAccessScope, SupportStatus |
| `exception` | BusinessException, ResourceNotFoundException, GlobalExceptionHandler, ErrorResponse, MessageHelper (i18n) |
| `filter` | AuthRateLimitFilter, JwtAuthenticationFilter |
| `repository` | Entity başına 1 Spring Data repo |
| `security` | CustomAccessDeniedHandler, CustomAuthenticationEntryPoint |
| `service` | `<alan>/Service` + `impl/ServiceImpl`; yardımcılar: RoutineValidator, RoutineScheduleMatcher, ActiveUserService, JwtService, CustomUserDetails(+Service) |
| `util` | InputNormalizer, TimeZoneResolver |

Resources: `application.properties` (+ `-dev`, `-prod`), `messages.properties` / `messages_en.properties`, `db/migration/V1..V8`.
Test: `src/test` (bkz. §9). Dökümanlar: `README.md`, `docs/postgres-backup-restore.md`.

## 3. Veri modeli ✅
```
Role ◄─M:N user_roles (uk user_id+role_id)─► User
User 1─N Supplement   (supplements.inserted_by_user_id)
User 1─N Routine      (routines.user_id)
Supplement 1─N Routine (routines.supplement_id)
Routine 1─N RoutineExecution (uk_routine_execution_routine_date: routine_id + scheduled_date)
Routine ─ daysOfWeek (ElementCollection, EnumType.STRING, Set<DayOfWeek>)
User(supportedUser) ─┐
User(supporter)     ─┴─ SupportRelationship (uk_support_relationship_users)
SupportRelationship 1─N SupportRoutinePermission N─1 Routine (uk relationship+routine)
AdminAuditLog: bağımsız; admin_user_id düz kolon (FK YOK)
```
Alanlar (önemliler):
- **User**: email(unique,254), username(unique,30), birthDate, password, firstName/lastName, score(int), isDeleted (soft), tokenVersion (JWT iptali), mustChangePassword, timeZone, createdAt/updatedAt.
- **Supplement**: name, suppDosage, expireDate(nullable, V6), type(RoutineCategory → kolon `category`), isDeleted, insertedByUser.
- **Routine**: frequency, durationType, startDate, endDate, routineTime, dayOfMonth, missedGracePeriod(default 30 dk, V7), isDeleted, user, supplement.
- **RoutineExecution**: scheduledDate/Time/ZoneId/At, status, resolvedAt, missedAt(not null), routine.
- **SupportRelationship**: status, accessScope, requestedAt/respondedAt/revokedAt.
- **AdminAuditLog**: action, resourceType/Id, oldValue/newValue(text), reason(not null, 500), successful, errorMessage.

Enum'lar ✅: Frequency{DAILY,WEEKLY,MONTHLY,SPECIFIC_DAYS,AS_NEEDED (execution üretilmez)}; DurationType{UNTIL_DATE,+1 değer ⚠️}; MissedGracePeriod{30,60,90,120 dk}; RoutineExecutionStatus{PENDING,COMPLETED,SKIPPED,MISSED}; SupportStatus{PENDING,ACCEPTED,REJECTED,CANCELLED}; SupportAccessScope{ALL_ROUTINES,SELECTED_ROUTINES}; RoutineCategory{MEDICINE,SUPPLEMENT}; RoleName{ROLE_USER,ROLE_ADMIN}.

Migration'lar: V1 baseline, V2 rol seed, V3 constraint sertleştirme, V4 admin_audit, V5 sorgu indexleri, V6 expire_date nullable, V7 missed_grace_period, V8 unique(routine_id, scheduled_date), V9 refresh_tokens, V10 password_reset_tokens.
**Kural:** Şema değişikliği = yeni V11+ migration.
- **RefreshToken** (V9): user N:1, token_hash (SHA-256, unique), family_id (oturum ailesi), token_version (snapshot), expires_at (kayan 30 gün), family_expires_at (mutlak 90 gün), rotated_at, replaced_by_id (self FK, ON DELETE SET NULL), revoked_at/revoke_reason, device_name, platform (`ClientPlatform`), ip, user_agent. `ddl-auto=validate` olduğu için entity/DB uyuşmazlığı açılışta hata verir.

## 4. Güvenlik akışı ✅
`AuthRateLimitFilter` → `JwtAuthenticationFilter` → `SecurityConfiguration`.
- permitAll: `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`. `/api/v1/admin/**` → ADMIN (ayrıca controller'larda `@PreAuthorize`). Diğer her şey authenticated.
- JWT: HMAC, claim `tokenVersion`; kullanıcıdaki `tokenVersion` değişince eski tokenlar geçersiz olur (şifre değişimi vb.). Access süresi `JWT_EXPIRATION_MS` (varsayılan 15 dk).
- Refresh token ✅: `RefreshTokenService(Impl)` (`service/auth`). Opak token, DB'de hash. `rotate`: kilitli okuma → revoked/reuse (10 sn grace, sonrası aile revoke) → expiry → user aktif mi → tokenVersion snapshot eşit mi → yeni token. Hata: `AuthTokenException(code)` → `GlobalExceptionHandler` 401 (`REFRESH_TOKEN_INVALID|EXPIRED|REUSED`, `TOKEN_REVOKED`). `rotate` `noRollbackFor` ile çalışır ki aile iptali kalıcı olsun. Günlük temizlik cron'u (`app.jwt.refresh-cleanup-cron`).
- Logout: `/auth/logout` (refresh ailesini revoke, idempotent), `/users/me/logout-all` (tokenVersion++ + tüm refresh revoke). mustChangePassword kullanıcısı logout-all çağıramaz (filtre sadece `/users/me/password`'a izin veriyor).
- Refresh taşıma ✅ (FEATURE_DESIGN §1.9): `AuthController` istemci moduna göre ayırır. `X-Client-Type: mobile` → body; yoksa web → `supplog_refresh` httpOnly cookie (`security/RefreshTokenCookieService`, Path=/api/v1/auth, SameSite=Strict, Secure dev'de kapalı), body'de `refreshToken=null`. Web refresh/logout'ta `security/RequestOriginValidator` Origin'i `app.cors.allowed-origins` ile karşılaştırır → 403 `ORIGIN_NOT_ALLOWED`. Başarısız web refresh ve logout/logout-all cookie'yi siler. Mobil modda cookie hiç okunmaz. CORS `allowCredentials=true`. Prod'da frontend Vercel rewrite ile `/api` proxy'ler (cookie birinci taraf olsun diye).
- Rate limit: IP `security/ClientIpResolver` ile (`TRUSTED_PROXY_HOPS`, 0 = remoteAddr, >0 = X-Forwarded-For sağdan N), auth 20/dk, support 120/dk, bellek içi `ConcurrentHashMap` (>10.000 kayıtta temizlik).
- Hata cevapları: `CustomAuthenticationEntryPoint` (401), `CustomAccessDeniedHandler` (403), diğerleri `GlobalExceptionHandler` + `MessageHelper` (messages tr/en).
- Admin seed: `ADMIN_SEED_ENABLED=true` + username/email/password env'leri → `DataInitializer`.

## 5. API haritası (`/api/v1`)
| Controller | Uçlar |
|---|---|
| auth | POST register, login (opsiyonel deviceName/platform), refresh, logout, forgot-password (hep 202), reset-password (204) |
| users | GET me; POST me/logout-all; PATCH me/password, me/profile, me/time-zone; DELETE me |
| supplements | POST, GET (liste), GET/PUT/DELETE {id}, PATCH {id}/dosage |
| routines | POST, GET, GET/PUT/DELETE {id}, PATCH {id}/time, days, frequency, day-of-month, duration |
| routine-executions | GET today, history; PATCH routines/{routineId}/today/complete, /skip |
| support | POST requests; GET requests/incoming, outgoing; PATCH requests/{id}/accept, reject, cancel; GET connections/supporters, supported-users; DELETE connections/{id}; PATCH …/access-scope; GET/PUT …/routines; GET …/routine-selection |
| admin/users | liste, detail, id, search username/email, activeUsers, inactiveUsers, POST oluştur, PATCH deactivate/activate, PUT updateProfile, POST {id}/password, PUT {id}/role |
| admin/supplements, admin/routines | liste, detail, active/inactive, users/{userId} (+ routines: supplements/{id}), PATCH activate/deactivate, PUT {id} |
| admin/executions | GET, PATCH {id}/status (düzeltme) |
| admin/support-relationships | GET, PATCH {id}/revoke |
| admin/audits, admin/dashboard | GET |

## 6. Servis bağımlılıkları ✅
```
AuthService        → UserRepo, RoleRepo, PasswordEncoder, JwtService, AuthenticationManager, TimeZoneResolver
UserService        → ActiveUserService, SupportService, RoutineExecutionService, UserRepo
SupplementService  → SupplementRepo, RoutineRepo, ActiveUserService
RoutineService     → RoutineRepo, SupplementRepo, RoutineValidator, SupportService, RoutineExecutionService, TimeZoneResolver
RoutineExecutionSvc→ RoutineExecutionRepo, RoutineRepo, UserRepo, RoutineScheduleMatcher, TimeZoneResolver, TransactionTemplate, EntityManager
SupportService     → SupportRelationshipRepo, SupportRoutinePermissionRepo, RoutineRepo, UserRepo
AdminUserSvc       → AdminSupplement/Routine/Execution/Support svc, SupportService, AdminAudit, RoleRepo
AdminSupplementSvc → AdminRoutineService, AdminAudit
AdminRoutineSvc    → AdminExecutionService, RoutineExecutionService, SupportService, RoutineValidator, AdminAudit
AdminDashboardSvc  → tüm repolar (okuma)
Tüm Admin* yazma işlemleri → AdminAuditService → AdminAuditLogRepo
```
Zincirleme kaskad: kullanıcı pasifleştirme → supplement → routine → execution → support (Admin* servisler arası çağrı). Pasifleştirme/silme (soft delete) hatalarında bu zincire bak.

## 7. Zamanlanmış işler (RoutineExecutionServiceImpl) ✅
- `@Scheduled` **lifecycle** (varsayılan 15 dk, ilk gecikme 10 sn): rutinlere göre execution üretir/senkronize eder. `RoutineScheduleMatcher` o gün için rutinin geçerli olup olmadığına karar verir.
- `@Scheduled` **missed** (varsayılan 1 dk): grace süresi geçen PENDING execution'ları MISSED yapar.
- Zaman dilimi: her kullanıcının `timeZone`'u; `TimeZoneResolver` çözer, execution'da `scheduledZoneId` ve `scheduledAt`(Instant) saklanır. Varsayılan `APP_TIME_ZONE=UTC`.
- Eşzamanlılık: `lockActiveRoutine` (kilit) + `entityManager.refresh`, unique(routine_id, scheduled_date) ile çift kayıt engellenir. Tek instance varsayımı: ⚠️ ShedLock yok, çoklu instance'ta job çakışır (unique constraint son savunma).

## 8. Konfigürasyon / Deploy ✅
- Env: MAIL_PROVIDER (log|brevo), BREVO_API_KEY, MAIL_FROM_ADDRESS, MAIL_FROM_NAME, FRONTEND_BASE_URL, PASSWORD_RESET_TOKEN_TTL_MINUTES (30), PASSWORD_RESET_MAX_REQUESTS_PER_HOUR (3), DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET (≥32 byte), JWT_EXPIRATION_MS (900000), REFRESH_TOKEN_EXPIRATION_DAYS (30), REFRESH_TOKEN_ABSOLUTE_EXPIRATION_DAYS (90), REFRESH_TOKEN_REUSE_GRACE_SECONDS (10), REFRESH_COOKIE_SECURE (prod true), TRUSTED_PROXY_HOPS (0), CORS_ALLOWED_ORIGINS, APP_TIME_ZONE, *_RATE_LIMIT_*, ROUTINE_EXECUTION_*_MS, ADMIN_SEED_*. Örnek: `.env.example`.
- Profiller: `dev` (show-sql açık, swagger açık), `prod` (SQL log kapalı, swagger kapalı). Varsayılan profil `dev`.
- Docker: `eclipse-temurin:21-jdk` build → `21-jre` runtime. compose: postgres:18 (host 5433), backend (768M/1 CPU, 8080). Postgres healthcheck var, backend healthcheck yok.
- Yedekleme: `docs/postgres-backup-restore.md`.

## 9. Testler
Mevcut: PasswordResetServiceImplTest(8), PasswordResetPostgresIntegrationTest(3, DB), BrevoMailSenderTest(4, MockRestServiceServer), MailTemplateRendererTest(2), RefreshTokenServiceImplTest(13), AuthControllerTest(12, standalone MockMvc), RefreshTokenCookieServiceTest(4), RequestOriginValidatorTest(2), ClientIpResolverTest(3), RefreshTokenPostgresIntegrationTest(4, DB gerekli), CorsConfigTest(1), RoutineScheduleMatcherTest(1), RoutineValidatorTest(2), RoutineExecutionServiceImplTest(9), PostgresConcurrencyIntegrationTest(4), SupplogPostgresIntegrationTest(1). Entegrasyon testleri `RUN_DB_INTEGRATION_TESTS=true` ve ayrı `TEST_DB_*` ister.
Çalıştırma: `./mvnw test`.

## 10. Sorun → nereye bak
| Belirti | Önce şuraya |
|---|---|
| Açılışta schema validation hatası | Entity ↔ `db/migration` uyumu (§3) |
| 401 / token geçersiz | JwtService, `tokenVersion`, JWT_SECRET, süre; refresh hataları için `RefreshTokenServiceImpl.rotate` + `refresh_tokens` (revoke_reason kolonu nedeni söyler) |
| 403 admin ucu | SecurityConfiguration + `@PreAuthorize`, kullanıcının rolü |
| 429 | AuthRateLimitFilter + ClientIpResolver (`TRUSTED_PROXY_HOPS` yanlışsa herkes aynı IP sayılır) |
| Web'de refresh cookie gelmiyor / 403 ORIGIN_NOT_ALLOWED | Vercel rewrite var mı, `CORS_ALLOWED_ORIGINS` frontend origin'ini içeriyor mu, istek `credentials: include` mu, prod'da HTTPS + `REFRESH_COOKIE_SECURE=true` |
| Reset maili gelmiyor | `MAIL_PROVIDER` (log ise mail gitmez, link logda), BREVO_API_KEY/MAIL_FROM_ADDRESS, logda `Brevo rejected mail` / `Password reset rate limit reached`, Brevo panelinde gönderici doğrulaması ve günlük 300 limit, spam klasörü |
| Reset linki geçersiz | Token 30 dk geçerli ve tek kullanımlık; yeni talep eskisini geçersiz kılar (`invalidateOpenTokens`) |
| Execution oluşmuyor / çift | §7, RoutineScheduleMatcher, V8 unique index |
| Yanlış MISSED / saat kayması | kullanıcı timeZone, MissedGracePeriod, missed job aralığı |
| Support erişim hatası | SupportServiceImpl (status ACCEPTED + accessScope + SupportRoutinePermission) |
| Silinen kaydın gelmesi/gelmemesi | `isDeleted` filtreleri repository sorgularında |
| Docker'da bağlanamıyor | compose env (DB_URL host=`postgres`), healthcheck, JWT_SECRET boş mu |

## 11. Eksik / risk analizi (öncelik sırasıyla)
**Güvenlik**
1. ✅ Rate limit `request.getRemoteAddr()` kullanıyor; reverse proxy arkasında tüm istekler tek IP sayılır. `forward-headers-strategy` ayarı da yok. Ayrıca bellek içi → çoklu instance'ta paylaşılmaz.
2. ✅ `compose.yaml`: backend `DB_PASSWORD: postgres` sabit, postgres portu host'a açık (5433).
3. ✅ Refresh token, şifre sıfırlama ("şifremi unuttum"), e-posta doğrulama yok (kodda eşleşme bulunamadı). Token iptali yalnızca `tokenVersion` ile; logout ucu yok.
4. ⚠️ JWT secret uzunluk/boşluk doğrulaması açılışta yapılmıyor olabilir (kontrol edilmeli). Docker container root kullanıcıyla çalışıyor (Dockerfile'da `USER` yok).
5. ✅ Actuator bağımlılığı var; prod'da hangi endpoint'lerin açık olduğu/korunduğu tanımlı değil (`management.*` yok). Healthcheck için kullanılabilir.

**İşlevsel**
6. ✅ `score` alanı sadece 0'a set ediliyor, hiçbir yerde güncellenmiyor (ölü özellik veya eksik gamification).
7. ✅ `AdminAuditLog.adminUserId` için FK yok (silinmiş admin için bilinçli olabilir, kararlaştırılmalı).
8. ⚠️ Scheduler çoklu instance'ta koordinasyonsuz (ShedLock/advisory lock yok).
9. ⚠️ Bildirim/hatırlatma yok (rutin saati gelince push/e-posta yok); yalnızca API.
10. ⚠️ `UserRepository.findAll()` List döndürüyor; büyük veride sayfalanmamış kullanım riski.
11. ⚠️ Kullanıcı hesabı silme: soft delete; KVKK/GDPR için kalıcı silme/anonimleştirme politikası yok.

**Kalite / operasyon**
12. ✅ Test boşlukları: Auth, User, Supplement, Support, Admin* servisleri, JwtAuthenticationFilter, AuthRateLimitFilter, GlobalExceptionHandler için test yok; controller (MockMvc) testi yok.
13. ✅ CI/CD yok (.github yok). Backend için Docker healthcheck yok.
14. ✅ Yapılandırılmış log/metrics/tracing yok; yalnızca log seviyesi ayarları.
15. ⚠️ API sürümlemesi `/v1`; DTO/Swagger açıklamaları ve hata kodu sözleşmesi dokümante edilmemiş. README dışında mimari doküman yoktu (bu dosya ilk).
16. ✅ Kod stili tutarsızlıkları: `UpdateSupplementRequestDtoAdmin` / `UpdateUserProfileRequestDtoByAdmin` isimlendirmesi diğerleriyle uyumsuz; `dto/routineExecution` camelCase paket; servis paketlerinde `adminExecutionService/impl` gibi tekrarlı yol.
17. ✅ `dev` varsayılan profil: prod'da `SPRING_PROFILES_ACTIVE` unutulursa show-sql + swagger açık kalır (compose bunu `prod` yapıyor ama başka ortamlar için risk).

## 12. Bu dosyanın bakımı
Entity, endpoint, migration, env değişkeni veya zamanlanmış iş eklendiğinde ilgili bölümü güncelle. Doğrulanmamış (⚠️) maddeler koda bakılınca ✅/düzeltme olarak işaretlenmeli.

## 13. Yol haritası (kullanıcı talepleri – tartışma aşamasında, kod yazılmadı)
| # | Konu | Durum |
|---|---|---|
| 1 | Şifremi unuttum (login akışı) | Yok |
| 2 | Logout | Yok |
| 3 | Refresh token | Yok |
| 4 | Bildirimler: rutin saati (sahibine), destekçilere bildirim, supporter→owner tek yönlü nudge/mesaj | Yok |
| 5 | Mobil (Flutter) | Sonra; şimdilik not. Backend, FCM/device-token ve kısa ömürlü access + refresh akışını buna göre tasarlamalı |

### Taslak tasarım (onay bekliyor)
- **2+3 birlikte**: `refresh_tokens` (hash'li token, userId, deviceInfo, expiresAt, revokedAt, rotation + reuse detection). Access token 15 dk'ya düşer. `POST /auth/refresh`, `POST /auth/logout` (o cihazın refresh'ini iptal), `POST /auth/logout-all` (tokenVersion++ ve tüm refresh iptal). Şifre değişimi/admin reset zaten tokenVersion'ı artırıyor → refresh'ler de iptal edilmeli.
- **1 Şifremi unuttum**: `password_reset_tokens` (hash'li, tek kullanımlık, ~30 dk), `POST /auth/forgot-password` (kullanıcı var/yok aynı cevap), `POST /auth/reset-password` (tokenVersion++, refresh'ler iptal). **Önkoşul: mail altyapısı** (spring-boot-starter-mail + SMTP/sağlayıcı) ve idealde e-posta doğrulama (şu an yok, kayıt e-postası doğrulanmıyor). Rate limit auth grubunda zaten var.
- **4 Bildirim**: 
  - `device_tokens` (userId, FCM token, platform), `notifications` (inbox: type, userId, payload, readAt), `notification_preferences`.
  - Rutin saati: mevcut lifecycle/missed scheduler'a bağlanır; `RoutineExecution`'a `reminder_sent_at` benzeri işaret gerekir (çift gönderimi önlemek için), kullanıcının timeZone'u kullanılır.
  - Destekçi bildirimi: sadece `ACCEPTED` ilişki + `accessScope`/`SupportRoutinePermission` kapsamındaki rutinler için (MISSED olduğunda vb.).
  - Nudge/mesaj: supporter→owner tek yönlü, sadece ACCEPTED ilişki + izinli rutin, rate limit (ör. rutin başına günde N), mesaj serbest metin mi hazır şablon mu karar verilecek.
  - Push (FCM) mobil gelene kadar gönderilemez; önce inbox + (opsiyonel) e-posta, device-token endpoint'i mobil için hazır tutulur.

### Önerilen sıra
1) Refresh + logout → 2) Mail altyapısı + şifremi unuttum (+ e-posta doğrulama kararı) → 3) Bildirim inbox + scheduler + nudge → 4) FCM push (Flutter ile birlikte).

### Açık sorular
- Mail sağlayıcısı (SMTP / Resend / SES …)? E-posta doğrulama zorunlu olsun mu?
- Access token süresi ve refresh ömrü (öneri: 15 dk / 30 gün, rotation açık)?
- Rutin hatırlatması tam saatte mi, X dk önce mi? Sessiz saatler/tercihler gerekli mi?
- Destekçi neyden haberdar olsun: sadece MISSED mı, SKIPPED/COMPLETED da mı?
- Nudge: serbest metin mi hazır şablon mu? Owner karşılık verebilir mi (şimdilik tek yön)?
- Bildirim geçmişi (inbox) mobilde gösterilecek mi, saklama süresi?

### Kararlar (2. tur)
- **Altyapı:** Frontend Vercel, backend Railway, tamamen ücretsiz. Push için FCM (ücretsiz). Mail için Firebase kullanılamaz (kendi JWT auth'umuz var; Firebase mail yalnızca Firebase Auth ile çalışır). Mail sağlayıcı seçimi açık (Resend domain ister, Brevo tek gönderici doğrulamasıyla çalışır, Gmail SMTP Railway'de engelli olabilir). Mail gönderimi `MailSender` arayüzü arkasında soyutlanacak ki sağlayıcı değişince kod değişmesin.
- **Hatırlatma kapsamı:** Destekçi, izin verilen rutinlerin tamamını görür (kaçırılan, zamanı gelen, gelecek).
- **Push zamanlaması:** Kullanıcı hatırlatma saatini seçebilir. Seçmezse varsayılan: rutin saatinden 30 dk öncesinden başlayıp her 10 dk'da bir (T-30, T-20, T-10), son 5 dk'da her dakika (T-5…T-1). Rutin COMPLETED (veya SKIPPED) olunca bildirimler durur. → Scheduler 1 dk aralıkla çalışmalı; gönderilen her hatırlatma kaydedilerek (çift gönderim önlenir).
- **Nudge:** Hazır şablon seçilebilir VEYA serbest metin yazılabilir (ikisi de desteklenir). Mesajlar sisteme kaydedilir (geçmiş). Tek yönlü supporter→owner.

> Yeni özellik tasarımı ve görev listesi: [FEATURE_DESIGN.md](FEATURE_DESIGN.md)

## 14. Mail ve şifre sıfırlama ✅ (Faz 1B/1C)
- `service/mail`: `MailSender` (arayüz), `MailMessage`, `MailTemplateRenderer` (`resources/mail/<ad>_<tr|en>.html|txt`, `{{key}}`, HTML escape), `impl/LoggingMailSender` (varsayılan, maili loglar), `impl/BrevoMailSender` (HTTPS `POST /v3/smtp/email`, `api-key` header, 3/5 sn timeout, 5xx'te 1 tekrar, 4xx'te tekrar yok). Bean seçimi `config/MailConfig` + `app.mail.provider`. Resend'e geçiş = yeni `ResendMailSender` + `MAIL_PROVIDER=resend`.
- `config/AsyncConfig`: `@EnableAsync`, `notificationExecutor` (2–4 thread, kuyruk 500). Mail/push için `@Async(AsyncConfig.NOTIFICATION_EXECUTOR)`.
- `service/auth/PasswordResetService(Impl)`: `requestReset` e-postayı normalize eder, kullanıcı yok/pasifse sessizce döner, saatte 3 talep sınırı, açık token'ları geçersiz kılar, yeni token'ın hash'ini (V10 `password_reset_tokens`) kaydeder, `PasswordResetRequestedEvent` yayınlar. `resetPassword`: confirm eşleşmesi → kilitli token okuma → kullanılmamış/süresi dolmamış/kullanıcı aktif → şifre + `mustChangePassword=false` + `tokenVersion++` → token used + diğer açık token'lar kapanır + refresh token'lar `PASSWORD_RESET` ile revoke. Hata: 400 `PASSWORD_RESET_TOKEN_INVALID`.
- `PasswordResetMailListener`: `@TransactionalEventListener(AFTER_COMMIT)` + async → link `${FRONTEND_BASE_URL}/reset-password?token=…`, konu `mail.password-reset.subject`, dil istek anındaki `Accept-Language`. Gönderim hatası loglanır, kullanıcıya yansımaz.
- Temizlik: `app.password-reset.cleanup-cron` (03:45), 1 günden eski kullanılmış/süresi dolmuş token'lar silinir.
- Gönderici: `norepplysupploop@gmail.com` (Brevo'da doğrulandı, `MAIL_FROM_ADDRESS`).
- Canlı durum (2026-10-20): refresh token + cookie ve Brevo ile şifre sıfırlama production'da uçtan uca doğrulandı (Railway env: MAIL_PROVIDER=brevo, BREVO_API_KEY, MAIL_FROM_ADDRESS, FRONTEND_BASE_URL=https://supplog.vercel.app).

## 15. Geri bildirim turu 1 (2026-10-22) – özet
- Hata cevabı: `messageKey` (sabit anahtar) + `details` (ör. takviye silme engelinde `routines`). Frontend kararlarını `message` değil `messageKey` ile verir.
- Execution cevabı: `supplementName`, `supplementDosage`, `routineDeleted` (silinmiş rutin geçmişte adıyla). Geçmiş sorguları `routine.supplement` ile fetch eder.
- `PATCH /routine-executions/routines/{id}/today/undo`: COMPLETED/SKIPPED → PENDING, sadece `missedAt` öncesi.
- Bugünkü doz kuralı (`datesToCreate`): rutin oluşturulduğunda/değiştirildiğinde bugünün `missedAt`'i geçmişse bugünkü kayıt oluşturulmaz (ilk doz yarın).
- Takviye adı aktif takviyeler arasında büyük/küçük harf duyarsız tekil (uygulama seviyesi, DB index yok).
- Zaman damgaları: audit alanları UTC ve ofsetsiz; frontend `parseServerTimestamp` ile UTC okur. Yeni kodda `LocalDateTime.now()` değil `LocalDateTime.now(ZoneOffset.UTC)` kullan.
- Ayrıntı ve test sonuçları: [FEEDBACK_FIXES.md](FEEDBACK_FIXES.md).
