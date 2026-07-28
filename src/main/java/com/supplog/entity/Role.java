package com.supplog.entity;

import com.supplog.enums.RoleName;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name",nullable = false,unique = true,length = 30)
    private RoleName name;
}
