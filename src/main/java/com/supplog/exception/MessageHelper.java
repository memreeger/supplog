package com.supplog.exception;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.context.MessageSource;

@Component
public class MessageHelper {
    private final MessageSource messageResource;

    public MessageHelper(MessageSource messageResource) {
        this.messageResource = messageResource;
    }

    public String getMessage(String code, Object... args){
        return messageResource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
