package com.supplog.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DeleteUserRequestDto {
    private String email;
    private String password;
}
