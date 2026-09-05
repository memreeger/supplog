package com.supplog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "support_routine_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_support_permission_relationship_routine",
                        columnNames = {
                                "support_relationship_id",
                                "routine_id"
                        }
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
public class SupportRoutinePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "support_relationship_id",
            nullable = false
    )
    private SupportRelationship supportRelationship;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "routine_id",
            nullable = false
    )
    private Routine routine;

    @CreatedDate
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
}
