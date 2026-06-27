package com.supplog.dto.user;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private LocalDateTime createdAt;
}
