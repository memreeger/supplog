package com.supplog.service.auth;

import com.supplog.enums.ClientPlatform;

// Refresh token oturumunun hangi cihazdan açıldığı bilgisi.
public record ClientInfo(
        String deviceName,
        ClientPlatform platform,
        String ipAddress,
        String userAgent
) {
}
