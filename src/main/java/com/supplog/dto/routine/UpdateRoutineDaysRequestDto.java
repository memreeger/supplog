package com.supplog.dto.routine;

import tools.jackson.databind.annotation.JsonDeserialize;
import com.supplog.config.json.UniqueDayOfWeekSetDeserializer;
import com.supplog.enums.DayOfWeek;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutineDaysRequestDto {

    @NotEmpty(message = "{validation.routine.days.required}")
    @JsonDeserialize(using = UniqueDayOfWeekSetDeserializer.class)
    private Set<DayOfWeek> daysOfWeek;
}
