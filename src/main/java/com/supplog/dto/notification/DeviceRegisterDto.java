package com.supplog.dto.notification;

import com.supplog.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegisterDto {

    @NotBlank
    @Size(max = 512)
    private String pushToken;

    @NotNull
    private DevicePlatform platform;

    // Bildirim dili için (tr, en, tr-TR…).
    @Pattern(regexp = "^[A-Za-z]{2,3}([-_][A-Za-z0-9]{2,5})?$")
    private String locale;

    @Size(max = 30)
    private String appVersion;
}
