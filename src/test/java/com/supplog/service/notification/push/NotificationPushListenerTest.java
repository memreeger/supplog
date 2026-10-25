package com.supplog.service.notification.push;

import com.supplog.entity.DeviceToken;
import com.supplog.entity.NotificationPreference;
import com.supplog.enums.NotificationType;
import com.supplog.repository.NotificationPreferenceRepository;
import com.supplog.service.notification.DeviceTokenService;
import com.supplog.service.notification.NotificationCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NotificationPushListenerTest {

    private final NotificationPreferenceRepository preferences = mock(NotificationPreferenceRepository.class);
    private final DeviceTokenService devices = mock(DeviceTokenService.class);
    private final PushSender sender = mock(PushSender.class);
    private NotificationPushListener listener;

    private final NotificationCreatedEvent event = new NotificationCreatedEvent(
            5L, 42L, NotificationType.ROUTINE_REMINDER, "exec:9:REMINDER", 3L,
            Map.of("supplementName", "Çinko", "minutes", 10, "phase", "PRE"));

    @BeforeEach
    void setUp() {
        ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
        messages.setBasename("classpath:messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        listener = new NotificationPushListener(preferences, devices, new PushMessageFactory(messages), sender);
        when(preferences.findById(42L)).thenReturn(Optional.empty());
        when(sender.send(anyString(), any())).thenReturn(PushOutcome.SENT);
    }

    private static DeviceToken device(String token, String locale) {
        DeviceToken device = new DeviceToken();
        device.setPushToken(token);
        device.setLocale(locale);
        return device;
    }

    @Test
    void sendsToEveryDeviceInTheDeviceLanguage() {
        when(devices.tokensFor(42L)).thenReturn(List.of(device("tok-tr", "tr-TR"), device("tok-en", "en"), device("tok-none", null)));

        listener.push(event);

        ArgumentCaptor<PushPayload> payloads = ArgumentCaptor.forClass(PushPayload.class);
        verify(sender).send(eq("tok-tr"), payloads.capture());
        verify(sender).send(eq("tok-en"), payloads.capture());
        verify(sender).send(eq("tok-none"), payloads.capture());
        assertEquals("Çinko vakti yaklaşıyor", payloads.getAllValues().get(0).title());
        assertEquals("Çinko is coming up", payloads.getAllValues().get(1).title());
        assertEquals("Çinko vakti yaklaşıyor", payloads.getAllValues().get(2).title()); // varsayılan dil tr
        assertEquals("exec:9:REMINDER", payloads.getAllValues().get(0).tag());
    }

    @Test
    void doesNothingWhenPushIsTurnedOffOrThereAreNoDevices() {
        NotificationPreference off = new NotificationPreference(42L);
        off.setPushEnabled(false);
        when(preferences.findById(42L)).thenReturn(Optional.of(off));
        when(devices.tokensFor(42L)).thenReturn(List.of(device("tok", "tr")));

        listener.push(event);
        verifyNoInteractions(sender);

        when(preferences.findById(42L)).thenReturn(Optional.empty());
        when(devices.tokensFor(42L)).thenReturn(List.of());
        listener.push(event);
        verifyNoInteractions(sender);
    }

    @Test
    void hideDetailsPreferenceSendsAGenericText() {
        NotificationPreference hide = new NotificationPreference(42L);
        hide.setHideDetailsInPush(true);
        when(preferences.findById(42L)).thenReturn(Optional.of(hide));
        when(devices.tokensFor(42L)).thenReturn(List.of(device("tok", "en")));

        listener.push(event);

        ArgumentCaptor<PushPayload> payload = ArgumentCaptor.forClass(PushPayload.class);
        verify(sender).send(eq("tok"), payload.capture());
        assertEquals("Supplog", payload.getValue().title());
        assertFalse(payload.getValue().body().contains("Çinko"));
    }

    @Test
    void invalidTokensAreRemovedButTemporaryFailuresKeepThem() {
        when(devices.tokensFor(42L)).thenReturn(List.of(device("dead", "tr"), device("flaky", "tr"), device("ok", "tr")));
        when(sender.send(eq("dead"), any())).thenReturn(PushOutcome.INVALID_TOKEN);
        when(sender.send(eq("flaky"), any())).thenReturn(PushOutcome.FAILED);

        listener.push(event);

        verify(devices).removeInvalid("dead");
        verify(devices, never()).removeInvalid("flaky");
        verify(devices, never()).removeInvalid("ok");
        verify(sender).send(eq("ok"), any());
    }

    @Test
    void aFailureNeverEscapesTheListener() {
        when(devices.tokensFor(42L)).thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(() -> listener.onNotification(event));
    }
}
