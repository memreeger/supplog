package com.supplog.config;

import com.supplog.service.mail.MailSender;
import com.supplog.service.mail.impl.BrevoMailSender;
import com.supplog.service.mail.impl.LoggingMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// app.mail.provider: log (varsayılan, mail gönderilmez) | brevo
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
    MailSender brevoMailSender(
            @Value("${app.mail.brevo.base-url:https://api.brevo.com}") String baseUrl,
            @Value("${app.mail.brevo.api-key:}") String apiKey,
            @Value("${app.mail.from-address:}") String fromAddress,
            @Value("${app.mail.from-name:Supplog}") String fromName
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return new BrevoMailSender(
                RestClient.builder().requestFactory(requestFactory),
                baseUrl,
                apiKey,
                fromAddress,
                fromName
        );
    }

    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "log", matchIfMissing = true)
    MailSender loggingMailSender() {
        return new LoggingMailSender();
    }
}
