package com.supplog.service.auth;

import com.supplog.entity.User;
import com.supplog.enums.RefreshTokenRevokeReason;

public interface RefreshTokenService {

    // Login/register: yeni oturum ailesi başlatır.
    IssuedRefreshToken issue(User user, ClientInfo clientInfo);

    // Refresh: token'ı tüketir, aynı aileden yenisini döndürür.
    IssuedRefreshToken rotate(String rawToken, ClientInfo clientInfo);

    // Logout: idempotent, geçersiz token sessizce yok sayılır.
    void revoke(String rawToken);

    void revokeAllForUser(Long userId, RefreshTokenRevokeReason reason);

    void deleteExpiredTokens();
}
