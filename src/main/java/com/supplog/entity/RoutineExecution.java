package com.supplog.entity;

import com.supplog.enums.RoutineExecutionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "routine_executions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_routine_execution_routine_date",
                        columnNames = {
                                "routine_id",
                                "scheduled_date"
                        }
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
public class RoutineExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "routine_id",
            nullable = false
    )
    private Routine routine;

    @Column(
            name = "scheduled_date",
            nullable = false
    )
    private LocalDate scheduledDate;

    @Column(
            name = "scheduled_time",
            nullable = false
    )
    private LocalTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private RoutineExecutionStatus status;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreatedDate
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(
            name = "scheduled_zone_id",
            nullable = false,
            length = 64
    )
    private String scheduledZoneId;

    @Column(
            name = "scheduled_at",
            nullable = false
    )
    private Instant scheduledAt;


}