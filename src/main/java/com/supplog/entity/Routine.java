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

    /*
    @OneToOne
    @JoinColumn(name = "supplement_id")
    @JsonIgnore
    private Supplement supplement;

     */
    @ManyToOne
    @JoinColumn(name = "supplement_id",nullable = false)
    @JsonIgnore
    Supplement supplement;

    @Column(name = "day_name",nullable = false)
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayName;

    @Column(name = "routine_time",nullable = false)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime routineTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false)
    private Period period;

    @Column(name = "is_deleted",nullable = false)
    private boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    @JsonIgnore
    private User user;


}
