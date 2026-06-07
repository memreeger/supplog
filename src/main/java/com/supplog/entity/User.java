package com.supplog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
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

    @Column(name = "e_mail")
    private String email;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "password")
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "score")
    private int score;


    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

}


