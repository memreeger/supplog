package com.supplog.service.auth.impl;

import com.supplog.config.AsyncConfig;
import com.supplog.service.auth.PasswordResetRequestedEvent;
import com.supplog.service.mail.MailMessage;
import com.supplog.service.mail.MailSender;
import com.supplog.service.mail.MailTemplateRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

// Mail commit sonrası ve istek thread'i dışında gider:
// rollback'te mail gitmez, Brevo yavaşlığı cevabı geciktirmez (hesap varlığı süreden anlaşılmaz).
@Slf4j
@Component
public class PasswordResetMailListener {

    private static final String TEMPLATE = "password-reset";

    private final MailSender mailSender;
    private final MailTemplateRenderer templateRenderer;
    private final MessageSource messageSource;
    private final String frontendBaseUrl;
    private final long tokenTtlMinutes;

    public PasswordResetMailListener(
            MailSender mailSender,
            MailTemplateRenderer templateRenderer,
            MessageSource messageSource,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${app.password-reset.token-ttl-minutes:30}") long tokenTtlMinutes
    ) {
        this.mailSender = mailSender;
        this.templateRenderer = templateRenderer;
        this.messageSource = messageSource;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        String resetLink = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/reset-password")
                .queryParam("token", event.rawToken())
                .build()
                .toUriString();

        Map<String, String> values = Map.of(
                "firstName", event.firstName(),
                "resetLink", resetLink,
                "ttlMinutes", Long.toString(tokenTtlMinutes)
        );

        MailMessage message = new MailMessage(
                event.email(),
                event.firstName(),
                messageSource.getMessage("mail.password-reset.subject", null, event.locale()),
                templateRenderer.renderHtml(TEMPLATE, event.locale(), values),
                templateRenderer.renderText(TEMPLATE, event.locale(), values),
                List.of("password-reset")
        );

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("Password reset mail could not be delivered", exception);
        }
    }
}
