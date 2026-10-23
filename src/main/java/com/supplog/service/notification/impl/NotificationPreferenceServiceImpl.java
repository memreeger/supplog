package com.supplog.service.notification.impl;

import com.supplog.dto.notification.NotificationPreferenceDto;
import com.supplog.entity.NotificationPreference;
import com.supplog.repository.NotificationPreferenceRepository;
import com.supplog.service.notification.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceDto get(Long userId) {
        NotificationPreference preference = preferenceRepository.findById(userId)
                .orElseGet(() -> new NotificationPreference(userId));
        return toDto(preference);
    }

    @Override
    @Transactional
    public NotificationPreferenceDto update(Long userId, NotificationPreferenceDto request) {
        NotificationPreference preference = preferenceRepository.findById(userId)
                .orElseGet(() -> new NotificationPreference(userId));

        preference.setPushEnabled(request.getPushEnabled());
        preference.setRemindersEnabled(request.getRemindersEnabled());
        preference.setSupporterAlertsEnabled(request.getSupporterAlertsEnabled());
        preference.setNudgesEnabled(request.getNudgesEnabled());
        preference.setHideDetailsInPush(request.getHideDetailsInPush());

        return toDto(preferenceRepository.save(preference));
    }

    private NotificationPreferenceDto toDto(NotificationPreference p) {
        return new NotificationPreferenceDto(
                p.isPushEnabled(),
                p.isRemindersEnabled(),
                p.isSupporterAlertsEnabled(),
                p.isNudgesEnabled(),
                p.isHideDetailsInPush()
        );
    }
}
