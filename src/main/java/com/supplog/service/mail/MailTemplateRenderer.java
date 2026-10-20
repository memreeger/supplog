package com.supplog.service.mail;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// classpath:mail/<name>_<lang>.html|txt şablonlarını {{key}} değiştirerek doldurur.
// HTML şablonda değerler escape edilir (kullanıcı adı gibi girdiler HTML olarak yorumlanmasın).
@Component
public class MailTemplateRenderer {

    private static final String DEFAULT_LANGUAGE = "tr";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String renderHtml(String name, Locale locale, Map<String, String> values) {
        return render(load(name, locale, "html"), values, true);
    }

    public String renderText(String name, Locale locale, Map<String, String> values) {
        return render(load(name, locale, "txt"), values, false);
    }

    private String load(String name, Locale locale, String extension) {
        String language = "en".equals(locale.getLanguage()) ? "en" : DEFAULT_LANGUAGE;
        String path = "mail/" + name + "_" + language + "." + extension;

        return cache.computeIfAbsent(path, key -> {
            try {
                return new ClassPathResource(key).getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new UncheckedIOException("Mail template not found: " + key, exception);
            }
        });
    }

    private static String render(String template, Map<String, String> values, boolean escapeHtml) {
        String result = template;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace(
                    "{{" + entry.getKey() + "}}",
                    escapeHtml ? HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name()) : value
            );
        }

        return result;
    }
}
