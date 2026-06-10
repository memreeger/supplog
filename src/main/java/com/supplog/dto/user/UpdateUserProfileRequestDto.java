package com.supplog.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateUserProfileRequestDto {
    private String firstName;
    private String lastName;
}
