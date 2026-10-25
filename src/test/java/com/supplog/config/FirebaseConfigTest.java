package com.supplog.config;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class FirebaseConfigTest {

    @Test
    void missingCredentialsStopTheAppWithAClearMessage() {
        for (String value : new String[]{null, "", "   "}) {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> FirebaseConfig.credentials(value));
            assertTrue(e.getMessage().contains("FIREBASE_CREDENTIALS_BASE64"));
        }
    }

    @Test
    void invalidBase64AndInvalidJsonAreRejectedWithoutEchoingTheValue() {
        String secretLooking = "not-base64-but-secret!";
        IllegalStateException notBase64 = assertThrows(IllegalStateException.class, () -> FirebaseConfig.credentials(secretLooking));
        assertFalse(notBase64.getMessage().contains(secretLooking));

        String notJson = Base64.getEncoder().encodeToString("hello".getBytes());
        IllegalStateException badJson = assertThrows(IllegalStateException.class, () -> FirebaseConfig.credentials(notJson));
        assertFalse(badJson.getMessage().contains(notJson));
    }
}
