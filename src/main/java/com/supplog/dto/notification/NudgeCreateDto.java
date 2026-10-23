package com.supplog.dto.notification;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NudgeCreateDto {

    @NotNull
    @Positive
    private Long routineId;

    // Hazır şablon anahtarı (NudgeTemplate) ya da serbest metin; tam olarak biri dolu olmalı.
    private String templateKey;

    // Üst sınır burada kaba bir koruma; asıl temizlik/doğrulama NudgeTextSanitizer'da.
    @Size(max = 1000)
    private String message;
}
