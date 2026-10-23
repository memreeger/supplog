package com.supplog.service.notification.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FcmPushSenderTest {

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FcmPushSender sender = new FcmPushSender(messaging);
    private final PushPayload payload = new PushPayload("t", "b", Map.of("type", "NUDGE"), "tag", "nudges");

    private FirebaseMessagingException failure(MessagingErrorCode code) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        return exception;
    }

    @Test
    void successIsReported() throws Exception {
        when(messaging.send(any(Message.class))).thenReturn("projects/x/messages/1");

        assertEquals(PushOutcome.SENT, sender.send("token", payload));
    }

    @Test
    void deadTokensAreReportedAsInvalid() throws Exception {
        for (MessagingErrorCode code : new MessagingErrorCode[]{
                MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT, MessagingErrorCode.SENDER_ID_MISMATCH}) {
            FirebaseMessagingException exception = failure(code);
            when(messaging.send(any(Message.class))).thenThrow(exception);

            assertEquals(PushOutcome.INVALID_TOKEN, sender.send("token", payload), code.name());
        }
    }

    @Test
    void temporaryErrorsKeepTheToken() throws Exception {
        for (MessagingErrorCode code : new MessagingErrorCode[]{
                MessagingErrorCode.UNAVAILABLE, MessagingErrorCode.QUOTA_EXCEEDED,
                MessagingErrorCode.INTERNAL, MessagingErrorCode.THIRD_PARTY_AUTH_ERROR}) {
            FirebaseMessagingException exception = failure(code);
            when(messaging.send(any(Message.class))).thenThrow(exception);

            assertEquals(PushOutcome.FAILED, sender.send("token", payload), code.name());
        }
    }

    @Test
    void unexpectedRuntimeErrorsDoNotEscape() throws Exception {
        when(messaging.send(any(Message.class))).thenThrow(new IllegalStateException("boom"));

        assertEquals(PushOutcome.FAILED, sender.send("token", payload));
    }
}
