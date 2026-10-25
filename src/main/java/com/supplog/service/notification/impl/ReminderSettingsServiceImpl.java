package com.supplog.service.notification.impl;

import com.supplog.dto.notification.ReminderSettingsDto;
import com.supplog.entity.RoutineReminderSetting;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineReminderSettingRepository;
import com.supplog.repository.RoutineRepository;
import com.supplog.service.notification.ReminderSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReminderSettingsServiceImpl implements ReminderSettingsService {

    // Veritabanındaki CHECK kısıtıyla aynı küme.
    static final Set<Integer> ALLOWED_LEAD_MINUTES = Set.of(10, 20, 30, 60);

    private final RoutineReminderSettingRepository settingRepository;
    private final RoutineRepository routineRepository;

    @Override
    @Transactional(readOnly = true)
    public ReminderSettingsDto get(Long userId, Long routineId) {
        requireOwnedRoutine(userId, routineId);
        RoutineReminderSetting setting = settingRepository.findById(routineId)
                .orElseGet(() -> new RoutineReminderSetting(routineId));
        return toDto(setting);
    }

    @Override
    @Transactional
    public ReminderSettingsDto update(Long userId, Long routineId, ReminderSettingsDto request) {
        requireOwnedRoutine(userId, routineId);
        if (!ALLOWED_LEAD_MINUTES.contains(request.getLeadMinutes())) {
            throw new BusinessException("reminder.lead.invalid");
        }

        RoutineReminderSetting setting = settingRepository.findById(routineId)
                .orElseGet(() -> new RoutineReminderSetting(routineId));
        setting.setEnabled(request.getEnabled());
        setting.setLeadMinutes(request.getLeadMinutes());
        return toDto(settingRepository.save(setting));
    }

    // Başkasının (ya da silinmiş) rutini "bulunamadı" gibi davranır.
    private void requireOwnedRoutine(Long userId, Long routineId) {
        if (routineRepository.findByIdAndUserIdAndIsDeletedFalse(routineId, userId).isEmpty()) {
            throw new ResourceNotFoundException("routine.not.found");
        }
    }

    private ReminderSettingsDto toDto(RoutineReminderSetting setting) {
        return new ReminderSettingsDto(setting.isEnabled(), setting.getLeadMinutes());
    }
}
