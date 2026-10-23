package com.supplog.service.notification;

import com.supplog.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * Serbest dürtme metnini temizler ve doğrular. Metin başkasının telefonunda bildirim olarak görüneceği için
 * tek satır, kontrol/görünmez karakter yok, link yok (kimlik avı ve spam riski), en çok 280 karakter.
 */
public final class NudgeTextSanitizer {

    public static final int MAX_LENGTH = 280;

    private static final Pattern LINEBREAKS_AND_TABS = Pattern.compile("[\\r\\n\\t]+");
    // Kontrol (Cc) ve biçim (Cf: sıfır genişlikli boşluk, yön değiştirme vb.) karakterleri.
    private static final Pattern INVISIBLE = Pattern.compile("[\\p{Cc}\\p{Cf}]");
    private static final Pattern MULTI_SPACE = Pattern.compile(" {2,}");
    private static final Pattern LINK = Pattern.compile(
            "(?i)(https?://|www\\.|\\b[a-z0-9-]+\\.(com|net|org|io|app|me|ly|co|tr|ru|xyz|info|biz|dev)\\b)");

    private NudgeTextSanitizer() {
    }

    public static String sanitize(String raw) {
        if (raw == null) {
            throw new BusinessException("nudge.message.invalid");
        }
        String text = LINEBREAKS_AND_TABS.matcher(raw).replaceAll(" ");
        text = INVISIBLE.matcher(text).replaceAll("");
        text = MULTI_SPACE.matcher(text).replaceAll(" ").trim();

        if (text.isEmpty() || text.codePointCount(0, text.length()) > MAX_LENGTH || LINK.matcher(text).find()) {
            throw new BusinessException("nudge.message.invalid");
        }
        return text;
    }
}
