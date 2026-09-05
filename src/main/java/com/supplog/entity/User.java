package com.supplog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Entity
@EntityListeners(AuditingEntityListener.class)
// programlama teknikleri
// 1. imperative : uygulamanıın ne yapacağını yazılımcı satır satır kod yazarak anlatır
// 2. declarative : kod yazmadan kullandığımız platforma ne yapmasını istediğinmiz söylüuoruz >> AOP : Aspect Oriented Programming
public class User {

    // User birthdate ekle ve hesaplat
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "insertedByUser")
    private List<Supplement> supplements = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Routine> routines = new ArrayList<>();

    @Column(name = "e_mail", unique = true, nullable = false, length = 254)
    private String email;

    @Column(name = "user_name", unique = true, nullable = false, length = 30)
    private String username;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;


    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(
            name = "time_zone",
            nullable = false,
            length = 64
    )
    private String timeZone;



    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id",
                    nullable = false
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id",
                    nullable = false
            ),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_user_roles_user_role",
                    columnNames = {"user_id", "role_id"}
            )
    )
    private Set<Role> roles = new HashSet<>();

    public int getAge() {
        if (birthDate == null) {
            return 0;
        }

        return Period.between(birthDate, LocalDate.now()).getYears();
    }

}


