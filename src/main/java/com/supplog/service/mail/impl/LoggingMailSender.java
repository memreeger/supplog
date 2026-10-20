package com.supplog.service.mail.impl;

import com.supplog.service.mail.MailMessage;
import com.supplog.service.mail.MailSender;
import lombok.extern.slf4j.Slf4j;

// Geliştirme ve test: mail gönderilmez, metin gövdesi loglanır (reset linki görünür).
@Slf4j
public class LoggingMailSender implements MailSender {

    @Override
    public void send(MailMessage message) {
        log.info(
                "[mail:log] to={} subject=\"{}\" tags={}\n{}",
                message.to(),
                message.subject(),
                message.tags(),
                message.textBody()
        );
    }
}
