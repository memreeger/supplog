package com.supplog.dto.user;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BaseUserDto {
    private String userName;
    private String firstName;
    private String lastName;
}
