package com.supplog.repository;

import com.supplog.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findAllByUserId(Long userId);

    long countByUserId(Long userId);

    /**
     * Token zaten varsa yeni kullanıcıya taşınır (aynı telefonda hesap değişimi) ve bilgileri tazelenir;
     * yoksa eklenir. Tek SQL olduğu için aynı token'ın yarışan kayıtları birbirini bozmaz.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO device_tokens
                (user_id, push_token, platform, locale, app_version, last_seen_at, created_at)
            VALUES (:userId, :pushToken, :platform, :locale, :appVersion, :now, :now)
            ON CONFLICT (push_token) DO UPDATE SET
                user_id = EXCLUDED.user_id,
                platform = EXCLUDED.platform,
                locale = EXCLUDED.locale,
                app_version = EXCLUDED.app_version,
                last_seen_at = EXCLUDED.last_seen_at
            """, nativeQuery = true)
    int upsert(
            @Param("userId") Long userId,
            @Param("pushToken") String pushToken,
            @Param("platform") String platform,
            @Param("locale") String locale,
            @Param("appVersion") String appVersion,
            @Param("now") Instant now
    );

    // Kullanıcı başına en yeni `keep` cihaz kalır (token şişmesini sınırlar).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM device_tokens
            WHERE user_id = :userId
              AND id NOT IN (
                  SELECT id FROM device_tokens
                  WHERE user_id = :userId
                  ORDER BY last_seen_at DESC, id DESC
                  LIMIT :keep)
            """, nativeQuery = true)
    int trimToNewest(@Param("userId") Long userId, @Param("keep") int keep);

    // Sadece sahibi silebilir; başkasının token'ı verilirse hiçbir şey olmaz.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int deleteByPushTokenAndUserId(String pushToken, Long userId);

    // FCM "kayıtsız token" dediğinde sahibine bakmadan silinir.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int deleteByPushToken(String pushToken);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int deleteAllByUserId(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int deleteByLastSeenAtBefore(Instant threshold);
}
