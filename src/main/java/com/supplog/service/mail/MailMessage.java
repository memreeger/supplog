package com.supplog.service.mail;

import java.util.List;

// Sağlayıcıdan bağımsız mail içeriği.
public record MailMessage(
        String to,
        String toName,
        String subject,
        String htmlBody,
        String textBody,
        List<String> tags
) {
}
