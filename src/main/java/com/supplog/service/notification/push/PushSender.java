package com.supplog.service.notification.push;

public interface PushSender {

    // Tek cihaza gönderir; hata fırlatmaz, sonucu döner. Token ve metin asla loglanmaz.
    PushOutcome send(String pushToken, PushPayload payload);
}
