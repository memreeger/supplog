package com.supplog.service.notification;

import com.supplog.dto.notification.NudgeCreateDto;
import com.supplog.dto.notification.NudgeMuteResponseDto;
import com.supplog.dto.notification.NudgeResponseDto;

import java.util.List;

public interface NudgeService {

    // Destekçi, izinli bir rutinin sahibine dürtme gönderir. Sahip bildirim kutusunda görür.
    NudgeResponseDto send(Long senderUserId, NudgeCreateDto request);

    List<String> templateKeys();

    // Sahip, belirli bir destekçiyi sessize alır / sessizden çıkarır (idempotent).
    void mute(Long recipientUserId, Long senderUserId);

    void unmute(Long recipientUserId, Long senderUserId);

    List<NudgeMuteResponseDto> listMutes(Long recipientUserId);
}
