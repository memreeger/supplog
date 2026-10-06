package com.supplog.dto.user;

import com.supplog.enums.RoleName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserResponseDto {
    private Long id;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private LocalDate birthDate;

    private int age;

    private int score;

    private String timeZone;

    private Set<RoleName> roles;

    private LocalDateTime createdAt;
}
