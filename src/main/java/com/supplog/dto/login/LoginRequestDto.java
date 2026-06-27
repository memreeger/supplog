package com.supplog.dto.login;

import com.supplog.dto.user.BaseUserDto;
import com.supplog.exception.ResourceNotFoundException;

// record immutable bir sınıftır
// inheriitance yapamaz child veya sub class olamaz
// kendisi de final bir sınıftır bu nedenle super/parent sınıf da olamaz
// sınıflar gibi arayüz impleent edebilir
public record LoginRequestDto(String username, String password) {

    // genelde record gövdesi olmaz ama propertylere atanan verileri doğrulamak istersek compact ctor yazmak gerekbilir

    public LoginRequestDto{
        if (username == null || username.isEmpty())
            throw new ResourceNotFoundException("validation.username.required");
    }
}
