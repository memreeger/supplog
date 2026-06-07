package com.supplog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supplog.enums.SupplementCategory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "supplements")
public class Supplement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplement_name")
    private String name;

    @Column(name = "supplement_dosage")
    private String suppDosage;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Column(name = "category")
    private SupplementCategory type;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @ManyToOne
    @JsonIgnore
    private User insertedByUser;

    @OneToOne(mappedBy = "supplement")
    @JsonIgnore
    private Routine routine;
}
