package com.supplog.dto.login;

import com.supplog.dto.user.BaseUserDto;
import com.supplog.exception.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// record immutable bir sınıftır
// inheriitance yapamaz child veya sub class olamaz
// kendisi de final bir sınıftır bu nedenle super/parent sınıf da olamaz
// sınıflar gibi arayüz implement edebilir
// genelde record gövdesi olmaz ama propertylere atanan verileri doğrulamak istersek compact ctor yazmak gerekbilir

public record LoginRequestDto(

        @NotBlank(message = "{validation.username.required}")
        @Size(min = 3, max = 30, message = "{validation.username.size}")
        String username,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 6, max = 50, message = "{validation.password.size}")
        String password
) {

    /* record class içinde de constructor kullanılabilir ama özel durumlarda
    public LoginRequestDto {
        if (username == null || username.isEmpty())
            throw new ResourceNotFoundException("validation.username.required");
    }

     */
}
