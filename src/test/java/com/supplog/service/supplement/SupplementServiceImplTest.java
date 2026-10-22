package com.supplog.service.supplement;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.entity.Routine;
import com.supplog.entity.Supplement;
import com.supplog.entity.User;
import com.supplog.enums.Frequency;
import com.supplog.exception.BusinessException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.service.supplement.impl.SupplementServiceImpl;
import com.supplog.service.user.ActiveUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplementServiceImplTest {

    @Mock SupplementRepository supplementRepository;
    @Mock ActiveUserService activeUserService;
    @Mock RoutineRepository routineRepository;

    private SupplementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SupplementServiceImpl(supplementRepository, activeUserService, routineRepository);
    }

    @Test
    void duplicateNameIsRejectedOnCreateAfterTrimming() {
        when(activeUserService.getRequiredActiveUser(7L)).thenReturn(new User());
        when(supplementRepository.existsByInsertedByUserIdAndIsDeletedFalseAndNameIgnoreCase(7L, "Vitamin D"))
                .thenReturn(true);

        CreateSupplementRequestDto request = mock(CreateSupplementRequestDto.class);
        when(request.getName()).thenReturn("  Vitamin D  ");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.addSupplement(7L, request));

        assertEquals("supplement.name.already.exists", exception.getMessage());
        verify(supplementRepository, never()).save(any());
    }

    @Test
    void updateIgnoresTheSupplementItself() {
        Supplement existing = new Supplement();
        existing.setId(3L);
        when(supplementRepository.findByIdAndInsertedByUserIdAndIsDeletedFalse(3L, 7L)).thenReturn(Optional.of(existing));
        when(supplementRepository.existsByInsertedByUserIdAndIsDeletedFalseAndNameIgnoreCaseAndIdNot(7L, "Zinc", 3L))
                .thenReturn(false);

        UpdateSupplementRequestDto request = mock(UpdateSupplementRequestDto.class);
        when(request.getName()).thenReturn("Zinc");

        service.updateMySupplement(7L, 3L, request);

        assertEquals("Zinc", existing.getName());
    }

    @Test
    void deleteInUseReturnsRoutineDetailsInsteadOfIds() {
        Supplement supplement = new Supplement();
        supplement.setId(3L);
        Routine routine = new Routine();
        routine.setId(6L);
        routine.setRoutineTime(LocalTime.of(8, 0));
        routine.setFrequency(Frequency.DAILY);

        when(supplementRepository.findByIdAndInsertedByUserIdAndIsDeletedFalse(3L, 7L)).thenReturn(Optional.of(supplement));
        when(routineRepository.findActiveRoutinesBySupplementId(3L)).thenReturn(List.of(routine));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.deleteMySupplement(7L, 3L));

        assertEquals("supplement.cannot.delete.in.use", exception.getMessage());
        assertArrayEquals(new Object[]{1}, exception.getArgs());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> routines = (List<Map<String, Object>>) exception.getDetails().get("routines");
        assertEquals(6L, routines.get(0).get("id"));
        assertEquals(LocalTime.of(8, 0), routines.get(0).get("routineTime"));
        assertEquals(Frequency.DAILY, routines.get(0).get("frequency"));
        assertFalse(supplement.isDeleted());
    }
}
