package com.supplog.service.mail.impl;

import com.supplog.service.mail.MailDeliveryException;
import com.supplog.service.mail.MailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BrevoMailSenderTest {

    private static final String URL = "https://brevo.test" + BrevoMailSender.SEND_PATH;
    private static final MailMessage MESSAGE = new MailMessage(
            "user@example.test", "User", "Subject", "<p>Hi</p>", "Hi", List.of("password-reset")
    );

    private MockRestServiceServer server;
    private BrevoMailSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new BrevoMailSender(builder, "https://brevo.test", "test-key", "noreply@example.test", "Supplog");
    }

    @Test
    void sendsTransactionalMailWithApiKey() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(jsonPath("$.sender.email").value("noreply@example.test"))
                .andExpect(jsonPath("$.sender.name").value("Supplog"))
                .andExpect(jsonPath("$.to[0].email").value("user@example.test"))
                .andExpect(jsonPath("$.subject").value("Subject"))
                .andExpect(jsonPath("$.htmlContent").value("<p>Hi</p>"))
                .andExpect(jsonPath("$.textContent").value("Hi"))
                .andExpect(jsonPath("$.tags[0]").value("password-reset"))
                .andRespond(withStatus(HttpStatus.CREATED).body("{\"messageId\":\"<id>\"}"));

        sender.send(MESSAGE);

        server.verify();
    }

    @Test
    void clientErrorIsNotRetried() {
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"code\":\"unauthorized\"}"));

        assertThrows(MailDeliveryException.class, () -> sender.send(MESSAGE));
        server.verify();
    }

    @Test
    void serverErrorIsRetriedOnce() {
        server.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withServerError());
        server.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withStatus(HttpStatus.CREATED));

        sender.send(MESSAGE);
        server.verify();
    }

    @Test
    void missingApiKeyFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> new BrevoMailSender(RestClient.builder(), "https://brevo.test", " ", "a@b.test", "Supplog"));
    }
}
