package com.supplog.service.mail;

// Mail sağlayıcısı soyutlaması. Sağlayıcı değişince (Brevo → Resend) sadece implementasyon değişir.
// Hata durumunda MailDeliveryException fırlatır.
public interface MailSender {

    void send(MailMessage message);
}
