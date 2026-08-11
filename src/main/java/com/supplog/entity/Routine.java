package com.supplog.entity;

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

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false // Java/JPA tarafında ilişkinin zorunlu olduğunu belirtir.
    )
    @JoinColumn(name = "supplement_id",nullable = false) // veritabanı tarafında foreign key’in boş olamayacağını belirtir.
    @JsonIgnore
    private Supplement supplement;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_name", nullable = false, length = 20)
    private DayOfWeek dayName;

    @Column(name = "routine_time",nullable = false)
    private LocalTime routineTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 30)
    private Period period;

    @Column(name = "is_deleted",nullable = false)
    private boolean isDeleted = false;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(name = "user_id",nullable = false)
    @JsonIgnore
    private User user;


}
