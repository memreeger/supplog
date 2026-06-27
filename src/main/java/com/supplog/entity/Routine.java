package com.supplog.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supplog.enums.DayOfWeek;
import com.supplog.enums.Period;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "routines")
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "supplement_id")
    @JsonIgnore
    private Supplement supplement;

    @Column(name = "day_name")
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayName;

    @Column(name = "routine_time")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime routineTime;

    @Enumerated(EnumType.STRING)
    private Period period;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;


}
