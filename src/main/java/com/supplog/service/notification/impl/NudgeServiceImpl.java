package com.supplog.service.notification.impl;

import com.supplog.dto.notification.NudgeCreateDto;
import com.supplog.dto.notification.NudgeMuteResponseDto;
import com.supplog.dto.notification.NudgeResponseDto;
import com.supplog.entity.Nudge;
import com.supplog.entity.NudgeMute;
import com.supplog.entity.NotificationPreference;
import com.supplog.entity.Routine;
import com.supplog.entity.User;
import com.supplog.enums.NotificationType;
import com.supplog.enums.NudgeTemplate;
import com.supplog.enums.SupportAccessScope;
import com.supplog.enums.SupportStatus;
import com.supplog.exception.BusinessException;
import com.supplog.exception.RateLimitedException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.NotificationPreferenceRepository;
import com.supplog.repository.NudgeMuteRepository;
import com.supplog.repository.NudgeRepository;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupportRelationshipRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.notification.NotificationDraft;
import com.supplog.service.notification.NotificationParams;
import com.supplog.service.notification.NotificationService;
import com.supplog.service.notification.NudgeService;
import com.supplog.service.notification.NudgeTextSanitizer;
import com.supplog.service.user.ActiveUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NudgeServiceImpl implements NudgeService {

    // Taciz ve spam önleme: aynı rutine 10 dk'da 1, aynı kişiye saatte 5 ve günde 20 dürtme.
    static final Duration ROUTINE_COOLDOWN = Duration.ofMinutes(10);
    static final Duration PAIR_HOUR = Duration.ofHours(1);
    static final int PAIR_PER_HOUR = 5;
    static final Duration PAIR_DAY = Duration.ofDays(1);
    static final int PAIR_PER_DAY = 20;

    private final NudgeRepository nudgeRepository;
    private final NudgeMuteRepository muteRepository;
    private final RoutineRepository routineRepository;
    private final SupportRelationshipRepository supportRelationshipRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationService notificationService;
    private final ActiveUserService activeUserService;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public NudgeServiceImpl(
            NudgeRepository nudgeRepository,
            NudgeMuteRepository muteRepository,
            RoutineRepository routineRepository,
            SupportRelationshipRepository supportRelationshipRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationService notificationService,
            ActiveUserService activeUserService,
            UserRepository userRepository
    ) {
        this(nudgeRepository, muteRepository, routineRepository, supportRelationshipRepository,
                preferenceRepository, notificationService, activeUserService, userRepository, Clock.systemUTC());
    }

    // Testlerde saat enjekte edilebilsin diye.
    NudgeServiceImpl(
            NudgeRepository nudgeRepository,
            NudgeMuteRepository muteRepository,
            RoutineRepository routineRepository,
            SupportRelationshipRepository supportRelationshipRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationService notificationService,
            ActiveUserService activeUserService,
            UserRepository userRepository,
            Clock clock
    ) {
        this.nudgeRepository = nudgeRepository;
        this.muteRepository = muteRepository;
        this.routineRepository = routineRepository;
        this.supportRelationshipRepository = supportRelationshipRepository;
        this.preferenceRepository = preferenceRepository;
        this.notificationService = notificationService;
        this.activeUserService = activeUserService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public NudgeResponseDto send(Long senderUserId, NudgeCreateDto request) {
        User sender = activeUserService.getRequiredActiveUser(senderUserId);
        String templateKey = resolveTemplateKey(request);
        String message = templateKey == null ? NudgeTextSanitizer.sanitize(request.getMessage()) : null;

        Routine routine = routineRepository.findById(request.getRoutineId())
                .filter(r -> !r.isDeleted() && !r.getSupplement().isDeleted() && !r.getUser().isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("routine.not.found"));
        Long recipientId = routine.getUser().getId();

        // Yetkisiz kişiye rutinin var olup olmadığını sızdırmamak için aynı 404.
        if (recipientId.equals(senderUserId)) {
            throw new BusinessException("nudge.self.not.allowed");
        }
        boolean allowed = supportRelationshipRepository
                .findNotifiableSupporters(List.of(routine.getId()), SupportStatus.ACCEPTED, SupportAccessScope.ALL_ROUTINES)
                .stream()
                .anyMatch(target -> target.supporterUserId().equals(senderUserId));
        if (!allowed) {
            throw new ResourceNotFoundException("routine.not.found");
        }

        // Alıcı kapattıysa ya da sessize aldıysa gönderen aynı hatayı görür (sebep sızmaz).
        boolean disabled = preferenceRepository.findById(recipientId)
                .map(preference -> !preference.isNudgesEnabled()).orElse(false);
        if (disabled || muteRepository.existsByRecipientUserIdAndSenderUserId(recipientId, senderUserId)) {
            throw new BusinessException("nudge.disabled.by.recipient");
        }

        enforceRateLimits(senderUserId, recipientId, routine.getId());

        Nudge nudge = new Nudge();
        nudge.setSenderUserId(senderUserId);
        nudge.setRecipientUserId(recipientId);
        nudge.setRoutineId(routine.getId());
        nudge.setTemplateKey(templateKey);
        nudge.setMessage(message);
        nudge = nudgeRepository.save(nudge);

        notificationService.upsertForExecution(new NotificationDraft(
                recipientId,
                NotificationType.NUDGE,
                "nudge:" + nudge.getId(),
                null,
                routine.getId(),
                senderUserId,
                NotificationParams.of(
                        "senderName", sender.getFirstName(),
                        "supplementName", routine.getSupplement().getName(),
                        "templateKey", templateKey,
                        "message", message
                ),
                nudge.getId()
        ));

        return new NudgeResponseDto(nudge.getId(), nudge.getCreatedAt());
    }

    @Override
    public List<String> templateKeys() {
        return Arrays.stream(NudgeTemplate.values()).map(Enum::name).toList();
    }

    @Override
    @Transactional
    public void mute(Long recipientUserId, Long senderUserId) {
        if (recipientUserId.equals(senderUserId)) {
            throw new BusinessException("nudge.self.not.allowed");
        }
        if (userRepository.findByIdAndIsDeletedFalse(senderUserId).isEmpty()) {
            throw new ResourceNotFoundException("user.not.found", senderUserId);
        }
        if (!muteRepository.existsByRecipientUserIdAndSenderUserId(recipientUserId, senderUserId)) {
            muteRepository.save(new NudgeMute(recipientUserId, senderUserId));
        }
    }

    @Override
    @Transactional
    public void unmute(Long recipientUserId, Long senderUserId) {
        muteRepository.deleteByRecipientUserIdAndSenderUserId(recipientUserId, senderUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NudgeMuteResponseDto> listMutes(Long recipientUserId) {
        List<NudgeMute> mutes = muteRepository.findAllByRecipientUserIdOrderByCreatedAtDesc(recipientUserId);
        Map<Long, User> users = userRepository
                .findAllById(mutes.stream().map(NudgeMute::getSenderUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return mutes.stream()
                .filter(mute -> users.containsKey(mute.getSenderUserId()))
                .map(mute -> {
                    User user = users.get(mute.getSenderUserId());
                    return new NudgeMuteResponseDto(user.getId(), user.getFirstName(), user.getUsername());
                })
                .toList();
    }

    // Tam olarak biri: geçerli bir hazır şablon anahtarı ya da serbest metin.
    private String resolveTemplateKey(NudgeCreateDto request) {
        boolean hasTemplate = request.getTemplateKey() != null && !request.getTemplateKey().isBlank();
        boolean hasMessage = request.getMessage() != null && !request.getMessage().isBlank();
        if (hasTemplate == hasMessage) {
            throw new BusinessException("nudge.content.invalid");
        }
        if (!hasTemplate) {
            return null;
        }
        try {
            return NudgeTemplate.valueOf(request.getTemplateKey().trim()).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("nudge.template.invalid");
        }
    }

    private void enforceRateLimits(Long senderId, Long recipientId, Long routineId) {
        Instant now = clock.instant();
        if (nudgeRepository.countBySenderUserIdAndRoutineIdAndCreatedAtAfter(
                senderId, routineId, now.minus(ROUTINE_COOLDOWN)) >= 1
                || nudgeRepository.countBySenderUserIdAndRecipientUserIdAndCreatedAtAfter(
                senderId, recipientId, now.minus(PAIR_HOUR)) >= PAIR_PER_HOUR
                || nudgeRepository.countBySenderUserIdAndRecipientUserIdAndCreatedAtAfter(
                senderId, recipientId, now.minus(PAIR_DAY)) >= PAIR_PER_DAY) {
            throw new RateLimitedException("nudge.rate.limited");
        }
    }
}
