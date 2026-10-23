package com.supplog.service.notification.push;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import java.nio.charset.StandardCharsets;

/**
 * {@link PushPayload} → FCM {@link Message}. Hem "notification" hem "data" alanı vardır: uygulama kapalıyken
 * sistem bildirimi gösterir, dokununca data uygulamaya ulaşır. FCM sınırı 4096 bayttır; metinler bunun çok altında kalır.
 */
public final class FcmMessageBuilder {

    static final int MAX_TITLE_CHARS = 100;
    static final int MAX_BODY_CHARS = 300;
    // APNs collapse-id en çok 64 bayt olabilir.
    static final int MAX_APNS_COLLAPSE_BYTES = 64;

    private FcmMessageBuilder() {
    }

    public static Message build(String pushToken, PushPayload payload) {
        String title = truncate(payload.title(), MAX_TITLE_CHARS);
        String body = truncate(payload.body(), MAX_BODY_CHARS);

        Message.Builder builder = Message.builder()
                .setToken(pushToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(payload.data())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId(payload.channelId())
                                .setTag(payload.tag())
                                .build())
                        .build());

        ApnsConfig.Builder apns = ApnsConfig.builder()
                .setAps(Aps.builder().setThreadId(payload.tag()).build());
        if (payload.tag() != null) {
            apns.putHeader("apns-collapse-id", truncateBytes(payload.tag(), MAX_APNS_COLLAPSE_BYTES));
        }
        return builder.setApnsConfig(apns.build()).build();
    }

    static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.codePointCount(0, text.length()) <= maxChars
                ? text
                : text.substring(0, text.offsetByCodePoints(0, maxChars - 1)) + "…";
    }

    private static String truncateBytes(String text, int maxBytes) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return bytes.length <= maxBytes ? text : new String(bytes, 0, maxBytes, StandardCharsets.UTF_8);
    }
}
