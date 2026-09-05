package com.supplog.dto.support;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SupportUserSummaryDto {

    private Long id;

    private String username;

    private String firstName;

    private String lastName;
}