package com.supplog.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CreateUserRequestDto extends BaseUserDto {
    private String email;
    private String password;
}
