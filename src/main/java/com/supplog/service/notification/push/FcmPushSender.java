package com.supplog.service.notification.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Gerçek gönderici (PUSH_PROVIDER=fcm). FCM HTTP v1 üzerinden tek cihaza gönderir; hataları
 * {@link PushOutcome}'a çevirir. Log'a token ve metin yazılmaz, yalnızca FCM hata kodu.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.notifications.push-provider", havingValue = "fcm")
public class FcmPushSender implements PushSender {

    private final FirebaseMessaging firebaseMessaging;

    public FcmPushSender(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public PushOutcome send(String pushToken, PushPayload payload) {
        try {
            firebaseMessaging.send(FcmMessageBuilder.build(pushToken, payload));
            return PushOutcome.SENT;
        } catch (FirebaseMessagingException e) {
            return classify(e);
        } catch (RuntimeException e) {
            // Ağ/SDK hatası bir bildirimi kaybettirir ama zinciri kırmaz.
            log.warn("FCM send failed unexpectedly: {}", e.getClass().getSimpleName());
            return PushOutcome.FAILED;
        }
    }

    // UNREGISTERED: uygulama silindi/token yenilendi. INVALID_ARGUMENT ve SENDER_ID_MISMATCH: token bu projeye
    // ait değil ya da bozuk; mesajımız kurulum gereği geçerli olduğundan token'ı silmek güvenlidir.
    static PushOutcome classify(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT
                || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
            return PushOutcome.INVALID_TOKEN;
        }
        // Çoğu kurulum hatası (yetki, kapalı API, ağ) FCM'e özel kod taşımaz: genel kategori ve Google'ın
        // mesajı teşhis için gerekir. Mesaj token ya da anahtar içermez; yine de kısaltılır.
        log.warn("FCM rejected a push, code={} category={} detail={}",
                code, e.getErrorCode(), abbreviate(e.getMessage()));
        return PushOutcome.FAILED;
    }

    private static String abbreviate(String message) {
        if (message == null) {
            return "";
        }
        String oneLine = message.replaceAll("\\s+", " ");
        return oneLine.length() <= 300 ? oneLine : oneLine.substring(0, 300) + "…";
    }
}
