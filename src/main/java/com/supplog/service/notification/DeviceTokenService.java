package com.supplog.service.notification;

import com.supplog.dto.notification.DeviceRegisterDto;
import com.supplog.entity.DeviceToken;

import java.util.List;

public interface DeviceTokenService {

    // Idempotent; token başka kullanıcıdaysa bu kullanıcıya taşınır.
    void register(Long userId, DeviceRegisterDto request);

    // Yalnızca sahibi siler; olmayan ya da başkasına ait token sessizce yok sayılır.
    void unregister(Long userId, String pushToken);

    void deleteAllForUser(Long userId);

    List<DeviceToken> tokensFor(Long userId);

    // FCM bu token'ı geçersiz saydı.
    void removeInvalid(String pushToken);

    // Uzun süredir görülmeyen token'ları siler (günlük iş).
    void deleteStale();
}
