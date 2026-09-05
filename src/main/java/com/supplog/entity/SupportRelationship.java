package com.supplog.entity;

import com.supplog.enums.SupportAccessScope;
import com.supplog.enums.SupportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "support_relationships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_support_relationship_users",
                        columnNames = {
                                "supported_user_id",
                                "supporter_user_id"
                        }
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
public class SupportRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "supported_user_id",
            nullable = false
    )
    private User supportedUser;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "supporter_user_id",
            nullable = false
    )
    private User supporter;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private SupportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "access_scope",
            nullable = false,
            length = 30
    )
    private SupportAccessScope accessScope;

    @Column(
            name = "requested_at",
            nullable = false
    )
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

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
}