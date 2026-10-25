package com.supplog.service.notification.push;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FcmMessageBuilderTest {

    private static PushPayload payload(String title, String body, String tag) {
        return new PushPayload(title, body, Map.of("type", "NUDGE", "notificationId", "1"), tag, "nudges");
    }

    @Test
    void buildsAMessageForNormalAndMissingTags() {
        assertNotNull(FcmMessageBuilder.build("token-1", payload("Başlık", "Metin", "exec:1:REMINDER")));
        assertNotNull(FcmMessageBuilder.build("token-1", payload("Başlık", "Metin", null)));
    }

    @Test
    void veryLongTextAndTagsDoNotBreakTheBuilder() {
        String huge = "ğ".repeat(10_000);
        assertNotNull(FcmMessageBuilder.build("token-1", payload(huge, huge, "x".repeat(500))));
    }

    // Regresyon: SDK enum adlarını JVM'in varsayılan diliyle küçültüyordu; Türkçe'de "HIGH" → "hıgh" olup FCM
    // tarafından reddediliyordu. Mesaj artık böyle bir alan taşımamalı ve JSON'u noktasız ı içermemeli.
    @Test
    void messageDoesNotDependOnTheJvmDefaultLocale() throws Exception {
        java.util.Locale original = java.util.Locale.getDefault();
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
        try {
            com.google.firebase.messaging.Message message =
                    FcmMessageBuilder.build("token-1", payload("Başlık", "Metin", "exec:1:REMINDER"));

            String json = com.google.api.client.json.gson.GsonFactory.getDefaultInstance().toString(message);

            assertFalse(json.contains("ıgh"), json);
            assertFalse(json.contains("\"priority\""), json);
            assertTrue(json.contains("channel_id"), json);

            // Kanıt: aynı serileştirme, öncelik gönderilseydi hatayı gerçekten yakalardı (SDK düzelirse atlanır).
            com.google.firebase.messaging.Message withPriority = com.google.firebase.messaging.Message.builder()
                    .setToken("token-1")
                    .setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
                            .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH).build())
                    .build();
            String buggy = com.google.api.client.json.gson.GsonFactory.getDefaultInstance().toString(withPriority);
            org.junit.jupiter.api.Assumptions.assumeTrue(buggy.contains("ıgh"),
                    "SDK no longer lower-cases with the default locale");
            assertTrue(buggy.contains("ıgh"));
        } finally {
            java.util.Locale.setDefault(original);
        }
    }

    @Test
    void truncatesByCodePointsAndAddsAnEllipsis() {
        assertEquals("kısa", FcmMessageBuilder.truncate("kısa", 10));
        assertEquals("", FcmMessageBuilder.truncate(null, 10));

        String truncated = FcmMessageBuilder.truncate("a".repeat(400), FcmMessageBuilder.MAX_BODY_CHARS);
        assertEquals(FcmMessageBuilder.MAX_BODY_CHARS, truncated.codePointCount(0, truncated.length()));
        assertTrue(truncated.endsWith("…"));

        // Emoji (iki UTF-16 birimi) ortadan bölünmez.
        String emoji = FcmMessageBuilder.truncate("💊".repeat(50), 10);
        assertEquals(10, emoji.codePointCount(0, emoji.length()));
        assertTrue(emoji.endsWith("…"));
    }
}
