package com.supplog.service.support.impl;

import com.supplog.dto.support.*;
import com.supplog.entity.Routine;
import com.supplog.entity.SupportRelationship;
import com.supplog.entity.SupportRoutinePermission;
import com.supplog.entity.User;
import com.supplog.enums.SupportAccessScope;
import com.supplog.enums.SupportStatus;
import com.supplog.exception.BusinessException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupportRelationshipRepository;
import com.supplog.repository.SupportRoutinePermissionRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.support.SupportService;
import com.supplog.service.user.ActiveUserService;
import com.supplog.util.InputNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {

    private final SupportRelationshipRepository supportRelationshipRepository;
    private final SupportRoutinePermissionRepository supportRoutinePermissionRepository;
    private final UserRepository userRepository;
    private final RoutineRepository routineRepository;
    private final ActiveUserService activeUserService;

    @Override
    @Transactional
    public SupportRequestResponseDto sendRequest(
            Long currentUserId,
            SupportRequestCreateDto request
    ) {

        User currentUser = activeUserService
                .getRequiredActiveUser(currentUserId);

        String identifier =
                InputNormalizer.normalizeIdentifier(
                        request.getIdentifier()
                );

        User targetUser = userRepository
                .findByUsernameOrEmail(
                        identifier,
                        identifier
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "support.user.not.found"
                        )
                );

        if (targetUser.isDeleted()) {
            throw new BusinessException(
                    "support.user.not.found"
            );
        }

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BusinessException(
                    "support.request.self.not.allowed"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        return supportRelationshipRepository
                .findPairForUpdate(
                        currentUser.getId(),
                        targetUser.getId()
                )
                .map(existingRelationship ->
                        resendRequest(
                                existingRelationship,
                                now
                        )
                )
                .orElseGet(() ->
                        createRequest(
                                currentUser,
                                targetUser,
                                now
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportRequestResponseDto> getIncomingRequests(
            Long currentUserId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        return supportRelationshipRepository
                .findAllBySupporterIdAndStatusOrderByRequestedAtDesc(
                        currentUserId,
                        SupportStatus.PENDING
                )
                .stream()
                .filter(this::hasActiveParticipants)
                .map(this::toRequestResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportRequestResponseDto> getOutgoingRequests(
            Long currentUserId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        return supportRelationshipRepository
                .findAllBySupportedUserIdAndStatusOrderByRequestedAtDesc(
                        currentUserId,
                        SupportStatus.PENDING
                )
                .stream()
                .filter(this::hasActiveParticipants)
                .map(this::toRequestResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public SupportConnectionResponseDto acceptRequest(
            Long currentUserId,
            Long relationshipId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        SupportRelationship candidate = supportRelationshipRepository
                .findByIdAndSupporterIdAndStatus(
                        relationshipId,
                        currentUserId,
                        SupportStatus.PENDING
                )
                .orElseThrow(() -> new BusinessException("support.request.not.available"));

        lockUsersInIdOrder(
                candidate.getSupportedUser().getId(),
                candidate.getSupporter().getId()
        );

        SupportRelationship relationship =
                supportRelationshipRepository
                        .findSupporterRelationshipForUpdate(
                                relationshipId,
                                currentUserId,
                                SupportStatus.PENDING
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "support.request.not.available"
                                )
                        );

        ensureActiveParticipants(relationship);

        transitionToAccepted(
                relationship,
                LocalDateTime.now()
        );

        return toConnectionResponseDto(
                relationship
        );
    }

    @Override
    @Transactional
    public SupportRequestResponseDto rejectRequest(
            Long currentUserId,
            Long relationshipId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        SupportRelationship relationship =
                supportRelationshipRepository
                        .findSupporterRelationshipForUpdate(
                                relationshipId,
                                currentUserId,
                                SupportStatus.PENDING
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "support.request.not.available"
                                )
                        );

        transitionToRejected(
                relationship,
                LocalDateTime.now()
        );

        return toRequestResponseDto(
                relationship
        );
    }

    @Override
    @Transactional
    public SupportRequestResponseDto cancelRequest(
            Long currentUserId,
            Long relationshipId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        SupportRelationship relationship =
                supportRelationshipRepository
                        .findOwnerRelationshipForUpdate(
                                relationshipId,
                                currentUserId,
                                SupportStatus.PENDING
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "support.request.not.available"
                                )
                        );

        transitionToCancelled(
                relationship
        );

        return toRequestResponseDto(
                relationship
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportConnectionResponseDto> getMySupporters(
            Long currentUserId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        return supportRelationshipRepository
                .findAllBySupportedUserIdAndStatusOrderByRequestedAtDesc(
                        currentUserId,
                        SupportStatus.ACCEPTED
                )
                .stream()
                .filter(this::hasActiveParticipants)
                .map(this::toConnectionResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportConnectionResponseDto> getSupportedUsers(
            Long currentUserId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        return supportRelationshipRepository
                .findAllBySupporterIdAndStatusOrderByRequestedAtDesc(
                        currentUserId,
                        SupportStatus.ACCEPTED
                )
                .stream()
                .filter(this::hasActiveParticipants)
                .map(this::toConnectionResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void revokeRelationship(
            Long currentUserId,
            Long relationshipId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        SupportRelationship relationship =
                supportRelationshipRepository
                        .findOwnerRelationshipForUpdate(
                                relationshipId,
                                currentUserId,
                                SupportStatus.ACCEPTED
                        )
                        .or(() ->
                                supportRelationshipRepository
                                        .findSupporterRelationshipForUpdate(
                                                relationshipId,
                                                currentUserId,
                                                SupportStatus.ACCEPTED
                                        )
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "support.relationship.not.available"
                                )
                        );

        transitionToRevoked(
                relationship,
                LocalDateTime.now()
        );
    }

    @Override
    @Transactional
    public SupportConnectionResponseDto updateAccessScope(
            Long currentUserId,
            Long relationshipId,
            SupportAccessUpdateDto request
    ) {

        activeUserService.requireActiveUser(currentUserId);


        SupportRelationship relationship =
                getAcceptedRelationshipForOwnerForUpdate(
                        currentUserId,
                        relationshipId
                );

        SupportAccessScope newScope =
                request.getAccessScope();

        if (relationship.getAccessScope() == newScope) {
            return toConnectionResponseDto(
                    relationship
            );
        }

        supportRoutinePermissionRepository
                .deleteAllBySupportRelationshipId(
                        relationship.getId()
                );

        relationship.setAccessScope(newScope);

        return toConnectionResponseDto(
                relationship
        );
    }

    @Override
    @Transactional
    public SupportConnectionResponseDto updateSelectedRoutines(
            Long currentUserId,
            Long relationshipId,
            SupportRoutineSelectionUpdateDto request
    ) {

        activeUserService.requireActiveUser(currentUserId);

        SupportRelationship relationship =
                getAcceptedRelationshipForOwnerForUpdate(
                        currentUserId,
                        relationshipId
                );

        if (relationship.getAccessScope()
                != SupportAccessScope.SELECTED_ROUTINES) {

            throw new BusinessException(
                    "support.permission.requires.selected.scope"
            );
        }

        Set<Long> routineIds = request.getRoutineIds();

        if (routineIds.isEmpty()) {

            supportRoutinePermissionRepository
                    .deleteAllBySupportRelationshipId(
                            relationshipId
                    );

            return toConnectionResponseDto(
                    relationship
            );
        }

        List<Routine> routines =
                routineRepository
                        .findAllByIdInAndUserIdAndIsDeletedFalse(
                                routineIds,
                                currentUserId
                        );

        if (routines.size() != routineIds.size()) {

            throw new BusinessException(
                    "support.routine.not.available"
            );
        }

        supportRoutinePermissionRepository
                .deleteAllBySupportRelationshipId(
                        relationshipId
                );

        List<SupportRoutinePermission> permissions =
                routines.stream()
                        .map(routine -> {

                            SupportRoutinePermission permission =
                                    new SupportRoutinePermission();

                            permission.setSupportRelationship(
                                    relationship
                            );

                            permission.setRoutine(
                                    routine
                            );

                            return permission;
                        })
                        .toList();

        supportRoutinePermissionRepository
                .saveAll(permissions);

        return toConnectionResponseDto(
                relationship
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportRoutineResponseDto> getSharedRoutines(
            Long currentUserId,
            Long relationshipId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        SupportRelationship relationship =
                getAcceptedRelationshipForSupporter(
                        currentUserId,
                        relationshipId
                );

        List<Routine> routines =
                switch (relationship.getAccessScope()) {

                    case ALL_ROUTINES -> getAllSharedRoutines(
                            relationship
                    );

                    case SELECTED_ROUTINES -> getSelectedSharedRoutines(
                            relationship
                    );
                };

        return routines.stream()
                .map(this::toSupportRoutineResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void handleUserDeactivation(Long userId) {

        LocalDateTime now = LocalDateTime.now();

        supportRelationshipRepository
                .findAllForUserAndStatusesForUpdate(
                        userId,
                        List.of(SupportStatus.ACCEPTED, SupportStatus.PENDING)
                )
                .forEach(relationship -> {
                    if (relationship.getStatus() == SupportStatus.ACCEPTED) {
                        transitionToRevoked(relationship, now);
                    } else {
                        transitionToCancelled(relationship);
                    }
                });
    }

    private void lockUsersInIdOrder(Long firstUserId, Long secondUserId) {
        List.of(firstUserId, secondUserId).stream()
                .distinct()
                .sorted()
                .forEach(userId -> userRepository.findByIdForUpdate(userId)
                        .orElseThrow(() -> new BusinessException("support.user.not.found")));
    }

    @Override
    @Transactional
    public void handleRoutineSoftDelete(Long routineId) {

        supportRoutinePermissionRepository
                .deleteAllByRoutineId(routineId);
    }


    @Override
    @Transactional(readOnly = true)
    public SupportRoutineSelectionResponseDto getRoutineSelection(
            Long currentUserId,
            Long relationshipId
    ) {

        activeUserService.requireActiveUser(currentUserId);

        SupportRelationship relationship =
                getAcceptedRelationshipForOwner(
                        currentUserId,
                        relationshipId
                );

        if (relationship.getAccessScope()
                == SupportAccessScope.ALL_ROUTINES) {

            return new SupportRoutineSelectionResponseDto(
                    SupportAccessScope.ALL_ROUTINES,
                    Set.of()
            );
        }

        Set<Long> routineIds =
                supportRoutinePermissionRepository
                        .findAllBySupportRelationshipIdAndRoutineIsDeletedFalse(
                                relationshipId
                        )
                        .stream()
                        .map(permission ->
                                permission
                                        .getRoutine()
                                        .getId()
                        )
                        .collect(Collectors.toSet());

        return new SupportRoutineSelectionResponseDto(
                SupportAccessScope.SELECTED_ROUTINES,
                routineIds
        );
    }




    // HELPER
    private SupportRequestResponseDto createRequest(
            User supportedUser,
            User supporter,
            LocalDateTime now
    ) {

        SupportRelationship relationship =
                new SupportRelationship();

        relationship.setSupportedUser(supportedUser);
        relationship.setSupporter(supporter);

        relationship.setStatus(
                SupportStatus.PENDING
        );

        relationship.setAccessScope(
                SupportAccessScope.SELECTED_ROUTINES
        );

        relationship.setRequestedAt(now);
        relationship.setRespondedAt(null);
        relationship.setRevokedAt(null);

        SupportRelationship saved =
                supportRelationshipRepository.save(
                        relationship
                );

        return toRequestResponseDto(saved);
    }

    private SupportRequestResponseDto resendRequest(
            SupportRelationship relationship,
            LocalDateTime now
    ) {

        switch (relationship.getStatus()) {

            case PENDING -> throw new BusinessException(
                    "support.request.already.pending"
            );

            case ACCEPTED -> throw new BusinessException(
                    "support.relationship.already.accepted"
            );

            case REJECTED, CANCELLED, REVOKED -> {

                transitionToPendingForReinvite(
                        relationship,
                        now
                );

                return toRequestResponseDto(
                        relationship
                );
            }
        }

        throw new BusinessException(
                "support.request.invalid.state"
        );
    }

    private SupportRequestResponseDto toRequestResponseDto(
            SupportRelationship relationship
    ) {

        return new SupportRequestResponseDto(
                relationship.getId(),

                toUserSummaryDto(
                        relationship.getSupportedUser()
                ),

                toUserSummaryDto(
                        relationship.getSupporter()
                ),

                relationship.getStatus(),
                relationship.getCreatedAt(),
                relationship.getRequestedAt(),
                relationship.getRespondedAt()
        );
    }

    private SupportUserSummaryDto toUserSummaryDto(
            User user
    ) {

        return new SupportUserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    private SupportConnectionResponseDto toConnectionResponseDto(
            SupportRelationship relationship
    ) {

        return new SupportConnectionResponseDto(
                relationship.getId(),

                toUserSummaryDto(
                        relationship.getSupportedUser()
                ),

                toUserSummaryDto(
                        relationship.getSupporter()
                ),

                relationship.getStatus(),
                relationship.getAccessScope(),
                relationship.getCreatedAt(),
                relationship.getRespondedAt()
        );
    }

    private List<Routine> getAllSharedRoutines(
            SupportRelationship relationship
    ) {

        if (relationship.getAccessScope()
                != SupportAccessScope.ALL_ROUTINES) {

            throw new BusinessException(
                    "support.access.scope.invalid"
            );
        }

        return routineRepository
                .findAllByUserIdAndIsDeletedFalseAndSupplementIsDeletedFalse(
                        relationship
                                .getSupportedUser()
                                .getId()
                );
    }

    private List<Routine> getSelectedSharedRoutines(
            SupportRelationship relationship
    ) {

        if (relationship.getAccessScope()
                != SupportAccessScope.SELECTED_ROUTINES) {

            throw new BusinessException(
                    "support.access.scope.invalid"
            );
        }

        return supportRoutinePermissionRepository
                .findAllBySupportRelationshipIdAndRoutineIsDeletedFalse(
                        relationship.getId()
                )
                .stream()
                .map(SupportRoutinePermission::getRoutine)
                .filter(routine -> !routine.getSupplement().isDeleted())
                .toList();
    }

    private SupportRelationship getAcceptedRelationshipForSupporter(
            Long currentUserId,
            Long relationshipId
    ) {

        SupportRelationship relationship = supportRelationshipRepository
                .findByIdAndSupporterIdAndStatus(
                        relationshipId,
                        currentUserId,
                        SupportStatus.ACCEPTED
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "support.relationship.not.available"
                        )
                );

        ensureActiveParticipants(relationship);
        return relationship;
    }

    private SupportRoutineResponseDto toSupportRoutineResponseDto(
            Routine routine
    ) {

        return new SupportRoutineResponseDto(
                routine.getId(),
                routine.getSupplement().getName(),
                routine.getSupplement().getSuppDosage(),
                routine.getFrequency(),
                routine.getDaysOfWeek(),
                routine.getDayOfMonth(),
                routine.getRoutineTime(),
                routine.getDurationType(),
                routine.getStartDate(),
                routine.getEndDate()
        );
    }

    private SupportRelationship getAcceptedRelationshipForOwner(
            Long currentUserId,
            Long relationshipId
    ) {

        SupportRelationship relationship = supportRelationshipRepository
                .findByIdAndSupportedUserIdAndStatus(
                        relationshipId,
                        currentUserId,
                        SupportStatus.ACCEPTED
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "support.relationship.not.available"
                        )
                );

        ensureActiveParticipants(relationship);
        return relationship;
    }

    private SupportRelationship getAcceptedRelationshipForOwnerForUpdate(
            Long currentUserId,
            Long relationshipId
    ) {
        SupportRelationship relationship = supportRelationshipRepository
                .findOwnerRelationshipForUpdate(
                        relationshipId,
                        currentUserId,
                        SupportStatus.ACCEPTED
                )
                .orElseThrow(() -> new BusinessException(
                        "support.relationship.not.available"
                ));

        ensureActiveParticipants(relationship);
        return relationship;
    }

    private boolean hasActiveParticipants(SupportRelationship relationship) {
        return !relationship.getSupportedUser().isDeleted()
                && !relationship.getSupporter().isDeleted();
    }

    private void ensureActiveParticipants(SupportRelationship relationship) {
        if (!hasActiveParticipants(relationship)) {
            throw new BusinessException(
                    "support.relationship.not.available"
            );
        }
    }


    private void transitionToPendingForReinvite(
            SupportRelationship relationship,
            LocalDateTime now
    ) {

        supportRoutinePermissionRepository
                .deleteAllBySupportRelationshipId(
                        relationship.getId()
                );

        relationship.setStatus(
                SupportStatus.PENDING
        );

        relationship.setAccessScope(
                SupportAccessScope.SELECTED_ROUTINES
        );

        relationship.setRequestedAt(now);
        relationship.setRespondedAt(null);
        relationship.setRevokedAt(null);
    }

    private void transitionToAccepted(
            SupportRelationship relationship,
            LocalDateTime now
    ) {

        supportRoutinePermissionRepository
                .deleteAllBySupportRelationshipId(
                        relationship.getId()
                );

        relationship.setStatus(
                SupportStatus.ACCEPTED
        );

        relationship.setAccessScope(
                SupportAccessScope.SELECTED_ROUTINES
        );

        relationship.setRespondedAt(now);
        relationship.setRevokedAt(null);
    }

    private void transitionToRejected(
            SupportRelationship relationship,
            LocalDateTime now
    ) {

        supportRoutinePermissionRepository
                .deleteAllBySupportRelationshipId(
                        relationship.getId()
                );

        relationship.setStatus(
                SupportStatus.REJECTED
        );

        relationship.setAccessScope(
                SupportAccessScope.SELECTED_ROUTINES
        );

        relationship.setRespondedAt(now);
        relationship.setRevokedAt(null);
    }

    private void transitionToCancelled(
            SupportRelationship relationship
    ) {

        supportRoutinePermissionRepository
                .deleteAllBySupportRelationshipId(
                        relationship.getId()
                );

        relationship.setStatus(
                SupportStatus.CANCELLED
        );

        relationship.setAccessScope(
                SupportAccessScope.SELECTED_ROUTINES
        );

        relationship.setRespondedAt(null);
        relationship.setRevokedAt(null);
    }

    private void transitionToRevoked(
            SupportRelationship relationship,
            LocalDateTime now
    ) {

        supportRoutinePermissionRepository
                .deleteAllBySupportRelationshipId(
                        relationship.getId()
                );

        relationship.setStatus(
                SupportStatus.REVOKED
        );

        relationship.setAccessScope(
                SupportAccessScope.SELECTED_ROUTINES
        );

        relationship.setRevokedAt(now);
    }
}
