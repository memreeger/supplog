package com.supplog.service.notification.push;

import com.supplog.enums.NotificationType;
import com.supplog.enums.NudgeTemplate;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Bildirimin type + params verisinden cihazın dilinde push başlığı/metni üretir (messages*.properties).
 * İstemci içindeki çanda aynı metni kendi i18n'iyle çizer; ikisi birbirine paraleldir.
 */
@Component
public class PushMessageFactory {

    public record Content(String title, String body, String tag, String channelId, Map<String, String> data) {
    }

    private final MessageSource messageSource;

    public PushMessageFactory(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * @param hideDetails kilit ekranında ilaç/isim görünmesin (kullanıcı tercihi): genel bir metin gider.
     */
    public Content create(
            NotificationType type,
            Map<String, Object> params,
            Long notificationId,
            Long routineId,
            String tag,
            Locale locale,
            boolean hideDetails
    ) {
        String title;
        String body;
        String[] texts = hideDetails ? null : texts(type, params, locale);
        if (texts == null) {
            title = text(hideDetails ? "push.hidden.title" : "push.generic.title", locale);
            body = text(hideDetails ? "push.hidden.body" : "push.generic.body", locale);
        } else {
            title = texts[0];
            body = texts[1];
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());
        data.put("notificationId", String.valueOf(notificationId));
        if (routineId != null) {
            data.put("routineId", String.valueOf(routineId));
        }
        return new Content(title, body, tag, channelFor(type), data);
    }

    static String channelFor(NotificationType type) {
        return switch (type) {
            case ROUTINE_REMINDER, ROUTINE_MISSED -> "routine_reminders";
            case SUPPORT_ALERT -> "routine_alerts";
            case NUDGE -> "nudges";
        };
    }

    // null: gerekli veri eksik, genel metne düşülür.
    private String[] texts(NotificationType type, Map<String, Object> params, Locale locale) {
        String supplement = str(params, "supplementName");
        String minutes = str(params, "minutes");
        String owner = str(params, "ownerName");
        String sender = str(params, "senderName");
        String phase = str(params, "phase");

        return switch (type) {
            case ROUTINE_REMINDER -> supplement.isEmpty() || minutes.isEmpty() ? null
                    : "LATE".equals(phase)
                    ? new String[]{text("push.reminder.late.title", locale, supplement), text("push.reminder.late.body", locale, minutes)}
                    : new String[]{text("push.reminder.pre.title", locale, supplement), text("push.reminder.pre.body", locale, minutes)};
            case ROUTINE_MISSED -> supplement.isEmpty() ? null
                    : new String[]{text("push.missed.title", locale, supplement), text("push.missed.body", locale)};
            case SUPPORT_ALERT -> owner.isEmpty() || supplement.isEmpty() ? null : switch (phase) {
                case "MISSED" -> new String[]{text("push.support.missed.title", locale, owner), text("push.support.missed.body", locale, owner, supplement)};
                case "LAST_CALL" -> minutes.isEmpty() ? null
                        : new String[]{text("push.support.lastcall.title", locale, owner), text("push.support.lastcall.body", locale, owner, supplement, minutes)};
                default -> new String[]{text("push.support.ontime.title", locale, owner), text("push.support.ontime.body", locale, owner, supplement)};
            };
            case NUDGE -> nudgeTexts(params, sender, locale);
        };
    }

    private String[] nudgeTexts(Map<String, Object> params, String sender, Locale locale) {
        if (sender.isEmpty()) {
            return null;
        }
        String templateKey = str(params, "templateKey");
        String body;
        if (!templateKey.isEmpty() && isTemplate(templateKey)) {
            body = text("push.nudge.template." + templateKey, locale);
        } else {
            body = str(params, "message");
        }
        return body.isEmpty() ? null : new String[]{text("push.nudge.title", locale, sender), body};
    }

    private static boolean isTemplate(String key) {
        for (NudgeTemplate template : NudgeTemplate.values()) {
            if (template.name().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private String text(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }

    private static String str(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value instanceof String || value instanceof Number ? String.valueOf(value) : "";
    }
}
