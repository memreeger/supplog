package com.supplog.service.notification;

import com.supplog.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NudgeTextSanitizerTest {

    @Test
    void trimsAndCollapsesWhitespaceIntoOneLine() {
        assertEquals("bro rutinini almadın", NudgeTextSanitizer.sanitize("  bro \n\t rutinini    almadın  "));
    }

    @Test
    void stripsInvisibleAndControlCharacters() {
        // Sıfır genişlikli boşluk (U+200B), yön değiştirme (U+202E) ve null karakter.
        assertEquals("merhaba", NudgeTextSanitizer.sanitize("mer​ha‮ba\u0000"));
    }

    @Test
    void rejectsEmptyBlankAndTooLongText() {
        assertThrows(BusinessException.class, () -> NudgeTextSanitizer.sanitize(null));
        assertThrows(BusinessException.class, () -> NudgeTextSanitizer.sanitize("   "));
        assertThrows(BusinessException.class, () -> NudgeTextSanitizer.sanitize("​​"));
        assertThrows(BusinessException.class, () -> NudgeTextSanitizer.sanitize("a".repeat(281)));
        assertEquals(280, NudgeTextSanitizer.sanitize("a".repeat(280)).length());
    }

    @Test
    void rejectsLinks() {
        for (String text : new String[]{
                "bak https://evil.example/x", "http://a.b", "www.evil.com", "git evil.com", "EVIL.COM'a gir",
                "login: bit.ly/abc", "x.io"
        }) {
            assertThrows(BusinessException.class, () -> NudgeTextSanitizer.sanitize(text), text);
        }
    }

    @Test
    void keepsEmojiAndNormalPunctuation() {
        assertEquals("Haydi, iç şunu! 💪 (lütfen)", NudgeTextSanitizer.sanitize("Haydi, iç şunu! 💪 (lütfen)"));
        assertEquals("Saat 8.30'da aldın mı?", NudgeTextSanitizer.sanitize("Saat 8.30'da aldın mı?"));
    }
}
