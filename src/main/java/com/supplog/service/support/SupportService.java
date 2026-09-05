package com.supplog.service.support;

import com.supplog.dto.support.*;

import java.util.List;

public interface SupportService {

    SupportRequestResponseDto sendRequest(
            Long currentUserId,
            SupportRequestCreateDto request
    );

    List<SupportRequestResponseDto> getIncomingRequests(
            Long currentUserId
    );

    List<SupportRequestResponseDto> getOutgoingRequests(
            Long currentUserId
    );

    SupportConnectionResponseDto acceptRequest(
            Long currentUserId,
            Long relationshipId
    );

    SupportRequestResponseDto rejectRequest(
            Long currentUserId,
            Long relationshipId
    );

    SupportRequestResponseDto cancelRequest(
            Long currentUserId,
            Long relationshipId
    );

    List<SupportConnectionResponseDto> getMySupporters(
            Long currentUserId
    );

    List<SupportConnectionResponseDto> getSupportedUsers(
            Long currentUserId
    );

    void revokeRelationship(
            Long currentUserId,
            Long relationshipId
    );

    SupportConnectionResponseDto updateAccessScope(
            Long currentUserId,
            Long relationshipId,
            SupportAccessUpdateDto request
    );

    SupportConnectionResponseDto updateSelectedRoutines(
            Long currentUserId,
            Long relationshipId,
            SupportRoutineSelectionUpdateDto request
    );

    List<SupportRoutineResponseDto> getSharedRoutines(
            Long currentUserId,
            Long relationshipId
    );

    void handleUserDeactivation(Long userId);

    void handleRoutineSoftDelete(Long routineId);

    SupportRoutineSelectionResponseDto getRoutineSelection(
            Long currentUserId,
            Long relationshipId
    );
}