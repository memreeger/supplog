package com.supplog.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserResponseDto extends BaseUserDto {
    private Long id;
    private String email;
}
