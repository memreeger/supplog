package com.supplog.service.notification;

import com.supplog.dto.notification.NotificationResponseDto;
import com.supplog.dto.notification.UnreadCountResponseDto;

import java.util.List;

public interface NotificationService {

    // Çözülmemiş bildirimler, en yeni önce. limit 1..50 arasına çekilir.
    List<NotificationResponseDto> list(Long userId, int limit);

    UnreadCountResponseDto unreadCount(Long userId);

    // Idempotent; başkasının bildirimi "bulunamadı" gibi davranır.
    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);

    // 90 günden eski bildirimleri siler (günlük iş).
    void deleteOldNotifications();
}
