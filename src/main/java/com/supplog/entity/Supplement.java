package com.supplog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supplog.enums.RoutineCategory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "supplements")
public class Supplement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplement_name", nullable = false, length = 100)
    private String name;

    @Column(name = "supplement_dosage", nullable = false, length = 100)
    private String suppDosage;

    @Column(name = "expire_date", nullable = false)
    private LocalDate expireDate;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "category",nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private RoutineCategory type;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JsonIgnore
    @JoinColumn(name = "inserted_by_user_id", nullable = false)
    private User insertedByUser;


    @OneToMany(mappedBy = "supplement",fetch = FetchType.LAZY)
    private List<Routine> routines = new ArrayList<>();
}
