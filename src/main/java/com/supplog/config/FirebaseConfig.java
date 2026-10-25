package com.supplog.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * Yalnızca PUSH_PROVIDER=fcm iken devreye girer. Service account JSON'u dosya olarak değil, base64 ortam
 * değişkeniyle (FIREBASE_CREDENTIALS_BASE64) verilir; böylece repoda ve imajda anahtar dosyası bulunmaz.
 * Eksik ya da bozuksa uygulama açılışta anlaşılır bir hatayla durur (değer asla loglanmaz).
 */
@Configuration
@ConditionalOnProperty(name = "app.notifications.push-provider", havingValue = "fcm")
public class FirebaseConfig {

    private static final String APP_NAME = "supplog";
    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    @Bean
    FirebaseMessaging firebaseMessaging(
            @Value("${app.notifications.firebase.credentials-base64:}") String credentialsBase64
    ) throws IOException {
        FirebaseApp app = FirebaseApp.getApps().stream()
                .filter(existing -> APP_NAME.equals(existing.getName()))
                .findFirst()
                .orElse(null);

        if (app == null) {
            app = FirebaseApp.initializeApp(
                    FirebaseOptions.builder().setCredentials(credentials(credentialsBase64)).build(),
                    APP_NAME
            );
        }
        return FirebaseMessaging.getInstance(app);
    }

    static GoogleCredentials credentials(String credentialsBase64) throws IOException {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            throw new IllegalStateException(
                    "PUSH_PROVIDER=fcm requires FIREBASE_CREDENTIALS_BASE64 (base64 of the service account JSON).");
        }
        byte[] json;
        try {
            json = Base64.getDecoder().decode(credentialsBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("FIREBASE_CREDENTIALS_BASE64 is not valid base64.");
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(json)) {
            return GoogleCredentials.fromStream(in).createScoped(List.of(MESSAGING_SCOPE));
        } catch (IOException e) {
            throw new IllegalStateException("FIREBASE_CREDENTIALS_BASE64 does not contain a valid service account JSON.");
        }
    }
}
