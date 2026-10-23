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
