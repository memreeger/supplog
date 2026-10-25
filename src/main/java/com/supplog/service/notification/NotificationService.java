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

    // Satır yoksa oluşturur, varsa günceller. Çözülmüş ya da faz (params.phase) değişmiş satır
    // tekrar okunmamış olur; aynı fazdaki güncelleme okunmuşluğu korur.
    void upsertForExecution(NotificationDraft draft);

    // Doz tamamlandı/atlandı: bu dozun bütün bildirimleri çandan düşer.
    void resolveForExecution(Long executionId);

    // Yalnızca bu türdeki bildirimleri çözer (kaçırılınca hatırlatma satırı gider, destekçi satırı güncellenir).
    void resolveForExecution(Long executionId, com.supplog.enums.NotificationType type);

    // Destekçi izni daraldı/kalktı: sahibin keepRoutineIds dışındaki rutinlerinden gelen uyarılar çandan düşer.
    void resolveSupportAlerts(Long supporterUserId, Long ownerUserId, java.util.Collection<Long> keepRoutineIds);

    // 90 günden eski bildirimleri siler (günlük iş).
    void deleteOldNotifications();
}
