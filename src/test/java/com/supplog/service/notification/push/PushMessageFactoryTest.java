package com.supplog.service.notification.push;

import com.supplog.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PushMessageFactoryTest {

    private static final Locale TR = Locale.forLanguageTag("tr");
    private static final Locale EN = Locale.forLanguageTag("en");

    private final PushMessageFactory factory = new PushMessageFactory(realMessages());

    private static ReloadableResourceBundleMessageSource realMessages() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    private PushMessageFactory.Content create(NotificationType type, Map<String, Object> params, Locale locale, boolean hide) {
        return factory.create(type, params, 7L, 3L, "exec:9:REMINDER", locale, hide);
    }

    @Test
    void rendersReminderPhasesInBothLanguages() {
        Map<String, Object> pre = Map.of("supplementName", "Çinko", "minutes", 10, "phase", "PRE");
        Map<String, Object> late = Map.of("supplementName", "Çinko", "minutes", 5, "phase", "LATE");

        assertEquals("Çinko vakti yaklaşıyor", create(NotificationType.ROUTINE_REMINDER, pre, TR, false).title());
        assertEquals("10 dakika sonra alma zamanı.", create(NotificationType.ROUTINE_REMINDER, pre, TR, false).body());
        assertEquals("Çinko dozunu kaçırıyorsun!", create(NotificationType.ROUTINE_REMINDER, late, TR, false).title());
        assertEquals("You are about to miss Çinko!", create(NotificationType.ROUTINE_REMINDER, late, EN, false).title());
        assertEquals("5 minutes left before it counts as missed.", create(NotificationType.ROUTINE_REMINDER, late, EN, false).body());
    }

    @Test
    void rendersMissedAndSupporterAlerts() {
        assertEquals("Çinko dozunu kaçırdın",
                create(NotificationType.ROUTINE_MISSED, Map.of("supplementName", "Çinko"), TR, false).title());

        Map<String, Object> lastCall = Map.of("ownerName", "Ahmet", "supplementName", "Çinko", "phase", "LAST_CALL", "minutes", 5);
        assertEquals("Ahmet dozu kaçırmak üzere!", create(NotificationType.SUPPORT_ALERT, lastCall, TR, false).title());
        assertEquals("Çinko için 5 dakika kaldı. Ona hatırlat!", create(NotificationType.SUPPORT_ALERT, lastCall, TR, false).body());
        assertEquals("Ahmet is about to miss a dose!", create(NotificationType.SUPPORT_ALERT, lastCall, EN, false).title());

        Map<String, Object> missed = Map.of("ownerName", "Ahmet", "supplementName", "Çinko", "phase", "MISSED");
        assertEquals("Çinko was missed. Don't forget to cheer them on.",
                create(NotificationType.SUPPORT_ALERT, missed, EN, false).body());
    }

    @Test
    void rendersNudgesFromTemplatesAndFreeText() {
        Map<String, Object> template = Map.of("senderName", "Mehmet", "templateKey", "DONT_FORGET");
        PushMessageFactory.Content fromTemplate = create(NotificationType.NUDGE, template, EN, false);
        assertEquals("Mehmet sent you a message", fromTemplate.title());
        assertEquals("Don't forget your supplement, I'm here for you. 🙂", fromTemplate.body());

        Map<String, Object> text = Map.of("senderName", "Mehmet", "message", "bro take it {0} ''x''");
        assertEquals("bro take it {0} ''x''", create(NotificationType.NUDGE, text, TR, false).body());
    }

    @Test
    void missingParamsFallBackToTheGenericText() {
        PushMessageFactory.Content content = create(NotificationType.ROUTINE_MISSED, Map.of(), EN, false);

        assertEquals("New notification", content.title());
        assertFalse(content.body().contains("{"));
        assertEquals("New notification", create(NotificationType.NUDGE, Map.of("senderName", "M", "templateKey", "NOPE"), EN, false).title());
    }

    @Test
    void hideDetailsKeepsSupplementNamesOffTheLockScreen() {
        Map<String, Object> params = Map.of("supplementName", "Çinko", "minutes", 10, "phase", "PRE");

        PushMessageFactory.Content hidden = create(NotificationType.ROUTINE_REMINDER, params, TR, true);

        assertEquals("Supplog", hidden.title());
        assertEquals("Yeni bir bildirimin var.", hidden.body());
        assertFalse((hidden.title() + hidden.body()).contains("Çinko"));
    }

    @Test
    void dataCarriesOnlyIdsAndTypeAndChannelsFollowTheType() {
        PushMessageFactory.Content content = create(NotificationType.SUPPORT_ALERT,
                Map.of("ownerName", "Ahmet", "supplementName", "Çinko"), TR, false);

        assertEquals(Map.of("type", "SUPPORT_ALERT", "notificationId", "7", "routineId", "3"), content.data());
        assertEquals("routine_alerts", content.channelId());
        assertEquals("exec:9:REMINDER", content.tag());
        assertEquals("routine_reminders", PushMessageFactory.channelFor(NotificationType.ROUTINE_MISSED));
        assertEquals("nudges", PushMessageFactory.channelFor(NotificationType.NUDGE));
    }
}
