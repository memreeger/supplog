package com.supplog.service.mail.impl;

import com.supplog.service.mail.MailDeliveryException;
import com.supplog.service.mail.MailMessage;
import com.supplog.service.mail.MailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Brevo transactional e-mail HTTP API (SMTP portları Railway'de kapalı olabilir; HTTPS her zaman açık).
@Slf4j
public class BrevoMailSender implements MailSender {

    static final String SEND_PATH = "/v3/smtp/email";
    private static final int MAX_ATTEMPTS = 2;

    private final RestClient restClient;
    private final String fromAddress;
    private final String fromName;

    public BrevoMailSender(
            RestClient.Builder restClientBuilder,
            String baseUrl,
            String apiKey,
            String fromAddress,
            String fromName
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.mail.brevo.api-key (BREVO_API_KEY) is required when app.mail.provider=brevo");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("app.mail.from-address (MAIL_FROM_ADDRESS) is required when app.mail.provider=brevo");
        }

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("api-key", apiKey)
                .build();
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void send(MailMessage message) {
        Map<String, Object> body = requestBody(message);

        for (int attempt = 1; ; attempt++) {
            try {
                restClient.post()
                        .uri(SEND_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientResponseException exception) {
                HttpStatusCode status = exception.getStatusCode();
                // 4xx: istek/hesap hatası, tekrar denemek işe yaramaz.
                if (status.is4xxClientError() || attempt >= MAX_ATTEMPTS) {
                    log.warn("Brevo rejected mail, status={} body={}", status.value(), exception.getResponseBodyAsString());
                    throw new MailDeliveryException("Brevo responded with " + status.value(), exception);
                }
            } catch (ResourceAccessException exception) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new MailDeliveryException("Brevo is unreachable", exception);
                }
            }
            log.info("Retrying Brevo mail delivery, attempt={}", attempt + 1);
        }
    }

    private Map<String, Object> requestBody(MailMessage message) {
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("email", message.to());
        if (message.toName() != null && !message.toName().isBlank()) {
            recipient.put("name", message.toName());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", Map.of("email", fromAddress, "name", fromName));
        body.put("to", List.of(recipient));
        body.put("subject", message.subject());
        body.put("htmlContent", message.htmlBody());
        body.put("textContent", message.textBody());
        if (message.tags() != null && !message.tags().isEmpty()) {
            body.put("tags", message.tags());
        }
        return body;
    }
}
