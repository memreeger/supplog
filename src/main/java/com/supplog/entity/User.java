package com.supplog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
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
    private List<Supplement> medicines;

    @OneToMany(mappedBy = "user")
    private List<Routine> routines;

    @Column(name = "e_mail", unique = true, nullable = false)
    private String email;

    @Column(name = "user_name", unique = true, nullable = false)
    private String username;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "score")
    private int score;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;


    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;


    public int getAge() {
        if (birthDate == null) {
            return 0;
        }

        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles = new HashSet<>();



}


