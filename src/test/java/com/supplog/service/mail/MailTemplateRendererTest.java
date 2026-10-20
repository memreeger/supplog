package com.supplog.service.mail;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MailTemplateRendererTest {

    private final MailTemplateRenderer renderer = new MailTemplateRenderer();
    private final Map<String, String> values = Map.of(
            "firstName", "<script>x</script>",
            "resetLink", "https://supplog.vercel.app/reset-password?token=abc",
            "ttlMinutes", "30"
    );

    @Test
    void htmlEscapesValuesAndFillsEveryPlaceholder() {
        String html = renderer.renderHtml("password-reset", Locale.forLanguageTag("tr"), values);

        assertTrue(html.contains("&lt;script&gt;x&lt;/script&gt;"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("https://supplog.vercel.app/reset-password?token=abc"));
        assertTrue(html.contains("30 dakika"));
        assertFalse(html.contains("{{"));
    }

    @Test
    void picksEnglishTemplateAndFallsBackToTurkish() {
        assertTrue(renderer.renderText("password-reset", Locale.ENGLISH, values).contains("30 minutes"));
        assertTrue(renderer.renderText("password-reset", Locale.GERMAN, values).contains("30 dakika"));
    }
}
