package com.supplog.service.notification;

public interface ReminderService {

    // Bir zamanlayıcı turu: vakti gelen hatırlatmaları ve yeni kaçırılan dozların bildirimini işler.
    void processDue();

    // Eski dispatch log satırlarını siler (günlük iş).
    void deleteOldDispatchLog();
}
