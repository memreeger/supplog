package com.supplog.controller;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.entity.Supplement;
import com.supplog.service.supplement.SupplementService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/supplement")
public class SupplementController {
    private final SupplementService supplementService;

    public SupplementController(SupplementService supplementService) {
        this.supplementService = supplementService;
    }

    @PostMapping
    public void addSupplement(@RequestBody CreateSupplementRequestDto supplementRequestDto) {
        supplementService.addSupplement(supplementRequestDto);
    }
}
